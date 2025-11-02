# Day 07: FBO（帧缓冲对象）

## 📚 今日目标

- 理解 FBO 的概念和作用
- 掌握离屏渲染（Render to Texture）
- 实现多通道渲染效果
- 学会使用 FBO 实现图像后处理

## 🎯 学习内容

### 1. FBO 简介

**FBO（Frame Buffer Object）** 是 OpenGL ES 中用于离屏渲染的核心技术。

#### 什么是帧缓冲？

在 OpenGL 中，渲染的最终目标是一个**帧缓冲**：

```
默认帧缓冲（屏幕）
  ├── 颜色缓冲（Color Buffer）  - 存储像素颜色
  ├── 深度缓冲（Depth Buffer）  - 存储深度信息
  └── 模板缓冲（Stencil Buffer）- 存储模板信息
```

- **默认帧缓冲**：由系统创建，绑定到屏幕，ID 为 0
- **FBO**：自定义帧缓冲，可以渲染到纹理而不是屏幕

#### FBO 的作用

| 应用场景 | 说明 |
|---------|------|
| **离屏渲染** | 渲染到纹理而非屏幕，后续可以使用该纹理 |
| **多通道渲染** | 先渲染到 FBO，再对纹理进行二次处理 |
| **后处理效果** | 模糊、锐化、边缘检测等图像滤镜 |
| **镜像/水面反射** | 先渲染场景到纹理，再作为反射使用 |
| **阴影映射** | 从光源视角渲染深度图 |
| **延迟渲染** | 分多个阶段渲染复杂场景 |

### 2. FBO 的组成

一个完整的 FBO 包含：

```
FBO (Frame Buffer Object)
  ├── 颜色附件（Color Attachment）
  │   └── 纹理对象（Texture）或渲染缓冲（Renderbuffer）
  ├── 深度附件（Depth Attachment）- 可选
  │   └── 渲染缓冲（Renderbuffer）
  └── 模板附件（Stencil Attachment）- 可选
      └── 渲染缓冲（Renderbuffer）
```

**纹理 vs 渲染缓冲**：

| 特性 | 纹理（Texture） | 渲染缓冲（Renderbuffer） |
|------|----------------|----------------------|
| **读取** | 可以在着色器中采样 | 不能被采样 |
| **用途** | 需要后续使用（颜色附件） | 仅辅助渲染（深度/模板） |
| **性能** | 稍慢 | 更快 |
| **示例** | 渲染到纹理，再应用滤镜 | 深度测试、模板测试 |

### 3. FBO 的创建和使用流程

#### 3.1 创建 FBO

```kotlin
// 1. 生成 FBO
val fbo = IntArray(1)
GLES20.glGenFramebuffers(1, fbo, 0)

// 2. 绑定 FBO
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])

// 3. 创建纹理（颜色附件）
val texture = IntArray(1)
GLES20.glGenTextures(1, texture, 0)
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])

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
    null  // 不填充数据
)

// 4. 将纹理附加到 FBO
GLES20.glFramebufferTexture2D(
    GLES20.GL_FRAMEBUFFER,
    GLES20.GL_COLOR_ATTACHMENT0,  // 颜色附件 0
    GLES20.GL_TEXTURE_2D,
    texture[0],
    0
)

// 5. 检查 FBO 完整性
val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
    Log.e(TAG, "FBO 不完整: $status")
}

// 6. 解绑 FBO
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
```

#### 3.2 使用 FBO 渲染

```kotlin
// 第一步：渲染到 FBO（离屏渲染）
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
GLES20.glViewport(0, 0, width, height)
GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

// 绘制内容到 FBO...
drawScene()

// 第二步：渲染到屏幕（使用 FBO 的纹理）
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
GLES20.glViewport(0, 0, screenWidth, screenHeight)
GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

// 使用 FBO 的纹理绘制到屏幕
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
drawQuad()  // 绘制全屏四边形
```

#### 3.3 完整的渲染流程

```
原始图片
    ↓
┌──────────────────┐
│  Pass 1: FBO 渲染   │  → 渲染到纹理 A
│  (离屏渲染)         │
└──────────────────┘
    ↓
┌──────────────────┐
│  Pass 2: 屏幕渲染   │  → 使用纹理 A + 应用滤镜
│  (后处理)          │
└──────────────────┘
    ↓
  显示到屏幕
```

