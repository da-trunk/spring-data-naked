package org.datrunk.naked.db.postgresql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Nonnull;

import org.datrunk.naked.db.SpringTestDbContainer;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.PostgreSQLContainer;

import lombok.extern.log4j.Log4j2;

/**
 * Spring integration for {@link PostgresTestContainer}.
 *
 * @author BA030483
 */
@Log4j2
public class PostgresTestContainer extends PostgreSQLContainer<PostgresTestContainer> implements SpringTestDbContainer {
  private String jdbcUrl;

  @SuppressWarnings("resource")
  public PostgresTestContainer(final @Nonnull Environment environment) {
    super(environment.getProperty("spring.datasource.container.image"));
    init(this, environment, this::addFixedExposedPort);
    withEnv("POSTGRES_HOST_AUTH_METHOD", "trust");
//    withStartupTimeout(Duration.ofMinutes(5));
//    waitingFor(new LogMessageWaitStrategy().withRegEx(".*database system is ready to accept connections.*\\s"));
  }

  @Override
  public String getJdbcUrl() {
    if (jdbcUrl != null)
      return jdbcUrl;
    else
      return super.getJdbcUrl();
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
      execInContainer("apt-get update && apt-get install -y procps vim");
    } catch (UnsupportedOperationException | IOException | InterruptedException e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void configure() {
    super.configure();
  }

  @Override
  public PostgresTestContainer getContainer() {
    return this;
  }

  public static class Factory extends SpringTestDbContainer.Factory {
    public Factory() {
      super(PostgresTestContainer.class);
    }
  }

  @Override
  public Connection getSysConnection() throws SQLException {
    return getConnection("root", "password");
  }
}
