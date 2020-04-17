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
public class LordKelvinFilter extends AbstractShaderFilter {

  private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
      "\n" +
      "precision lowp float;\n" +
      " varying highp vec2 vTextureCoord;\n" +
      " uniform samplerExternalOES uTextureSampler;\n" +
      " uniform sampler2D uTextureMap;\n" +
      " void main()\n" +
      " {\n" +
      "     vec3 texel = texture2D(uTextureSampler, vTextureCoord).rgb;\n" +
      "     vec2 lookup;\n" +
      "     lookup.y = .5;\n" +
      "     lookup.x = texel.r;\n" +
      "     texel.r = texture2D(uTextureMap, lookup).r;\n" +
      "     lookup.x = texel.g;\n" +
      "     texel.g = texture2D(uTextureMap, lookup).g;\n" +
      "     lookup.x = texel.b;\n" +
      "     texel.b = texture2D(uTextureMap, lookup).b;\n" +
      "     gl_FragColor = vec4(texel, 1.0);\n" +
      " }";

  private Bitmap map;

  private int mapTextureId;

  public LordKelvinFilter(Context context) {
    map = BitmapFactory.decodeResource(context.getResources(), R.drawable.kelvin_map);
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
  }

  @Override
  public void bindUniform() {
    GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mapTextureId);
    GLES20.glUniform1i(getParameterHandle("uTextureMap"), 3);

  }
}
