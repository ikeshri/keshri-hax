package com.keshri.hax

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private var launching = false
    private var selectedSide = "WHITE"

    private val captureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            launching = false

            if (result.resultCode != Activity.RESULT_OK ||
                result.data == null
            ) {
                Toast.makeText(
                    this,
                    "Screen capture permission required",
                    Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }

            val intent = Intent(this, LiveOverlayService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
                putExtra("side", selectedSide)
            }

            startForegroundService(intent)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 32)
            setBackgroundColor(0xFF05070D.toInt())
        }

        val title = TextView(this).apply {
            text = "⚡ KESHRI HAX ⚡"
            textSize = 27f
            setTextColor(0xFF00FF9D.toInt())
        }

        val subtitle = TextView(this).apply {
            text = "OFFLINE CHESS ANALYSIS"
            textSize = 13f
            setTextColor(0xFF8A94A6.toInt())
        }

        val sideTitle = TextView(this).apply {
            text = "YOUR SIDE"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
        }

        val side = Spinner(this)

        side.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("WHITE", "BLACK")
        )

        side.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    selectedSide =
                        if (position == 0) "WHITE" else "BLACK"
                }
            }

        val launch = Button(this).apply {
            text = "🚀 START BOARD ANALYSIS"
            setOnClickListener {
                requestCapture()
            }
        }

        val stop = Button(this).apply {
            text = "STOP"
            setOnClickListener {
                stopService(
                    Intent(this@MainActivity, LiveOverlayService::class.java)
                )
            }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(sideTitle)
        root.addView(side)
        root.addView(launch)
        root.addView(stop)

        setContentView(root)
    }

    private fun requestCapture() {

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Overlay permission enable karo",
                Toast.LENGTH_LONG
            ).show()

            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }

        if (launching) return

        launching = true

        val manager =
            getSystemService(MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager

        captureLauncher.launch(
            manager.createScreenCaptureIntent()
        )
    }
}
