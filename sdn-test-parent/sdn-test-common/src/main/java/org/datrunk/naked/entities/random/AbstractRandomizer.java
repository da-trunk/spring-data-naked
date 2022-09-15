package org.datrunk.naked.entities.random;

import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

import com.github.javafaker.Faker;

public abstract class AbstractRandomizer<T> implements Randomizer<T> {
  protected final Random random;
  protected final Faker faker;

  protected abstract T get() throws Exception;

  @Override
  public T getRandomValue() throws Exception {
    return get();
  }

  protected AbstractRandomizer(final long seed) {
    this(seed, Locale.ENGLISH);
  }

  protected AbstractRandomizer(final long seed, final Locale locale) {
    random = new Random(seed);
    faker = new Faker(locale, random);
  }

  public Stream<T> stream() {
    return Stream.generate(() -> {
      try {
        return getRandomValue();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
