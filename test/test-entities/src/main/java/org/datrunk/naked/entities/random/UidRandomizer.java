package org.datrunk.naked.entities.random;

import java.util.Random;

/** Generate random UUIDs with a deterministic sequence. */
public class UidRandomizer extends AbstractRandomizer<UidRandomizer.UUID> {
  public UidRandomizer() {
    super(0);
  }

  @Override
  public UUID get() {
    return UUID.randomUUID(random);
  }

  /*
   * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved. DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE
   * HEADER.
   *
   * This code is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License version 2
   * only, as published by the Free Software Foundation. Oracle designates this particular file as subject to the "Classpath" exception as
   * provided by Oracle in the LICENSE file that accompanied this code.
   *
   * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
   * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2 for more details (a copy is
   * included in the LICENSE file that accompanied this code).
   *
   * You should have received a copy of the GNU General Public License version 2 along with this work; if not, write to the Free Software
   * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
   *
   * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or visit www.oracle.com if you need additional information or
   * have any questions.
   */

  /**
   * A class that represents an immutable universally unique identifier (UUID). A UUID represents a
   * 128-bit value.
   *
   * <p>There exist different variants of these global identifiers. The methods of this class are
   * for manipulating the Leach-Salz variant, although the constructors allow the creation of any
   * variant of UUID (described below).
   *
   * <p>The layout of a variant 2 (Leach-Salz) UUID is as follows:
   *
   * <p>The most significant long consists of the following unsigned fields:
   *
   * <pre>
   * 0xFFFFFFFF00000000 time_low
   * 0x00000000FFFF0000 time_mid
   * 0x000000000000F000 version
   * 0x0000000000000FFF time_hi
   * </pre>
   *
   * The least significant long consists of the following unsigned fields:
   *
   * <pre>
   * 0xC000000000000000 variant
   * 0x3FFF000000000000 clock_seq
   * 0x0000FFFFFFFFFFFF node
   * </pre>
   *
   * <p>The variant field contains a value which identifies the layout of the {@code UUID}. The bit
   * layout described above is valid only for a {@code UUID} with a variant value of 2, which
   * indicates the Leach-Salz variant.
   *
   * <p>The version field holds a value that describes the type of this {@code UUID}. There are four
   * different basic types of UUIDs: time-based, DCE security, name-based, and randomly generated
   * UUIDs. These types have a version value of 1, 2, 3 and 4, respectively.
   *
   * <p>For more information including algorithms used to create {@code UUID}s, see <a
   * href="http://www.ietf.org/rfc/rfc4122.txt"><i>RFC&nbsp;4122: A Universally Unique IDentifier
   * (UUID) URN Namespace</i></a>, section 4.2 &quot;Algorithms for Creating a Time-Based
   * UUID&quot;.
   *
   * @since 1.5
   */
  public static final class UUID implements java.io.Serializable, Comparable<UUID> {
    private static final long serialVersionUID = 1;

    /*
     * The most significant 64 bits of this UUID.
     *
     * @serial
     */
    private final long mostSigBits;

    /*
     * The least significant 64 bits of this UUID.
     *
     * @serial
     */
    private final long leastSigBits;

    // Constructors and Factories

    /*
     * Private constructor which uses a byte array to construct the new UUID.
     */
    private UUID(byte[] data) {
      long msb = 0;
      long lsb = 0;
      assert data.length == 16 : "data must be 16 bytes in length";
      for (int i = 0; i < 8; i++) {
        msb = (msb << 8) | (data[i] & 0xff);
      }
      for (int i = 8; i < 16; i++) {
        lsb = (lsb << 8) | (data[i] & 0xff);
      }
      mostSigBits = msb;
      leastSigBits = lsb;
    }

