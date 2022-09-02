package org.datrunk.naked.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.datrunk.naked.client.config.Config;
import org.datrunk.naked.entities.User;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackageClasses = {RepoClient.Factory.class})
@Import({Config.class})
public class ConfigTest {
  private final RepoClient<User, Integer> jobClient;

  public ConfigTest(RepoClient.Factory repoClientFactory) {
    jobClient = repoClientFactory.create(User.class, Integer.class);
    assertThat(jobClient).isNotNull();
  }
}
