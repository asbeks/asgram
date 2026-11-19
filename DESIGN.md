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
  post-avatar: "21dp"
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
    padding: "0dp 16dp"
    height: "36dp"
    rounded: "{rounded.chip}"
  sort-tab:
    height: "48dp"
  timeline-post:
    padding: "12dp 20dp 0dp"
  post-actions:
    height: "48dp"
  photo:
    rounded: "{rounded.photo}"
  post-avatar:
    rounded: "{rounded.post-avatar}"
    size: "42dp"
---

# Design System: ASGRAM

## Overview

**Creative North Star: "The Native Social Timeline"**

ASGRAM puts real public channel posts at the front of a native Telegram client. A compact app bar, clear sorting and topic controls lead directly into repeated publisher-first posts: identity, text, optional media and actions. Chats remain one tap away. The publisher's real identity, ASGRAM naming and client artwork carry the identity; the selected Telegram theme supplies interface colors.

This is the current native Android design record, derived from `AsgramHubActivity.java`, `IntroActivity.java`, `AsgramSpacesActivity.java`, `AsgramUi.java`, `MainTabsActivity.java` and native theme sources. The Feed is active and first in root navigation. Its social timeline follows comp A in `.impeccable/mocks/feed-social-timeline-20261118.png`; the built native source resolves the actual measurements and behavior. The app remains native Android Java, with Telegram's existing account, messaging, theme controls and media viewer. This document records implemented design, not full feature verification or release status.

**Key Characteristics:**

- A media-first Feed with repeated publisher, text, media and action blocks, without a separate lead-card layout.
- User-selected Telegram theme roles shared across Feed, Spaces and inherited screens.
- Android system sans, scaled text, native navigation, dialogs, keyboard and touch feedback.
- Flat custom story surfaces alongside inherited Telegram navigation and full-screen media behavior.
- Visible loading, cached, failure, empty, saved and data-saving states, with in-post media galleries.

## Colors

ASGRAM follows the selected Telegram theme instead of imposing a forest-green interface. Color primitives are deliberately omitted from the frontmatter: the production source resolves native `Theme` keys at runtime, so a fixed light/dark hex palette would misstate the system. The bindings below are the source of truth. The sidecar records those native bindings and uses explicitly illustrative theme-asset snapshots for its HTML previews.

### Primary

- **Selected Theme Accent** (`Palette.primary`): resolved from `Theme.key_windowBackgroundWhiteBlueText`; used for reader metadata, sort selection, selected category fills and custom glyphs.
- **Selected Theme Tint** (`Palette.tint`): resolved from `Theme.key_groupcreate_spanBackground`; used for tonal buttons, unselected Feed chips, unloaded photos and native selector feedback. It is support for the selected theme, not a separate brand accent.
- **Readable Filled-Control Text** (`Palette.inverse`): white when its calculated contrast against `primary` is at least (4.5:1), otherwise near-black (`0xff111111`). The foreground is calculated from the actual selected accent, including in dark themes.

### Neutral

| Custom role | Native source | Use |
| --- | --- | --- |
| `background` | `Theme.key_windowBackgroundGray` | Feed and Spaces grounds, custom action bars, reader and media settings |
| `surface` | `Theme.key_windowBackgroundWhite` | Root navigation and native surface roles |
| `text` | `Theme.key_windowBackgroundWhiteBlackText` | Headings, primary copy and reading text |
| `secondary` | `Theme.key_windowBackgroundWhiteGrayText2` | Description, time, status and inactive labels |
| `outline` | `Theme.key_divider` | Timeline-post and settings separators |

**The Selected Theme Rule.** Resolve custom screen colors through `AsgramUi.Palette` and the current native `Theme`; do not freeze ASGRAM's former forest colors into new surfaces.

The media logo keeps its supplied green, red, black and white artwork on a white circular holder. The launcher/splash uses ASGRAM artwork rather than Telegram's official mark. These identity assets do not force the surrounding UI palette. Album-count labels use white over a translucent dark ground for legibility on real media; this local overlay is not a global surface color.

`Theme.java` migrates only prior ASGRAM default selections to Day or Dark Blue, retaining user-selected/custom themes. Appearance is controlled by the native `ThemeActivity`, reached from **Appearance** in Feed settings. Legacy ASGRAM theme assets remain available choices; they are not mandatory defaults.

## Typography

**Display Font:** Android system sans.
**Body Font:** Android `sans-serif` in custom ASGRAM views; inherited Telegram views retain their native type helpers.

Custom text uses normal or bold weight, disables extra font padding and adds (3dp) to native line spacing. Sizes are Android scaled pixels (`sp`), not CSS pixels. There is no extracted fixed-ratio scale, separate editorial display face or mono system.

### Hierarchy

