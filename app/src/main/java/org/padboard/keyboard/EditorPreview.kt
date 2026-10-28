package org.padboard.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.View

/** The caret is a line at the measured text advance, never a spacing glyph. */
class EditorPreview(context: Context) : View(context) {
    private val brush=Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize=16*resources.displayMetrics.scaledDensity }
    private var before=""
    private var after=""
    var color: Int=Color.LTGRAY
    fun show(before:CharSequence?,after:CharSequence?) {
        this.before=before?.toString()?.replace('\n',' ') ?: ""
        this.after=after?.toString()?.replace('\n',' ') ?: ""
        invalidate()
    }
    fun clear()=show(null,null)
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        brush.color=color
        val advance=brush.measureText(before)
        val origin=(8*resources.displayMetrics.density).coerceAtMost(width.toFloat())
        val offset=(advance-(width-origin*2)*.75f).coerceAtLeast(0f)
        val x=origin-offset
        val baseline=height/2f-(brush.ascent()+brush.descent())/2
        canvas.save(); canvas.clipRect(0,0,width,height)
        canvas.drawText(before,x,baseline,brush)
        canvas.drawText(after,x+advance,baseline,brush)
        brush.strokeWidth=1.5f*resources.displayMetrics.density
        canvas.drawLine(x+advance,baseline+brush.ascent(),x+advance,baseline+brush.descent(),brush)
        canvas.restore()
    }
}
