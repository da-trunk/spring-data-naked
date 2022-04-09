package org.datrunk.naked.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.datest.naked.test.entities.User;
import org.datrunk.naked.client.config.Config;
import org.datrunk.naked.server.tomcat.ContainerManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = { Config.class })
@ExtendWith({ SpringExtension.class })
// @TestMethodOrder(OrderAnnotation.class)
// @DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
public class ClientTest extends ContainerManager {
    /**
     * Reset the DB before running the next test class.
     */
    // @AfterAll
    // public static void resetContainers() {
    // ContainerManager.resetContainers();
    // }

    @Autowired
    private RestTemplate restTemplate;

    @BeforeEach
    public void before() {
        assertThat(restTemplate).isNotNull();
    }

    @Autowired
    RepoClient.Factory repoClientFactory;

    @Test
    public void testCreate() {
        repoClientFactory.create(User.class, Long.class);
    }
}
