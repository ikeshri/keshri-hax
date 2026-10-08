package com.keshri.hax

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_CAPTURE = 501
    }

    private var selectedSide = "WHITE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()
    }

    private fun buildUi() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 48, 32, 48)
            setBackgroundColor(Color.rgb(7, 11, 18))
        }

        val title = TextView(this).apply {
            text = "♚ KESHRI HAX"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(0, 255, 157))
        }

        val subtitle = TextView(this).apply {
            text = "OFFLINE CHESS ANALYZER"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 35)
        }

        val sideLabel = TextView(this).apply {
            text = "YOUR SIDE"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        val whiteButton = Button(this).apply {
            text = "WHITE"
            setOnClickListener {
                selectedSide = "WHITE"
                updateSideButtons(this, null)
            }
        }

        val blackButton = Button(this).apply {
            text = "BLACK"
            setOnClickListener {
                selectedSide = "BLACK"
                updateSideButtons(null, this)
            }
        }

        val startButton = Button(this).apply {
            text = "START ANALYSIS"
            setOnClickListener {
                startAnalysis()
            }
        }

        val info = TextView(this).apply {
            text = """
                Designed for offline positions,
                puzzles and self-game analysis.

                Screen capture is used only after
                you explicitly grant Android permission.
            """.trimIndent()

            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(0, 30, 0, 0)
        }

        root.addView(
            title,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            subtitle,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(sideLabel)

        root.addView(
            whiteButton,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            blackButton,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            startButton,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
        )

        root.addView(info)

        setContentView(root)
    }

    private fun updateSideButtons(
        white: Button?,
        black: Button?
    ) {
        white?.alpha = 1f
        black?.alpha = 1f
    }

    private fun startAnalysis() {

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )

            startActivity(intent)
            return
        }

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        startActivityForResult(
            manager.createScreenCaptureIntent(),
            REQUEST_CAPTURE
        )
    }

    @Deprecated("Android framework callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode != REQUEST_CAPTURE ||
            resultCode != RESULT_OK ||
            data == null
        ) {
            return
        }

        val serviceIntent =
            Intent(
                this,
                LiveOverlayService::class.java
            ).apply {

                putExtra(
                    "side",
                    selectedSide
                )

                putExtra(
                    "resultCode",
                    resultCode
                )

                putExtra(
                    "data",
                    data
                )
            }

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(
                serviceIntent
            )
        } else {
            startService(
                serviceIntent
            )
        }
    }
}