### 4. 多通道渲染示例

#### 4.1 高斯模糊（两次 Pass）

```kotlin
// Pass 1: 水平模糊
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboHorizontal)
applyHorizontalBlur(originalTexture)

// Pass 2: 垂直模糊
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboVertical)
applyVerticalBlur(horizontalBlurTexture)

// Pass 3: 渲染到屏幕
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
drawTexture(verticalBlurTexture)
```

#### 4.2 灰度滤镜（单次 Pass）

```glsl
// 片段着色器
precision mediump float;
varying vec2 vTexCoord;
uniform sampler2D uTexture;

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);

    // 灰度公式（加权平均）
    float gray = color.r * 0.299 + color.g * 0.587 + color.b * 0.114;

    gl_FragColor = vec4(gray, gray, gray, color.a);
}
```

### 5. 常见滤镜实现

#### 5.1 反色滤镜

```glsl
void main() {
    vec4 color = texture2D(uTexture, vTexCoord);
    gl_FragColor = vec4(1.0 - color.rgb, color.a);
}
```

#### 5.2 亮度调整

```glsl
uniform float uBrightness;  // -1.0 到 1.0

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);
    gl_FragColor = vec4(color.rgb + uBrightness, color.a);
}
```

#### 5.3 对比度调整

```glsl
uniform float uContrast;  // 0.0 到 2.0

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);
    vec3 adjusted = (color.rgb - 0.5) * uContrast + 0.5;
    gl_FragColor = vec4(adjusted, color.a);
}
```

#### 5.4 饱和度调整

```glsl
uniform float uSaturation;  // 0.0 到 2.0

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);

    // 计算灰度值
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));

    // 在灰度和原色之间插值
    vec3 result = mix(vec3(gray), color.rgb, uSaturation);

    gl_FragColor = vec4(result, color.a);
}
```

#### 5.5 边缘检测（Sobel 算子）

```glsl
uniform sampler2D uTexture;
uniform vec2 uTexelSize;  // 1.0 / 纹理尺寸

varying vec2 vTexCoord;

void main() {
    // Sobel 算子
    float tl = texture2D(uTexture, vTexCoord + uTexelSize * vec2(-1.0,  1.0)).r;
    float tm = texture2D(uTexture, vTexCoord + uTexelSize * vec2( 0.0,  1.0)).r;
    float tr = texture2D(uTexture, vTexCoord + uTexelSize * vec2( 1.0,  1.0)).r;

    float ml = texture2D(uTexture, vTexCoord + uTexelSize * vec2(-1.0,  0.0)).r;
    float mr = texture2D(uTexture, vTexCoord + uTexelSize * vec2( 1.0,  0.0)).r;

    float bl = texture2D(uTexture, vTexCoord + uTexelSize * vec2(-1.0, -1.0)).r;
    float bm = texture2D(uTexture, vTexCoord + uTexelSize * vec2( 0.0, -1.0)).r;
    float br = texture2D(uTexture, vTexCoord + uTexelSize * vec2( 1.0, -1.0)).r;

    float gx = -tl + tr - 2.0 * ml + 2.0 * mr - bl + br;
    float gy = tl + 2.0 * tm + tr - bl - 2.0 * bm - br;

    float edge = length(vec2(gx, gy));

    gl_FragColor = vec4(vec3(edge), 1.0);
}
```

### 6. FBO 的性能优化

#### 6.1 复用 FBO

```kotlin
// 不好的做法：每帧创建和销毁 FBO
fun onDrawFrame() {
    val fbo = createFBO()
    renderToFBO(fbo)
    deleteFBO(fbo)  // ❌ 性能差
}

// 好的做法：复用 FBO
fun onSurfaceCreated() {
    fbo = createFBO()  // ✅ 只创建一次
}

fun onDrawFrame() {
    renderToFBO(fbo)
}
```

#### 6.2 选择合适的纹理格式

```kotlin
// 对于不需要 Alpha 通道的场景
GLES20.glTexImage2D(
    GLES20.GL_TEXTURE_2D,
    0,
    GLES20.GL_RGB,  // 使用 RGB 而非 RGBA
    width,
    height,
    0,
    GLES20.GL_RGB,
    GLES20.GL_UNSIGNED_BYTE,
    null
)
```

