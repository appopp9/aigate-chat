# AiGate Chat — v3.0

اپلیکیشن اندروید چت هوش مصنوعی با طراحی Neobrutalism Minimalism، فونت وزیر و پشتیبانی از دو نوع API.

## نوع API های پشتیبانی‌شده

- **OpenAI Compatible** — `POST /chat/completions`، `GET /models`، `POST /images/generations` با هدر `Authorization: Bearer`
- **Anthropic Compatible** — `POST /messages` با هدرهای `x-api-key` و `anthropic-version: 2023-06-01`

هر تعداد API می‌توانید اضافه کنید؛ لیست مدل‌ها موقع افزودن گرفته و ذخیره می‌شود.

## امکانات نسخه ۳

1. ویرایش پیام و تولید دوباره (چند نسخه پاسخ با جابه‌جایی بین آن‌ها)
2. شاخه‌ای کردن گفت‌وگو از هر پیام
3. جست‌وجو در تمام چت‌ها
4. تست اتصال هر API با نمایش وضعیت و تأخیر (ms)
5. جست‌وجو و علاقه‌مندی (ستاره) در لیست مدل‌ها
6. فال‌بک با اجازه کاربر — هنگام خطا می‌پرسد و نام API جایگزین را نشان می‌دهد
7. شمارش توکن و هزینه تقریبی (قیمت ورودی/خروجی هر API قابل تنظیم)
8. پشتیبان‌گیری و بازیابی کامل (JSON)
9. خروجی گرفتن از گفت‌وگو با فرمت Markdown / HTML / متن ساده
10. هایلایت رنگی سینتکس در بلوک‌های کد
11. پیش‌نمایش زنده HTML/SVG
12. حافظه بلندمدت با ویرایش کامل از تنظیمات
13. مقایسه هم‌زمان دو مدل
14. شش تم رنگی + بازخورد لمسی (Haptic)
15. دریافت و ذخیره عکس، فایل، کد و ZIP
16. نوتیفیکیشن Foreground Service — تولید پاسخ در پس‌زمینه قطع نمی‌شود

## ساخت APK

با GitHub Actions:

```
./gradlew assembleDebug assembleRelease --stacktrace
```

ورک‌فلوی `.github/workflows/build.yml` خودش کی‌استور می‌سازد و APK ها را با `apksigner` امضا و در آرتیفکت `apks` آپلود می‌کند.

## نسخه ابزارها

- AGP 8.7.2 · Kotlin 2.0.21 · Gradle 8.11.1
- compileSdk/targetSdk 35 · minSdk 24 · JVM 17
- Compose BOM 2024.10.01 · OkHttp 4.12.0 · kotlinx-serialization-json 1.7.3 · Coil 2.7.0
