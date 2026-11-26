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
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            updateAppWidget(ctx, mgr, id)
        }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, RatesWidget::class.java))
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
        const val REFRESH = "com.example.nerkh.REFRESH"
        const val REDRAW = "com.example.nerkh.REDRAW"

        private val icon = intArrayOf(R.id.i0, R.id.i1)
        private val name = intArrayOf(R.id.nm0, R.id.nm1)
        private val code = intArrayOf(R.id.cd0, R.id.cd1)
        private val chg = intArrayOf(R.id.ch0, R.id.ch1)
        private val price = intArrayOf(R.id.pr0, R.id.pr1)

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
            val v = RemoteViews(ctx.packageName, R.layout.widget_rates)
            val show = WidgetConfigActivity.getWidgetItems(ctx, id, isSingle = false)

            show.forEachIndexed { i, c ->
                if (i < 2) {
                    val r = list.firstOrNull { it.code == c }
                    if (r != null) {
                        v.setTextViewText(icon[i], r.icon)
                        v.setInt(icon[i], "setBackgroundResource", r.bg)
                        v.setTextViewText(name[i], r.name)
                        v.setTextViewText(code[i], r.code)
                        v.setTextViewText(chg[i], Rates.changeText(r))
                        v.setTextColor(chg[i], Rates.changeColor(r))
                        v.setTextViewText(price[i], Rates.priceText(r))
                    }
                }
            }
            val f = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(ctx, id, Intent(ctx, MainActivity::class.java), f))
            mgr.updateAppWidget(id, v)
        }
    }
}
