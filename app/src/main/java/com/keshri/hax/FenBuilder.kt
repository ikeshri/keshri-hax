package com.keshri.hax

object FenBuilder {
    fun fromBoard(board: Array<CharArray>, sideToMove: Char): String {
        val rows = board.joinToString("/") { row ->
            val out = StringBuilder()
            var empty = 0
            for (p in row) {
                if (p == '.') empty++ else {
                    if (empty > 0) { out.append(empty); empty = 0 }
                    out.append(p)
                }
            }
            if (empty > 0) out.append(empty)
            out.toString()
        }
        return "$rows $sideToMove - - 0 1"
    }
}
