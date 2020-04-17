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
public class HefeFilter extends AbstractShaderFilter {

  private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
      "\n" +
      "precision lowp float;\n" +
      " varying highp vec2 vTextureCoord;\n" +
      " \n" +
      " uniform samplerExternalOES uTextureSampler;\n" +
      " uniform sampler2D uTextureBurn;  //edgeBurn\n" +
      " uniform sampler2D uTextureMap;  //hefeMap\n" +
      " uniform sampler2D uTextureGradientMap;  //hefeGradientMap\n" +
      " uniform sampler2D uTextureSoftLight;  //hefeSoftLight\n" +
      " uniform sampler2D uTextureMetal;  //hefeMetal\n" +
      " \n" +
      " void main()\n" +
      "{   \n" +
      "    vec3 texel = texture2D(uTextureSampler, vTextureCoord).rgb;\n" +
      "    vec3 edge = texture2D(uTextureBurn, vTextureCoord).rgb;\n" +
      "    texel = texel * edge;\n" +
      "    \n" +
      "    texel = vec3(\n" +
      "                 texture2D(uTextureMap, vec2(texel.r, .16666)).r,\n" +
      "                 texture2D(uTextureMap, vec2(texel.g, .5)).g,\n" +
      "                 texture2D(uTextureMap, vec2(texel.b, .83333)).b);\n" +
      "    \n" +
      "    vec3 luma = vec3(.30, .59, .11);\n" +
      "    vec3 gradSample = texture2D(uTextureGradientMap, vec2(dot(luma, texel), .5)).rgb;\n" +
      "    vec3 final = vec3(\n" +
      "                      texture2D(uTextureSoftLight, vec2(gradSample.r, texel.r)).r,\n" +
      "                      texture2D(uTextureSoftLight, vec2(gradSample.g, texel.g)).g,\n" +
      "                      texture2D(uTextureSoftLight, vec2(gradSample.b, texel.b)).b\n" +
      "                      );\n" +
      "    \n" +
      "    vec3 metal = texture2D(uTextureMetal, vTextureCoord).rgb;\n" +
      "    vec3 metaled = vec3(\n" +
      "                        texture2D(uTextureSoftLight, vec2(metal.r, texel.r)).r,\n" +
      "                        texture2D(uTextureSoftLight, vec2(metal.g, texel.g)).g,\n" +
      "                        texture2D(uTextureSoftLight, vec2(metal.b, texel.b)).b\n" +
      "                        );\n" +
      "    \n" +
      "    gl_FragColor = vec4(metaled, 1.0);\n" +
      "}\n";

  private Bitmap burn;
  private Bitmap map;
  private Bitmap gradientMap;
  private Bitmap softLight;
  private Bitmap metal;

  private int burnTextureId;
  private int mapTextureId;
  private int gradientMapTextureId;
  private int softLightTextureId;
  private int metalTextureId;

  public HefeFilter(Context context) {
    burn = BitmapFactory.decodeResource(context.getResources(), R.drawable.edge_burn);
    map = BitmapFactory.decodeResource(context.getResources(), R.drawable.hefe_map);
    gradientMap = BitmapFactory.decodeResource(context.getResources(), R.drawable.hefe_gradient_map);
    softLight = BitmapFactory.decodeResource(context.getResources(), R.drawable.hefe_soft_light);
    metal = BitmapFactory.decodeResource(context.getResources(), R.drawable.hefe_metal);
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
    burnTextureId = loadTexture(burn, NO_TEXTURE, true);
    mapTextureId = loadTexture(map, NO_TEXTURE, true);
    gradientMapTextureId = loadTexture(gradientMap, NO_TEXTURE, true);
    softLightTextureId = loadTexture(softLight, NO_TEXTURE, true);
    metalTextureId = loadTexture(metal, NO_TEXTURE, true);
  }

  @Override
  public void bindUniform() {
    GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, burnTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureBurn"), 3);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mapTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureMap"), 4);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gradientMapTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureGradientMap"), 5);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE6);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, softLightTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureSoftLight"), 6);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE7);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, metalTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureMetal"), 7);
  }
}
