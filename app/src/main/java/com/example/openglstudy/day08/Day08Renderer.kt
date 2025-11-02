package com.example.openglstudy.day08

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
 * Day 08 Renderer
 * 学习目标：多重纹理与混合
 * 实现功能：使用多个纹理单元实现图片混合、水印等效果
 */
class Day08Renderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "Day08Renderer"
    }

    /**
     * 混合模式枚举
     */
    enum class BlendMode(val displayName: String) {
        ALPHA("Alpha 混合"),
        ADD("相加"),
        MULTIPLY("相乘"),
        SCREEN("屏幕"),
        OVERLAY("叠加"),
        SOFT_LIGHT("柔光")
    }

    // 顶点着色器
    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;

        void main() {
            vTexCoord = aTexCoord;
            gl_Position = aPosition;
        }
    """.trimIndent()

    // 片段着色器（多纹理混合）
    private val fragmentShaderCode = """
        precision mediump float;
        
        varying vec2 vTexCoord;
        
        uniform sampler2D uTexture1;  // 底图
        uniform sampler2D uTexture2;  // 叠加图
        uniform int uBlendMode;       // 混合模式（0-5）
        uniform float uAlpha;         // 透明度（0.0 - 1.0）
        
        // 相加混合
        vec3 blendAdd(vec3 base, vec3 blend) {
            return min(base + blend, vec3(1.0));
        }
        
        // 相乘混合
        vec3 blendMultiply(vec3 base, vec3 blend) {
            return base * blend;
        }
        
        // 屏幕混合
        vec3 blendScreen(vec3 base, vec3 blend) {
            return 1.0 - (1.0 - base) * (1.0 - blend);
        }
        
        // 叠加混合
        vec3 blendOverlay(vec3 base, vec3 blend) {
            vec3 result;
            result.r = base.r < 0.5 ? (2.0 * base.r * blend.r) : (1.0 - 2.0 * (1.0 - base.r) * (1.0 - blend.r));
            result.g = base.g < 0.5 ? (2.0 * base.g * blend.g) : (1.0 - 2.0 * (1.0 - base.g) * (1.0 - blend.g));
            result.b = base.b < 0.5 ? (2.0 * base.b * blend.b) : (1.0 - 2.0 * (1.0 - base.b) * (1.0 - blend.b));
            return result;
        }
        
        // 柔光混合
        vec3 blendSoftLight(vec3 base, vec3 blend) {
            vec3 result;
            result.r = blend.r < 0.5 
                ? (2.0 * base.r * blend.r + base.r * base.r * (1.0 - 2.0 * blend.r)) 
                : (sqrt(base.r) * (2.0 * blend.r - 1.0) + 2.0 * base.r * (1.0 - blend.r));
            result.g = blend.g < 0.5 
                ? (2.0 * base.g * blend.g + base.g * base.g * (1.0 - 2.0 * blend.g)) 
                : (sqrt(base.g) * (2.0 * blend.g - 1.0) + 2.0 * base.g * (1.0 - blend.g));
            result.b = blend.b < 0.5 
                ? (2.0 * base.b * blend.b + base.b * base.b * (1.0 - 2.0 * blend.b)) 
                : (sqrt(base.b) * (2.0 * blend.b - 1.0) + 2.0 * base.b * (1.0 - blend.b));
            return result;
        }
        
        void main() {
            vec4 base = texture2D(uTexture1, vTexCoord);
            vec4 blend = texture2D(uTexture2, vTexCoord);
            
            vec3 result;
            
            if (uBlendMode == 0) {
                // Alpha 混合
                result = mix(base.rgb, blend.rgb, uAlpha);
            } else if (uBlendMode == 1) {
                // 相加
                result = blendAdd(base.rgb, blend.rgb * uAlpha);
            } else if (uBlendMode == 2) {
                // 相乘
                result = mix(base.rgb, blendMultiply(base.rgb, blend.rgb), uAlpha);
            } else if (uBlendMode == 3) {
                // 屏幕
                result = mix(base.rgb, blendScreen(base.rgb, blend.rgb), uAlpha);
            } else if (uBlendMode == 4) {
                // 叠加
                result = mix(base.rgb, blendOverlay(base.rgb, blend.rgb), uAlpha);
            } else if (uBlendMode == 5) {
                // 柔光
                result = mix(base.rgb, blendSoftLight(base.rgb, blend.rgb), uAlpha);
            } else {
                result = base.rgb;
            }
            
            gl_FragColor = vec4(result, 1.0);
        }
    """.trimIndent()

    // 全屏四边形顶点（位置 + 纹理坐标）
    private val quadVertices = floatArrayOf(
        // 位置          纹理坐标
        -1f,  1f, 0f,   0f, 1f,  // 左上
        -1f, -1f, 0f,   0f, 0f,  // 左下
         1f,  1f, 0f,   1f, 1f,  // 右上

        -1f, -1f, 0f,   0f, 0f,  // 左下
         1f, -1f, 0f,   1f, 0f,  // 右下
         1f,  1f, 0f,   1f, 1f   // 右上
    )

    private lateinit var vertexBuffer: FloatBuffer

    private var program: Int = 0

    // Uniform 位置
    private var uTexture1Location: Int = 0
    private var uTexture2Location: Int = 0
    private var uBlendModeLocation: Int = 0
    private var uAlphaLocation: Int = 0

    // 多纹理 ID
    private var texture1: Int = 0  // 底图
    private var texture2: Int = 0  // 叠加图

    // 用于从 Glide 接收 Bitmap
    @Volatile
    private var pendingBitmap1: Bitmap? = null
    @Volatile
    private var pendingBitmap2: Bitmap? = null
    @Volatile
    private var needUpdateTexture1 = false
    @Volatile
    private var needUpdateTexture2 = false

    // 当前混合模式和透明度
    @Volatile
    var currentBlendMode: BlendMode = BlendMode.ALPHA
    @Volatile
    var blendAlpha: Float = 0.5f

    private val VERTEX_STRIDE = 5 * 4

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated: 开始初始化")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 创建顶点缓冲
        vertexBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadVertices)
        vertexBuffer.position(0)
        Log.d(TAG, "onSurfaceCreated: 顶点缓冲创建完成")

        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // 创建程序
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        checkProgramLink(program, "Day08 Program")
        Log.d(TAG, "onSurfaceCreated: 着色器程序创建完成")

        // 获取 uniform 位置
        uTexture1Location = GLES20.glGetUniformLocation(program, "uTexture1")
        uTexture2Location = GLES20.glGetUniformLocation(program, "uTexture2")
        uBlendModeLocation = GLES20.glGetUniformLocation(program, "uBlendMode")
        uAlphaLocation = GLES20.glGetUniformLocation(program, "uAlpha")
        Log.d(TAG, "onSurfaceCreated: Uniform 位置获取完成")

        // 使用 Glide 加载两张图片
        loadImagesWithGlide()
    }

    /**
     * 使用 Glide 加载图片
     */
    private fun loadImagesWithGlide() {
        Log.d(TAG, "loadImagesWithGlide: 开始加载图片")

        // 加载底图
        Glide.with(context)
            .asBitmap()
            .load(R.mipmap.icon_test)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Log.d(TAG, "底图加载成功: ${resource.width}x${resource.height}")
                    pendingBitmap1 = resource
                    needUpdateTexture1 = true
                }
            })

        // 加载叠加图（使用 drawable 中的图片）
        Glide.with(context)
            .asBitmap()
            .load(R.drawable.sample_image)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Log.d(TAG, "叠加图加载成功: ${resource.width}x${resource.height}")
                    pendingBitmap2 = resource
                    needUpdateTexture2 = true
                }
            })
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: 视口改变 width=$width, height=$height")
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 检查是否有新的 Bitmap 需要创建纹理
        if (needUpdateTexture1 && pendingBitmap1 != null) {
            if (texture1 != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(texture1), 0)
            }
            texture1 = TextureHelper.loadTexture(pendingBitmap1)
            Log.d(TAG, "onDrawFrame: 底图纹理创建完成，textureId=$texture1")
            needUpdateTexture1 = false
        }

        if (needUpdateTexture2 && pendingBitmap2 != null) {
            if (texture2 != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(texture2), 0)
            }
            texture2 = TextureHelper.loadTexture(pendingBitmap2)
            Log.d(TAG, "onDrawFrame: 叠加图纹理创建完成，textureId=$texture2")
            needUpdateTexture2 = false
        }

        // 只有在两个纹理都加载完成后才渲染
        if (texture1 == 0 || texture2 == 0) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            return
        }

        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 使用着色器程序
        GLES20.glUseProgram(program)

        // 激活纹理单元 0 并绑定底图
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture1)
        GLES20.glUniform1i(uTexture1Location, 0)  // 传递纹理单元索引 0

        // 激活纹理单元 1 并绑定叠加图
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2)
        GLES20.glUniform1i(uTexture2Location, 1)  // 传递纹理单元索引 1

        // 设置混合模式和透明度
        GLES20.glUniform1i(uBlendModeLocation, currentBlendMode.ordinal)
        GLES20.glUniform1f(uAlphaLocation, blendAlpha)

        // 绘制全屏四边形
        drawQuad()

        // 解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    /**
     * 绘制全屏四边形
     */
    private fun drawQuad() {
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
     * 设置混合模式
     */
    fun setBlendMode(mode: BlendMode) {
        currentBlendMode = mode
        Log.d(TAG, "setBlendMode: 切换混合模式为 ${mode.displayName}")
    }

    /**
     * 设置透明度
     */
    fun setAlpha(alpha: Float) {
        blendAlpha = alpha.coerceIn(0f, 1f)
        Log.d(TAG, "setAlpha: 透明度更新为 $blendAlpha")
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

