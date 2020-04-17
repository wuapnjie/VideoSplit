package com.xiaopo.flying.glkit.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * @author wupanjie
 */
public final class BufferUtil {
    private BufferUtil() {
        //no instance
    }

    public static ByteBuffer createByteBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static ShortBuffer createShortBuffer(int size) {
        return createByteBuffer(size << 1).asShortBuffer();
    }

    public static CharBuffer createCharBuffer(int size) {
        return createByteBuffer(size << 1).asCharBuffer();
    }

    public static IntBuffer createIntBuffer(int size) {
        return createByteBuffer(size << 2).asIntBuffer();
    }

    public static LongBuffer createLongBuffer(int size) {
        return createByteBuffer(size << 3).asLongBuffer();
    }

    public static FloatBuffer createFloatBuffer(int size) {
        return createByteBuffer(size << 2).asFloatBuffer();
    }

    public static DoubleBuffer createDoubleBuffer(int size) {
        return createByteBuffer(size << 3).asDoubleBuffer();
    }

    public static ByteBuffer storeDataInBuffer(byte[] data) {
        ByteBuffer buffer = BufferUtil.createByteBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    public static IntBuffer storeDataInBuffer(int[] data) {
        IntBuffer buffer = BufferUtil.createIntBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer storeDataInBuffer(float[] data) {
        FloatBuffer buffer = BufferUtil.createFloatBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    public static ShortBuffer storeDataInBuffer(short[] data) {
        ShortBuffer buffer = BufferUtil.createShortBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    public static CharBuffer storeDataInBuffer(char[] data) {
        CharBuffer buffer = BufferUtil.createCharBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    public static LongBuffer storeDataInBuffer(long[] data) {
        LongBuffer buffer = BufferUtil.createLongBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    public static DoubleBuffer storeDataInBuffer(double[] data) {
        DoubleBuffer buffer = BufferUtil.createDoubleBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }
}
