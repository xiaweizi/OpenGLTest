# Day 08: 多重纹理与混合

## 📚 今日目标

- 理解多纹理单元的概念和使用
- 掌握纹理混合模式（Blend Modes）
- 学习 Alpha 通道处理和透明度
- 实现图片水印、纹理叠加等实用效果

## 🎯 学习内容

### 1. 多纹理单元简介

在 OpenGL ES 中，**纹理单元（Texture Unit）** 允许我们在一次绘制调用中使用多个纹理。

#### 什么是纹理单元？

纹理单元是 OpenGL 中的"纹理槽位"，每个槽位可以绑定一个纹理对象。

```
GPU 纹理单元
├── GL_TEXTURE0 → 纹理 ID: 123（底图）
├── GL_TEXTURE1 → 纹理 ID: 456（水印）
├── GL_TEXTURE2 → 纹理 ID: 789（遮罩）
├── ...
└── GL_TEXTURE31（OpenGL ES 至少支持 8 个，通常支持 16-32 个）
```

#### 为什么需要多纹理？

| 应用场景 | 说明 |
|---------|------|
| **水印效果** | 底图 + 水印图 |
| **遮罩效果** | 原图 + 遮罩图 |
| **光照贴图** | 颜色纹理 + 法线贴图 + 光照贴图 |
| **视频特效** | 视频帧 + 滤镜纹理（LUT） |
| **粒子效果** | 粒子纹理 + 渐变纹理 |

### 2. 多纹理的使用流程

#### 2.1 激活纹理单元

```kotlin
// 激活纹理单元 0
GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
// 绑定第一个纹理
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture1)

// 激活纹理单元 1
GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
// 绑定第二个纹理
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2)
```

#### 2.2 传递给着色器

```kotlin
// 将纹理单元索引传递给着色器的 sampler2D
GLES20.glUniform1i(texture1Location, 0)  // 使用 GL_TEXTURE0
GLES20.glUniform1i(texture2Location, 1)  // 使用 GL_TEXTURE1
```

#### 2.3 着色器中采样

```glsl
precision mediump float;

uniform sampler2D uTexture1;  // 底图
uniform sampler2D uTexture2;  // 水印

varying vec2 vTexCoord;

void main() {
    vec4 color1 = texture2D(uTexture1, vTexCoord);  // 采样底图
    vec4 color2 = texture2D(uTexture2, vTexCoord);  // 采样水印
    
    // 混合两个纹理
    gl_FragColor = mix(color1, color2, color2.a);
}
```

### 3. 纹理混合模式

#### 3.1 常见混合算法

| 混合模式 | 公式 | 效果 |
|---------|------|------|
| **正常（Normal）** | `color2` | 完全覆盖 |
| **Alpha 混合** | `mix(color1, color2, alpha)` | 透明叠加 |
| **相加（Add）** | `color1 + color2` | 增亮效果 |
| **相乘（Multiply）** | `color1 * color2` | 变暗效果 |
| **屏幕（Screen）** | `1.0 - (1.0 - color1) * (1.0 - color2)` | 柔光增亮 |
| **叠加（Overlay）** | 根据亮度混合相乘和屏幕模式 | 保留高光和阴影 |

#### 3.2 着色器实现

```glsl
precision mediump float;

uniform sampler2D uTexture1;  // 底图
uniform sampler2D uTexture2;  // 叠加图
uniform int uBlendMode;       // 混合模式（0-5）
uniform float uAlpha;         // 透明度（0.0 - 1.0）

varying vec2 vTexCoord;

// 正常混合
vec3 blendNormal(vec3 base, vec3 blend) {
    return blend;
}

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
    result.r = blend.r < 0.5 ? (2.0 * base.r * blend.r + base.r * base.r * (1.0 - 2.0 * blend.r)) 
                              : (sqrt(base.r) * (2.0 * blend.r - 1.0) + 2.0 * base.r * (1.0 - blend.r));
    result.g = blend.g < 0.5 ? (2.0 * base.g * blend.g + base.g * base.g * (1.0 - 2.0 * blend.g)) 
                              : (sqrt(base.g) * (2.0 * blend.g - 1.0) + 2.0 * base.g * (1.0 - blend.g));
    result.b = blend.b < 0.5 ? (2.0 * base.b * blend.b + base.b * base.b * (1.0 - 2.0 * blend.b)) 
                              : (sqrt(base.b) * (2.0 * blend.b - 1.0) + 2.0 * base.b * (1.0 - blend.b));
    return result;
}

void main() {
    vec4 base = texture2D(uTexture1, vTexCoord);
    vec4 blend = texture2D(uTexture2, vTexCoord);
    
    vec3 result;
    
    if (uBlendMode == 0) {
        // 正常混合（Alpha 混合）
        result = mix(base.rgb, blend.rgb, blend.a * uAlpha);
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
```

