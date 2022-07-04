package org.datrunk.naked.entities.random;

import java.util.Locale;
import java.util.Random;

import com.github.javafaker.Faker;

public abstract class AbstractRandomizer<T> implements Randomizer<T> {
  protected final Random random;
  protected final Faker faker;

  protected abstract T get() throws Exception;

  protected AbstractRandomizer(final long seed) {
    this(seed, Locale.ENGLISH);
  }

  protected AbstractRandomizer(final long seed, final Locale locale) {
    random = new Random(seed);
    faker = new Faker(locale, random);
  }

}
