package com.example.nerkh

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import kotlin.concurrent.thread
import kotlin.math.min

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var grid: GridLayout
    private lateinit var status: TextView
    private lateinit var pull: PullScrollView
    private lateinit var scrim: View
    private lateinit var sheet: View
    private lateinit var sheetList: LinearLayout
    private lateinit var menuCard: View
    private lateinit var menuDismiss: View
    private var all: List<Rate> = emptyList()
    private var shown: List<String> = emptyList()
    private val cardViews = HashMap<String, View>()
    private var sheetOpen = false
    private var convOpen = false
    private var menuOpen = false
    private var sheetH = 0
    private lateinit var convSheet: View
    private lateinit var convFromBtn: TextView
    private lateinit var convToBtn: TextView
    private lateinit var convInput: EditText
    private lateinit var convResult: TextView
    private lateinit var convPickerScroll: View
    private lateinit var convPickerList: LinearLayout
    private var convFrom = "USD"
    private var convTo = "LTC"
    private var pickingFrom = true
    private val tick = object : Runnable { override fun run() { fetch(); h.postDelayed(this, 30_000) } }

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)
        grid = findViewById(R.id.grid)
        status = findViewById(R.id.status)
        pull = findViewById(R.id.pull)
        pull.spinner = findViewById(R.id.spinner)
        pull.onRefresh = { status.text = "Updating…"; fetch() }
        scrim = findViewById(R.id.scrim)
        sheet = findViewById(R.id.sheet)
        sheetList = findViewById(R.id.sheetList)
        menuCard = findViewById(R.id.menuCard)
        menuDismiss = findViewById(R.id.menuDismiss)

        val plus = findViewById<View>(R.id.plus)
        val calc = findViewById<View>(R.id.calc)
        val more = findViewById<View>(R.id.more)
        val done = findViewById<View>(R.id.done)
        status.setOnClickListener { pull.startRefresh() }
        plus.setOnClickListener { openSheet() }
        calc.setOnClickListener { openConv() }
        more.setOnClickListener { openMenu() }
        done.setOnClickListener { closeSheet() }
        scrim.setOnClickListener { if (convOpen) closeConv() else closeSheet() }
        sheet.setOnClickListener { }
        menuDismiss.setOnClickListener { closeMenu() }
        findViewById<View>(R.id.mi_calc).setOnClickListener { closeMenu(); openConv() }
        findViewById<View>(R.id.mi_refresh).setOnClickListener { closeMenu(); pull.startRefresh() }
        findViewById<View>(R.id.mi_choose).setOnClickListener { closeMenu(); openSheet() }
        findViewById<View>(R.id.mi_all).setOnClickListener {
            closeMenu(); prefs().edit().remove("sel").apply(); render()
        }
        bounce(plus, 0.8f); bounce(calc, 0.8f); bounce(more, 0.8f); bounce(done, 0.9f)

        convSheet = findViewById(R.id.convSheet)
        convFromBtn = findViewById(R.id.convFromBtn)
        convToBtn = findViewById(R.id.convToBtn)
        convInput = findViewById(R.id.convInput)
        convResult = findViewById(R.id.convResult)
        convPickerScroll = findViewById(R.id.convPickerScroll)
        convPickerList = findViewById(R.id.convPickerList)
        val convDone = findViewById<View>(R.id.convDone)
        val convSwap = findViewById<View>(R.id.convSwap)
        convSheet.setOnClickListener { }
        convDone.setOnClickListener { closeConv() }
        bounce(convDone, 0.9f)
        convFromBtn.setOnClickListener { pickingFrom = true; showConvPicker() }
        convToBtn.setOnClickListener { pickingFrom = false; showConvPicker() }
        convSwap.setOnClickListener {
            val tmp = convFrom; convFrom = convTo; convTo = tmp
            convSwap.animate().rotationBy(180f).setDuration(260).setInterpolator(OvershootInterpolator(1.2f)).start()
            updateConvUI()
        }
        convInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateConvResult() }
            override fun afterTextChanged(s: Editable?) {}
        })

        val pill = findViewById<View>(R.id.pill)
        pill.translationY = dp(130).toFloat()
        pill.animate().translationY(0f).setStartDelay(200).setDuration(650)
            .setInterpolator(OvershootInterpolator(1.1f)).start()

        all = Rates.cached(this)
        render()
    }

    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when {
            convOpen -> closeConv()
            sheetOpen -> closeSheet()
            menuOpen -> closeMenu()
            else -> super.onBackPressed()
        }
    }

    private fun prefs() = getSharedPreferences("ui", 0)

    private fun fetch() {
        thread {
            val list = Rates.fetch(this)
            runOnUiThread {
                if (list != null) {
                    all = list
                    render()
                    sendBroadcast(Intent(this, RatesWidget::class.java).setAction(RatesWidget.REDRAW))
                    sendBroadcast(Intent(this, RatesWidgetSingle::class.java).setAction(RatesWidgetSingle.REDRAW))
                } else status.text = "offline"
                pull.endRefresh()
            }
        }
    }

    private fun selection(): Set<String>? = prefs().getString("sel", null)?.split(",")?.toSet()

    private fun setVisible(code: String, on: Boolean) {
        val cat = Rates.catalog.map { it.code }
        val cur = (selection() ?: cat.toSet()).toMutableSet()
        if (on) cur.add(code) else cur.remove(code)
        if (cur.size == cat.size) prefs().edit().remove("sel").apply()
        else prefs().edit().putString("sel", cat.filter { it in cur }.joinToString(",")).apply()
    }

    // ---------------- کارت‌ها ----------------
    private fun fill(v: View, r: Rate, animateChange: Boolean = false) {
        val icon = v.findViewById<TextView>(R.id.icon)
        icon.text = r.icon
        if (r.bg != 0) icon.setBackgroundResource(r.bg) else icon.background = null
        v.findViewById<TextView>(R.id.name).text = r.name
        v.findViewById<TextView>(R.id.code).text = r.code
        val c = v.findViewById<TextView>(R.id.change)
        c.text = Rates.changeText(r); c.setTextColor(Rates.changeColor(r))
        val pr = v.findViewById<TextView>(R.id.price)
        pr.text = Rates.priceText(r)

        if (animateChange) {
            pr.alpha = 0.4f
            pr.scaleX = 0.95f
            pr.scaleY = 0.95f
            pr.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(350).setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun render() {
        val sel = selection()
        val visible = all.filter { sel == null || it.code in sel }
        val codes = visible.map { it.code }
        if (codes == shown && grid.childCount == codes.size) {
            for (r in visible) cardViews[r.code]?.let { fill(it, r, animateChange = true) }
        } else {
            grid.removeAllViews(); cardViews.clear(); shown = codes
            val inf = LayoutInflater.from(this)
            visible.forEachIndexed { i, r ->
                val v = inf.inflate(R.layout.item_rate, grid, false)
                fill(v, r, animateChange = false)
                v.isClickable = true
                bounce(v, 0.95f)
                val lp = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f))
                lp.width = 0; lp.setMargins(6, 6, 6, 6)
                grid.addView(v, lp)
                cardViews[r.code] = v
                v.alpha = 0f; v.translationY = dp(28).toFloat(); v.scaleX = 0.92f; v.scaleY = 0.92f
                v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setStartDelay(min(i, 14) * 35L).setDuration(450)
                    .setInterpolator(DecelerateInterpolator(2.2f)).start()
            }
        }
        status.text = Rates.ago(this)
    }

    // ---------------- انیمیشن لمس ----------------
    private fun bounce(v: View, s: Float) {
        v.setOnTouchListener { view, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> view.animate().scaleX(s).scaleY(s).setStartDelay(0).setDuration(90)
                    .setInterpolator(DecelerateInterpolator()).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.animate().scaleX(1f).scaleY(1f)
                    .setStartDelay(0).setDuration(300).setInterpolator(OvershootInterpolator(3f)).start()
            }
            false
        }
    }

    // ---------------- صفحه انتخاب ارزها ----------------
    private fun buildRows() {
        sheetList.removeAllViews()
        val sel = selection()
        val allItems = Rates.catalog

        val groups = mapOf(
            "Currencies" to allItems.filter { it.category == Rates.CAT_FIAT },
            "Gold & Coins" to allItems.filter { it.category == Rates.CAT_GOLD },
            "Cryptocurrencies" to allItems.filter { it.category == Rates.CAT_CRYPTO }
        )

        for ((groupName, items) in groups) {
            if (items.isEmpty()) continue

            val header = TextView(this).apply {
                text = groupName
                setTextColor(Color.parseColor("#8E8E93"))
                textSize = 12f
                setPadding(dp(22), dp(14), dp(22), dp(4))
                isAllCaps = true
            }
            sheetList.addView(header)

            for (c in items) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(22), dp(9), dp(22), dp(9))
                }

                val icon = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
                    gravity = Gravity.CENTER
                    text = c.icon
                    textSize = 19f
                    setTextColor(-1)
                    if (c.bg != 0) setBackgroundResource(c.bg)
                }

                val col = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    val clp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    clp.marginStart = dp(14)
                    layoutParams = clp
                }
                val n = TextView(this).apply { text = c.name; textSize = 16f; setTextColor(-1) }
                val k = TextView(this).apply { text = c.code; textSize = 12f; setTextColor(0xFF8E8E93.toInt()) }
                col.addView(n); col.addView(k)

                val sw = Switch(this).apply {
                    isChecked = sel == null || c.code in sel
                    thumbTintList = ColorStateList.valueOf(-1)
                    trackTintList = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(0xFF30D158.toInt(), 0xFF3A3A3C.toInt())
                    )
                    setOnCheckedChangeListener { _, on -> setVisible(c.code, on) }
                }
                row.setOnClickListener { sw.toggle() }

                row.addView(icon); row.addView(col); row.addView(sw)
                sheetList.addView(row)
            }
        }
    }

    private fun openSheet() {
        if (sheetOpen) return
        sheetOpen = true
        buildRows()
        sheetH = (resources.displayMetrics.heightPixels * 0.82f).toInt()
        (sheet.layoutParams as FrameLayout.LayoutParams).height = sheetH
        sheet.requestLayout()
        scrim.visibility = View.VISIBLE; scrim.alpha = 0f
        scrim.animate().alpha(1f).setDuration(280).start()
        sheet.visibility = View.VISIBLE; sheet.translationY = sheetH.toFloat()
        sheet.animate().translationY(0f).setDuration(480).setInterpolator(DecelerateInterpolator(2.2f)).start()
        for (i in 0 until min(sheetList.childCount, 15)) {
            val v = sheetList.getChildAt(i)
            v.alpha = 0f; v.translationY = dp(20).toFloat()
            v.animate().alpha(1f).translationY(0f).setStartDelay(140L + i * 25L)
                .setDuration(320).setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun closeSheet() {
        if (!sheetOpen) return
        sheetOpen = false
        scrim.animate().alpha(0f).setDuration(240).withEndAction { scrim.visibility = View.GONE }.start()
        sheet.animate().translationY(sheetH.toFloat()).setDuration(300).setInterpolator(AccelerateInterpolator(1.6f))
            .withEndAction { sheet.visibility = View.GONE; render() }.start()
    }

    // ---------------- منوی ••• ----------------
    private fun openMenu() {
        if (menuOpen) return
        menuOpen = true
        menuDismiss.visibility = View.VISIBLE
        menuCard.alpha = 0f
        menuCard.visibility = View.VISIBLE
        menuCard.post {
            menuCard.pivotX = menuCard.width.toFloat(); menuCard.pivotY = menuCard.height.toFloat()
            menuCard.scaleX = 0.5f; menuCard.scaleY = 0.5f
            menuCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(320)
                .setInterpolator(OvershootInterpolator(1.4f)).start()
        }
    }

    private fun closeMenu() {
        if (!menuOpen) return
        menuOpen = false
        menuDismiss.visibility = View.GONE
        menuCard.animate().alpha(0f).scaleX(0.7f).scaleY(0.7f).setDuration(160)
            .withEndAction { menuCard.visibility = View.GONE }.start()
    }

    // ---------------- ماشین حساب تبدیل ارز ----------------
    private fun labelFor(code: String): String {
        if (code == "IRT") return "🇮🇷 IRT ▾"
        val c = Rates.catalog.firstOrNull { it.code == code }
        return if (c != null) "${c.icon} ${c.code} ▾" else "$code ▾"
    }

    private fun updateConvUI() {
        convFromBtn.text = labelFor(convFrom)
        convToBtn.text = labelFor(convTo)
        updateConvResult()
    }

    private fun updateConvResult() {
        val amt = convInput.text.toString().toDoubleOrNull() ?: 0.0
        val res = Rates.convert(amt, convFrom, convTo, all)
        convResult.text = if (res != null) Rates.formatConverted(res) else "---"
    }

    private fun showConvPicker() {
        convPickerList.removeAllViews()
        val items = listOf(Rates.Cat("IRT", "Iranian Toman", "🇮🇷", 0, Rates.CAT_FIAT)) + Rates.catalog
        for (c in items) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(22), dp(10), dp(22), dp(10))
            }
            val icon = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
                gravity = Gravity.CENTER
                text = c.icon
                textSize = 17f
                setTextColor(-1)
                if (c.bg != 0) setBackgroundResource(c.bg)
            }
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val clp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                clp.marginStart = dp(12)
                layoutParams = clp
            }
            val n = TextView(this).apply { text = c.name; textSize = 15f; setTextColor(-1) }
            val k = TextView(this).apply { text = c.code; textSize = 11f; setTextColor(0xFF8E8E93.toInt()) }
            col.addView(n); col.addView(k)
            row.addView(icon); row.addView(col)
            row.setOnClickListener {
                if (pickingFrom) convFrom = c.code else convTo = c.code
                convPickerScroll.visibility = View.GONE
                updateConvUI()
            }
            convPickerList.addView(row)
        }
        convPickerScroll.visibility = View.VISIBLE
        convPickerScroll.alpha = 0f
        convPickerScroll.animate().alpha(1f).setDuration(200).start()
    }

    private fun openConv() {
        if (convOpen) return
        convOpen = true
        convPickerScroll.visibility = View.GONE
        updateConvUI()
        sheetH = (resources.displayMetrics.heightPixels * 0.82f).toInt()
        (convSheet.layoutParams as FrameLayout.LayoutParams).height = sheetH
        convSheet.requestLayout()
        scrim.visibility = View.VISIBLE; scrim.alpha = 0f
        scrim.animate().alpha(1f).setDuration(280).start()
        convSheet.visibility = View.VISIBLE; convSheet.translationY = sheetH.toFloat()
        convSheet.animate().translationY(0f).setDuration(480).setInterpolator(DecelerateInterpolator(2.2f)).start()
    }

    private fun closeConv() {
        if (!convOpen) return
        convOpen = false
        scrim.animate().alpha(0f).setDuration(240).withEndAction { scrim.visibility = View.GONE }.start()
        convSheet.animate().translationY(sheetH.toFloat()).setDuration(300).setInterpolator(AccelerateInterpolator(1.6f))
            .withEndAction { convSheet.visibility = View.GONE }.start()
    }
}
