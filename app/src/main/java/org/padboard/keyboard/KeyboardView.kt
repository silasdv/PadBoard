package org.padboard.keyboard

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

data class Palette(val background: Int, val key: Int, val text: Int, val hint: Int, val focus: Int, val pressed: Int) {
    companion object {
        val ids = listOf("light","dark","oled","color","ocean","forest","rose","amber","crimson","slate")
        val names = listOf("Light","Dark","OLED","Purple","Ocean","Forest","Rose","Amber","Crimson","Slate")
        private fun colors(bg:String,key:String,text:String,hint:String,accent:String,pressed:String) =
            Palette(Color.parseColor(bg),Color.parseColor(key),Color.parseColor(text),Color.parseColor(hint),Color.parseColor(accent),Color.parseColor(pressed))
        fun named(name: String) = when (name) {
            "dark" -> colors("#151922","#293140","#F2F5FA","#B1BED2","#85BCFF","#3D526B")
            "ocean" -> colors("#071E30","#103B54","#ECF8FF","#A0D4E9","#64E2EB","#205D79")
            "forest" -> colors("#101F19","#203D2D","#F0FFF3","#B3D5BD","#95ECA9","#365A43")
            "rose" -> colors("#2B1424","#4B2941","#FFF0F6","#E4B7CE","#FFADD4","#70415F")
            "amber" -> colors("#271D0E","#47361C","#FFF6DF","#DCC69A","#FFD078","#6D5226")
            "crimson" -> colors("#281218","#48252F","#FFF1F2","#E0AFB8","#FF9AAB","#6C3D49")
            "slate" -> colors("#182127","#303F48","#F0F6FA","#B7C9D5","#A3D7EC","#4C626D")
            "light" -> Palette(Color.rgb(225,230,237), Color.WHITE, Color.rgb(25,33,45), Color.rgb(77,100,129), Color.rgb(0,119,160), Color.rgb(176,221,240))
            "color" -> Palette(Color.rgb(29,19,57), Color.rgb(58,40,92), Color.WHITE, Color.rgb(183,165,230), Color.rgb(76,222,197), Color.rgb(119,68,170))
            else -> Palette(Color.BLACK, Color.rgb(21,21,24), Color.WHITE, Color.rgb(151,163,180), Color.rgb(84,211,190), Color.rgb(63,77,85))
        }
    }
}

