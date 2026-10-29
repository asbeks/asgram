---
name: ASGRAM
description: ASGRAM native identity and archived media-surface reference.
colors:
  primary: "#176b49"
  background: "#f5f8f5"
  surface: "#ffffff"
  text: "#13281c"
  secondary: "#52665a"
  tint: "#e4f1e8"
  outline: "#dce6dd"
  inverse: "#ffffff"
  primary-dark: "#9bd7b6"
  background-dark: "#111814"
  surface-dark: "#19231d"
  text-dark: "#ecf3ed"
  secondary-dark: "#b4c5b9"
  tint-dark: "#253f30"
  outline-dark: "#34483b"
  inverse-dark: "#143422"
typography:
  section:
    fontFamily: "sans-serif"
    fontSize: "21sp"
    fontWeight: 700
  title:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 700
  reading:
    fontFamily: "sans-serif"
    fontSize: "17sp"
    fontWeight: 400
  body:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 400
  label:
    fontFamily: "sans-serif"
    fontSize: "13sp"
  caption:
    fontFamily: "sans-serif"
    fontSize: "12sp"
    fontWeight: 400
  action:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 700
rounded:
  photo: "12dp"
  destination-icon: "14dp"
  surface: "16dp"
  chip: "24dp"
spacing:
  compact: "6dp"
  tight: "8dp"
  related: "10dp"
  regular: "12dp"
  card-gap: "14dp"
  content: "16dp"
  screen: "20dp"
  reader: "22dp"
  section: "24dp"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.inverse}"
    typography: "{typography.title}"
    rounded: "{rounded.surface}"
    padding: "14dp 20dp"
  button-secondary:
    backgroundColor: "{colors.tint}"
    textColor: "{colors.primary}"
    typography: "{typography.title}"
    rounded: "{rounded.surface}"
    padding: "14dp 20dp"
  button-primary-dark:
    backgroundColor: "{colors.primary-dark}"
    textColor: "{colors.inverse-dark}"
    typography: "{typography.title}"
    rounded: "{rounded.surface}"
    padding: "14dp 20dp"
  chip-selected:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.inverse}"
    typography: "{typography.action}"
    rounded: "{rounded.chip}"
    padding: "0dp 17dp"
    height: "48dp"
  search:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.surface}"
    height: "52dp"
  article-card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.surface}"
    padding: "16dp 16dp 6dp"
---

# Design System: ASGRAM

## Current native client — 29 October 2026

The existing forest-green brand now extends into the normal Telegram client. `IntroActivity` is a native Android welcome screen: ASGRAM masthead, language and appearance controls, a code-drawn A/conversation mark, a 36sp two-line headline, 16sp supporting copy, one 56dp Start Messaging button and the unofficial-client disclosure. It uses a fill-viewport ScrollView, 28dp side gutters and Android status/navigation insets. No media feed, fictional chat list or account gate replaces messaging.

`asgram.attheme` and `asgram-night.attheme` carry the same palette into native login, chat list, message bubbles, composer and settings. Original themes remain available; first-launch migration preserves custom/imported themes. Light UI uses white, forest `#176B49`, text `#13281C`, secondary `#52665A` and tint `#E4F1E8`. Dark UI uses `#19231D` surfaces, `#111814` ground, mint `#9BD7B6`, text `#ECF3ED`, secondary `#B4C5B9` and tint `#253F30`. The button uses white on forest or dark green on mint. Headers, secondary text and actions are checked for 4.5:1 contrast. Android system type, ripple feedback and native Back behavior remain the interaction language.

A separate A notification glyph replaces the application notification branding. Functional messaging icons and required Telegram attribution remain native. An optional community dialog offers View channel and Not now; the channel subscription remains a separate native Telegram action. The normal sign-in screen omits the development-backend checkbox.

The sections below retain the prior media-hub design as a future reference. They are not the active app home.

## Archived media-hub reference

The former media welcome and hub have no active navigation entry. The following tokens and custom components document that retained experiment. The current app uses the native welcome and themes described above, followed by LoginActivity and DialogsActivity.

**Creative North Star: "The Compact Native Destination Directory"**

ASGRAM's own surfaces use forest green, pale green, white and near-black text to make Abdii Sabaa destinations and public stories easy to find on an Android phone. Rounded native controls and short, direct labels give the interface a calm, practical character. The supplied Abdii Sabaa logo remains a real identity asset; the separate ASGRAM mark identifies the client.

This records the archived media experiment, extracted from `AsgramUi.java`, `AsgramWelcomeActivity.java` and `AsgramHubActivity.java`. Measurements are Android density-independent pixels (`dp`) and scaled text units (`sp`), not web CSS units. The welcome screen, media hub, article reader and ASGRAM settings are native Java views. Telegram's existing native screens own account and messaging conventions; this document does not replace their full visual system or imply that authenticated flows have been verified.

