package org.datrunk.naked.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.datrunk.naked.test.setup.ContainerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;

import uk.co.blackpepper.bowman.ClientFactory;

@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = { ClientTest.Config.class })
@ExtendWith({ SpringExtension.class })
//@TestMethodOrder(OrderAnnotation.class)
//@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
public class ClientTest extends ContainerManager {
//@ActiveProfiles("test-9080")
//public class ClientTest {
    /**
     * Reset the DB before running the next test class.
     */
//    @AfterAll
//    public static void resetContainers() {
//        ContainerManager.resetContainers();
//    }

    @Autowired
    private RestTemplate restTemplate;

    @EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
    @ComponentScan(basePackageClasses = { RepoClient.Factory.class })
    @Import({ Config.class })
    public static class Config {}

    @BeforeEach
    public void before() {
        assertThat(restTemplate).isNotNull();
    }

    @Autowired
    ClientFactory clientFactory;

    @Autowired
    RepoClient.Factory repoClientFactory;

    @Test
    public void testCreate() throws JsonProcessingException {
    }
}
