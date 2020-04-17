package com.xiaopo.flying.glkit.filter;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.xiaopo.flying.glkit.gl.Shader;
import com.xiaopo.flying.glkit.gl.ShaderParameter;

/**
 * @author wupanjie
 */
public abstract class AbstractShaderFilter implements ShaderFilter {
    private Shader shader;

    @Override
    public void prepare() {
        shader = new Shader.Builder()
            .attachVertex(getVertexShaderCode())
            .attachFragment(getFragmentShaderCode())
            .inflate(ShaderParameter.attribute(POSITION_ATTRIBUTE))
            .inflate(ShaderParameter.uniform(MATRIX_UNIFORM))
            .inflate(ShaderParameter.uniform(TEXTURE_MATRIX_UNIFORM))
            .inflate(ShaderParameter.uniform(TEXTURE_SAMPLER_UNIFORM))
            .assemble();

        prepareTexture();
    }

    public void prepareTexture() {

    }

    @Override
    public int getParameterHandle(final String name) {
        return shader.getParameterHandle(name);
    }

    @Override
    public void activate() {
        shader.activate();
    }

    @Override
    public void release() {
        shader.release();
    }

    protected static final int NO_TEXTURE = -1;

    protected int loadTexture(final Bitmap img, final int usedTexId, final boolean recycle) {
        int textures[] = new int[1];
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, img);
            textures[0] = usedTexId;
        }
        if (recycle) {
            img.recycle();
        }
        return textures[0];
    }
}
