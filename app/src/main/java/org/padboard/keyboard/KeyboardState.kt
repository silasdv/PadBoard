package org.padboard.keyboard

import java.util.Locale
import kotlin.math.abs

data class PadKey(val id: String, val label: String, val action: String = "text", val width: Float = 1f, val badge: String = "")
data class KeyPlacement(val key: PadKey, val row: Int, val column: Int, val x: Float)

/** Layout units are shared by drawing, hit testing and controller navigation. */
class KeyboardState(var language: String = "de") {
    var email = false
    var numberRow = false
    var symbols = false
    var uppercase = false
    var row = 0
    var column = 0
    private var caseOverride: Boolean? = null
    private var wasEmpty: Boolean? = null
    fun keys(): List<List<PadKey>> {
        fun letters(text: String) = text.map { c -> PadKey(c.toString(), if (uppercase && !symbols && c != 'ß') c.toString().uppercase(Locale.ROOT) else c.toString()) }
        val lines = if (symbols && numberRow) listOf("@#€$%&*+-=", "!?;:()[]/_", "<>~`^{}\\|°")
            else if (symbols) listOf("1234567890", "@#€$%&*+-=", "!?;:()[]/_")
            else TypingLanguages.get(language).rows
        val accents=TypingLanguages.get(language).accents
        val extras = (if(accents.isNotEmpty()) listOf(letters(accents).map { it.copy(width=12.75f/accents.length) }) else emptyList()) + (if (email) listOf(listOf(PadKey("@","@",width=3.1875f),PadKey(".com",".com",width=3.1875f),PadKey(".de",".de",width=3.1875f),PadKey(".net",".net",width=3.1875f))) else emptyList()) +
            (if (numberRow) listOf(letters("1234567890").map { it.copy(width=1.275f) }) else emptyList())
        return extras + listOf(
            letters(lines[0]) + PadKey("delete","⌫","delete",1.25f,"X"),
            letters(lines[1]),
            listOf(PadKey("case","⇧","case",1.25f,"R2")) + letters(lines[2]) +
                (if (!symbols) listOf(PadKey("?","?",width=1f),PadKey("!","!",width=1f)) else emptyList()) +
                PadKey("rowTail","","none",10.5f-lines[2].length-(if(symbols) 0f else 2f)),
            listOf(PadKey("symbols",if(symbols) "ABC" else "?123","symbols",1.5f,"L2"),PadKey("language","◎","language",1.25f),PadKey("settings","⚙","settings",1f),
                PadKey(",",","),PadKey("space","␣","space",4.75f,"Y"),PadKey(".",".")) +
                (if(!symbols) listOf(PadKey("send","↵","send",2.25f,"▶")) else listOf(PadKey("reservedAction","","none",2.25f)))
        )
    }
    fun extraRows() = (if(TypingLanguages.get(language).accents.isNotEmpty()) 1 else 0) + (if(email) 1 else 0) + (if(numberRow) 1 else 0)
    fun rows() = keys().map { line -> line.map { it.label } }
    fun placements(): List<KeyPlacement> = keys().flatMapIndexed { r, line ->
        val baseRow=r-extraRows()
        val rowWidth=line.sumOf { it.width.toDouble() }.toFloat()
        var x = if(baseRow==2) 0.5f else (totalWidth()-rowWidth)/2
        line.mapIndexed { c, key -> KeyPlacement(key, r, c, x).also { x += key.width } }
    }
    fun totalWidth() = maxOf(12.75f,keys().maxOf { line -> line.sumOf { it.width.toDouble() }.toFloat() })
    fun selectedKey() = keys()[row][column]
    fun selected() = selectedKey().label
    fun move(dx: Int, dy: Int) {
        val current = placements().first { it.row == row && it.column == column }
        val center = current.x + current.key.width / 2
        row = (row + dy).coerceIn(0, keys().lastIndex)
        column = if (dy != 0) placements().filter { it.row == row && it.key.action!="none" }.minBy { abs(it.x + it.key.width / 2 - center) }.column
            else (column + dx).coerceIn(0, keys()[row].lastIndex)
        if(selectedKey().action=="none") column=(column-1).coerceAtLeast(0)
    }
    fun toggleSymbols() { symbols = !symbols; clamp() }
    fun toggleCase() { uppercase = !uppercase; caseOverride = uppercase }
    fun syncEmpty(empty: Boolean, allowed: Boolean) {
        if (wasEmpty != empty) caseOverride = null
        wasEmpty = empty
        uppercase = caseOverride ?: (empty && allowed)
    }
    fun resetCase() { caseOverride = null; wasEmpty = null; uppercase = false }
    fun changeLanguage(value: String) { language = value; clamp() }
    fun clamp() {
        row = row.coerceIn(0, keys().lastIndex); column = column.coerceIn(0, keys()[row].lastIndex)
        if(selectedKey().action=="none") column=(column-1).coerceAtLeast(0)
    }
}
class PressGate {
    private val sources = mutableMapOf<String, MutableSet<String>>()
    fun update(action: String, source: String, down: Boolean): Boolean {
        val active = sources.getOrPut(action) { mutableSetOf() }
        val wasDown = active.isNotEmpty()
        if (down) active.add(source) else active.remove(source)
        return down && !wasDown && active.isNotEmpty()
    }
    fun clear() = sources.clear()
}