**Key Characteristics:**

- Compact destination rows and vertically stacked public story cards.
- Forest-green actions with tonal surfaces in paired light and dark palettes.
- Android system sans, scaled text and familiar Back, dialog, switch and keyboard behavior.
- Flat resting surfaces, native ripple feedback on filled and tonal buttons.
- Real media identity and public channel imagery, with user-controlled photo loading.

## Colors

The palette is green-led and quiet: pale surfaces support near-black reading text in light mode; dark green surfaces support pale text and a mint action color in dark mode. The frontmatter values reproduce `AsgramUi.Palette`; unsuffixed keys are light mode and `-dark` keys are their dark-mode counterparts.

### Primary

- **Forest Action / Mint Action** (`primary`, `primary-dark`): filled actions, selected category chips, active navigation, source names, and text actions.
- **Leaf Tint / Deep Leaf Tint** (`tint`, `tint-dark`): secondary buttons, selected navigation icon grounds, destination icon grounds and unloaded photo areas. These are tonal support for the same green identity, not a second accent.

### Neutral

- **Pale Green Ground / Night Green Ground** (`background`, `background-dark`): screen and action-bar backgrounds.
- **White Surface / Raised Night Green** (`surface`, `surface-dark`): cards, search field and bottom navigation.
- **Ink Green / Pale Reading Green** (`text`, `text-dark`): headings and primary reading text.
- **Muted Green / Muted Pale Green** (`secondary`, `secondary-dark`): descriptions, timestamps, hints, inactive navigation and disclosure copy.
- **Leaf Divider / Night Divider** (`outline`, `outline-dark`): the settings separator.
- **White on Forest / Deep Green on Mint** (`inverse`, `inverse-dark`): text inside primary filled controls.

**The Paired Palette Rule.** Choose every screen color through the current `Palette`; change background, surface, reading, secondary and action roles together when appearance changes.

The Abdii Sabaa media logo uses its supplied colors on a white holder, including in dark mode. Its colors are an asset exception, not additional UI accent tokens. The broader identity's red is not implemented as a recurring custom UI accent and is not assigned a token here.

## Typography

**Display Font:** Android `sans-serif`.
**Body Font:** Android `sans-serif`.

The custom text helper uses `Typeface.NORMAL` or `Typeface.BOLD`, disables extra font padding and adds native line spacing. There is no separate display face, mono family, tracked uppercase system or fixed-ratio type scale.

### Hierarchy

- **Section:** bold, used for recurring feed and settings section headings.
- **Title:** bold, used in destination rows, settings labels and buttons.
- **Reading:** normal, used for the welcome description and full article body. Article body adds (7dp) to native line spacing and remains selectable.
- **Body:** normal, used for story excerpts, explanatory copy and privacy information.
- **Label:** normal or bold by function; destination descriptions are normal, source names and save actions are bold.
- **Caption:** normal, used for status, disclosures and build information.
- **Action:** bold, used for story reading actions and selected filter labels.

Local display sizes distinguish individual surfaces: the welcome heading uses (35sp), the hub masthead (29sp), settings introduction (27sp), article heading (23sp) and story-card heading (19sp). These are observed compositions, not a reusable proportional scale. Navigation labels and timestamps use (11sp); they are supporting information, not the body-reading standard. Default helper text adds (3dp) line spacing to the platform metrics rather than specifying an absolute line height.

**The Native Type Rule.** Express text sizes in `sp` and preserve Android font scaling; use font weight and size to establish hierarchy before adding decoration.

## Layout

The custom screens are vertical native layouts. The welcome screen is a scrollable column with (28dp) horizontal padding; the hub uses (20dp) content gutters. The reader and settings use (22dp) side padding. The hub body is centered and capped at (780dp); the source has no multi-column layout or named device breakpoints.

The hub keeps its masthead, search, horizontally scrolling category chips and status above the flexible story scroller, with a persistent navigation row below. Category chips are the category selector. Story cards stack with the `card-gap` spacing; the article reader gives the full heading and body their own blocks. Long previews are limited to four heading lines and two excerpt lines, with ellipses; full reading happens in the native reader.

Use the frontmatter spacing vocabulary for related content, card internals and section separation. Safe-area space comes from `AndroidUtilities.statusBarHeight` and `navigationBarHeight` on the relevant screens. The bottom row adds at least (8dp) or the navigation-bar inset, whichever is larger. Keep touch targets distinct from visual glyph size: chips and article actions occupy (48dp), main buttons have a minimum (52dp), and bottom navigation items a minimum (56dp).

## Elevation & Depth

