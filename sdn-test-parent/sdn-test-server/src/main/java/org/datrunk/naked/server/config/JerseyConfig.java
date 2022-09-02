package org.datrunk.naked.server.config;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletProperties;

public class JerseyConfig extends ResourceConfig {
  public JerseyConfig() {
    super();
    register(JacksonFeature.class);
    // packages("org.datrunk.naked.exception"); // Exception Handlers
    property(
        ServletProperties.FILTER_FORWARD_ON_404,
        true); // Gives spring-data-rest and actuator a chance to handle unrecognized
    // requests.
  }
}
