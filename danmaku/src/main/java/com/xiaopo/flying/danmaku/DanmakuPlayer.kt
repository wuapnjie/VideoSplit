package com.xiaopo.flying.danmaku

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.GLES20
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.xiaopo.flying.glkit.gl.EglCore
import com.xiaopo.flying.glkit.gl.WindowSurface
import java.lang.ref.WeakReference

/**
 * @author wupanjie
 */
class DanmakuPlayer(texture: SurfaceTexture, width: Int, height: Int, shaderProgram: DanmakuShaderProgram) : Thread(), OnFrameAvailableListener {
    private val lock = Any()
    private val surfaceWidth: Int
    private val surfaceHeight: Int
    private val previewSurfaceTexture: SurfaceTexture
    private var eglCore: EglCore? = null
    private var previewWindowSurface: WindowSurface? = null
    var renderHandler: RenderHandler? = null
        private set
    private var onRendererReadyListener: OnRendererReadyListener? = null
    private val shaderProgram: DanmakuShaderProgram

    private fun initialize() {
        setViewport(surfaceWidth, surfaceHeight)
    }

    private fun initGL() {
        eglCore = EglCore(null, EglCore.FLAG_RECORDABLE or EglCore.FLAG_TRY_GLES3)
        //create preview surface
        previewWindowSurface = WindowSurface(eglCore, previewSurfaceTexture)
        previewWindowSurface?.makeCurrent()
        initGLComponents()
    }

    private fun initGLComponents() {
        shaderProgram.prepare()
        shaderProgram.setOnFrameAvailableListener(this)
        onSetupComplete()
    }

    private fun deinitGL() {
        shaderProgram.release()
        previewWindowSurface?.release()
        eglCore?.release()
    }

    private fun onSetupComplete() {
        onRendererReadyListener?.onRendererReady()
    }

    @Synchronized
    override fun start() {
        initialize()
        if (onRendererReadyListener == null) {
            throw RuntimeException("OnRenderReadyListener is not set! Set listener prior to calling start()")
        }
        super.start()
    }

    override fun run() {
        Looper.prepare()
        renderHandler = RenderHandler(this)
        initGL()
        Looper.loop()
        deinitGL()
        onRendererReadyListener?.onRendererFinished()
    }

    fun shutdown() {
        deinitGL()
        onRendererReadyListener?.onRendererFinished()
        Looper.myLooper()?.quit()
    }

    fun play() {
        shaderProgram.play()
    }

    override fun onFrameAvailable(previewSurfaceTexture: SurfaceTexture) {
        renderHandler?.sendEmptyMessage(RenderHandler.MSG_RENDER)
    }

    private fun draw() {
        shaderProgram.run()
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
                shutdown()
            }
        }
    }

    fun setViewport(viewportWidth: Int, viewportHeight: Int) {
        shaderProgram.setViewport(viewportWidth, viewportHeight)
    }

    fun setOnRendererReadyListener(listener: OnRendererReadyListener?) {
        onRendererReadyListener = listener
    }

    class RenderHandler internal constructor(danmakuPlayer: DanmakuPlayer) : Handler() {

        private val weakRenderer: WeakReference<DanmakuPlayer> = WeakReference(danmakuPlayer)

        fun sendShutdown() {
            sendMessage(obtainMessage(MSG_SHUTDOWN))
        }

        override fun handleMessage(msg: Message) {
            val renderer = weakRenderer.get()
            if (renderer == null) {
                Log.w(TAG, "RenderHandler.handleMessage: weak ref is null")
                return
            }
            when (val what = msg.what) {
                MSG_SHUTDOWN -> renderer.shutdown()
                MSG_RENDER -> renderer.render()
                else -> throw RuntimeException("unknown message $what")
            }
        }

        companion object {
            private val TAG = RenderHandler::class.java.simpleName
            private const val MSG_SHUTDOWN = 0
            const val MSG_RENDER = 1
        }

    }

    interface OnRendererReadyListener {
        fun onRendererReady()
        fun onRendererFinished()
    }

    companion object {
        private val TAG = DanmakuPlayer::class.java.simpleName
        private const val THREAD_NAME = "DanmakuPlayer"
    }

    init {
        this.name = THREAD_NAME
        previewSurfaceTexture = texture
        surfaceWidth = width
        surfaceHeight = height
        this.shaderProgram = shaderProgram
    }
}