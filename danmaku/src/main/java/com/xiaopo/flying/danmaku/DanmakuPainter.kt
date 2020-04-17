package com.xiaopo.flying.danmaku

import android.graphics.Color
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import com.xiaopo.flying.glkit.gl.ShaderProgram
import com.xiaopo.flying.puzzlekit.Area

/**
 * @author wupanjie
 */
internal class DanmakuPainter : Thread() {
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

    fun configOutput(textureId: Int) {
        this.textureId = textureId
        outputTexture = SurfaceTexture(textureId)
        surface = Surface(outputTexture)
    }

    fun release() {
        outputTexture?.release()
        outputTexture = null
        interrupt()
    }

    fun play() {
        start()
    }

    fun setDisplayArea(area: Area) {
        val rect = area.areaRect
        setDisplayArea(rect.left, rect.top, rect.right, rect.bottom)
    }

    private fun setDisplayArea(left: Float, top: Float, right: Float, bottom: Float) {
        val paintWidth = (right - left).toInt()
        val paintHeight = (bottom - top).toInt()
        displayArea[left, top, right] = bottom
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

    override fun run() {
        val surface = this.surface ?: return

        var i = 0
        while (true) {
            try {
                val canvas = surface.lockCanvas(null)
                if (i % 2 == 0) {
                    canvas.drawColor(Color.RED)
                } else {
                    canvas.drawColor(Color.BLACK)
                }
                surface.unlockCanvasAndPost(canvas)
                Log.d(TAG, "i is $i")
                sleep(1000)
                i++
            } catch (e: Exception) {
                e.printStackTrace()
                break
            }
        }
    }
}