package com.keshri.hax

import android.graphics.Bitmap
import kotlin.math.abs

class BoardChangeDetector {

    private var previous: Bitmap? = null

    fun changed(current: Bitmap): Boolean {

        val old = previous

        if (old == null) {

            previous = copyBitmap(current)

            return true
        }

        if (
            old.width != current.width ||
            old.height != current.height
        ) {

            previous?.recycle()

            previous = copyBitmap(current)

            return true
        }

        var difference = 0L
        var samples = 0

        val stepX = maxOf(1, current.width / 32)
        val stepY = maxOf(1, current.height / 32)

        var y = 0

        while (y < current.height) {

            var x = 0

            while (x < current.width) {

                val a = old.getPixel(x, y)
                val b = current.getPixel(x, y)

                difference +=
                    abs(
                        android.graphics.Color.red(a) -
                        android.graphics.Color.red(b)
                    )

                difference +=
                    abs(
                        android.graphics.Color.green(a) -
                        android.graphics.Color.green(b)
                    )

                difference +=
                    abs(
                        android.graphics.Color.blue(a) -
                        android.graphics.Color.blue(b)
                    )

                samples++

                x += stepX
            }

            y += stepY
        }

        previous?.recycle()

        previous = copyBitmap(current)

        if (samples == 0) {
            return false
        }

        val average = difference.toDouble() / samples

        return average > 30.0
    }

    private fun copyBitmap(source: Bitmap): Bitmap {

        return source.copy(
            source.config ?: Bitmap.Config.ARGB_8888,
            false
        )
    }

    fun clear() {

        previous?.recycle()

        previous = null
    }
}
