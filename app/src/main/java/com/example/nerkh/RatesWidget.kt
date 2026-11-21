package com.example.nerkh

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlin.concurrent.thread

class RatesWidget : AppWidgetProvider() {
    private val icon = intArrayOf(R.id.i0, R.id.i1)
    private val name = intArrayOf(R.id.nm0, R.id.nm1)
    private val code = intArrayOf(R.id.cd0, R.id.cd1)
    private val chg = intArrayOf(R.id.ch0, R.id.ch1)
    private val price = intArrayOf(R.id.pr0, R.id.pr1)
    private val show = listOf("USD", "USDT")   // ویجت فقط دلار و تتر

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = refresh(ctx, mgr, ids)

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, RatesWidget::class.java))
        when (intent.action) {
            REDRAW -> draw(ctx, mgr, ids, Rates.cached(ctx))
            REFRESH -> refresh(ctx, mgr, ids)
        }
    }

    private fun refresh(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        draw(ctx, mgr, ids, Rates.cached(ctx))
        val pending = goAsync()
        thread {
            Rates.fetch(ctx)?.let { draw(ctx, mgr, ids, it) }
            pending.finish()
        }
    }

    private fun draw(ctx: Context, mgr: AppWidgetManager, ids: IntArray, list: List<Rate>) {
        val v = RemoteViews(ctx.packageName, R.layout.widget_rates)
        show.forEachIndexed { i, c ->
            val r = list.firstOrNull { it.code == c } ?: return@forEachIndexed
            v.setTextViewText(icon[i], r.icon)
            v.setInt(icon[i], "setBackgroundResource", r.bg)
            v.setTextViewText(name[i], r.name)
            v.setTextViewText(code[i], r.code)
            v.setTextViewText(chg[i], Rates.changeText(r))
            v.setTextColor(chg[i], Rates.changeColor(r))
            v.setTextViewText(price[i], Rates.priceText(r))
        }
        val f = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(ctx, 1, Intent(ctx, MainActivity::class.java), f))
        mgr.updateAppWidget(ids, v)
    }

    companion object {
        const val REFRESH = "com.example.nerkh.REFRESH"
        const val REDRAW = "com.example.nerkh.REDRAW"
    }
}
