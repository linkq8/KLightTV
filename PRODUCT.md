# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Arabic-speaking household members using an Android television with a directional remote, reading the clock and prayer times from across the room.

## Product Purpose

A lightweight Kuwait television home screen and screensaver for launching streaming applications and displaying time, date, Kuwait City prayer times, weather, and photographs of Kuwait.

## Operating Context

The current implementation uses native Android Views and Canvas. The latest installed release is 1.5 on a Skyworth SWTV-20AE running Android 10. The separately configured Xiaomi television is not automatically included in changes for the Skyworth television. Saved device inspection records report a 1920×1080 Android interface; native panel resolution and supported display modes require separate verification.

## Capabilities and Constraints

- Preserve clear large Arabic type, prayer times, weather, and Kuwait photography.
- Exclude football, market information, and television-series schedules from the home dashboard.
- Preserve microphone and voice search functionality.
- Keep the launcher and screensaver lightweight, with bounded network refreshes and no unnecessary persistent background service.
- Provide a user-selectable application checklist instead of a permanent blanket removal list. The checklist should distinguish keeping, hiding, disabling, background restrictions, and uninstalling, subject to actual device capabilities.
- Protect services required for input, networking, audio/video playback, authentication, and application installation.
- Reduce manufacturer advertising, recommendations, and tracking; package state alone is not proof that every vendor network request has stopped.
- Source real professional photographs of Kuwait city and nature, aiming for genuine 4K originals with verified usage rights.
- Add a Ramadan timetable for Kuwait City using verified Al-Ojeiri calendar data. The current Aladhan/calculated prayer times must not be labeled as Al-Ojeiri data.

## Evidence on Hand

Existing Java sources, local photo credits, screenshots, device baselines, restoration scripts, and release 1.5 verification records are available in the workspace. The last recorded screensaver process PSS was approximately 53 MiB. This is a single-process measurement, not a guarantee of future memory use or overall device performance.

## Current 1.6 Scope

The user explicitly requests emulator development and flexibility across televisions; no physical TV is currently available. Target Android TV / Google TV Android 8+; this does not install on Samsung Tizen or LG webOS. Read installed packages dynamically, preserve unknown system services, and never apply a model-specific removal list automatically. API and display compatibility are tested on an Android 14 emulator; real-device firmware behavior still needs validation.

Five licensed real photographs are bundled at width 3840; no paid license was purchased. Validated Al-Ojeiri records cover Ramadan 1447 and October 21–November 20, 2026. Ramadan 1448 is unavailable and must remain visibly unpublished until verified data is returned.

## Open Decisions

- Final application selections will come from the checklist; no new keep/remove list is assumed.
- Whether photo licensing must be free or may include a separately approved paid license remains unanswered.
- Await authoritative Ramadan 1448 records; the explicit refresh flow is implemented.
- A computer-assisted ADB checklist is proposed for system-package control; an ordinary television application cannot assume equivalent privileges.

## Product Principles

- Prioritize readability from a distance and predictable remote navigation.
- Make optional content and application choices explicit and reversible where possible.
- Use verifiable local data and clearly identify its source and freshness.
- Measure memory and responsiveness before and after additions on the target television.
- Preserve core television and accessibility functions when reducing background activity.


## Latest refinement 1.7

Three inspected landscapes replace all five previous photos: Frans van Heerden city panorama, Shashi Ghosh night skyline, Quintin Gellar marina sunset. All bundled files are 3840×2160. Screenshot favorites are TOD, Prime Video, Shasha (com.shasha.tv, distinct from Thmanyah), Stremio and YouTube. Isolated disclosed emulator preview packages supply publisher icons for screenshots, never production installers.


## Latest refinement 1.8

User requested AI regeneration of their Kuwait Towers sunset photograph specifically for free regions of the UI, and full-card streaming banners instead of square icons nested in rectangles. The native implementation now uses complete brand-colored banners with proportional marks. Generated wallpaper is1672×941 and explicitly identified as regenerated; no claim of original4K source.


## KLightTV 1.9.0

The product is now named KLightTV. Android package identifiers, preferences, and signature permission remain unchanged for upgrade compatibility. The composed sunset wallpaper retains its central tower corridor and now uses an AI-detail-enhanced export at3840×2160; native generation remained1672×941. Local bitmaps and minute-driven native rendering remain the implementation. No new runtime image network requests or resident services.


## KLightTV1.10.0

Shared regional prayer resolution distinguishes published offsets from geographic calculations. Khiran has no verified published offset and stays labelled calculated. Native reading settings use the existing deep-green/gold/Thmanyah system with a two-column draft preview and explicit save/cancel. Display and performance options are shared through the signature-protected provider; low-memory mode caps bitmap resolution, preserves native text and removes optional transitions. No new resident service or tracker.
