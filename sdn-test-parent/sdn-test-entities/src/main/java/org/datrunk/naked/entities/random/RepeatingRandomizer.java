package org.datrunk.naked.entities.random;

import com.github.javafaker.Faker;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public abstract class RepeatingRandomizer<T> extends AbstractRandomizer<T> {
  public static final int MAX_DISTINCT_ATTEMPTS = 100;

  private final int maxSize;
  private final boolean filterDuplicates = true;
  private final ArrayList<T> populatedBeans;

  private UnaryOperator<T> updateFn = UnaryOperator.identity();

  /** @param maxSize the maximum number of distinct objects this Randomizer can generate. */
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
      while (filterDuplicates && isDuplicate(result)) {
        duplicatesFiltered++;
        if (duplicatesFiltered > MAX_DISTINCT_ATTEMPTS) {
          throw new Exception(
              "%s generated %d duplicates in a row.  Is it able to generate %d distinct values?",
              getClass().getSimpleName(), duplicatesFiltered, maxSize);
        }
        result = get();
      }
      index = populatedBeans.size();
      result = updateFn.apply(result);
      populatedBeans.add(result);
    } else {
      result = populatedBeans.get(index);
    }
    return result;
  }

  public List<T> getAll() throws Exception {
    while (populatedBeans.size() < maxSize) {
      populatedBeans.add(getRandomValue());
    }
    return ImmutableList.copyOf(populatedBeans);
  }

  /**
   * Override this to filter duplicates in a custom way.
   *
   * @param newBean a newly generated bean
   * @return false if newBean should be discarded
   */
  protected boolean isDuplicate(final T newBean) {
    return populatedBeans.contains(newBean);
  }

  /**
   * Helper function to use with custom implementations of {@link #isDuplicate}
   *
   * @param <R> type of a unique property of T
   * @param newBean a newly generated bean
   * @param extractProperty function to extract property R from type T
   * @return true if the property of newBean matches any of the equivalent property in already
   *     generated beans.
   */
  protected <R> boolean anyMatch(final T newBean, final Function<T, R> extractProperty) {
    final R newProperty = extractProperty.apply(newBean);
    return populatedBeans.stream().map(extractProperty).anyMatch(newProperty::equals);
  }

  /**
   * The provided function will be called after generating a new entity. The generated entity will
   * be assigned to the result of this call.
   *
   * <p>This is to deal with error <i>A different object with the same identifier value was already
   * associated with the session</i>. For more information, see section
   * <b>DataIntegrityViolationException</b> in the README.
   *
   * <p>Implementation Note: this is a lambda to help decouple subclasses from persistence-related
   * dependencies.
   *
   * @param updater
   */
  public void setUpdater(UnaryOperator<T> updater) {
    this.updateFn = updater;
  }

  public void prePopulate(final Iterable<T> beans) {
    for (T bean : beans) {
      if (populatedBeans.size() >= maxSize) {
        break;
      }
      if (!populatedBeans.contains(bean)) {
        populatedBeans.add(bean);
      }
    }
  }

  public static <T> RepeatingRandomizer<T> createFromFaker(
      int poolSize, Function<Faker, T> randomizer) {
    return new RepeatingRandomizer<T>(poolSize) {
      @Override
      public T get() {
        return randomizer.apply(faker);
      }
    };
  }

  public static <T> RepeatingRandomizer<T> createFromRandom(
      int poolSize, Function<Random, T> randomizer) {
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
