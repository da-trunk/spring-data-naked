package org.datrunk.naked.server.entities;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application")
public class AppDetails {
    private String artifactId;
    private String deployedVersion;
    private String javaVersion;

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    public String getDeployedVersion() {
        return deployedVersion;
    }

    public void setDeployedVersion(final String deployedVersion) {
        this.deployedVersion = deployedVersion;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(final String javaVersion) {
        this.javaVersion = javaVersion;
    }
}
