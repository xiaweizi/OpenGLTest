package com.example.openglstudy.day07

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.openglstudy.R

/**
 * Day 07 Activity
 * 学习目标：FBO（帧缓冲对象）
 * 实现功能：使用 FBO 实现离屏渲染和图像滤镜效果
 */
class Day07Activity : AppCompatActivity() {

    companion object {
        private const val TAG = "Day07Activity"
    }

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: Day07Renderer

    private lateinit var tvCurrentFilter: TextView
    private lateinit var btnFilterNone: Button
    private lateinit var btnFilterGrayscale: Button
    private lateinit var btnFilterInvert: Button
    private lateinit var btnFilterBlur: Button
    private lateinit var btnFilterEdge: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: 开始创建 Activity")
        setContentView(R.layout.activity_day07)

        // 设置标题
        supportActionBar?.title = "Day 07: FBO（帧缓冲对象）"

        // 初始化 GLSurfaceView
        glSurfaceView = findViewById(R.id.gl_surface_view)
        glSurfaceView.setEGLContextClientVersion(2)
        Log.d(TAG, "onCreate: OpenGL ES 版本设置为 2.0")

        // 创建并设置 Renderer
        renderer = Day07Renderer(this)
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
        tvCurrentFilter = findViewById(R.id.tv_current_filter)
        btnFilterNone = findViewById(R.id.btn_filter_none)
        btnFilterGrayscale = findViewById(R.id.btn_filter_grayscale)
        btnFilterInvert = findViewById(R.id.btn_filter_invert)
        btnFilterBlur = findViewById(R.id.btn_filter_blur)
        btnFilterEdge = findViewById(R.id.btn_filter_edge)

        // 默认选中原图
        updateButtonStates(Day07Renderer.FilterType.NONE)
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        btnFilterNone.setOnClickListener {
            setFilter(Day07Renderer.FilterType.NONE, "原图")
        }

        btnFilterGrayscale.setOnClickListener {
            setFilter(Day07Renderer.FilterType.GRAYSCALE, "灰度滤镜")
        }

        btnFilterInvert.setOnClickListener {
            setFilter(Day07Renderer.FilterType.INVERT, "反色滤镜")
        }

        btnFilterBlur.setOnClickListener {
            setFilter(Day07Renderer.FilterType.BLUR, "模糊滤镜")
        }

        btnFilterEdge.setOnClickListener {
            setFilter(Day07Renderer.FilterType.EDGE_DETECT, "边缘检测")
        }
    }

    /**
     * 设置滤镜
     */
    private fun setFilter(filterType: Day07Renderer.FilterType, filterName: String) {
        glSurfaceView.queueEvent {
            renderer.setFilter(filterType)
        }
        tvCurrentFilter.text = "当前滤镜: $filterName"
        updateButtonStates(filterType)
        Log.d(TAG, "setFilter: 切换到 $filterName")
    }

    /**
     * 更新按钮状态
     */
    private fun updateButtonStates(currentFilter: Day07Renderer.FilterType) {
        // 重置所有按钮样式
        listOf(btnFilterNone, btnFilterGrayscale, btnFilterInvert, btnFilterBlur, btnFilterEdge).forEach {
            it.setBackgroundResource(android.R.drawable.btn_default)
            it.setTextColor(getColor(android.R.color.darker_gray))
        }

        // 高亮当前选中的按钮
        val selectedButton = when (currentFilter) {
            Day07Renderer.FilterType.NONE -> btnFilterNone
            Day07Renderer.FilterType.GRAYSCALE -> btnFilterGrayscale
            Day07Renderer.FilterType.INVERT -> btnFilterInvert
            Day07Renderer.FilterType.BLUR -> btnFilterBlur
            Day07Renderer.FilterType.EDGE_DETECT -> btnFilterEdge
        }

        selectedButton.setBackgroundResource(android.R.drawable.button_onoff_indicator_on)
        selectedButton.setTextColor(getColor(android.R.color.holo_blue_dark))
    }
}
