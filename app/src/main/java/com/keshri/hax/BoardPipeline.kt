package com.keshri.hax

import android.graphics.Bitmap
import android.util.Log

class BoardPipeline(
    private val pieceRecognizer: PieceRecognizer,
    private val boardDetector: ChessVision = ChessVision()
) {

    companion object {
        private const val TAG = "BoardPipeline"
    }

    data class Result(
        val success: Boolean,
        val fen: String? = null,
        val board: Array<CharArray>? = null,
        val confidence: Float = 0f,
        val message: String = ""
    )

    fun analyze(
        screenshot: Bitmap,
        sideToMove: Char = 'w'
    ): Result {

        if (screenshot.width <= 0 || screenshot.height <= 0) {
            return Result(
                false,
                message = "Invalid screenshot"
            )
        }

        if (sideToMove != 'w' && sideToMove != 'b') {
            return Result(
                false,
                message = "Invalid side to move"
            )
        }

        var boardBitmap: Bitmap? = null
        val squares = mutableListOf<Bitmap>()

        return try {

            val region = boardDetector.findBoard(screenshot)

                ?: return Result(
                    false,
                    message = "Chess board not detected"
                )

            boardBitmap = boardDetector.cropBoard(
                screenshot,
                region
            )

            val squareArray =
                boardDetector.splitSquares(boardBitmap)

            if (squareArray.size != 64) {
                return Result(
                    false,
                    message = "Could not create 64 squares"
                )
            }

            squares.addAll(squareArray)

            val board = Array(8) {
                CharArray(8) { '.' }
            }

            var confidenceTotal = 0f
            var confidenceCount = 0

            for (index in squares.indices) {

                val prediction =
                    pieceRecognizer.recognize(
                        squares[index]
                    )

                val row = index / 8
                val col = index % 8

                board[row][col] =
                    if (prediction.confidence >= 0.60f) {
                        prediction.piece
                    } else {
                        '.'
                    }

                if (prediction.confidence > 0f) {
                    confidenceTotal +=
                        prediction.confidence
                    confidenceCount++
                }
            }

            val averageConfidence =
                if (confidenceCount > 0) {
                    confidenceTotal /
                            confidenceCount
                } else {
                    0f
                }

            if (!PositionValidator.isValid(board)) {
                return Result(
                    success = false,
                    board = board,
                    confidence = averageConfidence,
                    message =
                        "Detected position is not a valid chess position"
                )
            }

            val fen = FenBuilder.fromBoard(
                board = board,
                sideToMove = sideToMove
            )

            Log.d(
                TAG,
                "FEN=$fen confidence=$averageConfidence"
            )

            Result(
                success = true,
                fen = fen,
                board = board,
                confidence = averageConfidence,
                message = "Board analyzed successfully"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Board analysis failed",
                e
            )

            Result(
                success = false,
                message =
                    e.message ?: "Board analysis error"
            )

        } finally {

            squares.forEach {
                try {
                    it.recycle()
                } catch (_: Exception) {
                }
            }

            try {
                boardBitmap?.recycle()
            } catch (_: Exception) {
            }
        }
    }

    fun close() {
        pieceRecognizer.close()
    }
}
