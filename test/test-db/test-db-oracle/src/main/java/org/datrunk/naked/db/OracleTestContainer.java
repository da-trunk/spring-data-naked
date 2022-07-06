package org.datrunk.naked.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testcontainers.utility.DockerImageName;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * Extends {@link OracleContainer} with properties and methods specific to our
 * Oracle database projects.
 *
 * @author BA030483
 */
public class OracleTestContainer extends OracleContainer implements SpringTestDbContainer {
  private static Logger log = LogManager.getLogger();
  private final Type type;
  private String jdbcUrl;

  public static enum Type {
    xe11, xe18, ee19, xe21;
  }

  public OracleTestContainer(final @Nonnull Environment environment) {
    super(DockerImageName.parse(environment.getProperty("spring.datasource.container.image")).asCompatibleSubstituteFor("gvenzl/oracle-xe"));
    init(this, environment, this::addFixedExposedPort);
    withEnv("ORACLE_PWD", "password");
    type = Type.valueOf(environment.getProperty("spring.datasource.container.type"));
    final String tmpDir = getHostTempDir(type.name()).toAbsolutePath().toString();
    switch (type) {
    case xe11:
      withSharedMemorySize(FileUtils.ONE_GB).withEnv("ORACLE_SID", "oracle")
          .withEnv("ORACLE_PDB", "oracle")
          .withDatabaseName("XE")
          .waitingFor(new LogMessageWaitStrategy().withRegEx("DATABASE IS READY TO USE!\\n"));
      break;
    case xe18:
    case xe21:
      this.withEnv("ORACLE_PDB", "XEPDB1")
          .withFileSystemBind(tmpDir, "/opt/oracle/oradata", BindMode.READ_WRITE)
          .waitingFor(
              new LogMessageWaitStrategy().withRegEx(".*DATABASE IS READY TO USE!.*\\s").withStartupTimeout(Duration.ofMinutes(20)))
          .withStartupTimeout(Duration.ofMinutes(20)); // necessary when building for the first time on the server
      break;
    case ee19:
      withSharedMemorySize(FileUtils.ONE_GB).withEnv("ORACLE_PDB", "ORCLPDB1")
          .withDatabaseName("ORCLPDB1")
          .withFileSystemBind(tmpDir, "/opt/oracle/oradata", BindMode.READ_WRITE)
          .waitingFor(new LogMessageWaitStrategy().withRegEx("DATABASE IS READY TO USE!\\n").withStartupTimeout(Duration.ofMinutes(20)))
          .withStartupTimeout(Duration.ofMinutes(20)); // necessary when building for the first time on the server
      break;
    default:
      break;
    }
//    this(environment.getProperty("spring.datasource.image"), Type.valueOf(environment.getProperty("spring.datasource.type")),
//        environment.getProperty("spring.datasource.username"), environment.getProperty("spring.datasource.password"),
//        SpringTestContainer.getPort("oracle", environment));
  }

//  public OracleTestContainer(final OracleTestContainer other) {
//    this(other.getDockerImageName(), other.getType(), other.getUsername(), other.getPassword(), OptionalInt.empty());
//  }
//
//  public OracleTestContainer(@Nonnull String imageName, Type type, @Nonnull String userName, @Nonnull String password,
//      OptionalInt hostPort) {
//    super(DockerImageName.parse(imageName).asCompatibleSubstituteFor("gvenzl/oracle-xe"));
//    withNetwork(SpringTestContainers.getNetwork());
//    withNetworkAliases("db");
//    withExposedPorts(1521);
//    withAccessToHost(true);
//    if (hostPort.isEmpty()) {
//      withExposedPorts(1521);
//      withReuse(false);
//    } else {
//      addFixedExposedPort(hostPort.getAsInt(), 1521, InternetProtocol.TCP);
//      withReuse(true);
//    }
//    withUsername(userName);
//    withPassword(password);
//
//    withEnv("ORACLE_PWD", "password");
//    final String tmpDir = getHostTempDir(type.name()).toAbsolutePath().toString();
//    switch (type) {
//    case xe11:
//      withSharedMemorySize(FileUtils.ONE_GB).withEnv("ORACLE_SID", "oracle")
//          .withEnv("ORACLE_PDB", "oracle")
//          .withDatabaseName("XE")
//          .waitingFor(new LogMessageWaitStrategy().withRegEx("DATABASE IS READY TO USE!\\n"));
//      break;
//    case xe18:
//    case xe21:
//      this.withEnv("ORACLE_PDB", "XEPDB1")
//          .withFileSystemBind(tmpDir, "/opt/oracle/oradata", BindMode.READ_WRITE)
//          .waitingFor(
//              new LogMessageWaitStrategy().withRegEx("Completed: ALTER DATABASE OPEN\\n").withStartupTimeout(Duration.ofMinutes(12)))
//          .withStartupTimeout(Duration.ofMinutes(12)); // necessary when building for the first time on the server
//      break;
//    case ee19:
//      withSharedMemorySize(FileUtils.ONE_GB).withEnv("ORACLE_PDB", "ORCLPDB1")
//          .withDatabaseName("ORCLPDB1")
//          .withFileSystemBind(tmpDir, "/opt/oracle/oradata", BindMode.READ_WRITE)
//          .waitingFor(new LogMessageWaitStrategy().withRegEx("DATABASE IS READY TO USE!\\n").withStartupTimeout(Duration.ofMinutes(12)))
//          .withStartupTimeout(Duration.ofMinutes(12)); // necessary when building for the first time on the server
//      break;
//    default:
//      break;
//    }
//    this.type = type;
//    if (imageName.endsWith(":latest")) {
//      this.withImagePullPolicy(PullPolicy.alwaysPull());
//    }

