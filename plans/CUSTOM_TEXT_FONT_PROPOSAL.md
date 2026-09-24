# Proposal: a user-chosen mono-spaced font for everything except the memory inspector's byte preview

## Request

Every place that today renders through `ComputerFont` should be able to use
a different, user-chosen mono-spaced font instead of the computer's real
character set - except the memory inspector's 8/16-character ASCII/ATASCII
byte preview column, which must always show the computer's authentic native
glyphs. The font choice is a user preference, picked later through some UI
not designed here, and persisted.

## Analysis: every current `ComputerFont` call site

`ComputerFont` (`ui/ComputerFont.java`) does two genuinely different jobs
through two different methods, and every consumer only ever calls one of
them:

- **`drawGlyph(g2, value, color, x, y)`** - draws one raw memory byte's
  *authentic hardware glyph* (shifted by `codePointBase` into the font's
  byte-indexed range). This is the "must always be native" case.
- **`drawText(g2, text, color, x, y)`** / **`getAwtFont()`** - draws this
  port's own already-generated text (hex digits, addresses, mnemonics,
  labels, comments, list entries, log lines) at each character's own direct
  Unicode code point. Nothing about this rendering path is
  computer-specific; it only uses the retro TTF today because that's the
  only font `ComputerFont` has ever offered.

Grepping every `setComputerFont`/`drawGlyph`/`drawText`/`getAwtFont` call
site gives three groups:

**Group A - always native, never swappable (the stated exception).**
Exactly one: `HexGridPanel`'s two `drawGlyph` calls
(`paintLine`/`paintCursorAsciiCell`), painting the memory inspector's (and
`DiskImageSectorsDialog`'s embedded sector-browsing grid's) ASCII/ATASCII
column - one authentic glyph per raw byte.

**Group B - custom-painted "text" grids, each owns its own pixel geometry.**
Rebuild their own row height/column width from whatever `ComputerFont`
they're given, with no dependency on Group A's font:

- `DisassemblyGridPanel` - the whole disassembly listing. No `drawGlyph`
  call anywhere in it; free to size itself entirely around a different
  font.
- `PartHeaderPanel` - every part window's colored title bar text.
  Standalone, no grid to stay aligned with.
- `ComputerFontListCellRenderer` - `JList` cell painting for
  `SegmentListPanel`/`XRefPanel`, already sizes each cell from
  `getFontMetrics(getFont())` independently per cell.

**Group C - plain Swing components via `getAwtFont()`.** Just want an
ordinary `Font` object for standard (antialiased) Swing text rendering, no
custom bitmap-glyph cache involved at all:

- `LogPanel`'s `JTextPane`
- `SegmentListPanel`'s `JList`
- `XRefPanel`'s `JList`

**The one hard case: `HexGridPanel`'s hex/address text is Group B, but it
shares one pixel grid with Group A.** `paintLine` draws the address, every
hex byte pair, and the `|` separator via `drawText` in the *same* row, at
the *same* `cellW`/`cellH` unit, as the ASCII glyph column's `drawGlyph`
calls right next to them - and that shared `cellW` also drives
`getPreferredSize`, scroll-to-visible, and both mouse-hit-testing methods
(`x / cellW`, `y / cellH`). Group A's font can never change; if Group B's
font in this one panel could suddenly have a different natural glyph
width, the grid stops being uniform and all of that pixel math breaks.

## Why forcing the chosen font to fit the native cell size is the wrong fix

The obvious-looking fix - derive the chosen text font, then squeeze/stretch
it with a non-uniform `AffineTransform` (the same trick `ComputerFont`
already uses for double-height) until its glyph box matches the native
font's `cellW`/`cellH` exactly - keeps every existing pixel-math line in
`HexGridPanel` untouched, but it defeats the point of the feature: it
visually distorts the very font the user picked *because* they wanted it to
look different. Double-height's Y-only stretch is acceptable because it is
a deliberate CRT-style effect; force-fitting an arbitrary system font's
natural proportions is not the same thing.

## Proposed design

### 1. Generalize `ComputerFont` to derive from *any* named font, not just the two retro TTFs

`ComputerFont.create(ComputerSystemType, boolean)` already has exactly this
fallback for Oric/Unknown:

```java
return derive(new Font(Font.MONOSPACED, Font.PLAIN, 1), doubleHeight, -1); // Oric, unknown.
```

