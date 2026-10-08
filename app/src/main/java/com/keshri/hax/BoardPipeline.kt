package com.keshri.hax

import android.graphics.Bitmap

class BoardPipeline(
    private val pieceRecognizer: PieceRecognizer,
    private val boardDetector: ChessVision = ChessVision()
) {
    data class Result(
        val success:Boolean,
        val fen:String?=null,
        val board:Array<CharArray>?=null,
        val confidence:Float=0f,
        val message:String=""
    )

    fun analyze(screenshot:Bitmap, sideToMove:Char='w'):Result {
        var boardBitmap:Bitmap?=null
        val squares=ArrayList<Bitmap>()
        return try {
            val region=boardDetector.findBoard(screenshot)
                ?: return Result(false,message="Chess board not detected")
            boardBitmap=boardDetector.cropBoard(screenshot,region)
            val squareArray=boardDetector.splitSquares(boardBitmap)
            if(squareArray.size!=64) return Result(false,message="Could not create 64 squares")
            squares.addAll(squareArray)
            val board=Array(8){CharArray(8){'.'}}
            var total=0f
            var n=0
            for(i in squares.indices) {
                val p=pieceRecognizer.recognize(squares[i])
                board[i/8][i%8]=if(p.confidence>=0.60f)p.piece else '.'
                if(p.confidence>0){total+=p.confidence;n++}
            }
            val conf=if(n>0) total/n else 0f
            if(!PositionValidator.isValid(board))
                return Result(false,board=board,confidence=conf,message="Position needs a clearer board/model")
            Result(true,FenBuilder.fromBoard(board,sideToMove),board,conf,"Board analyzed successfully")
        } catch(e:Exception) {
            Result(false,message=e.message ?: "Vision error")
        } finally {
            squares.forEach{try{it.recycle()}catch(_:Exception){}}
            try{boardBitmap?.recycle()}catch(_:Exception){}
        }
    }
    fun close(){pieceRecognizer.close()}
}
