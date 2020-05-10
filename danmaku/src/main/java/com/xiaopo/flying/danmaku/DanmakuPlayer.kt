package com.xiaopo.flying.danmaku

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.GLES20
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.xiaopo.flying.danmaku.api.IDanmakuPlayer
import com.xiaopo.flying.danmaku.utils.WeakHandler
import com.xiaopo.flying.glkit.gl.EglCore
import com.xiaopo.flying.glkit.gl.WindowSurface

/**
 * 弹幕播放器
 *
 * @author wupanjie
 */
class DanmakuPlayer(
    private val previewSurfaceTexture: SurfaceTexture,
    private val surfaceWidth: Int,
    private val surfaceHeight: Int,
    private val shaderProgram: DanmakuShaderProgram
) : HandlerThread(THREAD_NAME), OnFrameAvailableListener, IDanmakuPlayer {

    interface OnRendererListener {
        fun onRendererReady()
        fun onRendererFinished()
    }

    companion object {
        private val TAG = DanmakuPlayer::class.java.simpleName
        private val lock = Any()
        private const val THREAD_NAME = "DanmakuPlayer"

        // 一些消息类型
        private const val MSG_SHUTDOWN = 0
        private const val MSG_RENDER = 1
    }

    private var eglCore: EglCore? = null
    private var previewWindowSurface: WindowSurface? = null
    private var onRendererListener: OnRendererListener? = null

    private var renderHandler: WeakHandler? = null
    private val renderHandle = WeakHandler.IHandler { msg: Message ->
        when (msg.what) {
            MSG_SHUTDOWN -> shutdownInternal()
            MSG_RENDER -> render()
        }
    }

    private fun initialize() {
        setViewport(surfaceWidth, surfaceHeight)
    }

    private fun initGL() {
        eglCore = EglCore(null, EglCore.FLAG_RECORDABLE or EglCore.FLAG_TRY_GLES3)
        //create preview surface
        previewWindowSurface = WindowSurface(eglCore, previewSurfaceTexture)
        previewWindowSurface?.makeCurrent()
        initGLProgram()
    }

    private fun initGLProgram() {
        shaderProgram.prepare()
        shaderProgram.setOnFrameAvailableListener(this)
        onSetupComplete()
    }

    private fun deinitGL() {
        shaderProgram.release()
        previewWindowSurface?.release()
        eglCore?.release()
    }

    override fun release() {
        deinitGL()
    }

    private fun onSetupComplete() {
        onRendererListener?.onRendererReady()
    }

    @Synchronized
    override fun start() {
        initialize()
        if (onRendererListener == null) {
            throw RuntimeException("OnRenderReadyListener is not set! Set listener prior to calling start()")
        }
        super.start()
    }

    override fun onLooperPrepared() {
        super.onLooperPrepared()
        renderHandler = WeakHandler(looper, renderHandle)
        initGL()
    }

    override fun quit(): Boolean {
        release()
        onRendererListener?.onRendererFinished()
        return super.quit()
    }

    private fun shutdownInternal() {
        quit()
    }

    override fun play() {
        shaderProgram.play()
    }

    override fun pause() {

    }


    fun shutdown() {
        renderHandler?.sendEmptyMessage(MSG_SHUTDOWN)
    }

    override fun onFrameAvailable(previewSurfaceTexture: SurfaceTexture) {
        renderHandler?.sendEmptyMessage(MSG_RENDER)
    }

    private fun render() {
        val eglCore = this.eglCore ?: return
        val previewWindowSurface = this.previewWindowSurface ?: return

        var swapResult: Boolean
        synchronized(lock) {
            shaderProgram.updatePreviewTexture()
            swapResult = if (eglCore.glVersion >= 3) {
                draw()
                //swap main buff
                previewWindowSurface.makeCurrent()
                previewWindowSurface.swapBuffers()
            } else { //gl v2
                draw()
                // Restore previous values.
                GLES20.glViewport(0, 0, previewWindowSurface.width, previewWindowSurface.height)
                previewWindowSurface.makeCurrent()
                previewWindowSurface.swapBuffers()
            }
            if (!swapResult) { // This can happen if the Activity stops without waiting for us to halt.
                Log.e(TAG, "swapBuffers failed, killing renderer thread")
                shutdownInternal()
            }
        }
    }

    private fun draw() {
        shaderProgram.execute()
    }

    fun setViewport(viewportWidth: Int, viewportHeight: Int) {
        shaderProgram.setViewport(viewportWidth, viewportHeight)
    }

    fun setOnRendererListener(listener: OnRendererListener?) {
        onRendererListener = listener
    }
}