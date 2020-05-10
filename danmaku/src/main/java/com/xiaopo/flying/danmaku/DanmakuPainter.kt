package com.xiaopo.flying.danmaku

import android.os.HandlerThread
import android.os.Message
import android.view.Choreographer
import com.xiaopo.flying.danmaku.api.INeedRelease
import com.xiaopo.flying.danmaku.utils.WeakHandler

/**
 * 负责定时绘制弹幕，具体的绘制则是由DanmakuCanvas画的
 *
 * @author wupanjie
 */
internal class DanmakuPainter : Thread(THREAD_NAME), INeedRelease {
    companion object {
        const val THREAD_NAME = "DanmakuPainter"

        const val MSG_DRAW = 0
    }

    private lateinit var paintHandler: WeakHandler
    private val danmakuCanvas: LinkedHashSet<DanmakuCanvas> = LinkedHashSet()
//    private val drawNextFrame: Choreographer.FrameCallback = Choreographer.FrameCallback { frameTimeNanos ->
//        drawInCanvas()
//    }

//    override fun onLooperPrepared() {
//        super.onLooperPrepared()
//        paintHandler = WeakHandler(looper, this)
//        drawInCanvas()
//    }

//    override fun handleMsg(msg: Message) {
//        when (msg.what) {
//            MSG_DRAW -> {
//                drawInCanvas()
//            }
//        }
//    }

    fun bindDisplay(canvas: DanmakuCanvas) {
        danmakuCanvas.add(canvas)
    }

    private fun drawInCanvas() {
        danmakuCanvas.forEach { it.draw() }
    }

    fun startDraw() {
        start()
    }

    override fun release() {
//        Choreographer.getInstance().removeFrameCallback(drawNextFrame)
//        quit()
        interrupt()
    }

    override fun run() {
        super.run()
        while (true) {
            try {
                drawInCanvas()

                Thread.sleep(16)
            } catch (e: Exception) {
                break
            }
        }
    }
}