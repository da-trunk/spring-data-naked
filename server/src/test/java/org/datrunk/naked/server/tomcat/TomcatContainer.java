package org.datrunk.naked.server.tomcat;

import java.net.URI;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datrunk.naked.db.oracle.OracleTestContainer;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a server implementation within a docker container hosting Tomcat.
 *
 */
public class TomcatContainer extends GenericContainer<TomcatContainer> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final UriComponents baseUri;

    public TomcatContainer(@Nonnull String imageName, @Nonnull OracleTestContainer oracle) {
        this(imageName, Network.newNetwork(), oracle, container -> {});
    }

    public TomcatContainer(@Nonnull String imageName, Network network, @Nonnull OracleTestContainer oracle,
        Consumer<TomcatContainer> extraConfig) {
        super(DockerImageName.parse(imageName));
        DockerImageName.parse(imageName)
            .assertValid();
        this.withSharedMemorySize(org.apache.commons.io.FileUtils.ONE_GB)
            .withExposedPorts(8080)
            .withReuse(false)
            .withNetwork(network)
            .withNetworkAliases("test-api")
            .withEnv("URL", oracle.getJdbcUrl())
            .waitingFor(Wait.forLogMessage(
                ".*\\[main\\] org\\.apache\\.catalina\\.startup\\.Catalina\\.start Server startup in \\[\\d+\\] milliseconds\\n", 1));
        extraConfig.accept(this);
        if (imageName.endsWith(":latest")) {
            this.withImagePullPolicy(PullPolicy.alwaysPull());
        }
        start();
        int port = getMappedPort(8080);
        baseUri = UriComponentsBuilder.newInstance()
            .scheme("http")
            .host(getHost())
            .port(port)
            .path("")
            .build();
        LOGGER.info("Server started at [{}]", baseUri.toString());
    }

    public URI getBaseUrl() {
        return baseUri.toUri();
    }
}