/** Touch press and controller selection are deliberately separate visual states. */
class KeyboardView(context: Context, private val state: KeyboardState, private val invoke: (PadKey) -> Unit) : View(context) {
    var palette = Palette.named("oled")
    var controllerFocus = false
    var sendLabel = ""
    var buttonLayout = "nintendo"
    var bindings: Map<String,Int> = ButtonBindings.defaults
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var pressed: String? = null
    private var pointer = -1
    init { isClickable = true; isFocusable = false; contentDescription = "PadBoard" }
    private fun rectangles(): List<Pair<KeyPlacement, RectF>> {
        val unit = width.toFloat() / state.totalWidth()
        val rowHeight = height / state.keys().size.toFloat()
        val gap = min(2 * resources.displayMetrics.density, min(unit, rowHeight) * .035f)
        return state.placements().map { it to RectF(it.x * unit + gap, it.row * rowHeight + gap, (it.x + it.key.width) * unit - gap, (it.row + 1) * rowHeight - gap) }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); canvas.drawColor(palette.background)
        for ((place, rect) in rectangles()) {
            val key = place.key
            if(key.action=="none") continue
            val selected = controllerFocus && place.row == state.row && place.column == state.column
            paint.style = Paint.Style.FILL
            paint.color = if (pressed == key.id) palette.pressed else if (key.id == "case" && state.uppercase) palette.focus else palette.key
            canvas.drawRoundRect(rect, 9f, 9f, paint)
            if (selected) { paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f; paint.color = palette.focus; canvas.drawRoundRect(rect, 9f, 9f, paint); paint.style = Paint.Style.FILL }
            paint.color = if (key.id == "case" && state.uppercase) palette.background else palette.text; paint.textAlign = Paint.Align.CENTER
            paint.textSize = min(rect.height() * .65f, rect.width() * (if (key.label.length > 2) .32f else .78f)).coerceAtMost(34 * resources.displayMetrics.scaledDensity)
            val y = rect.centerY() - (paint.ascent() + paint.descent()) / 2
            when (key.id) {
                "case" -> shift(canvas, rect)
                "language" -> {
                    globe(canvas, rect.left + rect.width() * .29f, rect.centerY(), min(rect.width(), rect.height()) * .16f)
                    paint.color = palette.hint
                    paint.textSize = min(rect.height() * .28f, rect.width() * .25f)
                    canvas.drawText(state.language.uppercase(java.util.Locale.ROOT), rect.left + rect.width() * .72f, rect.centerY()-(paint.ascent()+paint.descent())/2, paint)
                }
                "space" -> {
                    val half = min(rect.width() * .17f, rect.height() * .48f)
                    val ySpace = rect.centerY() + rect.height() * .09f
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f
                    canvas.drawPath(Path().apply { moveTo(rect.centerX()-half,ySpace-rect.height()*.16f); lineTo(rect.centerX()-half,ySpace); lineTo(rect.centerX()+half,ySpace); lineTo(rect.centerX()+half,ySpace-rect.height()*.16f) },paint)
                    paint.style = Paint.Style.FILL
                }
                "send" -> {
                    if (sendLabel == "search") {
                        val r = min(rect.width(),rect.height())*.19f; val x=rect.centerX()-r*.3f; val y=rect.centerY()-r*.3f
                        paint.style=Paint.Style.STROKE; paint.strokeWidth=3f
                        canvas.drawCircle(x,y,r,paint); canvas.drawLine(x+r*.7f,y+r*.7f,x+r*1.6f,y+r*1.6f,paint)
                        paint.style=Paint.Style.FILL
                    } else {
                    val label = when(sendLabel) { "Next", "Nächstes" -> "⇥"; "Go" -> "→"; "Send", "Senden" -> "➤"; "Done", "Fertig" -> "✓"; else -> "↵" }
                    paint.textSize = min(paint.textSize, rect.width() / (label.length * .65f + 1))
                    canvas.drawText(label, rect.centerX(), rect.centerY()-(paint.ascent()+paint.descent())/2, paint)
                    }
                }
                else -> canvas.drawText(key.label, rect.centerX(), y, paint)
            }

            if (key.badge.isNotEmpty()) {
                bindings[key.action]?.let { code -> badge(canvas,rect,ButtonBindings.label(code,buttonLayout),code) }
            }
        }
    }
    private fun shift(c: Canvas, rect: RectF) {
        val r = min(rect.width(),rect.height()) * .26f
        val x=rect.centerX(); val y=rect.centerY()
        paint.style=Paint.Style.FILL
        c.drawPath(Path().apply {
            moveTo(x,y-r); lineTo(x+r,y); lineTo(x+r*.45f,y)
            lineTo(x+r*.45f,y+r); lineTo(x-r*.45f,y+r)
            lineTo(x-r*.45f,y); lineTo(x-r,y); close()
        },paint)
    }
    private fun globe(c: Canvas, x: Float, y: Float, r: Float) {
        paint.color = palette.text; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
        c.drawCircle(x,y,r,paint); c.drawOval(RectF(x-r*.45f,y-r,x+r*.45f,y+r),paint); c.drawLine(x-r,y,x+r,y,paint); paint.style = Paint.Style.FILL
    }
    private fun badge(c: Canvas, rect: RectF, text: String, code:Int) {
        val size = min(rect.height() * .23f, rect.width() * .24f).coerceAtMost(14 * resources.displayMetrics.density)
        val x = rect.right - size * .8f; val y = rect.top + size * .8f
        paint.color = palette.hint; paint.strokeWidth = 1.4f; paint.style = Paint.Style.STROKE
        if (code in listOf(96,97,99,100)) c.drawCircle(x,y,size*.64f,paint)
        else c.drawRoundRect(RectF(x-size, y-size*.58f,x+size*.5f,y+size*.58f),2f,2f,paint)
        paint.style = Paint.Style.FILL
        if (text == "▶") {
            c.drawPath(Path().apply { moveTo(x-size*.45f,y-size*.55f); lineTo(x+size*.45f,y); lineTo(x-size*.45f,y+size*.55f); close() },paint)
        } else {
            paint.textSize = size * .85f; paint.textAlign = Paint.Align.CENTER
            c.drawText(text, if (text.length > 1) x-size*.25f else x, y-(paint.ascent()+paint.descent())/2,paint)
        }
    }
    private fun hit(x: Float, y: Float) = rectangles().firstOrNull { it.first.key.action!="none" && it.second.contains(x,y) }?.first
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { controllerFocus = false; pointer = event.getPointerId(0); pressed = hit(event.x,event.y)?.key?.id; invalidate(); return true }
            MotionEvent.ACTION_MOVE -> { val i=event.findPointerIndex(pointer); if(i < 0 || hit(event.getX(i),event.getY(i))?.key?.id != pressed) pressed=null; invalidate(); return true }
            MotionEvent.ACTION_UP -> {
                val key = hit(event.x,event.y)?.key?.takeIf { it.id == pressed }
                pressed=null; pointer=-1; invalidate()
                if (key != null) { performClick(); invoke(key) }; return true
            }
            MotionEvent.ACTION_CANCEL -> { pressed=null; pointer=-1; invalidate(); return true }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}





