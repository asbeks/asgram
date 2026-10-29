---
name: ASGRAM
description: A media-first native Android Telegram client that follows the selected Telegram theme.
typography:
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
  action:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 700
  section:
    fontFamily: "sans-serif"
    fontSize: "21sp"
    fontWeight: 700
rounded:
  photo: "12dp"
  surface: "16dp"
  chip: "24dp"
  channel-circle: "28dp"
spacing:
  tight: "8dp"
  related: "10dp"
  regular: "12dp"
  media-gap: "14dp"
  content: "16dp"
  screen: "20dp"
  reader: "22dp"
  section: "24dp"
components:
  button-primary:
    typography: "{typography.title}"
    rounded: "{rounded.surface}"
    padding: "14dp 20dp"
  button-secondary:
    typography: "{typography.title}"
    rounded: "{rounded.surface}"
    padding: "14dp 20dp"
  chip-selected:
    typography: "{typography.action}"
    rounded: "{rounded.chip}"
    height: "48dp"
  feed-chip:
    padding: "0dp 18dp"
    height: "48dp"
    rounded: "{rounded.chip}"
  lead-card:
    rounded: "{rounded.surface}"
  photo:
    rounded: "{rounded.photo}"
  channel-circle:
    rounded: "{rounded.channel-circle}"
    size: "56dp"
---

# Design System: ASGRAM

## Overview

**Creative North Star: "The Native Media Front Desk"**

ASGRAM puts the Abdii Sabaa network and real public stories at the front of a native Telegram client. A channel rail, clear topic selection and a prominent first story make the Feed easy to scan. Chats remain one tap away. The publisher's supplied logo, ASGRAM naming and client artwork carry the identity; the selected Telegram theme supplies interface colors.

This is the current internal Android design record, derived from `AsgramHubActivity.java`, `AsgramSpacesActivity.java`, `AsgramUi.java`, `MainTabsActivity.java` and native theme sources. The Feed is active and first in root navigation. It supersedes the archived-directory guidance and fixed forest palette recorded previously. The app remains native Android Java, with Telegram's existing account, messaging, theme controls and media viewer. This document records implemented design, not full feature verification or release status.

**Key Characteristics:**

- A media-first Feed with a real publisher/channel rail and a lead story followed by compact stories.
- User-selected Telegram theme roles shared across Feed, Spaces and inherited screens.
- Android system sans, scaled text, native navigation, dialogs, keyboard and touch feedback.
- Flat custom story surfaces alongside inherited Telegram navigation and full-screen media behavior.
- Visible loading, cached, failure, empty, saved, photo-loading and album-count states.

## Colors

ASGRAM follows the selected Telegram theme instead of imposing a forest-green interface. Color primitives are deliberately omitted from the frontmatter: the production source resolves native `Theme` keys at runtime, so a fixed light/dark hex palette would misstate the system. The bindings below are the source of truth. The sidecar records those native bindings and uses explicitly illustrative theme-asset snapshots for its HTML previews.

### Primary

- **Selected Theme Accent** (`Palette.primary`): resolved from `Theme.key_windowBackgroundWhiteBlueText`; used for source metadata, action text, selected category fills and custom glyphs.
- **Selected Theme Tint** (`Palette.tint`): resolved from `Theme.key_groupcreate_spanBackground`; used for tonal buttons, channel grounds, unloaded photos and native selector feedback. It is support for the selected theme, not a separate brand accent.
- **Readable Filled-Control Text** (`Palette.inverse`): white when its calculated contrast against `primary` is at least (4.5:1), otherwise near-black (`0xff111111`). The foreground is calculated from the actual selected accent, including in dark themes.

### Neutral

| Custom role | Native source | Use |
| --- | --- | --- |
| `background` | `Theme.key_windowBackgroundGray` | Feed and Spaces grounds, custom action bars, reader and media settings |
| `surface` | `Theme.key_windowBackgroundWhite` | Lead story, root navigation and native surface roles |
| `text` | `Theme.key_windowBackgroundWhiteBlackText` | Headings, primary copy and reading text |
| `secondary` | `Theme.key_windowBackgroundWhiteGrayText2` | Description, time, status and inactive labels |
| `outline` | `Theme.key_divider` | Compact story and settings separators |

**The Selected Theme Rule.** Resolve custom screen colors through `AsgramUi.Palette` and the current native `Theme`; do not freeze ASGRAM's former forest colors into new surfaces.

The media logo keeps its supplied green, red, black and white artwork on a white circular holder. The launcher/splash uses ASGRAM artwork rather than Telegram's official mark. These identity assets do not force the surrounding UI palette. Album-count labels use white over a translucent dark ground for legibility on real media; this local overlay is not a global surface color.

`Theme.java` migrates only prior ASGRAM default selections to Day or Dark Blue, retaining user-selected/custom themes. Appearance is controlled by the native `ThemeActivity`, reached from **Chat themes & appearance** in media settings. Legacy ASGRAM theme assets remain available choices; they are not mandatory defaults.

