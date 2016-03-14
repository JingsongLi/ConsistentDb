package com.jslee.consistentDb.table;

import com.jslee.consistentDb.type.TypeComparator;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Created by jslee on 16/3/14.
 */
public class MemTable<K> {

    private ConcurrentSkipListMap<ByteBuf, ByteBuf> map;

    public MemTable(final TypeComparator<K> keyComparator) {
        map = new ConcurrentSkipListMap<>(new Comparator<ByteBuf>() {
            @Override
            public int compare(ByteBuf o1, ByteBuf o2) {
                return keyComparator.compareSerialized(o1, o2);
            }
        });
    }

    public void put(ByteBuf key, ByteBuf value) {
        map.put(key, value);
    }

    public ByteBuf get(ByteBuf key) {
        return map.get(key);
    }

}
