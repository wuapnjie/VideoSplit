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
public class SutroFilter extends AbstractShaderFilter {

  private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
      "\n" +
      "precision lowp float;\n" +
      " varying highp vec2 vTextureCoord;\n" +
      " \n" +
      " uniform samplerExternalOES uTextureSampler;\n" +
      " uniform sampler2D uTextureMap; //sutroMap;\n" +
      " uniform sampler2D uTextureMetal; //sutroMetal;\n" +
      " uniform sampler2D uTextureSoftLight; //softLight\n" +
      " uniform sampler2D uTextureBurn; //sutroEdgeburn\n" +
      " uniform sampler2D uTextureCurves; //sutroCurves\n" +
      " \n" +
      " void main()\n" +
      " {\n" +
      "     \n" +
      "     vec3 texel = texture2D(uTextureSampler, vTextureCoord).rgb;\n" +
      "     \n" +
      "     vec2 tc = (2.0 * vTextureCoord) - 1.0;\n" +
      "     float d = dot(tc, tc);\n" +
      "     vec2 lookup = vec2(d, texel.r);\n" +
      "     texel.r = texture2D(uTextureMap, lookup).r;\n" +
      "     lookup.y = texel.g;\n" +
      "     texel.g = texture2D(uTextureMap, lookup).g;\n" +
      "     lookup.y = texel.b;\n" +
      "     texel.b    = texture2D(uTextureMap, lookup).b;\n" +
      "     \n" +
      "     vec3 rgbPrime = vec3(0.1019, 0.0, 0.0); \n" +
      "     float m = dot(vec3(.3, .59, .11), texel.rgb) - 0.03058;\n" +
      "     texel = mix(texel, rgbPrime + m, 0.32);\n" +
      "     \n" +
      "     vec3 metal = texture2D(uTextureMetal, vTextureCoord).rgb;\n" +
      "     texel.r = texture2D(uTextureSoftLight, vec2(metal.r, texel.r)).r;\n" +
      "     texel.g = texture2D(uTextureSoftLight, vec2(metal.g, texel.g)).g;\n" +
      "     texel.b = texture2D(uTextureSoftLight, vec2(metal.b, texel.b)).b;\n" +
      "     \n" +
      "     texel = texel * texture2D(uTextureBurn, vTextureCoord).rgb;\n" +
      "     \n" +
      "     texel.r = texture2D(uTextureCurves, vec2(texel.r, .16666)).r;\n" +
      "     texel.g = texture2D(uTextureCurves, vec2(texel.g, .5)).g;\n" +
      "     texel.b = texture2D(uTextureCurves, vec2(texel.b, .83333)).b;\n" +
      "     \n" +
      "     \n" +
      "     gl_FragColor = vec4(texel, 1.0);\n" +
      " }";

  private Bitmap map;
  private Bitmap metal;
  private Bitmap softLight;
  private Bitmap burn;
  private Bitmap curves;

  private int mapTextureId;
  private int metalTextureId;
  private int softLightTextureId;
  private int burnTextureId;
  private int curvesTextureId;

  public SutroFilter(Context context) {
    map = BitmapFactory.decodeResource(context.getResources(), R.drawable.vignette_map);
    metal = BitmapFactory.decodeResource(context.getResources(), R.drawable.sutro_metal);
    softLight = BitmapFactory.decodeResource(context.getResources(), R.drawable.soft_light);
    burn = BitmapFactory.decodeResource(context.getResources(), R.drawable.sutro_edge_burn);
    curves = BitmapFactory.decodeResource(context.getResources(), R.drawable.sutro_curves);
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
    metalTextureId = loadTexture(metal, NO_TEXTURE, true);
    softLightTextureId = loadTexture(softLight, NO_TEXTURE, true);
    burnTextureId = loadTexture(burn, NO_TEXTURE, true);
    curvesTextureId = loadTexture(curves, NO_TEXTURE, true);
  }

  @Override
  public void bindUniform() {
    GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mapTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureMap"), 3);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, metalTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureMetal"), 4);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, softLightTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureSoftLight"), 5);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE6);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, burnTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureBurn"), 6);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE7);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, curvesTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureCurves"), 7);
  }
}
