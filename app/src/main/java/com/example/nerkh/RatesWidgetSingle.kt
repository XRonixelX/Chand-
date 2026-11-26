package com.example.nerkh

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlin.concurrent.thread

class RatesWidgetSingle : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            updateAppWidget(ctx, mgr, id)
        }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, RatesWidgetSingle::class.java))
        when (intent.action) {
            REDRAW -> {
                val list = Rates.cached(ctx)
                for (id in ids) draw(ctx, mgr, id, list)
            }
            REFRESH -> {
                thread {
                    val list = Rates.fetch(ctx) ?: Rates.cached(ctx)
                    for (id in ids) draw(ctx, mgr, id, list)
                }
            }
        }
    }

    companion object {
        const val REFRESH = "com.example.nerkh.SINGLE_REFRESH"
        const val REDRAW = "com.example.nerkh.SINGLE_REDRAW"

        fun updateAppWidget(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
            val list = Rates.cached(ctx)
            if (list.isNotEmpty()) {
                draw(ctx, mgr, widgetId, list)
            }
            thread {
                val fresh = Rates.fetch(ctx)
                if (fresh != null) {
                    draw(ctx, mgr, widgetId, fresh)
                }
            }
        }

        private fun draw(ctx: Context, mgr: AppWidgetManager, id: Int, list: List<Rate>) {
            val v = RemoteViews(ctx.packageName, R.layout.widget_rates_single)
            val selected = WidgetConfigActivity.getWidgetItems(ctx, id, isSingle = true)
            val code = selected.firstOrNull() ?: "USD"
            val r = list.firstOrNull { it.code == code }

            if (r != null) {
                v.setTextViewText(R.id.w_icon, r.icon)
                v.setInt(R.id.w_icon, "setBackgroundResource", r.bg)
                v.setTextViewText(R.id.w_name, r.name)
                v.setTextViewText(R.id.w_code, r.code)
                v.setTextViewText(R.id.w_change, Rates.changeText(r))
                v.setTextColor(R.id.w_change, Rates.changeColor(r))
                v.setTextViewText(R.id.w_price, Rates.priceText(r))
            }

            val f = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val mainIntent = Intent(ctx, MainActivity::class.java)
            v.setOnClickPendingIntent(R.id.widget_single_root, PendingIntent.getActivity(ctx, id, mainIntent, f))

            mgr.updateAppWidget(id, v)
        }
    }
}
