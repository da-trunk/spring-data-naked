package org.datrunk.naked.client.container;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datrunk.naked.test.container.SpringTestContainer;
import org.datrunk.naked.test.container.SpringTestContainers;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Deploys a war and starts a server implementation within the ROOT context of a
 * Tomcat container running inside of a Docker container.
 */
public class TomcatTestContainer extends GenericContainer<TomcatTestContainer> implements SpringTestContainer {
  private static final Logger log = LogManager.getLogger();

  private URI baseUri;
  private final String springDataRestBasePath;
  private final Environment environment;

  public TomcatTestContainer(final @Nonnull Environment environment) {
    super(DockerImageName.parse("tomcat:9-jdk8-adoptopenjdk-hotspot"));
    withNetwork(SpringTestContainers.getNetwork());
    withNetworkAliases("tomcat");
    OptionalInt hostPort = SpringTestContainer.getPort("tomcat", environment);
    if (hostPort.isEmpty()) {
      withExposedPorts(8080);
      withReuse(false);
    } else {
      addFixedExposedPort(hostPort.getAsInt(), 8080, InternetProtocol.TCP);
      withReuse(true);
    }
    // Delay copyFilesToContainer until just before start in order to save time if
    // we're attaching to an already running container
    this.environment = environment;
    springDataRestBasePath = environment.getProperty("spring.data.rest.base-path");
  }

  @Override
  public void start() {
    copyFilesToContainer(environment);
    waitingFor(Wait.forLogMessage(
        ".*\\[main\\] org\\.apache\\.catalina\\.startup\\.Catalina\\.start Server startup in \\[\\d+\\] milliseconds\\n", 1));
    super.start();
    int port = getMappedPort(8080);
    baseUri = UriComponentsBuilder.newInstance().scheme("http").host(getHost()).port(port).path("").build().toUri();
    log.info("Server started at [{}]", baseUri.toString());
  }

  /**
   * Uses Spring environment variables to locate a war file and conf directory
   * which should be accessible within the container before it is started. Certain
   * variables must be present in the {@link Environment}. Add a resource file to
   * the test classpath which defines these properties. Override this method to
   * provide a custom implementation.
   *
   * @param environment the Spring {@link Environment}
   */
  protected void copyFilesToContainer(final @Nonnull Environment environment) {
    String prefix = "test.containers.tomcat.config.";
    @Nonnull
    String warPathStr = environment.getProperty(prefix + "war-path");
    assertThat(warPathStr).isNotEmpty();
    assertThat(warPathStr).doesNotStartWith("@");
    @Nonnull
    String confPathStr = environment.getProperty(prefix + "conf-path");
    assertThat(confPathStr).isNotEmpty();
    assertThat(confPathStr).doesNotStartWith("@");
    Path confPath = Paths.get(confPathStr);
    log.info("Copying war from [{}] to container", warPathStr);
    withCopyFileToContainer(forHostPath(Paths.get(warPathStr)), "/usr/local/tomcat/webapps/ROOT.war");
    log.info("Copying [{}/server.xml] to container", confPath);
    withCopyFileToContainer(forHostPath(confPath, "server.xml"), "/usr/local/tomcat/conf/server.xml");
    log.info("Copying [{}/tomcat-users.xml] to container", confPath);
    withCopyFileToContainer(forHostPath(confPath, "tomcat-users.xml"), "/usr/local/tomcat/conf/tomcat-users.xml");
    log.info("Copying [{}/Catalina/localhost/] to container", confPath);
    withCopyFileToContainer(forHostPath(confPath, "/Catalina/localhost/"), "/usr/local/tomcat/conf/Catalina/localhost");
  }

  MountableFile forHostPath(Path basePath, String... strComponents) {
    Path path = Paths.get(basePath.toAbsolutePath().toString(), strComponents);
    assertThat(path).exists();
    String strPath = Stream.concat(Stream.of(basePath.toAbsolutePath().toString()), Stream.of(strComponents))
        .collect(Collectors.joining("/"));
    return MountableFile.forHostPath(strPath);
  }

  public URI getBaseUri() {
    UriComponentsBuilder result = UriComponentsBuilder.fromUri(baseUri);
    if (springDataRestBasePath != null) {
      result.pathSegment(springDataRestBasePath);
    }
    return result.build().toUri();
  }

  @Order(Ordered.LOWEST_PRECEDENCE)
  public static class Factory implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static TomcatTestContainer instance = null;

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
      System.setProperty("java.io.tmpdir", "/tmp");
      create(ctx);
    }

    private static void create(ConfigurableApplicationContext ctx) {
      final Environment env = ctx.getEnvironment();
      if (instance == null) {
        instance = new TomcatTestContainer(env);
        String url = env.getProperty("test.containers.tomcat.url");
        if (url == null) {
          instance.start();
          url = instance.getBaseUri().toASCIIString();
          System.setProperty("test.containers.tomcat.url", url);
          log.info("TomcatTestContainer connecting to {}", instance.getBaseUri());
        } else {
          instance.baseUri = URI.create(url);
          log.info("TomcatTestContainer connecting to {}", instance.getBaseUri());
        }
      }

      // Programmatically register the container as a bean so it can be injected
      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
      if (!beanFactory.containsBean(TomcatTestContainer.class.getName())) {
        beanFactory.registerSingleton(TomcatTestContainer.class.getName(), instance);
//        UriComponentsBuilder restUri = UriComponentsBuilder.fromUri(instance.getBaseUri());
//        String springDataRestBasePath = env.getProperty("spring.data.rest.base-path");
//        if (springDataRestBasePath != null) {
//          restUri.pathSegment(springDataRestBasePath);
//        }
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, "client.repo.location=" + instance.getBaseUri().toASCIIString());
      }
    }

    public static void destroy(ConfigurableApplicationContext ctx) {
      instance.stop();
      instance = null;
      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
      beanFactory.destroyBean(beanFactory.getBean(TomcatTestContainer.class.getName()));
    }

    public static void restart(ConfigurableApplicationContext ctx, String dbUrl) {
      if (instance == null) {
        throw new IllegalStateException("cannot restart an instance that was never started");
      }
      destroy(ctx);
      Factory.create(ctx);
    }
  }

  @Override
  public GenericContainer<?> getContainer() {
    return this;
  }
}
