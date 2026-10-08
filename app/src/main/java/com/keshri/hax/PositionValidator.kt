package com.keshri.hax

object PositionValidator {
    fun isValid(board: Array<CharArray>): Boolean {
        if (board.size != 8 || board.any { it.size != 8 }) return false
        var whiteKing = 0
        var blackKing = 0
        for (row in board) for (p in row) {
            if (p !in ".PNBRQKpnbrqk") return false
            if (p == 'K') whiteKing++
            if (p == 'k') blackKing++
        }
        return whiteKing == 1 && blackKing == 1
    }
}
