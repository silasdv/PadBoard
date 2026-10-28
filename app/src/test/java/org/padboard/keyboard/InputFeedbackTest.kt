package org.padboard.keyboard

import android.content.Context
import android.os.VibratorManager
import android.provider.Settings
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class InputFeedbackTest {
    class TestIme:PadIme() {
        val info=EditorInfo().apply { inputType=InputType.TYPE_CLASS_TEXT }
        lateinit var connection:BaseInputConnection
        override fun getCurrentInputConnection():InputConnection=connection
        override fun getCurrentInputEditorInfo():EditorInfo=info
    }
    @Test fun successfulControllerAndTouchCommitsBothRequestVibration() {
        val controller=Robolectric.buildService(TestIme::class.java).create()
        val ime=controller.get()
        ime.connection=BaseInputConnection(View(ime),true)
        Settings.System.putInt(ime.contentResolver,Settings.System.HAPTIC_FEEDBACK_ENABLED,1)
        val vibrator=(ime.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        val motor=shadowOf(vibrator)
        motor.setHasVibrator(true)
        ime.onStartInputView(ime.info,false)
        ime.onKeyDown(KeyEvent.KEYCODE_BUTTON_A,KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BUTTON_A))
        assertTrue(ime.connection.editable?.isNotEmpty()==true)
        assertTrue(motor.isVibrating)
        vibrator.cancel()
        PadIme::class.java.getDeclaredField("feedbackDevice").apply { isAccessible=true }.set(ime,null)
        PadIme::class.java.getDeclaredMethod("commit",String::class.java).apply { isAccessible=true }.invoke(ime,"t")
        assertTrue(motor.isVibrating)
        controller.destroy()
    }
}
