package com.xiaopo.flying.glkit.filter;

/**
 * @author wupanjie
 */
@SuppressWarnings("FieldCanBeLocal")
public class NoFilter extends AbstractShaderFilter {

    @Override
    public String getVertexShaderCode() {
        return DEFAULT_VERTEX_SHADER_CODE;
    }

    @Override
    public String getFragmentShaderCode() {
        return DEFAULT_FRAGMENT_SHADER_CODE;
    }

    @Override
    public void bindUniform() {
        // no-ops
    }
}
