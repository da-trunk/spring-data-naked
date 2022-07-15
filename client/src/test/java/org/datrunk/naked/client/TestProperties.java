package org.datrunk.naked.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.datrunk.naked.client.RepoClient.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = TestProperties.EmptyConfig.class, webEnvironment = WebEnvironment.NONE)
@ExtendWith({ SpringExtension.class })
@ActiveProfiles("test")
@EnableConfigurationProperties(RepoClient.Properties.class)
public class TestProperties {
  @Configuration
  static class EmptyConfig {
  }

  @Autowired
  private Properties clientProperties;

  @Test
  public void testClientProperties() {
    assertThat(clientProperties).isNotNull();
    assertThat(clientProperties.getRetrySleepDurations()).containsExactly("0");
  }
}
