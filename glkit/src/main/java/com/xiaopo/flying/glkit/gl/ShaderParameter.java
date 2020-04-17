package com.xiaopo.flying.glkit.gl;

import android.opengl.GLES20;

/**
 * @author wupanjie
 */
public abstract class ShaderParameter {

  final String name;
  int handle;

  private ShaderParameter(String name) {
    this.name = name;
  }

  public int getHandle() {
    return handle;
  }

  public static Uniform uniform(final String name) {
    return new Uniform(name);
  }

  public static Attribute attribute(final String name) {
    return new Attribute(name);
  }

  public abstract void loadHandle(final int program);

  public static final class Uniform extends ShaderParameter {

    private Uniform(String name) {
      super(name);
    }

    @Override
    public void loadHandle(int program) {
      handle = GLES20.glGetUniformLocation(program, name);
    }
  }

  public static final class Attribute extends ShaderParameter {

    private Attribute(String name) {
      super(name);
    }

    @Override
    public void loadHandle(int program) {
      handle = GLES20.glGetAttribLocation(program, name);
    }
  }

}
