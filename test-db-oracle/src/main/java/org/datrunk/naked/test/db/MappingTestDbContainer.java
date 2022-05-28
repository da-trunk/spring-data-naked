package org.datrunk.naked.test.db;

import javax.annotation.Nonnull;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.OracleContainer;

/**
 * Extends {@link OracleContainer} with properties and methods specific to code-mapping-database.
 *
 * @author BA030483
 */
public class MappingTestDbContainer extends OracleTestContainer {
  private MappingTestDbContainer(final @Nonnull Environment environment, Network network) {
    super(
        environment.getProperty("test.containers.mapping.image"),
        Enum.valueOf(
            OracleTestContainer.Type.class,
            environment.getProperty("test.containers.mapping.type")),
        environment.getProperty("test.containers.mapping.datasource.username"),
        environment.getProperty("test.containers.mapping.datasource.password"));
  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  public static class Factory
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static MappingTestDbContainer instance = null;

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
      System.setProperty("java.io.tmpdir", "/tmp");
      create(ctx);
    }

    private static void create(ConfigurableApplicationContext ctx) {
      final Environment env = ctx.getEnvironment();
      if (instance == null) {
        //        PropertySource<?> dataSourceProperties =
        //            env.getPropertySources().get("test.containers.mrds.datasource");
        final boolean reUse =
            Boolean.parseBoolean(env.getProperty("test.containers.mapping.reuse"));

        instance =
            new MappingTestDbContainer(env, getNetwork()) {
              @Override
              protected void configure(OracleTestContainer container) {
                container.withReuse(reUse);
              }
            };
        System.setProperty("test.containers.mapping.datasource.url", instance.getJdbcUrl());
      }

      // Programmatically register the container as a bean so it can be injected
      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
      if (!beanFactory.containsBean(MappingTestDbContainer.class.getName())) {
        beanFactory.registerSingleton(MappingTestDbContainer.class.getName(), instance);
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            ctx, "test.containers.mapping.datasource.url=" + instance.getJdbcUrl());
      }
    }

    /**
     * Destroy the container and recreate it under a new port. The container's bean in the provided
     * ApplicationContext is refreshed. Use this only when necessary to clean up the DB after a test
     * which has committed data.
     *
     * <p>WARNING: the provided context isn't refreshed, so other beans will still point to the old
     * container. To refresh all beans in your context, add a @DirtiesContext annotation to your
     * test class. I tried {@link ConfigurableApplicationContext#refresh} and {@link
     * org.springframework.cloud.context.refresh.ContextRefresher#refresh} (see the second answer <a
     *
     * <p>href="https://stackoverflow.com/questions/24720330/reload-or-refresh-a-spring-application-context-inside-a-test-method">here</a>),
     * but @DirtiesContext is all that worked for me.
     */
    public static void destroy(ConfigurableApplicationContext ctx) {
      instance.stop();
      instance = null;
      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
      beanFactory.destroyBean(beanFactory.getBean(MappingTestDbContainer.class.getName()));
    }

    public static String restart(ConfigurableApplicationContext ctx) {
      if (instance == null) {
        throw new IllegalStateException("cannot restart an instance that was never started");
      }
      destroy(ctx);
      Factory.create(ctx);
      return instance.getJdbcUrl();
    }

    public static Network getNetwork() {
      return instance == null ? Network.newNetwork() : instance.getNetwork();
    }
  }
}
