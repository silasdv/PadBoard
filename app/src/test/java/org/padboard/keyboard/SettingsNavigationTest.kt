package org.padboard.keyboard

import android.view.KeyEvent
import android.view.View
import android.widget.Spinner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class SettingsNavigationTest {
    @Test fun selectedLanguagesPersistAndRemovingTheActiveLanguageSelectsAnEnabledOne() {
        val controller=Robolectric.buildActivity(SetupActivity::class.java).setup()
        val prefs=controller.get().getSharedPreferences("settings",0)
        prefs.edit().clear().putString("language","de").commit()
        TypingLanguages.select(prefs,listOf("de","fr","de","unknown"))
        assertEquals(listOf("de","fr"),TypingLanguages.enabled(prefs))
        TypingLanguages.select(prefs,listOf("fr"))
        assertEquals("fr",prefs.getString("language",null))
        try { TypingLanguages.select(prefs,emptyList()); fail("Empty language selection must fail") } catch(expected:IllegalArgumentException) {}
        assertEquals(listOf("fr"),TypingLanguages.enabled(prefs))
        controller.pause().stop().destroy()
    }
    @Test fun calibrationReadsPrintedAAndKeepsOtherBindings() {
        val controller=Robolectric.buildActivity(SetupActivity::class.java).setup()
        val activity=controller.get()
        val prefs=activity.getSharedPreferences("settings",0)
        prefs.edit().putString("buttonLayout","nintendo").putString("map_send","106").commit()
        val calibrate=SetupActivity::class.java.getDeclaredMethod("calibrateButtons").apply { isAccessible=true }
        for(layout in listOf("nintendo","xbox","playstation")) {
        prefs.edit().putString("buttonLayout",layout).commit()
        for((key,mode) in listOf(97 to "position",96 to "logical")) {
            calibrate.invoke(activity)
            val dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            val message=dialog.findViewById<android.widget.TextView>(android.R.id.message).text.toString()
            assertTrue(message.contains(if(layout=="playstation") "✕ (cross)" else "printed A"))
            dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,key))
            assertEquals(mode,prefs.getString("inputConvention",null))
            val map=ButtonBindings.read(prefs)
            assertEquals(key,map["type"])
            assertEquals(106,map["send"])
            assertEquals(map.size,map.values.toSet().size)
        }
        }
        controller.pause().stop().destroy()
    }
    @Test fun changingPresetSwapsCustomBindingsAndCanRoundTrip() {
        val controller=Robolectric.buildActivity(SetupActivity::class.java).setup()
        val prefs=controller.get().getSharedPreferences("settings",0)
        prefs.edit().clear().putString("map_type","99").putString("map_delete","96").putString("map_send","106").commit()
        ButtonBindings.selectProfile(prefs,"playstation_swapped")
        assertEquals("position",prefs.getString("inputConvention",null))
        assertEquals("playstation",prefs.getString("buttonLayout",null))
        assertEquals(100,ButtonBindings.read(prefs)["type"])
        assertEquals(106,ButtonBindings.read(prefs)["send"])
        ButtonBindings.selectProfile(prefs,"playstation")
        assertEquals(99,ButtonBindings.read(prefs)["type"])
        assertEquals(96,ButtonBindings.read(prefs)["delete"])
        controller.pause().stop().destroy()
    }
    @Test fun vibrationSelectionKeepsTheSameScrolledSettingsView() {
        val controller=Robolectric.buildActivity(SetupActivity::class.java).setup().visible()
        val activity=controller.get()
        val content=activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        val scroll=content.getChildAt(0) as android.widget.ScrollView
        scroll.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY))
        scroll.layout(0,0,1000,600); scroll.scrollTo(0,250)
        val before=scroll.scrollY
        assertTrue(before>0)
        val field=SetupActivity::class.java.getDeclaredField("controls").apply { isAccessible=true }
        @Suppress("UNCHECKED_CAST") val controls=field.get(activity) as List<View>
        controls.filterIsInstance<android.widget.Button>().first { it.text.toString()=="Strong" }.performClick()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertSame(scroll,content.getChildAt(0))
        assertEquals(before,scroll.scrollY)
        assertEquals("strong",activity.getSharedPreferences("settings",0).getString("hapticStrength",null))
        controller.pause().stop().destroy()
    }
    @Test fun mappingIsAnIsolatedSubmenuAndTouchCheckboxChangesOnFirstTap() {
        val controller=Robolectric.buildActivity(SetupActivity::class.java).setup().visible()
        controller.windowFocusChanged(true)
        val activity=controller.get()
        val field=SetupActivity::class.java.getDeclaredField("controls").apply { isAccessible=true }
        @Suppress("UNCHECKED_CAST") fun controls()=(field.get(activity) as List<View>).toList()
        val mainCount=controls().filterIsInstance<Spinner>().size
        controls().filterIsInstance<android.widget.Button>().first { it.text.toString().startsWith("Button Mapping") }.performClick()
        assertTrue(controls().filterIsInstance<Spinner>().size>mainCount)
        controls().filterIsInstance<android.widget.Button>().first { it.text.toString().contains("Back") }.performClick()
        assertEquals(mainCount,controls().filterIsInstance<Spinner>().size)
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_UP))
        val down=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,0f,0f,0)
        activity.dispatchTouchEvent(down); down.recycle()
        val checkbox=controls().filterIsInstance<android.widget.CheckBox>().first()
        assertFalse(checkbox.isFocusableInTouchMode)
        checkbox.measure(View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(80,View.MeasureSpec.EXACTLY))
        checkbox.layout(0,0,900,80)
        val before=checkbox.isChecked
        for(action in listOf(android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP)) {
            val e=android.view.MotionEvent.obtain(0,1,action,450f,40f,0); checkbox.dispatchTouchEvent(e); e.recycle()
        }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(!before,checkbox.isChecked)
        controller.pause().stop().destroy()
    }
    @Test fun tenDistinctThemesIncludeBaseModes() {
        assertEquals(10,Palette.ids.size)
        assertEquals(10,Palette.ids.map { Palette.named(it) }.toSet().size)
        assertTrue(Palette.ids.containsAll(listOf("light","dark","oled")))
    }
    @Test fun controllerCanFocusControlsAfterTouchAndAdvanceInOrder() {
        val controller=Robolectric.buildActivity(SetupActivity::class.java).setup().visible()
        controller.windowFocusChanged(true)
        val activity=controller.get()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        activity.window.decorView.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY))
        activity.window.decorView.layout(0,0,1920,1080)
        val field=SetupActivity::class.java.getDeclaredField("controls").apply { isAccessible=true }
        @Suppress("UNCHECKED_CAST") val controls=field.get(activity) as List<View>
        assertTrue(controls.size>15)
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_UP))
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_UP))
        controls.forEach { assertTrue(it.isFocusableInTouchMode); assertFalse(it.defaultFocusHighlightEnabled) }
        assertTrue("First control cannot take focus",controls[0].requestFocus())
        assertSame("Focus control is detached from current content",activity.window.decorView,controls[0].rootView)
        assertTrue("View did not retain focus",controls[0].isFocused)
        assertNotNull("Decor has no focused view",activity.window.decorView.findFocus())
        assertTrue("Window not focused",activity.hasWindowFocus())
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN))
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_DOWN))
        assertSame(controls[1],activity.window.decorView.findFocus())
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT))
        assertSame(controls[1],activity.window.decorView.findFocus())
        val spinner=controls.filterIsInstance<Spinner>().first()
        val next=(spinner.selectedItemPosition+1).coerceAtMost(spinner.count-1)
        spinner.requestFocus()
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(next,spinner.selectedItemPosition)
        controller.pause().stop().destroy()
    }
}
