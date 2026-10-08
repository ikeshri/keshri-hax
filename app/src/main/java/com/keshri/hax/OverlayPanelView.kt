package com.keshri.hax

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class OverlayPanelView(
    context: Context,
    private val onStop: () -> Unit,
    private val onRescan: () -> Unit,
    private val onPause: () -> Unit
) : View(context) {

    data class State(
        var status: String = "STARTING",
        var bestMove: String = "—",
        var evaluation: String = "—",
        var depth: String = "—",
        var confidence: String = "—",
        var fen: String = "Waiting for board…",
        var detail: String = "Preparing screen capture"
    )

    var state = State()
        set(value) { field = value; postInvalidate() }

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG)
    private var downX = 0f
    private var downY = 0f
    private var startX = 0f
    private var startY = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        text.typeface = Typeface.create("sans", Typeface.NORMAL)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()

        bg.color = Color.argb(245, 7, 11, 18)
        c.drawRoundRect(0f,0f,w,h,28f,28f,bg)

        bg.color = Color.rgb(0,255,157)
        c.drawRoundRect(0f,0f,w,7f,5f,5f,bg)

        text.color = Color.rgb(0,255,157)
        text.textSize = 18f
        text.typeface = Typeface.DEFAULT_BOLD
        c.drawText("♚  KESHRI HAX", 22f, 36f, text)

        text.color = Color.LTGRAY
        text.textSize = 11f
        text.typeface = Typeface.DEFAULT
        c.drawText("OFFLINE ANALYSIS PANEL", 23f, 56f, text)

        text.color = Color.WHITE
        text.textSize = 13f
        c.drawText("●  ${state.status}", 22f, 84f, text)

        card(c, 16f, 102f, w-16f, 175f)
        label(c,"BEST MOVE",28f,125f)
        text.color=Color.WHITE; text.textSize=27f; text.typeface=Typeface.DEFAULT_BOLD
        c.drawText(state.bestMove,28f,158f,text)
        text.color=Color.GRAY; text.textSize=10f; text.typeface=Typeface.DEFAULT
        c.drawText("ENGINE EVALUATION",28f,183f,text)
        text.color=Color.rgb(0,255,157); text.textSize=20f
        c.drawText(state.evaluation,28f,207f,text)
        text.color=Color.GRAY; text.textSize=10f
        c.drawText("DEPTH  ${state.depth}     CONFIDENCE  ${state.confidence}",28f,230f,text)

        card(c,16f,287f,w-16f,352f)
        label(c,"VISION STATUS",28f,309f)
        text.color=Color.LTGRAY;text.textSize=11f
        c.drawText(state.detail,28f,333f,text)

        text.color=Color.GRAY;text.textSize=9f
        c.drawText("FEN",28f,373f,text)
        text.color=Color.WHITE;text.textSize=9f
        val fenLine = if(state.fen.length>70) state.fen.take(70)+"…" else state.fen
        c.drawText(fenLine,28f,390f,text)

        button(c,16f,410f,w/2-22f,458f,"PAUSE")
        button(c,w/2+6f,410f,w-16f,458f,"RESCAN")
        button(c,16f,468f,w-16f,516f,"STOP ANALYSIS")
    }

    private fun card(c:Canvas,l:Float,t:Float,r:Float,b:Float) {
        bg.color=Color.argb(120,20,30,42)
        c.drawRoundRect(l,t,r,b,20f,20f,bg)
        bg.style=Paint.Style.STROKE; bg.strokeWidth=1f; bg.color=Color.rgb(40,65,78)
        c.drawRoundRect(l,t,r,b,20f,20f,bg); bg.style=Paint.Style.FILL
    }
    private fun label(c:Canvas,s:String,x:Float,y:Float) {
        text.color=Color.GRAY;text.textSize=9f;text.typeface=Typeface.DEFAULT_BOLD
        c.drawText(s,x,y,text)
    }
    private fun button(c:Canvas,l:Float,t:Float,r:Float,b:Float,s:String) {
        bg.color=Color.argb(70,0,255,157)
        c.drawRoundRect(l,t,r,b,16f,16f,bg)
        text.color=Color.WHITE;text.textSize=11f;text.typeface=Typeface.DEFAULT_BOLD
        c.drawText(s,l+16f,(t+b)/2f+4f,text)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX=e.rawX; downY=e.rawY; startX=e.rawX; startY=e.rawY; return true }
            MotionEvent.ACTION_MOVE -> {
                val dx=e.rawX-downX; val dy=e.rawY-downY
                offsetX += dx; offsetY += dy
                translationX=offsetX; translationY=offsetY
                downX=e.rawX; downY=e.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                val x=e.x; val y=e.y
                if (kotlin.math.abs(e.rawX-startX)<20 && kotlin.math.abs(e.rawY-startY)<20) {
                    when {
                        y in 410f..458f && x <= width/2 -> onPause()
                        y in 410f..458f && x > width/2 -> onRescan()
                        y in 468f..516f -> onStop()
                    }
                }
                return true
            }
        }
        return true
    }
}
