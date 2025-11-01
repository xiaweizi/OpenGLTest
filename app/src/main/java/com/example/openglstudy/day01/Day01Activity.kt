package com.example.openglstudy.day01

import android.opengl.GLSurfaceView
import com.example.openglstudy.base.BaseGLActivity

/**
 * Day 01 Activity
 * 学习目标：OpenGL ES 入门与环境搭建
 * 实现功能：清屏并显示纯色背景
 */
class Day01Activity : BaseGLActivity() {

    private lateinit var renderer: Day01Renderer

    override fun createRenderer(): GLSurfaceView.Renderer {
        renderer = Day01Renderer()
        return renderer
    }

    override fun getActivityTitle(): String {
        return "Day 01: OpenGL ES 入门"
    }
}
