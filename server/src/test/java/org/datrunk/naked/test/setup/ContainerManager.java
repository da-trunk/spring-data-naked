package org.datrunk.naked.test.setup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datrunk.naked.test.db.TestLiquibaseConfiguration;
import org.datrunk.naked.test.db.oracle.OracleTestContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.Network;

import javax.sql.DataSource;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;


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
    protected static final String WAR = "../server/target/server-%s.war";

    private static OracleTestContainer oracle;
    private static TomcatContainer tomcat;
    private static ConfigurableApplicationContext ctx = null;

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
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
            System.setProperty("java.io.tmpdir", "/tmp");
            final Network network = oracle == null ? Network.newNetwork() : oracle.getNetwork();
            if (oracle == null) {
                // Take properties from application.properties
                String user = ctx.getEnvironment()
                    .getProperty("spring.datasource.username");
                String password = ctx.getEnvironment()
                    .getProperty("spring.datasource.password");
                String image = ctx.getEnvironment()
                    .getProperty("spring.datasource.container.image");
                boolean reUse = Boolean.parseBoolean(ctx.getEnvironment()
                    .getProperty("spring.datasource.container.reuse"));

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

                // This is populated via resource filtering during phase test-compile. It is in src/test/resources/application.properties.
                String version = ctx.getEnvironment()
                    .getProperty("application.version");
                String image = ctx.getEnvironment()
                    .getProperty("application.container.image");
                boolean reUse = Boolean.parseBoolean(ctx.getEnvironment()
                    .getProperty("application.container.reuse"));

                assertThat(version).isNotNull();
                final Path warPath = Paths.get(String.format(WAR, version));

                tomcat = new TomcatContainer(image, warPath, network, oracle, container -> container.withReuse(reUse));

                beanFactory.registerSingleton(tomcat.getClass()
                    .getCanonicalName(), tomcat);
                UriComponentsBuilder restUri = UriComponentsBuilder.fromUri(tomcat.getBaseUrl());
                String springDataRestBasePath = ctx.getEnvironment()
                    .getProperty("spring.data.rest.base-path");
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
            tomcat = new TomcatContainer(tomcat.getDockerImageName(), tomcat.getWarPath(), network, oracle,
                container -> container.withReuse(false));
            if (liquibaseContainer != null)
                liquibaseContainer.reset();
            Initializer initializer = new ContainerManager.Initializer();
            initializer.initialize(ctx);
        }
    }
}
