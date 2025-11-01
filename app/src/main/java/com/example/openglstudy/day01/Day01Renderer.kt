package com.example.openglstudy.day01

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 01 Renderer
 * 学习目标：OpenGL ES 入门与环境搭建
 * 实现功能：清屏并显示纯色背景
 */
class Day01Renderer : GLSurfaceView.Renderer {

    // 背景颜色（RGBA）
    private var red = 0.2f
    private var green = 0.3f
    private var blue = 0.5f
    private var alpha = 1.0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置清屏颜色（蓝灰色）
        GLES20.glClearColor(red, green, blue, alpha)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 设置视口大小（整个 Surface）
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 清空颜色缓冲区（使用设置的清屏颜色）
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    /**
     * 设置背景颜色
     * @param r 红色分量 (0.0 ~ 1.0)
     * @param g 绿色分量 (0.0 ~ 1.0)
     * @param b 蓝色分量 (0.0 ~ 1.0)
     */
    fun setBackgroundColor(r: Float, g: Float, b: Float) {
        red = r.coerceIn(0f, 1f)
        green = g.coerceIn(0f, 1f)
        blue = b.coerceIn(0f, 1f)
        GLES20.glClearColor(red, green, blue, alpha)
    }
}
