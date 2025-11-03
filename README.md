# OpenGL ES 学习项目
![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen)
![OpenGL ES](https://img.shields.io/badge/OpenGL%20ES-2.0-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple)
![License](https://img.shields.io/badge/License-MIT-yellow)

**一个面向 Android 开发者的 OpenGL ES 渐进式学习教程**

---

## 📖 项目简介

这是一个系统性的 OpenGL ES 学习项目，旨在帮助 Android 开发者从零开始掌握 OpenGL ES，最终能够独立开发相机滤镜和美颜应用。

### ✨ 项目特点

- 📅 **12 天学习计划**：从入门到进阶，循序渐进
- 📚 **详细教程文档**：每天配备完整的理论讲解和代码注释
- 💻 **可运行代码**：每个知识点都有对应的可运行示例
- 🎯 **实战导向**：最终目标是实现相机滤镜
- 📝 **进度追踪**：打卡系统帮助保持学习动力

### 🎓 学习路线

```
阶段一：OpenGL 基础（Day 1-4）
  → OpenGL ES 入门
  → 渲染三角形
  → GLSL 着色器
  → 纹理贴图

阶段二：OpenGL 进阶（Day 5-8）
  → 矩阵变换
  → EGL 深入
  → FBO 离屏渲染
  → 多重纹理与混合

阶段三：相机集成（Day 9-12）
  → CameraX 集成
  → OpenGL 结合相机
  → 实时滤镜效果
  → LUT 专业滤镜（19种）
```

## 🚀 快速开始

### 环境要求

- **Android Studio**: Arctic Fox 或更高版本
- **JDK**: 17
- **Gradle**: 8.0
- **最小 Android 版本**: 8.0 (API 26)

### 克隆项目

```bash
git clone https://github.com/xiaweizi/OpenGLTest.git
cd OpenGLTest
```

### 运行项目

1. 用 Android Studio 打开项目
2. 等待 Gradle 同步完成
3. 连接 Android 设备或启动模拟器
4. 点击运行按钮（或按 Shift+F10）

## 📂 项目结构

```
OpenGLTest/
├── docs/                    # 学习文档
│   ├── day01/              # Day 1: OpenGL ES 入门
...
├── app/src/main/
│   ├── java/com/example/openglstudy/
│   │   ├── MainActivity.kt         # 主页
│   │   ├── day01-day012/           # 各天的代码实现
│   │   ├── utils/                 # 工具类
│   │   └── base/                  # 基类
│   └── res/
│       ├── layout/                # 布局文件
│       └── drawable/              # 图片资源
├── LEARNING_PLAN.md        # 12 天学习计划
├── LEARNING_PROGRESS.md    # 打卡进度
```

## 📚 学习指南

### 如何使用本项目？

1. **阅读学习计划**：查看 [LEARNING_PLAN.md](LEARNING_PLAN.md) 了解整体规划
2. **按天学习**：从 Day 1 开始，每天学习一个主题
3. **看文档**：阅读 `docs/dayXX/README.md` 理解理论知识
4. **跑代码**：运行对应的 Activity，观察效果
5. **做实验**：修改参数、尝试不同效果
6. **打卡记录**：在 [LEARNING_PROGRESS.md](LEARNING_PROGRESS.md) 记录进度

### 学习时间分配

- **理论学习**：30-45 分钟（阅读文档）
- **代码实践**：60-90 分钟（运行、修改代码）
- **总结记录**：15-30 分钟（写笔记、打卡）

## 🎨 功能展示

| Day  | 功能               | 效果                                |
| ---- | ------------------ | ----------------------------------- |
| 01   | OpenGL ES 环境搭建 | 清屏显示纯色背景                    |
| 02   | 渲染三角形         | 彩色渐变三角形、正方形              |
| 03   | GLSL 着色器        | 动态改变颜色、大小、混合            |
| 04   | 纹理贴图           | 加载并显示图片纹理                  |
| 05   | 矩阵变换           | 图片旋转、缩放、平移                |
| 06   | EGL 深入           | 理解 EGL 上下文和渲染模式           |
| 07   | FBO 离屏渲染       | 灰度、反色、模糊、边缘检测滤镜      |
| 08   | 多重纹理混合       | 6 种混合模式（Alpha、相加、相乘等） |
| 09   | CameraX 基础集成   | 相机预览、拍照、保存到相册          |
| 10   | 相机与 OpenGL 结合 | SurfaceTexture + OES 纹理实时预览   |
| 11   | 实时滤镜效果       | 7 种滤镜（灰度、复古、暖色调等）    |
| 12   | LUT 专业滤镜       | 19 种专业调色滤镜 + 强度控制        |

### 🎬 核心功能预览

**🎨 实时滤镜（Day 11）**：

- ✅ 灰度滤镜（经典黑白）
- ✅ 复古滤镜（怀旧色调）
- ✅ 暖色滤镜（温暖氛围）
- ✅ 冷色滤镜（清冷风格）
- ✅ 反色滤镜（颠倒色彩）
- ✅ 黑白滤镜（高对比度）
- ✅ 褐色滤镜（复古棕色）

**🎭 LUT 专业滤镜（Day 12）**：

- ✅ 19 种专业调色滤镜
- ✅ 按类别分类（复古、电影、自然等）
- ✅ 实时强度调节（0-100%）
- ✅ 一键切换滤镜

## 🛠️ 技术栈

- **开发语言**：Kotlin
- **图形 API**：OpenGL ES 2.0
- **着色语言**：GLSL ES 1.0
- **相机框架**：CameraX
- **图片加载**：Glide
- **UI 框架**：Android View
- **构建工具**：Gradle + Kotlin DSL

## 📖 核心知识点

### OpenGL ES 基础 

- ✅ 渲染管线流程
- ✅ 着色器（Vertex Shader、Fragment Shader）
- ✅ GLSL 语言（uniform、attribute、varying）
- ✅ 顶点缓冲对象（VBO）
- ✅ 纹理映射与坐标系统

### OpenGL ES 进阶 

- ✅ 矩阵变换（平移、旋转、缩放）
- ✅ EGL 上下文管理
- ✅ FBO 离屏渲染
- ✅ 多重纹理单元
- ✅ 纹理混合模式

### 相机与实时渲染 

- ✅ CameraX 相机集成
- ✅ SurfaceTexture 与 OES 纹理
- ✅ 实时滤镜（色彩调整、风格化）
- ✅ LUT 专业调色滤镜
- ✅ 多 Pass 渲染流程

## 📝 学习资源

### 官方文档

- [Android OpenGL ES 指南](https://developer.android.com/develop/ui/views/graphics/opengl)
- [Khronos OpenGL ES 规范](https://www.khronos.org/opengles/)
- [GLSL ES 参考手册](https://www.khronos.org/files/opengles_shading_language.pdf)

### 推荐阅读

- [LearnOpenGL CN](https://learnopengl-cn.github.io/) - 优秀的 OpenGL 教程
- [GPUImage](https://github.com/cats-oss/android-gpuimage) - 开源滤镜库
- 《OpenGL ES 3.0 编程指南》

## 📄 许可证

本项目采用 MIT 许可证，详情请参阅 [LICENSE](LICENSE) 文件。
