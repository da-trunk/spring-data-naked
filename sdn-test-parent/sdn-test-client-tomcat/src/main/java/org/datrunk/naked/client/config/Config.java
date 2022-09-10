package org.datrunk.naked.client.config;

import java.time.Duration;
import org.datrunk.naked.client.CEClient;
import org.datrunk.naked.client.ClientProperties;
import org.datrunk.naked.client.RepoClient;
import org.datrunk.naked.client.error.FunctionalClientErrorHandler;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import(FunctionalClientErrorHandler.class)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackageClasses = {RepoClient.Factory.class})
@EnableConfigurationProperties({RepoClient.Properties.class})
public class Config {
  @Bean
  public RestTemplate getRestTemplate(
      RestTemplateBuilder restTemplateBuilder, ClientProperties properties) {
    RestTemplate restTemplate =
        restTemplateBuilder
            .rootUri(properties.getLocation())
            .setConnectTimeout(Duration.ofSeconds(2))
            .setReadTimeout(Duration.ofSeconds(2))
            .build();
    return restTemplate;
  }

  @Bean
  public CEClient.Factory getClientFactory(ClientProperties properties) {
    return new CEClient.Factory(properties);
  }
}
