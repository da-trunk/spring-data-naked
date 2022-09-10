package org.datrunk.naked.entities.random;

@FunctionalInterface
public interface Randomizer<T> {
  T getRandomValue() throws Randomizer.Exception;

  public static class Exception extends java.lang.Exception {
    private static final long serialVersionUID = 1L;

    public Exception(String msg) {
      super(msg);
    }

    public Exception(String fmt, Object... vals) {
      super(String.format(fmt, vals));
    }
  }
}
