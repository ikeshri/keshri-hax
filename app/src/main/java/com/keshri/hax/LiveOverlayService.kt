package com.keshri.hax

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LiveOverlayService : Service() {

    private lateinit var windowManager: WindowManager

    private var bubble: TextView? = null
    private var panel: LinearLayout? = null

    private var mediaProjection: MediaProjection? =
        null

    private var virtualDisplay: VirtualDisplay? =
        null

    private var imageReader: ImageReader? =
        null

    private var bubbleParams:
        WindowManager.LayoutParams? = null

    private var side = "WHITE"

    private var analysisSide = 'w'

    private var boardPipeline: BoardPipeline? =
        null

    private var chessEngine: ChessEngine? =
        null

    private var statusText: TextView? =
        null

    private var resultText: TextView? =
        null

    private val worker =
        Executors.newSingleThreadExecutor()

    private val processing =
        AtomicBoolean(false)

    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    companion object {
        private const val CHANNEL_ID =
            "keshri_hax_analysis"

        private const val NOTIFICATION_ID =
            7001

        private const val VIRTUAL_DISPLAY_NAME =
            "KeshriHaxCapture"
    }

    override fun onCreate() {
        super.onCreate()

        windowManager =
            getSystemService(
                Context.WINDOW_SERVICE
            ) as WindowManager

        createNotificationChannel()

        val metrics =
            DisplayMetrics()

        @Suppress("DEPRECATION")
        windowManager.defaultDisplay
            .getRealMetrics(metrics)

        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi

        chessEngine =
            ChessEngine(this)

        val recognizer =
            PieceRecognizer(this)

        if (recognizer.load()) {
            boardPipeline =
                BoardPipeline(recognizer)
        } else {
            recognizer.close()
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        side =
            intent?.getStringExtra("side")
                ?: "WHITE"

        analysisSide =
            if (side == "BLACK") {
                'b'
            } else {
                'w'
            }

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
                intent?.getParcelableExtra(
                    "data"
                )
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

            setupScreenCapture()

            showBubble()

        } catch (_: Exception) {

            stopSelf()

            return START_NOT_STICKY
        }

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
                .setContentTitle(
                    "Keshri Hax"
                )
                .setContentText(
                    "Offline chess analysis active"
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
                ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
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

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Keshri Hax Analysis",
                    NotificationManager
                        .IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun setupScreenCapture() {

        val projection =
            mediaProjection ?: return

        if (
            screenWidth <= 0 ||
            screenHeight <= 0
        ) {
            return
        }

        imageReader =
            ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

        imageReader?.setOnImageAvailableListener(
            { reader ->

                if (
                    !processing.compareAndSet(
                        false,
                        true
                    )
                ) {
                    return@setOnImageAvailableListener
                }

                var image =
                    reader.acquireLatestImage()

                if (image == null) {
                    processing.set(false)
                    return@setOnImageAvailableListener
                }

                worker.execute {

                    try {

                        val bitmap =
                            ScreenCaptureAnalyzer
                                .imageToBitmap(image)

                        image.close()
                        image = null

                        if (bitmap != null) {
                            analyzeFrame(bitmap)
                        }

                    } catch (_: Exception) {

                        try {
                            image?.close()
                        } catch (_: Exception) {
                        }

                    } finally {

                        processing.set(false)
                    }
                }

            },
            null
        )

        virtualDisplay =
            projection.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager
                    .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
    }

    private fun analyzeFrame(
        bitmap: Bitmap
    ) {

        try {

            val pipeline =
                boardPipeline

            if (pipeline == null) {

                updateStatus(
                    "MODEL NOT READY"
                )

                bitmap.recycle()

                return
            }

            val result =
                pipeline.analyze(
                    screenshot = bitmap,
                    sideToMove = analysisSide
                )

            bitmap.recycle()

            if (!result.success) {

                updateStatus(
                    "BOARD: NOT DETECTED"
                )

                return
            }

            updateStatus(
                "BOARD: DETECTED"
            )

            val fen =
                result.fen

            if (fen == null) {
                return
            }

            val engine =
                chessEngine

            if (engine == null) {
                return
            }

            val engineResult =
                engine.analyze(
                    fen = fen,
                    depth = 16,
                    timeoutMs = 8000
                )

            updateResult(
                engineResult
            )

        } catch (_: Exception) {

            updateStatus(
                "ANALYSIS ERROR"
            )
        }
    }

    private fun updateStatus(
        text: String
    ) {

        statusText?.post {
            statusText?.text =
                "● $text"
        }
    }

    private fun updateResult(
        result: EngineResult
    ) {

        resultText?.post {

            if (result.error != null) {

                resultText?.text =
                    """
                    BEST MOVE
                    —
                    
                    ERROR
                    ${result.error}
                    """.trimIndent()

                return@post
            }

            resultText?.text =
                """
                BEST MOVE
                ${result.bestMove ?: "—"}
                
                EVALUATION
                ${result.evaluation ?: "—"}
                
                DEPTH
                ${result.depth}
                
                PV
                ${result.pv.take(5).joinToString(" ")}
                """.trimIndent()
        }
    }

    private fun showBubble() {

        if (bubble != null) {
            return
        }

        bubble =
            TextView(this).apply {

                text = "♚"
                textSize = 25f
                gravity = Gravity.CENTER

                setTextColor(
                    Color.rgb(
                        0,
                        255,
                        157
                    )
                )

                setBackgroundColor(
                    Color.rgb(
                        12,
                        18,
                        27
                    )
                )

                elevation = 12f
            }

        bubbleParams =
            WindowManager.LayoutParams(
                dp(58),
                dp(58),
                if (Build.VERSION.SDK_INT >= 26) {
                    WindowManager.LayoutParams
                        .TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams
                        .TYPE_PHONE
                },
                WindowManager.LayoutParams
                    .FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {

                gravity =
                    Gravity.TOP or Gravity.START

                x = dp(18)
                y = dp(240)
            }

        setupBubbleTouch()

        windowManager.addView(
            bubble,
            bubbleParams
        )
    }

    private fun setupBubbleTouch() {

        val view =
            bubble ?: return

        var downX = 0f
        var downY = 0f

        var startX = 0
        var startY = 0

        var moved = false

        view.setOnTouchListener { _, event ->

            val params =
                bubbleParams
                    ?: return@setOnTouchListener false

            when (
                event.actionMasked
            ) {

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
                        (
                            event.rawX -
                                    downX
                        ).toInt()

                    val dy =
                        (
                            event.rawY -
                                    downY
                        ).toInt()

                    if (
                        kotlin.math.abs(dx) > 8 ||
                        kotlin.math.abs(dy) > 8
                    ) {
                        moved = true
                    }

                    params.x =
                        startX + dx

                    params.y =
                        startY + dy

                    windowManager
                        .updateViewLayout(
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

        if (panel != null) {
            return
        }

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(18),
                    dp(20),
                    dp(18)
                )

                setBackgroundColor(
                    Color.rgb(
                        8,
                        12,
                        20
                    )
                )
            }

        val title =
            TextView(this).apply {

                text =
                    "⚡ KESHRI HAX"

                textSize = 20f

                setTextColor(
                    Color.rgb(
                        0,
                        255,
                        157
                    )
                )
            }

        val mode =
            TextView(this).apply {

                text =
                    "OFFLINE ANALYSIS • SIDE: $side"

                textSize = 12f

                setTextColor(
                    Color.LTGRAY
                )

                setPadding(
                    0,
                    dp(6),
                    0,
                    dp(14)
                )
            }

        statusText =
            TextView(this).apply {

                text =
                    "● SCREEN CAPTURE READY"

                textSize = 13f

                setTextColor(
                    Color.rgb(
                        0,
                        220,
                        150
                    )
                )
            }

        resultText =
            TextView(this).apply {

                text =
                    """
                    BEST MOVE
                    —
                    
                    EVALUATION
                    —
                    
                    DEPTH
                    —
                    """.trimIndent()

                textSize = 15f

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    0,
                    dp(18),
                    0,
                    dp(18)
                )
            }

        val analyze =
            Button(this).apply {

                text =
                    "ANALYZE CURRENT FRAME"

                setOnClickListener {
                    requestSingleAnalysis()
                }
            }

        val close =
            Button(this).apply {

                text = "MINIMIZE"

                setOnClickListener {
                    removePanel()
                }
            }

        root.addView(title)
        root.addView(mode)
        root.addView(statusText)
        root.addView(resultText)
        root.addView(analyze)
        root.addView(close)

        panel = root

        val params =
            WindowManager.LayoutParams(
                dp(315),
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= 26) {
                    WindowManager.LayoutParams
                        .TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams
                        .TYPE_PHONE
                },
                WindowManager.LayoutParams
                    .FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {

                gravity =
                    Gravity.TOP or Gravity.END

                x = dp(14)
                y = dp(140)
            }

        windowManager.addView(
            root,
            params
        )
    }

    private fun requestSingleAnalysis() {

        updateStatus(
            "WAITING FOR NEXT SCREEN FRAME"
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
        statusText = null
        resultText = null
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                    resources
                        .displayMetrics
                        .density
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

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.close()
        imageReader = null

        mediaProjection?.stop()
        mediaProjection = null

        worker.shutdownNow()

        boardPipeline?.close()
        boardPipeline = null

        chessEngine?.stop()
        chessEngine = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null
}
