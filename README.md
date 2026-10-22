# فیت‌یار (Android Fit)

اپلیکیشن اندروید آفلاین برای ساخت برنامه‌های تمرینی، ثبت اجرای تمرین، قدم‌شمار و خروجی CSV. داده‌ها با SQLite روی دستگاه ذخیره می‌شوند.

## پیش‌نیازها
- Android Studio و JDK 17
- Android SDK Platform 35
- Android Gradle Plugin 8.7.3 و Gradle 8.9

## اجرا و تست
مخزن را در Android Studio باز کنید و Gradle Sync را انجام دهید. تست‌های واحد را از بخش verification اجرا کنید. برای ساخت APK از Build > Build APK(s) استفاده کنید.

GitHub Actions با هر push به شاخه main تست‌های واحد را اجرا می‌کند، APK دیباگ را می‌سازد و آن را به‌عنوان artifact نگه می‌دارد. فایل APK در صفحه Actions و اجرای موفق workflow قابل دریافت است.

## قابلیت‌ها
- ساخت و حذف برنامه تمرینی و افزودن، ویرایش یا حذف حرکات
- ثبت تاریخ شمسی، حرکت، ست، تکرار و وزن
- ذخیره محلی SQLite و خروجی CSV از سوابق
- قدم‌شمار با حسگر سخت‌افزاری پشتیبانی‌شده

تاریخ با DatePicker اندروید انتخاب و سپس به تاریخ شمسی تبدیل می‌شود. تا پیش از اجرای موفق CI، ساخت و اجرای پروژه تأییدشده محسوب نمی‌شود.


## Release readiness

- The UI follows the Android device language: Persian by default, with English resources for English-language devices.
- CI tests the app, runs Android Lint, builds a debug APK, and attempts a release Android App Bundle on Android SDK 36.
- The release bundle produced by CI is an **unsigned build candidate** unless a valid release signing configuration is supplied. Never commit signing keys or passwords to this repository.
- Before publishing, configure and protect the upload key, increment `versionCode`, complete the store listing and content declarations, provide a public privacy-policy URL, and test the signed release on physical devices.
- The app currently has no advertising SDK or ad network integration. Do not claim that ads are present until a provider is selected, implemented, tested, and its data disclosures and permissions are reviewed.
- Privacy-policy draft: [PRIVACY_POLICY.md](PRIVACY_POLICY.md). Replace the contact placeholder and publish the policy at a stable public URL before store submission.
