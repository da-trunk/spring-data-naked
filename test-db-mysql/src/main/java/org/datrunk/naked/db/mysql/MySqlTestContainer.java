package org.datrunk.naked.db.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.extern.log4j.Log4j2;
import org.datrunk.naked.db.SpringTestDbContainer;
import org.datrunk.naked.test.container.SpringTestContainers;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Extends {@link MySQLContainer} with properties and methods specific to an MRDS database.
 *
 * @author BA030483
 */
@Log4j2
public class MySqlTestContainer extends MySQLContainer<MySqlTestContainer>
    implements SpringTestDbContainer {

  public MySqlTestContainer(
      final @Nonnull Environment environment, Consumer<MySqlTestContainer> extraConfig) {
    this(
        environment.getProperty("test.containers.mrds.image"),
        environment.getProperty("test.containers.mrds.datasource.username"),
        environment.getProperty("test.containers.mrds.datasource.password"),
        environment.getProperty("test.containers.mrds.application.buildDir"),
        extraConfig);
  }

  public MySqlTestContainer(
      @Nonnull String imageName,
      @Nonnull String userName,
      @Nonnull String password,
      @Nonnull String buildDir,
      Consumer<MySqlTestContainer> extraConfig) {
    super(DockerImageName.parse(imageName));
    DockerImageName.parse(imageName).assertValid();
    withExposedPorts(3306);
    withNetwork(SpringTestContainers.getNetwork());
    withNetworkAliases("mysql");
    withAccessToHost(true);
    withUsername(userName);
    withPassword(password);
    withCopyFileToContainer(
        MountableFile.forHostPath(buildDir + "/mysql.cnf"), "/etc/mysql/conf.d/myconf.cnf");
    waitingFor(new LogMessageWaitStrategy().withRegEx("ready for connections"));

    extraConfig.accept(this);
    if (imageName.endsWith(":latest")) {
      withImagePullPolicy(PullPolicy.alwaysPull());
    }
    start();
    log.info("MrdsTestContainer started at {}", getJdbcUrl());
  }

  @Override
  public MySqlTestContainer getContainer() {
    return this;
  }

  public static class Factory
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static MySqlTestContainer instance = null;

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
      System.setProperty("java.io.tmpdir", "/tmp");
      create(ctx);
    }

    private static void create(ConfigurableApplicationContext ctx) {
      final Environment env = ctx.getEnvironment();
      if (instance == null) {
        //        PropertySource<?> dataSourceProperties =
        //            env.getPropertySources().get("test.containers.mrds.datasource");
        final boolean reUse = Boolean.parseBoolean(env.getProperty("test.containers.mrds.reuse"));

        instance = new MySqlTestContainer(env, container -> container.withReuse(reUse));

        System.setProperty("test.containers.mrds.datasource.url", instance.getJdbcUrl());
      }

      // Programmatically register the container as a bean so it can be injected
      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
      if (!beanFactory.containsBean(MySqlTestContainer.class.getName())) {
        beanFactory.registerSingleton(MySqlTestContainer.class.getName(), instance);
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            ctx, "test.containers.mrds.datasource.url=" + instance.getJdbcUrl());
      }
    }

    public static void destroy(ConfigurableApplicationContext ctx) {
      instance.stop();
      instance = null;
      ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
      beanFactory.destroyBean(beanFactory.getBean(MySqlTestContainer.class.getName()));
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
  public Connection getSysConnection() throws SQLException {
    return getConnection("root", "password");
  }
}
