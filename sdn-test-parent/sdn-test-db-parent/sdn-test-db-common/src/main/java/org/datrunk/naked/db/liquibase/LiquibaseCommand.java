package org.datrunk.naked.db.liquibase;

import java.util.Objects;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;

@FunctionalInterface
public interface LiquibaseCommand extends LiquibaseConsumer<Liquibase> {
  @Override
  void accept(Liquibase liquibase) throws LiquibaseException;

  /**
   * Returns a composed {@code Consumer} that performs, in sequence, this operation followed by the
   * {@code after} operation. If performing either operation throws an exception, it is relayed to
   * the caller of the composed operation. If performing this operation throws an exception, the
   * {@code after} operation will not be performed.
   *
   * @param after the operation to perform after this operation
   * @return a composed {@code Consumer} that performs in sequence this operation followed by the
   *     {@code after} operation
   * @throws NullPointerException if {@code after} is null
   */
  default LiquibaseCommand andThen(LiquibaseCommand after) {
    Objects.requireNonNull(after);
    return (Liquibase liquibase) -> {
      accept(liquibase);
      after.accept(liquibase);
    };
  }
}
