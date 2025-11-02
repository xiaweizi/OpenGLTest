# Day 06: EGL 与 GLSurfaceView 深入

## 📚 今日目标

- 理解 EGL 的作用和初始化流程
- 掌握 GLSurfaceView 的渲染模式
- 学会渲染模式的切换和应用场景
- 了解 EGL 上下文管理和线程模型

## 🎯 学习内容

### 1. EGL 简介

**EGL（Embedded Graphics Library）** 是 OpenGL ES 和原生窗口系统之间的接口层。它的主要作用：

- **创建渲染上下文**：管理 OpenGL ES 的上下文环境
- **创建渲染表面**：将 OpenGL 渲染结果输出到屏幕或离屏缓冲
- **同步渲染**：处理双缓冲和垂直同步
- **管理资源**：管理 OpenGL 的显示设备和资源

#### EGL 核心概念

```
EGLDisplay  : 代表显示设备（物理屏幕）
EGLConfig   : 帧缓冲配置（颜色格式、深度缓冲等）
EGLContext  : OpenGL ES 的渲染上下文（保存状态）
EGLSurface  : 渲染表面（Window Surface / Pbuffer Surface / Pixmap Surface）
```

#### EGL 初始化流程

```kotlin
// 1. 获取默认显示设备
val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

// 2. 初始化 EGL
val version = IntArray(2)
EGL14.eglInitialize(display, version, 0, version, 1)

// 3. 选择 EGL 配置
val configs = arrayOfNulls<EGLConfig>(1)
val numConfigs = IntArray(1)
EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0)

// 4. 创建 EGL 上下文
val context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

// 5. 创建 EGL 表面
val surface = EGL14.eglCreateWindowSurface(display, configs[0], nativeWindow, surfaceAttribs, 0)

// 6. 绑定上下文和表面
EGL14.eglMakeCurrent(display, surface, surface, context)

// 7. 渲染...

// 8. 交换缓冲（显示渲染结果）
EGL14.eglSwapBuffers(display, surface)
```

### 2. GLSurfaceView 封装

幸运的是，Android 提供的 `GLSurfaceView` 已经为我们封装了 EGL 的复杂操作：

- ✅ 自动创建 EGL Display、Config、Context、Surface
- ✅ 自动管理渲染线程（GLThread）
- ✅ 自动处理生命周期（onResume / onPause）
- ✅ 提供简洁的 Renderer 接口

**GLSurfaceView 的工作原理**：

```
主线程（UI Thread）
  └── 创建 GLSurfaceView
  └── 设置 Renderer
  └── 调用 setContentView()

渲染线程（GL Thread）
  └── EGL 初始化
  └── onSurfaceCreated()  ← 初始化 OpenGL 资源
  └── onSurfaceChanged()  ← 窗口大小改变
  └── 循环调用 onDrawFrame() ← 绘制每一帧
  └── eglSwapBuffers()    ← 交换缓冲显示结果
```

### 3. 渲染模式详解

GLSurfaceView 提供两种渲染模式：

#### 3.1 持续渲染模式（RENDERMODE_CONTINUOUSLY）

```kotlin
glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
```

**特点**：
- 不断循环调用 `onDrawFrame()`，类似游戏循环
- 默认约 60 FPS（受垂直同步限制）
- 适用于动画、游戏、实时预览等场景

**优点**：
- 画面流畅，适合动态内容
- 实现简单，不需要手动请求渲染

**缺点**：
- CPU 和 GPU 持续工作，耗电
- 即使画面没有变化也在渲染

**使用场景**：
- ✅ 3D 游戏渲染
- ✅ 实时相机预览
- ✅ 持续动画效果
- ✅ 粒子系统

#### 3.2 按需渲染模式（RENDERMODE_WHEN_DIRTY）

```kotlin
glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
```

**特点**：
- 只在调用 `requestRender()` 时才渲染一帧
- 静止时不消耗 GPU 资源
- 适用于静态内容或手动触发更新的场景

**优点**：
- 省电，节省资源
- 适合静态或低频更新的内容

**缺点**：
- 需要手动调用 `requestRender()`
- 不适合高频动画

**使用场景**：
- ✅ 静态图片显示
- ✅ 用户交互触发的渲染（如手势缩放）
- ✅ 低频更新的图表
- ✅ 离屏渲染

**手动触发渲染**：
```kotlin
// 在需要更新画面时调用
glSurfaceView.requestRender()
```

### 4. 渲染模式切换示例

```kotlin
class MyRenderer : GLSurfaceView.Renderer {
    var rotation = 0f
    var isAnimating = true

    override fun onDrawFrame(gl: GL10?) {
        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 动画逻辑
        if (isAnimating) {
            rotation += 2f
            if (rotation >= 360f) rotation = 0f
        }

        // 绘制旋转的三角形...
    }
}

// 切换渲染模式
fun toggleRenderMode() {
    if (glSurfaceView.renderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
        // 切换到按需渲染
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        renderer.isAnimating = false
    } else {
        // 切换到持续渲染
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        renderer.isAnimating = true
    }
}
```

### 5. EGL 上下文共享

