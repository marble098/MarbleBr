# راهنمای آپدیت یکپارچه وابستگی‌ها (Unified Dependency Updates)

تمام کتابخانه‌ها و تکنولوژی‌های **MarbleBr** فقط از طریق یک مسیر واحد به‌روزرسانی می‌شوند:

- **Workflow:** [`.github/workflows/dependency-update.yml`](../.github/workflows/dependency-update.yml)
- **اسکریپت:** [`scripts/update_dependencies.py`](../scripts/update_dependencies.py)
- **تست‌ها:** [`scripts/test_update_dependencies.py`](../scripts/test_update_dependencies.py)

> ⚠️ Dependabot، Renovate و Workflow هفتگی قدیمی عمداً حذف شده‌اند تا فقط یک منبع واحد و قابل‌اتکا برای نسخه‌ها وجود داشته باشد.

---

## چه چیزهایی آپدیت می‌شوند؟

| گروه | نمونه‌ها | منبع |
|---|---|---|
| Version Catalog | AndroidX، Compose BOM، Material3، Room، DataStore، Work، Hilt، Credentials، Biometric، Coroutines، Serialization | Google Maven / Maven Central |
| موتور مرورگر | Mozilla **GeckoView** | maven.mozilla.org |
| پلاگین‌ها و ابزار بیلد | AGP، Kotlin، KSP، Hilt Gradle Plugin، Ktlint، Detekt | Google Maven / Maven Central |
| Gradle Wrapper | `distributionUrl` در `gradle/wrapper/gradle-wrapper.properties` | services.gradle.org |
| GitHub Actions | `actions/checkout`، `setup-java`، `upload-artifact`، `gradle/actions`، `softprops/action-gh-release` | GitHub Releases API |

## قواعد انتخاب نسخه

- پروژه‌های پایدار فقط نسخه‌های **stable** می‌گیرند و هرگز به alpha/beta نمی‌پرند.
- ماژول‌هایی که روی `rc` هستند (مثل `androidx.biometric`) فقط `rc` جدیدتر یا نسخه نهایی می‌گیرند.
- نسخه‌های وابسته همیشه هم‌تراز می‌شوند (مثلاً `androidxHilt` و `androidxHiltNavigationCompose`، یا `kotlin` و `kotlin-metadata-jvm`).
- برای اجازه دادن به پیش‌انتشارها، Workflow را با ورودی `include_prereleases=true` اجرا کنید.

## چرخه اجرا

1. **زمان‌بندی:** هر دوشنبه ساعت ۰۴:۰۰ UTC (یا اجرای دستی با `workflow_dispatch`).
2. بررسی نسخه‌ها و اعمال تغییرات توسط اسکریپت.
3. اگر تغییری نبود → پایان موفق.
4. اگر تغییر بود → بیلد کامل + `ktlintCheck` + `detekt` + `testDebugUnitTest` با نسخه‌های جدید.
5. در صورت سبز بودن: ثبت ورودی فارسی در `CHANGELOG.md`، Commit روی شاخه `chore/unified-deps-<date>-<time>` و باز کردن PR.
6. در صورت شکست: هیچ PRی ساخته نمی‌شود و خلاصه خطاها در **Job Summary** ثبت می‌شود.

## اجرای دستی محلی

```bash
# فقط بررسی (بدون تغییر فایل‌ها)
python3 scripts/update_dependencies.py --check

# اعمال تغییرات
python3 scripts/update_dependencies.py --apply

# اجرای تست‌های آفلاین اسکریپت
python3 scripts/test_update_dependencies.py
```
