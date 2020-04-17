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
public class ValenciaFilter extends AbstractShaderFilter {

    private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
        "\n" +
        "precision lowp float;\n" +
        " varying highp vec2 vTextureCoord;\n" +
        " \n" +
        " uniform samplerExternalOES uTextureSampler;\n" +
        " uniform sampler2D uTextureMap; //map\n" +
        " uniform sampler2D uTextureGradMap; //gradMap\n" +
        " \n" +
        " mat3 saturateMatrix = mat3(\n" +
        "                            1.1402,\n" +
        "                            -0.0598,\n" +
        "                            -0.061,\n" +
        "                            -0.1174,\n" +
        "                            1.0826,\n" +
        "                            -0.1186,\n" +
        "                            -0.0228,\n" +
        "                            -0.0228,\n" +
        "                            1.1772);\n" +
        " \n" +
        " vec3 lumaCoeffs = vec3(.3, .59, .11);\n" +
        " \n" +
        " void main()\n" +
        " {\n" +
        "     vec3 texel = texture2D(uTextureSampler, vTextureCoord).rgb;\n" +
        "     \n" +
        "     texel = vec3(\n" +
        "                  texture2D(uTextureMap, vec2(texel.r, .1666666)).r,\n" +
        "                  texture2D(uTextureMap, vec2(texel.g, .5)).g,\n" +
        "                  texture2D(uTextureMap, vec2(texel.b, .8333333)).b\n" +
        "                  );\n" +
        "     \n" +
        "     texel = saturateMatrix * texel;\n" +
        "     float luma = dot(lumaCoeffs, texel);\n" +
        "     texel = vec3(\n" +
        "                  texture2D(uTextureGradMap, vec2(luma, texel.r)).r,\n" +
        "                  texture2D(uTextureGradMap, vec2(luma, texel.g)).g,\n" +
        "                  texture2D(uTextureGradMap, vec2(luma, texel.b)).b);\n" +
        "     \n" +
        "     gl_FragColor = vec4(texel, 1.0);\n" +
        " }\n";

    private Bitmap map;
    private Bitmap gradMap;

    private int mapTextureId;
    private int gradMapTextureId;

    public ValenciaFilter(Context context) {
        map = BitmapFactory.decodeResource(context.getResources(), R.drawable.valencia_map);
        gradMap = BitmapFactory.decodeResource(context.getResources(), R.drawable.valencia_gradient_map);
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
        mapTextureId = loadTexture(map, NO_TEXTURE, true);
        gradMapTextureId = loadTexture(gradMap, NO_TEXTURE, true);
    }

    @Override
    public void bindUniform() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mapTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureMap"), 3);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gradMapTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureGradMap"), 4);
    }
}
