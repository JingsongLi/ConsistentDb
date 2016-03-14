package com.jslee.consistentDb.type;

import io.netty.buffer.ByteBuf;

import java.io.DataInput;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by jslee on 16/3/14.
 */
public abstract class TypeComparator<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public abstract int hash(T record);

    public abstract int compare(T first, T second);

    public abstract int compareSerialized(ByteBuf first, ByteBuf second);

}
