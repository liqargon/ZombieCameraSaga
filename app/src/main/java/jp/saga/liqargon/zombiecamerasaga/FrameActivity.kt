package jp.saga.liqargon.zombiecamerasaga

import android.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

import android.content.res.Resources
import android.util.AttributeSet
import android.util.TypedValue
import android.util.TypedValue.applyDimension
import android.view.View.generateViewId
import android.view.ViewGroup
import androidx.core.view.setMargins
import androidx.core.view.setPadding

val Float.dp get() = applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)
val Float.sp get() = applyDimension(TypedValue.COMPLEX_UNIT_SP, this, Resources.getSystem().displayMetrics)
val Int.dp get() = toFloat().dp.toInt()
val Int.sp get() = toFloat().sp.toInt()

class FrameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_frame)

        val cLayout: ConstraintLayout = findViewById(R.id.frame_view)
        val attr: AttributeSet? = null

        // TODO(liqargon): 画像自動取得
        val id_s = (0..11).map { generateViewId() }

        for (i in 0..11) {

            // TODO(liqargon): 3で割れない数に対応, 横画像対応
            val params: ConstraintLayout.LayoutParams = ConstraintLayout.LayoutParams(0, 0).apply {
                dimensionRatio = "h,9:16"
                setMargins(16)
                when (i % 3) {
                    0 -> {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToStart = id_s[i + 1]
                        if (i == 0) {
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        } else {
                            topToBottom = id_s[i - 3]
                        }
                    }
                    1 -> {
                        startToEnd = id_s[i - 1]
                        endToStart = id_s[i + 1]
                        if (i == 1) {
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        } else {
                            topToBottom = id_s[i - 3]
                        }
                    }
                    else -> {
                        startToEnd = id_s[i - 1]
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        if (i == 2) {
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        } else {
                            topToBottom = id_s[i - 3]
                        }
                    }
                }

            }

            val frame: ImageView = ImageView(this, attr).apply {
                setImageResource(R.drawable.sakura_1)
                setPadding(10)
                setBackgroundResource(R.drawable.border)
                id = id_s[i]
                layoutParams = params
            }

            frame.setOnClickListener {
                // TODO(liqargon): 選択したフレームをカメラPreviewに表示する機能
                finish()
            }



            cLayout.addView(frame)
        }

    }
}
