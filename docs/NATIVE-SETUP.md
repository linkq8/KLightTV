# Native first-run setup and optional customization

The launcher now opens `SetupActivity` until the user completes setup. It is a native two-page Activity, not the browser planner. Completion is stored in private preferences; no first-run service remains active. Reopen from Personalize → إعداد الشاشة.

- Page 1: country availability and Kuwait region selection. Saving writes the selected existing region through the same-signature screensaver provider; the launcher and screensaver then use the same location. A missing companion APK prevents a false successful save.
- Page 2: setup appearance, reading size, Start, Back and optional application/remote customization. Reading size is shared with home and screensaver. The light/dark choice currently affects the setup surface only; it does not claim to reskin all older application screens.
- Optional customization opens the existing on-demand package inventory or the new remote editor. Returning preserves the draft setup page. No package is automatically disabled or removed.
- Native worldwide prayer calculation and country wallpaper delivery are still pending. The native country control explicitly explains current Kuwait-only availability instead of applying Kuwait times to other countries.

## Remote behavior

No hardcoded Netflix → TOD mapping. New defaults contain no mappings. The editor captures supported application/gamepad/color/media keys and lets the user select an installed launchable application. Navigation, volume and power keys are excluded. Android accessibility must be explicitly enabled by the user for remapping outside the editor. Removing mappings restores original key handling; the Restore action also disables the service when connected.

The service has no logcat process, polling loop or screen-content permission. The old `READ_LOGS` permission has been removed. Manufacturer-consumed keys, including some Netflix buttons, may not be delivered to accessibility and cannot be promised to work. Existing installations relying on that old hardcoded fallback will need explicit reconfiguration; that fallback is no longer provided.

## Validation

Build both debug APKs and run Android lint:

```sh
ANDROID_HOME=/path/to/sdk gradle -p android :launcher:assembleDebug :app:assembleDebug :launcher:lintDebug :app:lintDebug
```

`tests/native_setup_smoke.py` is an explicit emulator-only UI test. It resets only the launcher app's preferences, uses the paired debug APKs, verifies remote-key navigation, optional destinations, location persistence, mapping activation, default inactive behavior, restoration and first-run completion. Temporary accessibility test settings are restored in `finally`. It expects Chrome from the emulator image as a test target, not a bundled app.

Captures: `outputs/native-setup/`. Test environment: Android API 34 Google APIs ARM emulator, landscape 1920×1080, density 240. This is not an OEM Android TV image and does not prove behavior of physical TV remotes or manufacturer package restrictions. No physical TV is connected or modified.

The remote activation test uses Android's `uinput` command to register a temporary keyboard and send a real input-device event. `adb input keyevent` alone does not exercise this emulator's accessibility key path. The test requires a root-capable emulator image (`adb root`), not root on a user's TV. See the [AOSP uinput reference](https://android.googlesource.com/platform/frameworks/base/+/HEAD/cmds/uinput/). The temporary device is removed when the command exits. No diagnostic key logging is retained in application code.

Final validation: both debug APK builds and lint passed; native emulator smoke test passed all assertions, including hardware-path mapping to Chrome, clearing the mapping, selecting Khairan and skipping setup after restart. Region and table-day regression tests and browser setup tests also passed. Screenshots are actual emulator captures, not generated mockups.


## تحديث الدول والمظهر والتنبيهات

تم تجاوز قيود الكويت والمظهر داخل الإعداد المذكورة في النسخة السابقة: اختيار الدولة والبحث عن المدينة متاحان الآن، وطريقة الحساب والمنطقة الزمنية تنتقلان إلى الرئيسية وشاشة التوقف والطقس والتنبيهات. المظهر مشترك في التطبيقين. راجع [التفاصيل والمصادر والحدود الحالية](WORLD-PRAYERS.md).
