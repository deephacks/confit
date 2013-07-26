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
package org.deephacks.confit.internal.cached;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.deephacks.cached.CacheValueSerializer;
import org.deephacks.cached.buffer.ByteBuf;
import org.deephacks.cached.buffer.ByteBufOutputStream;
import org.deephacks.cached.buffer.Unpooled;
import org.deephacks.cached.buffer.util.internal.chmv8.ConcurrentHashMapV8;
import org.deephacks.confit.internal.cached.KryoSerializers.URISerializer;
import org.deephacks.confit.internal.cached.KryoSerializers.URLSerializer;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Kryo based serializer that read and write object proxies to a binary
 * off-heap cache. Note that DefaultCacheValueSerializer is roughly twice
 * as fast Kryo.
 *
 * Each class is given an integer id which is used to correlate how to read objects
 * from a byte buffer without actually writing the whole class into the buffer.
 */
public class KryoCacheValueSerializer extends CacheValueSerializer<Object> {

    /** able to write and read object without no-arg constructors */
    private static final KryoReflectionFactorySupport kryo = new KryoReflectionFactorySupport();

    /** keeps track of class -> id */
    private static final ConcurrentHashMapV8<Class<?>, Integer> classToId = new ConcurrentHashMapV8<>();

    /** keeps track of id -> class */
    private static final ConcurrentHashMapV8<Integer, Class<?>> idToClass = new ConcurrentHashMapV8<>();

    /** maintain unique ids to classes */
    private static final AtomicInteger clsCount = new AtomicInteger(0);


    static {
        // kryo cannot serialize URL and URI classes with a little help
        kryo.addDefaultSerializer(URL.class, new URLSerializer());
        kryo.addDefaultSerializer(URI.class, new URISerializer());
    }

    @Override
    public ByteBuf write(Object value) {
        Class<?> cls = value.getClass();
        Integer id = classToId.get(cls);
        if(id == null) {
            synchronized (kryo) {
                id = clsCount.incrementAndGet();
                classToId.put(cls, id);
                idToClass.put(id, cls);
            }
        }
        ByteBuf buf = Unpooled.directBuffer();
        buf.writeInt(id);
        ByteBufOutputStream byteBufOutput = new ByteBufOutputStream(buf);
        Output output = new Output(byteBufOutput);
        kryo.writeObject(output, value);
        output.flush();
        return buf;
    }

    @Override
    public Object read(ByteBuf buf) {
        buf.resetReaderIndex();
        int id = buf.readInt();
        int readable = buf.readableBytes();
        byte[] bytes = new byte[readable];
        buf.readBytes(bytes);
        Input input = new Input(bytes);
        return kryo.readObject(input, idToClass.get(id));
    }
}