## Typography

**Display Font:** Android system sans.
**Body Font:** Android `sans-serif` in custom ASGRAM views; inherited Telegram views retain their native type helpers.

Custom text uses normal or bold weight, disables extra font padding and adds (3dp) to native line spacing. Sizes are Android scaled pixels (`sp`), not CSS pixels. There is no extracted fixed-ratio scale, separate editorial display face or mono system.

### Hierarchy

- **Title:** bold destination/action names, shared buttons and Spaces rows.
- **Reading:** normal article text, selectable in the native reader, with (7dp) added line spacing.
- **Body:** descriptions, excerpts, privacy information and explanatory copy.
- **Label:** source/save details and supporting settings information; weight follows its function.
- **Caption:** small status and channel labels; channel names and source metadata are bold, status text is normal.
- **Action:** bold reading actions and selected category labels.
- **Section:** bold settings section headings.

Local compositions use larger bold headings without creating a second font system: Feed is (32sp), Spaces (29sp), media settings introduction (27sp), article heading (23sp), lead-story heading (22sp), and compact-story heading (18sp). The Feed-direction brief's earlier (30sp) title is superseded by the built (32sp) value. Both story-heading variants are limited to three lines; the reader retains the full heading.

**The Native Type Rule.** Preserve Android font scaling and native type metrics; use size, weight and spacing to distinguish title, metadata and body.

## Layout

The active root has five visible destinations: Feed, Chats, Spaces, Settings or Calls, and You. Feed occupies the first position and opens by default. Settings and Calls share one configurable slot in Telegram's inherited root navigation. They are not six simultaneous tabs. Search, saved stories and refresh belong to the Feed action bar; saved stories are a Feed state rather than a root tab.

Feed and Spaces use vertical `RecyclerView` lists with `LinearLayoutManager` and disabled item animation. Their headers scroll with the content. Each has (20dp) side gutters, status/action-bar top clearance and bottom padding equal to the navigation inset plus (104dp), allowing content to scroll clear of root navigation. The earlier custom (780dp) content cap and fixed four-column bottom row are not part of this implementation. Native root navigation has its own maximum width of (408dp), rounded surface and safe-area handling.

The first viewport presents the native ASGRAM bar, Feed heading, real publisher/network rail, topic row, refresh status and first story. The rail has four equal-width destinations with (56dp) circular grounds and labels below. Additional verified destinations can appear in a separate horizontal scroll row. Topic controls scroll horizontally; scrolling is intentional when the row exceeds the phone width.

The lead story uses a surfaced container and optional wide (190dp) media preview. Later stories are compact rows with optional (92dp) square media at the trailing edge, separated by a divider. The full reader and media settings use (22dp) horizontal padding, navigation insets and vertical scrolling. Reader media is (230dp) high before expansion.

**The Chronology Rule.** Latest is the initial default and sorts by descending timestamp without photo promotion. For you is an explicit saved preference that can promote one recent photo post while preserving the remaining order. Topic labels are Latest, For you, News, Learning and Sports; Learning maps to the Education category. Do not turn this Feed-specific ordering into a global navigation rule.

Shared primary/tonal buttons have a minimum (52dp) height. Category chips and read/save/share rows use (48dp) targets. Small glyph geometry does not define the touch target. Preserve safe-area space and Android Back behavior as these screens evolve.

## Elevation & Depth

Custom Feed and Spaces content is flat at rest: no explicit card shadow or view-elevation ladder is applied. Surfaces, tint grounds, separators and whitespace create hierarchy. Feed and Spaces action bars explicitly disable cast shadows. This does not prohibit Telegram's native depth, blur, animation or full-screen overlays: the inherited main tabs, dialogs and `PhotoViewer` own their existing behavior.

**The Native Depth Rule.** Keep custom story/list surfaces flat while preserving the inherited Telegram component's own depth and interaction behavior.

The main tab container currently uses a rounded theme surface. Shared filled/tonal buttons use `RippleDrawable` with a light-mode black overlay (alpha `0x20`) or dark-mode white overlay (alpha `0x33`); no custom duration or easing is defined. Feed chips use Telegram's rounded native selector. Web hover/focus behavior in sidecar examples is solely for documentation preview interaction.

## Shapes

Custom content uses softly rounded rectangles. Shared buttons and the lead card use the surface radius; media previews use the tighter photo radius; selected filters use capsule ends; channel grounds are circles. Compact story separators are (1dp). Spaces uses (50dp) circular peer avatars rather than generic media illustrations.

Canvas glyphs use rounded stroke caps/joins, a (1.8) stroke in a (48 × 48) drawing space, then native scaling to the view. The supplied publisher logo uses `FIT_CENTER`. It is an identity asset, not a replacement for the selected theme's action color.

## Components

### Buttons

