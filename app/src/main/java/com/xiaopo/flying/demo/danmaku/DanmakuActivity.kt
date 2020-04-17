package com.xiaopo.flying.demo.danmaku

import android.graphics.Color
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.TextureView
import com.xiaopo.flying.danmaku.DanmakuPlayer
import com.xiaopo.flying.danmaku.DanmakuShaderProgram
import com.xiaopo.flying.demo.R
import com.xiaopo.flying.demo.layout.OneLayout
import com.xiaopo.flying.puzzlekit.PuzzleLayout
import kotlinx.android.synthetic.main.activity_danmaku.*

/**
 * @author wupanjie
 */
class DanmakuActivity : AppCompatActivity(), TextureView.SurfaceTextureListener, DanmakuPlayer.OnRendererReadyListener {

    private lateinit var textureView: TextureView
    private var surfaceTexture: SurfaceTexture? = null
    private var renderer: DanmakuPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_danmaku)

        textureView = texture_view
        textureView.surfaceTextureListener = this
    }

    override fun onStop() {
        super.onStop()
        renderer?.renderHandler?.sendShutdown()
    }

    private fun play() {
        renderer?.play()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        surface ?: return
        this.surfaceTexture = surface

        val shaderProgram = DanmakuShaderProgram()


        val puzzleLayout: PuzzleLayout = OneLayout()
        puzzleLayout.setOuterBounds(RectF(0f, 0f, width.toFloat(), height.toFloat()))
        puzzleLayout.layout()
        shaderProgram.setPuzzleLayout(puzzleLayout)
        shaderProgram.setBackgroundColor(Color.parseColor("#532421"))

        shaderProgram.addPainter()

        renderer = DanmakuPlayer(surface, width, height, shaderProgram).also {
            it.setViewport(width, height)
            it.setOnRendererReadyListener(this)
            it.start()
        }

    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        return true
    }

    override fun onRendererReady() {
        play()
    }

    override fun onRendererFinished() {

    }

}