    /**
     * Static factory to retrieve a type 4 (pseudo randomly generated) UUID.
     *
     * <p>The {@code UUID} is generated using a cryptographically strong pseudo random number
     * generator.
     *
     * @return A randomly generated {@code UUID}
     */
    public static UUID randomUUID(Random random) {
      byte[] randomBytes = new byte[16];
      random.nextBytes(randomBytes);
      randomBytes[6] &= 0x0f; /* clear version */
      randomBytes[6] |= 0x40; /* set to version 4 */
      randomBytes[8] &= 0x3f; /* clear variant */
      randomBytes[8] |= 0x80; /* set to IETF variant */
      return new UUID(randomBytes);
    }

    // Field Accessor Methods

    /**
     * Returns the least significant 64 bits of this UUID's 128 bit value.
     *
     * @return The least significant 64 bits of this UUID's 128 bit value
     */
    public long getLeastSignificantBits() {
      return leastSigBits;
    }

    /**
     * Returns the most significant 64 bits of this UUID's 128 bit value.
     *
     * @return The most significant 64 bits of this UUID's 128 bit value
     */
    public long getMostSignificantBits() {
      return mostSigBits;
    }

    // Object Inherited Methods

    /**
     * Returns a {@code String} object representing this {@code UUID}.
     *
     * <p>The UUID string representation is as described by this BNF:
     *
     * <blockquote>
     *
     * <pre>{@code
     * UUID                   = <time_low> "-" <time_mid> "-"
     *                          <time_high_and_version> "-"
     *                          <variant_and_sequence> "-"
     *                          <node>
     * time_low               = 4*<hexOctet>
     * time_mid               = 2*<hexOctet>
     * time_high_and_version  = 2*<hexOctet>
     * variant_and_sequence   = 2*<hexOctet>
     * node                   = 6*<hexOctet>
     * hexOctet               = <hexDigit><hexDigit>
     * hexDigit               =
     *       "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
     *       | "a" | "b" | "c" | "d" | "e" | "f"
     *       | "A" | "B" | "C" | "D" | "E" | "F"
     * }</pre>
     *
     * </blockquote>
     *
     * @return A string representation of this {@code UUID}
     */
    @Override
    public String toString() {
      return (digits(mostSigBits >> 32, 8)
          + "-"
          + digits(mostSigBits >> 16, 4)
          + "-"
          + digits(mostSigBits, 4)
          + "-"
          + digits(leastSigBits >> 48, 4)
          + "-"
          + digits(leastSigBits, 12));
    }

    /**
     * Returns a hash code for this {@code UUID}.
     *
     * @return A hash code value for this {@code UUID}
     */
    @Override
    public int hashCode() {
      long hilo = mostSigBits ^ leastSigBits;
      return ((int) (hilo >> 32)) ^ (int) hilo;
    }

    /**
     * Compares this object to the specified object. The result is {@code true} if and only if the
     * argument is not {@code null}, is a {@code UUID} object, has the same variant, and contains
     * the same value, bit for bit, as this {@code UUID}.
     *
     * @param obj The object to be compared
     * @return {@code true} if the objects are the same; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
      if ((null == obj) || (obj.getClass() != UUID.class)) {
        return false;
      }
      UUID id = (UUID) obj;
      return (mostSigBits == id.mostSigBits && leastSigBits == id.leastSigBits);
    }

    // Comparison Operations

    /**
     * Compares this UUID with the specified UUID.
     *
     * <p>The first of two UUIDs is greater than the second if the most significant field in which
     * the UUIDs differ is greater for the first UUID.
     *
     * @param val {@code UUID} to which this {@code UUID} is to be compared
     * @return -1, 0 or 1 as this {@code UUID} is less than, equal to, or greater than {@code val}
     */
    @Override
    public int compareTo(UUID val) {
      // The ordering is intentionally set up so that the UUIDs
      // can simply be numerically compared as two numbers
      return (mostSigBits < val.mostSigBits
          ? -1
          : (mostSigBits > val.mostSigBits
              ? 1
              : (leastSigBits < val.leastSigBits
                  ? -1
                  : (leastSigBits > val.leastSigBits ? 1 : 0))));
    }

    /** Returns val represented by the specified number of hex digits. */
    private static String digits(long val, int digits) {
      long hi = 1L << (digits * 4);
      return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }
  }
}
