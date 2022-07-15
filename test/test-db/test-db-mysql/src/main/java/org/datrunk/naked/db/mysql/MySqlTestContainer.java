package org.datrunk.naked.db.mysql;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Nonnull;

import org.datrunk.naked.db.SpringTestDbContainer;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.MountableFile;

import lombok.extern.log4j.Log4j2;

/**
 * Spring integration for {@link MySQLContainer}.
 *
 * @author BA030483
 */
@Log4j2
public class MySqlTestContainer extends MySQLContainer<MySqlTestContainer> implements SpringTestDbContainer {
  private String jdbcUrl;

//  public MySqlTestContainer(final @Nonnull Environment environment) {
//    this(environment.getProperty("spring.datasource.image"), environment.getProperty("spring.datasource.config"),
//        environment.getProperty("spring.datasource.datasource.username"), environment.getProperty("spring.datasource.datasource.password"),
//        SpringTestContainer.getPort("mysql", environment));
//  }

  public MySqlTestContainer(final @Nonnull Environment environment) {
    super(environment.getProperty("spring.datasource.container.image"));
    init(this, environment, this::addFixedExposedPort);
    withCopyFileToContainer(MountableFile.forHostPath(environment.getProperty("spring.datasource.container.config")), "/etc/mysql/conf.d/myconf.cnf");
//    waitingFor(new LogMessageWaitStrategy().withRegEx("ready for connections"));
  }

//  public MySqlTestContainer(@Nonnull String imageName, @Nonnull String cnfPath, @Nonnull String userName, @Nonnull String password,
//      OptionalInt hostPort) {
//    super(DockerImageName.parse(imageName));
//    DockerImageName.parse(imageName).assertValid();
//    withNetwork(SpringTestContainers.getNetwork());
//    withNetworkAliases("db");
//    withAccessToHost(true);
//    if (hostPort.isEmpty()) {
//      withExposedPorts(3306);
//      withReuse(false);
//    } else {
//      addFixedExposedPort(hostPort.getAsInt(), 3306, InternetProtocol.TCP);
//      withReuse(true);
//    }
//    withUsername(userName);
//    withPassword(password);
//    if (imageName.endsWith(":latest")) {
//      withImagePullPolicy(PullPolicy.alwaysPull());
//    }
//    withCopyFileToContainer(MountableFile.forHostPath(cnfPath), "/etc/mysql/conf.d/myconf.cnf");
//    waitingFor(new LogMessageWaitStrategy().withRegEx("ready for connections"));
//  }

  @Override
  public String getJdbcUrl() {
    if (jdbcUrl != null)
      return jdbcUrl;
    else
      return super.getJdbcUrl();
  }

  @Override
  public void setJdbcUrl(String url) {
    jdbcUrl = url;
  }

  @Override
  public void start() {
    super.start();
    log.info("{} started at {}", getClass().getSimpleName(), getJdbcUrl());
  }

  @Override
  public MySqlTestContainer getContainer() {
    return this;
  }

  public static class Factory extends SpringTestDbContainer.Factory {
    public Factory() {
      super(MySqlTestContainer.class);
    }
  }

//  public static class Factory implements ApplicationContextInitializer<ConfigurableApplicationContext> {
//    private static MySqlTestContainer instance = null;
//
//    @Override
//    public void initialize(ConfigurableApplicationContext ctx) {
//      System.setProperty("java.io.tmpdir", "/tmp");
//      create(ctx);
//    }
//
//    private static void create(ConfigurableApplicationContext ctx) {
//      final Environment env = ctx.getEnvironment();
//      if (instance == null) {
//        instance = new MySqlTestContainer(env);
//        if (env.getProperty("spring.datasource.datasource.url") == null) {
//          instance.start();
//          System.setProperty("spring.datasource.datasource.url", instance.getJdbcUrl());
//        } else {
//          instance.setJdbcUrl(env.getProperty("spring.datasource.datasource.url"));
//          log.info("MySqlTestContainer connecting to {}", instance.getJdbcUrl());
//        }
//      }
//
//      // Programmatically register the container as a bean so it can be injected
//      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
//      if (!beanFactory.containsBean(MySqlTestContainer.class.getName())) {
//        beanFactory.registerSingleton(MySqlTestContainer.class.getName(), instance);
//        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, "spring.datasource.datasource.url=" + instance.getJdbcUrl());
//      }
//    }
//
//    public static void destroy(ConfigurableApplicationContext ctx) {
//      instance.stop();
//      instance = null;
//      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
//      beanFactory.destroyBean(beanFactory.getBean(MySqlTestContainer.class.getName()));
//    }
//
//    public static void restart(ConfigurableApplicationContext ctx, String dbUrl) {
//      if (instance == null) {
//        throw new IllegalStateException("cannot restart an instance that was never started");
//      }
//      destroy(ctx);
//      Factory.create(ctx);
//    }
//
//  }

  @Override
  public Connection getSysConnection() throws SQLException {
    return getConnection("root", "password");
  }
}
