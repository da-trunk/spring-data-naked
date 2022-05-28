package org.datrunk.naked.test.mysql;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.datrunk.naked.db.jdbc.DataSourceWrapper;
import org.datrunk.naked.db.mysql.MySqlTestContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith({SpringExtension.class})
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(
    initializers = {MySqlTestContainer.Factory.class},
    classes = {MySqlIntegrationTest.Config.class})
@EnableConfigurationProperties(DataSourceProperties.class)
@ActiveProfiles("test")
class MySqlIntegrationTest {
  private static boolean USE_TESTCONTAINERS = true;
  // @ActiveProfiles("test-fixed")
  // class IntegrationTest {
  // private static boolean USE_TESTCONTAINERS = false;

  @EnableAutoConfiguration
  public static class Config {
    boolean initialized = false;

    @Bean
    public DataSource dataSource(MySqlTestContainer db) throws LiquibaseException, SQLException {
      if (!initialized) {
        db.updateAsSys("liquibase/mysql/init.xml");
        db.update("liquibase/mysql/schema-update-versioned.xml");
        db.update("liquibase/mysql/content/master.xml");
        initialized = true;
      }
      return db.getDataSource();
    }
  }

  @Autowired private MySqlTestContainer mySql;
  private static List<Record> records;

  @BeforeAll
  void before() throws Exception {
    assertThat(mySql).isNotNull();
    if (USE_TESTCONTAINERS) {
      DataSourceWrapper db = mySql.getDataSourceWrapper();
      assertThat(db).isNotNull();
    }
  }

  static Stream<Record> getTestRecords() throws Exception {
    assertThat(records).isNotEmpty();
    return records.stream();
  }

  @Data
  @RequiredArgsConstructor
  private static class MyRow {
    private final String codeSystem;
    private final String value;
    private final String displayType;
    private final String primaryName;
    private final String altName;

    public MyRow(final ResultSet row) throws SQLException {
      codeSystem = row.getString("code_system_id");
      value = row.getString("code_system_value");
      displayType = row.getString("display_ype");
      primaryName = row.getString("primary_name");
      altName = row.getString("alt_name");
    }
  }

  @ParameterizedTest(name = "Given [{0}]")
  @MethodSource("getTestRecords")
  void testCompleted(Record record) throws Exception {
    DataSourceWrapper db = mySql.getDataSourceWrapper();
    assertThat(db).isNotNull();
  }

  @Data
  @RequiredArgsConstructor
  private static class Record {
    private final int id;
    private final String key;
    private final int count;

    public Record(ResultSet row) throws SQLException {
      id = row.getInt("id");
      key = row.getString("key");
      count = row.getInt("count");
    }
  }
  ;

  private List<Record> getRecordsWithCount(int id) throws Exception {
    return mySql
        .getDataSourceWrapper()
        .executeQuery(
            "select key, count(*) from records where id=? group by key order by key",
            stmt -> stmt.setInt(1, id),
            Record::new);
  }
}
