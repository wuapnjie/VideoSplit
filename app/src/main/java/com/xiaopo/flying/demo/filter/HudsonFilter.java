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
public class HudsonFilter extends AbstractShaderFilter {

    private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
        "\n" +
        "precision lowp float;\n" +
        " \n" +
        "varying highp vec2 vTextureCoord;\n" +
        " \n" +
        "uniform samplerExternalOES uTextureSampler;\n" +
        "uniform sampler2D uTextureBlowout; //blowout;\n" +
        "uniform sampler2D uTextureOverlay; //overlay;\n" +
        "uniform sampler2D uTextureMap; //map\n" +
        " \n" +
        "void main()\n" +
        "{\n" +
        "     \n" +
        "    vec4 texel = texture2D(uTextureSampler, vTextureCoord);\n" +
        "    vec3 bbTexel = texture2D(uTextureBlowout, vTextureCoord).rgb;\n" +
        "     \n" +
        "    texel.r = texture2D(uTextureOverlay, vec2(bbTexel.r, texel.r)).r;\n" +
        "    texel.g = texture2D(uTextureOverlay, vec2(bbTexel.g, texel.g)).g;\n" +
        "    texel.b = texture2D(uTextureOverlay, vec2(bbTexel.b, texel.b)).b;\n" +
        "     \n" +
        "    vec4 mapped;\n" +
        "    mapped.r = texture2D(uTextureMap, vec2(texel.r, .16666)).r;\n" +
        "    mapped.g = texture2D(uTextureMap, vec2(texel.g, .5)).g;\n" +
        "    mapped.b = texture2D(uTextureMap, vec2(texel.b, .83333)).b;\n" +
        "    mapped.a = 1.0;\n" +
        "     \n" +
        "    gl_FragColor = mapped;\n" +
        "}\n";

    private Bitmap blowout;
    private Bitmap overlay;
    private Bitmap map;

    private int blowoutTextureId;
    private int overlayTextureId;
    private int mapTextureId;

    public HudsonFilter(Context context) {
        blowout = BitmapFactory.decodeResource(context.getResources(), R.drawable.hudson_background);
        overlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.overlay_map);
        map = BitmapFactory.decodeResource(context.getResources(), R.drawable.hudson_map);
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
        blowoutTextureId = loadTexture(blowout, NO_TEXTURE, true);
        overlayTextureId = loadTexture(overlay, NO_TEXTURE, true);
        mapTextureId = loadTexture(map, NO_TEXTURE, true);
    }

    @Override
    public void bindUniform() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blowoutTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureBlowout"), 3);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureOverlay"), 4);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mapTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureMap"), 5);
    }
}
