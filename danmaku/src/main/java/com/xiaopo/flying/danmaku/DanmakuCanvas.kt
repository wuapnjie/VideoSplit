package com.xiaopo.flying.danmaku

import android.annotation.SuppressLint
import android.graphics.*
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.text.TextPaint
import android.util.Log
import android.view.Surface
import com.xiaopo.flying.danmaku.api.INeedRelease
import com.xiaopo.flying.glkit.gl.ShaderProgram
import com.xiaopo.flying.puzzlekit.Area

/**
 * 负责弹幕的绘制
 *
 * @author wupanjie
 */
internal class DanmakuCanvas : INeedRelease {
    companion object {
        private const val TAG = "DanmakuPainter"
    }

    private val positionMatrix = FloatArray(ShaderProgram.MATRIX_SIZE)
    private val textureMatrix = FloatArray(ShaderProgram.MATRIX_SIZE)

    var outputTexture: SurfaceTexture? = null
        private set
    private var surface: Surface? = null

    private var textureId = 0

    private val displayArea = RectF()
    private val textureArea = RectF(0f, 0f, 1f, 1f)

    private var canvasWidth = 0
    private var canvasHeight = 0

    @SuppressLint("Recycle")
    fun configOutput(textureId: Int, textureWidth: Int, textureHeight: Int) {
        this.textureId = textureId
        outputTexture = SurfaceTexture(textureId).also {
            it.setDefaultBufferSize(textureWidth, textureHeight)
        }
        surface = Surface(outputTexture)
    }

    override fun release() {
        outputTexture?.release()
        outputTexture = null
    }

    fun setDisplayArea(area: Area) {
        val rect = area.areaRect
        canvasWidth = rect.width().toInt()
        canvasHeight = rect.height().toInt()
        setDisplayArea(rect.left, rect.top, rect.right, rect.bottom)
    }

    private fun setDisplayArea(left: Float, top: Float, right: Float, bottom: Float) {
        val paintWidth = (right - left).toInt()
        val paintHeight = (bottom - top).toInt()
        displayArea.set(left, top, right, bottom)
        val scale: Float
        val displayWidth = displayArea.width()
        val displayHeight = displayArea.height()
        scale = if (paintWidth * displayHeight > displayWidth * paintHeight) {
            displayHeight / paintHeight
        } else {
            displayWidth / paintWidth
        }
        val scaleWidth = paintWidth * scale
        val scaleHeight = paintHeight * scale
        val offsetW = (scaleWidth - displayWidth) / 2
        val offsetH = (scaleHeight - displayHeight) / 2
        textureArea[offsetW, offsetH, scaleWidth - offsetW] = scaleHeight - offsetH
        normalize(textureArea, scaleWidth, scaleHeight)
    }

    private fun normalize(textureArea: RectF, scaleWidth: Float, scaleHeight: Float) {
        textureArea.left = textureArea.left / scaleWidth
        textureArea.top = textureArea.top / scaleHeight
        textureArea.right = textureArea.right / scaleWidth
        textureArea.bottom = textureArea.bottom / scaleHeight
    }

    fun setTexture(textureHandle: Int, textureMatrixHandle: Int) {
        val outputTexture = this.outputTexture ?: return
        outputTexture.getTransformMatrix(textureMatrix)
        textureMatrix[0] = textureArea.width()
        textureMatrix[5] = textureArea.height()
        textureMatrix[12] = textureArea.left
        textureMatrix[13] = textureArea.top
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(textureHandle, 0)
        GLES20.glUniformMatrix4fv(textureMatrixHandle, 1, false, textureMatrix, 0)
    }

    fun setMatrix(matrixHandle: Int, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        val x = displayArea.left
        val y = displayArea.top
        val width = displayArea.width()
        val height = displayArea.height()
        Matrix.translateM(positionMatrix, 0, viewMatrix, 0, x, y, 0f)
        Matrix.scaleM(positionMatrix, 0, width, height, 1f)
        Matrix.multiplyMM(positionMatrix, 0, projectionMatrix, 0, positionMatrix, 0)
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, positionMatrix, 0)
    }

    private var i = 0f
    private val paint = TextPaint().apply {
        flags =  Paint.ANTI_ALIAS_FLAG
        color = Color.BLUE
        strokeWidth = 5f
        textSize = 48f
    }
    private val rect = Rect()

    private val cache = Bitmap.createBitmap(150, 50, Bitmap.Config.ARGB_8888).apply {
        val canvas = Canvas(this)
        canvas.drawText("12412", 0f, 48f , paint)
    }

    fun draw() {
        val surface = this.surface ?: return

        rect.set(0,0,canvasWidth, canvasHeight)
        val canvas = surface.lockCanvas(rect)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        paint.color = Color.RED
        var y = 48f
        for (j in 0 .. 35) {
            for (k in 0 .. 6) {
                canvas.drawText("奥斯卡", i + k * 150, y , paint)
            }
            y += 48f
        }

        surface.unlockCanvasAndPost(canvas)
//        Log.d(TAG, "i is $i, current thread is ${Thread.currentThread().name}")
        i += 20f
        if (i > 1080f) {
            i = - 1080f
        }
    }
}