- **Title:** bold destination/action names, shared buttons and Spaces rows.
- **Reading:** normal article text, selectable in the native reader, with (7dp) added line spacing.
- **Body:** descriptions, privacy information and explanatory copy. Timeline post previews use normal (16sp) text.
- **Label:** Feed topics, save details and supporting settings information; weight follows its function. Post actions use bold (13sp) labels.
- **Caption:** time, status and reader metadata; status and timeline timestamps are normal, reader source metadata is bold.
- **Action:** bold reader/Spaces actions and selected Spaces category labels. Feed sort labels use (15sp), bold when selected; Feed topic labels use (13sp).
- **Section:** bold settings section headings.

Local compositions use larger bold headings without creating a second font system: Spaces is (29sp), Feed settings introduction (27sp), article heading (23sp) and Feed empty-state heading (22sp). The timeline has no separate large Feed heading or special lead-story headline. Publisher names use bold (15sp), timestamps normal (12sp), and each post uses a single (16sp) text preview capped at five lines; the reader retains the full heading and body.

**The Native Type Rule.** Preserve Android font scaling and native type metrics; use size, weight and spacing to distinguish title, metadata and body.

## Layout

The active root has five visible destinations: Feed, Chats, Spaces, Settings or Calls, and You. Feed occupies the first position and opens by default. Settings and Calls share one configurable slot in Telegram's inherited root navigation. They are not six simultaneous tabs. Feed search and saved posts are action-bar controls. The overflow holds Channels, Refresh and Feed settings. Saved posts are a Feed state rather than a root tab.

Feed and Spaces use vertical `RecyclerView` lists with `LinearLayoutManager` and disabled item animation. Their headers scroll with the content. Each has (20dp) side gutters, status/action-bar top clearance and bottom padding equal to the navigation inset plus (104dp), allowing content to scroll clear of root navigation. The earlier custom (780dp) content cap and fixed four-column bottom row are not part of this implementation. Native root navigation has its own maximum width of (408dp), rounded surface and safe-area handling.

The first viewport presents the compact native ASGRAM bar, equal-width Latest/For you sort tabs, separate topic chips and the first publisher/post. There is no separate Feed masthead or channel rail. The sort row is (48dp) high, with a (45dp) label area and a (3dp) selected underline. Topic chips have a visible (36dp) capsule centered within a (48dp) interaction wrapper, with (8dp) gaps. The topics scroll horizontally when they exceed the phone width. Updating or partial-failure status appears only while applicable; a successful refresh leaves no permanent status row.

Every timeline post uses the same sequence and full content width inside the (20dp) screen gutters: publisher identity, text, optional (200dp) media, then three equal action targets and a divider. Publisher avatars are (42dp) circles. The full reader and Feed settings use (22dp) horizontal padding, navigation insets and vertical scrolling. Reader media is (230dp) high before expansion.

**The Chronology Rule.** Latest is the initial default and sorts by descending timestamp without photo promotion. For you is an explicit saved preference that can promote one recent photo post while preserving the remaining order. Sorting and topic selection are independent controls: topics are All, News, Education and Sports. Do not turn this Feed-specific ordering into a global navigation rule.

Shared primary/tonal buttons have a minimum (52dp) height. Feed topic wrappers, publisher rows and Open/Save/Share actions use at least (48dp) targets. Small glyph geometry does not define the touch target. Preserve safe-area space and Android Back behavior as these screens evolve.

## Elevation & Depth

Custom Feed and Spaces content is flat at rest: no explicit card shadow or view-elevation ladder is applied. Surfaces, tint grounds, separators and whitespace create hierarchy. Feed and Spaces action bars explicitly disable cast shadows. This does not prohibit Telegram's native depth, blur, animation or full-screen overlays: the inherited main tabs, dialogs and `PhotoViewer` own their existing behavior.

**The Native Depth Rule.** Keep custom story/list surfaces flat while preserving the inherited Telegram component's own depth and interaction behavior.

The main tab container currently uses a rounded theme surface. Shared filled/tonal buttons use `RippleDrawable` with a light-mode black overlay (alpha `0x20`) or dark-mode white overlay (alpha `0x33`); no custom duration or easing is defined. Feed chips use Telegram's rounded native selector. Web hover/focus behavior in sidecar examples is solely for documentation preview interaction.

## Shapes

Shared controls and media use softly rounded rectangles. Buttons use the surface radius; timeline media uses the tighter photo radius; topic filters use capsule ends. Timeline posts themselves are flat content blocks with (1dp) bottom separators, not rounded cards. Publisher avatars are (42dp) circles; Spaces retains (50dp) circular peer avatars.

Canvas glyphs use rounded stroke caps/joins, a (1.8) stroke in a (48 × 48) drawing space, then native scaling to the view. The supplied publisher logo uses `FIT_CENTER`. It is an identity asset, not a replacement for the selected theme's action color.

## Components

### Buttons

Primary and tonal buttons are direct, legible native controls. Both use bold title text, centered content, the shared surface radius and (14dp) vertical / (20dp) horizontal padding. Primary uses `primary` with calculated `inverse`; tonal uses `tint` with `primary`. Native ripple feedback comes from the current light/dark mode. No global custom disabled/error styling has been extracted; retain the native feedback supplied by each control.

