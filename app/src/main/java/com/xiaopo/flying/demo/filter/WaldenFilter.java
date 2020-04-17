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
public class WaldenFilter extends AbstractShaderFilter {

  private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
      "\n" +
      "precision lowp float;\n" +
      " varying highp vec2 vTextureCoord;\n" +
      " \n" +
      " uniform samplerExternalOES uTextureSampler;\n" +
      " uniform sampler2D uTextureMap; //map\n" +
      " uniform sampler2D uTextureVigMap; //vigMap\n" +
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
      "     vec2 tc = (2.0 * vTextureCoord) - 1.0;\n" +
      "     float d = dot(tc, tc);\n" +
      "     vec2 lookup = vec2(d, texel.r);\n" +
      "     texel.r = texture2D(uTextureVigMap, lookup).r;\n" +
      "     lookup.y = texel.g;\n" +
      "     texel.g = texture2D(uTextureVigMap, lookup).g;\n" +
      "     lookup.y = texel.b;\n" +
      "     texel.b    = texture2D(uTextureVigMap, lookup).b;\n" +
      "     \n" +
      "     gl_FragColor = vec4(texel, 1.0);\n" +
      " }\n";

  private Bitmap map;
  private Bitmap vigmap;

  private int mapTextureId;
  private int vigmapTextureId;

  public WaldenFilter(Context context) {
    map = BitmapFactory.decodeResource(context.getResources(), R.drawable.walden_map);
    vigmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.vignette_map);
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
    vigmapTextureId = loadTexture(vigmap, NO_TEXTURE, true);
  }

  @Override
  public void bindUniform() {
    GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mapTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureMap"), 3);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vigmapTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureVigMap"), 4);
  }
}
