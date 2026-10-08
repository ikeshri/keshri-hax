package com.keshri.hax

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit

class ChessEngine(private val context: Context) {

    data class Result(
        val bestMove: String?,
        val evaluation: String?,
        val depth: Int?,
        val pv: String?
    )

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null

    fun start(): Boolean {
        return try {
            val file = EngineInstaller.install(context) ?: return false
            process = ProcessBuilder(file.absolutePath)
                .redirectErrorStream(true)
                .start()
            writer = OutputStreamWriter(process!!.outputStream)
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            send("uci")
            waitFor("uciok", 5000)
            send("isready")
            waitFor("readyok", 5000)
            true
        } catch (_: Exception) {
            close()
            false
        }
    }

    fun analyze(fen: String, depth: Int): Result {
        send("position fen $fen")
        send("go depth $depth")

        var best: String? = null
        var eval: String? = null
        var foundDepth: Int? = null
        var pv: String? = null
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)

        while (System.nanoTime() < end) {
            val line = reader?.readLine() ?: break
            if (line.startsWith("info ") && line.contains(" score ")) {
                val d = Regex("\bdepth (\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull()
                val cp = Regex("\bscore cp (-?\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull()
                val mate = Regex("\bscore mate (-?\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull()
                if (d != null) foundDepth = d
                eval = when {
                    mate != null -> "M$mate"
                    cp != null -> String.format("%+.2f", cp / 100.0)
                    else -> eval
                }
                pv = line.substringAfter(" pv ", pv ?: "")
            }
            if (line.startsWith("bestmove ")) {
                best = line.substringAfter("bestmove ").trim().split(" ").firstOrNull()
                break
            }
        }
        return Result(best, eval, foundDepth, pv)
    }

    private fun send(command: String) {
        writer?.apply { write(command + "\n"); flush() }
    }

    private fun waitFor(token: String, timeoutMs: Long) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            val line = reader?.readLine() ?: break
            if (line.contains(token)) return
        }
    }

    fun close() {
        try { send("quit") } catch (_: Exception) {}
        try { process?.destroy() } catch (_: Exception) {}
        process = null; writer = null; reader = null
    }
}
