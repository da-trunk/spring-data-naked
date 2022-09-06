package org.datrunk.naked.db.oracle;

import java.sql.SQLException;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Import({Application.Config.class})
@EnableAutoConfiguration
@EnableConfigurationProperties(DataSourceProperties.class)
public class Application {
  static class Config {
    @Bean
    DataSource dataSource(OracleTestContainer db) throws LiquibaseException, SQLException {
      return db.getDataSource();
    }
  }

  @Autowired private JdbcTemplate jdbcTemplate;

  @SuppressWarnings("resource")
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(Application.class);
    app.addInitializers(new OracleTestContainer.Factory());
    app.run(args);
  }

  @PostConstruct
  private void initDb() {}
}
