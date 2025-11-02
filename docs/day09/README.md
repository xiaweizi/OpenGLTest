# Day 09: CameraX 基础集成

## 📚 今日目标

- 理解 CameraX 架构和核心概念
- 掌握相机权限的申请和处理
- 学习 PreviewView 的使用
- 实现相机预览功能
- 了解生命周期绑定机制

## 🎯 学习内容

### 1. CameraX 简介

**CameraX** 是 Google 推出的 Jetpack 库，用于简化 Android 相机开发。

#### 为什么使用 CameraX？

| 传统 Camera2 API | CameraX |
|-----------------|---------|
| 代码复杂，API 繁琐 | 简单易用，代码量少 |
| 需要处理设备差异 | 自动处理兼容性问题 |
| 生命周期管理困难 | 自动绑定生命周期 |
| 预览、拍照、录像需要大量代码 | 几行代码即可实现 |

#### CameraX 的优势

- ✅ **简单易用**：几行代码实现预览和拍照
- ✅ **兼容性好**：自动处理不同设备的差异
- ✅ **生命周期感知**：自动管理相机的打开和关闭
- ✅ **向后兼容**：支持 Android 5.0+（API 21）
- ✅ **扩展支持**：美颜、夜景、HDR 等厂商扩展

### 2. CameraX 架构

#### 核心组件

```
CameraX 架构
├── Use Cases (用例)
│   ├── Preview (预览)
│   ├── ImageCapture (拍照)
│   ├── ImageAnalysis (图像分析)
│   └── VideoCapture (录像)
├── CameraSelector (相机选择器)
│   ├── LENS_FACING_BACK (后置)
│   └── LENS_FACING_FRONT (前置)
├── ProcessCameraProvider (相机提供者)
│   └── 管理相机的生命周期
└── PreviewView (预览视图)
    └── 显示相机预览画面
```

#### 工作流程

```
1. 获取 ProcessCameraProvider
   ↓
2. 创建 Use Cases (Preview, ImageCapture 等)
   ↓
3. 选择相机 (CameraSelector)
   ↓
4. 绑定到生命周期 (bindToLifecycle)
   ↓
5. 显示预览 / 拍照 / 分析
```

### 3. 相机权限处理

#### 3.1 在 AndroidManifest.xml 中声明权限

```xml
<!-- 相机权限（必需） -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- 存储权限（拍照保存需要） -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />

<!-- 相机特性 -->
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

#### 3.2 运行时权限申请（Android 6.0+）

```kotlin
class Day09Activity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
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

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
```

### 4. 实现相机预览

#### 4.1 布局文件

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- 相机预览视图 -->
    <androidx.camera.view.PreviewView
        android:id="@+id/preview_view"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toTopOf="@id/control_panel"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- 控制面板 -->
    <LinearLayout
        android:id="@+id/control_panel"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:background="#80000000"
        android:orientation="horizontal"
        android:padding="16dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent">

        <!-- 拍照按钮 -->
        <Button
            android:id="@+id/btn_capture"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="拍照" />

        <!-- 切换相机按钮 -->
        <Button
            android:id="@+id/btn_switch_camera"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:layout_weight="1"
            android:text="切换相机" />

    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

#### 4.2 启动相机

```kotlin
private fun startCamera() {
    // 获取 ProcessCameraProvider 的 Future
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener({
        // 获取 CameraProvider
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        // 创建 Preview Use Case
        val preview = Preview.Builder()
            .build()
            .also {
                // 设置 Surface Provider
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // 选择后置相机
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // 解绑之前的 Use Cases
            cameraProvider.unbindAll()

            // 绑定 Use Cases 到生命周期
            cameraProvider.bindToLifecycle(
                this,      // LifecycleOwner
                cameraSelector,
                preview
            )

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }

    }, ContextCompat.getMainExecutor(this))
}
```

### 5. CameraSelector 详解

#### 5.1 选择前置/后置相机

```kotlin
// 后置相机
val backCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

// 前置相机
val frontCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

// 自定义选择器
val customSelector = CameraSelector.Builder()
    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
    .build()
```

#### 5.2 切换相机

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

### 6. 拍照功能

#### 6.1 创建 ImageCapture Use Case

```kotlin
// 创建 ImageCapture
imageCapture = ImageCapture.Builder()
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    .build()

