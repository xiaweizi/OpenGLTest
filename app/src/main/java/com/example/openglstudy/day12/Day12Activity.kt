package com.example.openglstudy.day12

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.openglstudy.R
import java.util.concurrent.Executors

/**
 * Day 12 Activity
 * 学习目标：LUT（Look-Up Table）滤镜
 * 实现功能：使用 3D LUT 实现专业级调色滤镜
 */
class Day12Activity : AppCompatActivity() {

    companion object {
        private const val TAG = "Day11_1Activity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: Day12Renderer
    
    private lateinit var tvFilterName: TextView
    private lateinit var tvIntensity: TextView
    private lateinit var tvFilterCount: TextView
    private lateinit var seekBarIntensity: SeekBar
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var categoryContainer: LinearLayout

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    
    private var currentCategory = LUTFilter.Category.ALL
    private var filteredList = listOf<LUTFilter>()
    private var currentIndexInCategory = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: 开始创建 Activity")
        setContentView(R.layout.activity_day11_1)

        // 设置标题
        supportActionBar?.title = "Day 12: LUT 专业滤镜"

        // 初始化 GLSurfaceView
        glSurfaceView = findViewById(R.id.gl_surface_view)
        glSurfaceView.setEGLContextClientVersion(2)
        Log.d(TAG, "onCreate: OpenGL ES 版本设置为 2.0")

        // 创建 Renderer
        renderer = Day12Renderer(this, glSurfaceView)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        Log.d(TAG, "onCreate: Renderer 设置完成")

        // 初始化视图
        initViews()
        setupListeners()

        // LUT 加载完成回调
        renderer.onLUTsLoadedCallback = {
            runOnUiThread {
                updateFilteredList()
                updateFilterInfo()
            }
        }

        // 检查权限
        if (allPermissionsGranted()) {
            Log.d(TAG, "onCreate: 权限已授予")
            cameraExecutor.execute {
                try {
                    val surfaceTexture = renderer.getSurfaceTexture()
                    runOnUiThread {
                        startCamera(surfaceTexture)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onCreate: 获取 SurfaceTexture 失败", e)
                }
            }
        } else {
            Log.d(TAG, "onCreate: 请求权限")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        Log.d(TAG, "onCreate: Activity 创建完成")
    }

    private fun initViews() {
        tvFilterName = findViewById(R.id.tv_filter_name)
        tvIntensity = findViewById(R.id.tv_intensity)
        tvFilterCount = findViewById(R.id.tv_filter_count)
        seekBarIntensity = findViewById(R.id.seekbar_intensity)
        btnPrevious = findViewById(R.id.btn_previous)
        btnNext = findViewById(R.id.btn_next)
        categoryContainer = findViewById(R.id.category_container)

        // 设置初始值
        seekBarIntensity.progress = 100
        tvIntensity.text = "强度: 100%"
        
        // 创建分类按钮
        createCategoryButtons()
    }

    private fun createCategoryButtons() {
        val categories = LUTFilter.Category.values()
        
        categories.forEach { category ->
            val button = Button(this).apply {
                text = category.displayName
                setOnClickListener {
                    switchCategory(category)
                }
            }
            categoryContainer.addView(button)
        }
    }

    private fun setupListeners() {
        // 强度调节
        seekBarIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val intensity = progress / 100.0f
                tvIntensity.text = "强度: $progress%"
                
                glSurfaceView.queueEvent {
                    renderer.setIntensity(intensity)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 上一个滤镜
        btnPrevious.setOnClickListener {
            previousFilter()
        }

        // 下一个滤镜
        btnNext.setOnClickListener {
            nextFilter()
        }
    }

    private fun switchCategory(category: LUTFilter.Category) {
        currentCategory = category
        currentIndexInCategory = 0
        updateFilteredList()
        applyCurrentFilter()
        updateFilterInfo()
    }

    private fun updateFilteredList() {
        filteredList = renderer.getFiltersByCategory(currentCategory)
    }

    private fun previousFilter() {
        if (filteredList.isEmpty()) return
        
        currentIndexInCategory = if (currentIndexInCategory == 0) {
            filteredList.size - 1
        } else {
            currentIndexInCategory - 1
        }
        
        applyCurrentFilter()
        updateFilterInfo()
    }

    private fun nextFilter() {
        if (filteredList.isEmpty()) return
        
        currentIndexInCategory = (currentIndexInCategory + 1) % filteredList.size
        
        applyCurrentFilter()
        updateFilterInfo()
    }

    private fun applyCurrentFilter() {
        if (filteredList.isEmpty()) return
        
        val filter = filteredList[currentIndexInCategory]
        val allFilters = renderer.getAllFilters()
        val globalIndex = allFilters.indexOf(filter)
        
        glSurfaceView.queueEvent {
            renderer.setFilter(globalIndex)
        }
    }

    private fun updateFilterInfo() {
        if (filteredList.isEmpty()) {
            tvFilterName.text = "当前滤镜: 无"
            tvFilterCount.text = "0 / 0"
            return
        }
        
        val filter = filteredList[currentIndexInCategory]
        tvFilterName.text = "当前滤镜: ${filter.name}"
        tvFilterCount.text = "${currentIndexInCategory + 1} / ${filteredList.size}"
    }

    private fun startCamera(surfaceTexture: SurfaceTexture) {
        Log.d(TAG, "startCamera: 开始启动相机")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "startCamera: CameraProvider 获取成功")

                val preview = Preview.Builder().build()

                preview.setSurfaceProvider { request: SurfaceRequest ->
                    val resolution = request.resolution
                    Log.d(TAG, "startCamera: 相机分辨率 ${resolution.width}x${resolution.height}")

                    surfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)

                    val surface = Surface(surfaceTexture)

                    request.provideSurface(
                        surface,
                        ContextCompat.getMainExecutor(this)
                    ) {
                        surface.release()
                    }
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
                )

                Log.d(TAG, "startCamera: 相机启动成功")
                Toast.makeText(this, "相机启动成功", Toast.LENGTH_SHORT).show()

            } catch (exc: Exception) {
                Log.e(TAG, "startCamera: 相机启动失败", exc)
                Toast.makeText(this, "相机启动失败: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "onRequestPermissionsResult: 权限授予成功")
                cameraExecutor.execute {
                    try {
                        val surfaceTexture = renderer.getSurfaceTexture()
                        runOnUiThread {
                            startCamera(surfaceTexture)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "onRequestPermissionsResult: 获取 SurfaceTexture 失败", e)
                    }
                }
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

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: GLSurfaceView 恢复渲染")
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: GLSurfaceView 暂停渲染")
        glSurfaceView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: 释放资源")
        cameraExecutor.shutdown()
        renderer.release()
    }
}

