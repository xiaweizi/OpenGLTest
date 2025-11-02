package com.example.openglstudy.day08

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.openglstudy.R

/**
 * Day 08 Activity
 * 学习目标：多重纹理与混合
 * 实现功能：使用多个纹理单元实现图片混合、水印等效果
 */
class Day08Activity : AppCompatActivity() {

    companion object {
        private const val TAG = "Day08Activity"
    }

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: Day08Renderer

    private lateinit var tvCurrentBlendMode: TextView
    private lateinit var tvAlphaValue: TextView
    private lateinit var seekBarAlpha: SeekBar

    private lateinit var btnAlpha: Button
    private lateinit var btnAdd: Button
    private lateinit var btnMultiply: Button
    private lateinit var btnScreen: Button
    private lateinit var btnOverlay: Button
    private lateinit var btnSoftLight: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: 开始创建 Activity")
        setContentView(R.layout.activity_day08)

        // 设置标题
        supportActionBar?.title = "Day 08: 多重纹理与混合"

        // 初始化 GLSurfaceView
        glSurfaceView = findViewById(R.id.gl_surface_view)
        glSurfaceView.setEGLContextClientVersion(2)
        Log.d(TAG, "onCreate: OpenGL ES 版本设置为 2.0")

        // 创建并设置 Renderer
        renderer = Day08Renderer(this)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        Log.d(TAG, "onCreate: Renderer 设置完成")

        // 初始化 UI
        initViews()
        setupListeners()

        Log.d(TAG, "onCreate: Activity 创建完成")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: GLSurfaceView 恢复渲染")
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: GLSurfaceView 暂停渲染")
        glSurfaceView.onPause()
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        tvCurrentBlendMode = findViewById(R.id.tv_current_blend_mode)
        tvAlphaValue = findViewById(R.id.tv_alpha_value)
        seekBarAlpha = findViewById(R.id.seekbar_alpha)

        btnAlpha = findViewById(R.id.btn_blend_alpha)
        btnAdd = findViewById(R.id.btn_blend_add)
        btnMultiply = findViewById(R.id.btn_blend_multiply)
        btnScreen = findViewById(R.id.btn_blend_screen)
        btnOverlay = findViewById(R.id.btn_blend_overlay)
        btnSoftLight = findViewById(R.id.btn_blend_soft_light)

        // 设置初始值
        seekBarAlpha.progress = 50  // 50% 透明度
        tvAlphaValue.text = "透明度: 0.50"
        updateButtonStates(Day08Renderer.BlendMode.ALPHA)
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // SeekBar 监听器
        seekBarAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val alpha = progress / 100.0f
                tvAlphaValue.text = "透明度: %.2f".format(alpha)
                
                glSurfaceView.queueEvent {
                    renderer.setAlpha(alpha)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 混合模式按钮监听器
        btnAlpha.setOnClickListener {
            setBlendMode(Day08Renderer.BlendMode.ALPHA)
        }

        btnAdd.setOnClickListener {
            setBlendMode(Day08Renderer.BlendMode.ADD)
        }

        btnMultiply.setOnClickListener {
            setBlendMode(Day08Renderer.BlendMode.MULTIPLY)
        }

        btnScreen.setOnClickListener {
            setBlendMode(Day08Renderer.BlendMode.SCREEN)
        }

        btnOverlay.setOnClickListener {
            setBlendMode(Day08Renderer.BlendMode.OVERLAY)
        }

        btnSoftLight.setOnClickListener {
            setBlendMode(Day08Renderer.BlendMode.SOFT_LIGHT)
        }
    }

    /**
     * 设置混合模式
     */
    private fun setBlendMode(mode: Day08Renderer.BlendMode) {
        glSurfaceView.queueEvent {
            renderer.setBlendMode(mode)
        }
        tvCurrentBlendMode.text = "当前模式: ${mode.displayName}"
        updateButtonStates(mode)
        Log.d(TAG, "setBlendMode: 切换到 ${mode.displayName}")
    }

    /**
     * 更新按钮状态
     */
    private fun updateButtonStates(currentMode: Day08Renderer.BlendMode) {
        // 重置所有按钮样式
        listOf(btnAlpha, btnAdd, btnMultiply, btnScreen, btnOverlay, btnSoftLight).forEach {
            it.setBackgroundResource(android.R.drawable.btn_default)
            it.setTextColor(getColor(android.R.color.darker_gray))
        }

        // 高亮当前选中的按钮
        val selectedButton = when (currentMode) {
            Day08Renderer.BlendMode.ALPHA -> btnAlpha
            Day08Renderer.BlendMode.ADD -> btnAdd
            Day08Renderer.BlendMode.MULTIPLY -> btnMultiply
            Day08Renderer.BlendMode.SCREEN -> btnScreen
            Day08Renderer.BlendMode.OVERLAY -> btnOverlay
            Day08Renderer.BlendMode.SOFT_LIGHT -> btnSoftLight
        }

        selectedButton.setBackgroundResource(android.R.drawable.button_onoff_indicator_on)
        selectedButton.setTextColor(getColor(android.R.color.holo_blue_dark))
    }
}

