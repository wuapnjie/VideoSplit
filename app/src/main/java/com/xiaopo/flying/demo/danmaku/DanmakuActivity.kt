package com.xiaopo.flying.demo.danmaku

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.TextureView
import kotlinx.android.synthetic.main.activity_danmaku.*

/**
 * @author wupanjie
 */
class DanmakuActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textureView = texture_view
    }

}