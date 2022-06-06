package org.datrunk.naked.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.datest.naked.test.entities.User;
import org.datrunk.naked.client.container.TomcatTestContainer;
import org.datrunk.naked.db.jdbc.DataSourceWrapper;
import org.datrunk.naked.db.mysql.MySqlTestContainer;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import liquibase.exception.LiquibaseException;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith({ SpringExtension.class })
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = { MySqlTestContainer.Factory.class, TomcatTestContainer.Factory.class }, classes = {
    ClientTest.Config.class })
@EnableConfigurationProperties(DataSourceProperties.class)
@ActiveProfiles("test")
// @TestMethodOrder(OrderAnnotation.class)
// @DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ClientTest {
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

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private TomcatTestContainer server;

  @Autowired
  private MySqlTestContainer mySql;

  @BeforeEach
  void before() throws Exception {
    assertThat(mySql).isNotNull();
    DataSourceWrapper db = mySql.getDataSourceWrapper();
    assertThat(db).isNotNull();
  }

  @Autowired
  RepoClient.Factory repoClientFactory;

  @Test
  public void testCreate() {
    repoClientFactory.create(User.class, Long.class);
  }
}
