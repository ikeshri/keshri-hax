package com.keshri.hax

object PositionValidator {

    fun isValid(board: Array<CharArray>): Boolean {

        if (board.size != 8) {
            return false
        }

        for (rank in board) {

            if (rank.size != 8) {
                return false
            }

            for (piece in rank) {

                if (
                    piece != '.' &&
                    piece !in "KQRBNPkqrbnp"
                ) {
                    return false
                }
            }
        }

        var whiteKing = 0
        var blackKing = 0

        for (rank in board) {
            for (piece in rank) {

                when (piece) {
                    'K' -> whiteKing++
                    'k' -> blackKing++
                }
            }
        }

        /*
         * A recognizable chess position must contain
         * exactly one king for each side.
         */
        return whiteKing == 1 &&
                blackKing == 1
    }

    fun emptyBoard(): Array<CharArray> {

        return Array(8) {
            CharArray(8) { '.' }
        }
    }
}
