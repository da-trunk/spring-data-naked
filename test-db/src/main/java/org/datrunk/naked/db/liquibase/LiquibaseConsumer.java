package org.datrunk.naked.db.liquibase;

import liquibase.exception.LiquibaseException;

// Copied from org.junit.jupiter.api.function.ThrowingConsumer
@FunctionalInterface
public interface LiquibaseConsumer<T> {

  /**
   * Consume the supplied argument, potentially throwing an exception.
   *
   * @param t the argument to consume
   */
  void accept(T t) throws LiquibaseException;
}
