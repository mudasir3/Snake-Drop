# Typography & Palette Tokens
Direction: **Midnight arcade cabinet.** Ink-black field, acid-lime snake, hot-coral apple, six saturated chunk colors. High contrast, hard edges, zero gradients on UI chrome, zero drop shadows. Never use Material default purple/teal, rounded-pill buttons, or soft pastel cards.

- **Font display:** `Press Start 2P` (SIL OFL; bundle file + license in `res/font/`; verify license before shipping). Use for score, titles, button labels. Sizes: 10/14/20/32 sp only.
- **Font body:** System sans (`FontFamily.SansSerif`) 14/16 sp for Settings text and legal. Never bundle a second font.
- **Numerals:** Render score with display font, tabular by nature. Format with `Locale.ROOT`, thousands separator `,`.
- **Letter spacing:** Display +0.5 sp; body 0.
- **Case:** UPPERCASE for buttons and titles. Sentence case for body/overlay prompts.

Color tokens (define in `theme/Color.kt`; reference by name only, never by hex in UI code):
- **`Ink`:** `#0B0F1A` — background.
- **`Grid`:** `#1A2238` — arena/well grid lines (1 dp).
- **`Panel`:** `#121829` — well/arena backing fill.
- **`Snake`:** `#B6FF3B` — body. **`SnakeHead`:** `#E4FF8A` — head + eyes `#0B0F1A`.
- **`Apple`:** `#FF2E4D`.
- **`Danger`:** `#FF3B30` — danger line, crush warning pulse.
- **`ChunkPink`:** `#FF3D81`. **`ChunkAmber`:** `#FFB020`. **`ChunkCyan`:** `#25D9FF`. **`ChunkViolet`:** `#A66BFF`. **`ChunkMint`:** `#3DFF9A`. **`ChunkCoral`:** `#FF6B3D`.
- **`TextHi`:** `#E8F0FF`. **`TextDim`:** `#7C8AA8`.
- **`Accent`:** `#B6FF3B` — primary buttons (text `Ink`). **`AccentAlt`:** `#25D9FF` — secondary outlines.
- **Contrast rule:** Enforce ≥ 4.5:1 for all text on its background. Never put `TextDim` on `Panel` below 14 sp.
- **Chunk assignment:** Pick chunk color by `landedChunkCount % 6`. Never recolor a locked cell.
- **Glow:** Draw 2–3 concentric strokes at 18% / 10% / 5% alpha in the element's color. Never use blur.

# Layout Conventions
- **Grid:** `cell = floor(availableWidth / 12)`. Square cells. Center the board horizontally. Leftover pixels become equal side margins.
- **Vertical stack (top→bottom):** HUD bar (score left, best center, pause right) → arena (10 rows, shrinks) → danger line → well (9 rows). Arena + well share one continuous frame.
- **Safe areas:** Respect cutouts and gesture insets. Keep the bottom 8% of the screen free of touch-critical UI.
- **Spacing scale:** 4 / 8 / 12 / 16 / 24 / 32 dp only. Never use other values.
- **Touch targets:** ≥ 48 × 48 dp. Pause icon hit area 56 dp.
- **Shape:** Corner radius 2 dp on cells, 0 dp on buttons and panels. Never use fully rounded buttons.
- **Borders:** 2 dp solid `Accent`/`AccentAlt` outlines for secondary buttons. No elevation.
- **Home screen:** Left-aligned giant title (32 sp), best score below, one full-width PLAY button anchored to the thumb zone (bottom third). Icons (leaderboard, settings, remove ads) in a bottom row, 48 dp, outlined.
- **Game Over:** Dim board to 40%. Score in 32 sp. RETRY full-width primary. REVIVE (WATCH AD) full-width secondary outline. Home as text button.
- **Orientation:** Lock portrait.
- **Motion:** Durations 90 / 150 / 300 ms only. Easing linear or `FastOutSlowIn`. Screen shake ≤ 4 dp for 120 ms on chunk landing (toggleable). No parallax, no bouncing UI.
- **Danger line:** Pulse `Danger` stroke alpha 0.35↔1.0 at 2 Hz when stack height ≥ 6.

# UI Component Standards (Figma-to-Code mapping)
No Figma file exists. Treat this table as the design source of truth. Map each design token/element to exactly one code symbol.
- **`ArcadeButton(primary)` → `ui/ArcadeButton.kt`:** Fill `Accent`, text `Ink`, display font 14 sp, height 56 dp, radius 0.
- **`ArcadeButton(secondary)` → same file:** Transparent fill, 2 dp `AccentAlt` border, text `AccentAlt`.
- **`ArcadeIconButton` → `ui/ArcadeIconButton.kt`:** 48 dp, 2 dp `TextDim` border, vector icon `TextHi`. Draw icons as `ImageVector` in code. Never import PNG/SVG assets.
- **`ScoreText` → `ui/ScoreText.kt`:** Display font 20 sp, `TextHi`, `Locale.ROOT` formatting.
- **`StatRow` → `ui/StatRow.kt`:** Label (`TextDim`, body 14 sp) + value (`TextHi`, display 14 sp). Used on Game Over.
- **`SettingToggle` → `settings/SettingToggle.kt`:** Square checkbox style, 24 dp, `Accent` when on. Never use Material `Switch`.
- **`Overlay prompt` → `ui/TipChip.kt`:** `Panel` fill, 1 dp `Accent` border, body 14 sp. Max 1 visible at a time. Auto-dismiss on the matching action.
- **`GameCanvas` → `game/GameScreen.kt`:** One `Canvas`. Draw order: Ink → Panel → Grid → well cells → chunk → arena snake → apple → particles → HUD → danger line.
- **Theme object:** Expose tokens through `object SnakeTheme { val colors; val type; val dimens }`. Never use `MaterialTheme.colorScheme` for game visuals. If Material components are unavoidable (dialogs), override all colors/shapes from tokens.
- **Accessibility:** Provide content descriptions on icon buttons. Honor system font scale up to 1.3× on body text only (display text fixed). Provide `ReduceMotion` toggle that disables shake and pulse.
- **Adding a component:** Add a `@Preview` on dark background, use only tokens above, update this file in the same commit.
