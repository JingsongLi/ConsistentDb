package com.jslee.consistentDb.memory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * Created by jslee on 16/3/14.
 */
public class BufferFactory {

    private static PooledByteBufAllocator bufAllocator = PooledByteBufAllocator.DEFAULT;

    public static ByteBuf buffer(int size) {
        return bufAllocator.buffer(size);
    }

    public static void main(String[] args) {
    }
}
