package org.datrunk.naked.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datrunk.naked.db.jdbc.DataSourceWrapper;
import org.datrunk.naked.db.jdbc.ThrowingConsumer;
import org.datrunk.naked.test.container.SpringTestContainer;
import org.datrunk.naked.test.container.SpringTestContainers;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

/** Adds methods to {@link SpringTestContainer} that are useful for RDBMS containers. */
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
     * Returns a composed {@code Consumer} that performs, in sequence, this operation followed by
     * the {@code after} operation. If performing either operation throws an exception, it is
     * relayed to the caller of the composed operation. If performing this operation throws an
     * exception, the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code Consumer} that performs in sequence this operation followed by the
     *     {@code after} operation
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
      throws Exception {
    try (Connection conn = getConnection(user, password)) {
      Database database =
          DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
      try (ClassLoaderResourceAccessor clra = new ClassLoaderResourceAccessor();
          Liquibase liquibase = new Liquibase(changeLogFile, clra, database)) {
        command.accept(liquibase);
      }
    }
  }

  default void updateAsSys(String changeLog) throws Exception {
    try (Connection conn = getSysConnection()) {
      final Contexts contexts = new Contexts();
      execute(liquibase -> liquibase.update(contexts), changeLog, conn);
    }
  }

  default void update(String changeLog) throws Exception {
    final Contexts contexts = new Contexts();
    update(changeLog, contexts);
  }

  default void update(String changeLog, Contexts contexts) throws Exception {
    try (Connection conn = getConnection(getUsername(), getPassword())) {
      execute(liquibase -> liquibase.update(contexts), changeLog, conn);
    }
  }

  default void tag(String changeLog) throws Exception {
    execute(liquibase -> liquibase.tag("0"), changeLog, getUsername(), getPassword());
  }

  default void execute(LiquibaseCommand command, String changeLogFile, Connection connection)
      throws Exception {
    Database database =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection));
    try (ClassLoaderResourceAccessor clra = new ClassLoaderResourceAccessor();
        Liquibase liquibase = new Liquibase(changeLogFile, clra, database)) {
      command.accept(liquibase);
    } finally {
    }
  }

  default void rollback(String rollBackToTag) throws Exception {
    try (Connection conn = getConnection(getUsername(), getPassword())) {
      rollback(rollBackToTag, conn);
    }
  }

  default void rollback(String rollBackToTag, Connection connection) throws Exception {
    final Contexts contexts = new Contexts("xe", "production");
    execute(
        liquibase -> liquibase.rollback(rollBackToTag, contexts),
        "schema-update-versioned.xml",
        connection);
  }

  default void rollback() throws Exception {
    rollback("0");
  }

  @SuppressWarnings("resource")
  default void init(
      @Nonnull JdbcDatabaseContainer<?> result,
      final @Nonnull Environment environment,
      @Nonnull BiConsumer<Integer, Integer> addFixedExposedPort) {
    DockerImageName.parse(result.getDockerImageName()).assertValid();
    result.withNetwork(SpringTestContainers.getNetwork());
    result.withNetworkAliases("db");
    result.withAccessToHost(true);
    assert (!result.getExposedPorts().isEmpty());
    result.withDatabaseName(environment.getProperty("spring.datasource.database"));
    result.withUsername(environment.getProperty("spring.datasource.username"));
    result.withPassword(environment.getProperty("spring.datasource.password"));
    if (result.getDockerImageName().endsWith(":latest")) {
      result.withImagePullPolicy(PullPolicy.alwaysPull());
    }
    if (environment.getProperty("spring.datasource.container.port") == null) {
      result.withReuse(false);
    } else {
      addFixedExposedPort.accept(
          Integer.valueOf(environment.getProperty("spring.datasource.container.port")),
          result.getExposedPorts().get(0));
      result.withReuse(true);
    }
  }

  void setJdbcUrl(String url);

  void start();

  void stop();

  @Order(Ordered.HIGHEST_PRECEDENCE)
  public static class Factory
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static SpringTestDbContainer instance = null;
    private static boolean shouldStop = true;
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
      final ConfigurableEnvironment env = ctx.getEnvironment();
      BindResult<DataSourceProperties> binder =
          Binder.get(env).bind("spring.datasource", DataSourceProperties.class);
      DataSourceProperties props = binder.get();
      if (instance == null) {
        final Logger log = LogManager.getLogger();
        try {
          Constructor<? extends SpringTestDbContainer> constructor =
              clazz.getConstructor(Environment.class);
          instance = constructor.newInstance(env);
          shouldStop = env.getProperty("spring.datasource.container.port") == null;
        } catch (NoSuchMethodException
            | SecurityException
            | InstantiationException
            | IllegalAccessException
            | IllegalArgumentException
            | InvocationTargetException e) {
          log.catching(e);
          throw new IllegalArgumentException(
              "Cannot construct an instance of" + clazz.getName(), e);
        }
        if (props.getUrl() == null) {
          List<String> errors =
              Stream.of("database", "username", "password")
                  .map(name -> "spring.datasource." + name)
                  .filter(name -> !env.containsProperty(name))
                  .map(
                      name ->
                          "Property "
                              + name
                              + " must be defined in the environment when spring.datasource.url is undefined")
                  .collect(Collectors.toList());
          if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.stream().collect(Collectors.joining("\n")));
          }
          try {
            instance.start();
            props.setUrl(instance.getJdbcUrl());
          } catch (Exception e) {
            log.catching(e);
            throw e;
          }
          if (props.getUrl() == null) {
            throw new IllegalArgumentException(
                "The database started, but jdbcURL was not initialized");
          }
        } else {
          instance.setJdbcUrl(props.getUrl());
          log.info("{} connecting to {}", clazz.getSimpleName(), instance.getJdbcUrl());
        }

        ctx.addApplicationListener(
            applicationEvent -> {
              if (applicationEvent instanceof ContextClosedEvent) {
                if (shouldStop) {
                  instance.stop();
                }
              }
            });
      }

      // Programmatically register the container as a bean so it can be injected
      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
      if (!beanFactory.containsBean(clazz.getName())) {
        beanFactory.registerSingleton(clazz.getName(), instance);
        Map<String, Object> map = new HashMap<>();
        map.put("spring.datasource.url", instance.getJdbcUrl());
        env.getPropertySources().addFirst(new MapPropertySource("newmap", map));
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
