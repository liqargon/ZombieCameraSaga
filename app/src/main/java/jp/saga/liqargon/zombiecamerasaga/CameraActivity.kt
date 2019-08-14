package jp.saga.liqargon.zombiecamerasaga

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CameraActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val RESULT_FRAME_SELECT = 502
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        viewFinder = findViewById(R.id.view_finder)

        // fullscreen mode
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> viewFinder.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN -> viewFinder.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            else -> viewFinder.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

        // permissions
        if (allPermissionGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, requiredPermissions, REQUEST_CODE_PERMISSIONS
            )
        }

        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onResume() {
        super.onResume()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> viewFinder.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN -> viewFinder.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            else -> viewFinder.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }

    private lateinit var viewFinder: TextureView
    private var rscId: Int? = null

    private fun startCamera() {

        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(Rational(viewFinder.width, viewFinder.height))
        }.build()

        val preview = Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener {
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

//        CameraCaptureSession.
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setTargetAspectRatio(Rational(viewFinder.width, viewFinder.height))
                setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)
        findViewById<ImageView>(R.id.capture_button).setOnClickListener {
            val file = File(
                getExternalFilesDir(Environment.DIRECTORY_DCIM),
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
                        val bmb: Bitmap = BitmapFactory.decodeFile(file.path)
                        val btm: Bitmap = Bitmap.createBitmap(bmb.width, bmb.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(btm)

                        // adjust a bitmap size of captured image in accordance with frame aspect ratio (16:9)
                        val frameHeight: Int = when {
                            bmb.width / bmb.height >= 16 / 9 -> bmb.height
                            bmb.width / bmb.height == 16 / 9 -> bmb.height
                            bmb.width / bmb.height <= 16 / 9 -> bmb.width * 9 / 16
                            else -> return
                        }
                        val frameWidth: Int = when {
                            bmb.width / bmb.height >= 16 / 9 -> bmb.height * 16 / 9
                            bmb.width / bmb.height == 16 / 9 -> bmb.width
                            bmb.width / bmb.height <= 16 / 9 -> bmb.width
                            else -> return
                        }
                        canvas.drawBitmap(bmb, 0f, 0f, null)

                        // Combine frame
                        if (rscId != null) {
                            var bmf: Bitmap = BitmapFactory.decodeResource(resources, rscId!!)
                            bmf = Bitmap.createScaledBitmap(bmf, frameWidth, frameHeight, false)
                            canvas.drawBitmap(bmf, (bmb.width - frameWidth) / 2f, (bmb.height - frameHeight) / 2f, null)
                        }

                        // Save a processed image again
                        try {
                            val out = FileOutputStream(file)
                            btm.compress(Bitmap.CompressFormat.JPEG, 100, out)
                            out.flush()
                            out.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                            throw e
                        }

                        // TODO(liqargon): Add preview feature
//                        val intent = Intent(this@CameraActivity, PreviewActivity::class.java).apply {
//                            putExtra("capture", btm)
//                            putExtra("file", file)
//                        }
//                        startActivityForResult(intent, 0)

                        // Add a captured photo to the gallery
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            put("_data", file.absolutePath)
                        }
                        contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
                        )

                        // Message
                        val msg = "Photo capture succeeded: ${file.absolutePath}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d("CameraXApp", msg)
                    }
                })
        }

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        val rotationDegrees = when (viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
        viewFinder.setTransform(matrix)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun allPermissionGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    fun returnToMain(view: View) {
        finish()
    }

    fun selectFrame(view: View) {
        val intent = Intent(this, FrameActivity::class.java).apply {

        }
        startActivityForResult(intent, RESULT_FRAME_SELECT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_FRAME_SELECT) {
            if (resultCode == Activity.RESULT_OK) {
                rscId = data?.getIntExtra("resource_id", 0)!!
                val mat = Matrix()
                mat.postRotate(90f)
                var bmp: Bitmap = BitmapFactory.decodeResource(resources, rscId!!)
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, mat, true)
                imageView.setImageBitmap(bmp)
            }
        }
    }
}
