package com.xiaopo.flying.demo.filter;

import android.opengl.GLES20;
import com.xiaopo.flying.glkit.filter.AbstractShaderFilter;

/**
 * @author wupanjie
 */
public class MonochromeFilter extends AbstractShaderFilter {
  private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
      "\n" +
      "precision lowp float;\n" +
      "\n" +
      "varying vec2 vTextureCoord;\n" +
      "uniform samplerExternalOES uTextureSampler;\n" +
      "uniform float intensity;\n" +
      "const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
      "const mediump vec3 filterColor = vec3(0.6, 0.45, 0.3);\n" +
      "\n" +
      "void main() {\n" +
      "  lowp vec4 textureColor = texture2D(uTextureSampler, vTextureCoord);\n" +
      "  float luminance = dot(textureColor.rgb, luminanceWeighting);\n" +
      "  lowp vec4 desat = vec4(vec3(luminance), 1.0);\n" +
      "  lowp vec4 outputColor = vec4((desat.r < 0.5 ? (2.0 * desat.r * filterColor.r) : (1.0 - 2.0 * (1.0 - desat.r) * (1.0 - filterColor.r))),(desat.g < 0.5 ? (2.0 * desat.g * filterColor.g) : (1.0 - 2.0 * (1.0 - desat.g) * (1.0 - filterColor.g))),(desat.b < 0.5 ? (2.0 * desat.b * filterColor.b) : (1.0 - 2.0 * (1.0 - desat.b) * (1.0 - filterColor.b))),1.0);\n" +
      "  gl_FragColor = vec4(mix(textureColor.rgb, outputColor.rgb, intensity), textureColor.a);\n" +
      "}\n";

  private float intensity = 1.0f;

  @Override
  public String getVertexShaderCode() {
    return DEFAULT_VERTEX_SHADER_CODE;
  }

  @Override
  public String getFragmentShaderCode() {
    return FRAGMENT_SHADER;
  }

  @Override
  public void bindUniform() {
    GLES20.glUniform1f(getParameterHandle("intensity"),intensity);
  }
}
