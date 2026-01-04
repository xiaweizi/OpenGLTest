package com.example.openglstudy.day12

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 12 Renderer
 * 学习目标：LUT（Look-Up Table）滤镜
 * 实现功能：使用 3D LUT 实现专业级调色滤镜
 */
class Day12Renderer(
    private val context: Context,
    private val glSurfaceView: GLSurfaceView
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "Day11_1Renderer"
    }

    // 顶点着色器
    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec4 aTexCoord;
        uniform mat4 uTransformMatrix;
        varying vec2 vTexCoord;
        
        void main() {
            vec4 transformedCoord = uTransformMatrix * aTexCoord;
            vTexCoord = transformedCoord.xy;
            gl_Position = aPosition;
        }
    """.trimIndent()

    // LUT 滤镜片段着色器
    private val fragmentShaderCode = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        
        varying vec2 vTexCoord;
        uniform samplerExternalOES uTexture;
        uniform sampler2D uLUTTexture;
        uniform float uIntensity;
        
        vec4 applyLUT(vec4 color, sampler2D lut) {
            // 防止边界问题，稍微缩小范围
            float r = clamp(color.r, 0.0, 1.0) * 63.0;
            float g = clamp(color.g, 0.0, 1.0) * 63.0;
            float b = clamp(color.b, 0.0, 1.0) * 63.0;
            
            // 计算蓝色索引
            float blueFloor = floor(b);
            
            // 计算在 512x512 图片中的位置
            float quad = floor(blueFloor / 8.0);
            float xOffset = (blueFloor - quad * 8.0) / 8.0;
            float yOffset = quad / 8.0;
            
            // 在 64x64 小格子内的相对位置
            float xPos = (r + 0.5) / 512.0;
            float yPos = (g + 0.5) / 512.0;
            
            // 最终 UV 坐标
            float lutX = xOffset + xPos;
            float lutY = yOffset + yPos;
            
            // 采样 LUT
            vec4 newColor = texture2D(lut, vec2(lutX, lutY));
            
            return newColor;
        }
        
        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);
            vec4 lutColor = applyLUT(color, uLUTTexture);
            
            // 使用强度混合原色和 LUT 颜色
            gl_FragColor = mix(color, lutColor, uIntensity);
        }
    """.trimIndent()

    // 顶点数据
    private val quadVertices = floatArrayOf(
        -1f, -1f,
         1f, -1f,
        -1f,  1f,
         1f,  1f
    )

    private val texCoords = floatArrayOf(
        0f, 0f, 0f, 1f,
        1f, 0f, 0f, 1f,
        0f, 1f, 0f, 1f,
        1f, 1f, 0f, 1f
    )

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    private var program: Int = 0
    private var oesTextureId: Int = 0

    // Uniform 位置
    private var uTextureLocation: Int = 0
    private var uLUTTextureLocation: Int = 0
    private var uTransformMatrixLocation: Int = 0
    private var uIntensityLocation: Int = 0

    // Attribute 位置
    private var aPositionLocation: Int = 0
    private var aTexCoordLocation: Int = 0

    // SurfaceTexture
    private lateinit var surfaceTexture: SurfaceTexture
    private val transformMatrix = FloatArray(16)

    @Volatile
    private var surfaceTextureReady = false

    // LUT 滤镜列表
    private val lutFilters = LUTFilter.getAllFilters().toMutableList()
    private var currentFilterIndex = 0
    
    @Volatile
    var lutIntensity = 1.0f

    // 回调：通知 LUT 加载完成
    var onLUTsLoadedCallback: (() -> Unit)? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated: 开始初始化")
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        // 创建顶点缓冲
        vertexBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadVertices)
        vertexBuffer.position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords)
        texCoordBuffer.position(0)

        Log.d(TAG, "onSurfaceCreated: 顶点缓冲创建完成")

        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        checkProgramLink(program, "LUT Filter Program")
        Log.d(TAG, "onSurfaceCreated: 着色器程序创建完成")

        // 获取变量位置
        aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTextureLocation = GLES20.glGetUniformLocation(program, "uTexture")
        uLUTTextureLocation = GLES20.glGetUniformLocation(program, "uLUTTexture")
        uTransformMatrixLocation = GLES20.glGetUniformLocation(program, "uTransformMatrix")
        uIntensityLocation = GLES20.glGetUniformLocation(program, "uIntensity")

        Log.d(TAG, "onSurfaceCreated: 变量位置获取完成")

        // 创建 OES 纹理
        oesTextureId = createOESTexture()
        Log.d(TAG, "onSurfaceCreated: OES 纹理创建完成，textureId=$oesTextureId")

        // 创建 SurfaceTexture
        surfaceTexture = SurfaceTexture(oesTextureId)
        surfaceTexture.setOnFrameAvailableListener {
            glSurfaceView.requestRender()
        }

        surfaceTextureReady = true
        Log.d(TAG, "onSurfaceCreated: SurfaceTexture 创建完成")

        // 加载所有 LUT
        loadAllLUTs()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: 视口改变 width=$width, height=$height")
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (!surfaceTextureReady) {
            return
        }

        try {
            surfaceTexture.updateTexImage()
            surfaceTexture.getTransformMatrix(transformMatrix)
        } catch (e: Exception) {
            Log.e(TAG, "onDrawFrame: 更新纹理失败", e)
            return
        }

        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 使用着色器程序
        GLES20.glUseProgram(program)

        // 绑定相机纹理（纹理单元 0）
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(uTextureLocation, 0)

        // 绑定 LUT 纹理（纹理单元 1）
        val currentLUT = lutFilters[currentFilterIndex]
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currentLUT.textureId)
        GLES20.glUniform1i(uLUTTextureLocation, 1)

        // 传递变换矩阵和强度
        GLES20.glUniformMatrix4fv(uTransformMatrixLocation, 1, false, transformMatrix, 0)
        GLES20.glUniform1f(uIntensityLocation, lutIntensity)

        // 设置顶点属性
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            aPositionLocation,
            2, GLES20.GL_FLOAT, false, 0, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        texCoordBuffer.position(0)
        GLES20.glVertexAttribPointer(
            aTexCoordLocation,
            4, GLES20.GL_FLOAT, false, 0, texCoordBuffer
        )
        GLES20.glEnableVertexAttribArray(aTexCoordLocation)

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 清理
        GLES20.glDisableVertexAttribArray(aPositionLocation)
        GLES20.glDisableVertexAttribArray(aTexCoordLocation)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    /**
     * 加载所有 LUT 纹理
     */
    private fun loadAllLUTs() {
        Log.d(TAG, "loadAllLUTs: 开始加载 ${lutFilters.size} 个 LUT")
        
        lutFilters.forEach { filter ->
            filter.textureId = loadLUTFromAssets(context.assets, filter.fileName)
            Log.d(TAG, "加载 LUT: ${filter.name}, textureId=${filter.textureId}")
        }
        
        Log.d(TAG, "loadAllLUTs: 所有 LUT 加载完成")
        onLUTsLoadedCallback?.invoke()
    }

    /**
     * 从 Assets 加载 LUT 纹理
     */
    private fun loadLUTFromAssets(assetManager: AssetManager, fileName: String): Int {
        val bitmap = try {
            assetManager.open(fileName).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载 LUT 失败: $fileName", e)
            return 0
        }
        
        return loadLUTTexture(bitmap)
    }

    /**
     * 创建 LUT 纹理
     */
    private fun loadLUTTexture(bitmap: Bitmap?): Int {
        if (bitmap == null) return 0

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)

        val textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // 重要：LUT 必须使用线性过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        bitmap.recycle()

        return textureId
    }

    /**
     * 创建 OES 纹理
     */
    private fun createOESTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)

        val textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

        return textureId
    }

    fun getSurfaceTexture(): SurfaceTexture {
        while (!surfaceTextureReady) {
            Thread.sleep(10)
        }
        return surfaceTexture
    }

    /**
     * 切换到下一个滤镜
     */
    fun nextFilter() {
        currentFilterIndex = (currentFilterIndex + 1) % lutFilters.size
        Log.d(TAG, "切换滤镜: ${getCurrentFilter().name}")
    }

    /**
     * 切换到上一个滤镜
     */
    fun previousFilter() {
        currentFilterIndex = if (currentFilterIndex == 0) {
            lutFilters.size - 1
        } else {
            currentFilterIndex - 1
        }
        Log.d(TAG, "切换滤镜: ${getCurrentFilter().name}")
    }

    /**
     * 设置滤镜（通过索引）
     */
    fun setFilter(index: Int) {
        if (index in lutFilters.indices) {
            currentFilterIndex = index
            Log.d(TAG, "设置滤镜: ${getCurrentFilter().name}")
        }
    }

    /**
     * 设置 LUT 强度
     */
    fun setIntensity(intensity: Float) {
        lutIntensity = intensity.coerceIn(0f, 1f)
        Log.d(TAG, "设置强度: $lutIntensity")
    }

    /**
     * 获取当前滤镜
     */
    fun getCurrentFilter(): LUTFilter {
        return lutFilters[currentFilterIndex]
    }

    /**
     * 获取所有滤镜
     */
    fun getAllFilters(): List<LUTFilter> {
        return lutFilters
    }

    /**
     * 按分类获取滤镜
     */
    fun getFiltersByCategory(category: LUTFilter.Category): List<LUTFilter> {
        return if (category == LUTFilter.Category.ALL) {
            lutFilters
        } else {
            lutFilters.filter { it.category == category }
        }
    }

    fun release() {
        Log.d(TAG, "release: 释放资源")
        
        if (::surfaceTexture.isInitialized) {
            surfaceTexture.release()
        }
        
        // 删除 LUT 纹理
        lutFilters.forEach { filter ->
            if (filter.textureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(filter.textureId), 0)
            }
        }
        
        if (oesTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
        }
    }

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

        Log.d(TAG, "loadShader: $shaderType 编译成功")
        return shader
    }

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

