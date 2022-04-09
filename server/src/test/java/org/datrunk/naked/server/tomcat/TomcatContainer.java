package org.datrunk.naked.server.tomcat;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datrunk.naked.db.oracle.OracleTestContainer;
import org.springframework.core.env.Environment;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Starts a server implementation within a docker container hosting Tomcat.
 *
 */
public class TomcatContainer extends GenericContainer<TomcatContainer> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final UriComponents baseUri;

    public TomcatContainer(@Nonnull String imageName, final Environment environment, @Nonnull OracleTestContainer oracle) {
        this(imageName, Network.newNetwork(), environment, oracle, container -> {});
    }

    public TomcatContainer(@Nonnull String imageName, Network network, final Environment environment,
        @Nonnull JdbcDatabaseContainer<? extends JdbcDatabaseContainer<?>> db, Consumer<TomcatContainer> extraConfig) {
        super(DockerImageName.parse(imageName));
        DockerImageName.parse(imageName)
            .assertValid();
        @Nonnull
        String baseDir = environment.getProperty("application.buildDir");
        assertThat(baseDir).isNotEmpty();
        assertThat(baseDir).doesNotStartWith("@");
        if (baseDir.startsWith("/c/")) // this messes up Windows
            baseDir = baseDir.substring(2);
        @Nonnull
        String war = environment.getProperty("application.warName");
        assertThat(war).isNotEmpty();
        assertThat(war).doesNotStartWith("@");
        this.withSharedMemorySize(org.apache.commons.io.FileUtils.ONE_GB)
            .withExposedPorts(8080)
            .withReuse(false)
            .withNetwork(network)
            .withNetworkAliases("test-api")
            .withCopyFileToContainer(MountableFile.forHostPath(String.format("%s/%s", baseDir, war)), "/usr/local/tomcat/webapps/ROOT.war")
            .withCopyFileToContainer(MountableFile.forHostPath(String.format("%s/test-classes/conf/server.xml", baseDir)),
                "/usr/local/tomcat/conf/server.xml")
            .withCopyFileToContainer(MountableFile.forHostPath(String.format("%s/test-classes/conf/Catalina/localhost/", baseDir)),
                "/usr/local/tomcat/conf/Catalina/localhost")
            .withEnv("URL", db.getJdbcUrl())
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
