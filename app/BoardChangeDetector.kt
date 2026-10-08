package com.keshri.hax

import android.graphics.Bitmap

class BoardChangeDetector {

    private var previous: Bitmap? = null

    fun changed(current: Bitmap): Boolean {

        val old = previous

        if (old == null) {
            previous = current.copy(
                current.config ?: Bitmap.Config.ARGB_8888,
                false
            )
            return true
        }

        if (old.width != current.width ||
            old.height != current.height
        ) {
            previous?.recycle()
            previous = current.copy(
                current.config ?: Bitmap.Config.ARGB_8888,
                false
            )
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
                    kotlin.math.abs(
                        android.graphics.Color.red(a) -
                        android.graphics.Color.red(b)
                    )

                samples++
                x += stepX
            }

            y += stepY
        }

        previous?.recycle()

        previous = current.copy(
            current.config ?: Bitmap.Config.ARGB_8888,
            false
        )

        return samples > 0 &&
                difference / samples > 12
    }

    fun clear() {
        previous?.recycle()
        previous = null
    }
}
