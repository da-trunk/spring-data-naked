package org.datrunk.naked.entities.random;

import java.util.HashMap;
import java.util.Map;

public class SubClassRepeatingRandomizer<E> extends RepeatingRandomizer<E> {
  private final Map<String, Randomizer<? extends E>> randomizers;

  protected SubClassRepeatingRandomizer(int poolSize) {
    super(poolSize);
    this.randomizers = new HashMap<>();
  }

  protected void add(Randomizer<? extends E> randomizer) {
    this.randomizers.put(randomizer.getClass().getCanonicalName(), randomizer);
  }

  @Override
  public final E get() throws Exception {
    int i = random.nextInt(randomizers.size());
    String[] keys = this.randomizers.keySet().toArray(new String[randomizers.size()]);
    String key = keys[i];
    Randomizer<? extends E> randomizer = randomizers.get(key);
    return randomizer.getRandomValue();
  }

  @Override
  public void setMaxSize(int size) {
    super.setMaxSize(size);
    randomizers.values().stream()
        .filter(randomizer -> randomizer instanceof RepeatingRandomizer)
        .map(randomizer -> (RepeatingRandomizer<?>) randomizer)
        .forEach(randomizer -> randomizer.setMaxSize(size));
  }
}