### 4. Alpha 通道处理

#### 4.1 什么是 Alpha 通道？

**Alpha 通道**表示像素的不透明度：
- `alpha = 0.0`：完全透明
- `alpha = 0.5`：半透明
- `alpha = 1.0`：完全不透明

#### 4.2 启用 Alpha 混合

OpenGL 提供了硬件级别的 Alpha 混合：

```kotlin
// 启用混合
GLES20.glEnable(GLES20.GL_BLEND)

// 设置混合函数
GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
```

**混合公式**：
```
最终颜色 = 源颜色 × 源因子 + 目标颜色 × 目标因子
       = src.rgb × src.a + dst.rgb × (1.0 - src.a)
```

#### 4.3 常见混合函数

| 源因子 | 目标因子 | 效果 |
|-------|---------|------|
| `GL_SRC_ALPHA` | `GL_ONE_MINUS_SRC_ALPHA` | 标准 Alpha 混合（透明叠加） |
| `GL_ONE` | `GL_ONE` | 相加混合（增亮） |
| `GL_DST_COLOR` | `GL_ZERO` | 相乘混合（变暗） |
| `GL_ONE` | `GL_ONE_MINUS_SRC_ALPHA` | 预乘 Alpha 混合 |

### 5. 实用效果实现

#### 5.1 水印效果

```glsl
// 水印着色器
precision mediump float;

uniform sampler2D uTexture;    // 底图
uniform sampler2D uWatermark;  // 水印
uniform vec2 uWatermarkPos;    // 水印位置（0.0 - 1.0）
uniform vec2 uWatermarkSize;   // 水印大小（0.0 - 1.0）
uniform float uWatermarkAlpha; // 水印透明度

varying vec2 vTexCoord;

void main() {
    vec4 baseColor = texture2D(uTexture, vTexCoord);
    
    // 计算水印区域
    vec2 watermarkCoord = (vTexCoord - uWatermarkPos) / uWatermarkSize;
    
    // 判断是否在水印区域
    if (watermarkCoord.x >= 0.0 && watermarkCoord.x <= 1.0 &&
        watermarkCoord.y >= 0.0 && watermarkCoord.y <= 1.0) {
        
        vec4 watermarkColor = texture2D(uWatermark, watermarkCoord);
        
        // Alpha 混合水印
        gl_FragColor = mix(baseColor, watermarkColor, watermarkColor.a * uWatermarkAlpha);
    } else {
        gl_FragColor = baseColor;
    }
}
```

#### 5.2 双重曝光效果

```glsl
precision mediump float;

uniform sampler2D uTexture1;
uniform sampler2D uTexture2;
uniform float uMixRatio;  // 混合比例

varying vec2 vTexCoord;

void main() {
    vec4 color1 = texture2D(uTexture1, vTexCoord);
    vec4 color2 = texture2D(uTexture2, vTexCoord);
    
    // 屏幕混合模式（双重曝光常用）
    vec3 result = 1.0 - (1.0 - color1.rgb) * (1.0 - color2.rgb * uMixRatio);
    
    gl_FragColor = vec4(result, 1.0);
}
```

#### 5.3 遮罩效果

```glsl
precision mediump float;

uniform sampler2D uTexture;  // 原图
uniform sampler2D uMask;     // 遮罩（黑白图）

varying vec2 vTexCoord;

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);
    vec4 mask = texture2D(uMask, vTexCoord);
    
    // 使用遮罩的亮度作为 Alpha
    float maskAlpha = dot(mask.rgb, vec3(0.299, 0.587, 0.114));
    
    gl_FragColor = vec4(color.rgb, color.a * maskAlpha);
}
```

### 6. 性能优化

#### 6.1 纹理单元管理

```kotlin
class TextureManager {
    companion object {
        const val TEXTURE_UNIT_BASE = 0
        const val TEXTURE_UNIT_BLEND = 1
        const val TEXTURE_UNIT_WATERMARK = 2
    }
    
    fun bindTextures(baseTexture: Int, blendTexture: Int, watermarkTexture: Int) {
        // 底图
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + TEXTURE_UNIT_BASE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, baseTexture)
        
        // 混合图
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + TEXTURE_UNIT_BLEND)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blendTexture)
        
        // 水印
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + TEXTURE_UNIT_WATERMARK)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, watermarkTexture)
    }
}
```