Custom ASGRAM surfaces have no explicit resting shadows or view elevation. Depth comes from screen/surface color changes, green tint grounds, rounded clipping and whitespace. Native Telegram dialogs and system controls keep their platform behavior; no custom shadow or motion timing has been extracted for them.

**The Tonal Depth Rule.** Separate custom resting surfaces through `background`, `surface` and `tint`; do not introduce a shadow ladder that the native custom screens do not use.

## Shapes

Soft rectangular surfaces recur across buttons, search and cards. Photos have a slightly tighter radius, destination icon grounds sit between photo and surface radii, and selected chips form capsules. Dividers are a single (1dp) line. Cards and the search field have no explicit outline stroke.

Custom glyphs are drawn with native Canvas paths using rounded caps and joins and a (1.8) stroke in a (48 × 48) drawing coordinate space, then scaled to the view. The ASGRAM mark uses a rounded green tile with a white letterform. Preserve the supplied Abdii Sabaa logo's aspect ratio with `FIT_CENTER`; do not redraw it as a generic media symbol.

## Components

### Buttons

Confident filled actions and quiet tonal alternatives share the same shape and spacing. Use `button-primary` for the leading action and `button-secondary` for supporting destinations or preferences. Both use centered bold title text, a minimum (52dp) height, and the frontmatter padding and radius. Dark mode swaps to the paired palette roles.

The native helper applies `RippleDrawable` feedback with a black overlay in light mode (alpha `0x20`) and white in dark mode (alpha `0x33`). It does not define web hover animation, a custom focus ring, or a disabled/error variant. Preserve native accessibility and interaction semantics when extending it.

### Chips

The category selector is a horizontal row with (48dp) targets, (17dp) horizontal padding and (5dp) gaps. Selected chips use primary/inverse colors, bold action text and a capsule background. Unselected chips use normal (14sp) secondary text on the screen ground. Selection is exposed through `setSelected`; the saved view clears category-selection presentation.

### Cards / Containers

Story cards are flat surface rectangles with the frontmatter shape and padding. The first row pairs a primary-colored source name with a secondary timestamp. Optional real public-channel photography uses rounded clipping; otherwise the area can offer **Load photo**. The heading, excerpt and bottom reading/save/share actions form distinct rows. Save state is conveyed by **Save** / **Saved** wording, without a fabricated counter.

The native reader expands the full heading and selectable body. Preview truncation is not a full-article rule. Its primary action opens the original Telegram post and its tonal action toggles the local save state.

### Inputs / Fields

Search is a single-line native `EditText` inside a surface container. Its leading search glyph occupies (48dp); the field uses (15sp) text, primary reading color, a secondary hint and (12dp) trailing padding. It filters displayed public news and education as text changes. No custom error, border or focus transition is implemented; retain the Android cursor, keyboard and text-entry behavior.

### Navigation

The hub bottom navigation has four equal-width native columns: Chats, Home, Saved and Settings. Each combines a glyph with a small label. A selected Home or Saved item uses primary color, bold label and a tint capsule behind a (56 × 32dp) icon area; inactive items use secondary color. The row sits on `surface`. The Telegram action bar supplies Back and refresh. Other Telegram messaging surfaces retain their inherited navigation patterns.

### Destination Rows and Identity

Welcome destinations are compact horizontal rows. Each begins with a (48dp) icon ground, followed by a bold title and smaller secondary description with a (16dp) separation from the icon. Row padding is (10dp) vertically. The client mark and wordmark establish ASGRAM; the media logo identifies Abdii Sabaa. Keep these identities distinct from Telegram's official mark.

### Native Preferences

Appearance uses a native dialog with device/light/dark choices. Data saving uses an Android switch with a text explanation. Notification preferences open the Android settings surface. Their native interaction, focus and dialog treatment are inherited, not a new ASGRAM component skin.

## Do's and Don'ts

### Do:

- **Do** use the paired native palette roles for complete light and dark screens.
- **Do** keep Android `sp` text scaling, safe-area insets and at least (48dp) touch targets for custom main actions.
- **Do** preserve the real media logo and real public-channel imagery with their aspect ratios.
- **Do** keep heading, excerpt, metadata and actions visually distinct in a story card.
- **Do** carry Telegram's native account and messaging conventions into inherited screens.
- **Do** show loading, refresh failure, empty, saved and photo-loading states with readable native text.

### Don't:

- **Don't** replace native Android screens with web wrappers or treat the sidecar's HTML previews as application source.
- **Don't** use the official Telegram logo as ASGRAM's client identity.
- **Don't** invent a red UI accent, font pairing, shadow scale or animation timing that the custom source does not define.
- **Don't** promote preview truncation, duplicate category navigation or accidental article whitespace into reusable system rules.
- **Don't** treat public-feed rendering as evidence that account sign-in, messaging, calls or push notifications have passed runtime verification.