#### 6.3 降低渲染分辨率

```kotlin
// 对于模糊效果，可以降低分辨率
val fboWidth = width / 2
val fboHeight = height / 2
```

### 7. 错误检查

#### 7.1 FBO 完整性检查

```kotlin
fun checkFBOStatus(): Boolean {
    val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
    return when (status) {
        GLES20.GL_FRAMEBUFFER_COMPLETE -> {
            Log.d(TAG, "FBO 创建成功")
            true
        }
        GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> {
            Log.e(TAG, "FBO 附件不完整")
            false
        }
        GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> {
            Log.e(TAG, "FBO 缺少附件")
            false
        }
        GLES20.GL_FRAMEBUFFER_UNSUPPORTED -> {
            Log.e(TAG, "FBO 配置不支持")
            false
        }
        else -> {
            Log.e(TAG, "FBO 未知错误: $status")
            false
        }
    }
}
```

#### 7.2 OpenGL 错误检查

```kotlin
fun checkGLError(tag: String) {
    var error: Int
    while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
        Log.e(TAG, "$tag: glError $error")
    }
}
```

## 💻 代码实践

### 今日任务

实现一个图像滤镜应用：

1. **使用 FBO 进行离屏渲染**
2. **实现多种滤镜效果**：
   - 原图（无滤镜）
   - 灰度滤镜
   - 反色滤镜
   - 模糊滤镜
   - 边缘检测
3. **提供滤镜切换功能**

### 实现效果

- 📷 加载图片并渲染到 FBO
- 🎨 在 FBO 纹理上应用不同滤镜
- 🔄 实时切换滤镜效果
- 📊 对比原图和滤镜效果

### 核心代码结构

```kotlin
class Day07Renderer(context: Context) : GLSurfaceView.Renderer {

    // FBO 相关
    private var fbo = IntArray(1)
    private var fboTexture = IntArray(1)

    // 滤镜类型
    enum class FilterType {
        NONE,      // 无滤镜
        GRAYSCALE, // 灰度
        INVERT,    // 反色
        BLUR,      // 模糊
        EDGE       // 边缘检测
    }

    private var currentFilter = FilterType.NONE

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 创建 FBO
        createFBO(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Pass 1: 渲染原图到 FBO
        renderToFBO()

        // Pass 2: 应用滤镜并渲染到屏幕
        renderToScreen()
    }

    private fun createFBO(width: Int, height: Int) {
        // 生成 FBO
        GLES20.glGenFramebuffers(1, fbo, 0)

        // 创建纹理...
        // 附加到 FBO...
        // 检查完整性...
    }

    private fun renderToFBO() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
        // 绘制原图...
    }

    private fun renderToScreen() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        // 应用滤镜并绘制...
    }
}
```

## 🧪 练习任务

### 基础任务

1. ✅ 创建 FBO 并渲染图片
2. ✅ 实现至少 3 种滤镜效果
3. ✅ 实现滤镜切换功能

### 进阶任务

1. 📊 实现滤镜参数调节（如模糊半径、亮度、对比度）
2. 🎨 实现复古滤镜（暖色调 + 降低饱和度）
3. 🔍 实现卷积核滤镜（锐化、浮雕）
4. 📸 添加滤镜预览缩略图

### 挑战任务

1. 🚀 实现多 Pass 渲染（高斯模糊 = 水平模糊 + 垂直模糊）
2. 🎭 实现 LUT（Look-Up Table）颜色查找表滤镜
3. 🌈 实现 HSV 颜色空间调整
4. 💾 保存滤镜处理后的图片到相册

## 📖 知识点总结

### FBO vs 默认帧缓冲

| 特性 | 默认帧缓冲 | FBO |
|------|-----------|-----|
| **创建者** | 系统创建 | 开发者创建 |
| **ID** | 0 | 非 0 |
| **输出目标** | 屏幕 | 纹理/渲染缓冲 |
| **用途** | 显示最终画面 | 离屏渲染、后处理 |

### 附件类型

| 附件 | 用途 | 常用类型 |
|------|------|---------|
| **颜色附件** | 存储渲染结果 | 纹理（可采样） |
| **深度附件** | 深度测试 | 渲染缓冲 |
| **模板附件** | 模板测试 | 渲染缓冲 |

