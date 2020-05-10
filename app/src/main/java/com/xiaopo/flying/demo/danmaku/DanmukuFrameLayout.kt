package com.xiaopo.flying.demo.danmaku


import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.widget.FrameLayout

/**
 * @author wupanjie
 */
class DanmukuFrameLayout @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attr, defStyle) {

    init {
        setWillNotDraw(false)
    }

    private val rect = Rect()

    private val paint = TextPaint().apply {
        flags =  Paint.ANTI_ALIAS_FLAG
        color = Color.BLUE
        strokeWidth = 5f
        textSize = 48f
    }
    private var i = 0f

    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
//        paint.color = Color.RED
//        var y = 48f
//        for (j in 0 .. 35) {
//            for (k in 0 .. 6) {
//                canvas.drawText("奥斯卡", i + k * 150, y , paint)
//            }
//            y += 48f
//        }
//
////        surface.unlockCanvasAndPost(canvas)
////        Log.d(TAG, "i is $i, current thread is ${Thread.currentThread().name}")
//        i += 20f
//        if (i > 1080f) {
//            i = - 1080f
//        }
//
//        Choreographer.getInstance().postFrameCallback { invalidate() }
    }

}