#### 6.2 减少纹理切换

```kotlin
// ❌ 不好的做法：频繁切换纹理单元
fun renderObjects(objects: List<RenderObject>) {
    objects.forEach { obj ->
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, obj.texture)
        obj.draw()
    }
}

// ✅ 好的做法：批量渲染相同纹理的对象
fun renderObjectsBatched(objects: List<RenderObject>) {
    val grouped = objects.groupBy { it.texture }
    grouped.forEach { (texture, objs) ->
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        objs.forEach { it.draw() }
    }
}
```

#### 6.3 纹理压缩

对于水印等辅助纹理，可以使用较低分辨率或压缩格式：

```kotlin
// 调整水印尺寸
val watermarkSize = 256  // 不需要很高分辨率
val scaledWatermark = Bitmap.createScaledBitmap(
    originalWatermark, 
    watermarkSize, 
    watermarkSize, 
    true
)
```

### 7. 多纹理的限制

#### 7.1 查询纹理单元数量

```kotlin
val maxTextureUnits = IntArray(1)
GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, maxTextureUnits, 0)
Log.d(TAG, "设备支持的最大纹理单元数: ${maxTextureUnits[0]}")
```

OpenGL ES 2.0 规范要求至少支持 8 个纹理单元，大多数设备支持 16-32 个。

#### 7.2 纹理内存管理

```kotlin
// 监控纹理内存使用
fun estimateTextureMemory(width: Int, height: Int, format: Int): Int {
    val bytesPerPixel = when (format) {
        GLES20.GL_RGBA -> 4
        GLES20.GL_RGB -> 3
        GLES20.GL_LUMINANCE_ALPHA -> 2
        GLES20.GL_LUMINANCE -> 1
        else -> 4
    }
    return width * height * bytesPerPixel
}

// 示例：1920x1080 RGBA 纹理 = 8.3 MB
val memory = estimateTextureMemory(1920, 1080, GLES20.GL_RGBA)
Log.d(TAG, "纹理内存占用: ${memory / 1024 / 1024} MB")
```

## 💻 代码实践

### 今日任务

实现一个多纹理混合应用：

1. **加载两张图片**
2. **实现多种混合模式**：
   - Alpha 混合
   - 相加
   - 相乘
   - 屏幕
   - 叠加
   - 柔光
3. **添加水印功能**
4. **提供透明度调节**

### 实现效果

- 🖼️ 同时加载两张纹理
- 🎨 6 种混合模式实时切换
- 💧 透明度 SeekBar 调节
- 🏷️ 水印位置和透明度控制

### 核心代码结构

```kotlin
class Day08Renderer(context: Context) : GLSurfaceView.Renderer {

    // 多纹理 ID
    private var texture1: Int = 0  // 底图
    private var texture2: Int = 0  // 叠加图

    // 混合模式
    enum class BlendMode {
        ALPHA,      // Alpha 混合
        ADD,        // 相加
        MULTIPLY,   // 相乘
        SCREEN,     // 屏幕
        OVERLAY,    // 叠加
        SOFT_LIGHT  // 柔光
    }

    private var currentBlendMode = BlendMode.ALPHA
    private var blendAlpha = 0.5f

    override fun onDrawFrame(gl: GL10?) {
        // 激活纹理单元 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture1)
        GLES20.glUniform1i(texture1Location, 0)

        // 激活纹理单元 1
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2)
        GLES20.glUniform1i(texture2Location, 1)

        // 设置混合模式
        GLES20.glUniform1i(blendModeLocation, currentBlendMode.ordinal)
        GLES20.glUniform1f(alphaLocation, blendAlpha)

        // 绘制
        drawQuad()
    }
}
```

## 🧪 练习任务

### 基础任务

1. ✅ 实现两个纹理的加载和绑定
2. ✅ 实现至少 3 种混合模式
3. ✅ 添加透明度调节功能

### 进阶任务

1. 🎨 实现水印功能（可调节位置和大小）
2. 🖼️ 实现遮罩效果
3. 🌈 实现双重曝光效果
4. 📸 添加预设混合效果（复古、电影、梦幻等）

### 挑战任务

1. 🚀 实现三个纹理的混合（底图 + 叠加图 + 水印）
2. 🎭 实现动态混合（混合比例随时间变化）
3. 🎬 实现分区域混合（不同区域不同混合模式）
4. 💾 保存混合后的图片到相册

## 📖 知识点总结

### 纹理单元 vs 纹理对象

