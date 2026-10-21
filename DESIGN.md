---
name: KLightTV · الكويت
description: Arabic television views and local management tools in deep green, warm white, sand gold, and Thmanyah.
colors:
  deep-green: "#091610"
  warm-white: "#f6efe0"
  sand-gold: "#dcb86f"
  clock-white: "#fffdf6"
  native-surface: "#183125"
  native-muted: "#bfcbbc"
  native-checkbox-focus: "#294d37"
  web-surface: "#11251a"
  web-field: "#14281d"
  web-selected: "#1e3929"
  web-divider: "#294132"
  web-border: "#3a5341"
  gold-hover: "#edcd8e"
  gold-focus: "#f1d79a"
  secondary-text: "#eee9dc"
  secondary-hover: "#203a29"
  muted-text: "#b5c7b8"
  preview-bg: "#342d1d"
  preview-text: "#efd79e"
  error-bg: "#582c25"
  error-text: "#ffe7dd"
typography:
  web-headline:
    fontFamily: "Thmanyah, Arial, sans-serif"
    fontSize: "38px"
    fontWeight: 600
    lineHeight: 1.25
  web-title:
    fontFamily: "Thmanyah, Arial, sans-serif"
    fontSize: "23px"
    fontWeight: 700
    lineHeight: 1.6
  web-body:
    fontFamily: "Thmanyah, Arial, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.6
  web-app-name:
    fontFamily: "Thmanyah, Arial, sans-serif"
    fontSize: "18px"
    fontWeight: 500
    lineHeight: 1.6
  web-label:
    fontFamily: "Thmanyah, Arial, sans-serif"
    fontSize: "13px"
    lineHeight: 1.6
rounded:
  web-tab: "6px"
  web-select: "7px"
  web-status: "8px"
  web-control: "9px"
  web-panel: "12px"
spacing:
  web-tight: "8px"
  web-control-block: "12px"
  web-row-gap: "16px"
  web-control-inline: "18px"
  web-panel: "24px"
  web-workspace-gap: "30px"
components:
  button-primary:
    backgroundColor: "{colors.sand-gold}"
    textColor: "{colors.deep-green}"
    typography: "{typography.web-body}"
    rounded: "{rounded.web-control}"
    padding: "12px 18px"
  button-primary-hover:
    backgroundColor: "{colors.gold-hover}"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.secondary-text}"
    typography: "{typography.web-body}"
    rounded: "{rounded.web-control}"
    padding: "12px 18px"
  button-text:
    backgroundColor: "transparent"
    textColor: "{colors.sand-gold}"
    typography: "{typography.web-body}"
    padding: "10px 0"
  search-field:
    backgroundColor: "{colors.web-field}"
    rounded: "{rounded.web-control}"
    padding: "0 14px"
  filter-tab-selected:
    backgroundColor: "{colors.sand-gold}"
    textColor: "{colors.deep-green}"
    rounded: "{rounded.web-tab}"
    padding: "8px 13px"
  checklist-row-selected:
    backgroundColor: "{colors.web-selected}"
    textColor: "{colors.warm-white}"
    padding: "17px 10px"
  review-panel:
    backgroundColor: "{colors.web-surface}"
    textColor: "{colors.warm-white}"
    rounded: "{rounded.web-panel}"
    padding: "24px"
---

# Design System: UrTV · الكويت

## Overview

**Creative North Star: "Kuwait at home"**

The implemented identity combines real Kuwait photography, deep green surfaces, warm white Arabic text, and sand gold emphasis. Thmanyah gives clocks, prayer times, and controls a shared voice. Large television information remains readable over dark photographic veils; utility screens use quiet rows and solid surfaces.

This is a record of the existing implementation, not a replacement identity. The native Android launcher and Canvas screensaver carry the immersive photographic treatment. Personalization, app visibility, and Ramadan tables use a restrained native utility treatment. The local computer checklist translates that treatment into responsive browser controls. Its specific inspection → selection → review → apply → restore sequence remains a surface contract in `tools/tv-manager/index.html`.

**Key Characteristics:**

