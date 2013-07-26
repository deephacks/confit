package org.deephacks.confit.internal.cached;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtils {
    private static Unsafe unsafe;
    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe) unsafeField.get(null);
            // Ensure the unsafe supports list necessary methods to work around the mistake in the latest OpenJDK.
            // https://github.com/netty/netty/issues/1061
            // http://www.mail-archive.com/jdk6-dev@openjdk.java.net/msg00698.html
            try {
                unsafe.getClass().getDeclaredMethod(
                        "copyMemory",
                        new Class[] { Object.class, long.class, Object.class, long.class, long.class });
            } catch (NoSuchMethodError t) {
                throw t;
            }
        } catch (Throwable cause) {
            unsafe = null;
        }

    }

    public static Unsafe getUnsafe() {
        return unsafe;
    }
}
