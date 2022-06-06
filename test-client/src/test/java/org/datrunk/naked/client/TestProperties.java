package org.datrunk.naked.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.datrunk.naked.test.config.EmptyConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = EmptyConfig.class, webEnvironment = WebEnvironment.NONE)
@ExtendWith({ SpringExtension.class })
@ActiveProfiles("protected")
@EnableConfigurationProperties(RepoClient.Properties.class)
public class TestProperties {
    @Autowired
    private ClientProperties clientProperties;

    @Value("${test.containers.db.datasource.username}")
    private String userName;

    @Value("${test.containers.db.application.build-dir}")
    private String buildDir;

    @Test
    public void testClientProperties() {
        assertThat(clientProperties).isNotNull();
        assertThat(clientProperties.getRetrySleepDurations()).containsExactly("0");
    }

    @Test
    public void testProjectProperties() {
        assertThat(userName).isNotNull();
        assertThat(buildDir).isNotNull();
    }
}
