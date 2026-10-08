package com.keshri.hax

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo

class LiveOverlayService : Service() {

    private lateinit var windowManager: WindowManager

    private var bubble: TextView? = null
    private var panel: LinearLayout? = null

    private var mediaProjection: MediaProjection? = null

    private var bubbleParams: WindowManager.LayoutParams? = null

    private var side = "WHITE"

    companion object {
        private const val CHANNEL_ID = "keshri_hax_analysis"
        private const val NOTIFICATION_ID = 7001
    }

    override fun onCreate() {
        super.onCreate()

        windowManager =
            getSystemService(Context.WINDOW_SERVICE)
                    as WindowManager

        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        side = intent?.getStringExtra("side")
            ?: "WHITE"

        /*
         * Android 14+:
         * Foreground service MUST be promoted before
         * requesting MediaProjection.
         */
        startProjectionForeground()

        val resultCode =
            intent?.getIntExtra(
                "resultCode",
                Activity.RESULT_CANCELED
            )

        val data =
            if (Build.VERSION.SDK_INT >= 33) {
                intent?.getParcelableExtra(
                    "data",
                    Intent::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra("data")
            }

        if (
            resultCode != Activity.RESULT_OK ||
            data == null
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {

            val manager =
                getSystemService(
                    MEDIA_PROJECTION_SERVICE
                ) as MediaProjectionManager

            mediaProjection =
                manager.getMediaProjection(
                    resultCode,
                    data
                )

        } catch (_: Exception) {

            stopSelf()
            return START_NOT_STICKY
        }

        showBubble()

        return START_NOT_STICKY
    }

    private fun startProjectionForeground() {

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_menu_view
                )
                .setContentTitle("Keshri Hax")
                .setContentText(
                    "Chess analysis overlay is active"
                )
                .setOngoing(true)
                .setPriority(
                    NotificationCompat.PRIORITY_LOW
                )
                .build()

        if (Build.VERSION.SDK_INT >= 29) {

            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Keshri Hax Analysis",
                NotificationManager.IMPORTANCE_LOW
            )

            channel.description =
                "Keshri Hax screen analysis service"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    private fun showBubble() {

        if (bubble != null) return

        bubble = TextView(this).apply {

            text = "♚"

            textSize = 25f

            gravity = Gravity.CENTER

            setTextColor(
                Color.rgb(0, 255, 157)
            )

            setBackgroundColor(
                Color.rgb(12, 18, 27)
            )

            elevation = 12f
        }

        bubbleParams =
            WindowManager.LayoutParams(
                58,
                58,
                if (Build.VERSION.SDK_INT >= 26)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {

                gravity = Gravity.TOP or Gravity.START

                x = 25
                y = 250
            }

        setupBubbleTouch()

        windowManager.addView(
            bubble,
            bubbleParams
        )
    }

    private fun setupBubbleTouch() {

        val view = bubble ?: return

        var downX = 0f
        var downY = 0f

        var startX = 0
        var startY = 0

        var moved = false

        view.setOnTouchListener { _, event ->

            val params =
                bubbleParams ?: return@setOnTouchListener false

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    downY = event.rawY

                    startX = params.x
                    startY = params.y

                    moved = false

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val dx =
                        (event.rawX - downX).toInt()

                    val dy =
                        (event.rawY - downY).toInt()

                    if (
                        kotlin.math.abs(dx) > 8 ||
                        kotlin.math.abs(dy) > 8
                    ) {
                        moved = true
                    }

                    params.x = startX + dx
                    params.y = startY + dy

                    windowManager.updateViewLayout(
                        view,
                        params
                    )

                    true
                }

                MotionEvent.ACTION_UP -> {

                    if (!moved) {
                        togglePanel()
                    }

                    true
                }

                else -> false
            }
        }
    }

    private fun togglePanel() {

        if (panel == null) {
            showPanel()
        } else {
            removePanel()
        }
    }

    private fun showPanel() {

        if (panel != null) return

        val root = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                22,
                18,
                22,
                18
            )

            setBackgroundColor(
                Color.rgb(8, 12, 20)
            )
        }

        val title = TextView(this).apply {

            text = "⚡ KESHRI HAX"

            textSize = 20f

            setTextColor(
                Color.rgb(0, 255, 157)
            )
        }

        val mode = TextView(this).apply {

            text =
                "OFFLINE ANALYSIS • YOUR SIDE: $side"

            textSize = 12f

            setTextColor(
                Color.LTGRAY
            )

            setPadding(0, 6, 0, 16)
        }

        val status = TextView(this).apply {

            text =
                "● SCREEN CAPTURE READY"

            textSize = 13f

            setTextColor(
                Color.rgb(0, 220, 150)
            )
        }

        val result = TextView(this).apply {

            text =
                "\nBEST MOVE\n—\n\nEVALUATION\n—\n\nDEPTH\n—"

            textSize = 15f

            setTextColor(Color.WHITE)

            setPadding(
                0,
                20,
                0,
                20
            )
        }

        val close = Button(this).apply {

            text = "MINIMIZE"

            setOnClickListener {
                removePanel()
            }
        }

        root.addView(title)
        root.addView(mode)
        root.addView(status)
        root.addView(result)
        root.addView(close)

        panel = root

        val params =
            WindowManager.LayoutParams(
                dp(310),
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= 26)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {

                gravity =
                    Gravity.TOP or Gravity.END

                x = 18
                y = 160
            }

        windowManager.addView(
            root,
            params
        )
    }

    private fun removePanel() {

        panel?.let {

            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }

        panel = null
    }

    private fun dp(value: Int): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    override fun onDestroy() {

        removePanel()

        bubble?.let {

            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }

        bubble = null

        mediaProjection?.stop()
        mediaProjection = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null
}
