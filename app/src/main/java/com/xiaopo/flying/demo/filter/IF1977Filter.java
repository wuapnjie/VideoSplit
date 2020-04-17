package com.xiaopo.flying.demo.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;

import com.xiaopo.flying.demo.R;
import com.xiaopo.flying.glkit.filter.AbstractShaderFilter;

/**
 * @author wupanjie
 */
public class IF1977Filter extends AbstractShaderFilter {
    private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
        "\n" +
        "precision lowp float;\n" +
        " varying highp vec2 vTextureCoord;\n" +
        " \n" +
        " uniform samplerExternalOES uTextureSampler;\n" +
        " uniform sampler2D uTextureMap;\n" +
        " \n" +
        " void main()\n" +
        " {\n" +
        "     \n" +
        "     vec3 texel = texture2D(uTextureSampler, vTextureCoord).rgb;\n" +
        "     \n" +
        "     texel = vec3(\n" +
        "                  texture2D(uTextureMap, vec2(texel.r, .16666)).r,\n" +
        "                  texture2D(uTextureMap, vec2(texel.g, .5)).g,\n" +
        "                  texture2D(uTextureMap, vec2(texel.b, .83333)).b);\n" +
        "     \n" +
        "     gl_FragColor = vec4(texel, 1.0);\n" +
        " }\n";

    private Bitmap map;

    private int mapTextureId;

    public IF1977Filter(Context context) {
        map = BitmapFactory.decodeResource(context.getResources(), R.drawable.map_1977);
    }

    @Override
    public String getVertexShaderCode() {
        return DEFAULT_VERTEX_SHADER_CODE;
    }

    @Override
    public String getFragmentShaderCode() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void prepareTexture() {
        super.prepareTexture();
        mapTextureId = loadTexture(map, NO_TEXTURE, true);
    }

    @Override
    public void bindUniform() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mapTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureMap"), 3);
    }
}