在某些场景下，我们需要在多个线程之间共享 OpenGL 资源（如纹理、着色器）：

```kotlin
// 创建共享上下文
val sharedContext = EGL14.eglCreateContext(
    display,
    config,
    mainContext,  // 主上下文
    contextAttribs,
    0
)
```

**应用场景**：
- 后台线程预加载纹理
- 多个 GLSurfaceView 共享纹理
- 视频编码线程访问 OpenGL 纹理

**注意事项**：
- ⚠️ 每个线程同时只能有一个活动上下文
- ⚠️ 不能在不同线程同时修改同一个资源
- ⚠️ 需要手动管理同步（使用 Fence Sync）

### 6. 离屏渲染基础

离屏渲染（Offscreen Rendering）是指渲染到非窗口表面（如 Pbuffer 或 FBO）：

#### 6.1 Pbuffer Surface

```kotlin
val surfaceAttribs = intArrayOf(
    EGL14.EGL_WIDTH, 1024,
    EGL14.EGL_HEIGHT, 1024,
    EGL14.EGL_NONE
)

val pbufferSurface = EGL14.eglCreatePbufferSurface(
    display,
    config,
    surfaceAttribs,
    0
)
```

#### 6.2 FBO（Frame Buffer Object）

FBO 是更现代、更灵活的离屏渲染方式（Day 7 详细学习）：

```kotlin
val fbo = IntArray(1)
GLES20.glGenFramebuffers(1, fbo, 0)
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
// 附加纹理...
```

### 7. 性能优化建议

#### 选择合适的渲染模式
- 动态内容 → CONTINUOUSLY
- 静态内容 → WHEN_DIRTY

#### 避免过度绘制
```kotlin
// 在按需模式下，避免不必要的 requestRender()
var isDirty = false

fun updateData() {
    // 数据改变
    isDirty = true
    glSurfaceView.requestRender()
}

override fun onDrawFrame(gl: GL10?) {
    if (isDirty) {
        // 重新渲染
        isDirty = false
    }
}
```

#### 使用垂直同步
GLSurfaceView 默认启用垂直同步（VSync），防止画面撕裂。

## 💻 代码实践

### 今日任务

实现一个可以切换渲染模式的 Demo：

1. **渲染一个旋转的纹理图片**
2. **提供按钮切换渲染模式**
   - 持续渲染模式：图片持续旋转
   - 按需渲染模式：图片静止，点击按钮才旋转一步
3. **显示当前渲染模式和 FPS**

### 实现效果

- 🔄 持续渲染模式：图片平滑旋转（60 FPS）
- ⏸️ 按需渲染模式：图片静止
- 🎮 点击"旋转一步"：手动触发一次渲染，旋转 5 度
- 🔁 点击"切换模式"：在两种模式间切换

### 核心代码

#### Day06Renderer.kt

```kotlin
class Day06Renderer(private val context: Context) : GLSurfaceView.Renderer {
    var rotation = 0f
    var renderMode = RenderMode.CONTINUOUSLY

    override fun onDrawFrame(gl: GL10?) {
        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 更新旋转角度
        if (renderMode == RenderMode.CONTINUOUSLY) {
            rotation += 2f
            if (rotation >= 360f) rotation = 0f
        }

        // 绘制旋转的图片...
    }

    fun rotateStep() {
        rotation += 5f
        if (rotation >= 360f) rotation = 0f
    }

    enum class RenderMode {
        CONTINUOUSLY,
        WHEN_DIRTY
    }
}
```

#### Day06Activity.kt

```kotlin
class Day06Activity : BaseGLActivity() {
    private lateinit var renderer: Day06Renderer

    override fun createRenderer(): GLSurfaceView.Renderer {
        renderer = Day06Renderer(this)
        return renderer
    }

    private fun toggleRenderMode() {
        if (glSurfaceView.renderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            renderer.renderMode = Day06Renderer.RenderMode.WHEN_DIRTY
        } else {
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            renderer.renderMode = Day06Renderer.RenderMode.CONTINUOUSLY
        }
    }

    private fun rotateStep() {
        renderer.rotateStep()
        glSurfaceView.requestRender()
    }
}
```

## 🧪 练习任务

### 基础任务

1. ✅ 实现渲染模式的切换功能
2. ✅ 在按需模式下，点击按钮手动旋转图片
3. ✅ 观察两种模式的 CPU 占用差异（使用 Android Profiler）

### 进阶任务

1. 📊 显示当前 FPS（帧率）
   - 提示：记录 `onDrawFrame()` 的调用时间间隔
2. 🎨 添加暂停/恢复动画功能
   - 在持续渲染模式下也能暂停旋转
3. 🔍 使用 Android Studio 的 GPU Profiler 分析渲染性能
4. 🧪 尝试手动创建 EGL 环境（不使用 GLSurfaceView）

### 挑战任务

1. 🚀 实现双 GLSurfaceView，使用 EGL 上下文共享同一个纹理
2. 📹 实现离屏渲染，将渲染结果保存为图片
3. 🎮 添加手势控制：拖动旋转图片（按需渲染模式）

## 📖 知识点总结

### 核心概念

