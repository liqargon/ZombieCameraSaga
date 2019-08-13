package jp.saga.liqargon.zombiecamerasaga

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import android.view.View.generateViewId
import androidx.core.view.setMargins
import androidx.core.view.setPadding


class FrameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_frame)

        val cLayout: ConstraintLayout = findViewById(R.id.frame_view)
        val attr: AttributeSet? = null

        // TODO(liqargon): 画像自動取得
        val resourceIdes:Array<Int> =
            arrayOf(
                R.drawable.sakura_1,
                R.drawable.sakura_2,
                R.drawable.sakura_3,
                R.drawable.sakura_4,
                R.drawable.ai_1,
                R.drawable.tatsumi_1,
                R.drawable.tatsumi_2
                )
        val ides = (0..11).map { generateViewId() }
//        val hoge = resources.obtainTypedArray(R.drawable)

        for (i in 0..11) {
            // TODO(liqargon): 2で割れない数に対応, 縦画像対応
            val params: ConstraintLayout.LayoutParams = ConstraintLayout.LayoutParams(0, 0).apply {
                dimensionRatio = "h,16:9"
                setMargins(16)
                when (i % 2) {
                    0 -> {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToStart = ides[i + 1]
                        if (i == 0) {
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        } else {
                            topToBottom = ides[i - 2]
                        }
                    }
                    1 -> {
                        startToEnd = ides[i - 1]
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        if (i == 1) {
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        } else {
                            topToBottom = ides[i - 2]
                        }
                    }
                    else -> {
                        startToEnd = ides[i - 1]
                        endToStart = ides[i + 1]
                        if (i == 1) {
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        } else {
                            topToBottom = ides[i - 3]
                        }
                    }
                }

            }

            val frame: ImageView = ImageView(this, attr).apply {
                setImageResource(resourceIdes[i%resourceIdes.size])
                setPadding(10)
                setBackgroundResource(R.drawable.border)
                id = ides[i]
                layoutParams = params
            }

            frame.setOnClickListener {
                // TODO(liqargon): 選択したフレームをカメラPreviewに表示する機能
                val intent = Intent()
                intent.putExtra("resource_id", resourceIdes[i%resourceIdes.size])
                setResult(Activity.RESULT_OK, intent)
                finish()
            }

            cLayout.addView(frame)
        }

    }
}
