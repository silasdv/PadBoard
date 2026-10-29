package org.padboard.keyboard

/** Firmware can report printed labels or physical positions; presentation is configured separately. */
object ButtonBindings {
    val buttons=listOf(96,97,99,100,102,103,104,105,106,107,108,109)
    val defaults=linkedMapOf("type" to 96,"close" to 97,"delete" to 99,"space" to 100,"up" to 102,"down" to 103,"symbols" to 104,"case" to 105,"send" to 108,"language" to 109)
    fun forLayout(layout:String,convention:String="logical"):Map<String,Int> = if(convention=="position")
        defaults + mapOf("type" to 97,"close" to 96,"delete" to 100,"space" to 99) else defaults
    fun read(prefs:android.content.SharedPreferences):Map<String,Int> {
        val base=forLayout(prefs.getString("buttonLayout","nintendo") ?: "nintendo",prefs.getString("inputConvention","logical") ?: "logical")
        return base.mapValues { (a,c)->prefs.getString("map_$a",c.toString())?.toIntOrNull() ?: c }
    }
    fun migrate(prefs:android.content.SharedPreferences) {
        if(prefs.getString("buttonLayout",null)=="retroid") prefs.edit().putString("buttonLayout","nintendo").apply()
        if(prefs.getInt("profileSchema",0)<1) {
            val edit=prefs.edit().putInt("profileSchema",1)
            if(prefs.getString("buttonLayout","nintendo")!="nintendo") edit.putString("inputConvention","logical")
            edit.apply()
        }
        if(prefs.getInt("mappingSchema",0)>=2) return
        val layout=prefs.getString("buttonLayout","nintendo")
        val previous=defaults.mapValues { (a,c)->prefs.getString("map_$a",c.toString())?.toIntOrNull() ?: c }
        val edit=prefs.edit()
        if(layout=="nintendo" && previous==defaults) defaults.keys.forEach { edit.remove("map_$it") }
        edit.putInt("mappingSchema",2).apply()
    }
    private fun swapped(code:Int):Int = when(code) { 96->97; 97->96; 99->100; 100->99; else->code }
    fun selectProfile(prefs:android.content.SharedPreferences,profile:String) {
        val layout=profile.removeSuffix("_swapped")
        val mode=if(profile.endsWith("_swapped")) "position" else "logical"
        val changed=mode!=(prefs.getString("inputConvention","logical") ?: "logical")
        val edit=prefs.edit().putString("buttonLayout",layout).putString("inputConvention",mode)
        // Keep custom actions attached to the same printed button when changing the reporting preset.
        if(changed) defaults.keys.forEach { action ->
            prefs.getString("map_$action",null)?.toIntOrNull()?.let { edit.putString("map_$action",swapped(it).toString()) }
        }
        edit.apply()
    }
    fun label(code:Int,layout:String,convention:String="logical"):String = when(if(convention=="position") swapped(code) else code) {
        96 -> if(layout=="playstation") "✕" else "A"
        97 -> if(layout=="playstation") "○" else "B"
        99 -> if(layout=="playstation") "□" else "X"
        100 -> if(layout=="playstation") "△" else "Y"
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
