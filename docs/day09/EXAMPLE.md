# Day 09 示例代码讲解

## 🎯 代码结构

Day 09 实现了 CameraX 基础集成，主要包含以下文件：

```
day09/
├── Day09Activity.kt      # 相机预览和拍照逻辑
└── activity_day09.xml    # 布局文件
```

## 💡 核心功能

### 1. 权限申请

```kotlin
companion object {
    private const val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
}

private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 检查权限
    if (allPermissionsGranted()) {
        startCamera()
    } else {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }
}
```

**关键点**：
- Android 6.0+ 需要运行时权限申请
- 使用 `allPermissionsGranted()` 检查权限状态
- 权限被拒绝时应给出提示并退出

### 2. 启动相机

```kotlin
private fun startCamera() {
    // 1. 获取 ProcessCameraProvider 的 Future
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener({
        // 2. 获取 CameraProvider
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        // 3. 创建 Preview Use Case
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // 4. 创建 ImageCapture Use Case
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        // 5. 选择相机
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // 6. 解绑旧的 Use Cases
        cameraProvider.unbindAll()

        // 7. 绑定 Use Cases 到生命周期
        cameraProvider.bindToLifecycle(
            this,           // LifecycleOwner
            cameraSelector, // 相机选择器
            preview,        // 预览
            imageCapture    // 拍照
        )

    }, ContextCompat.getMainExecutor(this))
}
```

**工作流程**：
1. 获取 `ProcessCameraProvider`（异步）
2. 在回调中获取 `CameraProvider` 实例
3. 创建 `Preview` 和 `ImageCapture` Use Cases
4. 选择前置或后置相机
5. 解绑旧的 Use Cases（避免冲突）
6. 绑定新的 Use Cases 到生命周期

### 3. 拍照功能

```kotlin
private fun takePhoto() {
    val imageCapture = imageCapture ?: return

    // 1. 创建保存文件
    val photoFile = File(
        getOutputDirectory(),
        SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )

    // 2. 配置输出选项
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    // 3. 拍照
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                // 拍照失败
                Log.e(TAG, "拍照失败: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // 拍照成功
                val msg = "拍照成功！保存到：${photoFile.name}"
                Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
            }
        }
    )
}
```

**关键点**：
- 使用时间戳作为文件名，避免覆盖
- `OutputFileOptions` 指定保存路径
- `takePicture()` 是异步操作，通过回调获取结果
- 使用 `getMainExecutor()` 确保回调在主线程

### 4. 切换前后相机

```kotlin
private var lensFacing = CameraSelector.LENS_FACING_BACK

private fun switchCamera() {
    // 切换前后摄像头
    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
        CameraSelector.LENS_FACING_FRONT
    } else {
        CameraSelector.LENS_FACING_BACK
    }
    
    // 重新启动相机
    startCamera()
}
```

**原理**：
- 维护 `lensFacing` 变量记录当前相机
- 切换时更新变量值
- 调用 `startCamera()` 重新绑定（会自动解绑旧的）

## 🔍 代码流程图

```
1. onCreate
   ├── 初始化视图
   ├── 创建相机执行器
   └── 检查权限
       ├── 已授予 → startCamera()
       └── 未授予 → requestPermissions()

2. startCamera
   ├── 获取 ProcessCameraProvider (异步)
   └── 回调中
       ├── 创建 Preview Use Case
       ├── 创建 ImageCapture Use Case
       ├── 选择相机（前置/后置）
       ├── 解绑旧的 Use Cases
       └── 绑定新的 Use Cases

3. takePhoto
   ├── 检查 ImageCapture 是否初始化
   ├── 创建保存文件
   ├── 配置输出选项
   └── takePicture (异步)
       ├── onError → 显示错误
       └── onImageSaved → 显示成功消息

4. switchCamera
   ├── 切换 lensFacing 值
   └── 重新调用 startCamera()
```

## 🚀 运行效果

运行应用后，你会看到：

1. **权限请求**：首次运行时请求相机权限
2. **实时预览**：全屏显示相机预览画面
3. **控制面板**（底部）：
   - 标题文字："CameraX 基础集成 - 预览与拍照"
   - 📷 拍照按钮（绿色）
   - 🔄 切换按钮（蓝色）

**操作步骤**：
1. 授予相机权限
2. 查看实时预览画面
3. 点击"拍照"按钮拍照（Toast 提示保存路径）
4. 点击"切换"按钮切换前后摄像头

