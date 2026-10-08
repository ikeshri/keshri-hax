package com.keshri.hax

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PiecePrediction(val piece: Char, val confidence: Float)

class PieceRecognizer(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val labels = charArrayOf(
        '.', 'P','N','B','R','Q','K',
        'p','n','b','r','q','k'
    )

    fun load(): Boolean {
        if (interpreter != null) return true
        return try {
            context.assets.open("chess_pieces.tflite").use { input ->
                val bytes = input.readBytes()
                val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
                buffer.put(bytes).rewind()
                interpreter = Interpreter(buffer)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun recognize(square: Bitmap): PiecePrediction {
        val model = interpreter ?: return PiecePrediction('.', 0f)
        val input = ByteBuffer.allocateDirect(1 * 64 * 64 * 3 * 4)
            .order(ByteOrder.nativeOrder())
        val scaled = Bitmap.createScaledBitmap(square, 64, 64, true)
        val pixels = IntArray(64 * 64)
        scaled.getPixels(pixels, 0, 64, 0, 0, 64, 64)
        for (px in pixels) {
            input.putFloat(ColorUtil.r(px) / 255f)
            input.putFloat(ColorUtil.g(px) / 255f)
            input.putFloat(ColorUtil.b(px) / 255f)
        }
        val output = Array(1) { FloatArray(labels.size) }
        model.run(input, output)
        var best = 0
        for (i in 1 until labels.size) if (output[0][i] > output[0][best]) best = i
        val conf = output[0][best].coerceIn(0f,1f)
        if (scaled !== square) scaled.recycle()
        return PiecePrediction(labels[best], conf)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

private object ColorUtil {
    fun r(c:Int)= (c shr 16) and 255
    fun g(c:Int)= (c shr 8) and 255
    fun b(c:Int)= c and 255
}
