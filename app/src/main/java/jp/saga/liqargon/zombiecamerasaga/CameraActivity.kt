package jp.saga.liqargon.zombiecamerasaga

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Rational
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class CameraActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    private val REQUESTED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val flagFlash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)


        viewFinder = findViewById(R.id.view_finder)

        // fullscreen mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            viewFinder.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            viewFinder.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        } else {
            viewFinder.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }

        if (allPermissionGranted()){
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUESTED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    private lateinit var viewFinder: TextureView

    private fun startCamera() {
        val previewConfig = PreviewConfig.Builder().apply {
//            setTargetAspectRatio(Rational(1,1))
        }.build()

        val preview = Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener {
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setTargetAspectRatio(Rational(1, 1))
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)
        findViewById<ImageButton>(R.id.capture_button).setOnClickListener {
            val file = File(
                externalMediaDirs.first(),
                "${System.currentTimeMillis()}.jpg"
            )
            imageCapture.takePicture(file,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(
                        useCaseError: ImageCapture.UseCaseError,
                        message: String,
                        cause: Throwable?
                    ) {
                        val msg = "Photo capture failed: $message"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.e("CameraXApp", msg)
                        cause?.printStackTrace()
                    }

                    override fun onImageSaved(file: File) {
                        val msg = "Photo capture succeeded: ${file.absolutePath}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d("CameraXApp", msg)
                    }
                })
        }

        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            val analyzerThread = HandlerThread("LuminosityAnalysis").apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            analyzer = LuminosityAnalyzer()
        }

        CameraX.bindToLifecycle(this, preview, imageCapture, analyzerUseCase)

    }

    private fun updateTransform() {
        val matrix = Matrix()

        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        viewFinder.setTransform(matrix)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionGranted() = REQUESTED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    fun returnToMain(view: View) {
        finish()
    }

    fun selectFrame(view: View) {
        val intent = Intent(this, FrameActivity::class.java).apply {

        }
        startActivity(intent)
    }

    fun hoge(view: View) {

    }

    private class LuminosityAnalyzer : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }

        override fun analyze(image: ImageProxy, rotationDegrees: Int) {
            val currentTimestamp = System.currentTimeMillis()

            if (currentTimestamp - lastAnalyzedTimestamp >= TimeUnit.SECONDS.toMillis(1)) {
                val buffer = image.planes[0].buffer
                val data = buffer.toByteArray()
                val pixels = data.map { it.toInt() and 0xFF }
                val luma = pixels.average()
                Log.d("CameraXApp", "Average luminosity: $luma")
                lastAnalyzedTimestamp = currentTimestamp
            }

        }
    }
}
