package com.example.nerkh

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs

/** value و change به تومان (یا دلار برای انس) هستند؛ نمایش در زمان رسم ساخته می‌شود. dec=-1 یعنی خودکار */
data class Rate(val name: String, val code: String, val icon: String, val bg: Int,
                val value: Double, val change: Double, val dir: Int, val dec: Int)

object Rates {
    // ---- تنظیمات منبع داده ----
    const val URL_JSON = "https://call3.tgju.org/ajax.json"

    private class Item(val name: String, val code: String, val icon: String, val bg: Int,
                       val keys: List<String>, val contains: String? = null,
                       val crypto: Double = 0.0, val usd: Boolean = false)

    private fun fiat(name: String, code: String, flag: String, key: String) = Item(name, code, flag, 0, listOf(key))
    private fun gold(name: String, code: String, key: String, usd: Boolean = false) =
        Item(name, code, "", R.drawable.ic_gold, listOf(key), usd = usd)

    // ترتیب: ۸ تای اول مثل عکس، بقیه بعدش
    private val ITEMS = listOf(
        fiat("US Dollar", "USD", "🇺🇸", "price_dollar_rl"),
        fiat("Euro", "EUR", "🇪🇺", "price_eur"),
        Item("Tether USDT", "USDT", "₮", R.drawable.ic_green, listOf("crypto-tether-irr"), "tether", 5000.0),
        gold("Mithqal", "MITHQAL", "mesghal"),
        fiat("British Pound", "GBP", "🇬🇧", "price_gbp"),
        Item("Bitcoin", "BTC", "₿", R.drawable.ic_orange, listOf("crypto-bitcoin-irr"), "bitcoin", 5_000_000.0),
        gold("Azadi", "AZADI", "sekeb"),
        fiat("Chinese Yuan", "CNY", "🇨🇳", "price_cny"),
        gold("Emami Coin", "EMAMI", "sekee"),
        gold("Half Azadi", "NIM", "nim"),
        gold("Quarter Azadi", "ROB", "rob"),
        gold("Gram Coin", "GERAMI", "gerami"),
        gold("18K Gold Gram", "GOLD18", "geram18"),
        gold("24K Gold Gram", "GOLD24", "geram24"),
        gold("Gold Ounce", "ONS", "ons", usd = true),
        Item("Ethereum", "ETH", "Ξ", R.drawable.ic_blue, listOf("crypto-ethereum-irr"), "ethereum", 1_000_000.0),
        fiat("UAE Dirham", "AED", "🇦🇪", "price_aed"),
        fiat("Turkish Lira", "TRY", "🇹🇷", "price_try"),
        fiat("Canadian Dollar", "CAD", "🇨🇦", "price_cad"),
        fiat("Australian Dollar", "AUD", "🇦🇺", "price_aud"),
        fiat("Swiss Franc", "CHF", "🇨🇭", "price_chf"),
        fiat("Japanese Yen", "JPY", "🇯🇵", "price_jpy"),
        fiat("Russian Ruble", "RUB", "🇷🇺", "price_rub"),
        fiat("Saudi Riyal", "SAR", "🇸🇦", "price_sar"),
        fiat("Iraqi Dinar", "IQD", "🇮🇶", "price_iqd"),
        fiat("Afghan Afghani", "AFN", "🇦🇫", "price_afn"),
        fiat("Indian Rupee", "INR", "🇮🇳", "price_inr"),
        fiat("Kuwaiti Dinar", "KWD", "🇰🇼", "price_kwd"),
        fiat("Armenian Dram", "AMD", "🇦🇲", "price_amd"),
        fiat("Azerbaijani Manat", "AZN", "🇦🇿", "price_azn"),
    )

    data class Cat(val code: String, val name: String, val icon: String, val bg: Int)

    /** لیست همه ارزها برای صفحه انتخاب */
    val catalog: List<Cat> get() = ITEMS.map { Cat(it.code, it.name, it.icon, it.bg) }

    const val UP = "#FF453A"     // مثل عکس: بالا قرمز
    const val DOWN = "#30D158"   // پایین سبز
    private const val PREFS = "rates"

    private fun num(s: String?) = s?.replace(",", "")?.trim()?.toDoubleOrNull()

