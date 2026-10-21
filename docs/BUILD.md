# بناء KLightTV واختباره

المتطلبات: JDK 17، Gradle 8.11.1، Android SDK مع `platforms;android-34` و`build-tools;34.0.0`. عيّن `ANDROID_HOME` إلى مسار SDK و`JAVA_HOME` إلى JDK.

```sh
git clone https://github.com/linkq8/KLightTV.git
cd KLightTV
gradle -p android assembleDebug
```

الملفات في `android/launcher/build/outputs/apk/debug/launcher-debug.apk` و`android/app/build/outputs/apk/debug/app-debug.apk`. تُنسخ الخطوط والصور إلى أصول التطبيقين أثناء البناء؛ لا يلزم بناء HTML لتشغيلهما.

## التوقيع والتحديث

يستخدم `assembleDebug` مفتاح التطوير المحلي. بناء `assembleRelease` دون إعداد توقيع يستخدم أيضًا مفتاح debug، فلا تمثله كإصدار رسمي. للبناء بمفتاحك الخاص، اضبط المتغيرات محليًا دون وضع قيمها في المستودع:

```sh
export ANDROID_KEYSTORE=/absolute/path/to/private-release.p12
# Set ANDROID_KEYSTORE_PASSWORD securely in your environment.
# The current Gradle configuration expects key alias: urtv.
gradle -p android assembleRelease
```

يجب توقيع التطبيقين بالمفتاح نفسه لأن مشاركة الإعدادات تستخدم صلاحية signature. مفتاح إصدارات المشروع السابق محفوظ خارج المستودع، ولا يُرفع ضمن المصدر. لا يستطيع بناء مستقل بمفتاح مختلف تحديث APK الرسمي المثبت مباشرةً.

الإصدار الحالي 1.9.0 ورمز Android هو 10. عند إصدار تحديث، ارفع رقم `versionCode` في التطبيقين معًا. يمكن ضبطه عبر `ANDROID_VERSION_CODE`؛ لا تربطه برقم تشغيل CI قد يبدأ من 1 في مستودع جديد.

## الاختبارات

لتشغيل الاختبارات ومعاينة المتصفح القديمة تحتاج Python 3.9+ وNode.js:

```sh
python3 -m venv .venv
. .venv/bin/activate
pip install pillow fonttools brotli
npm install --no-save playwright@1.56.1
npx playwright install chromium
python3 src/build.py
tests/run.sh
```

إن استخدمت Chromium مثبتًا مسبقًا، يمكن ضبط `CHROMIUM` إلى الملف التنفيذي. خط Java المطلوب لاختبارات الصلاة هو JDK 17. تجميع APK وحده لا يحتاج Python أو Node.js.

اختبار واجهة التلفزيون: أنشئ محاكي Android 14، ثبّت التطبيقين بمفتاح واحد، افتح `kw.urtv.home/.MainActivity` ثم `kw.urtv.screensaver/.MainActivity`، وتحقق من D-pad وقراءة الصلاة والطقس واختيار الخلفية. `adb shell wm size 3840x2160` يختبر رسم واجهة 4K؛ لا يثبت توافق البرنامج الثابت لكل تلفزيون.

## إعادة رسم لافتة العلامة

```sh
node tools/render_brand.mjs
```

تُحفظ نسخ SVG في `.github/brand/` وPNG بأحجام Android في موارد التطبيقين. الرسم مشتق من علامة التلفزيون الحالية، مع اسم KLightTV.
