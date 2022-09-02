package org.datrunk.naked.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.datasource.tomcat")
public class TomcatConnectionProperties {
  private int validationQueryTimeout = 300;
  private int maxActive = 10;
  private int maxWait = 10;
  private int maxIdle = 10;

  public int getValidationQueryTimeout() {
    return validationQueryTimeout;
  }

  public void setValidationQueryTimeout(int validationQueryTimeout) {
    this.validationQueryTimeout = validationQueryTimeout;
  }

  public int getMaxActive() {
    return maxActive;
  }

  public void setMaxActive(int maxActive) {
    this.maxActive = maxActive;
  }

  public int getMaxWait() {
    return maxWait;
  }

  public void setMaxWait(int maxWait) {
    this.maxWait = maxWait;
  }

  public int getMaxIdle() {
    return maxIdle;
  }

  public void setMaxIdle(int maxIdle) {
    this.maxIdle = maxIdle;
  }
}
