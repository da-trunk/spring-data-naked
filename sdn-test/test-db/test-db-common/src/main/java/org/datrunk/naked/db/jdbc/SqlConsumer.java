package org.datrunk.naked.db.jdbc;

import java.sql.SQLException;

// Copied from org.junit.jupiter.api.function.ThrowingConsumer
@FunctionalInterface
public interface SqlConsumer<T> {

  /**
   * Consume the supplied argument, potentially throwing an exception.
   *
   * @param t the argument to consume
   */
  void accept(T t) throws SQLException;
}
