package org.padboard.keyboard

data class TypingLanguage(val code:String,val name:String,val rows:List<String>,val accents:String="")

object TypingLanguages {
    val all=listOf(
        TypingLanguage("en","English · QWERTY",listOf("qwertyuiop","asdfghjkl","zxcvbnm")),
        TypingLanguage("de","Deutsch · QWERTZ",listOf("qwertzuiopü","asdfghjklöä","yxcvbnmß")),
        TypingLanguage("fr","Français · AZERTY",listOf("azertyuiop","qsdfghjklm","wxcvbn"),"éèêëàâçùîô"),
        TypingLanguage("es","Español · QWERTY",listOf("qwertyuiop","asdfghjklñ","zxcvbnm"),"áéíóúü¿¡"),
        TypingLanguage("it","Italiano · QWERTY",listOf("qwertyuiop","asdfghjkl","zxcvbnm"),"àèéìòù"),
        TypingLanguage("pt","Português · QWERTY",listOf("qwertyuiop","asdfghjklç","zxcvbnm"),"áàâãéêíóôõú"),
        TypingLanguage("nl","Nederlands · QWERTY",listOf("qwertyuiop","asdfghjkl","zxcvbnm"),"éëïöüè"),
        TypingLanguage("sv","Svenska · QWERTY",listOf("qwertyuiopå","asdfghjklöä","zxcvbnm")),
        TypingLanguage("da","Dansk · QWERTY",listOf("qwertyuiopå","asdfghjklæø","zxcvbnm")),
        TypingLanguage("no","Norsk · QWERTY",listOf("qwertyuiopå","asdfghjkløæ","zxcvbnm")),
        TypingLanguage("fi","Suomi · QWERTY",listOf("qwertyuiopå","asdfghjklöä","zxcvbnm")),
        TypingLanguage("pl","Polski · QWERTY",listOf("qwertyuiop","asdfghjkl","zxcvbnm"),"ąćęłńóśźż"),
        TypingLanguage("cs","Čeština · QWERTZ",listOf("qwertzuiop","asdfghjklů","yxcvbnm"),"ěščřžýáíéúďťň")
    )
    fun get(code:String)=all.firstOrNull { it.code==code } ?: all.first()
    fun enabled(prefs:android.content.SharedPreferences):List<String> {
        val saved=prefs.getString("enabledLanguages",null)
        val codes=if(saved==null) listOf(prefs.getString("language","en") ?: "en") else saved.split(',')
        return codes.filter { code->all.any { it.code==code } }.distinct().ifEmpty { listOf("en") }
    }
    fun select(prefs:android.content.SharedPreferences,codes:List<String>) {
        val selected=codes.filter { code->all.any { it.code==code } }.distinct()
        require(selected.isNotEmpty())
        val current=prefs.getString("language","en") ?: "en"
        prefs.edit().putString("enabledLanguages",selected.joinToString(","))
            .putString("language",if(current in selected) current else selected.first()).apply()
    }
    fun next(current:String,enabled:List<String>):String = enabled[(enabled.indexOf(current)+1).mod(enabled.size)]
}
