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
public class BrannanFilter extends AbstractShaderFilter {

  private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
      "\n" +
      "precision lowp float;\n" +
      " varying highp vec2 vTextureCoord;\n" +
      " \n" +
      " uniform samplerExternalOES uTextureSampler;\n" +
      " uniform sampler2D uTextureProcess;  //process\n" +
      " uniform sampler2D uTextureBlowout;  //blowout\n" +
      " uniform sampler2D uTextureContract;  //contract\n" +
      " uniform sampler2D uTextureLuma;  //luma\n" +
      " uniform sampler2D uTextureScreen;  //screen\n" +
      " \n" +
      " mat3 saturateMatrix = mat3(\n" +
      "                            1.105150,\n" +
      "                            -0.044850,\n" +
      "                            -0.046000,\n" +
      "                            -0.088050,\n" +
      "                            1.061950,\n" +
      "                            -0.089200,\n" +
      "                            -0.017100,\n" +
      "                            -0.017100,\n" +
      "                            1.132900);\n" +
      " \n" +
      " vec3 luma = vec3(.3, .59, .11);\n" +
      " \n" +
      " void main()\n" +
      " {\n" +
      "     \n" +
      "     vec3 texel = texture2D(uTextureSampler, vTextureCoord).rgb;\n" +
      "     \n" +
      "     vec2 lookup;\n" +
      "     lookup.y = 0.5;\n" +
      "     lookup.x = texel.r;\n" +
      "     texel.r = texture2D(uTextureProcess, lookup).r;\n" +
      "     lookup.x = texel.g;\n" +
      "     texel.g = texture2D(uTextureProcess, lookup).g;\n" +
      "     lookup.x = texel.b;\n" +
      "     texel.b = texture2D(uTextureProcess, lookup).b;\n" +
      "     \n" +
      "     texel = saturateMatrix * texel;\n" +
      "     \n" +
      "     \n" +
      "     vec2 tc = (2.0 * vTextureCoord) - 1.0;\n" +
      "     float d = dot(tc, tc);\n" +
      "     vec3 sampled;\n" +
      "     lookup.y = 0.5;\n" +
      "     lookup.x = texel.r;\n" +
      "     sampled.r = texture2D(uTextureBlowout, lookup).r;\n" +
      "     lookup.x = texel.g;\n" +
      "     sampled.g = texture2D(uTextureBlowout, lookup).g;\n" +
      "     lookup.x = texel.b;\n" +
      "     sampled.b = texture2D(uTextureBlowout, lookup).b;\n" +
      "     float value = smoothstep(0.0, 1.0, d);\n" +
      "     texel = mix(sampled, texel, value);\n" +
      "     \n" +
      "     lookup.x = texel.r;\n" +
      "     texel.r = texture2D(uTextureContract, lookup).r;\n" +
      "     lookup.x = texel.g;\n" +
      "     texel.g = texture2D(uTextureContract, lookup).g;\n" +
      "     lookup.x = texel.b;\n" +
      "     texel.b = texture2D(uTextureContract, lookup).b;\n" +
      "     \n" +
      "     \n" +
      "     lookup.x = dot(texel, luma);\n" +
      "     texel = mix(texture2D(uTextureLuma, lookup).rgb, texel, .5);\n" +
      "\n" +
      "     lookup.x = texel.r;\n" +
      "     texel.r = texture2D(uTextureScreen, lookup).r;\n" +
      "     lookup.x = texel.g;\n" +
      "     texel.g = texture2D(uTextureScreen, lookup).g;\n" +
      "     lookup.x = texel.b;\n" +
      "     texel.b = texture2D(uTextureScreen, lookup).b;\n" +
      "     \n" +
      "     gl_FragColor = vec4(texel, 1.0);\n" +
      " }\n";

  private Bitmap process;
  private Bitmap blowout;
  private Bitmap contract;
  private Bitmap luma;
  private Bitmap screen;

  private int processTextureId;
  private int blowoutTextureId;
  private int contractTextureId;
  private int lumaTextureId;
  private int screenTextureId;

  public BrannanFilter(Context context) {
    process = BitmapFactory.decodeResource(context.getResources(), R.drawable.brannan_process);
    blowout = BitmapFactory.decodeResource(context.getResources(), R.drawable.brannan_blowout);
    contract = BitmapFactory.decodeResource(context.getResources(), R.drawable.brannan_contract);
    luma = BitmapFactory.decodeResource(context.getResources(), R.drawable.brannan_luma);
    screen = BitmapFactory.decodeResource(context.getResources(), R.drawable.brannan_screen);
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
    processTextureId = loadTexture(process, NO_TEXTURE, true);
    blowoutTextureId = loadTexture(blowout, NO_TEXTURE, true);
    contractTextureId = loadTexture(contract, NO_TEXTURE, true);
    lumaTextureId = loadTexture(luma, NO_TEXTURE, true);
    screenTextureId = loadTexture(screen, NO_TEXTURE, true);
  }

  @Override
  public void bindUniform() {
    GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, processTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureProcess"), 3);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blowoutTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureBlowout"), 4);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, contractTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureContract"), 5);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE6);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lumaTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureLuma"), 6);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE7);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, screenTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureScreen"), 7);
  }
}
