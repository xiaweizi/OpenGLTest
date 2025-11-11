package com.example.openglstudy.day09

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.openglstudy.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Day 09 Activity
 * 学习目标：CameraX 基础集成
 * 实现功能：相机预览、拍照、前后摄像头切换
 */
class Day09Activity : AppCompatActivity() {

    companion object {
        private const val TAG = "Day09Activity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            // Android 10 及以下需要存储权限
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    // UI 组件
    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var btnSwitchCamera: Button

    // CameraX 相关
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: 开始创建 Activity")
        setContentView(R.layout.activity_day09)

        // 设置标题
        supportActionBar?.title = "Day 09: CameraX 基础集成"

        // 初始化视图
        initViews()

        // 创建相机执行器
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 检查权限
        if (allPermissionsGranted()) {
            Log.d(TAG, "onCreate: 权限已授予，启动相机")
            startCamera()
        } else {
            Log.d(TAG, "onCreate: 权限未授予，请求权限")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        Log.d(TAG, "onCreate: Activity 创建完成")
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        previewView = findViewById(R.id.preview_view)
        btnCapture = findViewById(R.id.btn_capture)
        btnSwitchCamera = findViewById(R.id.btn_switch_camera)

        // 拍照按钮
        btnCapture.setOnClickListener {
            takePhoto()
        }

        // 切换相机按钮
        btnSwitchCamera.setOnClickListener {
            switchCamera()
        }

        Log.d(TAG, "initViews: 视图初始化完成")
    }

    /**
     * 启动相机
     */
    private fun startCamera() {
        Log.d(TAG, "startCamera: 开始启动相机")
        
        // 获取 ProcessCameraProvider 的 Future
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                // 获取 CameraProvider
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "startCamera: CameraProvider 获取成功")

                // 创建 Preview Use Case
                val preview = Preview.Builder()
                    .build()
                    .also {
                        // 设置 Surface Provider
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                Log.d(TAG, "startCamera: Preview Use Case 创建完成")

                // 创建 ImageCapture Use Case
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                Log.d(TAG, "startCamera: ImageCapture Use Case 创建完成")

                // 选择相机
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                val cameraType = if (lensFacing == CameraSelector.LENS_FACING_BACK) "后置" else "前置"
                Log.d(TAG, "startCamera: 选择${cameraType}相机")

                // 解绑之前的 Use Cases
                cameraProvider.unbindAll()
                Log.d(TAG, "startCamera: 已解绑旧的 Use Cases")

                // 绑定 Use Cases 到生命周期
                cameraProvider.bindToLifecycle(
                    this,           // LifecycleOwner
                    cameraSelector, // 相机选择器
                    preview,        // 预览
                    imageCapture    // 拍照
                )
                Log.d(TAG, "startCamera: Use Cases 绑定成功")

                Toast.makeText(this, "相机启动成功", Toast.LENGTH_SHORT).show()

            } catch (exc: Exception) {
                Log.e(TAG, "startCamera: Use case 绑定失败", exc)
                Toast.makeText(this, "相机启动失败: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 拍照
     */
    private fun takePhoto() {
        Log.d(TAG, "takePhoto: 开始拍照")

        // 确保 ImageCapture 已初始化
        val imageCapture = imageCapture ?: run {
            Log.e(TAG, "takePhoto: ImageCapture 未初始化")
            Toast.makeText(this, "相机未就绪", Toast.LENGTH_SHORT).show()
            return
        }

        // 创建保存文件
        val photoFile = File(
            getOutputDirectory(),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )
        Log.d(TAG, "takePhoto: 保存路径 ${photoFile.absolutePath}")

        // 配置输出选项
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // 拍照
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "takePhoto: 拍照失败 ${exc.message}", exc)
                    Toast.makeText(
                        baseContext,
                        "拍照失败: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "拍照成功！保存到：${photoFile.path}"
                    Log.d(TAG, "takePhoto: $msg")
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    
                    // 通知系统扫描新文件，让照片立即在相册中显示
                    android.media.MediaScannerConnection.scanFile(
                        this@Day09Activity,
                        arrayOf(photoFile.absolutePath),
                        arrayOf("image/jpeg"),
                        null
                    )
                }
            }
        )
    }

    /**
     * 切换前后摄像头
     */
    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            Log.d(TAG, "switchCamera: 切换到前置相机")
            CameraSelector.LENS_FACING_FRONT
        } else {
            Log.d(TAG, "switchCamera: 切换到后置相机")
            CameraSelector.LENS_FACING_BACK
        }

        // 重新启动相机
        startCamera()
    }

    /**
     * 获取输出目录
     * 保存到系统相册目录：/sdcard/DCIM/Camera/
     */
    private fun getOutputDirectory(): File {
        // 使用系统相册目录（用户可在相册中直接查看）
        val dcimDir = File(
            android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DCIM
            ),
            "Camera"
        )
        
        // 确保目录存在
        if (!dcimDir.exists()) {
            dcimDir.mkdirs()
        }
        
        return dcimDir
    }

    /**
     * 检查所有权限是否已授予
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 权限请求结果回调
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "onRequestPermissionsResult: 权限授予成功")
                startCamera()
            } else {
                Log.e(TAG, "onRequestPermissionsResult: 权限被拒绝")
                Toast.makeText(
                    this,
                    "相机权限被拒绝，应用无法使用相机功能",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: 关闭相机执行器")
        cameraExecutor.shutdown()
    }
}

