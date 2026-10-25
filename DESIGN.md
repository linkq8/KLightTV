---
name: KLightTV
description: Clean native Arabic television interface with solid light/dark surfaces, a vertical prayer timetable, and grouped remote-first settings.
colors:
  clean-dark: "#121316"
  clean-light: "#F2F2F7"
  settings-row-dark: "#25262B"
  settings-row-light: "#FFFFFF"
  settings-focus: "#F5F5F7"
  settings-focus-ink: "#151519"
  ink-navy: "#101D2D"
  control-navy: "#1B3046"
  porcelain: "#F1F5FA"
  glacier-cyan: "#8EDBEA"
  muted-blue: "#B9CADA"
  selected-prayer: "#234C60"
  reminder-blue: "#113D52"
  reminder-detail: "#B9E7F0"
  warning-amber: "#E8B86A"
typography:
  utility-title:
    fontFamily: "Thmanyah Medium"
    fontSize: "34sp"
  utility-body:
    fontFamily: "Thmanyah Regular"
    fontSize: "14sp"
  utility-action:
    fontFamily: "Thmanyah Regular"
    fontSize: "17sp"
rounded:
  utility: "7dp"
  home-panel: "12dp"
  prayer-selection: "8dp"
spacing:
  utility-horizontal: "64dp"
  utility-vertical: "36dp"
  button-horizontal: "18dp"
  button-vertical: "12dp"
components:
  utility-button:
    backgroundColor: "{colors.control-navy}"
    textColor: "{colors.porcelain}"
    typography: "{typography.utility-action}"
    rounded: "{rounded.utility}"
    padding: "12dp 18dp"
  utility-button-focused:
    backgroundColor: "{colors.glacier-cyan}"
    textColor: "{colors.ink-navy}"
    rounded: "{rounded.utility}"
  selected-prayer-cell:
    backgroundColor: "{colors.selected-prayer}"
    rounded: "{rounded.prayer-selection}"
---

# Design System: KLightTV

## Current brief: clean television surfaces and rebuilt settings — 23 October 2026

The user's newest direction is a clean Apple TV-like appearance and a completely new settings layout based on Mobbin. This section is authoritative and supersedes the photographic-background, skyline, wallpaper-transition, and old settings prescriptions retained below. The established vertical prayer timetable and aligned clock/date/weather composition remain the current information hierarchy; older horizontal itineraries are historical.

**Solid surfaces.** Home (`MainActivity`) and screensaver (`ScreenView`) use flat `#F2F2F7` in light mode and `#121316` in dark mode. Photography is no longer part of either composition. Photo decoding and rotation are disabled, and settings no longer exposes wallpaper selection. The launcher and screensaver Gradle asset-copy tasks use `Sync` and include calendar data and required fonts rather than photographs; synchronizing their generated asset directories also removes stale copied photographs. Existing legacy wallpaper preference fields do not enable photographs in the current UI.

**Rebuilt settings.** `PersonalizeActivity` is a native two-pane RTL screen headed “الإعدادات”: a 270dp category sidebar on the right and a scrollable, concise options pane on the left. Its five categories are “الصلاة والموقع”, “المظهر والقراءة”, “التطبيقات والريموت”, “النظام والتحديث”, and “النسخ الاحتياطي”. Each category presents a short heading and hint followed by only its relevant actions. Prayer/location includes reminders and calendar; appearance includes reading and performance; applications/remote includes app management, favorites, remote buttons and football; system includes updates, device settings and setup; backup includes preference export/import.

Rows are 52dp high with 8dp separation, 20dp horizontal padding and 12dp corners. Unfocused rows use white in light mode and `#25262B` in dark mode. D-pad focus uses an off-white `#F5F5F7` fill and dark `#151519` text; light mode adds a `#707078` 2dp outline so focus remains visible. These settings focus tokens supersede the historical cyan utility-focus rule for this screen.

Focusing a sidebar category refreshes its options. Left moves from the category into its first option; Right returns from an option to its category. Selecting a category also enters its first option. Back returns from the options pane to the category sidebar, then closes settings when pressed from the sidebar. Existing destination screens and their actions remain reachable through the new grouping.

