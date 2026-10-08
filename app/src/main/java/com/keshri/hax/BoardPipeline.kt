package com.keshri.hax

import android.graphics.Bitmap
import android.util.Log

/**
 * Complete offline board-analysis pipeline:
 *
 * Bitmap
 *   ↓
 * Board detection
 *   ↓
 * Board crop
 *   ↓
 * 64 squares
 *   ↓
 * Piece recognition
 *   ↓
 * Position validation
 *   ↓
 * FEN
 */
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

    /**
     * Analyze one screen frame.
     *
     * This is intended for offline/self-game/puzzle analysis.
     */
    fun analyze(
        screenshot: Bitmap,
        sideToMove: Char = 'w'
    ): Result {

        if (screenshot.width <= 0 || screenshot.height <= 0) {
            return Result(
                success = false,
                message = "Invalid screenshot"
            )
        }

        if (sideToMove != 'w' && sideToMove != 'b') {
            return Result(
                success = false,
                message = "Invalid side to move"
            )
        }

        return try {
            // 1. Locate chess board.
            val region = boardDetector.findBoard(screenshot)

            if (region == null) {
                return Result(
                    success = false,
                    message = "Chess board not detected"
                )
            }

            // 2. Crop board.
            val boardBitmap = boardDetector.cropBoard(
                screenshot,
                region
            )

            if (boardBitmap.width <= 0 || boardBitmap.height <= 0) {
                return Result(
                    success = false,
                    message = "Invalid board crop"
                )
            }

            // 3. Split board into 64 squares.
            val squares = boardDetector.splitSquares(
                boardBitmap
            )

            if (squares.size != 64) {
                return Result(
                    success = false,
                    message = "Expected 64 squares, got ${squares.size}"
                )
            }

            val board = Array(8) {
                CharArray(8) { '?' }
            }

            var totalConfidence = 0f
            var recognizedSquares = 0

            // 4. Recognize every square.
            for (index in squares.indices) {

                val squareBitmap = squares[index]

                val prediction = pieceRecognizer.recognize(
                    squareBitmap
                )

                val row = index / 8
                val col = index % 8

                board[row][col] = prediction.piece

                if (prediction.confidence > 0f) {
                    totalConfidence += prediction.confidence
                    recognizedSquares++
                }

                squareBitmap.recycle()
            }

            boardBitmap.recycle()

            val averageConfidence =
                if (recognizedSquares > 0) {
                    totalConfidence / recognizedSquares
                } else {
                    0f
                }

            // 5. Convert unknown predictions to empty only
            // when confidence is extremely low.
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    if (board[r][c] == '?') {
                        board[r][c] = '1'
                    }
                }
            }

            // 6. Validate chess position.
            val validation = PositionValidator.validate(board)

            if (!validation.isValid) {
                return Result(
                    success = false,
                    board = board,
                    confidence = averageConfidence,
                    message = validation.message
                )
            }

            // 7. Build FEN.
            val fen = FenBuilder.build(
                board = board,
                sideToMove = sideToMove
            )

            Log.d(
                TAG,
                "Board detected. FEN=$fen confidence=$averageConfidence"
            )

            Result(
                success = true,
                fen = fen,
                board = board,
                confidence = averageConfidence,
                message = "Board analyzed successfully"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Board pipeline failed", e)

            Result(
                success = false,
                message = e.message ?: "Unknown analysis error"
            )
        }
    }

    fun close() {
        try {
            pieceRecognizer.close()
        } catch (e: Exception) {
            Log.w(TAG, "Recognizer close failed", e)
        }
    }
}
