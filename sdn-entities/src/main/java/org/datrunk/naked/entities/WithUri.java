package org.datrunk.naked.entities;

import java.net.URI;

/**
 * Entities supported from by {@link CEClient} should implement this interface.
 *
 * @author da-trunk@outlook.com
 */
public interface WithUri {
  URI getUri();

  void setUri(URI uri);
}
