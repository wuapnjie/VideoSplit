package com.xiaopo.flying.glkit.gl;

import android.opengl.GLES20;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import static com.xiaopo.flying.glkit.gl.ShaderProgram.checkError;

/**
 * @author wupanjie
 */
public class Shader {
    private static final String TAG = "Shader";
    private int program = -1;
    private int vertexId = -1;
    private int fragmentId = -1;
    private ArrayList<ShaderParameter> shaderParameters = new ArrayList<>();
    private HashMap<String, Integer> parameterHandleMap = new HashMap<>();
    private ArrayList<Integer> textureIds = new ArrayList<>();

    private Shader() {
        //no instance
    }

    public void activate() {
        GLES20.glUseProgram(program);
        loadHandle();
    }

    public void release() {
        GLES20.glDeleteShader(vertexId);
        GLES20.glDeleteShader(fragmentId);
        GLES20.glDeleteProgram(program);

        program = -1;
        vertexId = -1;
        fragmentId = -1;
        shaderParameters.clear();
    }

    public void bindUniformValue(String name, int value) {
        GLES20.glUniform1i(getParameterHandle(name), value);
    }

    public void bindUniformValue(ShaderParameter.Uniform uniform, float value) {
        GLES20.glUniform1f(uniform.getHandle(), value);
    }

    public int getParameterHandle(String name) {
        Integer handle = parameterHandleMap.get(name);
        if (handle == null) {
            return GLES20.glGetUniformLocation(program, name);
        }
        return handle;
    }

    protected void loadHandle() {
        parameterHandleMap.clear();
        for (ShaderParameter shaderParameter : shaderParameters) {
            shaderParameter.loadHandle(program);
            parameterHandleMap.put(shaderParameter.name, shaderParameter.handle);
        }
    }

    public static class Builder {
        private Shader shader = new Shader();
        private int[] linkStatus = new int[1];

        public Shader.Builder attachVertex(String vertexShaderCode) {
            shader.vertexId = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            return this;
        }

        public Shader.Builder attachFragment(String fragmentShaderCode) {
            shader.fragmentId = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
            return this;
        }


        public Shader.Builder inflate(ShaderParameter parameter) {
            shader.shaderParameters.add(parameter);
            return this;
        }

        public Shader assemble() {
            shader.program = assembleProgram(shader.vertexId, shader.fragmentId, linkStatus);
            return shader;
        }

        private int assembleProgram(int vertexShader, int fragmentShader, int[] linkStatus) {
            int program = GLES20.glCreateProgram();
            checkError("create program");
            if (program == 0) {
                throw new RuntimeException("Cannot create GL program: " + GLES20.glGetError());
            }
            GLES20.glAttachShader(program, vertexShader);
            checkError("attach vertex shader");
            GLES20.glAttachShader(program, fragmentShader);
            checkError("attach fragment shader");
            GLES20.glLinkProgram(program);
            checkError("link program");
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
            return program;
        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);

            GLES20.glShaderSource(shader, shaderCode);
            checkError("shader source");
            GLES20.glCompileShader(shader);
            checkError("compile shader");

            return shader;
        }

    }
}