### 最佳实践

1. ✅ **在 onSurfaceCreated 中创建 FBO**，而非每帧创建
2. ✅ **检查 FBO 完整性**，避免运行时错误
3. ✅ **使用合适的纹理格式**，RGB 比 RGBA 节省内存
4. ✅ **绑定 FBO 前保存状态**，绘制完成后恢复
5. ✅ **及时检查 OpenGL 错误**，使用 glGetError()

## 🐛 常见问题

### Q1: FBO 渲染结果是黑屏？

**可能原因**：
1. FBO 附件未正确配置
2. 纹理未正确分配存储空间
3. 未检查 FBO 完整性
4. 视口（Viewport）设置错误

**解决方法**：
```kotlin
// 1. 检查 FBO 完整性
if (!checkFBOStatus()) {
    Log.e(TAG, "FBO 创建失败")
}

// 2. 确保纹理已分配存储
GLES20.glTexImage2D(
    GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
    width, height, 0,
    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
    null  // 不能省略
)

// 3. 设置正确的视口
GLES20.glViewport(0, 0, fboWidth, fboHeight)
```

### Q2: 如何调试 FBO 渲染结果？

```kotlin
// 方法 1：读取 FBO 像素数据
val pixels = ByteBuffer.allocateDirect(width * height * 4)
GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
// 将 pixels 保存为图片查看

// 方法 2：直接渲染 FBO 纹理到屏幕（不应用滤镜）
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTexture[0])
drawQuad()  // 应该能看到原图
```

### Q3: FBO 纹理上下颠倒？

OpenGL 的纹理坐标原点在左下角，而图片通常原点在左上角。

**解决方法 1**：翻转纹理坐标
```kotlin
val texCoords = floatArrayOf(
    0f, 1f,  // 左上 → 左下
    0f, 0f,  // 左下 → 左上
    1f, 1f,  // 右上 → 右下
    1f, 0f   // 右下 → 右上
)
```

**解决方法 2**：在着色器中翻转
```glsl
varying vec2 vTexCoord;
void main() {
    vec2 flipped = vec2(vTexCoord.x, 1.0 - vTexCoord.y);
    gl_FragColor = texture2D(uTexture, flipped);
}
```

### Q4: 如何实现高斯模糊？

高斯模糊需要两次 Pass（分离卷积）：

```kotlin
// Pass 1: 水平模糊
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboHorizontal)
useShader(horizontalBlurShader)
drawTexture(originalTexture)

// Pass 2: 垂直模糊
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboVertical)
useShader(verticalBlurShader)
drawTexture(horizontalBlurTexture)

// Pass 3: 显示结果
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
drawTexture(verticalBlurTexture)
```

## 🔗 参考资料

### 官方文档
- [OpenGL ES FBO 规范](https://www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glFramebufferTexture2D.xml)
- [Android GLSurfaceView](https://developer.android.com/reference/android/opengl/GLSurfaceView)

### 推荐阅读
- 《OpenGL ES 3.0 Programming Guide》- Chapter 10: FBO
- [GPUImage](https://github.com/cats-oss/android-gpuimage) - 开源滤镜库

### 滤镜算法参考
- [Image Processing Kernels](https://en.wikipedia.org/wiki/Kernel_(image_processing))
- [Sobel Operator](https://en.wikipedia.org/wiki/Sobel_operator)

## 📝 今日总结

今天我们深入学习了 FBO 和离屏渲染：

1. ✅ 理解了 FBO 的概念：自定义帧缓冲，渲染到纹理
2. ✅ 掌握了 FBO 的创建流程：生成 → 绑定 → 附加纹理 → 检查完整性
3. ✅ 学会了多通道渲染：先渲染到 FBO，再应用滤镜
4. ✅ 实现了多种图像滤镜效果

**关键要点**：
- FBO 允许我们渲染到纹理而非屏幕
- 离屏渲染是实现图像后处理的基础
- 多 Pass 渲染可以实现复杂效果（如高斯模糊）
- 滤镜本质是在片段着色器中修改像素颜色

明天我们将学习 **多重纹理与混合**，实现更复杂的视觉效果！🎉

---

**完成打卡**：学完本节后，请在 `LEARNING_PROGRESS.md` 中勾选 Day 07 ✅
