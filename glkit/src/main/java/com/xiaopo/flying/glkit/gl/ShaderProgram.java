package com.xiaopo.flying.glkit.gl;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

/**
 * @author wupanjie
 */
public abstract class ShaderProgram {
    private static final String TAG = "ShaderProgram";
    public static final int FLOAT_SIZE = Float.SIZE / Byte.SIZE;
    public static final int SHORT_SIZE = Short.SIZE / Byte.SIZE;
    public static final int BYTE_SIZE = Byte.SIZE;
    public static final int MATRIX_SIZE = 16;

    public static void checkError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }
    }

    private int[] tempId = new int[1];
    private ArrayList<Integer> vboIds = new ArrayList<>();
    private ArrayList<Integer> textureIds = new ArrayList<>();

    private int viewportWidth;
    private int viewportHeight;

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public void setViewport(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    public abstract void prepare();

    public abstract void run();

    public void release() {
        int[] bufferIds = new int[vboIds.size()];
        for (int i = 0; i < bufferIds.length; i++) {
            bufferIds[i] = vboIds.get(i);
        }
        GLES20.glDeleteBuffers(bufferIds.length, bufferIds, 0);

        int[] texIds = new int[textureIds.size()];
        for (int i = 0; i < texIds.length; i++) {
            texIds[i] = textureIds.get(i);
        }
        GLES20.glDeleteTextures(texIds.length, texIds, 0);

        vboIds.clear();
        textureIds.clear();
    }

    // return vboId
    protected int uploadBuffer(FloatBuffer buffer) {
        return uploadBuffer(buffer, FLOAT_SIZE);
    }

    // return vboId
    protected int uploadBuffer(ShortBuffer buffer) {
        return uploadBuffer(buffer, SHORT_SIZE);
    }

    // return vboId
    protected int uploadBuffer(ByteBuffer buffer) {
        return uploadBuffer(buffer, BYTE_SIZE);
    }

    private int uploadBuffer(Buffer buffer, int elementSize) {
        GLES20.glGenBuffers(1, tempId, 0);
        checkError("gen buffer");
        int bufferId = tempId[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId);
        checkError("bind buffer");
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buffer.capacity() * elementSize, buffer,
            GLES20.GL_STATIC_DRAW);
        checkError("buffer data");
        vboIds.add(bufferId);
        return bufferId;
    }

    protected int generateTexture() {
        GLES20.glGenTextures(1, tempId, 0);
        textureIds.add(tempId[0]);
        return tempId[0];
    }

    protected void generateTextures(int size, int[] ids, int offset) {
        GLES20.glGenTextures(size, ids, offset);
        for (int i = 0; i < size; i++) {
            textureIds.add(ids[i]);
        }
    }

}