Reference: [Mobbin — Apple Fitness grouped settings](https://mobbin.com/screens/f618bfa8-e996-4cb6-b7b8-f138e29963b1). The grouped hierarchy is adapted to a television's Arabic layout and directional remote navigation rather than copied as a phone layout. Local visual evidence is in `outputs/clean-settings-preview/home.png`, `settings.png`, and `saver.png`; these previews do not establish physical-TV validation.

## Historical design record

The sections below preserve earlier design decisions and iteration history. Where they conflict with the current brief above—especially photographs, wallpaper controls or animation, horizontal prayer rails, and the previous settings hub—the current brief takes precedence.

## Aligned television composition — 23 October 2026

Latest user direction: order and consistency take priority. Home uses equal 344dp columns: vertical timetable on the left; the original ThmanyahDisplay-Bold clock, centered date, centered weather, next-prayer pair and optional football on a single center axis on the right. Greeting is removed to reduce visual noise. The dock is a centered row of 104dp banners with equal 20dp gaps and a restrained 1.025 focus scale. Screensaver follows the same hierarchy and uses the identical display font for its main clock; period sits beside the digits. Weather remains below date and before next prayer. No system/remote modifications.

## Weather refinement — 23 October 2026

Weather stays below the date and above the next-prayer panel on both surfaces. A shared static Canvas symbol family uses consistent round strokes for sun/moon, cloud, rain, snow, thunder, fog and dust. Day/night uses the provider's `is_day` value (older caches default to daytime). Dust retains the existing measured threshold. Symbols supplement the Arabic description, never replace it. Home uses a 36dp symbol with 12dp separation and 4dp between condition and location. Main clock and Arabic numeral font remain unchanged. Leading zero tiles stay empty.

Glass panels use dark #BC101D2D / light #D0F5F8FA for better separation from detailed photographs; high contrast remains opaque. No added timer, animation, bitmap, dependency or extra network request. One extra weather field rides the existing request.

References reviewed: Mobbin The Weather Channel (https://mobbin.com/screens/1c6acbe7-6b08-4c7c-ab73-4f6f1bf8a76e) for condition symbols and grouped details; Not Boring Weather (https://mobbin.com/screens/2f5d6c68-0085-4a81-b708-3754398d677f) for hierarchy, without adopting oversized display typography; Sunlitt (https://mobbin.com/screens/5ec977d1-c2fb-4147-83d6-9d62e69ae9f5) for concise condition presentation, without adopting bright backgrounds or 3D rendering. UI UX Pro Max contrast/icon rules and Impeccable layout assessment guide this refinement.

## Approved prayer layout — 23 October 2026

The home and screensaver follow the user's approved revised brief: a vertical six-row timetable on the left; Arabic digits in individual translucent rectangles with a separate ص/م cell; next prayer and countdown grouped beneath the unchanged main clock on the right, followed by weather. This supersedes the horizontal home and screensaver itineraries described below. The large clock's typography, sizing, and position are unchanged. `TimeTiles.GlassPainter` is shared between the home TileViews and screensaver Canvas, including thick colon dots and separate period cells.

The table uses a 344dp column, two heading/date lines, 40dp rows and a full-row selected background. Rectangle fills and outlines adapt to light/dark and high contrast; the colon uses two explicitly drawn dots. No blur, flip animation, new timer, or network work is introduced. The app dock moves to the bottom with separation from the information above; large reading modes extend the scrollable hero when needed instead of overlapping the dock. Optional cached football remains below the weather. The screensaver retains its original unboxed 200-unit clock. Its vertical table occupies x=80–780/y=174–918 in the 1920×1080 design space; next prayer/countdown sits below the clock, then weather and optional football. Light/dark and large-reading screenshots are recorded in `outputs/prayer-layout-final`. Physical installation is tracked separately in the local device safety journal.

## Overview

**Creative North Star: Kuwait Horizon / Broadcast Continuity.** A quiet television continuity screen: a large clock, clear next-prayer information, an open skyline, a streaming row, and one horizontal prayer itinerary. Ink navy provides structure; porcelain text and glacier cyan carry the hierarchy. Arabic typography and Kuwait photography anchor the interface in place.

This describes the native 2.0.0 implementation, superseding the previous green/gold identity and individual split-clock rectangles. The user explicitly delegated a complete visual rebuild. Existing data, reading preferences, regional corrections, and performance controls remain functional requirements. The companion computer checklist is a separate legacy web surface; this document does not claim it was visually rebuilt.

Key characteristics: flat tonal panels, generous skyline space, direct remote navigation, restrained focus motion, and large unboxed time strings.

## Colors

The frontmatter contains the native control palette extracted from Java. Android translucent panels use the same ink-navy RGB with alpha: the home prayer rail is `0xED101D2D`, the next-prayer panel `0xE8101D2D`, and header controls `0xD9101D2D`. High-contrast mode makes the prayer rail opaque.

Glacier cyan marks navigation focus and informational hierarchy. Selected prayer cells use Selected Prayer blue. Porcelain and Muted Blue distinguish primary and secondary copy. The reminder uses its own saturated Reminder Blue with Reminder Detail text. Weather warning/stale states retain amber; existing saver weather detail uses warm neutral text. Third-party app artwork retains brand colors.

## Typography

Bundled Thmanyah Regular and Medium provide Arabic text. The main home clock selects Thmanyah Display for Arabic numerals and Medium for Latin numerals; its runtime baseline is 58sp multiplied by the reading preference. The saver clock uses Medium at 200 design units. Do not confuse constructor defaults with the runtime reading settings.

Utility headings are 34sp, descriptions/notes 14sp, and actions 17sp. Home weather starts at 23sp, secondary weather at 13sp, and prayer labels at 14sp, multiplied by the applicable reading scale. Saver prayer labels start at 29 design units and values at 36, with reading scale applied. Canvas coordinates are not Android sp.

Time values are plain strings with proportional fitting. `TimeTiles` is now a retained API name, not a visual prescription: it draws no digit boxes, split seams, or flip animation. Home timetable values use a bounded 33dp-high drawing area and upcoming time/countdown use 45dp areas. AM/PM is abbreviated ص/م; 24-hour and Latin numeral preferences remain available. Preserve stable numeric order inside RTL layouts and accessible descriptions.

## Layout

**Home:** the upper right groups the clock, date, and weather. A separate two-column next-prayer/countdown panel sits upper left. The skyline remains open between these groups and the streaming row. The six equal prayer cells run right to left in a single bottom rail, with heading/location and displayed date above. The rail is 112dp high with 16dp horizontal padding; it sits inside a native information surface beginning at 80dp. Weather begins 146dp below that surface's top. The next-prayer panel is 360dp wide. These values describe the implemented landscape composition, not a universal responsive grid.

**Screensaver:** the scene uses a 1920×1080 design coordinate system scaled uniformly and centered. The clock and weather occupy the right; next-prayer time and remaining duration sit left. The six-stop prayer rail spans x=76–1844 and y=818–1028. It carries its own displayed-day heading, weekday, and date. Background photography fills the scene with a small crop allowance for content shifts.

**Utility pages:** native RTL scrolling surfaces with 64dp side and 36dp vertical insets. “مساحتك” uses paired 86dp-high destination controls. The application center has category controls, up to 20 package rows per page, and explicit next/previous navigation. Focusable controls use native directional navigation; the ambient data rail does not capture focus.

Reading settings, long Arabic region names, unavailable weather, and both date-rollover states require visual checks. Emulator evidence does not establish all television aspect ratios, overscan, remote behavior, or firmware compatibility.

## Elevation & Depth

Tonal contrast and photograph veils provide most depth. Keep the skyline visible while maintaining clear text contrast; high-contrast and dimming preferences are functional states. There is no new blur renderer or animated background effect.

App banners retain native focus elevation and scale, a white ring, and a brief sheen. Header and utility controls use cyan focus fill with dark text. These focus cues are distinct from a selected prayer or a completed operation. The saver retains night dimming and periodic content shifting.

## Shapes

Use compact rounded rectangles: utility controls 7dp, home header controls 6dp, app artwork 10dp, home data panels 12dp, selected prayer cells 8dp. Saver panel corners are 22 design units and selected cells 16 units. Do not substitute one radius across dp and Canvas coordinate spaces.

Numbers themselves are unboxed. The timetable selection is a whole cell with surrounding padding. Application banners fill their complete 16:9 card; preserve the proportions of the embedded brand mark.

## Components

### Navigation and settings

The home header provides “مساحتك”, updates, reminders, and applications. Utility controls use the frontmatter button variants, generous native padding, and visible directional focus. “مساحتك” groups reminders, app management, updates, reading, wallpapers, region/corrections, favorites, performance, preference export/import, TV settings, and calendar. Legacy Android dialogs keep platform behavior rather than introducing a custom modal framework.

### Prayer itinerary and glance information

Six equal cells contain prayer name and time. The next prayer receives a blue full-cell fill. The date belongs to the displayed timetable: after adjusted Isha + 30 minutes, show tomorrow; at midnight that date becomes today. Current clock and next-prayer countdown keep current-time semantics independently. Source details belong in settings/calendar; do not mislabel calculated regional times as verified records.

### Streaming cards

Use edge-to-edge TV banners. TOD, Prime Video, Shasha, Stremio, and YouTube have full-card brand treatment; other installed apps use supplied banners where available. Maintain recognizable third-party artwork and long-press app actions. Screenshot preview packages are fixtures, not bundled production streaming clients.

### Silent prayer reminder

A bounded upper-right overlay displays a white prayer headline and smaller blue detail for seven seconds. It is non-focusable and non-touchable. Settings offer five minutes before prayer, at prayer time, or off, plus permission status and preview. Do not imply that permission alone guarantees visibility over an app that explicitly blocks overlays.

### Update and application management pages

Updates are initiated explicitly, with installed versions, release channel, check/download state, and Android installation confirmation. Errors remain visible; a verified download is not a completed install. App rows distinguish enabled state, system/user status, and APK size. Hiding affects launcher visibility only; system app control opens Android App Info and explains firmware/privilege limits.

### Motion and assets

Focus transitions retain 120ms scale, 160ms ring/sheen, and 200ms label timing, disabled by lite mode. Existing wallpaper transitions are 600ms on initial presentation and 2000ms between images when enabled. The saver is minute-driven rather than continuously animated. Backgrounds are bundled local images; the AI-enhanced Kuwait Towers export must not be described as a native 4K photograph. Consult photo credits for the other photographic assets.

## Do's and Don'ts

- **Do** preserve the open central skyline and horizontal prayer itinerary in the native home and saver.
- **Do** distinguish dp/sp, Canvas units, and browser pixels in implementation and documentation.
- **Do** keep visible remote focus, Arabic reading support, and data-unavailable states.
- **Do** maintain reading and performance preferences when extending the design.
- **Don't** reintroduce the retired split-clock boxes or green/gold identity as the default native design.
- **Don't** equate hiding with disabling, or a requested installation with a successful update.
- **Don't** claim universal overlay visibility, zero overhead, verified data without a source, or physical-TV validation from emulator screenshots.

## Team crests

Draw the original transparent crest without a circular backing. A cached radial shader scaled to a 58×14 horizontal footprint sits beneath the crest, fading to transparent; no animated glow or blur. Missing crest uses a short neutral dash, with the team name retained. Shared FootballBoard applies to home and screensaver.

## Quiet navigation and focus

Home header has Applications, Sources and Settings. Reminder/update/football and customization remain reachable in Settings. Dock retains fixed geometry and shows a quiet white focus outline plus a reserved caption beneath the selected app; no scale or vertical movement. Shared football cards omit full update timestamps (detail screen retains them) while preserving a compact cached-result notice. Both surfaces use the same Arabic Gregorian date format and a compact weather hierarchy.


## Display modes — 2026-10-23 revision

The settings hub remains neutral and grouped. Home and screensaver default to the restored Kuwait photo mode (`surface=scenic`), retaining Arabic numerals and the existing prayer-first composition. `surface=focus` is an optional solid-background composition with English numerals, two balanced columns and a 24 dp home / 40 design-pixel screensaver central gap. It uses proximity instead of large panel backgrounds. The underlying numeral preference is preserved when switching modes. Find the choice in Settings → Appearance and reading → Screen design; it applies to both surfaces.

Club crests have neither a backing circle nor a glow. Match-card content scales uniformly to preserve crest proportions. Photo decode/rotation is skipped entirely in Focus; scenic decoding remains off the main thread and capped by device memory policy. This revision supersedes the previous removal of all photographic backgrounds.

Preview evidence: `outputs/display-modes/`, emulator only; no physical-device installation in this revision.


### Focus / city composition refinement
Focus uses a native static diagonal navy–charcoal–muted plum gradient (ivory–cool gray in light mode), with two low-opacity edge curves. No bitmap, animation or blur. The prayer timetable is compact on Home; matches sit in a dedicated full-width strip above the dock, and along the bottom of the screensaver.
City mode uses narrow 280 dp Home side groups, a smaller 88 dp dock and translucent local backing. Screensaver side groups are uniformly scaled to preserve typography and digit proportions, leaving approximately the middle third clear. Existing local photo crop is retained for Kuwait Towers; other-city imagery is not added by this visual revision.
Emulator preview: `outputs/composition-preview/`. Physical TV unchanged.


### Focus colors and readable scenic screensaver
Focus background is now a persisted choice (`focusColor`): white pearl, charcoal black, or deep blue (default). Each uses two cached native gradients with no bitmaps, animation, blur, or decorative strokes. White automatically uses dark text; black and blue use light text. Changing the background recreates Home to refresh its palette and renderer. The preference is shared with the screensaver and included in backups.
The scenic screensaver now uses 1.06 scale in place of 0.80, enlarging its content by 32.5% and using more screen height; Home scenic composition is unchanged. Preview evidence: `outputs/focus-colors-preview/`. No physical TV changes.


### Reading and match-card balance
Focus Home now uses taller prayer cells with reduced vertical padding, anchored next to the prayer-name column. The prayer timetable rises alongside the clock; the match strip follows the taller of timetable and weather/next-prayer groups. Large reading sizes increase row height and extend the scrollable hero instead of overlapping matches or the dock. The screensaver uses the same time/name alignment and a matching nominal Focus clock size, keeping its additional vertical room for rows.
Match-card contents now use the full card width: crests and names at either side, score/time centered, status above. Wide cards enlarge crests, scores, and names; narrow cards retain proportionate sizing. No crest glow or backing circle.
Validation previews: `outputs/reading-balance-preview/` at normal and largest reading sizes. No remote-button changes or remote-input testing; no physical television installation.

### Home column grouping — 2026-10-23
- Home match cards stay in the right column below weather, including Focus mode.
- Next prayer and countdown sit side by side directly beneath the left timetable.
- Compact vertical spacing preserves the app dock at normal reading size; larger reading settings retain safe content-driven height.
- Screensaver and remote handling are unchanged by this revision.
- Verified launcher assemble/lint and actual emulator screenshots in blue Focus, Kuwait scenic, and enlarged reading modes (`outputs/home-columns-preview` in workspace).

### Restore right-side prayer summary — 2026-10-23
Supersedes the Home column grouping above: restore the prior timetable spacing and next-prayer/countdown block under weather on the right. Match cards remain confined to that right column in both modes, below the summary. No screensaver or remote changes. Launcher assemble/lint passed; emulator previews saved under workspace outputs/home-right-preview.

### Focus timetable spacing — 2026-10-23
Distribute time tiles and prayer names across the left column instead of clustering them on its right. Focus rows are 43dp at normal reading size so the final prayer aligns visually with the right match strip. Names are centered in a consistent 156dp column; tiles retain their own left column. Preserve right-side content and scenic layout. Launcher assemble/lint and layout detector pass; normal and enlarged reading emulator captures checked in outputs/home-spacing-preview.

### Screensaver parity — 2026-10-23
Focus screensaver now uses the same left time/right name timetable structure and keeps matches entirely beneath the right prayer summary. Taller row rhythm aligns the final prayer with the match-card lower edge; burn-in movement remains intact. Scenic mode retains its expanded side groups and Kuwait imagery. Four fresh real emulator captures reviewed: outputs/final-pair-preview/{blue,scenic}-{home,saver}.png. App assemble/lint and layout scan passed. No physical TV installation.

### Relevant matches and optional screensaver content — 2026-10-23
- Upcoming cards: at most 72 hours ahead; stale scheduled fixtures disappear one hour after kickoff unless updated. Completed results expire 24 hours after kickoff; stale live fixtures expire after six hours. Cancelled, postponed, suspended and abandoned fixtures are hidden.
- The same local display policy gates provider snapshots and both boards. Existing minute refresh is reused; no service, network request or extra timer added. Empty Home boards collapse without placeholders.
- Appearance settings offer prayer/weather-only screensaver or prayer/weather/matches; persisted in shared DisplayOptions and included in backups. Disabling matches skips screensaver board refresh and logo decoding.
- Validation: 13 policy boundary/status tests plus existing football normalization tests, both APK builds and lint. Emulator cases: nearby match, all fixtures four days away, and screensaver matches disabled. The four-day case uses temporary test cache dates; original cache/preferences restored. Captures: workspace outputs/match-options-preview. No TV install.

### Prayer location migration and correction context — 2026-10-24
Preserve the approved vertical timetable and both visual modes. Unknown legacy locations require country/city reapproval rather than silently using Kuwait; selected methods disclose automatic or manual provenance. Correction UI uses the shared ±30-minute range and explains that its profile follows location, calculation method and Asr choice. New emulator evidence and limitations are documented in docs/PRAYER-LOCATION-FIX.md; no physical TV update belongs to this change.
