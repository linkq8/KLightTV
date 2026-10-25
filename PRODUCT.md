# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Arabic-speaking households using Android TV or Google TV with a directional remote, including viewers who need larger text and stronger contrast from across the room.

## Product Purpose

KLightTV is a lightweight native television home and screensaver for launching streaming apps and displaying Kuwait regional prayer times, weather, time/date, and Kuwait scenery. The native 2.0.0 redesign uses the visual system recorded in `DESIGN.md`.

## Operating Context

Two upgrade-compatible packages remain `kw.urtv.home` and `kw.urtv.screensaver`, sharing configuration through a signature-protected provider. Android Views and Canvas render the interface; no WebView was introduced. Android 8+ is the target platform. Samsung Tizen and LG webOS are outside that target.

Current development and screenshots use an Android 14 emulator, including a 3840×2160 interface configuration. No current physical-TV validation is implied. Prior Skyworth and Xiaomi installation records are historical device-specific evidence, not proof that this version is installed or validated there. Version 2.0.0 is the current local implementation; publication and physical deployment are separate actions.

## Current Capabilities

- Native home with streaming favorites, app grid and long-press actions; native screensaver with corresponding time, weather, and prayer information.
- Kuwait region selection, per-prayer corrections, tomorrow's table after adjusted Isha + 30 minutes, and explicit displayed-day/date labels. At midnight the same table becomes today's.
- Reading preferences for numeral style, 12/24-hour time, text/prayer/countdown size, weight, contrast and dimming. Hardware-aware automatic, normal and lite modes bound bitmap work and optional transitions.
- Local Kuwait backgrounds, including an explicitly AI-enhanced 3840×2160 Kuwait Towers export and credited photographs. Full-card streaming artwork preserves brand proportions.
- Optional silent prayer overlay: five minutes before prayer, at prayer time, or off. Sunrise is excluded. One future Android alarm is scheduled; the noninteractive overlay lasts seven seconds. Region and correction changes reschedule it.
- Manual GitHub update checks with selectable stable-only or stable-plus-preview channel; preview inclusion is the current default. Home and saver update separately. Downloads are bounded, checked for SHA-256 digest, package, release version and signing certificate, then passed to Android for user confirmation. Existing versions cannot be downgraded through this path. Leaving an active download cancels it.
- On-demand app inventory with user/system/hidden categories, 20 displayed rows per page, enabled status, APK file size and available device storage. Launcher hiding has undo; removal and system management use Android confirmation/App Info.
- Preference export/import, favorite ordering, wallpaper selection and access to television settings from “مساحتك”. Export covers interface preferences, not streaming accounts or all system settings.

## Data and Permission Boundaries

Prayer resolution uses verified city calendar records where available, separately documented published region offsets, and coordinate-based calculation where no verified offset exists. Khiran has no verified published fixed offset in the current catalogue and uses coordinates; do not invent a fixed minute band or label its calculated result official. Settings retain source explanations. Missing official records must not be silently presented as verified data. The calendar/archive remains in the implementation, without expanding its verified coverage by assertion.

Weather uses cached Open-Meteo responses for the configured coordinates and shows unavailable/stale states. Forecast and air-quality requests are bounded; network refreshes are not a continuous foreground service.

The reminder requires draw-over-other-apps permission and, on relevant Android versions, exact-alarm permission. It does not wake the screen, request audio focus, play sound, or take input focus. Android power policy and apps that hide overlays can prevent or delay visibility; protected playback and each target firmware need their own validation.

An ordinary app cannot promise to disable arbitrary manufacturer packages, enforce background restrictions, or remove system apps. The center exposes actual state and Android management routes. Computer-assisted ADB tools remain the route for supported privileged actions. Hiding an app is not disabling its background activity. Protect input, microphone/voice search, networking, authentication, installation, and playback services.

The updater requires Android install-source permission and an available system installer. GitHub release assets must supply the expected metadata and a matching trusted signature. A newer local build may correctly reject an older public release. Do not claim end-to-end upgrade success until a newer correctly signed release has actually been installed.

## Performance and Evidence

New reminders use a single scheduled alarm and a short-lived broadcast overlay, not a resident polling service. Update checks/downloads and package enumeration run on demand. Package rows are paginated; home data callbacks stop while the activity is paused. Existing hardware policy and background decoding remain in use.

Every feature has some cost. “Zero slowdown on every television” is not a supportable guarantee. Report the tested device/emulator, foreground state, memory measure, and workload together; do not carry forward an old PSS sample as current evidence. Builds, pure-Java schedule/date tests, screenshots, alarm/overlay tests, update checks, and playback validation should be recorded by the release verifier, with untested firmware and DRM combinations stated explicitly.

## Product Principles

- Make everyday viewing and remote navigation simple; preserve prayer, weather and scenery, with football available only as an optional feature; markets and series schedules remain outside scope.
- Keep optional work bounded and initiated by explicit settings or user actions.
- Preserve core television and accessibility services when managing applications.
- Distinguish actual package state, requested changes, and confirmed results.
- Keep permissions, data provenance, stale data and firmware limitations understandable.
- Measure representative workloads before making performance or compatibility claims.

## Remaining Validation Boundaries

- Physical televisions, protected streaming playback, overlay permission availability, and exact-alarm behavior differ by firmware.
- System-app disable/removal choices remain per-device and user-controlled; no universal manufacturer removal list is applied.
- Update delivery depends on future correctly signed published releases and Android installer behavior.
- Photograph attribution/licensing and AI-enhancement provenance must remain with distributed assets.

## Optional Football Extension — 2026-10-22

Football is an ordinary extension of the existing native design world in `DESIGN.md`: the same Arabic typography, palette, reading preferences and quiet cards remain in use. Users may enable it and select up to six clubs or national teams. Home and screensaver display up to two cached match cards at once, rotating through available cards on the existing minute cycle. Match refresh is manual in the football manager; the signature-protected provider and display surfaces perform no network polling. Arabic names use a local dictionary, Arabic country labels and a phonetic fallback for unknown clubs. Team crests or flags supplied as team assets have bounded downloads and a small local cache, with a generic placeholder when unavailable.

See `docs/FOOTBALL.md` for source coverage, cache limits and stale-data behavior. The current build and Android Lint pass; football and team-name tests and light/dark emulator captures are recorded there. The observed ten-logo cache occupied approximately 128 KiB on the emulator; this is not a universal performance claim. No physical-TV installation or GitHub deployment was performed in this turn.
