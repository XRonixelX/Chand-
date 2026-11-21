# Chand?! 💱

**Live Iranian market rates on your Android home screen.**
Dollar, euro, gold, coins and crypto in a clean, pure-black app, plus a home-screen widget.

*By Ronixel*

[فارسی ↓](#فارسی)

---

## Features

- 30 currencies and assets: USD, EUR, GBP, USDT, Bitcoin, Ethereum, gold (18K / 24K / ounce), Azadi, Emami, half and quarter coins, and more
- Pure-black design with rounded cards, big prices and short numbers (`59,750`, `15.74M`, `3.4B`)
- Daily change shown as ↑ / ↓ (red = up, green = down, the Iranian market convention)
- Pull down to refresh with a spring animation
- Bottom sheet to switch currencies on and off
- 4×2 home-screen widget with US Dollar and Tether
- Auto-refresh every 30 seconds while the app is open, and the last prices are cached for offline use
- No ads, no tracking, no account. The only permission is Internet.

## Install

1. Open the [Releases](../../releases) page and download the latest `Chand.apk`.
2. Open it on your phone. If Android asks, allow installing from this source.
3. To add the widget: long-press the home screen → **Widgets** → **Chand?!**.

Requires Android 7.0 (API 24) or newer.

## Customize

Everything about the data lives at the top of
`app/src/main/java/com/example/nerkh/Rates.kt`:

- `URL_JSON`: the data source
- `ITEMS`: which assets exist, their names, icons and JSON keys

To change what the widget shows, edit the `show` list in `RatesWidget.kt` (currently `USD` and `USDT`).

## Data source and disclaimer

Prices come from a public, **unofficial** JSON endpoint of a third-party website. It may change, rate-limit, or disappear at any time, and some assets may not show up if the source stops providing them. Prices can be delayed or wrong.

**This app is for information only. It is not financial advice. Do not trade based on it.**

This is an independent hobby project. It is not affiliated with, endorsed by, or connected to any other app with a similar name or design, or to the data provider.

## Project structure

```
app/src/main/java/com/example/nerkh/
  MainActivity.kt      main screen, bottom sheet, menu, animations
  PullScrollView.kt    iPhone-style pull to refresh
  Rates.kt             data source, parsing, number formatting
  RatesWidget.kt       home-screen widget
.github/workflows/build.yml   builds the APK on GitHub Actions
```

## Contributing

Issues and pull requests are welcome. Good first ideas: more assets, a light theme, widget size options, and Persian / RTL support.

## License

[MIT](LICENSE) © 2026 Ronixel

---

<a name="فارسی"></a>

# چند؟! 💱 (فارسی)

**نرخ لحظه‌ای بازار ایران، روی صفحه اصلی گوشی اندرویدت.**
دلار، یورو، طلا، سکه و ارز دیجیتال توی یه اپ مشکی و خوشگل، به‌همراه ویجت.

*By Ronixel*

## امکانات

- ۳۰ ارز و دارایی: دلار، یورو، پوند، تتر، بیت‌کوین، اتریوم، طلای ۱۸ و ۲۴، انس، سکه آزادی، امامی، نیم و ربع و بقیه
- طراحی کاملاً مشکی با کارت‌های گرد، قیمت درشت و اعداد کوتاه (`59,750`، `15.74M`)
- نمایش تغییر روزانه با ↑ و ↓ (قرمز یعنی بالا، سبز یعنی پایین، مثل بازار ایران)
- با کشیدن صفحه به پایین، نرخ‌ها رفرش می‌شن، با انیمیشن فنری
- صفحه پایین‌کش برای روشن و خاموش کردن ارزها
- ویجت ۴×۲ برای دلار و تتر
- هر ۳۰ ثانیه آپدیت خودکار وقتی اپ بازه، و آخرین قیمت‌ها برای حالت آفلاین ذخیره می‌شن
- بدون تبلیغ و ردیابی و حساب کاربری. فقط دسترسی اینترنت می‌خواد.

## نصب

1. از بخش [Releases](../../releases) فایل `app-debug.apk` رو دانلود کن.
2. روی گوشی بازش کن و اگه خواست، اجازه نصب از این منبع رو بده.
3. برای ویجت: روی صفحه اصلی نگه دار ← **Widgets** ← **Chand?!**

نیاز به اندروید ۷ یا بالاتر.

## شخصی‌سازی

منبع داده و لیست ارزها بالای فایل `Rates.kt` هستن. برای عوض کردن ارزهای ویجت، لیست `show` رو توی `RatesWidget.kt` تغییر بده.

## هشدار

قیمت‌ها از یه آدرس عمومی و **غیررسمی** یه سایت دیگه می‌آن و ممکنه هر لحظه تغییر کنن یا قطع شن. ممکنه قیمت‌ها دیر یا اشتباه باشن.

**این اپ فقط برای اطلاع‌رسانیه و توصیه مالی نیست. بر اساسش معامله نکن.**

این یه پروژه شخصیه و به هیچ اپ دیگه‌ای با اسم یا ظاهر مشابه یا ارائه‌دهنده داده وابسته نیست.

## مجوز

[MIT](LICENSE) © 2026 Ronixel
