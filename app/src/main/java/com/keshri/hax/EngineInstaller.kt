package com.keshri.hax

import android.content.Context
import java.io.File

object EngineInstaller {

    private const val ENGINE_NAME = "stockfish"

    fun install(context: Context): File {

        val destination = File(
            context.filesDir,
            ENGINE_NAME
        )

        if (!destination.exists()) {

            context.assets.open(
                ENGINE_NAME
            ).use { input ->

                destination.outputStream().use { output ->

                    input.copyTo(output)
                }
            }
        }

        if (!destination.setExecutable(true, false)) {
            throw IllegalStateException(
                "Could not make Stockfish executable"
            )
        }

        return destination
    }
}
