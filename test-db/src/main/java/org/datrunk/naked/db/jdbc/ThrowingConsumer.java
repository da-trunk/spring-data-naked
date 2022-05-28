package org.datrunk.naked.db.jdbc;

// Copied from org.junit.jupiter.api.function.ThrowingConsumer
@FunctionalInterface
public interface ThrowingConsumer<T> {

  /**
   * Consume the supplied argument, potentially throwing an exception.
   *
   * @param t the argument to consume
   */
  void accept(T t) throws Exception;
}
