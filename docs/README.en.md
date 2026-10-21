# KLightTV

A quiet, native Arabic Android TV / Google TV home screen and screensaver for Kuwait: clock, date, daily prayers, next-prayer countdown, weather and local Kuwait wallpapers.

![Native home screen on the emulator](images/home-4k.png)

Android 8+ is required. Current screenshots and validation use an Android 14 emulator; OEM Home and screensaver support varies. Samsung Tizen and LG webOS are unsupported.

Download both APKs from [Releases](https://github.com/linkq8/KLightTV/releases), install the screensaver and then the home app, and select them in your TV settings. [Full installation guide](INSTALL.md), [architecture diagrams](ARCHITECTURE.md) and [build instructions](BUILD.md).

```sh
adb -s TV_IP:PORT install -r KLightTV-screensaver-android-tv.apk
adb -s TV_IP:PORT install -r KLightTV-home-android-tv.apk
```

Replace the address and port with your actual authorized device. Retained package identifiers `kw.urtv.home` and `kw.urtv.screensaver` allow upgrades from the previous Kuwait builds when signed with the same key. CI builds are development artifacts with a different debug signature.

The composed sunset wallpaper is AI-enhanced and exported at3840×2160 from a1672×941 generated image; it is upscaled artwork, not native4K photography. Three separately credited photographs are also bundled. See [image sources](PHOTO-CREDITS.md).

Prayer times use a verified Kuwait City record from the Al-Ojairi calendar when available; calculated fallback times are labelled accordingly. Ramadan tables require source data and local month announcements. The optional desktop app checklist uses authorized ADB, lets the user review changes, and protects essential services including the microphone. It cannot guarantee removal of all OEM tracking.

Streaming logos in screenshots use isolated emulator preview apps; streaming services are not bundled. Install your own official applications and accounts.

Based on [tshi-dev/UrTV](https://github.com/tshi-dev/UrTV), with a home component derived from [tv-home](https://github.com/mihirgupta0900/tv-home). Preserve the [component MIT notice](../android/launcher/LICENSE-tv-home) and [third-party credits](THIRD-PARTY.md).
