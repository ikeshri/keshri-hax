package com.keshri.hax

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.*
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LiveOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "keshri_hax_analysis"
        private const val NOTIFICATION_ID = 42
    }

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var panel: OverlayPanelView? = null
    private var windowManager: WindowManager? = null
    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()

    private var side = "WHITE"
    private var lastAnalysis = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        side = intent?.getStringExtra("side") ?: "WHITE"
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtraCompat<Intent>("data")

        if (resultCode != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundNow()

        if (running.compareAndSet(false, true)) {
            showOverlay()
            setupCapture(resultCode, data)
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNow() {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Keshri Hax")
            .setContentText("Chess analysis is active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        val type = if (Build.VERSION.SDK_INT >= 29)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun setupCapture(resultCode: Int, data: Intent) {
        try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)

            val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            reader?.setOnImageAvailableListener({ r ->
                if (!running.get() || paused.get()) return@setOnImageAvailableListener
                val now = SystemClock.elapsedRealtime()
                if (now - lastAnalysis < 1400) return@setOnImageAvailableListener
                lastAnalysis = now
                val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                executor.execute { processImage(image) }
            }, Handler(Looper.getMainLooper()))

            virtualDisplay = projection?.createVirtualDisplay(
                "KeshriHaxCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader?.surface, null, null
            )

            updatePanel("CAPTURING", "—", "—", "—", "—",
                "Screen capture is live. Looking for a chess board.")
        } catch (e: Exception) {
            updatePanel("CAPTURE ERROR", "—", "—", "—", "—",
                e.message ?: "Could not start screen capture")
        }
    }

    private fun processImage(image: Image) {
        var bitmap: Bitmap? = null
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val full = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            full.copyPixelsFromBuffer(buffer)
            bitmap = Bitmap.createBitmap(full, 0, 0, image.width, image.height)
            full.recycle()

            updatePanel("SCANNING", "—", "—", "—", "—",
                "Searching the captured screen for a chess board.")

            // The actual vision pipeline is deliberately kept local/offline.
            // It requires chess_pieces.tflite in app/src/main/assets.
            val recognizer = PieceRecognizer(this)
            if (!recognizer.load()) {
                updatePanel("MODEL MISSING", "—", "—", "—", "—",
                    "Add chess_pieces.tflite to app/src/main/assets.")
                recognizer.close()
                return
            }

            val pipeline = BoardPipeline(recognizer)
            val result = pipeline.analyze(bitmap, if (side == "BLACK") 'b' else 'w')
            if (!result.success || result.fen == null) {
                updatePanel("BOARD NOT READY", "—", "—", "—",
                    "${(result.confidence * 100).toInt()}%",
                    result.message)
                pipeline.close()
                return
            }

            updatePanel("POSITION FOUND", "—", "—", "—",
                "${(result.confidence * 100).toInt()}%", result.fen)

            val engine = ChessEngine(this)
            if (!engine.start()) {
                updatePanel("VISION OK / ENGINE MISSING", "—", "—", "—",
                    "${(result.confidence * 100).toInt()}%",
                    "Add a compatible Stockfish binary to app/src/main/assets/stockfish.")
                engine.close()
                pipeline.close()
                return
            }

            val analysis = engine.analyze(result.fen, 18)
            updatePanel(
                "ANALYZING",
                analysis.bestMove ?: "—",
                analysis.evaluation ?: "—",
                analysis.depth?.toString() ?: "—",
                "${(result.confidence * 100).toInt()}%",
                analysis.pv ?: result.fen
            )
            engine.close()
            pipeline.close()
        } catch (e: Exception) {
            updatePanel("ANALYSIS ERROR", "—", "—", "—", "—",
                e.message ?: "Analysis failed")
        } finally {
            try { image.close() } catch (_: Exception) {}
            try { bitmap?.recycle() } catch (_: Exception) {}
        }
    }

    private fun updatePanel(
        status: String,
        best: String,
        eval: String,
        depth: String,
        confidence: String,
        detail: String
    ) {
        Handler(Looper.getMainLooper()).post {
            panel?.state = OverlayPanelView.State(status, best, eval, depth, confidence, detail)
        }
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val view = OverlayPanelView(
            this,
            onStop = { stopSelf() },
            onRescan = { lastAnalysis = 0L },
            onPause = { paused.set(!paused.get()) }
        )
        panel = view

        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            330,
            535,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 18
            y = 120
        }

        wm.addView(view, params)
    }

    override fun onDestroy() {
        running.set(false)
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        try { projection?.stop() } catch (_: Exception) {}
        try { panel?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        executor.shutdownNow()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Keshri Hax Analysis",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

private inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? =
    if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(key, T::class.java)
    else @Suppress("DEPRECATION") getParcelableExtra(key)
