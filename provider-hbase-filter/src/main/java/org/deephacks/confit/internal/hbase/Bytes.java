/*
* Copyright (C) 2010-2012 The Async HBase Authors. All rights reserved.
* This file is part of Async HBase.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
* - Redistributions of source code must retain the above copyright notice,
* this list of conditions and the following disclaimer.
* - Redistributions in binary form must reproduce the above copyright notice,
* this list of conditions and the following disclaimer in the documentation
* and/or other materials provided with the distribution.
* - Neither the name of the StumbleUpon nor the names of its contributors
* may be used to endorse or promote products derived from this software
* without specific prior written permission.
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package org.deephacks.confit.internal.hbase;

/**
* Helper functions to manipulate byte arrays.
*
* This is slightly modified version of the Bytes class
* found in asynchbase.
*
* All credit goes to The Async HBase Authors.
*
* https://github.com/stumbleupon/asynchbase
*/
public class Bytes {

    Bytes() { // See BytesUtils.
    }

    // ------------------------------ //
    // Byte array conversion utilies. //
    // ------------------------------ //

    /**
    * Reads a big-endian 2-byte short from the begining of the given array.
    * @param b The array to read from.
    * @return A short integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static short getShort(final byte[] b) {
        return getShort(b, 0);
    }

    /**
    * Reads a big-endian 2-byte short from an offset in the given array.
    * @param b The array to read from.
    * @param offset The offset in the array to start reading from.
    * @return A short integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static short getShort(final byte[] b, final int offset) {
        return (short) (b[offset] << 8 | b[offset + 1] & 0xFF);
    }

    /**
    * Reads a big-endian 2-byte unsigned short from the begining of the
    * given array.
    * @param b The array to read from.
    * @return A positive short integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static int getUnsignedShort(final byte[] b) {
        return getUnsignedShort(b, 0);
    }

    /**
    * Reads a big-endian 2-byte unsigned short from an offset in the
    * given array.
    * @param b The array to read from.
    * @param offset The offset in the array to start reading from.
    * @return A positive short integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static int getUnsignedShort(final byte[] b, final int offset) {
        return getShort(b, offset) & 0x0000FFFF;
    }

    /**
    * Writes a big-endian 2-byte short at the begining of the given array.
    * @param b The array to write to.
    * @param n A short integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static void setShort(final byte[] b, final short n) {
        setShort(b, n, 0);
    }

    /**
    * Writes a big-endian 2-byte short at an offset in the given array.
    * @param b The array to write to.
    * @param offset The offset in the array to start writing at.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static void setShort(final byte[] b, final short n, final int offset) {
        b[offset + 0] = (byte) (n >>> 8);
        b[offset + 1] = (byte) (n >>> 0);
    }

    /**
    * Creates a new byte array containing a big-endian 2-byte short integer.
    * @param n A short integer.
    * @return A new byte array containing the given value.
    */
    public static byte[] fromShort(final short n) {
        final byte[] b = new byte[2];
        setShort(b, n);
        return b;
    }

    /**
    * Reads a big-endian 4-byte integer from the begining of the given array.
    * @param b The array to read from.
    * @return An integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static int getInt(final byte[] b) {
        return getInt(b, 0);
    }

    /**
    * Reads a big-endian 4-byte integer from an offset in the given array.
    * @param b The array to read from.
    * @param offset The offset in the array to start reading from.
    * @return An integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static int getInt(final byte[] b, final int offset) {
        return (b[offset + 0] & 0xFF) << 24 | (b[offset + 1] & 0xFF) << 16
                | (b[offset + 2] & 0xFF) << 8 | (b[offset + 3] & 0xFF) << 0;
    }

    /**
    * Reads a big-endian 4-byte unsigned integer from the begining of the
    * given array.
    * @param b The array to read from.
    * @return A positive integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static long getUnsignedInt(final byte[] b) {
        return getUnsignedInt(b, 0);
    }

    /**
    * Reads a big-endian 4-byte unsigned integer from an offset in the
    * given array.
    * @param b The array to read from.
    * @param offset The offset in the array to start reading from.
    * @return A positive integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static long getUnsignedInt(final byte[] b, final int offset) {
        return getInt(b, offset) & 0x00000000FFFFFFFFL;
    }

    /**
    * Writes a big-endian 4-byte int at the begining of the given array.
    * @param b The array to write to.
    * @param n An integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static void setInt(final byte[] b, final int n) {
        setInt(b, n, 0);
    }

    /**
    * Writes a big-endian 4-byte int at an offset in the given array.
    * @param b The array to write to.
    * @param offset The offset in the array to start writing at.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static void setInt(final byte[] b, final int n, final int offset) {
        b[offset + 0] = (byte) (n >>> 24);
        b[offset + 1] = (byte) (n >>> 16);
        b[offset + 2] = (byte) (n >>> 8);
        b[offset + 3] = (byte) (n >>> 0);
    }

    /**
    * Creates a new byte array containing a big-endian 4-byte integer.
    * @param n An integer.
    * @return A new byte array containing the given value.
    */
    public static byte[] fromInt(final int n) {
        final byte[] b = new byte[4];
        setInt(b, n);
        return b;
    }

