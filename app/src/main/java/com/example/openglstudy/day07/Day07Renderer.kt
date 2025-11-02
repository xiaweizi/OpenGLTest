package com.example.openglstudy.day07

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.openglstudy.R
import com.example.openglstudy.utils.TextureHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 07 Renderer
 * 学习目标：FBO（帧缓冲对象）
 * 实现功能：使用 FBO 实现离屏渲染和图像滤镜效果
 */
class Day07Renderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "Day07Renderer"
    }

    /**
     * 滤镜类型枚举
     */
    enum class FilterType {
        NONE,       // 无滤镜
        GRAYSCALE,  // 灰度
        INVERT,     // 反色
        BLUR,       // 模糊
        EDGE_DETECT // 边缘检测
    }

    // Pass 1: 渲染原图到 FBO 的顶点着色器
    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;

        void main() {
            vTexCoord = aTexCoord;
            gl_Position = aPosition;
        }
    """.trimIndent()

    // Pass 1: 简单纹理渲染（无滤镜）
    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;

        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """.trimIndent()

    // Pass 2: 滤镜着色器
    private val filterFragmentShaderCode = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform int uFilterType;
        uniform vec2 uTexelSize;

        // 灰度滤镜
        vec4 grayscale(vec4 color) {
            float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
            return vec4(vec3(gray), color.a);
        }

        // 反色滤镜
        vec4 invert(vec4 color) {
            return vec4(1.0 - color.rgb, color.a);
        }

        // 简单模糊滤镜（3x3 均值滤波）
        vec4 blur() {
            vec4 sum = vec4(0.0);
            for (float x = -1.0; x <= 1.0; x += 1.0) {
                for (float y = -1.0; y <= 1.0; y += 1.0) {
                    vec2 offset = vec2(x, y) * uTexelSize;
                    sum += texture2D(uTexture, vTexCoord + offset);
                }
            }
            return sum / 9.0;
        }

        // 边缘检测（Sobel 算子）
        vec4 edgeDetect() {
            // 采样周围 8 个像素
            float tl = texture2D(uTexture, vTexCoord + uTexelSize * vec2(-1.0,  1.0)).r;
            float tm = texture2D(uTexture, vTexCoord + uTexelSize * vec2( 0.0,  1.0)).r;
            float tr = texture2D(uTexture, vTexCoord + uTexelSize * vec2( 1.0,  1.0)).r;

            float ml = texture2D(uTexture, vTexCoord + uTexelSize * vec2(-1.0,  0.0)).r;
            float mr = texture2D(uTexture, vTexCoord + uTexelSize * vec2( 1.0,  0.0)).r;

            float bl = texture2D(uTexture, vTexCoord + uTexelSize * vec2(-1.0, -1.0)).r;
            float bm = texture2D(uTexture, vTexCoord + uTexelSize * vec2( 0.0, -1.0)).r;
            float br = texture2D(uTexture, vTexCoord + uTexelSize * vec2( 1.0, -1.0)).r;

            // Sobel 算子
            float gx = -tl + tr - 2.0 * ml + 2.0 * mr - bl + br;
            float gy = tl + 2.0 * tm + tr - bl - 2.0 * bm - br;

            float edge = length(vec2(gx, gy));

            return vec4(vec3(edge), 1.0);
        }

        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);

            if (uFilterType == 0) {
                // 无滤镜
                gl_FragColor = color;
            } else if (uFilterType == 1) {
                // 灰度
                gl_FragColor = grayscale(color);
            } else if (uFilterType == 2) {
                // 反色
                gl_FragColor = invert(color);
            } else if (uFilterType == 3) {
                // 模糊
                gl_FragColor = blur();
            } else if (uFilterType == 4) {
                // 边缘检测
                gl_FragColor = edgeDetect();
            } else {
                gl_FragColor = color;
            }
        }
    """.trimIndent()

    // Pass 1: 渲染原始图片到 FBO（需要翻转 Y 轴，因为 Bitmap 坐标系与 OpenGL 不同）
    private val quadVerticesFlipped = floatArrayOf(
        // 位置          纹理坐标（Y 轴翻转）
        -1f,  1f, 0f,   0f, 0f,  // 左上 → 纹理左下
        -1f, -1f, 0f,   0f, 1f,  // 左下 → 纹理左上
         1f,  1f, 0f,   1f, 0f,  // 右上 → 纹理右下

        -1f, -1f, 0f,   0f, 1f,  // 左下 → 纹理左上
         1f, -1f, 0f,   1f, 1f,  // 右下 → 纹理右上
         1f,  1f, 0f,   1f, 0f   // 右上 → 纹理右下
    )

    // Pass 2: 从 FBO 渲染到屏幕（不翻转，FBO 纹理已经是正确方向）
    private val quadVerticesNormal = floatArrayOf(
        // 位置          纹理坐标（正常）
        -1f,  1f, 0f,   0f, 1f,  // 左上
        -1f, -1f, 0f,   0f, 0f,  // 左下
         1f,  1f, 0f,   1f, 1f,  // 右上

        -1f, -1f, 0f,   0f, 0f,  // 左下
         1f, -1f, 0f,   1f, 0f,  // 右下
         1f,  1f, 0f,   1f, 1f   // 右上
    )

    private lateinit var vertexBufferFlipped: FloatBuffer
    private lateinit var vertexBufferNormal: FloatBuffer

    // Pass 1: 渲染原图到 FBO
    private var simpleProgram: Int = 0

    // Pass 2: 从 FBO 渲染到屏幕（应用滤镜）
    private var filterProgram: Int = 0
    private var uFilterTypeLocation: Int = 0
    private var uTexelSizeLocation: Int = 0

    // FBO 相关
    private var fbo = IntArray(1)
    private var fboTexture = IntArray(1)
    private var fboWidth: Int = 0
    private var fboHeight: Int = 0

    // 原始纹理
    private var originalTextureId: Int = 0

    // 用于从 Glide 接收 Bitmap
    @Volatile
    private var pendingBitmap: Bitmap? = null

    @Volatile
    private var needUpdateTexture = false

    // 当前滤镜类型
    @Volatile
    var currentFilter: FilterType = FilterType.NONE

    private val VERTEX_STRIDE = 5 * 4

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated: 开始初始化")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 创建翻转的顶点缓冲（用于 Pass 1）
        vertexBufferFlipped = ByteBuffer.allocateDirect(quadVerticesFlipped.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadVerticesFlipped)
        vertexBufferFlipped.position(0)

        // 创建正常的顶点缓冲（用于 Pass 2）
        vertexBufferNormal = ByteBuffer.allocateDirect(quadVerticesNormal.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadVerticesNormal)
        vertexBufferNormal.position(0)
        Log.d(TAG, "onSurfaceCreated: 顶点缓冲创建完成")

        // 编译 Pass 1 着色器（简单纹理渲染）
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        simpleProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(simpleProgram, vertexShader)
        GLES20.glAttachShader(simpleProgram, fragmentShader)
        GLES20.glLinkProgram(simpleProgram)

        checkProgramLink(simpleProgram, "Simple Program")
        Log.d(TAG, "onSurfaceCreated: Simple Program 创建完成")

        // 编译 Pass 2 着色器（滤镜）
        val filterFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, filterFragmentShaderCode)

        filterProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(filterProgram, vertexShader)
        GLES20.glAttachShader(filterProgram, filterFragmentShader)
        GLES20.glLinkProgram(filterProgram)

        checkProgramLink(filterProgram, "Filter Program")
        Log.d(TAG, "onSurfaceCreated: Filter Program 创建完成")

        // 获取 uniform 位置
        uFilterTypeLocation = GLES20.glGetUniformLocation(filterProgram, "uFilterType")
        uTexelSizeLocation = GLES20.glGetUniformLocation(filterProgram, "uTexelSize")
        Log.d(TAG, "onSurfaceCreated: uFilterType=$uFilterTypeLocation, uTexelSize=$uTexelSizeLocation")

        // 使用 Glide 加载图片（会自动处理 EXIF 旋转信息）
        loadImageWithGlide()
    }

    /**
     * 使用 Glide 加载图片
     * Glide 会自动处理 EXIF 旋转信息，确保 Bitmap 方向正确
     */
    private fun loadImageWithGlide() {
        Log.d(TAG, "loadImageWithGlide: 开始使用 Glide 加载图片")
        Glide.with(context)
            .asBitmap()
            .load(R.mipmap.icon_test)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Log.d(TAG, "Glide 加载成功: ${resource.width}x${resource.height}")
                    // 保存 Bitmap 并标记需要更新纹理
                    pendingBitmap = resource
                    needUpdateTexture = true
                }
            })
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: 视口改变 width=$width, height=$height")
        GLES20.glViewport(0, 0, width, height)

        // 设置 FBO 尺寸（与屏幕相同）
        fboWidth = width
        fboHeight = height

        // 创建 FBO
        createFBO(fboWidth, fboHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 检查是否有新的 Bitmap 需要创建纹理
        if (needUpdateTexture && pendingBitmap != null) {
            // 删除旧纹理（如果存在）
            if (originalTextureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(originalTextureId), 0)
            }

            // 从 Bitmap 创建新纹理
            originalTextureId = TextureHelper.loadTexture(pendingBitmap)
            Log.d(TAG, "onDrawFrame: 纹理创建完成，textureId=$originalTextureId")

            // 重置标志
            needUpdateTexture = false
            // 注意：不要在这里回收 Bitmap，因为 Glide 会管理它
        }

        // 只有在纹理加载完成后才渲染
        if (originalTextureId == 0) {
            // 纹理还未加载，清屏并返回
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            return
        }

        // Pass 1: 渲染原图到 FBO
        renderToFBO()

        // Pass 2: 从 FBO 读取纹理，应用滤镜，渲染到屏幕
        renderToScreen()
    }

    /**
     * Pass 1: 渲染原图到 FBO
     */
    private fun renderToFBO() {
        // 绑定 FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
        GLES20.glViewport(0, 0, fboWidth, fboHeight)

        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 使用简单纹理程序
        GLES20.glUseProgram(simpleProgram)

        // 绑定原始纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, originalTextureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(simpleProgram, "uTexture"), 0)

        // 绘制全屏四边形（使用翻转的顶点缓冲）
        drawQuad(simpleProgram, vertexBufferFlipped)

        // 解绑 FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    /**
     * Pass 2: 从 FBO 渲染到屏幕（应用滤镜）
     */
    private fun renderToScreen() {
        // 绑定默认帧缓冲（屏幕）
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, fboWidth, fboHeight)

        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 使用滤镜程序
        GLES20.glUseProgram(filterProgram)

        // 绑定 FBO 纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTexture[0])
        GLES20.glUniform1i(GLES20.glGetUniformLocation(filterProgram, "uTexture"), 0)

        // 设置滤镜类型
        GLES20.glUniform1i(uFilterTypeLocation, currentFilter.ordinal)

        // 设置纹理像素大小（用于模糊和边缘检测）
        GLES20.glUniform2f(uTexelSizeLocation, 1.0f / fboWidth, 1.0f / fboHeight)

        // 绘制全屏四边形（使用正常的顶点缓冲）
        drawQuad(filterProgram, vertexBufferNormal)

        // 解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    /**
     * 绘制全屏四边形
     * @param program 着色器程序
     * @param vertexBuffer 顶点缓冲（包含位置和纹理坐标）
     */
    private fun drawQuad(program: Int, vertexBuffer: FloatBuffer) {
        val aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        val aTexCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord")

        // 设置顶点属性
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            aPositionLocation,
            3, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(aPositionLocation)

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
    }

    /**
     * 创建 FBO
     */
    private fun createFBO(width: Int, height: Int) {
        Log.d(TAG, "createFBO: 创建 FBO, width=$width, height=$height")

        // 删除旧的 FBO（如果存在）
        if (fbo[0] != 0) {
            GLES20.glDeleteFramebuffers(1, fbo, 0)
            GLES20.glDeleteTextures(1, fboTexture, 0)
        }

        // 1. 生成 FBO
        GLES20.glGenFramebuffers(1, fbo, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
        Log.d(TAG, "createFBO: FBO 生成完成, fbo=${fbo[0]}")

        // 2. 创建纹理（颜色附件）
        GLES20.glGenTextures(1, fboTexture, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTexture[0])

        // 设置纹理参数
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // 分配纹理存储（但不填充数据）
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )
        Log.d(TAG, "createFBO: 纹理创建完成, texture=${fboTexture[0]}")

        // 3. 将纹理附加到 FBO
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            fboTexture[0],
            0
        )

        // 4. 检查 FBO 完整性
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "createFBO: FBO 不完整, status=$status")
            when (status) {
                GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT ->
                    Log.e(TAG, "createFBO: GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT")
                GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT ->
                    Log.e(TAG, "createFBO: GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT")
                GLES20.GL_FRAMEBUFFER_UNSUPPORTED ->
                    Log.e(TAG, "createFBO: GL_FRAMEBUFFER_UNSUPPORTED")
            }
        } else {
            Log.d(TAG, "createFBO: FBO 创建成功")
        }

        // 5. 解绑 FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    /**
     * 设置滤镜类型
     */
    fun setFilter(filter: FilterType) {
        currentFilter = filter
        Log.d(TAG, "setFilter: 切换滤镜为 ${filter.name}")
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

    /**
     * 检查程序链接状态
     */
    private fun checkProgramLink(program: Int, name: String) {
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            Log.e(TAG, "checkProgramLink: $name 链接失败 - $log")
            throw RuntimeException("Program link failed: $log")
        }
        Log.d(TAG, "checkProgramLink: $name 链接成功")
    }
}
