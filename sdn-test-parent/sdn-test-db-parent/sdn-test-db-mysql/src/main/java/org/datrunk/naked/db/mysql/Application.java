// package org.datrunk.naked.db.mysql;
//
// import java.sql.SQLException;
// import javax.annotation.PostConstruct;
// import javax.sql.DataSource;
// import liquibase.exception.LiquibaseException;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.context.annotation.Bean;
// import org.springframework.jdbc.core.JdbcTemplate;
//
// @SpringBootApplication
//// @EnableConfigurationProperties(DataSourceProperties.class)
// public class Application {
//  static class Config {
//    @Bean
//    DataSource dataSource(MySqlTestContainer db) throws LiquibaseException, SQLException {
//      return db.getDataSource();
//    }
//  }
//
//  @Autowired private JdbcTemplate jdbcTemplate;
//
//  @SuppressWarnings("resource")
//  public static void main(String[] args) {
//    SpringApplication app = new SpringApplication(Application.class);
//    app.addInitializers(new MySqlTestContainer.Factory());
//    app.run(args);
//  }
//
//  @PostConstruct
//  private void initDb() {}
// }
