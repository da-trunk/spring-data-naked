package org.datrunk.naked.db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.annotation.Nonnull;
import liquibase.exception.LiquibaseException;
import lombok.extern.log4j.Log4j2;
import org.datrunk.naked.db.SpringTestDbContainer;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Spring integration for {@link MySQLContainer}.
 *
 * @author BA030483
 */
@Log4j2
public class MySqlTestContainer extends MySQLContainer<MySqlTestContainer>
    implements SpringTestDbContainer {
  private String jdbcUrl;
  private String liquibaseInitializationScript = null;

  public MySqlTestContainer(final @Nonnull Environment environment) {
    super(
        DockerImageName.parse(environment.getProperty("spring.datasource.container.image"))
            .asCompatibleSubstituteFor("mysql"));
    init(this, environment, this::addFixedExposedPort);
    withCopyFileToContainer(
        MountableFile.forHostPath(environment.getProperty("spring.datasource.container.config")),
        "/etc/mysql/conf.d/myconf.cnf");
    liquibaseInitializationScript =
        environment.getProperty("spring.datasource.container.liquibase.changeLog");
  }

  @Override
  public String getJdbcUrl() {
    if (jdbcUrl != null) {
      return jdbcUrl;
    } else {
      return super.getJdbcUrl();
    }
  }

  @Override
  public void setJdbcUrl(String url) {
    jdbcUrl = url;
  }

  @Override
  public void start() {
    super.start();
    log.info("{} started at {}", getClass().getSimpleName(), getJdbcUrl());
    try {
      if (liquibaseInitializationScript != null) {
        log.trace("executing {}", liquibaseInitializationScript);
        updateAsSys(liquibaseInitializationScript);
      }
    } catch (LiquibaseException | SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public MySqlTestContainer getContainer() {
    return this;
  }

  public static class Factory extends SpringTestDbContainer.Factory {
    public Factory() {
      super(MySqlTestContainer.class);
    }
  }

  @Override
  public Connection getSysConnection() throws SQLException {
    final String url = getJdbcUrl();
    final String sysUrl = url.substring(0, url.lastIndexOf("/") + 1) + "sys";
    return DriverManager.getConnection(sysUrl, "root", getPassword());
  }
}