    private fun find(cur: JSONObject, item: Item): JSONObject? {
        for (k in item.keys) cur.optJSONObject(k)?.let { o -> return o }
        val sub = item.contains ?: return null
        val matches = cur.keys().asSequence().filter { k -> k.contains(sub, true) }.toList()
        val key = matches.firstOrNull { k -> k.contains("irr", true) || k.endsWith("rl", true) } ?: matches.firstOrNull()
        return key?.let { k -> cur.optJSONObject(k) }
    }

    fun parse(text: String): List<Rate> {
        val cur = JSONObject(text).getJSONObject("current")
        val usd = num(cur.optJSONObject("price_dollar_rl")?.optString("p"))?.div(10) ?: 0.0
        return ITEMS.mapNotNull { item ->
            val o = find(cur, item) ?: return@mapNotNull null
            val raw = num(o.optString("p")) ?: return@mapNotNull null
            val d = num(o.optString("d")) ?: 0.0
            val inUsd = item.crypto > 0 && raw < item.crypto && usd > 0
            val value = when { item.usd -> raw; inUsd -> raw * usd; else -> raw / 10 }
            val change = abs(when { item.usd -> d; inUsd -> d * usd; else -> d / 10 })
            val dir = when (o.optString("dt")) { "high" -> 1; "low" -> -1; else -> if (d > 0) 1 else if (d < 0) -1 else 0 }
            Rate(item.name, item.code, item.icon, item.bg, value, change, dir, if (item.usd) 2 else -1)
        }
    }

    fun fetch(ctx: Context): List<Rate>? = try {
        val c = URL(URL_JSON).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "Mozilla/5.0")
        val text = c.inputStream.bufferedReader().readText()
        val list = parse(text)
        if (list.isNotEmpty()) ctx.getSharedPreferences(PREFS, 0).edit().putString("json", text)
            .putLong("t", System.currentTimeMillis()).apply()
        list.ifEmpty { null }
    } catch (e: Exception) { null }

    fun cached(ctx: Context): List<Rate> =
        ctx.getSharedPreferences(PREFS, 0).getString("json", null)?.let { runCatching { parse(it) }.getOrNull() } ?: emptyList()

    fun ago(ctx: Context): String {
        val t = ctx.getSharedPreferences(PREFS, 0).getLong("t", 0)
        if (t == 0L) return "no data yet"
        val m = (System.currentTimeMillis() - t) / 60000
        return when { m < 1 -> "moments ago"; m < 60 -> "$m min ago"; else -> "${m / 60} h ago" }
    }

    // ---- فرمت‌دهی مثل عکس: 59,750  15.74M  3.4B ----
    private fun trimNum(s: String) = if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    private fun fmt(d: Double, dec: Int) = trimNum(String.format(Locale.US, "%.${dec}f", d))

    fun priceText(r: Rate): CharSequence {
        val v = r.value
        val (num, suf) = when {
            r.dec >= 0 -> trimNum(String.format(Locale.US, "%,.${r.dec}f", v)) to ""
            v >= 1e9 -> fmt(v / 1e9, 1) to "B"
            v >= 1e6 -> fmt(v / 1e6, 2) to "M"
            v >= 100 -> String.format(Locale.US, "%,d", v.toLong()) to ""
            else -> fmt(v, 2) to ""
        }
        val sb = SpannableStringBuilder(num + suf)
        if (suf.isNotEmpty()) sb.setSpan(RelativeSizeSpan(0.6f), num.length, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    fun changeText(r: Rate): String {
        val a = r.change
        val s = when {
            a >= 1e9 -> fmt(a / 1e9, 1) + "B"
            a >= 1e6 -> fmt(a / 1e6, 1) + "M"
            a >= 1e3 -> fmt(a / 1e3, 1) + "K"
            a >= 10 -> a.toLong().toString()
            else -> fmt(a, 2)
        }
        return (if (r.dir > 0) "↑" else if (r.dir < 0) "↓" else "") + s
    }

    fun changeColor(r: Rate) = Color.parseColor(if (r.dir > 0) UP else if (r.dir < 0) DOWN else "#8E8E93")
}
