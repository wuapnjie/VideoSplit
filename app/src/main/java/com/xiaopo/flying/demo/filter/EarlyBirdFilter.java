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
public class EarlyBirdFilter extends AbstractShaderFilter {

  private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
      "\n" +
      "precision lowp float;\n" +
      " \n" +
      " varying highp vec2 vTextureCoord;\n" +
      " \n" +
      " uniform samplerExternalOES uTextureSampler;\n" +
      " uniform sampler2D uTextureCurves; //earlyBirdCurves\n" +
      " uniform sampler2D uTextureOverlay; //earlyBirdOverlay\n" +
      " uniform sampler2D uTextureVig; //vig\n" +
      " uniform sampler2D uTextureBlowout; //earlyBirdBlowout\n" +
      " uniform sampler2D uTextureMap; //earlyBirdMap\n" +
      " \n" +
      " const mat3 saturate = mat3(\n" +
      "                            1.210300,\n" +
      "                            -0.089700,\n" +
      "                            -0.091000,\n" +
      "                            -0.176100,\n" +
      "                            1.123900,\n" +
      "                            -0.177400,\n" +
      "                            -0.034200,\n" +
      "                            -0.034200,\n" +
      "                            1.265800);\n" +
      " const vec3 rgbPrime = vec3(0.25098, 0.14640522, 0.0); \n" +
      " const vec3 desaturate = vec3(.3, .59, .11);\n" +
      " \n" +
      " void main()\n" +
      " {\n" +
      "     \n" +
      "     vec3 texel = texture2D(uTextureSampler, vTextureCoord).rgb;\n" +
      "     \n" +
      "     \n" +
      "     vec2 lookup;    \n" +
      "     lookup.y = 0.5;\n" +
      "     \n" +
      "     lookup.x = texel.r;\n" +
      "     texel.r = texture2D(uTextureCurves, lookup).r;\n" +
      "     \n" +
      "     lookup.x = texel.g;\n" +
      "     texel.g = texture2D(uTextureCurves, lookup).g;\n" +
      "     \n" +
      "     lookup.x = texel.b;\n" +
      "     texel.b = texture2D(uTextureCurves, lookup).b;\n" +
      "     \n" +
      "     float desaturatedColor;\n" +
      "     vec3 result;\n" +
      "     desaturatedColor = dot(desaturate, texel);\n" +
      "     \n" +
      "     \n" +
      "     lookup.x = desaturatedColor;\n" +
      "     result.r = texture2D(uTextureOverlay, lookup).r;\n" +
      "     lookup.x = desaturatedColor;\n" +
      "     result.g = texture2D(uTextureOverlay, lookup).g;\n" +
      "     lookup.x = desaturatedColor;\n" +
      "     result.b = texture2D(uTextureOverlay, lookup).b;\n" +
      "     \n" +
      "     texel = saturate * mix(texel, result, .5);\n" +
      "     \n" +
      "     vec2 tc = (2.0 * vTextureCoord) - 1.0;\n" +
      "     float d = dot(tc, tc);\n" +
      "     \n" +
      "     vec3 sampled;\n" +
      "     lookup.y = .5;\n" +
      "     \n" +
      "     /*\n" +
      "      lookup.x = texel.r;\n" +
      "      sampled.r = texture2D(uTextureVig, lookup).r;\n" +
      "      \n" +
      "      lookup.x = texel.g;\n" +
      "      sampled.g = texture2D(uTextureVig, lookup).g;\n" +
      "      \n" +
      "      lookup.x = texel.b;\n" +
      "      sampled.b = texture2D(uTextureVig, lookup).b;\n" +
      "      \n" +
      "      float value = smoothstep(0.0, 1.25, pow(d, 1.35)/1.65);\n" +
      "      texel = mix(texel, sampled, value);\n" +
      "      */\n" +
      "     \n" +
      "     //---\n" +
      "     \n" +
      "     lookup = vec2(d, texel.r);\n" +
      "     texel.r = texture2D(uTextureVig, lookup).r;\n" +
      "     lookup.y = texel.g;\n" +
      "     texel.g = texture2D(uTextureVig, lookup).g;\n" +
      "     lookup.y = texel.b;\n" +
      "     texel.b    = texture2D(uTextureVig, lookup).b;\n" +
      "     float value = smoothstep(0.0, 1.25, pow(d, 1.35)/1.65);\n" +
      "     \n" +
      "     //---\n" +
      "     \n" +
      "     lookup.x = texel.r;\n" +
      "     sampled.r = texture2D(uTextureBlowout, lookup).r;\n" +
      "     lookup.x = texel.g;\n" +
      "     sampled.g = texture2D(uTextureBlowout, lookup).g;\n" +
      "     lookup.x = texel.b;\n" +
      "     sampled.b = texture2D(uTextureBlowout, lookup).b;\n" +
      "     texel = mix(sampled, texel, value);\n" +
      "     \n" +
      "     \n" +
      "     lookup.x = texel.r;\n" +
      "     texel.r = texture2D(uTextureMap, lookup).r;\n" +
      "     lookup.x = texel.g;\n" +
      "     texel.g = texture2D(uTextureMap, lookup).g;\n" +
      "     lookup.x = texel.b;\n" +
      "     texel.b = texture2D(uTextureMap, lookup).b;\n" +
      "     \n" +
      "     gl_FragColor = vec4(texel, 1.0);\n" +
      " }";

  private Bitmap curves;
  private Bitmap overlay;
  private Bitmap vig;
  private Bitmap blowout;
  private Bitmap map;

  private int curvesTextureId;
  private int overlayTextureId;
  private int vigTextureId;
  private int blowoutTextureId;
  private int mapTextureId;

  public EarlyBirdFilter(Context context) {
    curves = BitmapFactory.decodeResource(context.getResources(), R.drawable.earlybird_curves);
    overlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.earlybird_overlay_map);
    vig = BitmapFactory.decodeResource(context.getResources(), R.drawable.vignette_map);
    blowout = BitmapFactory.decodeResource(context.getResources(), R.drawable.earlybird_blowout);
    map = BitmapFactory.decodeResource(context.getResources(), R.drawable.earlybird_map);
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
    curvesTextureId = loadTexture(curves, NO_TEXTURE, true);
    overlayTextureId = loadTexture(overlay, NO_TEXTURE, true);
    vigTextureId = loadTexture(vig, NO_TEXTURE, true);
    blowoutTextureId = loadTexture(blowout, NO_TEXTURE, true);
    mapTextureId = loadTexture(map, NO_TEXTURE, true);
  }

  @Override
  public void bindUniform() {
    GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, curvesTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureCurves"), 3);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureOverlay"), 4);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vigTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureVig"), 5);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE6);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blowoutTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureBlowout"), 6);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE7);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mapTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureMap"), 7);
  }
}
