package com.keshri.hax

import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import java.nio.ByteBuffer

class ScreenCaptureAnalyzer {

    companion object {
        private const val TAG =
            "ScreenCaptureAnalyzer"

        fun imageToBitmap(
            image: Image
        ): Bitmap? {

            return try {

                if (image.planes.isEmpty()) {
                    return null
                }

                val plane = image.planes[0]

                val buffer: ByteBuffer =
                    plane.buffer

                val pixelStride =
                    plane.pixelStride

                val rowStride =
                    plane.rowStride

                val width =
                    image.width

                val height =
                    image.height

                if (
                    pixelStride <= 0 ||
                    rowStride <= 0 ||
                    width <= 0 ||
                    height <= 0
                ) {
                    return null
                }

                val rowPadding =
                    rowStride -
                            pixelStride * width

                val bitmapWidth =
                    width +
                            rowPadding / pixelStride

                val bitmap =
                    Bitmap.createBitmap(
                        bitmapWidth,
                        height,
                        Bitmap.Config.ARGB_8888
                    )

                buffer.rewind()

                bitmap.copyPixelsFromBuffer(
                    buffer
                )

                if (bitmapWidth != width) {

                    val cropped =
                        Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            width,
                            height
                        )

                    bitmap.recycle()

                    cropped
                } else {
                    bitmap
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Image conversion failed",
                    e
                )

                null
            }
        }
    }
}