`derive()` already takes an arbitrary base `Font`, an already-generic
mechanism just never exposed. Add one new public factory next to
`get(ComputerSystemType, boolean)`, using the identical `codePointBase < 0`
(no byte-shift) path, cached the same way (a second pair of maps keyed by
font family name instead of `ComputerSystemType`):

```java
private static final Map<String, ComputerFont> CUSTOM_NORMAL_INSTANCES = new HashMap<>();
private static final Map<String, ComputerFont> CUSTOM_DOUBLE_HEIGHT_INSTANCES = new HashMap<>();

/** Derives a ComputerFont from any installed font family, for a user-chosen
  * "text" font - same no-byte-shift mechanism as the Oric/Unknown fallback,
  * just exposed and keyed by name instead of computer system. */
public static synchronized ComputerFont getCustom(String fontFamilyName, boolean doubleHeight) {
    Map<String, ComputerFont> instances = doubleHeight ? CUSTOM_DOUBLE_HEIGHT_INSTANCES : CUSTOM_NORMAL_INSTANCES;
    ComputerFont existing = instances.get(fontFamilyName);
    if (existing != null) {
        return existing;
    }
    ComputerFont created = derive(new Font(fontFamilyName, Font.PLAIN, 1), doubleHeight, -1);
    instances.put(fontFamilyName, created);
    return created;
}
```

Because `derive()` always sets the font's point size to the same fixed
`NATIVE_HEIGHT * ZOOM` (or its double-height transform) regardless of which
`Font` comes in, a custom instance's `getGlyphHeight()` always matches a
native instance's - only `getGlyphWidth()` legitimately differs per font
family. That is the whole reason only width, never height, needs
reconciling below.

### 2. Thread two `ComputerFont` references through the app instead of one

`Dis6502.updateFonts()` becomes the single place that decides what the
"text font" actually is - `nativeFont` unchanged, `textFont` either the
same instance (default, zero visual change) or a `getCustom(...)` instance
once a preference is set:

```java
private void updateFonts() {
    ComputerFont nativeFont = ComputerFont.get(workspace.getComputerSystem().getType(), workspace.isViewDoubleHeight());
    ComputerFont textFont = getTextFont(workspace.isViewDoubleHeight()); // nativeFont if no preference is set

    mainWindow.memoryInspectorPanel.setComputerFont(nativeFont);
    mainWindow.memoryInspectorPanel.setTextFont(textFont);       // new
    mainWindow.disassemblyPanel.setComputerFont(textFont);       // was nativeFont
    mainWindow.xrefPanel.setComputerFont(textFont);              // was nativeFont
    mainWindow.logPanel.setComputerFont(textFont);               // was nativeFont
    mainWindow.segmentListPanel.setComputerFont(textFont);       // was nativeFont
}
```

Every Group B/C panel keeps its existing single-argument `setComputerFont`
method completely unchanged - only *which* font `updateFonts()` passes it
changes (from `nativeFont` to `textFont`), since none of them ever call
`drawGlyph`. `DisassemblyPanel`/`MemoryInspectorPanel` (which each wrap a
grid plus a `PartHeaderPanel`) forward accordingly - `DisassemblyPanel`
passes the one font it receives to both children unchanged (its
`DisassemblyGridPanel` has no Group A dependency at all); only
`MemoryInspectorPanel` needs both:

```java
public void setComputerFont(ComputerFont computerFont) { // native, for the grid's ASCII column
    grid.setComputerFont(computerFont);
}
public void setTextFont(ComputerFont textFont) { // new
    grid.setTextFont(textFont);
    header.setComputerFont(textFont);
}
```

`DiskImageSectorsDialog` (its own embedded `HexGridPanel`, set once when
the dialog opens, not part of the live `updateFonts()` cascade) gets the
same two-call treatment at its one call site in
`Dis6502.openDiskImageSectors`.

### 3. `HexGridPanel`: two independent column widths, one shared row height

