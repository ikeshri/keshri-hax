package com.keshri.hax

object FenBuilder {

    fun fromBoard(
        board: Array<CharArray>,
        sideToMove: Char
    ): String {

        val fen = StringBuilder()

        for (rank in 0 until 8) {

            var empty = 0

            for (file in 0 until 8) {

                val piece = board[rank][file]

                if (piece == '.') {
                    empty++
                } else {

                    if (empty > 0) {
                        fen.append(empty)
                        empty = 0
                    }

                    fen.append(piece)
                }
            }

            if (empty > 0) {
                fen.append(empty)
            }

            if (rank != 7) {
                fen.append("/")
            }
        }

        fen.append(" ")
        fen.append(sideToMove)
        fen.append(" - - 0 1")

        return fen.toString()
    }
}
