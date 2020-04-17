package com.xiaopo.flying.videosplit;

import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.Nullable;

import com.xiaopo.flying.glkit.filter.NoFilter;
import com.xiaopo.flying.glkit.filter.ShaderFilter;
import com.xiaopo.flying.glkit.gl.BufferUtil;
import com.xiaopo.flying.glkit.gl.ShaderProgram;
import com.xiaopo.flying.puzzlekit.Area;
import com.xiaopo.flying.puzzlekit.PuzzleLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;

import static com.xiaopo.flying.glkit.filter.ShaderFilter.MATRIX_UNIFORM;
import static com.xiaopo.flying.glkit.filter.ShaderFilter.POSITION_ATTRIBUTE;
import static com.xiaopo.flying.glkit.filter.ShaderFilter.TEXTURE_MATRIX_UNIFORM;
import static com.xiaopo.flying.glkit.filter.ShaderFilter.TEXTURE_SAMPLER_UNIFORM;

/**
 * @author wupanjie
 */
public class SplitShaderProgram extends ShaderProgram {
    private static final String TAG = "SplitShaderProgram";
    private static final int COORDS_PER_VERTEX = 2;
    private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * FLOAT_SIZE;

    private static final float vertexCoordinates[] = {
        0, 0,
        1, 0,
        0, 1,
        1, 1,
    };
    private int vertexBufferId;

    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];

    private ArrayList<VideoPiece> videoPieces = new ArrayList<>();
    private ArrayList<ShaderFilter> shaderFilters = new ArrayList<>();
    private PuzzleLayout puzzleLayout;
    private HashMap<Class<? extends ShaderFilter>, ShaderFilter> shaderFilterCache = new HashMap<>();

    private ShaderFilter noFilter = new NoFilter();

    private VideoPiece shortestDurationPiece;
    private VideoPiece longestDurationPiece;

    private float colorRed;
    private float colorBlue;
    private float colorGreen;

    public void setPuzzleLayout(PuzzleLayout puzzleLayout) {
        this.puzzleLayout = puzzleLayout;
    }

    public void addPiece(final String path) {
        addPiece(path, noFilter);
    }

    public void addPiece(final String path, ShaderFilter filter) {
        Class<? extends ShaderFilter> filterClass = filter.getClass();
        ShaderFilter cached = shaderFilterCache.get(filterClass);
        if (cached == null) {
            shaderFilterCache.put(filterClass, filter);
            cached = filter;
        }
        videoPieces.add(new VideoPiece(path));
        shaderFilters.add(cached);
    }

    public void addPiece(final String path, Class<? extends ShaderFilter> filterClass) {
        ShaderFilter cached = shaderFilterCache.get(filterClass);
        if (cached == null) {
            try {
                cached = filterClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            shaderFilterCache.put(filterClass, cached);
        }
        videoPieces.add(new VideoPiece(path));
        shaderFilters.add(cached);
    }

    public void setBackgroundColor(int backgroundColor) {
        this.colorRed = Color.red(backgroundColor) / 255f;
        this.colorBlue = Color.blue(backgroundColor) / 255f;
        this.colorGreen = Color.green(backgroundColor) / 255f;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends ShaderFilter> T getShaderFilter(Class<T> filterClass) {
        return (T) shaderFilterCache.get(filterClass);
    }

    @Override
    public void prepare() {
        // prepare filter
        for (Class<? extends ShaderFilter> key : shaderFilterCache.keySet()) {
            ShaderFilter filter = shaderFilterCache.get(key);
            filter.prepare();
        }

        vertexBufferId = uploadBuffer(BufferUtil.storeDataInBuffer(vertexCoordinates));

        final int size = videoPieces.size();
        int[] textureIds = new int[size];
        generateTextures(size, textureIds, 0);
        CyclicBarrier cyclicBarrier = new CyclicBarrier(size);
        for (int i = 0; i < size; i++) {
            videoPieces.get(i).configOutput(textureIds[i]);
            videoPieces.get(i).setBarrier(cyclicBarrier);
        }

        shortestDurationPiece = Collections.min(videoPieces, new Comparator<VideoPiece>() {
            @Override
            public int compare(VideoPiece o1, VideoPiece o2) {
                return Long.compare(o1.getVideoDuration(), o2.getVideoDuration());
            }
        });

        longestDurationPiece = Collections.max(videoPieces, new Comparator<VideoPiece>() {
            @Override
            public int compare(VideoPiece o1, VideoPiece o2) {
                return Long.compare(o1.getVideoDuration(), o2.getVideoDuration());
            }
        });

        Matrix.setIdentityM(projectionMatrix, 0);
        Matrix.orthoM(projectionMatrix, 0, 0, getViewportWidth(), 0, getViewportHeight(), -1, 1);

        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.translateM(viewMatrix, 0, 0, getViewportHeight(), 0);
        Matrix.scaleM(viewMatrix, 0, 1, -1, 1);
    }

    public boolean isFinished() {
        for (VideoPiece videoPiece : videoPieces) {
            if (!videoPiece.isFinished()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        GLES20.glClearColor(colorRed, colorGreen, colorBlue, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glViewport(0, 0, getViewportWidth(), getViewportHeight());

        final int areaCount = puzzleLayout.getAreaCount();
        final int pieceCount = videoPieces.size();
        for (int i = 0; i < areaCount; i++) {
            Area area = puzzleLayout.getArea(i);

            ShaderFilter filter = shaderFilters.get(i % pieceCount);
            filter.activate();
            final int textureHandle = filter.getParameterHandle(TEXTURE_SAMPLER_UNIFORM);
            final int textureMatrixHandle = filter.getParameterHandle(TEXTURE_MATRIX_UNIFORM);
            final int matrixHandle = filter.getParameterHandle(MATRIX_UNIFORM);
            filter.bindUniform();

            VideoPiece piece = videoPieces.get(i % pieceCount);
            piece.setDisplayArea(area);
            piece.setTexture(textureHandle, textureMatrixHandle);
            piece.setMatrix(matrixHandle, viewMatrix, projectionMatrix);
            drawElements(filter);
        }
    }

    @Override
    public void release() {
        super.release();
        for (Class<? extends ShaderFilter> key : shaderFilterCache.keySet()) {
            shaderFilterCache.get(key).release();
        }
        for (VideoPiece videoPiece : videoPieces) {
            videoPiece.release();
        }
        videoPieces.clear();
        shaderFilters.clear();
    }

    private void drawElements(ShaderFilter filter) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);
        GLES20.glVertexAttribPointer(
            filter.getParameterHandle(POSITION_ATTRIBUTE),
            2,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE,
            0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glEnableVertexAttribArray(filter.getParameterHandle(POSITION_ATTRIBUTE));
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCoordinates.length);
        GLES20.glDisableVertexAttribArray(filter.getParameterHandle(POSITION_ATTRIBUTE));
    }

    public long getPresentationTimeUs() {
        return longestDurationPiece.getPresentationTimeUs();
    }

    void updatePreviewTexture() {
        for (VideoPiece videoPiece : videoPieces) {
            videoPiece.getOutputTexture().updateTexImage();
        }
    }

    void setOnFrameAvailableListener(final SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener) {
        for (VideoPiece videoPiece : videoPieces) {
            videoPiece.getOutputTexture().setOnFrameAvailableListener(onFrameAvailableListener);
        }
    }

    void play() {
        for (VideoPiece videoPiece : videoPieces) {
            videoPiece.play();
        }
    }

    public long getShortestVideoPieceDuration() {
        return shortestDurationPiece.getVideoDuration();
    }

    public long getLongestVideoPieceDuration() {
        return longestDurationPiece.getVideoDuration();
    }
}
