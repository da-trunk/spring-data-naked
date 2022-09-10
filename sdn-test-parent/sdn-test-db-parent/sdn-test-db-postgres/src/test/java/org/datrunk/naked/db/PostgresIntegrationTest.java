package org.datrunk.naked.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import liquibase.exception.LiquibaseException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.datrunk.naked.db.jdbc.DataSourceWrapper;
import org.datrunk.naked.db.postgresql.PostgresTestContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith({SpringExtension.class})
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(
    initializers = {PostgresTestContainer.Factory.class},
    classes = {PostgresIntegrationTest.Config.class})
@EnableConfigurationProperties(DataSourceProperties.class)
@ActiveProfiles("test")
@Disabled("connection refused")
class PostgresIntegrationTest {
  @Configuration
  @EnableAutoConfiguration
  @EnableTransactionManagement
  public static class Config {
    boolean initialized = false;

    @Bean
    DataSource dataSource(PostgresTestContainer db) throws LiquibaseException, SQLException {
      if (!initialized) {
        initialized = true;
      }
      return db.getDataSource();
    }
  }

  private DataSourceWrapper db;

  @Autowired
  void setDataSource(PostgresTestContainer mySql) {
    db = mySql.getDataSourceWrapper();
  }

  @BeforeAll
  void before() throws Exception {
    assertThat(db).isNotNull();
  }

  @Transactional
  @Test
  void getYValues() throws Exception {
    db.executeUpdate("create table if not exists points (x int, y int)");
    db.executeUpdate("insert into points values (1,1)");
    db.executeUpdate("insert into points values (2,4)");
    List<Point> actual = Point.findByX(db, 1);
    assertThat(actual).containsOnly(new Point(1, 1));
  }

  @Data
  @RequiredArgsConstructor
  private static class Point {
    private final Integer x;
    private final Integer y;

    public Point(final ResultSet row) throws SQLException {
      x = row.getInt("x");
      y = row.getInt("y");
    }

    public static List<Point> findByX(DataSourceWrapper db, int x) throws Exception {
      return db.executeQuery(
          "select x,y from points where x = ?", stmt -> stmt.setInt(1, x), Point::new);
    }
  }
}
