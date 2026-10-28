package org.padboard.keyboard

import org.junit.Assert.*
import org.junit.Test

class KeyboardStateTest {
    @Test fun nintendoLogicalAndPositionReportingResolvePrintedButtonsConsistently() {
        for(mode in listOf("logical","position")) {
            val map=ButtonBindings.forLayout("nintendo",mode)
            for((action,label) in mapOf("type" to "A","close" to "B","delete" to "X","space" to "Y")) {
                assertEquals(label,ButtonBindings.label(map.getValue(action),"nintendo",mode))
            }
        }
        assertEquals(96,ButtonBindings.forLayout("nintendo","logical")["type"])
        assertEquals(97,ButtonBindings.forLayout("nintendo","position")["type"])
    }
    @Test fun shiftAndSymbolsKeepTheirAnchorsAcrossLanguagesAndLayers() {
        for(language in listOf("de","en")) for(numbers in listOf(false,true)) {
            val s=KeyboardState(language); s.numberRow=numbers
            val anchors=s.placements().filter { it.key.id in listOf("case","symbols","language","settings","space",".") }.associate { it.key.id to (it.row to it.x) }
            s.toggleSymbols()
            for(p in s.placements().filter { it.key.id in anchors }) assertEquals(anchors[p.key.id],p.row to p.x)
            assertTrue(s.placements().filter { it.key.action!="none" }.all { it.x>=0 && it.x+it.key.width<=s.totalWidth() })
        }
        val retroid=ButtonBindings.forLayout("nintendo","position")
        assertEquals("A",ButtonBindings.label(retroid.getValue("type"),"nintendo","position"))
        assertEquals("B",ButtonBindings.label(retroid.getValue("close"),"nintendo","position"))
        assertEquals(97,retroid["type"])
    }
    @Test fun casePreservesSharpSAndDoesNotMoveSpaceOrActionForEitherLanguage() {
        for(language in listOf("de","en")) {
            val s=KeyboardState(language)
            val lower=s.placements().associate { it.key.id to (it.x to it.key.width) }
            s.uppercase=true
            for(p in s.placements()) assertEquals(lower[p.key.id],p.x to p.key.width)
            if(language=="de") assertEquals("ß",s.keys().flatten().first { it.id=="ß" }.label)
            assertTrue(s.keys().flatten().any { it.id=="send" })
            val bottom=s.keys().last(); val globe=bottom.indexOfFirst { it.id=="language" }
            assertEquals("settings",bottom[globe+1].id)
            for(r in s.keys().indices) {
                val row=s.placements().filter { it.row==r }
                assertEquals(row.first().x,s.totalWidth()-row.last().x-row.last().key.width,0.001f)
                assertTrue(row.first().x>=0f)
            }
        }
    }
    @Test fun centeredRowsHaveEqualMarginsAndNumberRowIsNotDuplicated() {
        for(language in listOf("de","en")) for(symbols in listOf(false,true)) {
            val s=KeyboardState(language); s.symbols=symbols; s.numberRow=true
            for(r in s.keys().indices) {
                val row=s.placements().filter { it.row==r }
                assertEquals(row.first().x,s.totalWidth()-(row.last().x+row.last().key.width),0.001f)
            }
            for(digit in "1234567890") assertEquals(1,s.keys().flatten().count { it.label==digit.toString() })
        }
    }
    @Test fun mappingSwapsConflictsAndLabelsFollowLayout() {
        val changed=ButtonBindings.assign(ButtonBindings.defaults,"space",99)
        assertEquals(99,changed["space"]); assertEquals(100,changed["delete"])
        assertEquals(changed.size,changed.values.toSet().size)
        assertEquals("X",ButtonBindings.label(changed.getValue("space"),"xbox"))
        assertEquals("Y",ButtonBindings.label(changed.getValue("space"),"nintendo","position"))
        assertEquals("□",ButtonBindings.label(changed.getValue("space"),"playstation"))
    }
    @Test fun contextAddsRowsAboveWithoutChangingBaseGeometry() {
        val s=KeyboardState()
        val base=s.placements().associate { it.key.id to (it.x to it.key.width) }
        s.email=true
        for(p in s.placements().filter { it.row>=s.extraRows() }) assertEquals(base[p.key.id],p.x to p.key.width)
        assertFalse(s.keys().flatten().any { it.action=="close" })
        val bottom=s.keys().last()
        assertEquals("send",bottom[bottom.indexOfFirst { it.id=="." }+1].id)
        s.symbols=true
        assertFalse(s.keys().flatten().any { it.action=="send" })
    }
    @Test fun optionalNumberRowPrecedesLettersAndKeepsStagger() {
        val s=KeyboardState()
        assertEquals("q",s.keys().first().first().id)
        s.numberRow=true
        assertEquals("1234567890",s.keys().first().joinToString("") { it.label })
        assertEquals("q",s.keys()[1].first().id)
        assertTrue(s.placements().first { it.key.id=="a" }.x > s.placements().first { it.key.id=="q" }.x)
        s.email=true
        for(r in s.keys().indices) { s.row=r; s.column=0; repeat(20) { s.move(1,0) }; s.selected() }
        s.numberRow=false; s.email=false; s.clamp(); s.selected()
        assertEquals("␣",s.keys().flatten().first { it.id=="space" }.label)
    }
    @Test fun emailShortcutsAppearAboveStableBaseLayout() {
        val s = KeyboardState()
        assertFalse(s.placements().any { it.key.id == ".com" })
        s.email = true
        for (domain in listOf("@", ".com", ".de", ".net")) assertTrue(s.keys().first().any { it.label == domain && it.action == "text" })
        val send=s.placements().first { it.key.id == "send" }
        assertEquals(s.extraRows()+3,send.row); assertTrue(send.key.width > 2f)
        s.row=4; s.column=3; s.email=false; s.clamp(); s.selected()
    }
    @Test fun emptyFieldCapitalizationReturnsAfterDeletingAllText() {
        val s = KeyboardState()
        s.syncEmpty(true, true); assertTrue(s.uppercase)
        s.syncEmpty(false, true); assertFalse(s.uppercase)
        s.syncEmpty(true, true); assertTrue(s.uppercase)
        s.toggleCase(); s.syncEmpty(true, true); assertFalse(s.uppercase)
        s.syncEmpty(false, true); s.syncEmpty(true, true); assertTrue(s.uppercase)
        s.resetCase(); s.syncEmpty(true, false); assertFalse(s.uppercase)
    }
    @Test fun staggeredRowsAndTouchActionsUseSharedGeometry() {
        val s = KeyboardState()
        val p = s.placements()
        assertTrue(p.first { it.key.id == "a" }.x > p.first { it.key.id == "q" }.x)
        for (id in listOf("language", "space", "case", "symbols", "delete", "send", ".", ",")) assertTrue(p.any { it.key.id == id })
        for (r in s.keys().indices) {
            val line = p.filter { it.row == r }
            for (i in 1 until line.size) assertTrue(line[i].x >= line[i-1].x + line[i-1].key.width)
        }
    }
    @Test fun everyKeyReachableAndSwitchingKeepsSelectionValid() {
        for (lang in listOf("de", "en")) for (symbol in listOf(false, true)) {
            val s = KeyboardState(lang); s.symbols = symbol
            for (r in s.rows().indices) {
                s.row = r; s.column = 0
                val seen = mutableSetOf(s.selected())
                repeat(20) { s.move(1, 0); seen.add(s.selected()) }
                assertEquals(s.keys()[r].filter { it.action!="none" }.map { it.label }.toSet(), seen)
                s.toggleSymbols(); s.selected(); s.changeLanguage("en"); s.selected()
                s.changeLanguage(lang); s.symbols = symbol; s.clamp()
            }
        }
    }
    @Test fun triggerKeyAndAxisProduceOneActionUntilBothReleased() {
        val g = PressGate()
        assertTrue(g.update("symbols", "key", true))
        assertFalse(g.update("symbols", "axis", true))
        assertFalse(g.update("symbols", "key", false))
        assertFalse(g.update("symbols", "axis", true))
        g.update("symbols", "axis", false)
        assertTrue(g.update("symbols", "key", true))
    }
    @Test fun navigationStopsAtBoundaries() {
        val s = KeyboardState(); s.move(-1, -1)
        assertEquals(0, s.row); assertEquals(0, s.column)
        repeat(20) { s.move(1, 1) }
        assertEquals(s.rows().lastIndex, s.row)
        assertTrue(s.column in s.rows()[s.row].indices)
    }
}
