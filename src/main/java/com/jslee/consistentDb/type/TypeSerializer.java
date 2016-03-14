package com.jslee.consistentDb.type;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by jslee on 16/3/14.
 */
public abstract class TypeSerializer<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public abstract void serialize(T record, DataOutput target) throws IOException;

    public abstract T deserialize(DataInput source) throws IOException;

    public abstract T deserialize(T reuse, DataInput source) throws IOException;

}
