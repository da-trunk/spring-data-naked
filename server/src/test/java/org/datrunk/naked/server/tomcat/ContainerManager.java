package org.datrunk.naked.server.tomcat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

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

    private static JdbcDatabaseContainer<? extends JdbcDatabaseContainer<?>> db;
    private static TomcatContainer tomcat;
    private static ConfigurableApplicationContext ctx = null;

    protected ContainerManager() {
        Initializer.configurer = this::configure;
    }

    /**
     * Uses Spring environment variables to locate a war file and conf directory which should be accessible within the container before it
     * is started. Certain variables must be present in the {@link Environment}. Add a resource file to the test classpath which defines
     * these properties. Override this method to provide a custom implementation.
     * 
     * @param container a partially configured {@link TomcatContainer}
     * @param environment the Spring {@link Environment}
     * @return the {@link TomcatContainer} after configuring the war file and conf directories.
     */
    protected TomcatContainer configure(final TomcatContainer container, final Environment environment) {
        @Nonnull
        String baseDir = environment.getProperty("application.buildDir");
        assertThat(baseDir).isNotEmpty();
        assertThat(baseDir).doesNotStartWith("@");
        if (baseDir.startsWith("/c/")) // this messes up Windows
            baseDir = baseDir.substring(2);
        @Nonnull
        String war = environment.getProperty("application.warName");
        assertThat(war).isNotEmpty();
        assertThat(war).doesNotStartWith("@");

        // return container.withCopyFileToContainer(MountableFile.forClasspathResource(war, 0600), "/usr/local/tomcat/webapps/service.war")
        // .withClasspathResourceMapping("conf/", "/usr/local/tomcat/", BindMode.READ_ONLY);
        // container.copyFileToContainer(MountableFile.forHostPath(Paths.get(String.format("%s/%s", baseDir, war))
        // .toAbsolutePath()
        // .toString()), "/usr/local/tomcat/webapps/ROOT.war");
        // container.copyFileToContainer(MountableFile.forHostPath(Paths.get(String.format("%s/test-classes/conf/server.xml", baseDir))
        // .toAbsolutePath()
        // .toString()), "/usr/local/tomcat/conf/server.xml");
        // container
        // .copyFileToContainer(MountableFile.forHostPath(Paths.get(String.format("%s/test-classes/conf/Catalina/localhost/", baseDir))
        // .toAbsolutePath()
        // .toString()), "/usr/local/tomcat/conf/conf/Catalina/localhost");

//        exec(container, "rm", "-rf", "/usr/local/tomcat/webapps/ROOT");
        copyFileToContainer(container, String.format("%s/%s", baseDir, war), "/usr/local/tomcat/webapps/ROOT.war");
//        copyFileToContainer(container, String.format("%s/test-classes/conf/server.xml", baseDir), "/usr/local/tomcat/conf/server.xml");
        copyFileToContainer(container, String.format("%s/test-classes/conf/Catalina/localhost/", baseDir),
            "/usr/local/tomcat/conf/Catalina/localhost");
//        exec(container, "bin/shutdown.sh; sleep 5; bin/startup.sh");

        // return in.withFileSystemBind(confPath.toAbsolutePath()
        // .toString(), "/usr/local/tomcat/conf")
        // .withFileSystemBind(warPath.toAbsolutePath()
        // .toString(), "/usr/local/tomcat/webapps/service.war");

        return container;
    }

    private int exec(TomcatContainer container, String... command) {
        ExecResult result = null;
        try {
            log.info("exec [{}] in [{}]", Stream.of(command)
                .collect(Collectors.joining(" ")), container.getDockerImageName());
            result = container.execInContainer(command);
            log.info(result.getStdout());
        } catch (UnsupportedOperationException | IOException | InterruptedException e) {
            log.catching(e);
            if (result == null)
                log.error(result.getStderr());
        }
        assert (result != null);
        return result.getExitCode();
    }

    private TomcatContainer copyFileToContainer(TomcatContainer container, String fromPath, String toPath) {
//        exec(container, "rm", "-rf", toPath);
        log.info("Copying [{}:{}] to [{}:{}]", container.getHost(), fromPath, container.getDockerImageName(), toPath);
        container.copyFileToContainer(MountableFile.forHostPath(fromPath), toPath);
        return container;
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
        public static BiFunction<TomcatContainer, Environment, TomcatContainer> configurer = (container, env) -> {
            throw new IllegalStateException("The configurer has not been initialized");
        };

        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            final Network network = db == null ? Network.newNetwork() : db.getNetwork();
            log.info("created network [{}]", network.getId());
            System.setProperty("java.io.tmpdir", "/tmp");
            initialize(ctx, network);
        }

        public void initialize(ConfigurableApplicationContext ctx, Network network) {
            ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
            final Environment env = ctx.getEnvironment();
            if (db == null) {
                // Take properties from application.properties
                String user = env.getProperty("spring.datasource.username");
                assertThat(user).isNotNull();
                String password = env.getProperty("spring.datasource.password");
                assertThat(password).isNotNull();
                String image = env.getProperty("spring.datasource.container.image");
                assertThat(image).isNotNull();
                boolean reUse = Boolean.parseBoolean(env.getProperty("spring.datasource.container.reuse"));

                db = new OracleTestContainer(image, user, password, network, container -> container.withReuse(reUse));
                log.info("Oracle started at {}", db.getJdbcUrl());

                // Programmatically register the containers as beans so they can be injected
                beanFactory.registerSingleton(db.getClass()
                    .getCanonicalName(), db);
                TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, "spring.datasource.url=" + db.getJdbcUrl());
                System.setProperty("DB_URL", db.getJdbcUrl());
            }
            if (tomcat == null) {
                assertThat(db).isNotNull();

                @Nonnull
                String image = env.getProperty("application.container.image");
                @Nonnull
                boolean reUse = Boolean.parseBoolean(env.getProperty("application.container.reuse"));

                tomcat = new TomcatContainer(image, network, env, db, container -> container.withReuse(reUse));

//                configurer.apply(tomcat, env);

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
        if (db != null)
            registry.add("spring.datasource.url", db::getJdbcUrl);
    }

    public static JdbcDatabaseContainer<? extends JdbcDatabaseContainer<?>> getDbInstance() {
        if (db == null) {
            throw new IllegalStateException("No instance has been initialized");
        }
        return db;
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
        if (db != null) {
            assertThat(ctx).isNotNull();
            tomcat.close();
            tomcat = null;
            db.close();
            db = null;
            Network network = db.getNetwork();
            // db = new OracleTestContainer(db.getDockerImageName(), db.getUsername(), db.getPassword(), network,
            // container -> container.withReuse(false));
            // tomcat = new TomcatContainer(tomcat.getDockerImageName(), network, db, container -> container.withReuse(false));
            if (liquibaseContainer != null)
                liquibaseContainer.reset();
            Initializer initializer = new ContainerManager.Initializer();
            initializer.initialize(ctx, network);
        }
    }
}
