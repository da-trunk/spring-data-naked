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

  @SuppressWarnings("resource")
  public OracleTestContainer(final @Nonnull Environment environment) {
    super(
        DockerImageName.parse(environment.getProperty("spring.datasource.container.image")).asCompatibleSubstituteFor("gvenzl/oracle-xe"));
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
          .waitingFor(new LogMessageWaitStrategy().withRegEx(".*DATABASE IS READY TO USE!.*\\s").withStartupTimeout(Duration.ofMinutes(20)))
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
  }

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
   * @throws Exception 
   */
  public void create(String user) throws Exception {
    try (Connection connection = getSysConnection()) {
      String changeSet = user.equals(getUsername()) ? "schema-drop-create.xml" : String.format("schema-drop-create-%s.xml", user);
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (ClassLoaderResourceAccessor clra = new ClassLoaderResourceAccessor();
          Liquibase liquibase = new Liquibase(changeSet, clra, database)) {
        Contexts contexts = new Contexts("xe");
        liquibase.update(contexts);
      }
    }
  }

  /**
   * Apply schema in a fresh database. After it is built, tag "0" is applied.
   * 
   * @throws Exception
   */
  public void installVersioned(String changeLog) throws Exception {
    create(getUsername()); // drop and recreate the user
    final Contexts contexts = new Contexts("xe", "dev");
    update(changeLog, contexts);
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
}