Primary and tonal buttons are direct, legible native controls. Both use bold title text, centered content, the shared surface radius and (14dp) vertical / (20dp) horizontal padding. Primary uses `primary` with calculated `inverse`; tonal uses `tint` with `primary`. Native ripple feedback comes from the current light/dark mode. No global custom disabled/error styling has been extracted; per-action text such as **Joining…** explains the local state.

### Chips

Feed filters use (48dp) height and (18dp) horizontal padding. Selected filters have a `primary` fill, `inverse` text and bold action typography. Unselected filters use secondary normal text on a transparent ground. Native rounded selectors use `tint` feedback. Spaces reuses the capsule/selected-color language with its own (16dp) horizontal padding and All, Private, Groups, Channels and Bots choices. Selection is exposed with native selected state.

### Cards / Containers

The lead story has a surfaced, rounded outer container. Text receives (16dp) inset; metadata precedes the heading, and an optional excerpt follows with separate spacing. The excerpt is limited to two lines and shown only when the lead heading is shorter than (130) characters and a body exists. This compact preview rule does not truncate the full native reader.

Following stories use compact text/media rows on the screen ground instead of repeating elevated cards. Titles are limited to three preview lines. Each story has native read/open, save and share actions. Wording adapts to **Read story**, **Open video** or **Open post · N items**; **Save** / **Saved** conveys local save state. Real timestamps and source names provide provenance without invented engagement numbers.

### Inputs / Fields

Feed and Spaces use Telegram action-bar search fields, including native text entry, cursor, keyboard and search-field expansion. Feed search filters media text/author; Spaces search filters existing dialog titles. There is no separate rounded ASGRAM search box in the current Feed. Link input and creation choices use native dialogs.

### Navigation

`MainTabsActivity` owns the persistent five-position navigation. Feed uses the article glyph, Chats the native chat glyph, Spaces the native gallery glyph, Settings/Calls their native alternatives, and You the actual account avatar. Selected/animated states and unread indicators remain native. Root tabs use theme surfaces, native icons and labels; do not reconstruct them as the earlier Home/Saved toolbar.

Spaces is a native directory over the account's existing private/secret chats, groups, channels and bots, with actual peer avatars, names, kinds and unread counts. Its Create action opens native choices; it does not create server folders merely by filtering. Contacts remains an explicit action within Spaces.

### Channel Rail and Invitation

The rail pairs the real Abdii Sabaa logo with native University, Sports and Community glyphs, each with a readable label. Opening a destination and subscribing are separate actions. The optional dismissible publisher invitation presents an explicit **Join Abdii Sabaa** and **Not now** choice; a joining state disables repeat taps. Additional official destinations are sourced through the explicit `ASGRAM:` directory line and verification, not inferred from advert links.

### Media and Albums

`BackupImageView` loads real Telegram media. Data saving can replace a preview with a **Load photo** action. A multi-item post remains one Feed story, with an item-count overlay and **Tap to expand** wording. Tapping available native media opens Telegram's `PhotoViewer` with the photo/video album items, retaining its swipe, thumbnail, zoom, sharing and Back behavior. If native media is unavailable, the source Telegram link is the fallback.

The reader keeps the full heading and selectable body separate. It offers native media expansion, the original Telegram post and a local save toggle. Do not apply the Feed's three-line heading cap to this reader or reproduce the album as separate fake Feed posts.

### Native Preferences

Media settings opens `ThemeActivity(THEME_TYPE_BASIC)` for appearance. Android's switch controls automatic photo loading; Android notification preferences handle notification settings. Telegram's existing chat themes, messaging settings and account controls retain their native screens. There is no separate media-only appearance palette to keep in sync.

## Do's and Don'ts

### Do:

- **Do** resolve custom colors through the current Telegram theme and preserve the user's selected theme.
- **Do** retain ASGRAM launcher/client artwork and the actual publisher logo without forcing their colors onto app surfaces.
- **Do** keep native Android text scaling, safe-area insets and (48dp) main action targets.
- **Do** distinguish lead story, compact following stories, metadata, body and actions through hierarchy and spacing.
- **Do** preserve Latest chronology, explicit For you selection and a full-text native reader.
- **Do** keep albums as one story and expand available items through native PhotoViewer.
- **Do** make loading, partial failure, empty, saved and data-saving states visible in readable native text.

### Don't:

- **Don't** restore fixed forest-green screen defaults or overwrite custom themes to match branding.
- **Don't** replace the current five-position navigation with the obsolete Chats/Home/Saved/Settings row.
- **Don't** treat a chosen Feed photo, preview line cap or source-image whitespace as a universal design token.
- **Don't** invent engagement counts, silently subscribe accounts or infer official channels from personal/advert links.
- **Don't** replace native Android production screens with the sidecar's illustrative HTML/CSS examples.
- **Don't** describe the current internal build as fully verified or publicly released on the strength of screenshots or this design record.
