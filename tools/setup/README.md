# First-run setup preview

Run `python3 tools/setup/server.py` from the repository with Python 3.9+. The localhost browser is a **computer preview**, not installed TV UI.

Two pages only:
1. Country and city. Suggested prayer method: Kuwait → Kuwait, Saudi Arabia → Umm al-Qura, Egypt → Egyptian General Authority of Survey. Other countries remain unresolved (`local`) until an appropriate Sunni local source is verified.
2. Theme and reading size. Performance stays automatic; backgrounds follow the country; remote buttons remain original. No app modifications.

The browser stores validated preferences locally and skips the wizard on return to the same origin. The completion view explicitly says nothing was applied to a TV. There is no download-file step. Changing the server port changes the browser origin; this preview behavior is not Android persistence.

The native first-launch gate, worldwide prayer runtime, official timetable integration and country wallpaper delivery remain pending. No provider is described as officially integrated just because its calculation preset is selected. The internal read-only API and `tools/install.py --profile-plan FILE` remain available for development.

Only Sunni calculation methods are in the internal allowlist. Calculation presets are distinct from schools of jurisprudence and official published timetables. Standard and Hanafi Asr are both Sunni choices; advanced choices are excluded from initial setup. No Jafari/Tehran presets are accepted. Do not infer authority or accuracy from a preset name.

Source references for future verification:
- Egypt: https://www.esa.gov.eg/praytimes.aspx
- Saudi official guidance for Umm al-Qura: https://www.mof.gov.sa/help/faq/Pages/FAQ_008.aspx
- Kuwait: verification against the official local timetable remains required before runtime integration.
- City search: https://open-meteo.com/en/docs/geocoding-api (Open-Meteo / GeoNames, network only on Search).

No continuous polling, animations, blur rendering, external fonts or image downloads in this preview. Illustration is inline vector geometry. Local server binds to loopback, authenticates API requests with a per-session token, and makes no device writes.

Tests: `python3 tests/setup_profile_test.py`; `node tests/setup_ui_test.mjs` (Playwright and Chrome, or `CHROMIUM` path). City tests use deterministic mock responses; no physical TV is tested.
