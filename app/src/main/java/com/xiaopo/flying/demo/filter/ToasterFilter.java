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
public class ToasterFilter extends AbstractShaderFilter {

    private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
        "\n" +
        "precision lowp float;\n" +
        " varying highp vec2 vTextureCoord;\n" +
        " \n" +
        " uniform samplerExternalOES uTextureSampler;\n" +
        " uniform sampler2D uTextureMetal; //toasterMetal\n" +
        " uniform sampler2D uTextureSoftLight; //toasterSoftlight\n" +
        " uniform sampler2D uTextureCurves; //toasterCurves\n" +
        " uniform sampler2D uTextureOverlayMap; //toasterOverlayMapWarm\n" +
        " uniform sampler2D uTextureColorShift; //toasterColorshift\n" +
        " \n" +
        " void main()\n" +
        " {\n" +
        "     lowp vec3 texel;\n" +
        "     mediump vec2 lookup;\n" +
        "     vec2 blue;\n" +
        "     vec2 green;\n" +
        "     vec2 red;\n" +
        "     lowp vec4 tmpvar_1;\n" +
        "     tmpvar_1 = texture2D (uTextureSampler, vTextureCoord);\n" +
        "     texel = tmpvar_1.xyz;\n" +
        "     lowp vec4 tmpvar_2;\n" +
        "     tmpvar_2 = texture2D (uTextureMetal, vTextureCoord);\n" +
        "     lowp vec2 tmpvar_3;\n" +
        "     tmpvar_3.x = tmpvar_2.x;\n" +
        "     tmpvar_3.y = tmpvar_1.x;\n" +
        "     texel.x = texture2D (uTextureSoftLight, tmpvar_3).x;\n" +
        "     lowp vec2 tmpvar_4;\n" +
        "     tmpvar_4.x = tmpvar_2.y;\n" +
        "     tmpvar_4.y = tmpvar_1.y;\n" +
        "     texel.y = texture2D (uTextureSoftLight, tmpvar_4).y;\n" +
        "     lowp vec2 tmpvar_5;\n" +
        "     tmpvar_5.x = tmpvar_2.z;\n" +
        "     tmpvar_5.y = tmpvar_1.z;\n" +
        "     texel.z = texture2D (uTextureSoftLight, tmpvar_5).z;\n" +
        "     red.x = texel.x;\n" +
        "     red.y = 0.16666;\n" +
        "     green.x = texel.y;\n" +
        "     green.y = 0.5;\n" +
        "     blue.x = texel.z;\n" +
        "     blue.y = 0.833333;\n" +
        "     texel.x = texture2D (uTextureCurves, red).x;\n" +
        "     texel.y = texture2D (uTextureCurves, green).y;\n" +
        "     texel.z = texture2D (uTextureCurves, blue).z;\n" +
        "     mediump vec2 tmpvar_6;\n" +
        "     tmpvar_6 = ((2.0 * vTextureCoord) - 1.0);\n" +
        "     mediump vec2 tmpvar_7;\n" +
        "     tmpvar_7.x = dot (tmpvar_6, tmpvar_6);\n" +
        "     tmpvar_7.y = texel.x;\n" +
        "     lookup = tmpvar_7;\n" +
        "     texel.x = texture2D (uTextureOverlayMap, tmpvar_7).x;\n" +
        "     lookup.y = texel.y;\n" +
        "     texel.y = texture2D (uTextureOverlayMap, lookup).y;\n" +
        "     lookup.y = texel.z;\n" +
        "     texel.z = texture2D (uTextureOverlayMap, lookup).z;\n" +
        "     red.x = texel.x;\n" +
        "     green.x = texel.y;\n" +
        "     blue.x = texel.z;\n" +
        "     texel.x = texture2D (uTextureColorShift, red).x;\n" +
        "     texel.y = texture2D (uTextureColorShift, green).y;\n" +
        "     texel.z = texture2D (uTextureColorShift, blue).z;\n" +
        "     lowp vec4 tmpvar_8;\n" +
        "     tmpvar_8.w = 1.0;\n" +
        "     tmpvar_8.xyz = texel;\n" +
        "     gl_FragColor = tmpvar_8;\n" +
        " }";

    private Bitmap metal;
    private Bitmap softLight;
    private Bitmap curves;
    private Bitmap overlayMap;
    private Bitmap colorShift;

    private int metalTextureId;
    private int softLightTextureId;
    private int curvesTextureId;
    private int overlayMapTextureId;
    private int colorShiftTextureId;

    public ToasterFilter(Context context) {
        metal = BitmapFactory.decodeResource(context.getResources(), R.drawable.toaster_metal);
        softLight = BitmapFactory.decodeResource(context.getResources(), R.drawable.toaster_soft_light);
        curves = BitmapFactory.decodeResource(context.getResources(), R.drawable.toaster_curves);
        overlayMap = BitmapFactory.decodeResource(context.getResources(), R.drawable.toaster_overlay_map_warm);
        colorShift = BitmapFactory.decodeResource(context.getResources(), R.drawable.toaster_color_shift);
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
        metalTextureId = loadTexture(metal, NO_TEXTURE, true);
        softLightTextureId = loadTexture(softLight, NO_TEXTURE, true);
        curvesTextureId = loadTexture(curves, NO_TEXTURE, true);
        overlayMapTextureId = loadTexture(overlayMap, NO_TEXTURE, true);
        colorShiftTextureId = loadTexture(colorShift, NO_TEXTURE, true);
    }

    @Override
    public void bindUniform() {

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, metalTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureMetal"), 3);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, softLightTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureSoftLight"), 4);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, curvesTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureCurves"), 5);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE6);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayMapTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureOverlayMap"), 6);


        GLES20.glActiveTexture(GLES20.GL_TEXTURE7);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, colorShiftTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureColorShift"), 7);
    }
}