- Arabic-first layouts with explicit handling of Latin package names and numeric runs.
- Large, legible information over real Kuwait photographs.
- Sand gold emphasis with visible keyboard and directional-remote focus.
- Flat utility rows with clear selection, source, and availability states.
- Platform-specific sizing rather than a single pixel scale for every screen.

This extraction uses `tools/tv-manager/style.css`, `tools/tv-manager/index.html`, shared `TvPage.java`, launcher `MainActivity.java`, `HomeCard.java`, `ManageAppsActivity.java`, and screensaver `ScreenView.java`, checked against release 1.6 screenshots. Frontmatter typography, dimensions, and component tokens describe the browser surface; native measurements below remain Android dp/sp or Canvas design-space units. The sidecar provides browser component specimens, not a simulated Android runtime.

## Colors

The shared palette is a dark green field with warm light text and a restrained sandy accent. Frontmatter values are extracted implementation values; the files currently use literals, not a central token API.

### Primary

- **Sand Gold** (`sand-gold`): primary browser actions, selected filters, native utility focus, checkbox marks, prayer emphasis, and small photographic accents.
- **Gold Hover** and **Gold Focus**: browser interaction feedback. The keyboard outline remains visible outside the control.

### Neutral

- **Deep Green**: full-page utility background and text on gold controls.
- **Warm White**: body text and ordinary television information. **Clock White** is the brighter native clock and prayer-value color.
- **Native Surface**: resting native utility buttons. **Native Muted**: native descriptions and notes. **Native Checkbox Focus** identifies the active checklist checkbox row.
- **Web Surface**, **Web Field**, and **Web Selected**: separate the review panel, editable controls, and selected package rows. **Web Divider** and **Web Border** preserve quiet structure.
- **Secondary Text**, **Secondary Hover**, and **Muted Text** support browser secondary actions and explanatory copy.

### Status treatments

The browser pairs **Preview Background / Preview Text** for saved-record preview and **Error Background / Error Text** for failures. State names remain visible in words; color alone does not carry operational meaning. Existing native photographic headings also use a slightly deeper gold (`#d6af6b`); preserve that incumbent variation rather than mechanically replacing every gold literal.

**The Visible State Rule.** Distinguish focus, selection, and data status with the component's existing text and state treatment; a gold accent is not proof that an operation has executed.

## Typography

**Display Font:** bundled ThmanyahDisplay-Bold for native clock numerals.
**Body Font:** bundled Thmanyah Regular and Medium on Android; the browser loads its Thmanyah face from `/font.otf` with Arial and sans-serif fallbacks.

The hierarchy is built around Arabic reading and prominent time values. Keep the Latin application names supplied by installed packages. Browser package identifiers use isolated left-to-right text with ellipsis; temperature runs in native views use directional isolation. Existing Android code sometimes synthesizes bold from Medium; do not imply that an additional bold text font file exists.

### Browser hierarchy

The frontmatter records the desktop headline, section title, body, application name, and label roles. The headline reduces to (31px) at the tablet breakpoint and (26px) at the mobile breakpoint. Application names reduce to (16px) on mobile. Small operational metadata ranges from (10px) package identifiers to (14px) filter labels; these browser sizes are not television defaults.

### Native hierarchy

- Utility pages: title (30sp, Medium), description and note (14sp, Regular), action label (17sp, Regular), app checkbox label (19sp, Medium).
- Home card: table heading (16sp), prayer name (20sp), prayer value (23sp), weather summary (28sp), weather detail and next-prayer text (18sp).
- Home clock: display numerals (58sp); date (14sp).
- Screensaver: a (1920 × 1080) design coordinate space. Clock digits start at (326 design units) and fit the available width; weather uses (48 units), secondary weather text (32 units), and Gregorian date (23 units). These are Canvas coordinates scaled with the scene, not CSS pixels or sp.

## Layout

Native utility pages are vertical RTL scroll surfaces with side insets (48dp), top inset (30dp), and bottom inset (32dp). Actions retain generous padding and predictable remote focus. App rows contain an installed icon (38dp), checkbox, name, and separate options control. Preserve the distinction between choosing visibility and opening application options.

