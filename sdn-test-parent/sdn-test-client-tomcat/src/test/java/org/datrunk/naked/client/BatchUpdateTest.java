package org.datrunk.naked.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import lombok.extern.log4j.Log4j2;
import org.datrunk.naked.client.container.TomcatTestContainer;
import org.datrunk.naked.db.mysql.MySqlTestContainer;
import org.datrunk.naked.entities.User;
import org.datrunk.naked.entities.random.Randomizer;
import org.datrunk.naked.entities.random.Randomizer.Exception;
import org.datrunk.naked.entities.random.UserRandomizer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith({SpringExtension.class})
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(
    initializers = {MySqlTestContainer.Factory.class, TomcatTestContainer.Factory.class},
    classes = {BatchUpdateTest.Config.class, RepoClient.Factory.class})
@EnableConfigurationProperties({DataSourceProperties.class, RepoClient.Properties.class})
@ActiveProfiles("test")
@Log4j2
public class BatchUpdateTest {
  @EnableAutoConfiguration
  public static class Config {
    boolean initialized = false;

    @Bean
    public DataSource dataSource(MySqlTestContainer db) throws LiquibaseException, SQLException {
      if (!initialized) {
        initialized = true;
      }
      return db.getDataSource();
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource ds) {
      return new NamedParameterJdbcTemplate(ds);
    }

    @Bean
    public CEClient.Factory getClientFactory(ClientProperties properties) {
      return new CEClient.Factory(properties);
    }

    @Bean
    public RestTemplate getRestTemplate(
        TomcatTestContainer server, RestTemplateBuilder restTemplateBuilder) {
      RestTemplate restTemplate =
          restTemplateBuilder
              .rootUri(server.getBaseUri().toASCIIString())
              .setConnectTimeout(Duration.ofSeconds(2))
              .setReadTimeout(Duration.ofSeconds(2))
              .build();
      return restTemplate;
    }
  }

  @Autowired RepoClient.Factory repoClientFactory;

  RepoClient<User, Integer> client;
  UserRandomizer randomizer;

  @BeforeAll
  void beforeAll() {
    client = repoClientFactory.create(User.class, Integer.class);
    assertThat(client).isNotNull();
    randomizer = new UserRandomizer(100);
  }

  @BeforeEach
  void before() {
    for (User user : client.getAll()) {
      client.delete(user);
    }
    List<User> users = client.getAll();
    assertThat(users).isEmpty();
    client.clear();
  }

  @Test
  void testSave() throws Exception {
    User expected = randomizer.getRandomValue();
    assertThat(expected.getId()).isNull();
    User saved = client.save(expected);
    assertThat(expected.getId()).isNotNull();
    assertThat(saved).isEqualTo(expected);
    User actual = client.get(saved.getId());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void testPersist() throws Exception {
    client.setMaxSize(2);
    List<User> expected =
        ImmutableList.of(randomizer.getRandomValue(), randomizer.getRandomValue());
    log.info("expected = [{}]", expected);
    List<User> persisted = client.persist(expected.get(0));
    assertThat(persisted).isEmpty();
    persisted = client.persist(expected.get(1));
    log.info("persisted = [{}]", persisted);
    assertThat(persisted).hasSize(2);
    assertThat(persisted.stream().map(User::getId)).allSatisfy(id -> assertThat(id).isNotNull());
    assertThat(persisted).containsExactlyInAnyOrderElementsOf(expected);
    List<User> actual = client.getAll();
    log.info("actual = [{}]", actual);
    assertThat(actual).hasSize(2);
  }

  @Disabled("redundant")
  @Test
  void testCreate() throws Randomizer.Exception {
    client.setMaxSize(10);
    final List<User> expected = randomizer.getAll();
    int numPersisted = 0;
    for (User user : expected) {
      List<User> persisted = client.persist(user);
      numPersisted++;
      if (numPersisted % 10 == 0) {
        assertThat(persisted.stream().map(User::getId)).allSatisfy(u -> assertThat(u).isNotNull());
      }
    }
    client.flush();
    List<User> actual = client.getAll();
    assertThat(actual).hasSize(expected.size());
  }
}
