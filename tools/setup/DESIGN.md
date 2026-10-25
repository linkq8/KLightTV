# First-run setup preview

Mode: Operate. Arabic RTL, prayer-first, two pages. This scoped record supersedes the earlier six-step planner design. Final native TV implementation is pending.

## Flow
Country/city → appearance/reading → saved preview. Two choices on each page; prayer method follows country. No sidebar, app manager, hardware inspector, file export or advanced prayer controls in onboarding. Remote and application requests remain untouched. Completed preview skips onboarding on the same browser origin and exposes Edit.

## Visual language
Pearl light and charcoal dark, static translucent surface without runtime blur. A small inline vector screen diagram explains prayer, location backgrounds and applications. No logo, marketing copy or photographic assets. Tahoma/Arial inherited prototype font. Native typography is not changed.

## Tokens and layout
CSS custom properties: bg, sheet, fg, muted, line, field, focus, button, buttontext. Main max-width 1320px; two columns with 80px gap. At 850px and below, single column and hidden illustration. Text 22px, heading 34px, hero 44px; mobile text 18px. Controls minimum 58px. Surface radius 16px; controls 12px.

## Accessibility and states
Labeled country/city, semantic fieldsets for theme and reading, aria-pressed choices, visible 3px keyboard focus, step text, focused headings on navigation, error live alert. Pending country prevents continuation until selecting a city. Search ignores stale responses. No motion. Completion explicitly states computer preview and no TV application.

## Verification
Browser tests cover two pages, Saudi/Egypt preset mapping with mocked geocoding, pending city gate, theme/size persistence, repeat visit skipping, editing, mobile overflow and unauthenticated API rejection. Screenshots in outputs/setup-verification/first-light.png and second-dark.png. Native remote navigation, verified prayer schedules and physical TV performance remain pending.

## Native implementation
The Android launcher now has a separate native two-page SetupActivity and optional CustomizationActivity on page two. It reuses bundled Thmanyah typography, vector drawing and remote focus states. The browser remains a preview with broader country search. Native availability is explicitly Kuwait-only until global runtime validation. Native screenshots and emulator test details are recorded in docs/NATIVE-SETUP.md; native setup appearance does not reskin the older management pages.
