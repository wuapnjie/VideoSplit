package com.xiaopo.flying.danmaku

import android.graphics.Color
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.GLES20
import android.opengl.Matrix
import com.xiaopo.flying.glkit.filter.NoFilter
import com.xiaopo.flying.glkit.filter.ShaderFilter
import com.xiaopo.flying.glkit.gl.BufferUtil
import com.xiaopo.flying.glkit.gl.ShaderProgram
import com.xiaopo.flying.puzzlekit.PuzzleLayout
import java.util.*

/**
 * @author wupanjie
 */
class DanmakuShaderProgram : ShaderProgram() {
    private var vertexBufferId = 0
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    private val danmakuPainters = ArrayList<DanmakuPainter>()
    private var puzzleLayout: PuzzleLayout? = null

    private val shaderFilters = ArrayList<ShaderFilter>()
    private val shaderFilterCache = HashMap<Class<out ShaderFilter>, ShaderFilter>()
    private val noFilter: ShaderFilter = NoFilter()

    private var colorRed = 0f
    private var colorBlue = 0f
    private var colorGreen = 0f

    fun setPuzzleLayout(puzzleLayout: PuzzleLayout) {
        this.puzzleLayout = puzzleLayout
    }

    @JvmOverloads
    fun addPainter(filter: ShaderFilter = noFilter) {
        val filterClass: Class<out ShaderFilter> = filter.javaClass
        var cached = shaderFilterCache[filterClass]
        if (cached == null) {
            shaderFilterCache[filterClass] = filter
            cached = filter
        }
        danmakuPainters.add(DanmakuPainter())
        shaderFilters.add(cached)
    }

    fun setBackgroundColor(backgroundColor: Int) {
        colorRed = Color.red(backgroundColor) / 255f
        colorBlue = Color.blue(backgroundColor) / 255f
        colorGreen = Color.green(backgroundColor) / 255f
    }


    override fun prepare() { // prepare filter
        for (key in shaderFilterCache.keys) {
            val filter = shaderFilterCache[key]
            filter?.prepare()
        }
        vertexBufferId = uploadBuffer(BufferUtil.storeDataInBuffer(vertexCoordinates))
        val size = danmakuPainters.size
        val textureIds = IntArray(size)
        generateTextures(size, textureIds, 0)
        for (i in 0 until size) {
            danmakuPainters[i].configOutput(textureIds[i])
        }
        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.orthoM(projectionMatrix, 0, 0f, viewportWidth.toFloat(), 0f, viewportHeight.toFloat(), -1f, 1f)
        // 坐标变换，变成Android View坐标系，左上角为(0, 0)
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.translateM(viewMatrix, 0, 0f, viewportHeight.toFloat(), 0f)
        Matrix.scaleM(viewMatrix, 0, 1f, -1f, 1f)
    }

    override fun run() {
        val puzzleLayout = this.puzzleLayout ?: return

        GLES20.glClearColor(colorRed, colorGreen, colorBlue, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        val painterCount = danmakuPainters.size
        for (i in 0 until painterCount) {
            val area = puzzleLayout.getArea(i)
            val filter = shaderFilters[i]
            filter.activate()
            val textureHandle = filter.getParameterHandle(ShaderFilter.TEXTURE_SAMPLER_UNIFORM)
            val textureMatrixHandle = filter.getParameterHandle(ShaderFilter.TEXTURE_MATRIX_UNIFORM)
            val matrixHandle = filter.getParameterHandle(ShaderFilter.MATRIX_UNIFORM)
            filter.bindUniform()
            val painter = danmakuPainters[i]
            painter.setDisplayArea(area)
            painter.setTexture(textureHandle, textureMatrixHandle)
            painter.setMatrix(matrixHandle, viewMatrix, projectionMatrix)
            drawElements(filter)
        }
    }

    override fun release() {
        super.release()
        for (key in shaderFilterCache.keys) {
            shaderFilterCache[key]?.release()
        }
        for (DanmakuPainter in danmakuPainters) {
            DanmakuPainter.release()
        }
        danmakuPainters.clear()
        shaderFilters.clear()
    }

    private fun drawElements(filter: ShaderFilter) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId)
        GLES20.glVertexAttribPointer(
            filter.getParameterHandle(ShaderFilter.POSITION_ATTRIBUTE),
            2,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE,
            0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glEnableVertexAttribArray(filter.getParameterHandle(ShaderFilter.POSITION_ATTRIBUTE))
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCoordinates.size)
        GLES20.glDisableVertexAttribArray(filter.getParameterHandle(ShaderFilter.POSITION_ATTRIBUTE))
    }

    fun updatePreviewTexture() {
        for (DanmakuPainter in danmakuPainters) {
            DanmakuPainter.outputTexture?.updateTexImage()
        }
    }

    fun setOnFrameAvailableListener(onFrameAvailableListener: OnFrameAvailableListener?) {
        for (DanmakuPainter in danmakuPainters) {
            DanmakuPainter.outputTexture?.setOnFrameAvailableListener(onFrameAvailableListener)
        }
    }

    fun play() {
        for (DanmakuPainter in danmakuPainters) {
            DanmakuPainter.play()
        }
    }

    companion object {
        private const val TAG = "DanmakuShaderProgram"
        private const val COORDS_PER_VERTEX = 2
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * FLOAT_SIZE
        private val vertexCoordinates = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)
    }
}