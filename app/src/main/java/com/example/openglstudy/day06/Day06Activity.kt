package com.example.openglstudy.day06

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.openglstudy.R

/**
 * Day 06 Activity
 * 学习目标：EGL 与 GLSurfaceView 深入
 * 实现功能：演示渲染模式切换，理解持续渲染与按需渲染的区别
 */
class Day06Activity : AppCompatActivity() {

    companion object {
        private const val TAG = "Day06Activity"
    }

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: Day06Renderer

    private lateinit var tvRenderMode: TextView
    private lateinit var tvFps: TextView
    private lateinit var tvRotation: TextView
    private lateinit var btnToggleMode: Button
    private lateinit var btnRotateStep: Button
    private lateinit var btnReset: Button

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 100)  // 每 100ms 更新一次 UI
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: 开始创建 Activity")
        setContentView(R.layout.activity_day06)

        // 设置标题
        supportActionBar?.title = "Day 06: EGL 与 GLSurfaceView 深入"

        // 初始化 GLSurfaceView
        glSurfaceView = findViewById(R.id.gl_surface_view)
        glSurfaceView.setEGLContextClientVersion(2)
        Log.d(TAG, "onCreate: OpenGL ES 版本设置为 2.0")

        // 创建并设置 Renderer
        renderer = Day06Renderer(this)
        glSurfaceView.setRenderer(renderer)
        Log.d(TAG, "onCreate: Renderer 设置完成")

        // 初始化为持续渲染模式
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        renderer.renderMode = Day06Renderer.RenderMode.CONTINUOUSLY
        Log.d(TAG, "onCreate: 初始渲染模式为持续渲染")

        // 初始化 UI
        initViews()
        setupListeners()

        Log.d(TAG, "onCreate: Activity 创建完成")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: GLSurfaceView 恢复渲染")
        glSurfaceView.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: GLSurfaceView 暂停渲染")
        glSurfaceView.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        tvRenderMode = findViewById(R.id.tv_render_mode)
        tvFps = findViewById(R.id.tv_fps)
        tvRotation = findViewById(R.id.tv_rotation)
        btnToggleMode = findViewById(R.id.btn_toggle_mode)
        btnRotateStep = findViewById(R.id.btn_rotate_step)
        btnReset = findViewById(R.id.btn_reset)

        updateUI()
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 切换渲染模式
        btnToggleMode.setOnClickListener {
            toggleRenderMode()
        }

        // 旋转一步（按需渲染模式下使用）
        btnRotateStep.setOnClickListener {
            rotateStep()
        }

        // 重置旋转
        btnReset.setOnClickListener {
            resetRotation()
        }
    }

    /**
     * 切换渲染模式
     */
    private fun toggleRenderMode() {
        if (glSurfaceView.renderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
            // 切换到按需渲染
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            renderer.renderMode = Day06Renderer.RenderMode.WHEN_DIRTY
            btnRotateStep.isEnabled = true
            Log.d(TAG, "toggleRenderMode: 切换到按需渲染模式")
        } else {
            // 切换到持续渲染
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            renderer.renderMode = Day06Renderer.RenderMode.CONTINUOUSLY
            btnRotateStep.isEnabled = false
            Log.d(TAG, "toggleRenderMode: 切换到持续渲染模式")
        }
        updateUI()
    }

    /**
     * 手动旋转一步
     */
    private fun rotateStep() {
        glSurfaceView.queueEvent {
            renderer.rotateStep(5f)
        }
        // 在按需模式下，手动请求渲染
        glSurfaceView.requestRender()
        Log.d(TAG, "rotateStep: 手动旋转一步")
    }

    /**
     * 重置旋转角度
     */
    private fun resetRotation() {
        glSurfaceView.queueEvent {
            renderer.resetRotation()
        }
        // 如果是按需模式，需要手动请求渲染
        if (glSurfaceView.renderMode == GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
            glSurfaceView.requestRender()
        }
        Log.d(TAG, "resetRotation: 重置旋转角度")
    }

    /**
     * 更新 UI 显示
     */
    private fun updateUI() {
        // 更新渲染模式
        val modeText = if (glSurfaceView.renderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
            "渲染模式: 持续渲染 (CONTINUOUSLY)"
        } else {
            "渲染模式: 按需渲染 (WHEN_DIRTY)"
        }
        tvRenderMode.text = modeText

        // 更新 FPS
        tvFps.text = String.format("FPS: %.1f", renderer.currentFps)

        // 更新旋转角度
        tvRotation.text = String.format("旋转角度: %.1f°", renderer.rotation)
    }
}
