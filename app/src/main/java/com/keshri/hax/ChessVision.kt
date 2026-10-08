package com.keshri.hax

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ChessVision {

    data class Rect(val left:Int,val top:Int,val right:Int,val bottom:Int)

    fun findBoard(bitmap: Bitmap): Rect? {
        val w=bitmap.width; val h=bitmap.height
        if(w<200 || h<200) return null

        // Conservative heuristic: find the largest near-square region with
        // alternating light/dark chess-like pixel blocks.
        val maxSide=min(w,h)
        val candidates=listOf(
            Rect((w-maxSide)/2, (h-maxSide)/2, (w+maxSide)/2, (h+maxSide)/2),
            Rect(0, (h-maxSide)/2, maxSide, (h+maxSide)/2),
            Rect(w-maxSide, (h-maxSide)/2, w, (h+maxSide)/2)
        )
        return candidates.maxByOrNull { score(bitmap,it) }?.takeIf { score(bitmap,it) > 0.05 }
    }

    private fun score(b:Bitmap,r:Rect):Double {
        val side=min(r.right-r.left,r.bottom-r.top)
        if(side<160) return 0.0
        var contrast=0.0
        var count=0
        for(row in 0 until 8) for(col in 0 until 8) {
            val x=r.left + ((col+0.5)*side/8).toInt().coerceIn(0,b.width-1)
            val y=r.top + ((row+0.5)*side/8).toInt().coerceIn(0,b.height-1)
            val c=b.getPixel(x,y)
            val lum=(Color.red(c)+Color.green(c)+Color.blue(c))/3
            if(row+col<2) contrast+=lum
            count++
        }
        return abs((contrast/count)-128.0)/128.0
    }

    fun cropBoard(bitmap:Bitmap,r:Rect):Bitmap {
        val left=r.left.coerceIn(0,bitmap.width-1)
        val top=r.top.coerceIn(0,bitmap.height-1)
        val right=r.right.coerceIn(left+1,bitmap.width)
        val bottom=r.bottom.coerceIn(top+1,bitmap.height)
        return Bitmap.createBitmap(bitmap,left,top,right-left,bottom-top)
    }

    fun splitSquares(board:Bitmap):List<Bitmap> {
        val side=min(board.width,board.height)
        val x0=(board.width-side)/2
        val y0=(board.height-side)/2
        val size=side/8
        val out=ArrayList<Bitmap>(64)
        for(r in 0 until 8) for(c in 0 until 8) {
            out.add(Bitmap.createBitmap(board,x0+c*size,y0+r*size,size,size))
        }
        return out
    }
}
