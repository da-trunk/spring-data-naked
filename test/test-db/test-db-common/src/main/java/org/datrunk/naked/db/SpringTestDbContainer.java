package org.datrunk.naked.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datrunk.naked.db.jdbc.DataSourceWrapper;
import org.datrunk.naked.db.jdbc.ThrowingConsumer;
import org.datrunk.naked.test.container.SpringTestContainer;
import org.datrunk.naked.test.container.SpringTestContainers;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * Adds methods to {@link SpringTestContainer} that are useful for RDBMS
 * containers.
 */
public interface SpringTestDbContainer extends SpringTestContainer {
  String getJdbcUrl();

  String getUsername();

  String getPassword();

  String getDriverClassName();

  Connection getSysConnection() throws SQLException;

  default Connection getConnection() throws SQLException {
    return getConnection(getUsername(), getPassword());
  }

  default Connection getConnection(String user, String password) throws SQLException {
    return DriverManager.getConnection(getJdbcUrl(), user, password);
  }

  default DataSourceWrapper getDataSourceWrapper() {
    return new DataSourceWrapper(getUsername(), getPassword(), getJdbcUrl());
  }

  default DataSource getDataSource() {
    return DataSourceBuilder.create()
        .url(getJdbcUrl())
        .driverClassName(getDriverClassName())
        .username(getUsername())
        .password(getPassword())
        .build();
  }

  @FunctionalInterface
  public interface LiquibaseCommand extends ThrowingConsumer<Liquibase> {
    @Override
    void accept(Liquibase liquibase) throws LiquibaseException;

    /**
     * Returns a composed {@code Consumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the composed
     * operation. If performing this operation throws an exception, the
     * {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code Consumer} that performs in sequence this operation
     *         followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default LiquibaseCommand andThen(LiquibaseCommand after) {
      Objects.requireNonNull(after);
      return (Liquibase liquibase) -> {
        accept(liquibase);
        after.accept(liquibase);
      };
    }
  }

  default void execute(LiquibaseCommand command, String changeLogFile, String user, String password)
      throws SQLException, LiquibaseException {
    Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(getConnection(user, password)));
    try (Liquibase liquibase = new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), database)) {
      command.accept(liquibase);
    }
  }

  default void updateAsSys(String changeLog) throws LiquibaseException, SQLException {
    update(changeLog, getSysConnection());
  }

  default void update(String changeLog) throws LiquibaseException, SQLException {
    update(changeLog, getConnection());
  }

  default void update(String changeLog, String user, String password) throws LiquibaseException, SQLException {
    update(changeLog, getConnection(user, password));
  }

  default void update(String changeLog, Connection connection) throws LiquibaseException, SQLException {
    final Contexts contexts = new Contexts("xe", "production");
    execute(liquibase -> liquibase.update(contexts), changeLog, connection);
  }

  default void execute(LiquibaseCommand command, String changeLogFile, Connection connection) throws SQLException, LiquibaseException {
    Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
    try (Liquibase liquibase = new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), database)) {
      command.accept(liquibase);
    } finally {
    }
  }

  default void rollback(String rollBackToTag) throws LiquibaseException, SQLException {
    rollback(rollBackToTag, getConnection());
  }

  default void rollback(String rollBackToTag, Connection connection) throws LiquibaseException, SQLException {
    final Contexts contexts = new Contexts("xe", "production");
    execute(liquibase -> liquibase.rollback(rollBackToTag, contexts), "schema-update-versioned.xml", connection);
  }

  default void rollback() throws LiquibaseException, SQLException {
    rollback("0");
  }

  default void init(@Nonnull JdbcDatabaseContainer<?> result, final @Nonnull Environment environment,
      @Nonnull BiConsumer<Integer, Integer> addFixedExposedPort) {
    DockerImageName.parse(result.getDockerImageName()).assertValid();
    result.withNetwork(SpringTestContainers.getNetwork());
    result.withNetworkAliases("db");
    result.withAccessToHost(true);
    assert (!result.getExposedPorts().isEmpty());
    result.withUsername(environment.getProperty("spring.datasource.username"));
    result.withPassword(environment.getProperty("spring.datasource.password"));
    if (result.getDockerImageName().endsWith(":latest")) {
      result.withImagePullPolicy(PullPolicy.alwaysPull());
    }
    if (environment.getProperty("spring.datasource.container.port") == null) {
      result.withReuse(false);
    } else {
      addFixedExposedPort.accept(Integer.valueOf(environment.getProperty("spring.datasource.container.port")), result.getExposedPorts().get(0));
      result.withReuse(true);
    }
  }

  void setJdbcUrl(String url);

  void start();

  void stop();

  @Order(Ordered.HIGHEST_PRECEDENCE)
  public static class Factory implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static SpringTestDbContainer instance = null;
    private final Class<? extends SpringTestDbContainer> clazz;

    public Factory(Class<? extends SpringTestDbContainer> clazz) {
      this.clazz = clazz;
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
      System.setProperty("java.io.tmpdir", "/tmp");
      create(ctx);
    }

    private void create(ConfigurableApplicationContext ctx) {
      final Environment env = ctx.getEnvironment();
      if (instance == null) {
        final Logger log = LogManager.getLogger();
        try {
          Constructor<? extends SpringTestDbContainer> constructor = clazz.getConstructor(Environment.class);
          instance = constructor.newInstance(env);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException e) {
          log.catching(e);
          throw new IllegalArgumentException("Cannot construct an instance of" + clazz.getName());
        }
        if (env.getProperty("spring.datasource.url") == null) {
          instance.start();
          System.setProperty("spring.datasource.url", instance.getJdbcUrl());
        } else {
          instance.setJdbcUrl(env.getProperty("spring.datasource.url"));
          log.info("{} connecting to {}", clazz.getSimpleName(), instance.getJdbcUrl());
        }
      }

      // Programmatically register the container as a bean so it can be injected
      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
      if (!beanFactory.containsBean(clazz.getName())) {
        beanFactory.registerSingleton(clazz.getName(), instance);
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, "spring.datasource.url=" + instance.getJdbcUrl());
      }
    }

    public void destroy(ConfigurableApplicationContext ctx) {
      instance.stop();
      instance = null;
      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
      beanFactory.destroyBean(beanFactory.getBean(clazz.getName()));
    }

    public void restart(ConfigurableApplicationContext ctx, String dbUrl) {
      if (instance == null) {
        throw new IllegalStateException("cannot restart an instance that was never started");
      }
      destroy(ctx);
      create(ctx);
    }
  }
}