The home places a prayer table on the left and clock/weather on the right over a full-screen photograph. Its native information area allocates approximately (53%) to the table and (43%) to weather. The favorites dock sits below, using 16:9 app art with dynamic widths capped at (145dp). Native utility pages do not inherit this photographic composition.

The screensaver scales its complete (1920 × 1080) composition uniformly by the smaller screen dimension ratio, then centers it. When the table is hidden the clock centers horizontally. This is an implemented scaling strategy, not a guarantee for every television or aspect ratio. Keep photograph crops and text contrast inspectable at each tested display size.

The browser container is capped at (1450px), with side padding (5%) and top padding (40px). Its RTL workspace places a flexible checklist beside a (300px) review panel, separated by (30px). The review panel is sticky with top offset (20px). At widths up to (950px), the workspace becomes one column and the panel returns to normal flow. At widths up to (600px), side padding becomes (18px), the list header disappears, and each row uses a flexible app column plus a (125px) action column; state appears below the action. Filters wrap rather than requiring horizontal scrolling.

Current evidence is emulator-based. Release 1.6 screenshots include the native home/screensaver at 720p, 1080p, and 2160p, plus desktop and mobile browser checklist views. No physical television was available for this round. Android 8+ is the target; real firmware, remote behavior, overscan, and unusual display configurations remain separate validation work. See `PRODUCT.md` for the compatibility and release constraints.

## Elevation & Depth

Utility surfaces are flat: solid green layers, thin dividers, and modest borders do the structural work. Browser controls and review panels have no decorative shadow. The browser confirmation dialog uses a dark backdrop (`#000b`) to separate review from the underlying checklist.

Photography has its own contrast system. The home uses a vertically graded black scrim and subtle text shadows. The prayer surface uses translucent deep green (Android `0xD9091810`) with a low-opacity gold stroke (Android `0x29DCB86F`). The screensaver adds linear and radial veils, then dims at night. These layers support reading over varying photographs; they are not general-purpose glass cards.

### Native elevation vocabulary

- Home header text: shadow radius (3dp), vertical offset (1dp), black at Android alpha `0x80`.
- Screensaver digits: shadow radius (8 design units), vertical offset (4 units), black at Android alpha `0x99`.
- Focused home tile: translationZ (14dp), scale (1.045), white ring, and a subtle sheen.
- Native launcher popup: elevation (16dp).

**The Reading Veil Rule.** Preserve the native photographic overlays when changing images; text readability is part of the image treatment.

## Shapes

Browser corners stay compact: filter tabs, selects, statuses, controls, and panels each use the extracted role in frontmatter. Rows remain rectangular and separated by rules. A selected row changes its fill without becoming a raised card.

Native utility buttons use (10dp) corners, checkbox focus backgrounds (8dp), and the home prayer panel (12dp). App artwork uses (10dp) clipping; the dock band uses (26dp) corners. Header pills use (15dp) corners on (30dp) height. The screensaver prayer panel uses (24 design units) and its upcoming-prayer row (12 units). Do not flatten these values into one cross-platform radius.

## Components

### Buttons

Browser primary actions are sand gold with deep green text; secondary actions are transparent with a muted green border; text actions use gold text. All share the same font family. Hover changes fill over (120ms). Keyboard focus uses a (3px) Gold Focus outline at (3px) offset. Disabled controls use opacity (.45), with the disabled property determining behavior.

Native utility buttons rest in Native Surface and change to Sand Gold with Deep Green text when focused. Their padding is (18dp horizontal / 12dp vertical). The whole focusable text control is the target. Preserve the native surface's directional interaction instead of copying browser hover-only behavior.

### Filters and navigation

Browser category filters are wrapping buttons with `aria-pressed`; the selected one is filled gold. They are filter controls, not links to separate pages. The native home header uses icon-and-label pills with white focus fill, dark text, and scale (1.03) over (120ms). This existing white photographic focus treatment coexists with the utility pages' gold focus treatment.

### Search and selectors

The browser search field pairs a simple line icon with a labeled input on a dark green surface and thin green border. The input is transparent inside the container. Selects use the same field fill with a stronger border. Preserve native HTML input, checkbox, and select semantics; the small package name is supporting identification, not the primary label.

### Checklist rows and review panel

