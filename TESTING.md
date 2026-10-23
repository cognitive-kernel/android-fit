# FitYar test plan

## CI checks

The GitHub Actions workflow runs these checks on the latest commit:

1. Java 17 / Gradle 8.11.1 build.
2. JVM unit tests for Gregorian-to-Jalali conversion, including Nowruz boundaries, zero padding, and invalid dates, localized numeric parsing, and UTF-8 CSV quoting/formula-injection protection.
3. Android Lint for the debug variant.
4. Android 16 / API 36 debug APK build.
5. Keystore certificate verification and an explicit APK certificate check. The artifact is rejected if it is not signed with the fixed CI test certificate.
6. Instrumented tests on an API 34 emulator:
   - All home-screen navigation actions and the illustration are present.
   - Plan/exercise create, list, update, and delete behavior.
   - Invalid set counts and negative weights are rejected.
   - Detailed workout metadata and health-log persistence.
   - Additive migration from historical database schemas v1, v2, and v3 without losing records.
   - Selective backup export and restoration of workout data without overwriting unrelated health records.

## Local commands

Use JDK 17, Android SDK Platform 36, Build Tools 36.0.0, and Gradle 8.11.1.

```bash
gradle test
gradle lintDebug
gradle assembleDebug
gradle connectedDebugAndroidTest
```

The instrumented test task requires a running Android emulator or device. The workflow uses a Google APIs x86_64 API 34 emulator while compiling against API 36. API 34 is used for emulator stability; newer platform behavior still needs a separate device/API-level check.

## Selective backup and restore

The app exports a versioned UTF-8 JSON backup. The user can select workout plans/exercise logs, health records (sleep, vitals, body metrics), step-counter history, and app preferences independently. Restoring step-counter history stops any active counting session and never resumes it automatically. Restore lists only sections found in the chosen backup; selected sections replace their corresponding current data while unselected sections remain unchanged. Database changes are applied in a transaction. Verify on a test install: back up one section at a time, restore it, restore multiple sections, cancel restore, and confirm unrelated records/preferences remain unchanged. Do not use production data for destructive restore tests.

## Update-safety rule

Do not uninstall the existing app to bypass a signature conflict. Before installing an APK over an existing FitYar installation, compare its signer certificate with the installed version and confirm the new version code is not lower. The CI key is not the same as some older automatically signed artifacts; see [ANDROID_SIGNING.md](docs/ANDROID_SIGNING.md). Never replace or regenerate the registered key silently.


## آزمون دستی روی گوشی واقعی (نسخهٔ آزمایشی مستقل)

این چک‌لیست برای APK با شناسهٔ بستهٔ `com.cognitivekernel.androidfit.test2` است؛ این نسخه در کنار نسخهٔ اصلی نصب می‌شود و نتیجهٔ آن، سازگاری امضای APK اصلی یا مهاجرت داده‌های برنامهٔ اصلی را ثابت نمی‌کند.

1. **نصب و شروع:** APK را نصب کنید، برنامه را باز کنید و صفحهٔ اصلی را در حالت فارسی و انگلیسی بررسی کنید.
2. **GIFها:** کتابخانهٔ حرکات را باز کنید؛ هر شش حرکت اسکوات، شنا، لانج، پلانک، پارویی و راه‌رفتن را جداگانه باز کنید. بررسی کنید تصویر حرکت می‌کند، تصویر بریده یا خالی نیست و بدون اینترنت هم نمایش داده می‌شود.
3. **ثبت تمرین:** یک برنامه و تمرین آزمایشی بسازید، یک رکورد ثبت کنید، برنامه را ببندید و دوباره باز کنید؛ رکورد باید باقی بماند.
4. **پشتیبان‌گیری انتخابی و بازیابی:** در بخش پشتیبان‌گیری ابتدا فقط «برنامه‌ها و سوابق تمرین» را انتخاب کنید و فایل JSON بسازید؛ بررسی کنید فایل فقط بخش انتخاب‌شده را دارد. یک برنامه و رکورد آزمایشی تازه بسازید، فایل را بازیابی کنید و تأیید کنید داده‌های تمرین به وضعیت زمان پشتیبان برمی‌گردند ولی سوابق سلامت، قدم‌شمار و تنظیمات تغییر نمی‌کنند. سپس همین آزمون را جداگانه برای «سوابق سلامت»، «سوابق قدم‌شمار» و «تنظیمات برنامه» انجام دهید. در صفحهٔ بازیابی نیز فقط یک بخش از فایل چندبخشی را انتخاب کنید و تأیید کنید بقیهٔ بخش‌ها دست‌نخورده می‌مانند. آزمون جایگزینی داده‌ها را فقط در نسخهٔ آزمایشی انجام دهید.
5. **فایل نامعتبر، لغو و پشتیبان قدیمی:** فایل خالی، JSON ناقص و فایل با نسخهٔ پشتیبان ناشناخته را انتخاب کنید؛ برنامه نباید داده‌های موجود را پاک کند. در انتخاب فایل و تأیید بازیابی نیز لغو را آزمایش کنید. برای فایل قدیمی SQLite، تأیید جایگزینی کل پایگاه داده باید پیش از هر تغییر نمایش داده شود و با لغو، اطلاعات فعلی بدون تغییر بماند.
6. **قدم‌شمار:** مجوز اعلان/فعالیت درخواستی را بدهید، شروع، مکث، ادامه و پایان را بررسی کنید. یک بار مجوز را هنگام انتخاب «ادامه» رد/اعطا کنید و مطمئن شوید درخواست ادامه به شروع تبدیل نمی‌شود و شمارندهٔ جلسه صفر نمی‌شود. هنگام مکث، چند قدم بردارید و مطمئن شوید عدد جلسه تغییر نمی‌کند. سپس برنامه را به پس‌زمینه ببرید و رفتار را بررسی کنید.
7. **راه‌اندازی مجدد:** با قدم‌شمار در حالت مکث، گوشی را راه‌اندازی مجدد کنید و بررسی کنید سرویس بدون اجازهٔ کاربر شمارش را از سر نگیرد. سپس حالت شروع‌شده را جداگانه آزمایش کنید؛ رفتار پس از راه‌اندازی مجدد به نسخهٔ Android، حسگر و محدودیت‌های سازنده وابسته است.
8. **چیدمان:** صفحه‌های کوچک، بزرگ‌نمایی متن و تغییر زبان را بررسی کنید؛ دکمه‌ها نباید روی هم بیفتند و متن نباید از صفحه بیرون بزند.
9. **گزارش خطا:** برای هر مشکل، مدل گوشی، نسخهٔ Android، زبان برنامه، مراحل بازتولید و تصویر صفحه را ثبت کنید.

**هشدار داده‌ها:** برای آزمون نسخهٔ اصلی، برنامهٔ نصب‌شده را حذف نکنید. نصب APK به‌عنوان به‌روزرسانی فقط پس از تطبیق گواهی امضا و تأیید نسخهٔ بالاتر مجاز است؛ پیش از آن، پشتیبان دستی مستقل بگیرید.
