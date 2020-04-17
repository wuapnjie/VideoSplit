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
public class XprollFilter extends AbstractShaderFilter {

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
        "     vec2 tc = (2.0 * vTextureCoord) - 1.0;\n" +
        "     float d = dot(tc, tc);\n" +
        "     vec2 lookup = vec2(d, texel.r);\n" +
        "     texel.r = texture2D(uTextureVigMap, lookup).r;\n" +
        "     lookup.y = texel.g;\n" +
        "     texel.g = texture2D(uTextureVigMap, lookup).g;\n" +
        "     lookup.y = texel.b;\n" +
        "     texel.b    = texture2D(uTextureVigMap, lookup).b;\n" +
        "     \n" +
        "     vec2 red = vec2(texel.r, 0.16666);\n" +
        "     vec2 green = vec2(texel.g, 0.5);\n" +
        "     vec2 blue = vec2(texel.b, .83333);\n" +
        "     texel.r = texture2D(uTextureMap, red).r;\n" +
        "     texel.g = texture2D(uTextureMap, green).g;\n" +
        "     texel.b = texture2D(uTextureMap, blue).b;\n" +
        "     \n" +
        "     gl_FragColor = vec4(texel, 1.0);\n" +
        "     \n" +
        " }";

    private Bitmap vigmap;
    private Bitmap map;

    private int vigmapTextureId;
    private int mapTextureId;

    public XprollFilter(Context context) {
        vigmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.valencia_gradient_map);
        map = BitmapFactory.decodeResource(context.getResources(), R.drawable.xpro_map);
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
        vigmapTextureId = loadTexture(vigmap, NO_TEXTURE, true);
        mapTextureId = loadTexture(map, NO_TEXTURE, true);
    }

    @Override
    public void bindUniform() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vigmapTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureVigMap"), 3);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mapTextureId);
        GLES20.glUniform1i(getParameterHandle("uTextureMap"), 4);

    }
}
