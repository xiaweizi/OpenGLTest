package com.example.openglstudy.day06

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.example.openglstudy.R
import com.example.openglstudy.utils.TextureHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 06 Renderer
 * 学习目标：EGL 与 GLSurfaceView 深入
 * 实现功能：演示渲染模式切换，理解持续渲染与按需渲染的区别
 */
class Day06Renderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "Day06Renderer"
    }

    /**
     * 渲染模式枚举
     */
    enum class RenderMode {
        CONTINUOUSLY,   // 持续渲染
        WHEN_DIRTY      // 按需渲染
    }

    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        uniform mat4 uMatrix;

        void main() {
            vTexCoord = aTexCoord;
            gl_Position = uMatrix * aPosition;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;

        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """.trimIndent()

    // 正方形顶点（位置 + 纹理坐标）
    private val vertices = floatArrayOf(
        // 位置          纹理坐标
        -0.5f,  0.5f, 0f,   0f, 0f,  // 左上
        -0.5f, -0.5f, 0f,   0f, 1f,  // 左下
         0.5f,  0.5f, 0f,   1f, 0f,  // 右上

        -0.5f, -0.5f, 0f,   0f, 1f,  // 左下
         0.5f, -0.5f, 0f,   1f, 1f,  // 右下
         0.5f,  0.5f, 0f,   1f, 0f   // 右上
    )

    private lateinit var vertexBuffer: FloatBuffer
    private var program: Int = 0
    private var textureId: Int = 0
    private var uMatrixLocation: Int = 0

    // 旋转角度（使用 volatile 保证多线程可见性）
    @Volatile
    var rotation: Float = 0f

    // 当前渲染模式
    @Volatile
    var renderMode: RenderMode = RenderMode.CONTINUOUSLY

    private val modelMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val VERTEX_STRIDE = 5 * 4

    // FPS 计算
    private var lastFrameTime = System.nanoTime()
    private var frameCount = 0
    @Volatile
    var currentFps: Double = 0.0
        private set

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated: 开始初始化")
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f)

        // 创建顶点缓冲
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)
        Log.d(TAG, "onSurfaceCreated: 顶点缓冲创建完成，顶点数=${vertices.size / 5}")

        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        Log.d(TAG, "onSurfaceCreated: 着色器编译完成")

        // 创建程序
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // 检查链接状态
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            Log.e(TAG, "onSurfaceCreated: 程序链接失败 - $log")
            throw RuntimeException("Program link failed: $log")
        }
        Log.d(TAG, "onSurfaceCreated: 程序链接成功，program=$program")

        // 获取 uniform 和 attribute 位置
        uMatrixLocation = GLES20.glGetUniformLocation(program, "uMatrix")
        Log.d(TAG, "onSurfaceCreated: uMatrix location=$uMatrixLocation")

        // 加载纹理
        textureId = TextureHelper.loadTexture(context, R.drawable.sample_image)
        Log.d(TAG, "onSurfaceCreated: 纹理加载完成，textureId=$textureId")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: 视口改变 width=$width, height=$height")
        GLES20.glViewport(0, 0, width, height)

        // 创建正交投影矩阵
        val ratio = width.toFloat() / height.toFloat()
        if (width > height) {
            Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, -1f, 1f)
        } else {
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -1 / ratio, 1 / ratio, -1f, 1f)
        }
        Log.d(TAG, "onSurfaceChanged: 投影矩阵创建完成，ratio=$ratio")
    }

    override fun onDrawFrame(gl: GL10?) {
        // 计算 FPS
        calculateFps()

        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 根据渲染模式更新旋转角度
        if (renderMode == RenderMode.CONTINUOUSLY) {
            rotation += 2f
            if (rotation >= 360f) rotation = 0f
        }

        // 构建模型矩阵（旋转）
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotation, 0f, 0f, 1f)

        // 计算 MVP 矩阵
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)

        // 使用程序
        GLES20.glUseProgram(program)

        // 传递矩阵
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, mvpMatrix, 0)

        // 激活并绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTexture"), 0)

        // 设置顶点属性
        val aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            aPositionLocation,
            3, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        val aTexCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(
            aTexCoordLocation,
            2, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(aTexCoordLocation)

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)

        // 清理
        GLES20.glDisableVertexAttribArray(aPositionLocation)
        GLES20.glDisableVertexAttribArray(aTexCoordLocation)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    /**
     * 手动旋转一步（用于按需渲染模式）
     */
    fun rotateStep(degrees: Float = 5f) {
        rotation += degrees
        if (rotation >= 360f) rotation = 0f
        Log.d(TAG, "rotateStep: 手动旋转 $degrees 度，当前角度=$rotation")
    }

    /**
     * 重置旋转角度
     */
    fun resetRotation() {
        rotation = 0f
        Log.d(TAG, "resetRotation: 旋转角度已重置")
    }

    /**
     * 计算 FPS
     */
    private fun calculateFps() {
        val currentTime = System.nanoTime()
        frameCount++

        if (currentTime - lastFrameTime >= 1_000_000_000) {  // 1 秒
            currentFps = frameCount.toDouble()
            Log.d(TAG, "FPS: $currentFps")
            frameCount = 0
            lastFrameTime = currentTime
        }
    }

    /**
     * 编译着色器
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shaderType = if (type == GLES20.GL_VERTEX_SHADER) "顶点着色器" else "片段着色器"
        Log.d(TAG, "loadShader: 开始编译$shaderType")

        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            Log.e(TAG, "loadShader: $shaderType 编译失败 - $log")
            throw RuntimeException("Shader compilation failed: $log")
        }

        Log.d(TAG, "loadShader: $shaderType 编译成功，shader=$shader")
        return shader
    }
}
