package org.datrunk.naked.test.container;

import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.Network;

/** Manages docker containers for integration tests. */
@Component
public class SpringTestContainers {
  private static Network network = null;
  private static ConfigurableApplicationContext ctx = null;

  private final LinkedHashMap<String, SpringTestContainer> containers;

  protected SpringTestContainers() {
    containers = new LinkedHashMap<>();
    network = Network.newNetwork();
  }

  public void add(SpringTestContainer container) {
    container.setNetwork(network);
    containers.put(container.getContainerId(), container);
  }

  @Autowired
  public void setContext(ConfigurableApplicationContext ctx) {
    SpringTestContainers.ctx = ctx;
  }

  public static Network getNetwork() {
    if (network == null) {
      network = Network.newNetwork();
    }
    return network;
  }
}
