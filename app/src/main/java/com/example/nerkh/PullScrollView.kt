package com.example.nerkh

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ScrollView
import kotlin.math.exp
import kotlin.math.min

/** کشیدن به پایین مثل آیفون: خود کارت‌ها با انگشت پایین میان و موقع رها کردن فنری برمی‌گردن */
class PullScrollView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : ScrollView(ctx, attrs) {
    var onRefresh: (() -> Unit)? = null
    var spinner: View? = null
    var refreshing = false
        private set

    private val density = resources.displayMetrics.density
    private val threshold = 72 * density
    private val hold = 64 * density
    private val maxPull = 220 * density
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private var startY = 0f
    private var tracking = false
    private var pulling = false
    private var startedAt = 0L

    private val content: View? get() = getChildAt(0)

    private fun applyPull(pull: Float) {
        content?.translationY = pull
        spinner?.let { s ->
            s.visibility = if (pull > 1f || refreshing) View.VISIBLE else View.INVISIBLE
            s.alpha = if (refreshing) 1f else min(1f, pull / threshold)
            s.rotation = pull * 2f
        }
    }

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startY = e.y
                tracking = scrollY == 0 && !refreshing
                pulling = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (tracking && !pulling && scrollY == 0 && e.y - startY > slop) {
                    pulling = true
                    startY = e.y
                    content?.animate()?.cancel()
                    spinner?.animate()?.cancel()
                    val c = MotionEvent.obtain(e)
                    c.action = MotionEvent.ACTION_CANCEL
                    super.dispatchTouchEvent(c)
                    c.recycle()
                }
                if (pulling) {
                    val d = (e.y - startY).coerceAtLeast(0f)
                    applyPull(maxPull * (1f - exp(-d / (2f * maxPull))))
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                tracking = false
                if (pulling) {
                    pulling = false
                    val pull = content?.translationY ?: 0f
                    if (e.actionMasked == MotionEvent.ACTION_UP && pull >= threshold) startRefresh() else settle()
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(e)
    }

    /** از بیرون هم می‌شه صدا زد (دکمه منو): انیمیشن کشیدن + شروع رفرش */
    fun startRefresh() {
        if (refreshing) return
        refreshing = true
        startedAt = System.currentTimeMillis()
        spinner?.animate()?.cancel()
        spinner?.apply { visibility = View.VISIBLE; alpha = 1f }
        content?.animate()?.translationY(hold)?.setDuration(260)?.setInterpolator(DecelerateInterpolator())?.start()
        onRefresh?.invoke()
    }

    fun endRefresh() {
        if (!refreshing) return
        val wait = 700 - (System.currentTimeMillis() - startedAt)
        postDelayed({ refreshing = false; settle() }, wait.coerceAtLeast(0))
    }

    private fun settle() {
        content?.animate()?.translationY(0f)?.setDuration(450)?.setInterpolator(OvershootInterpolator(1.2f))?.start()
        spinner?.animate()?.alpha(0f)?.setDuration(260)?.withEndAction { spinner?.visibility = View.INVISIBLE }?.start()
    }
}