// 绑定时同时绑定 Preview 和 ImageCapture
cameraProvider.bindToLifecycle(
    this,
    cameraSelector,
    preview,
    imageCapture  // 添加拍照功能
)
```

#### 6.2 拍照并保存

```kotlin
private fun takePhoto() {
    val imageCapture = imageCapture ?: return

    // 创建保存文件
    val photoFile = File(
        getOutputDirectory(),
        SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )

    // 配置输出选项
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    // 拍照
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "拍照失败: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val msg = "拍照成功: ${photoFile.absolutePath}"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, msg)
            }
        }
    )
}

private fun getOutputDirectory(): File {
    val mediaDir = externalMediaDirs.firstOrNull()?.let {
        File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists())
        mediaDir else filesDir
}
```

### 7. PreviewView 配置

#### 7.1 缩放模式

```kotlin
// 填充整个视图（可能裁剪）
previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

// 适应视图大小（可能有黑边）
previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
```

#### 7.2 实现模式

```kotlin
// 使用 SurfaceView（性能更好）
previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE

// 使用 TextureView（兼容性更好）
previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
```

### 8. 生命周期管理

CameraX 会自动管理相机的生命周期：

```kotlin
// CameraX 自动处理
override fun onResume() {
    super.onResume()
    // 相机自动恢复（如果已绑定）
}

override fun onPause() {
    super.onPause()
    // 相机自动暂停
}

override fun onDestroy() {
    super.onDestroy()
    // 相机自动释放
}
```

**注意**：使用 `bindToLifecycle()` 时，CameraX 会自动处理相机的打开、关闭和暂停。

### 9. 相机配置选项

#### 9.1 分辨率配置

```kotlin
val preview = Preview.Builder()
    .setTargetResolution(Size(1280, 720))  // 设置目标分辨率
    .build()
```

#### 9.2 宽高比配置

```kotlin
val preview = Preview.Builder()
    .setTargetAspectRatio(AspectRatio.RATIO_16_9)  // 16:9
    .build()
```

#### 9.3 旋转配置

```kotlin
val preview = Preview.Builder()
    .setTargetRotation(Surface.ROTATION_0)  // 设置目标旋转角度
    .build()
```

### 10. 错误处理

#### 10.1 常见错误

```kotlin
try {
    cameraProvider.bindToLifecycle(this, cameraSelector, preview)
} catch (exc: Exception) {
    when (exc) {
        is CameraInfoUnavailableException -> {
            Log.e(TAG, "相机信息不可用")
        }
        is IllegalArgumentException -> {
            Log.e(TAG, "Use Case 绑定参数错误")
        }
        else -> {
            Log.e(TAG, "Use Case 绑定失败", exc)
        }
    }
}
```

#### 10.2 检查相机可用性

```kotlin
private fun hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

private fun hasCamera(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
}
```

## 💻 代码实践

### 今日任务

实现一个简单的相机预览应用：

1. **权限申请**：请求相机权限
2. **相机预览**：使用 PreviewView 显示实时画面
3. **拍照功能**：点击按钮拍照并保存
4. **切换相机**：前置/后置相机切换

### 实现效果

- 📷 实时相机预览
- 📸 拍照并保存到相册
- 🔄 前后摄像头切换
- ⚙️ 自动处理生命周期

### 核心代码结构

```kotlin
class Day09Activity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day09)

        previewView = findViewById(R.id.preview_view)

        // 检查权限
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // 拍照按钮
        findViewById<Button>(R.id.btn_capture).setOnClickListener {
            takePhoto()
        }

        // 切换相机按钮
        findViewById<Button>(R.id.btn_switch_camera).setOnClickListener {
            switchCamera()
        }
    }

    private fun startCamera() {
        // 实现相机启动逻辑
    }

    private fun takePhoto() {
        // 实现拍照逻辑
    }

    private fun switchCamera() {
        // 实现切换相机逻辑
    }
}
```

## 🧪 练习任务

### 基础任务

1. ✅ 实现相机权限申请
2. ✅ 实现相机预览功能
3. ✅ 实现拍照并保存到相册

### 进阶任务

1. 📊 显示拍照成功的缩略图
2. 🎨 添加拍照动画效果（闪光）
3. 📏 支持捏合缩放（Pinch to Zoom）
4. 🔦 添加闪光灯控制

### 挑战任务

1. 🎬 实现录像功能（VideoCapture）
2. 📐 添加网格线辅助构图
3. 💾 实现连拍功能
4. 🖼️ 支持不同宽高比切换（4:3, 16:9, 1:1）

## 📖 知识点总结

### CameraX 核心概念

| 概念 | 说明 |
|------|------|
| **Use Case** | 相机的使用场景（预览、拍照、分析、录像） |
| **CameraSelector** | 选择使用哪个相机（前置/后置） |
| **ProcessCameraProvider** | 管理相机生命周期的提供者 |
| **PreviewView** | 显示相机预览的视图 |
| **LifecycleOwner** | 生命周期拥有者（通常是 Activity） |

### Use Cases 对比

| Use Case | 用途 | 典型场景 |
|----------|------|---------|
| **Preview** | 实时预览 | 相机取景 |
| **ImageCapture** | 拍照 | 拍摄照片 |
| **ImageAnalysis** | 图像分析 | 二维码扫描、人脸识别 |
| **VideoCapture** | 录像 | 录制视频 |

### 最佳实践

1. ✅ **使用 bindToLifecycle()**：自动管理相机生命周期
2. ✅ **在主线程使用 CameraX API**：避免线程问题
3. ✅ **复用 CameraProvider**：不要频繁获取
4. ✅ **处理配置变更**：屏幕旋转时正确更新
5. ✅ **及时解绑 Use Cases**：切换相机前调用 `unbindAll()`

## 🐛 常见问题

### Q1: 相机预览画面是黑色的？

**可能原因**：
1. 权限未授予
2. PreviewView 未正确设置 SurfaceProvider
3. Use Case 未绑定到生命周期

**解决方法**：
```kotlin
// 1. 确认权限已授予
if (!allPermissionsGranted()) {
    requestPermissions()
    return
}

