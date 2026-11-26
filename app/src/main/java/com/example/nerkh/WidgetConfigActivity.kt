package com.example.nerkh

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView

class WidgetConfigActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isSingle = false
    private val selectedCodes = mutableListOf<String>()
    private lateinit var itemsList: LinearLayout
    private lateinit var selectionBox: LinearLayout
    private lateinit var subtitle: TextView

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // بررسی اینکه آیا ویجت تکی است یا دوتایی
        val mgr = AppWidgetManager.getInstance(this)
        val info = mgr.getAppWidgetInfo(appWidgetId)
        val providerName = info?.provider?.className ?: ""
        isSingle = providerName.contains("Single")

        itemsList = findViewById(R.id.config_items_list)
        selectionBox = findViewById(R.id.config_selection_box)
        subtitle = findViewById(R.id.config_subtitle)

        subtitle.text = if (isSingle) "Select 1 currency for this single widget" else "Select 2 currencies for this double widget"

        // خواندن انتخاب‌های قبلی
        val saved = getWidgetItems(this, appWidgetId, isSingle)
        selectedCodes.addAll(saved)

        val doneBtn = findViewById<View>(R.id.config_save_btn)
        doneBtn.setOnClickListener { saveAndFinish() }

        renderSelectionChips()
        populateList()
    }

    private fun renderSelectionChips() {
        selectionBox.removeAllViews()
        for (code in selectedCodes) {
            val item = Rates.catalog.firstOrNull { it.code == code } ?: continue
            val chip = TextView(this).apply {
                text = "${item.icon} ${item.code}"
                setTextColor(Color.WHITE)
                textSize = 14f
                setBackgroundResource(R.drawable.card_bg)
                setPadding(dp(14), dp(8), dp(14), dp(8))
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = dp(8)
                layoutParams = lp
            }
            selectionBox.addView(chip)
        }
    }

    private fun populateList() {
        itemsList.removeAllViews()
        val allItems = Rates.catalog

        // دسته‌بندی
        val groups = mapOf(
            "Currencies" to allItems.filter { it.category == Rates.CAT_FIAT },
            "Gold & Coins" to allItems.filter { it.category == Rates.CAT_GOLD },
            "Cryptocurrencies" to allItems.filter { it.category == Rates.CAT_CRYPTO }
        )

        for ((groupName, items) in groups) {
            if (items.isEmpty()) continue

            // عنوان دسته‌بندی
            val header = TextView(this).apply {
                text = groupName
                setTextColor(Color.parseColor("#8E8E93"))
                textSize = 13spToPx()
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                setPadding(dp(20), dp(16), dp(20), dp(6))
                isAllCaps = true
            }
            itemsList.addView(header)

            for (c in items) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(20), dp(10), dp(20), dp(10))
                    isClickable = true
                    isFocusable = true
                }

                val icon = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
                    gravity = Gravity.CENTER
                    text = c.icon
                    textSize = 18f
                    setTextColor(Color.WHITE)
                    if (c.bg != 0) setBackgroundResource(c.bg)
                }

                val col = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    val clp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    clp.marginStart = dp(14)
                    layoutParams = clp
                }
                val n = TextView(this).apply { text = c.name; textSize = 15f; setTextColor(Color.WHITE) }
                val k = TextView(this).apply { text = c.code; textSize = 11f; setTextColor(Color.parseColor("#8E8E93")) }
                col.addView(n); col.addView(k)

                val check = TextView(this).apply {
                    textSize = 17f
                    setTextColor(Color.parseColor("#FFD60A"))
                    text = if (c.code in selectedCodes) "✓" else ""
                }

                row.addView(icon); row.addView(col); row.addView(check)

                row.setOnClickListener {
                    toggle(c.code)
                    populateList()
                    renderSelectionChips()
                }

                itemsList.addView(row)
            }
        }
    }

    private fun 13spToPx(): Float = 13f

    private fun toggle(code: String) {
        if (isSingle) {
            selectedCodes.clear()
            selectedCodes.add(code)
        } else {
            if (code in selectedCodes) {
                if (selectedCodes.size > 1) selectedCodes.remove(code)
            } else {
                if (selectedCodes.size >= 2) selectedCodes.removeAt(0)
                selectedCodes.add(code)
            }
        }
    }

    private fun saveAndFinish() {
        val sp = getSharedPreferences("widgets_pref", Context.MODE_PRIVATE).edit()
        sp.putString("widget_${appWidgetId}", selectedCodes.joinToString(","))
        sp.apply()

        // بروزرسانی ویجت
        val mgr = AppWidgetManager.getInstance(this)
        if (isSingle) {
            RatesWidgetSingle.updateAppWidget(this, mgr, appWidgetId)
        } else {
            RatesWidget.updateAppWidget(this, mgr, appWidgetId)
        }

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }

    companion object {
        fun getWidgetItems(ctx: Context, widgetId: Int, isSingle: Boolean): List<String> {
            val sp = ctx.getSharedPreferences("widgets_pref", Context.MODE_PRIVATE)
            val saved = sp.getString("widget_$widgetId", null)
            if (saved != null) {
                return saved.split(",").filter { it.isNotEmpty() }
            }
            return if (isSingle) listOf("USD") else listOf("USD", "USDT")
        }
    }
}
