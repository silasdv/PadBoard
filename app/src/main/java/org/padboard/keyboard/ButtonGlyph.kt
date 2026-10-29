package org.padboard.keyboard

import android.graphics.*
import android.graphics.drawable.Drawable

/** Font-independent controller symbols, normalized to the same square bounds. */
class ButtonGlyph(private val symbol:String,private val tint:Int):Drawable() {
    override fun draw(canvas:Canvas) { draw(canvas,symbol,bounds.exactCenterX(),bounds.exactCenterY(),bounds.width()*.4f,Paint(Paint.ANTI_ALIAS_FLAG).apply { color=tint }) }
    override fun setAlpha(alpha:Int) {}
    override fun setColorFilter(filter:ColorFilter?) {}
    @Deprecated("Drawable opacity") override fun getOpacity()=PixelFormat.TRANSLUCENT
    companion object {
        val names=mapOf("✕" to "Cross","○" to "Circle","□" to "Square","△" to "Triangle")
        fun draw(c:Canvas,symbol:String,x:Float,y:Float,r:Float,source:Paint) {
            val p=Paint(source).apply { style=Paint.Style.STROKE; strokeWidth=r*.22f; strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND }
            when(symbol) {
                "✕" -> { c.drawLine(x-r,y-r,x+r,y+r,p); c.drawLine(x-r,y+r,x+r,y-r,p) }
                "○" -> c.drawCircle(x,y,r,p)
                "□" -> c.drawRect(x-r,y-r,x+r,y+r,p)
                "△" -> c.drawPath(Path().apply { moveTo(x,y-r); lineTo(x+r,y+r); lineTo(x-r,y+r); close() },p)
            }
        }
    }
}
