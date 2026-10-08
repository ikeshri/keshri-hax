package com.keshri.hax

import android.content.Context
import java.io.File

object EngineInstaller {
    fun install(context: Context): File? {
        val out = File(context.filesDir, "stockfish")
        if (out.exists() && out.length() > 0) return out
        return try {
            context.assets.open("stockfish").use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            out.setExecutable(true, false)
            out
        } catch (_: Exception) {
            null
        }
    }
}
