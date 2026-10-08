package com.keshri.hax

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.*
import android.graphics.drawable.GradientDrawable

class MainActivity : Activity() {
    companion object { private const val REQUEST_CAPTURE = 501 }

    private var selectedSide = "WHITE"
    private lateinit var status: TextView
    private lateinit var whiteCard: TextView
    private lateinit var blackCard: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun bg(stroke: Int, fill: Int, radius: Float = 22f): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius
            setStroke(2, stroke)
        }

    private fun tv(text: String, size: Float, color: Int): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            typeface = Typeface.create("sans", Typeface.NORMAL)
        }

    private fun buildUi() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(4, 7, 12))
        }

        val glow = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(0,55,42), Color.rgb(4,7,12), Color.rgb(5,18,42))
            )
        }
        root.addView(glow, FrameLayout.LayoutParams(-1,-1))

        val scroll = ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 42, 28, 42)
        }
        scroll.addView(box)
        root.addView(scroll, FrameLayout.LayoutParams(-1,-1))

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = tv("♚  KESHRI HAX", 27f, Color.rgb(0,255,157))
        brand.typeface = Typeface.DEFAULT_BOLD
        top.addView(brand, LinearLayout.LayoutParams(0,64,1f))
        val settings = tv("⚙", 27f, Color.WHITE).apply { gravity = Gravity.CENTER }
        top.addView(settings, LinearLayout.LayoutParams(64,64))
        box.addView(top)

        val sub = tv("OFFLINE CHESS INTELLIGENCE", 12f, Color.LTGRAY)
        sub.setPadding(4, 0, 0, 20)
        box.addView(sub)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = bg(Color.rgb(0,170,115), Color.argb(55,0,255,157), 30f)
            setPadding(20, 22, 20, 22)
        }
        val core = tv("◉", 52f, Color.rgb(0,255,157)).apply { gravity = Gravity.CENTER }
        hero.addView(core, LinearLayout.LayoutParams(-1,72))
        val h1 = tv("NEURAL ANALYSIS CORE", 16f, Color.WHITE).apply { gravity = Gravity.CENTER }
        h1.typeface = Typeface.DEFAULT_BOLD
        hero.addView(h1)
        hero.addView(tv("Vision  •  FEN  •  Engine", 12f, Color.LTGRAY).apply { gravity=Gravity.CENTER })
        box.addView(hero, LinearLayout.LayoutParams(-1, 145).apply { bottomMargin=22 })

        box.addView(tv("ANALYSIS MODE", 12f, Color.GRAY).apply {
            setPadding(4,0,0,10)
        })

        val sides = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
        }
        whiteCard = sideCard("♙  WHITE", true)
        blackCard = sideCard("♟  BLACK", false)
        sides.addView(whiteCard, LinearLayout.LayoutParams(0,70,1f).apply { rightMargin=7 })
        sides.addView(blackCard, LinearLayout.LayoutParams(0,70,1f).apply { leftMargin=7 })
        box.addView(sides)

        val start = TextView(this).apply {
            text = "▶   START ANALYSIS"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(2,15,12))
            typeface = Typeface.DEFAULT_BOLD
            background = bg(Color.rgb(0,255,157), Color.rgb(0,255,157), 24f)
            setOnClickListener { startAnalysis() }
        }
        box.addView(start, LinearLayout.LayoutParams(-1,72).apply { topMargin=22 })

        status = tv("●  SYSTEM READY", 13f, Color.rgb(0,255,157)).apply {
            gravity = Gravity.CENTER
            background = bg(Color.rgb(30,70,58), Color.argb(45,0,255,157), 18f)
            setPadding(16,0,16,0)
        }
        box.addView(status, LinearLayout.LayoutParams(-1,52).apply { topMargin=16 })

        val info = tv(
            "Screen capture starts only after Android permission.\n" +
            "Use Keshri Hax for offline positions, puzzles and self-game analysis.",
            12f, Color.GRAY
        )
        info.gravity = Gravity.CENTER
        info.setPadding(18,20,18,0)
        box.addView(info, LinearLayout.LayoutParams(-1,90))

        setContentView(root)
        refreshSideUi()
    }

    private fun sideCard(label: String, selected: Boolean): TextView =
        TextView(this).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                selectedSide = if (label.contains("WHITE")) "WHITE" else "BLACK"
                refreshSideUi()
            }
        }

    private fun refreshSideUi() {
        val active = Color.rgb(0,255,157)
        val inactive = Color.rgb(95,105,116)
        whiteCard.setTextColor(if (selectedSide=="WHITE") Color.rgb(2,18,13) else Color.WHITE)
        blackCard.setTextColor(if (selectedSide=="BLACK") Color.rgb(2,18,13) else Color.WHITE)
        whiteCard.background = bg(
            if (selectedSide=="WHITE") active else inactive,
            if (selectedSide=="WHITE") active else Color.argb(35,255,255,255)
        )
        blackCard.background = bg(
            if (selectedSide=="BLACK") active else inactive,
            if (selectedSide=="BLACK") active else Color.argb(35,255,255,255)
        )
    }

    private fun startAnalysis() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
            status.text = "●  GRANT OVERLAY PERMISSION, THEN TAP START AGAIN"
            status.setTextColor(Color.YELLOW)
            return
        }

        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    @Deprecated("Android framework callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE || resultCode != RESULT_OK || data == null) {
            status.text = "●  SCREEN CAPTURE CANCELLED"
            status.setTextColor(Color.rgb(255,120,120))
            return
        }

        val serviceIntent = Intent(this, LiveOverlayService::class.java).apply {
            putExtra("side", selectedSide)
            putExtra("resultCode", resultCode)
            putExtra("data", data)
        }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent)
        else startService(serviceIntent)

        status.text = "●  ANALYZER RUNNING — PANEL IS NOW ACTIVE"
        status.setTextColor(Color.rgb(0,255,157))
    }
}