Row height stays exactly as today (`cellH = computerFont.getGlyphHeight()`,
Group A's font) - per the point above, it already matches whatever text
font is in use, by construction. Column width splits in two:

```java
private ComputerFont computerFont; // native - drives cellH and the ASCII column (unchanged name/field, existing call sites keep working)
private ComputerFont textFont;     // new - drives the address/hex-pair columns; defaults to computerFont until setTextFont is called

public void setTextFont(ComputerFont textFont) {
    this.textFont = textFont;
    revalidate();
    repaint();
}

private ComputerFont textFont() {
    return textFont != null ? textFont : computerFont; // never null in practice once wired, but keeps construction-order safe
}
```

Every pixel-math line that currently multiplies by one `cellW` splits into
a `textCellW` (address prefix, each hex byte pair, the `|` separator) and
an `asciiCellW` (the ASCII glyph column only) - contained entirely within
`HexGridPanel.java`:

- `paintLine`: `hexX = (5 + row * 3) * textCellW`; the ASCII column starts
  at a fixed pixel offset (`5 * textCellW + bytesPerLine * 3 * textCellW`)
  plus `row * asciiCellW`, instead of everything sharing one `cellW`.
- `getPreferredSize`/`neededFor16BytesPerLine`: total width is the hex
  portion's `textCellW`-based width plus the ASCII portion's
  `asciiCellW`-based width, not one uniform multiply.
- The two hit-testing methods (`getEditCursorOffset`-style pixel-to-cell
  lookups) split the same way: which pane the `x` coordinate falls in first
  (compare against the hex/ASCII boundary pixel offset), then divide by
  that pane's own cell width.
- `drawText(g2, "|", ...)` (the separator) and the cursor-nibble painting
  (`paintCursorHexCell`/`paintCursorSubCell`) use `textFont()` instead of
  `computerFont`, matching whichever pane they paint into;
  `paintCursorAsciiCell`'s `drawGlyph` call stays on `computerFont`
  unconditionally.

This is a contained, single-file change - real, but bounded, and it never
distorts either font.

### 4. Persistence

A new small, app-wide (not per-workspace, not per-computer-system)
preference, following the exact pattern `ProfileLogic`'s `"LastProfile"`
already uses:

```java
ApplicationSettingsSection settings = application.getSettingsSection("Display");
String textFontFamily = settings.getString("TextFontFamily", ""); // "" = use the native font, today's behavior
```

Read once at startup (or lazily in `updateFonts()`/a small
`Dis6502.getTextFont(boolean doubleHeight)` helper), written whenever the
user changes it through whatever picker UI eventually calls
`settings.writeString("TextFontFamily", chosenFamilyName)` -
`ApplicationSettingsSection` already persists through
`java.util.prefs.Preferences`, the same cross-platform mechanism MRU
lists/default folders/last-profile already use, so nothing new is needed
on the storage side.

## Explicitly out of scope for this proposal

- **The actual picker UI.** Per the request, the font choice is the user's
  to make "later" - this proposal only makes the choice possible and
  makes sure it is remembered. A future small dialog (or a combo box
  in a future general Preferences dialog, following `DefaultFoldersDialog`'s
  existing shape) would list installed mono-spaced fonts (the standard
  Java idiom: `GraphicsEnvironment.getAvailableFontFamilyNames()` filtered
  to families where `metrics.charWidth('i') == metrics.charWidth('W')`)
  and call `settings.writeString(...)` plus re-run `updateFonts()`.
- **Any change to `drawGlyph`, `codePointBase`, or the two retro TTFs
  themselves** - Group A is completely untouched.
- **`GraphicPanel`** - shares `ComputerFont`'s glyph-caching *technique*
  conceptually (per that class's own javadoc reference) but does not
  actually consume `ComputerFont` - nothing to change there.

## Verification plan

1. With no preference set (`textFontFamily=""`), `textFont` is always
   exactly the `nativeFont` instance - confirm every Group B/C panel's
   rendering is pixel-identical to today's (a hand-computed-value
   regression check, not a new visible feature yet).
2. Manually set the new preference to a real installed mono-spaced family
   (e.g. `"Consolas"` on Windows) and confirm, via a real (non-headless)
   `TestRunner` run and a live smoke test: the disassembly listing, log
   panel, segment list, and XRef panel all render in the chosen font, the
   memory inspector's hex/address text renders in the chosen font, and its
   ASCII/ATASCII preview column still renders the computer's authentic
   glyphs unchanged, correctly aligned with the hex column.
3. Confirm double-height mode still keeps both fonts' row heights equal
   with a custom text font selected.
4. Add a hand-computed-value unit test for `ComputerFont.getCustom` (glyph
   height matches `ComputerFont.get(...)`'s for the same `doubleHeight`,
   instances are cached/reused per family name) alongside existing
   `ComputerFont`-adjacent coverage.
