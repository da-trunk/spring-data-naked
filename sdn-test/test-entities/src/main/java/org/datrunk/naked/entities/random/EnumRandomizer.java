package org.datrunk.naked.entities.random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EnumRandomizer<E extends Enum<E>> extends AbstractRandomizer<E> {
  protected static final Logger LOGGER = LogManager.getLogger();
  private final E[] values;

  public EnumRandomizer(Class<E> type) {
    super(0);
    values = type.getEnumConstants();
  }

  @Override
  protected final E get() {
    int i = random.nextInt(values.length);
    return values[i];
  }

  @Override
  public final E getRandomValue() {
    return get();
  }
}
