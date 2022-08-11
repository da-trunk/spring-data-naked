package org.datrunk.naked.entities.random;

import com.github.javafaker.Faker;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This saves and compares against every value generated to ensure uniqueness. It is not cheap.
 *
 * @param <T> type
 */
public abstract class UniqueRandomizer<T> extends AbstractRandomizer<T> {
  protected static final Logger LOGGER = LogManager.getLogger();
  private final Set<T> values = new HashSet<>();

  protected UniqueRandomizer() {
    super(0);
  }

  @Override
  protected abstract T get();

  @Override
  public final T getRandomValue() {
    for (int i = 0; i < RepeatingRandomizer.MAX_DISTINCT_ATTEMPTS; i++) {
      T result = get();
      assert (result != null);
      if (values.add(result)) {
        return result;
      }
    }
    throw new IllegalStateException(
        String.format(
            "%s generated %d duplicates in a row.  Is it able to generate more than %d distinct values?",
            getClass().getSimpleName(), RepeatingRandomizer.MAX_DISTINCT_ATTEMPTS, values.size()));
  }

  public static <T> UniqueRandomizer<T> create(Supplier<T> randomizer) {
    return new UniqueRandomizer<T>() {
      @Override
      protected T get() {
        return randomizer.get();
      }
    };
  }

  public static <T> UniqueRandomizer<T> createFromFaker(Function<Faker, T> randomizer) {
    return new UniqueRandomizer<T>() {
      @Override
      protected T get() {
        return randomizer.apply(faker);
      }
    };
  }

  public static <T> UniqueRandomizer<T> createFromRandom(Function<Random, T> randomizer) {
    return new UniqueRandomizer<T>() {
      @Override
      protected T get() {
        return randomizer.apply(random);
      }
    };
  }
}
