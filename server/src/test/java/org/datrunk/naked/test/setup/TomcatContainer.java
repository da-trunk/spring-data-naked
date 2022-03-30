package org.datrunk.naked.test.setup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datrunk.naked.test.db.oracle.OracleTestContainer;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nonnull;
import java.net.URI;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Starts a server implementation within a docker container hosting Tomcat.
 *
 */
public class TomcatContainer extends GenericContainer<TomcatContainer> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final UriComponents baseUri;
    private final Path warPath;

    public TomcatContainer(@Nonnull String imageName, @Nonnull Path warPath, @Nonnull OracleTestContainer oracle) {
        this(imageName, warPath, Network.newNetwork(), oracle, container -> {});
    }

    public TomcatContainer(@Nonnull String imageName, @Nonnull Path warPath, Network network, @Nonnull OracleTestContainer oracle,
        Consumer<TomcatContainer> extraConfig) {
        super(DockerImageName.parse(imageName));
        DockerImageName.parse(imageName)
            .assertValid();
        this.withSharedMemorySize(org.apache.commons.io.FileUtils.ONE_GB)
            .withExposedPorts(9080)
            .withReuse(false)
            .withNetwork(network)
            .withNetworkAliases("test-api")
            .withEnv("URL", oracle.getJdbcUrl())
            .withFileSystemBind(warPath.toAbsolutePath()
                .toString(), "/usr/local/tomcat/webapps/service.war")
            .waitingFor(Wait.forLogMessage(
                ".*\\[main\\] org\\.apache\\.catalina\\.startup\\.Catalina\\.start Server startup in \\[\\d+\\] milliseconds\\n", 1));
        extraConfig.accept(this);
        if (imageName.endsWith(":latest")) {
            this.withImagePullPolicy(PullPolicy.alwaysPull());
        }
        start();
        int port = getMappedPort(9080) + 1; // TODO: The mapped port is always one less than it should be. That is why Wait.forHttp(...)
                                            // fails. Something is wrong here ...
        baseUri = UriComponentsBuilder.newInstance()
            .scheme("http")
            .host(getHost())
            .port(port)
            .path("")
            .build();
        LOGGER.info("Server started at [{}]", baseUri.toString());

        this.warPath = warPath;
    }

    public Path getWarPath() {
        return warPath;
    }

    public URI getBaseUrl() {
        return baseUri.toUri();
    }
}
