package org.datrunk.naked.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.time.Duration;

import javax.sql.DataSource;

import org.datrunk.naked.client.container.TomcatTestContainer;
import org.datrunk.naked.db.jdbc.DataSourceWrapper;
import org.datrunk.naked.db.mysql.MySqlTestContainer;
import org.datrunk.naked.entities.User;
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
import org.springframework.boot.web.client.RestTemplateBuilder;
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
    ClientTest.Config.class, RepoClient.Factory.class })
@EnableConfigurationProperties({ DataSourceProperties.class, RepoClient.Properties.class })
@ActiveProfiles("test")
// @TestMethodOrder(OrderAnnotation.class)
// @DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ClientTest {
  @EnableAutoConfiguration
  public static class Config {
    boolean initialized = false;

    @Bean
    DataSource dataSource(MySqlTestContainer db) throws LiquibaseException, SQLException {
      if (!initialized) {
//        db.updateAsSys("liquibase/mysql/init.xml");
//        db.update("liquibase/mysql/schema-update-versioned.xml");
//        db.update("liquibase/mysql/content/master.xml");
        initialized = true;
      }
      return db.getDataSource();
    }

    @Bean
    CEClient.Factory getClientFactory(ClientProperties properties) {
      return new CEClient.Factory(properties);
    }

    @Bean
    RestTemplate getRestTemplate(TomcatTestContainer server, RestTemplateBuilder restTemplateBuilder) {
      RestTemplate restTemplate = restTemplateBuilder.rootUri(server.getBaseUri().toASCIIString())
          .setConnectTimeout(Duration.ofSeconds(2))
          .setReadTimeout(Duration.ofSeconds(2))
          .build();
      return restTemplate;
    }
  }

  @Autowired
  private RestTemplate restTemplate;

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
    repoClientFactory.create(User.class, Integer.class);
  }
}
