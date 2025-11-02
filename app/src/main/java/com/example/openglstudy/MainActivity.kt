package com.example.openglstudy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openglstudy.adapter.DayAdapter
import com.example.openglstudy.day01.Day01Activity
import com.example.openglstudy.day02.Day02Activity
import com.example.openglstudy.day03.Day03Activity
import com.example.openglstudy.day04.Day04Activity
import com.example.openglstudy.day05.Day05Activity
import com.example.openglstudy.day06.Day06Activity
import com.example.openglstudy.day07.Day07Activity
import com.example.openglstudy.day08.Day08Activity
import com.example.openglstudy.model.DayItem

/**
 * 主界面 Activity
 * 展示 14 天 OpenGL 学习计划列表
 */
class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var dayAdapter: DayAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initRecyclerView()
    }

    private fun initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 创建 14 天的学习数据
        val dayList = listOf(
            DayItem(1, "OpenGL ES 入门与环境搭建", "理解 OpenGL ES 的基本概念，掌握 GLSurfaceView 的使用", Day01Activity::class.java),
            DayItem(2, "渲染第一个三角形", "学习顶点数据和着色器，渲染第一个图形", Day02Activity::class.java),
            DayItem(3, "着色器基础 - GLSL 语言", "深入学习 GLSL 语法，掌握 uniform、attribute、varying", Day03Activity::class.java),
            DayItem(4, "纹理贴图基础", "学习纹理加载和映射，将图片渲染到 OpenGL", Day04Activity::class.java),
            DayItem(5, "纹理变换与矩阵操作", "掌握矩阵变换，实现图片的旋转、缩放、平移", Day05Activity::class.java),
            DayItem(6, "EGL 与 GLSurfaceView 深入", "理解 EGL 上下文，掌握渲染模式和线程管理", Day06Activity::class.java),
            DayItem(7, "FBO（帧缓冲对象）", "学习离屏渲染，实现多通道渲染效果", Day07Activity::class.java),
            DayItem(8, "多重纹理与混合", "掌握多纹理使用和混合模式", Day08Activity::class.java),
            DayItem(9, "CameraX 基础集成", "集成 CameraX，实现相机预览功能", MainActivity::class.java),
            DayItem(10, "相机预览与 OpenGL 结合", "使用 SurfaceTexture 获取相机数据并渲染", MainActivity::class.java),
            DayItem(11, "实时滤镜效果", "实现灰度、复古、暖色调等滤镜效果", MainActivity::class.java),
            DayItem(12, "美颜算法 - 磨皮", "学习双边滤波算法，实现磨皮效果", MainActivity::class.java),
            DayItem(13, "美颜算法 - 美白与瘦脸", "实现美白和局部扭曲变形效果", MainActivity::class.java),
            DayItem(14, "综合项目整合与性能优化", "整合所有效果，优化性能，项目总结", MainActivity::class.java)
        )

        dayAdapter = DayAdapter(dayList)
        recyclerView.adapter = dayAdapter
    }
}