    /**
    * Reads a big-endian 8-byte long from the begining of the given array.
    * @param b The array to read from.
    * @return A long integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static long getLong(final byte[] b) {
        return getLong(b, 0);
    }

    /**
    * Reads a big-endian 8-byte long from an offset in the given array.
    * @param b The array to read from.
    * @param offset The offset in the array to start reading from.
    * @return A long integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static long getLong(final byte[] b, final int offset) {
        return (b[offset + 0] & 0xFFL) << 56 | (b[offset + 1] & 0xFFL) << 48
                | (b[offset + 2] & 0xFFL) << 40 | (b[offset + 3] & 0xFFL) << 32
                | (b[offset + 4] & 0xFFL) << 24 | (b[offset + 5] & 0xFFL) << 16
                | (b[offset + 6] & 0xFFL) << 8 | (b[offset + 7] & 0xFFL) << 0;
    }

    /**
    * Writes a big-endian 8-byte long at the begining of the given array.
    * @param b The array to write to.
    * @param n A long integer.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static void setLong(final byte[] b, final long n) {
        setLong(b, n, 0);
    }

    /**
    * Writes a big-endian 8-byte long at an offset in the given array.
    * @param b The array to write to.
    * @param offset The offset in the array to start writing at.
    * @throws IndexOutOfBoundsException if the byte array is too small.
    */
    public static void setLong(final byte[] b, final long n, final int offset) {
        b[offset + 0] = (byte) (n >>> 56);
        b[offset + 1] = (byte) (n >>> 48);
        b[offset + 2] = (byte) (n >>> 40);
        b[offset + 3] = (byte) (n >>> 32);
        b[offset + 4] = (byte) (n >>> 24);
        b[offset + 5] = (byte) (n >>> 16);
        b[offset + 6] = (byte) (n >>> 8);
        b[offset + 7] = (byte) (n >>> 0);
    }

    /**
    * Creates a new byte array containing a big-endian 8-byte long integer.
    * @param n A long integer.
    * @return A new byte array containing the given value.
    */
    public static byte[] fromLong(final long n) {
        final byte[] b = new byte[8];
        setLong(b, n);
        return b;
    }

    // ---------------------------- //
    // Pretty-printing byte arrays. //
    // ---------------------------- //

    private static final byte[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F' };

    // This doesn't really belong here but it doesn't belong anywhere else
    // either, so let's put it close to the other pretty-printing functions.
    /**
    * Pretty-prints a {@code long} into a fixed-width hexadecimal number.
    * @return A string of the form {@code 0x0123456789ABCDEF}.
    */
    public static String hex(long v) {
        final byte[] buf = new byte[2 + 16];
        buf[0] = '0';
        buf[1] = 'x';
        int i = 2 + 16;
        do {
            buf[--i] = HEX[(int) v & 0x0F];
            v >>>= 4;
        } while (v != 0);
        for (/**/; i > 1; i--) {
            buf[i] = '0';
        }
        return new String(buf);
    }

    /**
    * {@code memcmp} in Java, hooray.
    * @param a First non-{@code null} byte array to compare.
    * @param b Second non-{@code null} byte array to compare.
    * @return 0 if the two arrays are identical, otherwise the difference
    * between the first two different bytes, otherwise the different between
    * their lengths.
    */
    public static int memcmp(final byte[] a, final byte[] b) {
        final int length = Math.min(a.length, b.length);
        if (a == b) { // Do this after accessing a.length and b.length
            return 0; // in order to NPE if either a or b is null.
        }
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return (a[i] & 0xFF) - (b[i] & 0xFF); // "promote" to unsigned.
            }
        }
        return a.length - b.length;
    }

    /**
    * Tests whether two byte arrays have the same contents.
    * @param a First non-{@code null} byte array to compare.
    * @param b Second non-{@code null} byte array to compare.
    * @return {@code true} if the two arrays are identical,
    * {@code false} otherwise.
    */
    public static boolean equals(final byte[] a, final byte[] b) {
        return memcmp(a, b) == 0;
    }

}