### Chips

Feed sorting and topics have separate forms. Latest/For you are equal-width tabs with (15sp) text and a (3dp) selected underline; the selected label is bold and `primary`, while the other is normal and `secondary`. All/News/Education/Sports are (13sp) topic capsules with (16dp) horizontal padding. A selected topic uses `primary`/`inverse` and bold text; an unselected topic uses `tint`/`text`. The visible (36dp) capsule sits in a (48dp) wrapper that owns the click and accessibility description; native selector feedback uses `outline`. Spaces retains its existing (48dp) category controls and All, Private, Groups, Channels and Bots choices.

### Cards / Containers

Each post is a repeated flat block with a (12dp) top inset and the shared screen gutters. A real channel avatar, bold publisher name and timestamp precede the original post text. Publisher rows open the source channel. Post text is (16sp), limited to five preview lines with ellipsis, and opens the native reader when tapped. A short first line ending within (90) characters is bold within the same preview when a newline follows it; the remaining text stays normal. This preview limit is local Feed behavior, not a rule for the full reader.

Optional post media follows the text at the same content width. Open, Save/Saved and Share occupy equal-width (48dp) action targets; video posts use **Watch** in place of **Open**. Each action combines a (17dp) native glyph with a bold (13sp) label in a single centered button, using the theme secondary color and tint selector feedback. Watch opens available video media; Open presents the native reader. Real timestamps and source names provide provenance without invented engagement numbers.

### Inputs / Fields

Feed and Spaces use Telegram action-bar search fields, including native text entry, cursor, keyboard and search-field expansion. Feed search filters media text/author; Spaces search filters existing dialog titles. There is no separate rounded ASGRAM search box in the current Feed. Link input and creation choices use native dialogs.

### Navigation

`MainTabsActivity` owns the persistent five-position navigation. Feed uses the article glyph, Chats the native chat glyph, Spaces the native gallery glyph, Settings/Calls their native alternatives, and You the actual account avatar. Selected/animated states and unread indicators remain native. Root tabs use theme surfaces, native icons and labels; do not reconstruct them as the earlier Home/Saved toolbar.

Spaces is a native directory over the account's existing private/secret chats, groups, channels and bots, with actual peer avatars, names, kinds and unread counts. Its Create action opens native choices; it does not create server folders merely by filtering. Contacts remains an explicit action within Spaces.

### Publisher Identity and Channels

Posts identify their real publisher through the native channel avatar, name and timestamp. When an avatar is unavailable, Telegram's `AvatarDrawable` supplies the native initials fallback. Channels is a native overflow dialog listing Abdii Sabaa, University, Sports, Community and additional verified destinations. The former home channel rail and inline join invitation are absent from the timeline. Opening a destination remains separate from subscribing; official directory entries are verified rather than inferred from advert links.

### Media and Albums

`BackupImageView` loads Telegram previews. Each album appears in one Feed story and shows all its items in order. A single preview keeps the full content width. Multi-item galleries use two columns, with a full-width first item when the count is odd. Data saving offers a **Load previews** action. Tapping a native tile opens that item in `PhotoViewer`, where users can swipe through the album. Public web previews link to the original Telegram post.

The reader keeps the full heading and selectable body separate. It offers the same in-post gallery, the original Telegram post and a local save toggle.

### Native Preferences

Media settings opens `ThemeActivity(THEME_TYPE_BASIC)` for appearance. Android's switch controls automatic photo loading; Android notification preferences handle notification settings. Telegram's existing chat themes, messaging settings and account controls retain their native screens. There is no separate media-only appearance palette to keep in sync.

## Do's and Don'ts

### Do:

- **Do** resolve custom colors through the current Telegram theme and preserve the user's selected theme.
- **Do** retain ASGRAM launcher/client artwork and the actual publisher logo without forcing their colors onto app surfaces.
- **Do** keep native Android text scaling, safe-area insets and (48dp) main action targets.
- **Do** keep the repeated publisher, post text, optional media and action sequence clear through hierarchy and spacing.
- **Do** keep sort tabs independent of topic chips, preserve Latest chronology and retain a full-text native reader.
- **Do** keep albums as one story and expand available items through native PhotoViewer.
- **Do** make loading, partial failure, empty, saved and data-saving states visible in readable native text.

### Don't:

- **Don't** restore fixed forest-green screen defaults or overwrite custom themes to match branding.
- **Don't** replace the current five-position navigation with the obsolete Chats/Home/Saved/Settings row.
- **Don't** treat a chosen Feed photo, preview line cap or source-image whitespace as a universal design token.
- **Don't** invent engagement counts, silently subscribe accounts or infer official channels from personal/advert links.
- **Don't** replace native Android production screens with the sidecar's illustrative HTML/CSS examples.
- **Don't** describe the current internal build as fully verified or publicly released on the strength of screenshots or this design record.