| 概念 | 说明 |
|------|------|
| **EGL** | OpenGL ES 与原生窗口系统的接口 |
| **EGLDisplay** | 代表显示设备 |
| **EGLContext** | OpenGL ES 渲染上下文（保存状态） |
| **EGLSurface** | 渲染表面（输出目标） |
| **GLThread** | GLSurfaceView 的渲染线程 |

### 渲染模式对比

| 特性 | CONTINUOUSLY | WHEN_DIRTY |
|------|--------------|------------|
| **调用频率** | 持续调用（~60 FPS） | 手动调用 |
| **CPU 占用** | 高 | 低 |
| **GPU 占用** | 高 | 低 |
| **耗电量** | 高 | 低 |
| **适用场景** | 动画、游戏 | 静态内容 |
| **触发方式** | 自动 | requestRender() |

### 最佳实践

1. ✅ **优先使用 GLSurfaceView**，除非需要精细控制 EGL
2. ✅ **静态内容使用按需渲染**，节省电量
3. ✅ **动态内容使用持续渲染**，保证流畅度
4. ✅ **在 onSurfaceCreated 中初始化资源**，而非 onDrawFrame
5. ✅ **使用 queueEvent() 在 GL 线程执行代码**
   ```kotlin
   glSurfaceView.queueEvent {
       // 在 GL 线程执行
       renderer.updateTexture()
   }
   ```

## 🐛 常见问题

### Q1: 为什么切换到按需模式后，requestRender() 不生效？

**A:** 检查是否在 GL 线程外调用。如果需要在其他线程触发渲染：

```kotlin
// 正确方式
runOnUiThread {
    glSurfaceView.requestRender()
}

// 或使用 queueEvent
glSurfaceView.queueEvent {
    // 更新数据
    renderer.rotation += 10f
    glSurfaceView.requestRender()  // 在 GL 线程内调用
}
```

### Q2: 如何测量实际 FPS？

```kotlin
class Day06Renderer : GLSurfaceView.Renderer {
    private var lastFrameTime = System.nanoTime()
    private var frameCount = 0
    private var fps = 0.0

    override fun onDrawFrame(gl: GL10?) {
        val currentTime = System.nanoTime()
        frameCount++

        if (currentTime - lastFrameTime >= 1_000_000_000) {  // 1 秒
            fps = frameCount.toDouble()
            frameCount = 0
            lastFrameTime = currentTime
            Log.d(TAG, "FPS: $fps")
        }

        // 渲染...
    }
}
```

### Q3: 多个 GLSurfaceView 如何共享纹理？

需要使用 EGL 上下文共享，但 GLSurfaceView 不直接支持。可以：
1. 手动创建 EGL 环境并共享上下文
2. 使用 TextureView + 手动 EGL 管理
3. 将纹理 ID 在不同上下文中重新绑定（不推荐）

### Q4: 为什么修改 Renderer 的变量没有效果？

OpenGL 运行在独立的 GL 线程，修改 Renderer 变量需要同步：

```kotlin
// 方式 1：使用 volatile（简单场景）
class Day06Renderer : GLSurfaceView.Renderer {
    @Volatile var rotation = 0f
}

// 方式 2：使用 queueEvent（推荐）
glSurfaceView.queueEvent {
    renderer.rotation = 45f
}

// 方式 3：使用同步锁（复杂场景）
class Day06Renderer : GLSurfaceView.Renderer {
    private val lock = Object()
    var rotation = 0f
        get() = synchronized(lock) { field }
        set(value) = synchronized(lock) { field = value }
}
```

## 🔗 参考资料

### 官方文档
- [EGL Specification](https://www.khronos.org/registry/EGL/)
- [Android GLSurfaceView](https://developer.android.com/reference/android/opengl/GLSurfaceView)
- [EGL14 API](https://developer.android.com/reference/android/opengl/EGL14)

### 推荐阅读
- 《OpenGL ES 3.0 Programming Guide》- Chapter 3: EGL
- [Grafika](https://github.com/google/grafika) - Google 官方 OpenGL 示例项目

### 调试工具
- Android Studio GPU Profiler
- Logcat（查看 OpenGL 错误）
- `adb shell dumpsys gfxinfo <package-name>` - 查看渲染性能

## 📝 今日总结

今天我们深入学习了 EGL 和 GLSurfaceView 的工作原理：

1. ✅ 理解了 EGL 的作用：OpenGL ES 与窗口系统的桥梁
2. ✅ 掌握了 GLSurfaceView 的两种渲染模式及使用场景
3. ✅ 学会了根据应用场景选择合适的渲染模式
4. ✅ 了解了 EGL 上下文共享和离屏渲染的基础概念

**关键要点**：
- EGL 负责 OpenGL ES 的上下文和表面管理
- GLSurfaceView 封装了 EGL 的复杂操作
- 持续渲染适合动画，按需渲染适合静态内容
- 渲染线程独立于主线程，需要注意线程同步

明天我们将学习 **FBO（帧缓冲对象）**，实现更强大的离屏渲染和多通道效果！🎉

---

**完成打卡**：学完本节后，请在 `LEARNING_PROGRESS.md` 中勾选 Day 06 ✅
