package org.padboard.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.text.InputType
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*

class SetupActivity : Activity() {
    private var enableButton:Button?=null
    private var selectButton:Button?=null
    private fun refreshSetup() {
        val manager=getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled=manager.enabledInputMethodList.any { it.packageName==packageName }
        val selected=Settings.Secure.getString(contentResolver,Settings.Secure.DEFAULT_INPUT_METHOD)?.startsWith("$packageName/")==true
        enableButton?.text=if(enabled) "1 · Enable  ✓" else "1 · Enable"
        selectButton?.text=if(selected) "2 · Select  ✓" else "2 · Select"
    }
    override fun onResume() { super.onResume(); refreshSetup() }
    private var mappingPage=false
    private var controllerMode=false
    private fun focusedControl():View? = window.decorView.findFocus()
    private val controls=mutableListOf<View>()
    private val navHandler=android.os.Handler(android.os.Looper.getMainLooper())
    private var stickDirection=0
    private val stickRepeat=object:Runnable { override fun run() { if(stickDirection!=0) { navigate(stickDirection); navHandler.postDelayed(this,140) } } }
    private fun installFocus(view:View) {
        if(view is Button || view is Spinner || view is SeekBar || view is EditText || view is CompoundButton) {
            controls.add(view)
            view.isFocusable=true
            view.isFocusableInTouchMode=controllerMode || view is EditText
            view.defaultFocusHighlightEnabled=false
            view.backgroundTintList=null
            if(view is Spinner) view.descendantFocusability=ViewGroup.FOCUS_BLOCK_DESCENDANTS
            val normal=view.background
            val focused=android.graphics.drawable.GradientDrawable().apply {
                setColor(if(view.tag=="choice:false") Color.TRANSPARENT else if(view is Button && view.text.toString()!="ⓘ") Color.rgb(91,220,193) else Color.rgb(38,50,67))
                cornerRadius=18*resources.displayMetrics.density
                setStroke((3*resources.displayMetrics.density).toInt(),Color.rgb(174,255,231))
            }
            view.background=android.graphics.drawable.StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused),focused)
                addState(intArrayOf(android.R.attr.state_pressed),focused.constantState!!.newDrawable().mutate())
                addState(intArrayOf(),normal ?: android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            }
            return
        }
        if(view is ViewGroup) { view.isFocusable=false; for(i in 0 until view.childCount) installFocus(view.getChildAt(i)) }
    }
    private fun navigate(direction:Int) {
        if(kotlin.math.abs(direction)==2) {
            dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,if(direction<0) android.view.KeyEvent.KEYCODE_DPAD_LEFT else android.view.KeyEvent.KEYCODE_DPAD_RIGHT))
            return
        }
        if(controls.isEmpty()) return
        val index=controls.indexOf(focusedControl())
        val next=if(index<0) 0 else (index+direction).coerceIn(0,controls.lastIndex)
        controls[next].requestFocus()
        controls[next].requestRectangleOnScreen(android.graphics.Rect(0,0,controls[next].width,controls[next].height),false)
    }
    override fun dispatchKeyEvent(event:android.view.KeyEvent):Boolean {
        var focus=focusedControl()
        val key=event.keyCode
        val confirm=ButtonBindings.read(getSharedPreferences("settings",MODE_PRIVATE)).getValue("type")
        val navigation=key==confirm || key in listOf(android.view.KeyEvent.KEYCODE_DPAD_UP,android.view.KeyEvent.KEYCODE_DPAD_DOWN,android.view.KeyEvent.KEYCODE_DPAD_LEFT,android.view.KeyEvent.KEYCODE_DPAD_RIGHT,android.view.KeyEvent.KEYCODE_BUTTON_A,android.view.KeyEvent.KEYCODE_DPAD_CENTER)
        if(!navigation || hasWindowFocus().not()) return super.dispatchKeyEvent(event)
        setControllerMode(true); focus=focusedControl()
        if(focus is EditText && keyboardVisible()) return super.dispatchKeyEvent(event)
        if(focus is EditText && (key==confirm || key in listOf(android.view.KeyEvent.KEYCODE_BUTTON_A,android.view.KeyEvent.KEYCODE_DPAD_CENTER))) {
            if(event.action==android.view.KeyEvent.ACTION_DOWN && event.repeatCount==0)
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(focus,InputMethodManager.SHOW_IMPLICIT)
            return true
        }
        if(focus is EditText && key in listOf(android.view.KeyEvent.KEYCODE_DPAD_LEFT,android.view.KeyEvent.KEYCODE_DPAD_RIGHT)) return super.dispatchKeyEvent(event)
        if(event.action==android.view.KeyEvent.ACTION_UP) return true
        when(key) {
            android.view.KeyEvent.KEYCODE_DPAD_UP -> navigate(-1)
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> navigate(1)
            android.view.KeyEvent.KEYCODE_DPAD_LEFT,android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val delta=if(key==android.view.KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1
                when(focus) {
                    is SeekBar -> focus.progress=(focus.progress+delta).coerceIn(0,focus.max)
                    is Spinner -> focus.setSelection((focus.selectedItemPosition+delta).coerceIn(0,focus.count-1))
                    is CompoundButton -> if(event.repeatCount==0) focus.isChecked=delta>0
                    is EditText -> return super.dispatchKeyEvent(event)
                    is Button -> {
                        val parent=focus.parent as? LinearLayout
                        if(parent?.orientation==LinearLayout.HORIZONTAL) {
                            val siblings=controls.filter { it.parent==parent }
                            val index=siblings.indexOf(focus)
                            if(index>=0) siblings[(index+delta).coerceIn(0,siblings.lastIndex)].requestFocus()
                        }
                    }
                    // Other controls stay in place horizontally. Up/down changes rows.
                }
            }
            else -> if(event.repeatCount==0) { if(focus is EditText) return super.dispatchKeyEvent(event) else focus?.performClick() }
        }
        return true
    }
    private fun keyboardVisible():Boolean {
        if(android.os.Build.VERSION.SDK_INT>=30) return window.decorView.rootWindowInsets?.isVisible(android.view.WindowInsets.Type.ime())==true
        val area=android.graphics.Rect(); window.decorView.getWindowVisibleDisplayFrame(area)
        return window.decorView.height-area.height()>100*resources.displayMetrics.density
    }
    override fun onGenericMotionEvent(event:android.view.MotionEvent):Boolean {
        if(!event.isFromSource(android.view.InputDevice.SOURCE_JOYSTICK) || (focusedControl() is EditText && keyboardVisible())) return super.onGenericMotionEvent(event)
        val y=event.getAxisValue(android.view.MotionEvent.AXIS_Y).let { if(kotlin.math.abs(it)>.55f) it else event.getAxisValue(android.view.MotionEvent.AXIS_HAT_Y) }
        val x=event.getAxisValue(android.view.MotionEvent.AXIS_X).let { if(kotlin.math.abs(it)>.55f) it else event.getAxisValue(android.view.MotionEvent.AXIS_HAT_X) }
        val next=if(kotlin.math.abs(y)>=kotlin.math.abs(x)) { if(y>.55f) 1 else if(y<-.55f) -1 else 0 } else { if(x>.55f) 2 else if(x<-.55f) -2 else 0 }
        if(next!=0) setControllerMode(true)
        if(next!=stickDirection) {
            stickDirection=next; navHandler.removeCallbacks(stickRepeat)
            if(next!=0) { navigate(next); navHandler.postDelayed(stickRepeat,400) }
        }
        return true
    }
    override fun onPause() { stickDirection=0; navHandler.removeCallbacksAndMessages(null); super.onPause() }
    override fun onWindowFocusChanged(hasFocus:Boolean) {
        if(!hasFocus) { stickDirection=0; navHandler.removeCallbacksAndMessages(null) }
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus) refreshSetup()
    }
    private fun setControllerMode(enabled:Boolean) {
        if(controllerMode==enabled) return
        controllerMode=enabled
        if(!enabled) {
            stickDirection=0; navHandler.removeCallbacksAndMessages(null)
            if(focusedControl() !is EditText) focusedControl()?.clearFocus()
        }
        controls.forEach { it.isFocusableInTouchMode=enabled || it is EditText }
        if(enabled && focusedControl()==null) controls.firstOrNull()?.requestFocus()
    }
    override fun dispatchTouchEvent(event:android.view.MotionEvent):Boolean {
        if(event.actionMasked==android.view.MotionEvent.ACTION_DOWN) setControllerMode(false)
        return super.dispatchTouchEvent(event)
    }
    @Deprecated("Legacy back dispatcher for API 26 compatibility")
    override fun onBackPressed() {
        if(mappingPage) { mappingPage=false; render() } else super.onBackPressed()
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); ButtonBindings.migrate(getSharedPreferences("settings",MODE_PRIVATE)); render() }
    private fun calibrateButtons() {
        val dialog=AlertDialog.Builder(this).setTitle("Calibrate controller")
            .setMessage("Press the button printed A on your controller. This sets A to type, B to close, X to delete and Y to space. Other bindings are preserved by swapping conflicts. Use this again after changing the handheld's Android controller mode.")
            .setNegativeButton("Cancel",null).create()
        dialog.setOnKeyListener { _,key,event ->
            if(key !in listOf(96,97)) false else {
                if(event.action==android.view.KeyEvent.ACTION_DOWN && event.repeatCount==0) {
                    val prefs=getSharedPreferences("settings",MODE_PRIVATE)
                    val mode=if(key==96) "logical" else "position"
                    var updated=ButtonBindings.read(prefs)
                    val face=ButtonBindings.forLayout("nintendo",mode)
                    for(action in listOf("type","close","delete","space")) updated=ButtonBindings.assign(updated,action,face.getValue(action))
                    val edit=prefs.edit().putString("inputConvention",mode)
                    updated.forEach { (a,c)->edit.putString("map_$a",c.toString()) }; edit.apply()
                    dialog.dismiss(); render()
                }
                true
            }
        }
        dialog.show()
    }
    private fun render() {
        val previousIndex=controls.indexOf(focusedControl()).coerceAtLeast(0); controls.clear()
        val prefs=getSharedPreferences("settings",MODE_PRIVATE)
        fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
        val ink=Color.rgb(231,237,246); val muted=Color.rgb(161,175,196); val accent=Color.rgb(91,220,193)
        fun rounded(color:Int)=GradientDrawable().apply { setColor(color); cornerRadius=dp(18).toFloat() }
        val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(24),dp(20),dp(24)); setBackgroundColor(Color.rgb(13,18,28)) }
        val scroll=ScrollView(this).apply { isFillViewport=true; addView(body) }; setContentView(scroll)
        fun text(parent:LinearLayout, value:String, size:Float=16f, color:Int=ink):TextView = TextView(this).also {
            it.text=value; it.textSize=size; it.setTextColor(color); it.typeface=Typeface.create("sans-serif",Typeface.NORMAL)
            it.setPadding(0,dp(5),0,dp(8)); parent.addView(it)
        }
        fun card(title:String, info:String):LinearLayout {
            val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; background=rounded(Color.rgb(24,32,47)); setPadding(dp(18),dp(14),dp(18),dp(16)) }
            body.addView(card,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(14) })
            val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }; card.addView(header)
            val heading=TextView(this).apply { this.text=title; textSize=19f; setTextColor(ink); typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL) }
            header.addView(heading,LinearLayout.LayoutParams(0,-2,1f))
            header.addView(Button(this).apply {
                this.text="ⓘ"; textSize=21f; setTextColor(accent); background=rounded(Color.rgb(38,50,67)); minWidth=0; minimumWidth=0; setPadding(0,0,0,0)
                contentDescription="Information about "+title
                setOnClickListener { AlertDialog.Builder(this@SetupActivity).setTitle(title).setMessage(info).setPositiveButton("OK",null).show() }
            },LinearLayout.LayoutParams(dp(44),dp(44)))
            return card
        }
        fun button(parent:LinearLayout,title:String,action:()->Unit):Button {
            val control=Button(this).apply {
                this.text=title; isAllCaps=false; textSize=15f; setTextColor(Color.rgb(11,28,28)); typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
                background=rounded(accent); setPadding(dp(18),dp(8),dp(18),dp(8)); setOnClickListener { action() }
            }
            parent.addView(control,LinearLayout.LayoutParams(-2,-2).apply { topMargin=dp(8); bottomMargin=dp(12); marginEnd=dp(10) })
            return control
        }
        fun dropdown(parent:LinearLayout,labels:List<String>,values:List<String>,key:String,default:String,onChanged:()->Unit={}) {
            val selected=values.indexOf(prefs.getString(key,default)).coerceAtLeast(0)
            val spinner=Spinner(this,Spinner.MODE_DROPDOWN)
            spinner.adapter=object:ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,labels) {
                override fun getView(position:Int,convertView:View?,parent:ViewGroup):View = super.getView(position,convertView,parent).apply {
                    (this as TextView).setTextColor(ink); textSize=17f; setPadding(dp(12),dp(12),dp(12),dp(12))
                }
            }.apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spinner.setSelection(selected,false)
            spinner.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p:AdapterView<*>?,v:View?,position:Int,id:Long) {
                    if(prefs.getString(key,default)!=values[position]) { prefs.edit().putString(key,values[position]).apply(); onChanged() }
                }
                override fun onNothingSelected(p:AdapterView<*>?) {}
            }
            parent.addView(spinner,LinearLayout.LayoutParams(-1,dp(56)))
        }
        if(mappingPage) {
            button(body,"‹ Back") { mappingPage=false; render() }
        val mappingCard=card("Controller & button mapping","Choose button labels and map keyboard actions. Assigning a used button swaps the two actions so both remain reachable. D-pad navigation stays available. Bindings apply to all controllers.")
        dropdown(mappingCard,listOf("Nintendo","Xbox","PlayStation"),listOf("nintendo","xbox","playstation"),"buttonLayout","nintendo") { render() }
        val layout=prefs.getString("buttonLayout","nintendo") ?: "nintendo"
        val bindings=ButtonBindings.read(prefs)
        val convention=prefs.getString("inputConvention","logical") ?: "logical"
        text(mappingCard,"Android button reporting",14f,muted)
        dropdown(mappingCard,listOf("Logical A/B/X/Y","Physical positions (Nintendo labels swapped)"),listOf("logical","position"),"inputConvention","logical") { render() }
        button(mappingCard,"Calibrate printed A button") { calibrateButtons() }
        val profileDefaults=ButtonBindings.forLayout(layout,convention)
        val names=mapOf("type" to "Confirm character","close" to "Close keyboard","delete" to "Delete","space" to "Space","up" to "Previous row","down" to "Next row","symbols" to "Symbols","case" to "Upper/lower case","send" to "Editor action","language" to "Switch language")
        for((action,code) in profileDefaults) {
            text(mappingCard,names[action] ?: action,14f,muted)
            dropdown(mappingCard,ButtonBindings.buttons.map { ButtonBindings.label(it,layout,convention) },ButtonBindings.buttons.map { it.toString() },"map_$action",code.toString()) {
                val chosen=prefs.getString("map_$action",code.toString())!!.toInt()
                val update=ButtonBindings.assign(bindings,action,chosen)
                val edit=prefs.edit(); update.forEach { (a,c)->edit.putString("map_$a",c.toString()) }; edit.apply(); render()
            }
        }
        button(mappingCard,"Reset mapping") { val edit=prefs.edit(); ButtonBindings.defaults.keys.forEach { edit.remove("map_$it") }; edit.apply(); render() }
            installFocus(body)
            if(controllerMode && controls.isNotEmpty()) controls[previousIndex.coerceAtMost(controls.lastIndex)].requestFocus()
            return
        }
        text(body,"PadBoard",34f).typeface=Typeface.create("sans-serif-light",Typeface.NORMAL)
        text(body,"Your keyboard. Your controller.",16f,muted)
        val setup=card("Setup","Enable PadBoard in Android and select it as your input method. Keep your previous keyboard available.")
        enableButton=button(setup,"1 · Enable") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        selectButton=button(setup,"2 · Select") { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }
        refreshSetup()
        button(body,"Button Mapping  ›") { mappingPage=true; render() }.apply {
            (layoutParams as LinearLayout.LayoutParams).bottomMargin=dp(24)
        }
        val languages=card("Languages","Typing language selects QWERTZ or QWERTY. Interface language only changes settings. More languages will follow.")
        text(languages,"Typing language",14f,muted)
        dropdown(languages,listOf("Deutsch · QWERTZ","English · QWERTY"),listOf("de","en"),"language","de")

        val design=card("Appearance","Height is a fraction of full display height: 50% of 1080 px is 540 px. Android system bars can limit usable space. Changes apply when the keyboard next opens.")
        dropdown(design,Palette.names,Palette.ids,"theme","oled")
        design.addView(CheckBox(this).apply {
            text="Show number row above keyboard"
            setTextColor(ink); textSize=16f; isChecked=prefs.getBoolean("numberRow",false)
            setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("numberRow",checked).apply() }
        },LinearLayout.LayoutParams(-1,-2))
        val size=text(design,"",16f)
        val scale=SeekBar(this).apply { max=80; progress=prefs.getInt("heightPercent",35)-20 }
        fun update(v:Int) { size.text=getString(R.string.size_en,v) }
        update(scale.progress+20)
        scale.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar:SeekBar?,v:Int,user:Boolean) { prefs.edit().putInt("heightPercent",v+20).apply(); update(v+20) }
            override fun onStartTrackingTouch(bar:SeekBar?) {}
            override fun onStopTrackingTouch(bar:SeekBar?) {}
        }); design.addView(scale)
        button(design,"Reset to 35%") { scale.progress=15 }
        val feedback=card("Feedback & privacy","Vibration: a very short pulse after the editor accepts input, for touch and controller. Strength depends on hardware and respects Android's haptic setting. Preview displays available context, never passwords. Typed text is not saved.")
        fun toggle(title:String,key:String) { feedback.addView(Switch(this).apply {
            this.text=title; textSize=16f; setTextColor(ink); setPadding(0,dp(12),0,dp(12)); isChecked=prefs.getBoolean(key,true)
            setOnCheckedChangeListener { _,checked->prefs.edit().putBoolean(key,checked).apply() }
        },LinearLayout.LayoutParams(-1,-2)) }
        toggle("Vibration feedback","haptic")
        text(feedback,"Vibration strength",14f,muted)
        val strengths=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        feedback.addView(strengths)
        val strengthButtons=mutableListOf<Button>()
        fun refreshStrengths() {
            strengthButtons.forEach { control ->
                val selected=prefs.getString("hapticStrength","light")==control.getTag(R.id.vibration_strength)
                control.tag="choice:$selected"
                val normal=rounded(if(selected) accent else Color.TRANSPARENT).apply { if(!selected) setStroke(dp(2),accent) }
                val focused=rounded(if(selected) accent else Color.TRANSPARENT).apply { setStroke(dp(3),Color.rgb(174,255,231)) }
                control.background=android.graphics.drawable.StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_focused),focused); addState(intArrayOf(),normal)
                }
                control.setTextColor(if(selected) Color.rgb(11,28,28) else accent)
            }
        }
        for((value,label) in listOf("light" to "Light","medium" to "Medium","strong" to "Strong")) {
            val selected=prefs.getString("hapticStrength","light")==value
            button(strengths,label) { prefs.edit().putString("hapticStrength",value).apply(); refreshStrengths() }.apply {
                setTag(R.id.vibration_strength,value); strengthButtons.add(this)
                tag="choice:$selected"
                background=rounded(if(selected) accent else Color.TRANSPARENT).apply { if(!selected) setStroke(dp(2),accent) }
                setTextColor(if(selected) Color.rgb(11,28,28) else accent)
            }
        }
        val tests=card("Try it","Try non-private text first, then your target apps. The email field offers @ and domain endings. Avoid real passwords while testing.")
        fun field(hint:String,type:Int,action:Int) { tests.addView(EditText(this).apply {
            this.hint=hint; inputType=type; imeOptions=action; textSize=17f; setTextColor(ink); setHintTextColor(muted)
            setPadding(dp(12),dp(10),dp(12),dp(10)); background=rounded(Color.rgb(35,45,62))
        },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(10) }) }
        field("Search",InputType.TYPE_CLASS_TEXT,android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)
        field("E-Mail",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,android.view.inputmethod.EditorInfo.IME_ACTION_NEXT)
        field("URL",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,android.view.inputmethod.EditorInfo.IME_ACTION_GO)
        field("Multiline text",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,android.view.inputmethod.EditorInfo.IME_ACTION_NONE)
        field("Password",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
        val support=card("Support the project","Optional tips through Ko-fi. The link opens the external Ko-fi page in your browser. All keyboard features remain available for free.")
        button(support,"Support SilasDV on Ko-fi ↗") {
            try { startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://ko-fi.com/silasdv"))) }
            catch (_:android.content.ActivityNotFoundException) { Toast.makeText(this,"No browser available",Toast.LENGTH_SHORT).show() }
        }
        text(body,"1.0.1",12f,muted)
        installFocus(body)
        if (controllerMode && controls.isNotEmpty()) controls[previousIndex.coerceAtMost(controls.lastIndex)].requestFocus()
    }
}
