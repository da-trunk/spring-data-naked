package org.datrunk.naked.entities;

import com.github.javafaker.Name;
import org.datrunk.naked.entities.random.Randomizer;
import org.datrunk.naked.entities.random.RepeatingRandomizer;

public class UserRandomizer extends RepeatingRandomizer<User> {
  private RepeatingRandomizer<Role> roleRandomizer;

  public UserRandomizer(int poolSize) {
    super(poolSize);
    roleRandomizer = RepeatingRandomizer.createFromChoices(Role.values());
  }

  @Override
  protected User get() throws Randomizer.Exception {
    final Name name = faker.name();
    return new User(name.firstName(), roleRandomizer.getRandomValue());
  }
}
