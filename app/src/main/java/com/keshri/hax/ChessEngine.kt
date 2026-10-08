package com.keshri.hax

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit

class ChessEngine(
    private val context: Context
) {

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null

    @Synchronized
    fun start(): Boolean {

        if (process?.isAlive == true) {
            return true
        }

        return try {

            val engineFile =
                EngineInstaller.install(context)

            process = ProcessBuilder(
                engineFile.absolutePath
            )
                .redirectErrorStream(true)
                .start()

            writer = OutputStreamWriter(
                process!!.outputStream
            )

            reader = BufferedReader(
                InputStreamReader(
                    process!!.inputStream
                )
            )

            send("uci")

            if (!waitFor("uciok", 5000)) {
                stop()
                return false
            }

            send("setoption name Threads value 2")
            send("setoption name Hash value 64")
            send("setoption name Ponder value false")

            send("isready")

            if (!waitFor("readyok", 5000)) {
                stop()
                return false
            }

            true

        } catch (_: Exception) {

            stop()
            false
        }
    }

    @Synchronized
    fun analyze(
        fen: String,
        depth: Int = 18,
        timeoutMs: Long = 12000
    ): EngineResult {

        if (process?.isAlive != true) {

            if (!start()) {

                return EngineResult(
                    bestMove = null,
                    evaluation = null,
                    depth = 0,
                    pv = emptyList(),
                    error = "Stockfish could not start"
                )
            }
        }

        return try {

            send("stop")
            send("position fen $fen")
            send("go depth $depth")

            var bestMove: String? = null
            var evaluation: String? = null
            var actualDepth = 0

            val principalVariation =
                mutableListOf<String>()

            val endTime =
                System.currentTimeMillis() + timeoutMs

            while (
                System.currentTimeMillis() < endTime
            ) {

                val line = reader?.readLine()
                    ?: break

                if (line.startsWith("info ")) {

                    Regex("""\bdepth\s+(\d+)""")
                        .find(line)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.toIntOrNull()
                        ?.let {
                            actualDepth = it
                        }

                    val score =
                        Regex(
                            """\bscore\s+(cp|mate)\s+(-?\d+)"""
                        ).find(line)

                    if (score != null) {

                        val type =
                            score.groupValues[1]

                        val value =
                            score.groupValues[2]
                                .toIntOrNull() ?: 0

                        evaluation =
                            if (type == "mate") {
                                "M$value"
                            } else {
                                String.format(
                                    "%+.2f",
                                    value / 100.0
                                )
                            }
                    }

                    val pvPosition =
                        line.indexOf(" pv ")

                    if (pvPosition >= 0) {

                        principalVariation.clear()

                        line.substring(
                            pvPosition + 4
                        )
                            .trim()
                            .split("\\s+".toRegex())
                            .take(10)
                            .forEach { move ->

                                if (move.isNotBlank()) {
                                    principalVariation.add(move)
                                }
                            }
                    }
                }

                if (line.startsWith("bestmove ")) {

                    bestMove = line
                        .removePrefix("bestmove ")
                        .trim()
                        .split("\\s+".toRegex())
                        .firstOrNull()

                    break
                }
            }

            if (bestMove == null) {
                send("stop")
            }

            EngineResult(
                bestMove = bestMove,
                evaluation = evaluation,
                depth = actualDepth,
                pv = principalVariation.toList(),
                error = null
            )

        } catch (e: Exception) {

            EngineResult(
                bestMove = null,
                evaluation = null,
                depth = 0,
                pv = emptyList(),
                error = e.message ?: "Engine error"
            )
        }
    }

    private fun send(command: String) {

        writer?.let {

            it.write(command)
            it.write("\n")
            it.flush()
        }
    }

    private fun waitFor(
        expected: String,
        timeoutMs: Long
    ): Boolean {

        val start =
            System.currentTimeMillis()

        while (
            System.currentTimeMillis() - start < timeoutMs
        ) {

            val line =
                reader?.readLine()
                    ?: return false

            if (line.contains(expected)) {
                return true
            }
        }

        return false
    }

    @Synchronized
    fun stop() {

        try {
            send("quit")
        } catch (_: Exception) {
        }

        try {
            writer?.close()
        } catch (_: Exception) {
        }

        try {
            reader?.close()
        } catch (_: Exception) {
        }

        try {
            process?.destroy()

            process?.waitFor(
                500,
                TimeUnit.MILLISECONDS
            )

        } catch (_: Exception) {
        }

        process = null
        writer = null
        reader = null
    }
}

data class EngineResult(
    val bestMove: String?,
    val evaluation: String?,
    val depth: Int,
    val pv: List<String>,
    val error: String?
)
