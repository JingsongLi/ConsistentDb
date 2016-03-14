package com.jslee.consistentDb.type;


import com.jslee.consistentDb.util.MemoryUtils;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * A simple and efficient deserializer for the {@link java.io.DataInput} interface.
 */
public class DataInputDeserializer implements DataInput {

    @SuppressWarnings("restriction")
    private static final sun.misc.Unsafe UNSAFE = MemoryUtils.UNSAFE;
    @SuppressWarnings("restriction")
    private static final long BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    private static final boolean LITTLE_ENDIAN = (MemoryUtils.NATIVE_BYTE_ORDER == ByteOrder.LITTLE_ENDIAN);
    private byte[] buffer;
    private int end;
    private int position;

    public DataInputDeserializer() {
    }

    public DataInputDeserializer(byte[] buffer, int start, int len) {
        setBuffer(buffer, start, len);
    }

    public DataInputDeserializer(ByteBuffer buffer) {
        setBuffer(buffer);
    }

    // ----------------------------------------------------------------------------------------
    //                               Data Input
    // ----------------------------------------------------------------------------------------

    public void setBuffer(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            this.buffer = buffer.array();
            this.position = buffer.arrayOffset() + buffer.position();
            this.end = this.position + buffer.remaining();
        } else if (buffer.isDirect()) {
            this.buffer = new byte[buffer.remaining()];
            this.position = 0;
            this.end = this.buffer.length;

            buffer.get(this.buffer);
        } else {
            throw new IllegalArgumentException("The given buffer is neither an array-backed heap ByteBuffer, nor a direct ByteBuffer.");
        }
    }

    public void setBuffer(byte[] buffer, int start, int len) {
        if (buffer == null) {
            throw new NullPointerException();
        }

        if (start < 0 || len < 0 || start + len > buffer.length) {
            throw new IllegalArgumentException();
        }

        this.buffer = buffer;
        this.position = start;
        this.end = start + len;
    }

    public void releaseArrays() {
        this.buffer = null;
    }

    @Override
    public boolean readBoolean() throws IOException {
        if (this.position < this.end) {
            return this.buffer[this.position++] != 0;
        } else {
            throw new EOFException();
        }
    }

    @Override
    public byte readByte() throws IOException {
        if (this.position < this.end) {
            return this.buffer[this.position++];
        } else {
            throw new EOFException();
        }
    }

    @Override
    public char readChar() throws IOException {
        if (this.position < this.end - 1) {
            return (char) (((this.buffer[this.position++] & 0xff) << 8) | (this.buffer[this.position++] & 0xff));
        } else {
            throw new EOFException();
        }
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        if (len >= 0) {
            if (off <= b.length - len) {
                if (this.position <= this.end - len) {
                    System.arraycopy(this.buffer, position, b, off, len);
                    position += len;
                } else {
                    throw new EOFException();
                }
            } else {
                throw new ArrayIndexOutOfBoundsException();
            }
        } else if (len < 0) {
            throw new IllegalArgumentException("Length may not be negative.");
        }
    }

    @Override
    public int readInt() throws IOException {
        if (this.position >= 0 && this.position < this.end - 3) {
            @SuppressWarnings("restriction")
            int value = UNSAFE.getInt(this.buffer, BASE_OFFSET + this.position);
            if (LITTLE_ENDIAN) {
                value = Integer.reverseBytes(value);
            }

            this.position += 4;
            return value;
        } else {
            throw new EOFException();
        }
    }

    @Override
    public String readLine() throws IOException {
        if (this.position < this.end) {
            // read until a newline is found
            StringBuilder bld = new StringBuilder();
            char curr = (char) readUnsignedByte();
            while (position < this.end && curr != '\n') {
                bld.append(curr);
                curr = (char) readUnsignedByte();
            }
            // trim a trailing carriage return
            int len = bld.length();
            if (len > 0 && bld.charAt(len - 1) == '\r') {
                bld.setLength(len - 1);
            }
            String s = bld.toString();
            bld.setLength(0);
            return s;
        } else {
            return null;
        }
    }

    @Override
    public long readLong() throws IOException {
        if (position >= 0 && position < this.end - 7) {
            @SuppressWarnings("restriction")
            long value = UNSAFE.getLong(this.buffer, BASE_OFFSET + this.position);
            if (LITTLE_ENDIAN) {
                value = Long.reverseBytes(value);
            }
            this.position += 8;
            return value;
        } else {
            throw new EOFException();
        }
    }

    @Override
    public short readShort() throws IOException {
        if (position >= 0 && position < this.end - 1) {
            return (short) ((((this.buffer[position++]) & 0xff) << 8) | ((this.buffer[position++]) & 0xff));
        } else {
            throw new EOFException();
        }
    }

    @Override
    public String readUTF() throws IOException {
        int utflen = readUnsignedShort();
        byte[] bytearr = new byte[utflen];
        char[] chararr = new char[utflen];

        int c, char2, char3;
        int count = 0;
        int chararr_count = 0;

        readFully(bytearr, 0, utflen);

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            if (c > 127) {
                break;
            }
            count++;
            chararr[chararr_count++] = (char) c;
        }

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                /* 0xxxxxxx */
                    count++;
                    chararr[chararr_count++] = (char) c;
                    break;
                case 12:
                case 13:
                /* 110x xxxx 10xx xxxx */
                    count += 2;
                    if (count > utflen) {
                        throw new UTFDataFormatException("malformed input: partial character at end");
                    }
                    char2 = (int) bytearr[count - 1];
                    if ((char2 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException("malformed input around byte " + count);
                    }
                    chararr[chararr_count++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
                    break;
                case 14:
				/* 1110 xxxx 10xx xxxx 10xx xxxx */
                    count += 3;
                    if (count > utflen) {
                        throw new UTFDataFormatException("malformed input: partial character at end");
                    }
                    char2 = (int) bytearr[count - 2];
                    char3 = (int) bytearr[count - 1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) {
                        throw new UTFDataFormatException("malformed input around byte " + (count - 1));
                    }
                    chararr[chararr_count++] = (char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | (char3 & 0x3F));
                    break;
                default:
				/* 10xx xxxx, 1111 xxxx */
                    throw new UTFDataFormatException("malformed input around byte " + count);
            }
        }
        // The number of chars produced may be less than utflen
        return new String(chararr, 0, chararr_count);
    }

    @Override
    public int readUnsignedByte() throws IOException {
        if (this.position < this.end) {
            return (this.buffer[this.position++] & 0xff);
        } else {
            throw new EOFException();
        }
    }

    @Override
    public int readUnsignedShort() throws IOException {
        if (this.position < this.end - 1) {
            return ((this.buffer[this.position++] & 0xff) << 8) | (this.buffer[this.position++] & 0xff);
        } else {
            throw new EOFException();
        }
    }

    @Override
    public int skipBytes(int n) throws IOException {
        if (this.position <= this.end - n) {
            this.position += n;
            return n;
        } else {
            n = this.end - this.position;
            this.position = this.end;
            return n;
        }
    }

}