Browser rows align app identity, current state, and requested change. A checked row receives Web Selected fill and a gold checkbox accent. Protected services display explanatory text where a change selector would otherwise appear. The adjacent review panel has a border, modest corners, and full-width actions. Its empty state explains that untouched apps stay unchanged; its review dialog separates selection from execution.

The native app list manages visibility. Its subtitle explicitly says hiding does not stop background activity. Saving and undoing are separate actions; system-package management is explained through a native dialog and the computer tool. Preserve those labels and distinctions when extending the interface.

### Prayer, weather, and time

The recurring signature is a readable table next to a prominent Arabic clock/weather block. Prayer names align on the right and values on the left. Fine gold rules separate rows; the next prayer adds a translucent green highlight and lighter gold text. Source labels distinguish Al-Ojeiri records from calculated times, and weather has unavailable/stale copy. Do not remove these states to make a screenshot cleaner.

The Ramadan table retains an archive label for 1447. The upcoming 1448 view is intentionally empty until verified records exist, with source and refresh guidance still visible. An empty verified-data state is an implemented component state, not missing placeholder content.

### Photography and app artwork

Five real, licensed local photographs at width (3840px) are bundled; no paid asset purchase is claimed. The imagery supplies place and atmosphere while the overlays stabilize legibility. App artwork comes from the installed app catalogue and retains its own colors; do not recolor third-party icons into the green palette.

### Motion

Native focus feedback is quick: tile scale/elevation (120ms), ring fade (160ms), label reveal (200ms). The existing home wallpaper fades in over (600ms) initially and crossfades over (2000ms) afterward. Screensaver text shifts position at four-minute intervals, and its night dimming is separate from focus animation. These are observed behaviors, not a new universal motion framework.

## Do's and Don'ts

### Do:

- **Do** retain Thmanyah, deep green, warm white, sand gold, and real Kuwait photography as the shared identity.
- **Do** keep native dp/sp, Canvas coordinates, and browser px distinct when extending the system.
- **Do** preserve visible focus, Arabic direction, isolated Latin identifiers, and native control semantics.
- **Do** show source, archive, preview, unavailable, and stale states where they affect interpretation.
- **Do** test new layouts at the relevant emulator or browser sizes and record physical-device limitations separately.

### Don't:

- **Don't** turn quiet utility rows into decorative photographic cards or remove the reading veils from native photo views.
- **Don't** describe hiding an app as disabling it or present pending checklist choices as completed changes.
- **Don't** populate Ramadan 1448 with calculated or archived dates in place of verified records.
- **Don't** claim universal television compatibility from emulator screenshots or imply that a paid photo license was purchased.
- **Don't** replace installed app artwork with fabricated uniform brand tiles.


Release 1.8 refinement: the five requested streaming brands use edge-to-edge native 16:9 banners through BrandBanner.java. Brand-colored backgrounds fill the card; proportional marks have no inset mobile-icon box. Other apps retain their supplied TV banner when available. The user-provided Kuwait sunset was regenerated to place towers between the prayer panel and clock; its source is1672×941, not native4K. Previous licensed4K choices remain available.


## KLightTV 1.9.0

The product is now named KLightTV. Android package identifiers, preferences, and signature permission remain unchanged for upgrade compatibility. The composed sunset wallpaper retains its central tower corridor and now uses an AI-detail-enhanced export at3840×2160; native generation remained1672×941. Local bitmaps and minute-driven native rendering remain the implementation. No new runtime image network requests or resident services.


## KLightTV1.10.0

Shared regional prayer resolution distinguishes published offsets from geographic calculations. Khiran has no verified published offset and stays labelled calculated. Native reading settings use the existing deep-green/gold/Thmanyah system with a two-column draft preview and explicit save/cancel. Display and performance options are shared through the signature-protected provider; low-memory mode caps bitmap resolution, preserves native text and removes optional transitions. No new resident service or tracker.

Prayer times and remaining duration use static split-clock tiles: fixed digit cells, zero-padded hours and minutes, a subtle center seam, and separate colons/AM-PM. Native Canvas keeps digit order stable in RTL and adds no animation loop. The large primary clock remains unboxed. Preview, home and saver share the tile renderer.
