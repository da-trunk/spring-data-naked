package org.datrunk.naked.entities.random;

import com.github.javafaker.Faker;
import java.util.Locale;
import java.util.Random;

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
}
