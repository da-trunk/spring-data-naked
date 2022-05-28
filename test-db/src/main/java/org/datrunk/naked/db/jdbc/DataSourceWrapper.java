package org.datrunk.naked.db.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.testcontainers.utility.ThrowingFunction;

public class DataSourceWrapper {
  protected static final Logger log = LogManager.getLogger();

  private String url;
  private String userName;
  private String password;

  public DataSourceWrapper(DataSourceProperties properties) {
    this.url = properties.getUrl();
    this.userName = properties.getUsername();
    this.password = properties.getPassword();
  }

  public DataSourceWrapper(String userName, String password, String url) {
    this.url = url;
    this.userName = userName;
    this.password = password;
  }

  private String getUsername() {
    return userName;
  }

  private String getPassword() {
    return password;
  }

  public String getJdbcUrl() {
    return url;
  }

  public Connection getConnection() throws SQLException {
    return getConnection(getUsername(), getPassword());
  }

  public Connection getConnection(String user, String password) throws SQLException {
    Connection connection = DriverManager.getConnection(getJdbcUrl(), user, password);
    assert (connection != null);
    return connection;
  }

  public Connection getSysConnection() throws SQLException {
    return getConnection("SYS as SYSDBA", "password");
  }

  public int getInt(String sql, Object param) throws Exception {
    List<Integer> results =
        executeQuery(sql, stmt -> stmt.setObject(1, param), row -> row.getInt(1));
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).isNotNull();
    return results.get(0).intValue();
  }

  public String getString(String sql, Object param) throws Exception {
    List<String> results =
        executeQuery(sql, stmt -> stmt.setObject(1, param), row -> row.getString(1));
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).isNotNull();
    return results.get(0);
  }

  public <T> List<T> executeQuery(String sql, ThrowingFunction<ResultSet, T> rowMapper)
      throws Exception {
    return executeQuery(getUsername(), getPassword(), sql, stmt -> {}, rowMapper);
  }

  public <T> List<T> executeQuery(
      final String sql,
      ThrowingConsumer<PreparedStatement> initPreparedStatement,
      ThrowingFunction<ResultSet, T> rowMapper)
      throws Exception {
    return executeQuery(getUsername(), getPassword(), sql, initPreparedStatement, rowMapper);
  }

  public <T> List<T> executeQuery(
      String user, String password, String sql, ThrowingFunction<ResultSet, T> rowMapper)
      throws Exception {
    return executeQuery(user, password, sql, stmt -> {}, rowMapper);
  }

  public <T> List<T> executeQuery(
      String user,
      String password,
      final String sql,
      ThrowingConsumer<PreparedStatement> initPreparedStatement,
      ThrowingFunction<ResultSet, T> rowMapper)
      throws Exception {
    List<T> result = new ArrayList<>();
    log.trace(sql);
    try (Connection connection = getConnection(user, password);
        PreparedStatement statement = connection.prepareStatement(sql)) {
      initPreparedStatement.accept(statement);
      log.trace(statement);
      statement.executeQuery();
      try (ResultSet rs = statement.getResultSet()) {
        while (rs.next()) {
          result.add(rowMapper.apply(rs));
        }
      }
    }
    return result;
  }

  public int executeCount(String sql) throws Exception {
    int result = 0;
    log.trace(sql);
    try (Connection connection = getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(sql);
      try (ResultSet rs = statement.getResultSet()) {
        while (rs.next()) {
          result = rs.getInt(1);
        }
      }
    }
    return result;
  }

  public void consumeQuery(
      String user, String password, String sql, ThrowingConsumer<ResultSet> consumer)
      throws Throwable {
    log.trace(sql);
    try (Connection connection = getConnection(user, password);
        Statement statement = connection.createStatement()) {
      statement.execute(sql);
      try (ResultSet rs = statement.getResultSet()) {
        while (rs.next()) {
          consumer.accept(rs);
        }
      }
    }
  }
}
