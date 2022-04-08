package org.datrunk.naked.db;

import org.datrunk.naked.db.oracle.OracleTestContainer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.Network;

import javax.sql.DataSource;

/**
 * Useful base class for {@link SpringBootTest}-style tests when {@link EnableAutoConfiguration} is active. This ensures that the oracle-xe
 * database container is started before other Spring auto configured classes. It does this in a way to ensure the database URL is provided
 * to other auto-configured beans before they are created.
 * 
 * Only one container instance is created. All tests must share this instance. If you require the container to be recreated between tests,
 * you'll need to inherit from a different base class.
 *
 * <p>
 * USAGE NOTES: Ensure the following properties are configured in application.properties. With the exception of spring.datasource.url, these
 * should match your database image. spring.datasource.url must be provided as specified so that testcontainers can injected the appropriate
 * value after creating the container.
 * <ul>
 * <li>spring.datasource.image=?
 * <li>spring.datasource.url=${DB_URL}
 * <li>spring.datasource.username=test
 * <li>spring.datasource.password=password
 * </ul>
 * 
 * @author ansonator
 *
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // Prevent tests from replacing the application's db config.
@ContextConfiguration(initializers = SpringTestBase.Initializer.class, classes = { SpringTestBase.Config.class })
public class SpringTestBase {
    private static OracleTestContainer instance;
    private static final Network network = Network.newNetwork();

    /**
     * This is only used to avoid requiring some implementing classes to declare @EnableAutoConfiguration again. See
     * <a href="https://stackoverflow.com/questions/52650551/enableautoconfiguration-on-abstractintegrationtest-possible">this issue</a>.
     */
    @EnableAutoConfiguration
    static class Config {}

    /**
     * Spring's {@link EnableAutoConfiguration} creates many beans which depend on the {@link javax.sql.DataSource}. But, we need the
     * {@link OracleTestContainer} to be created first. We use this to ensure that it is.
     * 
     */
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            if (instance == null) {
                // Take properties from application.properties
                String image = ctx.getEnvironment()
                    .getProperty("spring.datasource.container.image");
                String user = ctx.getEnvironment()
                    .getProperty("spring.datasource.username");
                String password = ctx.getEnvironment()
                    .getProperty("spring.datasource.password");
                boolean reUse = Boolean.parseBoolean(ctx.getEnvironment()
                    .getProperty("spring.datasource.container.reuse"));

                instance = new OracleTestContainer(image, user, password, network, container -> container.withReuse(reUse));

                // Programmatically register the container as a bean so it can be injected
                ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
                beanFactory.registerSingleton(instance.getClass()
                    .getCanonicalName(), instance);
            }
            String jdbcUrl = instance.getJdbcUrl();
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, "spring.datasource.url=" + jdbcUrl);
            System.setProperty("DB_URL", jdbcUrl);
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
        if (instance != null)
            registry.add("spring.datasource.url", instance::getJdbcUrl);
    }

    public static OracleTestContainer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("No instance has been initialized");
        }
        return instance;
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
     * 
     * @param ctx
     */
    public static void refresh(ConfigurableApplicationContext ctx) {
        if (instance != null) {
            instance.close();
            instance = new OracleTestContainer(instance.getDockerImageName(), instance.getUsername(), instance.getPassword(), network,
                container -> container.withReuse(instance.isShouldBeReused()));
            Initializer initializer = new SpringTestBase.Initializer();
            initializer.initialize(ctx);
        }
    }
}
