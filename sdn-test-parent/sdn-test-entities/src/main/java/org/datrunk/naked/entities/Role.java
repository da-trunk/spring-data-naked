package org.datrunk.naked.entities;

public enum Role {
  Admin,
  User;

  public static Role fromString(String value) {
    return Role.valueOf(value);
  }
}