    /*
     * docker exec -it <container started by testcontainers> sqlplus
     * user/password@//localhost:1521/ORCLPDB1 # can't find user docker run --name
     * db --shm-size=1g -d -p 1521:1521 -e ORACLE_PWD=password -v
     * "/c/Users/me/AppData/Local/Temp/ee19:/opt/oracle/oradata" my-image:latest
     */
//  }
  
  @Override
  public String getJdbcUrl() {
    if (jdbcUrl != null)
      return jdbcUrl;
    else
      return super.getJdbcUrl();
  }
  
  public void setJdbcUrl(String url) {
    jdbcUrl = url;
  }
  
  @Override
  public void start() {
    super.start();
    log.info("{} started at {}", getClass().getSimpleName(), getJdbcUrl());
  }

  @Override
  public GenericContainer<?> getContainer() {
    return this;
  }

  @Override
  public Connection getSysConnection() throws SQLException {
    return getConnection("SYS as SYSDBA", "password");
  }

  @Override
  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(getJdbcUrl(), getUsername(), getPassword());
  }

  @Override
  public Connection getConnection(String user, String password) throws SQLException {
    return DriverManager.getConnection(getJdbcUrl(), user, password);
  }

  /**
   * Drop and recreate the provided user by executing "schema-drop-create.xml".
   * This file must be found on the root classpath. If the user is different from
   * the one this container was originally created with,
   * "schema-drop-create-${user}.xml" is executed instead.
   *
   * @param user
   * @throws SQLException
   * @throws LiquibaseException
   */
  public void create(String user) throws SQLException, LiquibaseException {
    try (Connection connection = getSysConnection()) {
      String changeSet = user.equals(getUsername()) ? "schema-drop-create.xml" : String.format("schema-drop-create-%s.xml", user);
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (Liquibase liquibase = new Liquibase(changeSet, new ClassLoaderResourceAccessor(), database)) {
        Contexts contexts = new Contexts("xe");
        liquibase.update(contexts);
      }
    }
  }

  /**
   * Apply schema in a fresh database. After it is built, tag "0" is applied.
   *
   * @throws SQLException
   * @throws LiquibaseException
   */
  public void installVersioned(String changeLog) throws SQLException, LiquibaseException {
    create(getUsername()); // drop and recreate the user
    final Contexts contexts = new Contexts("xe", "dev");
    execute(liquibase -> liquibase.update(contexts), changeLog, getUsername(), getPassword());
    log.trace("applying tag");
    execute(liquibase -> liquibase.tag("0"), changeLog, getUsername(), getPassword());
  }

  public Type getType() {
    return type;
  }

  public static class Factory extends SpringTestDbContainer.Factory {
    public Factory() {
      super(OracleTestContainer.class);
    }
  }
  
//  @Order(Ordered.HIGHEST_PRECEDENCE)
//  public static class Factory implements ApplicationContextInitializer<ConfigurableApplicationContext> {
//    private static OracleTestContainer instance = null;
//
//    @Override
//    public void initialize(ConfigurableApplicationContext ctx) {
//      System.setProperty("java.io.tmpdir", "/tmp");
//      create(ctx);
//    }
//
//    private static void create(ConfigurableApplicationContext ctx) {
//      final Environment env = ctx.getEnvironment();
//      if (instance == null) {
//        instance = new OracleTestContainer(env);
//        if (env.getProperty("spring.datasource.url") == null) {
//          instance.start();
//          System.setProperty("spring.datasource.url", instance.getJdbcUrl());
//        } else {
//          instance.setJdbcUrl(env.getProperty("spring.datasource.url"));
//          log.info("MySqlTestContainer connecting to {}", instance.getJdbcUrl());
//        }
//      }
//
//      // Programmatically register the container as a bean so it can be injected
//      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
//      if (!beanFactory.containsBean(OracleTestContainer.class.getName())) {
//        beanFactory.registerSingleton(OracleTestContainer.class.getName(), instance);
//        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, "spring.datasource.url=" + instance.getJdbcUrl());
//      }
//    }
//
//    /**
//     * Destroy the container and recreate it under a new port. The container's bean
//     * in the provided ApplicationContext is refreshed. Use this only when necessary
//     * to clean up the DB after a test which has committed data.
//     *
//     * <p>
//     * WARNING: the provided context isn't refreshed, so other beans will still
//     * point to the old container. To refresh all beans in your context, add
//     * a @DirtiesContext annotation to your test class. I tried
//     * {@link ConfigurableApplicationContext#refresh} and
//     * {@link org.springframework.cloud.context.refresh.ContextRefresher#refresh}
//     * (see the second answer <a
//     *
//     * <p>href=
//     * "https://stackoverflow.com/questions/24720330/reload-or-refresh-a-spring-application-context-inside-a-test-method">here</a>),
//     * but @DirtiesContext is all that worked for me.
//     */
//    public static void destroy(ConfigurableApplicationContext ctx) {
//      instance.stop();
//      instance = null;
//      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
//      beanFactory.destroyBean(beanFactory.getBean(OracleTestContainer.class.getName()));
//    }
//
//    public static String restart(ConfigurableApplicationContext ctx) {
//      if (instance == null) {
//        throw new IllegalStateException("cannot restart an instance that was never started");
//      }
//      destroy(ctx);
//      Factory.create(ctx);
//      return instance.getJdbcUrl();
//    }
//  }
}
