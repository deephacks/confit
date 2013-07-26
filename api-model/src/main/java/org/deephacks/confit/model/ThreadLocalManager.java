/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.confit.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class ThreadLocalManager {
    private static final ThreadLocal<Map<Class<?>, Stack<Object>>> threadLocal = new ThreadLocal<Map<Class<?>, Stack<Object>>>();

    public static <T> void push(Class<T> cls, T value) {
        Map<Class<?>, Stack<Object>> map = threadLocal.get();
        if (map == null) {
            map = new HashMap<Class<?>, Stack<Object>>();
        }
        Stack<Object> stack = map.get(cls);
        if (stack == null) {
            stack = new Stack<Object>();
        }
        stack.push(value);
        map.put(cls, stack);
        threadLocal.set(map);
    }

    public static <T> T peek(Class<T> cls) {
        Map<Class<?>, Stack<Object>> map = threadLocal.get();
        if (map == null) {
            return null;
        }
        Stack<Object> stack = map.get(cls);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return cls.cast(stack.peek());
    }

    public static <T> T pop(Class<T> cls) {
        Map<Class<?>, Stack<Object>> map = threadLocal.get();
        if (map == null) {
            return null;
        }
        Stack<Object> stack = map.get(cls);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return cls.cast(stack.pop());
    }

    public static <T> void clear(Class<T> cls) {
        Map<Class<?>, Stack<Object>> map = threadLocal.get();
        if (map == null) {
            return;
        }
        map.remove(cls);
    }

    public static void clear() {
        threadLocal.set(null);
    }
}
