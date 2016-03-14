package com.jslee.consistentDb;

import com.jslee.consistentDb.memory.BufferFactory;
import com.jslee.consistentDb.table.MemTable;
import com.jslee.consistentDb.type.DataInputDeserializer;
import com.jslee.consistentDb.type.DataOutputSerializer;
import com.jslee.consistentDb.type.TypeComparator;
import com.jslee.consistentDb.type.TypeSerializer;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

/**
 * Created by jslee on 16/3/14.
 */
public class ConsistentDb<K,V> {

    private TypeComparator<K> keyComparator;
    private TypeSerializer<K> keySerializer;
    private TypeSerializer<V> valueSerializer;
    private ThreadLocal<DataOutputSerializer> outputSerializerLocal;
    private ThreadLocal<DataInputDeserializer> inputDeserializerLocal;

    private volatile MemTable memTable;
    private volatile MemTable immutableMemTable;

    public ConsistentDb(
            TypeComparator<K> keyComparator,
            TypeSerializer<K> keySerializer,
            TypeSerializer<V> valueSerializer) {
        this.keyComparator = keyComparator;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        outputSerializerLocal = new ThreadLocal<>();
        inputDeserializerLocal = new ThreadLocal<>();
        memTable = new MemTable<K>(keyComparator);
    }

    public void put(K k, V v) throws IOException {
        memTable.put(
                serialize(k, keySerializer),
                serialize(v, valueSerializer));
    }

    public V get(K k) throws IOException {
        ByteBuf valueBuf = memTable.get(serialize(k, keySerializer));
        if (valueBuf != null) {
            return deserialize(valueBuf, valueSerializer);
        }
        return null;
    }

    private <T> ByteBuf serialize(T t, TypeSerializer<T> serializer) throws IOException {
        DataOutputSerializer outputSerializer = getThreadLocalSerializer();
        try {
            serializer.serialize(t, outputSerializer);
            ByteBuf buf = BufferFactory.buffer(outputSerializer.length());
            buf.writeBytes(outputSerializer.wrapAsByteBuffer());
            return buf;
        } finally {
            outputSerializer.clear();
        }
    }

    private <T> T deserialize(ByteBuf buf, TypeSerializer<T> serializer) throws IOException {
        DataInputDeserializer deserializer = getThreadLocalDeserializer();
        try {
            deserializer.setBuffer(buf.nioBuffer());
            return serializer.deserialize(deserializer);
        } finally {
            deserializer.releaseArrays();
        }
    }

    private DataOutputSerializer getThreadLocalSerializer() {
        DataOutputSerializer outputSerializer = outputSerializerLocal.get();
        if (outputSerializer == null) {
            outputSerializer = new DataOutputSerializer(128);
            outputSerializerLocal.set(outputSerializer);
        }
        return outputSerializer;
    }

    private DataInputDeserializer getThreadLocalDeserializer() {
        DataInputDeserializer inputDeserializer = inputDeserializerLocal.get();
        if (inputDeserializer == null) {
            inputDeserializer = new DataInputDeserializer();
            inputDeserializerLocal.set(inputDeserializer);
        }
        return inputDeserializer;
    }

}