// 2. 正确设置 SurfaceProvider
preview.setSurfaceProvider(previewView.surfaceProvider)

// 3. 确保绑定到生命周期
cameraProvider.bindToLifecycle(this, cameraSelector, preview)
```

### Q2: 应用崩溃，提示 "Camera is being used"？

**原因**：同一个相机被多次绑定。

**解决方法**：
```kotlin
// 绑定前先解绑所有 Use Cases
cameraProvider.unbindAll()
cameraProvider.bindToLifecycle(this, cameraSelector, preview)
```

### Q3: 切换前后相机时闪烁？

**原因**：切换时没有正确处理。

**解决方法**：
```kotlin
private fun switchCamera() {
    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
        CameraSelector.LENS_FACING_FRONT
    } else {
        CameraSelector.LENS_FACING_BACK
    }
    
    // 重新启动相机（会自动解绑旧的）
    startCamera()
}
```

### Q4: 拍照的图片方向不对？

CameraX 会自动处理图片旋转，但如果遇到问题：

```kotlin
val imageCapture = ImageCapture.Builder()
    .setTargetRotation(previewView.display.rotation)  // 设置旋转角度
    .build()
```

## 🔗 参考资料

### 官方文档
- [CameraX 官方文档](https://developer.android.com/training/camerax)
- [CameraX 架构](https://developer.android.com/training/camerax/architecture)
- [CameraX 最佳实践](https://developer.android.com/training/camerax/best-practices)

### 示例代码
- [CameraX 官方示例](https://github.com/android/camera-samples)
- [Google Codelabs - CameraX](https://codelabs.developers.google.com/codelabs/camerax-getting-started)

### 相关资源
- [Android 相机权限处理](https://developer.android.com/training/permissions/requesting)
- [PreviewView API 文档](https://developer.android.com/reference/androidx/camera/view/PreviewView)

## 📝 今日总结

今天我们学习了 CameraX 的基础知识：

1. ✅ 理解了 CameraX 的架构和核心概念
2. ✅ 掌握了相机权限的申请和处理
3. ✅ 学会了使用 PreviewView 显示相机预览
4. ✅ 实现了拍照和保存功能
5. ✅ 了解了生命周期管理机制

**关键要点**：
- CameraX 简化了相机开发，自动处理兼容性问题
- Use Cases 是 CameraX 的核心，代表不同的使用场景
- `bindToLifecycle()` 会自动管理相机的生命周期
- PreviewView 提供了简单易用的预览功能

明天我们将学习 **相机预览与 OpenGL 结合**，使用 SurfaceTexture 将相机数据传递给 OpenGL 渲染！🎨

---

**完成打卡**：学完本节后，请在 `LEARNING_PROGRESS.md` 中勾选 Day 09 ✅