## 💡 学习要点

### CameraX 三大核心

| 概念 | 说明 | 代码 |
|------|------|------|
| **ProcessCameraProvider** | 相机提供者 | `ProcessCameraProvider.getInstance()` |
| **Use Cases** | 使用场景 | `Preview`, `ImageCapture` |
| **CameraSelector** | 相机选择器 | `LENS_FACING_BACK`, `LENS_FACING_FRONT` |

### Use Cases 详解

```kotlin
// Preview - 预览
val preview = Preview.Builder().build()
preview.setSurfaceProvider(previewView.surfaceProvider)

// ImageCapture - 拍照
val imageCapture = ImageCapture.Builder()
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    .build()
```

### 生命周期绑定

```kotlin
cameraProvider.bindToLifecycle(
    this,           // LifecycleOwner (Activity/Fragment)
    cameraSelector, // 选择前置/后置相机
    preview,        // Use Case 1
    imageCapture    // Use Case 2
)
```

**优势**：
- 自动管理相机的打开和关闭
- Activity 暂停时自动暂停相机
- Activity 销毁时自动释放相机
- 无需手动调用 `camera.close()`

## 📁 文件存储

### 存储路径选择

```kotlin
private fun getOutputDirectory(): File {
    // 1. 优先使用外部媒体目录（用户可见）
    val mediaDir = externalMediaDirs.firstOrNull()?.let {
        File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
    }
    
    // 2. 如果外部目录不可用，使用内部文件目录
    return if (mediaDir != null && mediaDir.exists()) {
        mediaDir
    } else {
        filesDir
    }
}
```

**路径说明**：
- **externalMediaDirs**：`/sdcard/Android/media/com.example.openglstudy/`
  - 优点：用户可在相册中查看
  - 缺点：需要存储权限（Android 10+）
  
- **filesDir**：`/data/data/com.example.openglstudy/files/`
  - 优点：无需权限
  - 缺点：用户不可见，卸载时删除

### 文件命名

```kotlin
companion object {
    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
}

val photoFile = File(
    getOutputDirectory(),
    SimpleDateFormat(FILENAME_FORMAT, Locale.US)
        .format(System.currentTimeMillis()) + ".jpg"
)
```

**示例文件名**：`2025-11-02-18-30-45-123.jpg`

## 🎯 扩展练习

### 基础练习

1. **修改保存路径**：将照片保存到相册而不是应用目录
2. **添加拍照计数**：显示"已拍摄 X 张照片"
3. **添加拍照音效**：使用 MediaPlayer 播放快门声

### 进阶练习

1. **缩略图预览**：拍照后在屏幕角落显示缩略图
2. **闪光灯控制**：添加闪光灯开关按钮
3. **捏合缩放**：实现双指缩放功能

```kotlin
// 获取 Camera 对象
val camera = cameraProvider.bindToLifecycle(...)

// 启用捏合缩放
val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: 0f
        val delta = detector.scaleFactor
        camera.cameraControl.setZoomRatio(currentZoomRatio * delta)
        return true
    }
}
val scaleGestureDetector = ScaleGestureDetector(this, listener)
previewView.setOnTouchListener { _, event ->
    scaleGestureDetector.onTouchEvent(event)
    return@setOnTouchListener true
}
```

### 挑战练习

1. **录像功能**：使用 `VideoCapture` 实现录像
2. **图像分析**：使用 `ImageAnalysis` 实现实时人脸检测
3. **自定义分辨率**：让用户选择拍照分辨率（720p, 1080p, 4K）
4. **HDR 模式**：启用 HDR 拍照模式

## 📚 参考资料

- [CameraX 官方文档](https://developer.android.com/training/camerax)
- [CameraX Codelab](https://codelabs.developers.google.com/codelabs/camerax-getting-started)
- 本项目 `docs/day09/README.md` 详细教程

## 🆚 CameraX vs Camera2

| 特性 | Camera2 | CameraX |
|------|---------|---------|
| **代码量** | ~500 行 | ~50 行 |
| **生命周期** | 手动管理 | 自动管理 |
| **兼容性** | 需处理设备差异 | 自动处理 |
| **学习曲线** | 陡峭 | 平缓 |
| **推荐度** | ❌ 不推荐 | ✅ 强烈推荐 |

**结论**：除非有特殊需求，否则应该使用 CameraX！

---

**祝学习愉快！** 🎉

