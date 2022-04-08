package org.datrunk.naked.server.tomcat;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datrunk.naked.db.TestLiquibaseConfiguration;
import org.datrunk.naked.db.oracle.OracleTestContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.Network;

/**
 * Manages docker containers for integration tests.
 * 
 * Usage: test classes requiring a v1 service and DB should extend this class. Their application properties should also point to a Liquibase
 * change set which deploys additional schema to the DB container.
 * 
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = ContainerManager.Initializer.class,
    classes = { TestLiquibaseConfiguration.LiquibaseRunOnce.class, ContainerManager.Config.class })
public class ContainerManager {
    private static Logger log = LogManager.getLogger();

    private static OracleTestContainer oracle;
    private static TomcatContainer tomcat;
    private static ConfigurableApplicationContext ctx = null;

    protected ContainerManager() {
        Initializer.configurer = Initializer::defaultConfigurer;
    }

    protected ContainerManager(BiFunction<TomcatContainer, Environment, TomcatContainer> configurer) {
        Initializer.configurer = configurer;
    }

    protected Path getWarPath(String artifactId, String version) {
        return Paths.get(String.format("target/$s-%s.war", artifactId, version));
    }

    @Autowired
    public void setContext(ConfigurableApplicationContext ctx) {
        ContainerManager.ctx = ctx;
    }

    private static TestLiquibaseConfiguration.LiquibaseRunOnce liquibaseContainer = null;

    @Autowired(required = false)
    public void setLiquibaseRunOnce(TestLiquibaseConfiguration.LiquibaseRunOnce liquibaseContainer) {
        ContainerManager.liquibaseContainer = liquibaseContainer;
    }

    /**
     * This is only used to avoid requiring some implementing classes to declare @EnableAutoConfiguration again. See
     * <a href="https://stackoverflow.com/questions/52650551/enableautoconfiguration-on-abstractintegrationtest-possible">this issue</a>.
     */
    @EnableAutoConfiguration
    public static class Config {}

    /**
     * Spring's {@link EnableAutoConfiguration} creates many beans which depend on the {@link javax.sql.DataSource}. But, we need the
     * {@link OracleTestContainer} to be created first. We use this to ensure that it is.
     * 
     */
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public static BiFunction<TomcatContainer, Environment, TomcatContainer> configurer;

        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
            System.setProperty("java.io.tmpdir", "/tmp");
            final Environment env = ctx.getEnvironment();
            final Network network = oracle == null ? Network.newNetwork() : oracle.getNetwork();
            if (oracle == null) {
                // Take properties from application.properties
                String user = env.getProperty("spring.datasource.username");
                assertThat(user).isNotNull();
                String password = env.getProperty("spring.datasource.password");
                assertThat(password).isNotNull();
                String image = env.getProperty("spring.datasource.container.image");
                assertThat(image).isNotNull();
                boolean reUse = Boolean.parseBoolean(env.getProperty("spring.datasource.container.reuse"));

                oracle = new OracleTestContainer(image, user, password, network, container -> container.withReuse(reUse));
                log.info("Oracle started at {}", oracle.getJdbcUrl());

                // Programmatically register the containers as beans so they can be injected
                beanFactory.registerSingleton(oracle.getClass()
                    .getCanonicalName(), oracle);
                TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, "spring.datasource.url=" + oracle.getJdbcUrl());
                System.setProperty("DB_URL", oracle.getJdbcUrl());
            }
            if (tomcat == null) {
                assertThat(oracle).isNotNull();

                @Nonnull
                String image = env.getProperty("application.container.image");
                @Nonnull
                boolean reUse = Boolean.parseBoolean(env.getProperty("application.container.reuse"));

                tomcat = new TomcatContainer(image, network, oracle, container -> defaultConfigurer(container.withReuse(reUse), env));

                beanFactory.registerSingleton(tomcat.getClass()
                    .getCanonicalName(), tomcat);
                UriComponentsBuilder restUri = UriComponentsBuilder.fromUri(tomcat.getBaseUrl());
                String springDataRestBasePath = env.getProperty("spring.data.rest.base-path");
                if (springDataRestBasePath != null) {
                    restUri.pathSegment(springDataRestBasePath);
                }
                TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, "client.repo.location=" + restUri.build()
                    .toUri()
                    .toASCIIString());
                System.setProperty("V1_URL", tomcat.getBaseUrl()
                    .toASCIIString());
            }
        }

        public static TomcatContainer defaultConfigurer(final TomcatContainer in, final Environment env) {
            // This is populated via resource filtering during phase test-compile. It is in src/test/resources/application.properties.
            @Nonnull
            String baseDir = env.getProperty("application.buildDir");
            @Nonnull
            String artifactId = env.getProperty("application.artifactId");
            assertThat(artifactId).isNotNull();
            @Nonnull
            String version = env.getProperty("application.version");
            assertThat(version).isNotNull();
            Path warPath = Paths.get(String.format("%s/%s-%s.war", baseDir, artifactId, version));
            
            String warDir = env.getProperty("application.warDir");
            String confDir = env.getProperty("application.confDir");

            return in.withFileSystemBind(String.format("%s/conf", baseDir), "/usr/local/tomcat/conf")
                .withFileSystemBind(warPath.toAbsolutePath()
                    .toString(), "/usr/local/tomcat/webapps/service.war");
        }
    }

    /**
     * This is called twice. First, it is called before {@link Initializer#initialize} and we do nothing. The second time it is called, we
     * extract the JDBC URL from the container and assign it to the {@link DataSource} property "spring.datasource.url". This makes it
     * available for beans that are auto configured after this second call.
     * 
     * @param registry
     */
    @DynamicPropertySource
    static void datasourceConfig(DynamicPropertyRegistry registry) {
        if (oracle != null)
            registry.add("spring.datasource.url", oracle::getJdbcUrl);
    }

    public static OracleTestContainer getOracleInstance() {
        if (oracle == null) {
            throw new IllegalStateException("No instance has been initialized");
        }
        return oracle;
    }

    public static TomcatContainer getServerInstance() {
        if (tomcat == null) {
            throw new IllegalStateException("No instance has been initialized");
        }
        return tomcat;
    }

    public URI getBaseURI() {
        if (tomcat == null) {
            throw new IllegalStateException("No instance has been initialized");
        }
        return tomcat.getBaseUrl();
    }

    /**
     * Destroy the container and recreate it under a new port. The container's bean in the provided ApplicationContext is refreshed. Use
     * this only when necessary to clean up the DB after a test which has committed data.
     * 
     * WARNING: the provided context isn't refreshed, so other beans will still point to the old container. To refresh all beans in your
     * context, add a @DirtiesContext annotation to your test class. I tried {@link ConfigurableApplicationContext#refresh} and
     * {@link org.springframework.cloud.context.refresh.ContextRefresher#refresh} (see the second answer
     * <a href="https://stackoverflow.com/questions/24720330/reload-or-refresh-a-spring-application-context-inside-a-test-method">here</a>),
     * but @DirtiesContext is all that worked for me.
     */
    public static void resetContainers() {
        if (oracle != null) {
            assertThat(ctx).isNotNull();
            tomcat.close();
            oracle.close();
            Network network = oracle.getNetwork();
            oracle = new OracleTestContainer(oracle.getDockerImageName(), oracle.getUsername(), oracle.getPassword(), network,
                container -> container.withReuse(false));
            tomcat = new TomcatContainer(tomcat.getDockerImageName(), network, oracle, container -> container.withReuse(false));
            if (liquibaseContainer != null)
                liquibaseContainer.reset();
            Initializer initializer = new ContainerManager.Initializer();
            initializer.initialize(ctx);
        }
    }
}
