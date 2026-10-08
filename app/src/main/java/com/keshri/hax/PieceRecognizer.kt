package com.keshri.hax

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class PieceRecognizer(
    private val context: Context
) {

    private var interpreter: Interpreter? = null

    /*
     * Expected model:
     *
     * app/src/main/assets/chess_pieces.tflite
     *
     * The model must classify:
     *
     * 0  empty
     * 1  white pawn
     * 2  white knight
     * 3  white bishop
     * 4  white rook
     * 5  white queen
     * 6  white king
     * 7  black pawn
     * 8  black knight
     * 9  black bishop
     * 10 black rook
     * 11 black queen
     * 12 black king
     */

    private val labels = arrayOf(
        '.',
        'P',
        'N',
        'B',
        'R',
        'Q',
        'K',
        'p',
        'n',
        'b',
        'r',
        'q',
        'k'
    )

    private val inputSize = 64

    fun load(): Boolean {

        return try {

            val model =
                loadModel("chess_pieces.tflite")

            interpreter =
                Interpreter(model)

            true

        } catch (_: Exception) {

            interpreter = null

            false
        }
    }

    fun recognize(
        square: Bitmap
    ): PiecePrediction {

        val engine = interpreter
            ?: return PiecePrediction(
                piece = '.',
                confidence = 0f
            )

        return try {

            val input =
                bitmapToInput(square)

            /*
             * Output shape expected:
             * [1][13]
             */
            val output =
                Array(1) {
                    FloatArray(labels.size)
                }

            engine.run(
                input,
                output
            )

            var bestIndex = 0
            var bestScore = output[0][0]

            for (i in 1 until labels.size) {

                if (
                    output[0][i] >
                    bestScore
                ) {

                    bestIndex = i
                    bestScore =
                        output[0][i]
                }
            }

            PiecePrediction(
                piece = labels[bestIndex],
                confidence = bestScore
            )

        } catch (_: Exception) {

            PiecePrediction(
                piece = '.',
                confidence = 0f
            )
        }
    }

    fun recognizeBoard(
        squares: Array<Bitmap>
    ): Array<CharArray> {

        require(squares.size == 64) {
            "Exactly 64 chess squares required"
        }

        val board =
            Array(8) {
                CharArray(8) { '.' }
            }

        for (index in squares.indices) {

            val prediction =
                recognize(squares[index])

            /*
             * Low-confidence predictions are treated
             * as empty/unknown rather than inventing a piece.
             */
            board[index / 8][index % 8] =
                if (prediction.confidence >= 0.60f) {
                    prediction.piece
                } else {
                    '.'
                }
        }

        return board
    }

    fun close() {

        interpreter?.close()
        interpreter = null
    }

    private fun bitmapToInput(
        bitmap: Bitmap
    ): ByteBuffer {

        val resized =
            Bitmap.createScaledBitmap(
                bitmap,
                inputSize,
                inputSize,
                true
            )

        val buffer =
            ByteBuffer.allocateDirect(
                1 *
                    inputSize *
                    inputSize *
                    3 *
                    4
            )

        buffer.order(
            ByteOrder.nativeOrder()
        )

        for (y in 0 until inputSize) {

            for (x in 0 until inputSize) {

                val pixel =
                    resized.getPixel(x, y)

                buffer.putFloat(
                    ((pixel shr 16) and 0xFF) / 255f
                )

                buffer.putFloat(
                    ((pixel shr 8) and 0xFF) / 255f
                )

                buffer.putFloat(
                    (pixel and 0xFF) / 255f
                )
            }
        }

        buffer.rewind()

        if (resized !== bitmap) {
            resized.recycle()
        }

        return buffer
    }

    private fun loadModel(
        filename: String
    ): ByteBuffer {

        val descriptor =
            context.assets.openFd(filename)

        FileInputStream(
            descriptor.fileDescriptor
        ).use { input ->

            val channel =
                input.channel

            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                descriptor.startOffset,
                descriptor.declaredLength
            )
        }
    }
}

data class PiecePrediction(
    val piece: Char,
    val confidence: Float
)
