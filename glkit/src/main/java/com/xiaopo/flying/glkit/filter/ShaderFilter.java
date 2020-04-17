package com.xiaopo.flying.glkit.filter;

/**
 * @author wupanjie
 */
public interface ShaderFilter {
  String DEFAULT_VERTEX_SHADER_CODE = "uniform mat4 uMatrix;\n" +
      "uniform mat4 uTextureMatrix;\n" +
      "attribute vec2 aPosition;\n" +
      "varying vec2 vTextureCoord;\n" +
      "\n" +
      "void main(){\n" +
      "  vec4 pos = vec4(aPosition,0.0,1.0);\n" +
      "  gl_Position = uMatrix * pos;\n" +
      "  vTextureCoord = (uTextureMatrix * pos).xy;\n" +
      "}";

  String DEFAULT_FRAGMENT_SHADER_CODE = "#extension GL_OES_EGL_image_external : require\n" +
      "\n" +
      "precision mediump float;\n" +
      "varying vec2 vTextureCoord;\n" +
      "uniform samplerExternalOES uTextureSampler;\n" +
      "\n" +
      "void main(){\n" +
      "  gl_FragColor = texture2D(uTextureSampler,vTextureCoord);\n" +
      "}\n";
  
  
  String POSITION_ATTRIBUTE = "aPosition";
  String MATRIX_UNIFORM = "uMatrix";
  String TEXTURE_MATRIX_UNIFORM = "uTextureMatrix";
  String TEXTURE_SAMPLER_UNIFORM = "uTextureSampler";
  
  String getVertexShaderCode();

  String getFragmentShaderCode();

  void prepare();

  void bindUniform();

  int getParameterHandle(String name);

  void activate();
  
  void release();
}
