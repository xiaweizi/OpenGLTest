package com.example.openglstudy.base

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * OpenGL Activity 基类
 * 提供 GLSurfaceView 的基础配置和生命周期管理
 */
abstract class BaseGLActivity : AppCompatActivity() {

    protected lateinit var glSurfaceView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建 GLSurfaceView
        glSurfaceView = GLSurfaceView(this)

        // 设置 OpenGL ES 版本为 2.0
        glSurfaceView.setEGLContextClientVersion(2)

        // 创建并设置 Renderer（由子类实现）
        glSurfaceView.setRenderer(createRenderer())

        // 设置渲染模式（默认为持续渲染）
        glSurfaceView.renderMode = getRenderMode()

        setContentView(glSurfaceView)

        // 设置标题
        supportActionBar?.title = getActivityTitle()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    /**
     * 创建 Renderer（由子类实现）
     */
    protected abstract fun createRenderer(): GLSurfaceView.Renderer

    /**
     * 获取渲染模式（子类可覆盖）
     * @return GLSurfaceView.RENDERMODE_CONTINUOUSLY 或 GLSurfaceView.RENDERMODE_WHEN_DIRTY
     */
    protected open fun getRenderMode(): Int {
        return GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    /**
     * 获取 Activity 标题（子类可覆盖）
     */
    protected open fun getActivityTitle(): String {
        return "OpenGL ES"
    }
}
