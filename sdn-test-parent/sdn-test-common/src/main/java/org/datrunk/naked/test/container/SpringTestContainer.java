package org.datrunk.naked.test.container;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

/** Manages docker containers for integration tests. */
public interface SpringTestContainer extends AutoCloseable {
  GenericContainer<?> getContainer();

  default int exec(String... command) {
    ExecResult result = null;
    final Logger log = LogManager.getLogger();
    try {
      log.info(
          "exec [{}] in [{}]",
          Stream.of(command).collect(Collectors.joining(" ")),
          getContainer().getDockerImageName());
      result = getContainer().execInContainer(command);
      log.info(result.getStdout());
    } catch (UnsupportedOperationException | IOException | InterruptedException e) {
      log.catching(e);
    }
    assert (result != null);
    return result.getExitCode();
  }

  default void copyFileToContainer(String fromPath, String toPath) {
    // exec(container, "rm", "-rf", toPath);
    final Logger log = LogManager.getLogger();
    log.info(
        "Copying [{}:{}] to [{}:{}]",
        getContainer().getHost(),
        fromPath,
        getContainer().getDockerImageName(),
        toPath);
    getContainer().copyFileToContainer(MountableFile.forHostPath(fromPath), toPath);
  }

  /**
   * In Windows, ensure this directory is shared. Go to docker dashboard -> settings -> Resources ->
   * FileSharing. Add /c/Users/<user>/AppData/Local/Temp, then click "Apply & Restart".
   *
   * @return the directory on the host where container will write data.
   */
  default String getVolume(String name) {
    // return "/c/Users/Ansonator/AppData/Local/Temp/xe21";
    String tmpDir = System.getProperty("java.io.tmpdir");
    String strPath = toHostPath(Paths.get(tmpDir, name));
    final Logger log = LogManager.getLogger();
    log.info("Mounting volume [{}]", strPath);
    return strPath;
  }

  /**
   * Creates a temporary directory on the host.
   *
   * <p>In Windows, ensure this directory is shared. Go to docker dashboard -> settings -> Resources
   * -> FileSharing. Add <i>/c/Users/<user>/AppData/Local/Temp</i>, then click "Apply & Restart".
   *
   * @return {@link Path} to a newly create temporary directory on the host.
   */
  default Path getHostTempDir(String name) {
    String tmpDir = System.getProperty("java.io.tmpdir");
    Path path = Paths.get(tmpDir, name).toAbsolutePath();
    LogManager.getLogger()
        .info("Creating temporary directory for mounting at [{}] on host", toHostPath(path));
    return path;
  }

  static String toHostPath(final @Nonnull Path path) {
    final Pattern pattern = Pattern.compile("^([A-Z]):\\\\");
    String strPath = path.toString();
    Matcher matcher = pattern.matcher(strPath);
    if (matcher.find()) {
      String drive = matcher.group(1);
      strPath =
          String.format("/%s/%s", drive.toLowerCase(), strPath.substring(matcher.end()))
              .replace('\\', '/');
    }
    return strPath;
  }

  default void setNetwork(Network network) {
    getContainer().setNetwork(network);
  }

  default String getContainerId() {
    return getContainer().getContainerId();
  }

  static OptionalInt getPort(String name, final @Nonnull Environment environment) {
    String propertyStr = environment.getProperty("test.containers." + name + ".port");
    return propertyStr == null ? OptionalInt.empty() : OptionalInt.of(Integer.valueOf(propertyStr));
  }
}
