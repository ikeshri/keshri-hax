package com.keshri.hax

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.util.Log
import java.nio.ByteBuffer

/**
 * Converts MediaProjection/ImageReader frames into Bitmap frames.
 *
 * This class intentionally does not start MediaProjection itself.
 * LiveOverlayService remains responsible for the projection lifecycle.
 */
class ScreenCaptureAnalyzer {

    companion object {
        private const val TAG = "ScreenCaptureAnalyzer"

        /**
         * Converts an ImageReader Image (RGBA_8888) to a Bitmap.
         *
         * Returns null when the frame cannot be converted safely.
         */
        fun imageToBitmap(image: Image): Bitmap? {
            return try {
                if (image.planes.isEmpty()) {
                    return null
                }

                val plane = image.planes[0]
                val buffer: ByteBuffer = plane.buffer

                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride

                if (pixelStride <= 0 || rowStride <= 0) {
                    return null
                }

                val width = image.width
                val height = image.height

                val rowPadding = rowStride - pixelStride * width

                val bitmapWidth = width +
                        (rowPadding / pixelStride)

                val bitmap = Bitmap.createBitmap(
                    bitmapWidth,
                    height,
                    Bitmap.Config.ARGB_8888
                )

                buffer.rewind()

                bitmap.copyPixelsFromBuffer(buffer)

                if (bitmapWidth != width) {
                    Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        width,
                        height
                    ).also {
                        if (it !== bitmap) {
                            bitmap.recycle()
                        }
                        return it
                    }
                }

                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Failed to convert Image to Bitmap", e)
                null
            }
        }

        /**
         * Optional helper for encoded image bytes.
         */
        fun decode(bytes: ByteArray): Bitmap? {
            return try {
                BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.size
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode image", e)
                null
            }
        }
    }

    /**
     * Safely releases a frame after processing.
     */
    fun closeFrame(image: Image?) {
        try {
            image?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Could not close frame", e)
        }
    }
}