| 特性 | 纹理单元 | 纹理对象 |
|------|---------|---------|
| **概念** | GPU 的"纹理槽位" | 实际的纹理数据 |
| **数量** | 有限（通常 8-32 个） | 可以创建很多 |
| **操作** | `glActiveTexture()` | `glBindTexture()` |
| **类比** | USB 接口 | U 盘 |

### 混合模式对比

| 模式 | 视觉效果 | 常用场景 |
|------|---------|---------|
| **Alpha** | 透明叠加 | 水印、UI 叠加 |
| **Add** | 增亮、发光 | 光效、粒子 |
| **Multiply** | 变暗、阴影 | 阴影、色彩校正 |
| **Screen** | 柔和增亮 | 柔光、氛围 |
| **Overlay** | 对比增强 | 照片滤镜 |
| **Soft Light** | 柔和混合 | 肖像美化 |

### 最佳实践

1. ✅ **复用纹理单元**：不要每帧都重新激活和绑定
2. ✅ **批量渲染**：相同纹理的对象一起渲染
3. ✅ **纹理压缩**：对辅助纹理使用较低分辨率
4. ✅ **及时释放**：不再使用的纹理要删除
5. ✅ **检查限制**：查询设备支持的最大纹理单元数

## 🐛 常见问题

### Q1: 纹理显示是黑色的？

**可能原因**：
1. 纹理单元索引错误
2. `glUniform1i` 传递的值不对应激活的纹理单元

**解决方法**：
```kotlin
// 纹理单元 0
GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture1)
GLES20.glUniform1i(texture1Location, 0)  // 传递 0，不是 GL_TEXTURE0

// 纹理单元 1
GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2)
GLES20.glUniform1i(texture2Location, 1)  // 传递 1，不是 GL_TEXTURE1
```

### Q2: 混合效果不正确？

检查是否启用了 OpenGL 混合：
```kotlin
// 如果使用硬件混合
GLES20.glEnable(GLES20.GL_BLEND)
GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

// 如果使用着色器混合
// 不需要启用 glBlend，直接在片段着色器中计算即可
```

### Q3: 水印位置不对？

OpenGL 坐标系原点在左下角，而 UI 坐标系在左上角：

```kotlin
// UI 坐标（左上角为原点）
val uiY = 100f

// 转换为 OpenGL 坐标（左下角为原点）
val glY = screenHeight - uiY - watermarkHeight
```

### Q4: 如何实现圆形水印？

在着色器中使用距离函数：

```glsl
void main() {
    vec2 center = vec2(0.5, 0.5);
    float dist = distance(watermarkCoord, center);
    
    // 圆形遮罩
    float alpha = smoothstep(0.5, 0.48, dist);
    
    vec4 watermarkColor = texture2D(uWatermark, watermarkCoord);
    watermarkColor.a *= alpha;
    
    gl_FragColor = mix(baseColor, watermarkColor, watermarkColor.a);
}
```

## 🔗 参考资料

### 官方文档
- [OpenGL ES Texture Units](https://www.khronos.org/opengl/wiki/Texture)
- [GLSL Sampler Types](https://www.khronos.org/opengl/wiki/Sampler_(GLSL))

### 混合模式参考
- [Photoshop Blend Modes](https://photoblogstop.com/photoshop/photoshop-blend-modes-explained)
- [WebGL Blend Modes](https://github.com/jamieowen/glsl-blend)

### 推荐阅读
- 《Real-Time Rendering》- Chapter 5: Visual Appearance
- [GPU Gems - Image Processing](https://developer.nvidia.com/gpugems/gpugems3/part-iv-image-effects)

## 📝 今日总结

今天我们深入学习了多重纹理和混合技术：

1. ✅ 理解了纹理单元的概念：GPU 中的"纹理槽位"
2. ✅ 掌握了多纹理的使用流程：激活 → 绑定 → 传递索引
3. ✅ 学习了 6 种常用混合模式：Alpha、Add、Multiply、Screen、Overlay、Soft Light
4. ✅ 实现了实用效果：水印、双重曝光、遮罩

**关键要点**：
- 纹理单元允许我们在一次绘制中使用多个纹理
- `glActiveTexture()` 激活纹理单元，`glUniform1i()` 传递索引（不是 GL_TEXTURE 常量）
- 混合模式本质是不同的颜色计算公式
- Alpha 通道是实现透明效果的关键

明天我们将进入**阶段三：相机集成**，学习如何将 **CameraX 与 OpenGL 结合**！📷

---

**完成打卡**：学完本节后，请在 `LEARNING_PROGRESS.md` 中勾选 Day 08 ✅

