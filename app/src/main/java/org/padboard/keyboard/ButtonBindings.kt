package org.padboard.keyboard

/** Firmware can report printed labels or physical positions; presentation is configured separately. */
object ButtonBindings {
    val buttons=listOf(96,97,99,100,102,103,104,105,106,107,108,109)
    val defaults=linkedMapOf("type" to 96,"close" to 97,"delete" to 99,"space" to 100,"up" to 102,"down" to 103,"symbols" to 104,"case" to 105,"send" to 108,"language" to 109)
    fun forLayout(layout:String,convention:String="logical"):Map<String,Int> = if(layout=="nintendo" && convention=="position")
        defaults + mapOf("type" to 97,"close" to 96,"delete" to 100,"space" to 99) else defaults
    fun read(prefs:android.content.SharedPreferences):Map<String,Int> {
        val base=forLayout(prefs.getString("buttonLayout","nintendo") ?: "nintendo",prefs.getString("inputConvention","logical") ?: "logical")
        return base.mapValues { (a,c)->prefs.getString("map_$a",c.toString())?.toIntOrNull() ?: c }
    }
    fun migrate(prefs:android.content.SharedPreferences) {
        if(prefs.getString("buttonLayout",null)=="retroid") prefs.edit().putString("buttonLayout","nintendo").apply()
        if(prefs.getInt("mappingSchema",0)>=2) return
        val layout=prefs.getString("buttonLayout","nintendo")
        val previous=defaults.mapValues { (a,c)->prefs.getString("map_$a",c.toString())?.toIntOrNull() ?: c }
        val edit=prefs.edit()
        if(layout=="nintendo" && previous==defaults) defaults.keys.forEach { edit.remove("map_$it") }
        edit.putInt("mappingSchema",2).apply()
    }
    fun label(code:Int,layout:String,convention:String="logical"):String = when(code) {
        96 -> when(layout) { "nintendo"->if(convention=="position") "B" else "A"; "playstation"->"✕"; else->"A" }
        97 -> when(layout) { "nintendo"->if(convention=="position") "A" else "B"; "playstation"->"○"; else->"B" }
        99 -> when(layout) { "nintendo"->if(convention=="position") "Y" else "X"; "playstation"->"□"; else->"X" }
        100 -> when(layout) { "nintendo"->if(convention=="position") "X" else "Y"; "playstation"->"△"; else->"Y" }
        102 -> if(layout=="xbox") "LB" else "L1"
        103 -> if(layout=="xbox") "RB" else "R1"
        104 -> if(layout=="xbox") "LT" else "L2"
        105 -> if(layout=="xbox") "RT" else "R2"
        106 -> "L3"; 107 -> "R3"
        108 -> if(layout=="playstation") "≡" else "▶"
        109 -> if(layout=="playstation") "Share" else "Select"
        else -> "—"
    }
    fun assign(bindings:Map<String,Int>,action:String,code:Int):Map<String,Int> {
        val result=bindings.toMutableMap()
        val old=result[action]
        val conflict=result.entries.firstOrNull { it.key!=action && it.value==code }?.key
        result[action]=code
        if(conflict!=null && old!=null) result[conflict]=old
        return result
    }
}
