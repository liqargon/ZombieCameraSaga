package jp.saga.liqargon.zombiecamerasaga

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun launchCamera(view: View) {
        val intent = Intent(this, CameraActivity::class.java).apply { }
        startActivity(intent)
    }
}
