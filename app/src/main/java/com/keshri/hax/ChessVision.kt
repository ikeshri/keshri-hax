package com.keshri.hax

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

data class BoardRegion(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top
}

class ChessVision {

    /**
     * Finds a likely square chessboard region.
     *
     * This is intentionally conservative:
     * it searches for a large region whose horizontal and
     * vertical color transitions resemble an 8x8 board.
     */
    fun findBoard(bitmap: Bitmap): BoardRegion? {

        if (bitmap.width < 200 || bitmap.height < 200) {
            return null
        }

        val maxWidth = bitmap.width
        val maxHeight = bitmap.height

        /*
         * Most phone chess boards are close to square.
         * We inspect several possible square sizes instead
         * of assuming the board fills the whole screen.
         */
        val largest =
            minOf(maxWidth, maxHeight)

        val sizes = listOf(
            (largest * 0.95f).toInt(),
            (largest * 0.85f).toInt(),
            (largest * 0.75f).toInt(),
            (largest * 0.65f).toInt()
        )

        var best: BoardRegion? = null
        var bestScore = 0.0

        for (size in sizes) {

            if (size < 160) continue

            val step =
                maxOf(8, size / 20)

            var top = 0

            while (top + size <= maxHeight) {

                var left = 0

                while (left + size <= maxWidth) {

                    val region = BoardRegion(
                        left = left,
                        top = top,
                        right = left + size,
                        bottom = top + size
                    )

                    val score =
                        boardScore(
                            bitmap,
                            region,
                            step
                        )

                    if (score > bestScore) {
                        bestScore = score
                        best = region
                    }

                    left += step
                }

                top += step
            }
        }

        return if (
            best != null &&
            bestScore >= 0.55
        ) {
            best
        } else {
            null
        }
    }

    /**
     * Crops the detected board.
     */
    fun cropBoard(
        bitmap: Bitmap,
        region: BoardRegion
    ): Bitmap {

        return Bitmap.createBitmap(
            bitmap,
            region.left,
            region.top,
            region.width,
            region.height
        )
    }

    /**
     * Splits an 8x8 board into 64 square bitmaps.
     *
     * Index:
     * 0  = a8
     * 7  = h8
     * 56 = a1
     * 63 = h1
     */
    fun splitSquares(
        board: Bitmap
    ): Array<Bitmap> {

        val squareWidth =
            board.width / 8

        val squareHeight =
            board.height / 8

        return Array(64) { index ->

            val rank = index / 8
            val file = index % 8

            val x =
                file * squareWidth

            val y =
                rank * squareHeight

            Bitmap.createBitmap(
                board,
                x,
                y,
                squareWidth,
                squareHeight
            )
        }
    }

    /**
     * Estimates whether the board appears flipped.
     *
     * This is only an orientation hint.
     * The user's selected WHITE/BLACK side remains
     * the authoritative orientation setting.
     */
    fun orientationHint(
        board: Bitmap
    ): BoardOrientation {

        val topLeft =
            averageBrightness(
                board,
                0,
                0,
                board.width / 4,
                board.height / 4
            )

        val bottomRight =
            averageBrightness(
                board,
                board.width * 3 / 4,
                board.height * 3 / 4,
                board.width,
                board.height
            )

        return if (
            abs(topLeft - bottomRight) < 12
        ) {
            BoardOrientation.UNKNOWN
        } else {
            BoardOrientation.NORMAL
        }
    }

    private fun boardScore(
        bitmap: Bitmap,
        region: BoardRegion,
        step: Int
    ): Double {

        var transitions = 0
        var samples = 0

        /*
         * Sample horizontal and vertical boundaries
         * expected from an 8x8 alternating board.
         */
        for (i in 1 until 8) {

            val x =
                region.left +
                    (region.width * i / 8)

            val y =
                region.top +
                    (region.height * i / 8)

            if (
                x >= 1 &&
                x < bitmap.width
            ) {

                val a =
                    averageBrightness(
                        bitmap,
                        x - 2,
                        region.top,
                        x,
                        region.bottom
                    )

                val b =
                    averageBrightness(
                        bitmap,
                        x,
                        region.top,
                        minOf(
                            x + 2,
                            bitmap.width
                        ),
                        region.bottom
                    )

                if (abs(a - b) > 4) {
                    transitions++
                }

                samples++
            }

            if (
                y >= 1 &&
                y < bitmap.height
            ) {

                val a =
                    averageBrightness(
                        bitmap,
                        region.left,
                        y - 2,
                        region.right,
                        y
                    )

                val b =
                    averageBrightness(
                        bitmap,
                        region.left,
                        y,
                        region.right,
                        minOf(
                            y + 2,
                            bitmap.height
                        )
                    )

                if (abs(a - b) > 4) {
                    transitions++
                }

                samples++
            }
        }

        if (samples == 0) {
            return 0.0
        }

        return transitions.toDouble() /
                samples.toDouble()
    }

    private fun averageBrightness(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): Double {

        val l =
            left.coerceIn(0, bitmap.width)

        val t =
            top.coerceIn(0, bitmap.height)

        val r =
            right.coerceIn(l + 1, bitmap.width)

        val b =
            bottom.coerceIn(t + 1, bitmap.height)

        var total = 0L
        var count = 0

        val sx =
            maxOf(1, (r - l) / 12)

        val sy =
            maxOf(1, (b - t) / 12)

        var y = t

        while (y < b) {

            var x = l

            while (x < r) {

                val pixel =
                    bitmap.getPixel(x, y)

                total +=
                    (
                        Color.red(pixel) +
                        Color.green(pixel) +
                        Color.blue(pixel)
                    ) / 3

                count++

                x += sx
            }

            y += sy
        }

        return if (count == 0) {
            0.0
        } else {
            total.toDouble() /
                    count.toDouble()
        }
    }
}

enum class BoardOrientation {
    NORMAL,
    FLIPPED,
    UNKNOWN
}
