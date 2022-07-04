package org.datrunk.naked.entities.random;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import com.github.javafaker.Faker;
import com.google.common.collect.ImmutableList;

public abstract class RepeatingRandomizer<T> extends AbstractRandomizer<T> {
  private static final int MAX_DISTINCT_ATTEMPTS = 100;

  private int maxSize;
  private final boolean filterDuplicates = true;
  private final ArrayList<T> populatedBeans;

  /**
   * @param maxSize the maximum number of distinct objects this Randomizer can
   *                generate.
   */
  protected RepeatingRandomizer(int maxSize) {
    super(0);
    this.maxSize = maxSize;
    populatedBeans = new ArrayList<>(maxSize);
  }

  protected int nextIndex() {
    return random.nextInt(maxSize);
  }

  public void setMaxSize(int size) {
    if (size < populatedBeans.size()) {
      for (T obj : populatedBeans.subList(size, populatedBeans.size())) {
        populatedBeans.remove(obj);
      }
    } else {
      populatedBeans.ensureCapacity(size);
    }
  }

  public int getMaxSize() {
    return maxSize;
  }

  @Override
  public final T getRandomValue() throws Exception {
    T result;
    int index = nextIndex();
    if (index < 0 || index >= maxSize) {
      throw new Exception("nextIndex must return values in range [0, " + maxSize + ")");
    }
    if (index >= populatedBeans.size()) {
      result = get();
      int duplicatesFiltered = 0;
      while (filterDuplicates && populatedBeans.contains(result)) {
        duplicatesFiltered++;
        if (duplicatesFiltered > MAX_DISTINCT_ATTEMPTS) {
          throw new Exception("%s generated %d duplicates in a row.  Is it able to generate %d distinct values?",
              getClass().getSimpleName(), duplicatesFiltered, maxSize);
        }
        result = get();
      }
      index = populatedBeans.size();
      populatedBeans.add(result);
    } else {
      result = populatedBeans.get(index);
    }
    return result;
  }

  public List<T> getAll() throws Exception {
    while (populatedBeans.size() < maxSize) {
      populatedBeans.add(get());
    }
    return ImmutableList.copyOf(populatedBeans);
  }

  public static <T> RepeatingRandomizer<T> createFromFaker(int poolSize, Function<Faker, T> randomizer) {
    return new RepeatingRandomizer<T>(poolSize) {
      @Override
      public T get() {
        return randomizer.apply(faker);
      }
    };
  }

  public static <T> RepeatingRandomizer<T> createFromRandom(int poolSize, Function<Random, T> randomizer) {
    return new RepeatingRandomizer<T>(poolSize) {
      @Override
      public T get() {
        return randomizer.apply(random);
      }
    };
  }

  @SafeVarargs
  public static <T> RepeatingRandomizer<T> createFromChoices(final T... choices) {
    return new RepeatingRandomizer<T>(choices.length) {
      @Override
      public T get() {
        int i = nextIndex();
        return choices[i];
      }
    };
  }
}
