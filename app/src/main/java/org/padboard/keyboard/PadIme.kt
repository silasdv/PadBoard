package org.padboard.keyboard

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.hardware.input.InputManager
import android.text.InputType
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype
import android.widget.*
import android.icu.text.BreakIterator

open class PadIme : InputMethodService(), InputManager.InputDeviceListener {
    private val state = KeyboardState()
    private val gate = PressGate()
    private val handler = Handler(Looper.getMainLooper())
    private val held = mutableSetOf<Pair<Int, Int>>()
    private val consumedUps = mutableSetOf<Pair<Int, Int>>()
    private var root: LinearLayout? = null
    private var preview: EditorPreview? = null
    private var keyboard: KeyboardView? = null
    private var active = false
    private var feedbackDevice: Int? = null
    private fun bindings() = ButtonBindings.read(prefs)
    private var stickX = 0
    private var stickY = 0
    private val repeatStick = object : Runnable {
        override fun run() {
            if (!active || (stickX == 0 && stickY == 0)) return
            state.move(stickX, stickY); paint(); handler.postDelayed(this, 90)
        }
    }
    private val prefs get() = getSharedPreferences("settings", MODE_PRIVATE)
    override fun onCreate() {
        super.onCreate()
        ButtonBindings.migrate(prefs)
        (getSystemService(INPUT_SERVICE) as InputManager).registerInputDeviceListener(this, handler)
    }
    override fun onInputDeviceAdded(deviceId: Int) { clearHeld() }
    override fun onInputDeviceChanged(deviceId: Int) { clearHeld() }
    override fun onInputDeviceRemoved(deviceId: Int) {
        clearHeld(); consumedUps.removeAll { it.first == deviceId }
    }
    override fun onEvaluateFullscreenMode() = false
    override fun onEvaluateInputViewShown(): Boolean { super.onEvaluateInputViewShown(); return true }
    override fun onCreateInputView(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.rgb(17, 23, 34)); setPadding(8, 4, 8, 4)
        root = this
    }
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        clearHeld(); active = true; keyboard?.controllerFocus = false
        state.changeLanguage((prefs.getString("language", "en") ?: "en").takeIf { it in TypingLanguages.enabled(prefs) } ?: TypingLanguages.enabled(prefs).first())
        state.numberRow = prefs.getBoolean("numberRow", false)
        val variation = (info?.inputType ?: 0) and InputType.TYPE_MASK_VARIATION
        state.email = (info?.inputType ?: 0) and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT && variation in listOf(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
        state.symbols = false; state.resetCase(); state.clamp(); syncCaps(); paint()
    }
    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        newSubtype?.locale?.let { locale ->
            val lang = locale.substringBefore('_').substringBefore('-')
            if(lang !in TypingLanguages.enabled(prefs)) return
            prefs.edit().putString("language", lang).apply(); state.changeLanguage(lang); paint()
        }
    }
    private fun clearHeld() { held.clear(); gate.clear(); stickX = 0; stickY = 0; handler.removeCallbacksAndMessages(null) }
    override fun onFinishInputView(finishingInput: Boolean) {
        active = false; preview?.clear(); clearHeld(); super.onFinishInputView(finishingInput)
    }
    override fun onFinishInput() { active = false; preview?.clear(); clearHeld(); super.onFinishInput() }
    override fun onDestroy() {
        (getSystemService(INPUT_SERVICE) as InputManager).unregisterInputDeviceListener(this)
        clearHeld(); consumedUps.clear(); super.onDestroy()
    }
    private fun actionLabel(): String {
        val en = true
        return when (editorActionId()) {
            EditorInfo.IME_ACTION_SEND -> if (en) "Send" else "Senden"
            EditorInfo.IME_ACTION_SEARCH -> "search"
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_NEXT -> if (en) "Next" else "Nächstes"
            EditorInfo.IME_ACTION_DONE -> if (en) "Done" else "Fertig"
            else -> "↵"
        }
    }
    private fun editorActionId(): Int {
        val info = currentInputEditorInfo ?: return EditorInfo.IME_ACTION_NONE
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        if (action == EditorInfo.IME_ACTION_NEXT) return action
        if (action in listOf(EditorInfo.IME_ACTION_NONE, EditorInfo.IME_ACTION_UNSPECIFIED) &&
            info.imeOptions and EditorInfo.IME_FLAG_NAVIGATE_NEXT != 0) return EditorInfo.IME_ACTION_NEXT
        return if (info.actionLabel != null) info.actionId else action
    }
    private fun screenHeight(): Int {
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        return if (android.os.Build.VERSION.SDK_INT >= 30) manager.maximumWindowMetrics.bounds.height()
        else android.util.DisplayMetrics().also { @Suppress("DEPRECATION") manager.defaultDisplay.getRealMetrics(it) }.heightPixels
    }
    private fun paint() {
        val box = root ?: return
        val palette = Palette.named(prefs.getString("theme", "oled") ?: "oled")
        val baseHeight = (screenHeight() * prefs.getInt("heightPercent", 35).coerceIn(20,100) / 100f).toInt()
        val previewHeight = (24 * resources.displayMetrics.density).toInt()
        val rowHeight = (baseHeight-previewHeight-8).coerceAtLeast(4)/4f
        val height = (baseHeight + rowHeight * state.extraRows()).toInt().coerceAtMost(screenHeight())
        box.setPadding(4, 4, 4, 4); box.setBackgroundColor(palette.background)
        box.layoutParams = box.layoutParams?.apply { this.height = height }
            ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        if (keyboard == null || keyboard?.parent != box) {
            box.removeAllViews()
            preview = EditorPreview(this).apply { box.addView(this, LinearLayout.LayoutParams(-1, previewHeight)) }
            keyboard = KeyboardView(this, state) { key ->
                feedbackDevice=null
                if (key.action == "text") { commit(key.label); syncCaps(); paint() }
                else perform(key.action)
            }.also { box.addView(it, LinearLayout.LayoutParams(-1,0,1f)) }
        }
        preview?.color = palette.hint
        preview?.visibility = if (!password()) View.VISIBLE else View.INVISIBLE
        keyboard?.palette = palette
        keyboard?.sendLabel = actionLabel()
        keyboard?.bindings = bindings()
        keyboard?.buttonLayout = prefs.getString("buttonLayout","nintendo") ?: "nintendo"
        keyboard?.inputConvention = prefs.getString("inputConvention","logical") ?: "logical"
        keyboard?.invalidate(); box.requestLayout()
        updatePreview()
    }
    private fun syncCaps() {
        val type = currentInputEditorInfo?.inputType ?: return
        val variation = type and InputType.TYPE_MASK_VARIATION
        val allowed = !password() && type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT &&
            variation !in listOf(InputType.TYPE_TEXT_VARIATION_URI,InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(1,0)
        val after = ic.getTextAfterCursor(1,0)
        if (before != null && after != null) state.syncEmpty(before.isEmpty() && after.isEmpty(), allowed)
    }
    private fun password(): Boolean {
        val type = currentInputEditorInfo?.inputType ?: return true
        val variation = type and InputType.TYPE_MASK_VARIATION
        return (type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) ||
            (type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT && variation in listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
    }
    private fun updatePreview() {
        if (!active || password()) { preview?.clear(); return }
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(80, 0)
        val after = ic.getTextAfterCursor(40, 0)
        preview?.show(before,after)
    }
    override fun onUpdateSelection(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int) {
        super.onUpdateSelection(a, b, c, d, e, f); if (active) { syncCaps(); keyboard?.invalidate(); updatePreview() }
    }
    private fun commit(text: String) {
        if (currentInputConnection?.commitText(text,1) != true || !prefs.getBoolean("haptic",true)) return
        if (android.provider.Settings.System.getInt(contentResolver,android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED,1) == 0) return
        val systemVibrator = if (android.os.Build.VERSION.SDK_INT >= 31) (getSystemService(VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
            else @Suppress("DEPRECATION") (getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator)
        val device=feedbackDevice?.let { InputDevice.getDevice(it) }
        val controllerVibrator = if (device!=null && android.os.Build.VERSION.SDK_INT>=31) device.vibratorManager.defaultVibrator
            else @Suppress("DEPRECATION") device?.vibrator
        // Prefer the handheld motor for touch AND controller input, fall back to the input device motor.
        val vibrator=if(systemVibrator.hasVibrator()) systemVibrator else controllerVibrator?.takeIf { it.hasVibrator() } ?: return
        val strength=prefs.getString("hapticStrength","light")
        val duration=when(strength) { "strong"->24L; "medium"->14L; else->8L }
        val amplitude=when(strength) { "strong"->150; "medium"->70; else->24 }
        try {
            val effect=android.os.VibrationEffect.createOneShot(duration,if(vibrator.hasAmplitudeControl()) amplitude else android.os.VibrationEffect.DEFAULT_AMPLITUDE)
            // PHYSICAL_EMULATION describes controller feedback; TOUCH may be suppressed for key input on OEM firmware.
            if(android.os.Build.VERSION.SDK_INT>=33) vibrator.vibrate(effect,android.os.VibrationAttributes.Builder().setUsage(if(feedbackDevice==null) android.os.VibrationAttributes.USAGE_TOUCH else android.os.VibrationAttributes.USAGE_PHYSICAL_EMULATION).build())
            else vibrator.vibrate(effect,android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
        } catch (_: SecurityException) { /* Device policy can disable vibration. */ }
    }
    private fun perform(action: String) {
        if (!active) return
        val ic = currentInputConnection
        when (action) {
            "type" -> { val key = state.selectedKey(); if (key.action == "text") commit(key.label) else { perform(key.action); return } }
            "space" -> commit(" ")
            "delete" -> if (ic != null) {
                if (!ic.getSelectedText(0).isNullOrEmpty()) ic.commitText("", 1)
                else {
                    val before = ic.getTextBeforeCursor(256, 0)?.toString()
                    if (!before.isNullOrEmpty()) {
                        val iterator = BreakIterator.getCharacterInstance(); iterator.setText(before)
                        val boundary = iterator.preceding(before.length)
                        ic.deleteSurroundingText(before.length - boundary.coerceAtLeast(0), 0)
                    } else if (before == null) {
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                    }
                }
            }
            "send" -> if (ic != null) {
                val info = currentInputEditorInfo
                val id = editorActionId()
                if (id !in listOf(EditorInfo.IME_ACTION_NONE, EditorInfo.IME_ACTION_UNSPECIFIED)) ic.performEditorAction(id)
                else if ((info?.inputType ?: 0) and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0) ic.commitText("\n", 1)
            }
            "symbols" -> state.toggleSymbols()
            "case" -> state.toggleCase()
            "language" -> { state.changeLanguage(TypingLanguages.next(state.language,TypingLanguages.enabled(prefs))); prefs.edit().putString("language", state.language).apply() }
            "settings" -> {
                active=false; clearHeld(); preview?.clear(); requestHideSelf(0)
                startActivity(android.content.Intent(this,SetupActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            }
            "left" -> state.move(-1, 0)
            "right" -> state.move(1, 0)
            "up" -> state.move(0, -1)
            "down" -> state.move(0, 1)
            "close" -> { active = false; clearHeld(); preview?.clear(); requestHideSelf(0); return }
        }
        syncCaps(); paint()
    }
    private fun mapping(key:Int):String? = when(key) {
        KeyEvent.KEYCODE_DPAD_CENTER -> "type"
        KeyEvent.KEYCODE_DPAD_UP -> "up"
        KeyEvent.KEYCODE_DPAD_DOWN -> "down"
        KeyEvent.KEYCODE_DPAD_LEFT -> "left"
        KeyEvent.KEYCODE_DPAD_RIGHT -> "right"
        else -> bindings().entries.firstOrNull { it.value==key }?.key
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val action = mapping(keyCode) ?: return super.onKeyDown(keyCode, event)
        if (!active) return super.onKeyDown(keyCode, event)
        keyboard?.controllerFocus = true
        keyboard?.invalidate()
        feedbackDevice=event.deviceId
        val key = event.deviceId to keyCode
        consumedUps.add(key)
        val first = held.add(key)
        if (keyCode in listOf(KeyEvent.KEYCODE_BUTTON_L2,KeyEvent.KEYCODE_BUTTON_R2) || action == "symbols" || action == "case") {
            if (gate.update("${event.deviceId}:$action", "key", true)) perform(action)
        } else if ((first && event.repeatCount == 0) || (event.repeatCount > 0 && action in listOf("left", "right", "up", "down", "delete"))) perform(action)
        return true
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val key = event.deviceId to keyCode
        held.remove(key)
        mapping(keyCode)?.let { gate.update("${event.deviceId}:$it", "key", false) }
        if (consumedUps.remove(key)) return true
        return super.onKeyUp(keyCode, event)
    }
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (!active || !event.isFromSource(InputDevice.SOURCE_JOYSTICK) || event.action != MotionEvent.ACTION_MOVE) return super.onGenericMotionEvent(event)
        for ((axis, action) in listOf(MotionEvent.AXIS_LTRIGGER to mapping(KeyEvent.KEYCODE_BUTTON_L2), MotionEvent.AXIS_RTRIGGER to mapping(KeyEvent.KEYCODE_BUTTON_R2))) {
            if(action==null) continue
            feedbackDevice=event.deviceId
            val value = maxOf(event.getAxisValue(axis),event.getAxisValue(if(axis==MotionEvent.AXIS_LTRIGGER) MotionEvent.AXIS_BRAKE else MotionEvent.AXIS_GAS))
            if (value > .65f) { if (gate.update("${event.deviceId}:$action", "axis", true)) { keyboard?.controllerFocus = true; perform(action) } }
            else if (value < .35f) gate.update("${event.deviceId}:$action", "axis", false)
        }
        fun direction(axis: Int): Int { val v = event.getAxisValue(axis); return if (v > .55f) 1 else if (v < -.55f) -1 else 0 }
        val x = direction(MotionEvent.AXIS_X).let { if (it == 0) direction(MotionEvent.AXIS_HAT_X) else it }
        val y = direction(MotionEvent.AXIS_Y).let { if (it == 0) direction(MotionEvent.AXIS_HAT_Y) else it }
        if (x != stickX || y != stickY) {
            stickX = x; stickY = y; handler.removeCallbacks(repeatStick)
            if (x != 0 || y != 0) { keyboard?.controllerFocus = true; state.move(x, y); paint(); handler.postDelayed(repeatStick, 350) }
        }
        return true
    }
}
