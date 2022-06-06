package org.datrunk.naked.db.oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datrunk.naked.db.SpringTestDbContainer;
import org.datrunk.naked.test.container.SpringTestContainers;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.PullPolicy;
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
 * Extends {@link OracleContainer} with properties and methods specific to our Oracle database
 * projects.
 *
 * @author BA030483
 */
public class OracleTestContainer extends OracleContainer implements SpringTestDbContainer {
  private static Logger log = LogManager.getLogger();
  private final Type type;

  public static enum Type {
    xe11,
    xe18,
    ee19,
    xe21;
  }

  public OracleTestContainer(final @Nonnull Environment environment) {
    this(
        environment.getProperty("spring.datasource.container.image"),
        Type.valueOf(environment.getProperty("spring.datasource.container.type")),
        environment.getProperty("spring.datasource.username"),
        environment.getProperty("spring.datasource.password"));
  }

  public OracleTestContainer(final OracleTestContainer other) {
    this(other.getDockerImageName(), other.getType(), other.getUsername(), other.getPassword());
  }

  public OracleTestContainer(
      @Nonnull String imageName, Type type, @Nonnull String userName, @Nonnull String password) {
    super(DockerImageName.parse(imageName).asCompatibleSubstituteFor("gvenzl/oracle-xe"));
    withNetwork(SpringTestContainers.getNetwork());
    withNetworkAliases("code-mapping-database");
    withExposedPorts(1521);
    withAccessToHost(true);
    withUsername(userName);
    withPassword(password);
    withEnv("ORACLE_PWD", "password");

    final String tmpDir = getHostTempDir(type.name()).toAbsolutePath().toString();
    switch (type) {
      case xe11:
        withSharedMemorySize(FileUtils.ONE_GB)
            .withEnv("ORACLE_SID", "oracle")
            .withEnv("ORACLE_PDB", "oracle")
            .withDatabaseName("XE")
            .waitingFor(new LogMessageWaitStrategy().withRegEx("DATABASE IS READY TO USE!\\n"));
        break;
      case xe18:
      case xe21:
        this.withEnv("ORACLE_PDB", "XEPDB1")
            .withFileSystemBind(tmpDir, "/opt/oracle/oradata", BindMode.READ_WRITE)
            .waitingFor(
                new LogMessageWaitStrategy()
                    .withRegEx("Completed: ALTER DATABASE OPEN\\n")
                    .withStartupTimeout(Duration.ofMinutes(12)))
            .withStartupTimeout(
                Duration.ofMinutes(12)); // necessary when building for the first time on the server
        break;
      case ee19:
        withSharedMemorySize(FileUtils.ONE_GB)
            .withEnv("ORACLE_PDB", "ORCLPDB1")
            .withDatabaseName("ORCLPDB1")
            .withFileSystemBind(tmpDir, "/opt/oracle/oradata", BindMode.READ_WRITE)
            .waitingFor(
                new LogMessageWaitStrategy()
                    .withRegEx("DATABASE IS READY TO USE!\\n")
                    .withStartupTimeout(Duration.ofMinutes(12)))
            .withStartupTimeout(
                Duration.ofMinutes(12)); // necessary when building for the first time on the server
        break;
      default:
        break;
    }
    if (imageName.endsWith(":latest")) {
      this.withImagePullPolicy(PullPolicy.alwaysPull());
    }
    configure(this);

    this.type = type;
    start();
    log.info("code-mapping-database started at {}", getJdbcUrl());

    /*
     * docker exec -it <container started by testcontainers> sqlplus cm_test/password@//localhost:1521/ORCLPDB1 # can't find user docker
     * run --name code-mapping-database2 --shm-size=1g -d -p 1521:1521 -e ORACLE_PWD=password -v
     * "/c/Users/ba030483/AppData/Local/Temp/ee19:/opt/oracle/oradata"
     * docker-snapshot.cernerrepos.net/si-mapping/code-mapping-database:latest # can't find user
     */
  }

  protected void configure(OracleTestContainer oracleTestContainer) {
    // do nothing
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
   * Drop and recreate the provided user by executing "schema-drop-create.xml". This file must be
   * found on the root classpath. If the user is different from the one this container was
   * originally created with, "schema-drop-create-${user}.xml" is executed instead.
   *
   * @param user
   * @throws SQLException
   * @throws LiquibaseException
   */
  public void create(String user) throws SQLException, LiquibaseException {
    try (Connection connection = getSysConnection()) {
      String changeSet =
          user.equals(getUsername())
              ? "schema-drop-create.xml"
              : String.format("schema-drop-create-%s.xml", user);
      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (Liquibase liquibase =
          new Liquibase(changeSet, new ClassLoaderResourceAccessor(), database)) {
        Contexts contexts = new Contexts("xe");
        liquibase.update(contexts);
      }
    }
  }

  /**
   * Build a fresh code-mapping-database. After it is built, tag "0" is applied. This mimics the
   * process we used when creating the docker image that we use in tests.
   *
   * @throws SQLException
   * @throws LiquibaseException
   */
  public void installCodeMappingVersioned() throws SQLException, LiquibaseException {
    create(getUsername()); // drop and recreate the user
    final Contexts contexts = new Contexts("xe", "dev");
    execute(
        liquibase -> liquibase.update(contexts), "v000/master.xml", getUsername(), getPassword());
    update("schema-update-versioned.xml");
    log.trace("applying tag");
    execute(
        liquibase -> liquibase.tag("0"),
        "schema-update-versioned.xml",
        getUsername(),
        getPassword());
  }

  public Type getType() {
    return type;
  }
}
