# Proposal: a user-chosen mono-spaced font for everything except the memory inspector's grid

**Status: done.** Implemented as designed below - `TextFont`/`PlainTextFont`
added, every Group B panel switched over, `HexGridPanel`/
`MemoryInspectorPanel`/`DiskImageSectorsDialog` untouched, persistence
wired into `Dis6502.updateFonts()`/`getTextFont()`. Verified with the full
test suite (headless and real-display) plus a live smoke test with a real
installed font. The picker UI itself remains a follow-up, as scoped below.

## Request

Every place that today renders through `ComputerFont` should be able to use
a different, user-chosen mono-spaced font instead of the computer's real
character set - except the memory inspector's grid (its hex/address text
*and* its 8/16-character ASCII/ATASCII byte preview column), which always
stays exactly as it is today, in the computer's authentic native font. The
font choice is a user preference, picked later through some UI not designed
here, and persisted.

## Analysis: every current `ComputerFont` call site

`ComputerFont` (`ui/ComputerFont.java`) does two genuinely different jobs
through two different methods, and every consumer only ever calls one of
them:

- **`drawGlyph(g2, value, color, x, y)`** - draws one raw memory byte's
  *authentic hardware glyph* (shifted by `codePointBase` into the font's
  byte-indexed range).
- **`drawText(g2, text, color, x, y)`** / **`getAwtFont()`** - draws this
  port's own already-generated text (hex digits, addresses, mnemonics,
  labels, comments, list entries, log lines) at each character's own direct
  Unicode code point. Nothing about this rendering path is
  computer-specific; it only uses the retro TTF today because that's the
  only font `ComputerFont` has ever offered.

Grepping every `setComputerFont`/`drawGlyph`/`drawText`/`getAwtFont` call
site, and folding in the two scope decisions made while discussing this
proposal (the memory inspector's grid stays native end to end, not just its
byte-preview column; and the other consumers should not be forced through
`ComputerFont`'s own rendering machinery at all - see below), gives two
groups:

**Group A - always native, untouched by this proposal.**
`HexGridPanel` (the memory inspector's grid, and `DiskImageSectorsDialog`'s
embedded sector-browsing grid) - every `drawText`/`drawGlyph` call in it,
not just the ASCII column's `drawGlyph` calls. `MemoryInspectorPanel`'s and
`DiskImageSectorsDialog`'s existing `setComputerFont(ComputerFont)` methods
need no change at all: they always receive the workspace's native
`ComputerFont`, exactly as today. This sidesteps the one genuinely hard
problem the first pass at this proposal ran into - `HexGridPanel`'s
hex/address text sharing one pixel grid (`cellW`/`cellH`, also driving
`getPreferredSize` and both mouse-hit-testing methods) with the
always-native ASCII column - by simply not putting `HexGridPanel` in scope.

**Group B - "generated text" consumers, candidates for the user's chosen font:**

- `DisassemblyGridPanel` - the whole disassembly listing.
- `PartHeaderPanel` - every part window's colored title bar text.
- `ComputerFontListCellRenderer` - `JList` cell painting for
  `SegmentListPanel`/`XRefPanel`.
- `LogPanel`'s `JTextPane`, `SegmentListPanel`'s `JList`, `XRefPanel`'s
  `JList` - via `getAwtFont()`, already just plain Swing components.

## Why Group B should not route through `ComputerFont` for a custom font

`ComputerFont`'s `drawText` exists to solve one specific problem: the tiny
pixel-art retro TTFs (Atari Classic/C64 Classic, native 8px cell height)
need crisp, hard edges at any display scale, so it disables antialiasing
and rasterizes each glyph into a cached bitmap blitted with nearest-
neighbor scaling (a real HiDPI bug fix, per that class's own javadoc).
`ComputerFontListCellRenderer` exists for the matching reason - its own
javadoc notes that plain `list.setFont(...)` blurs that pixel-art font into
"illegible dots" under ClearType/subpixel antialiasing.

None of that helps an ordinary system monospace font the user picks for
readability - forcing antialiasing *off* and nearest-neighbor-scaling a
bitmap is the wrong way to render a normal font; it would look worse
(jagged) than plain antialiased `Graphics2D`/Swing rendering, which is
presumably the whole point of offering a font choice. So Group B should get
a genuinely separate, much simpler rendering path for a custom font, not a
`ComputerFont` instance wrapping an arbitrary font family.

## Proposed design

### 1. A small `TextFont` interface; `ComputerFont` implements it unchanged

```java
public interface TextFont {
    void drawText(Graphics2D g2, String text, Color color, int x, int y);
    int getGlyphWidth();
    int getGlyphHeight();
    Font getAwtFont();
}
```

`ComputerFont` already has exactly these four methods, with matching
signatures - `public final class ComputerFont implements TextFont` is the
only change needed to it. `drawGlyph` stays `ComputerFont`-only, outside
the interface - Group B never calls it, and a plain custom font has no
byte-indexed glyph range to shift into anyway.

### 2. `PlainTextFont` - a much smaller sibling for a custom font

```java
public final class PlainTextFont implements TextFont {
    private final Font font;
    private final int glyphWidth;
    private final int glyphHeight;

    private PlainTextFont(Font font, int glyphWidth, int glyphHeight) {
        this.font = font;
        this.glyphWidth = glyphWidth;
        this.glyphHeight = glyphHeight;
    }

    /** {@code pointSize} lets the caller match the app's current text size - see the sizing note below. */
    public static PlainTextFont get(String fontFamilyName, int pointSize) {
        Font font = new Font(fontFamilyName, Font.PLAIN, pointSize);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = probe.createGraphics();
        try {
            FontMetrics metrics = g2.getFontMetrics(font);
            return new PlainTextFont(font, metrics.charWidth('M'), metrics.getHeight());
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void drawText(Graphics2D g2, String text, Color color, int x, int y) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(font);
        g2.setColor(color);
        g2.drawString(text, x, y + g2.getFontMetrics(font).getAscent());
    }

    @Override
    public int getGlyphWidth() { return glyphWidth; }
    @Override
    public int getGlyphHeight() { return glyphHeight; }
    @Override
    public Font getAwtFont() { return font; }
}
```

No bitmap cache, no forced-off antialiasing, no per-character rendering
loop: since the feature only ever offers genuinely mono-spaced fonts (a
constraint on what the picker lists, not enforced here), a single
`drawString` call already lands every character at its font's own uniform
advance width - the same guarantee `ComputerFont`'s per-character bitmap
placement exists to provide for a font that (for the retro TTFs' authentic
byte-indexed glyphs) cannot be assumed to behave that way.

**Sizing**: `PlainTextFont.get` takes an explicit point size rather than
deriving one the way `ComputerFont.derive` pins to the pixel-art font's
tiny native height - a user-chosen readable font wants an ordinary text
point size, not the retro font's native 8px-derived one. One shared
`PlainTextFont` instance (one fixed point size) is reused across every
Group B consumer, so sizing stays consistent app-wide; the exact default
point size is an implementation detail for whenever this is built, not
fixed here. Double-height mode, if it should also stretch a custom font,
needs the same Y-only `AffineTransform` trick `ComputerFont.derive` already
uses for its own double-height case - a direct reuse of an existing
technique, not a new problem.

### 3. Group B fields widen from `ComputerFont` to `TextFont`; call sites barely change

`DisassemblyGridPanel`, `PartHeaderPanel`, and `ComputerFontListCellRenderer`
each already hold exactly one `ComputerFont` field and call `drawText`/
`getGlyphWidth`/`getGlyphHeight` on it - widening that field's declared
type to `TextFont` (renaming the setter to `setTextFont(TextFont)` for
clarity, since it's no longer necessarily a `ComputerFont`) needs no other
change in any of them: each `TextFont` implementation already renders
itself correctly, so there is no caller-side branching to add anywhere,
including in `ComputerFontListCellRenderer` - it keeps working for both the
native and the custom-font case without knowing which one it has.
`LogPanel`/`SegmentListPanel`/`XRefPanel` keep calling `.getAwtFont()`
through the same widened field, feeding a `JTextPane`/`JList` exactly as
today.

`Dis6502.updateFonts()` becomes the one place that decides what the "text
font" is:

```java
private void updateFonts() {
    ComputerFont nativeFont = ComputerFont.get(workspace.getComputerSystem().getType(), workspace.isViewDoubleHeight());
    TextFont textFont = getTextFont(); // nativeFont itself if no preference is set, else a shared PlainTextFont instance

    mainWindow.memoryInspectorPanel.setComputerFont(nativeFont); // unchanged
    mainWindow.disassemblyPanel.setTextFont(textFont);           // was setComputerFont(nativeFont)
    mainWindow.xrefPanel.setTextFont(textFont);                  // was setComputerFont(nativeFont)
    mainWindow.logPanel.setTextFont(textFont);                   // was setComputerFont(nativeFont)
    mainWindow.segmentListPanel.setTextFont(textFont);           // was setComputerFont(nativeFont)
}
```

`DiskImageSectorsDialog` and `HexGridPanel` are Group A - neither needs any
change.

### 4. Persistence

A new small, app-wide (not per-workspace, not per-computer-system)
preference, following the exact pattern `ProfileLogic`'s `"LastProfile"`
already uses:

```java
ApplicationSettingsSection settings = application.getSettingsSection("Display");
String textFontFamily = settings.getString("TextFontFamily", ""); // "" = use the native font, today's behavior
```

Read once at startup (or lazily in a small `Dis6502.getTextFont()` helper),
written whenever the user changes it through whatever picker UI eventually
calls `settings.writeString("TextFontFamily", chosenFamilyName)`.
`ApplicationSettingsSection` already persists through
`java.util.prefs.Preferences`, the same cross-platform mechanism MRU
lists/default folders/last-profile already use, so nothing new is needed
on the storage side.

## Explicitly out of scope for this proposal

- **The actual picker UI.** Per the request, the font choice is the user's
  to make "later" - this proposal only makes the choice possible and makes
  sure it is remembered. A future small dialog (or a combo box in a future
  general Preferences dialog, following `DefaultFoldersDialog`'s existing
  shape) would list installed mono-spaced fonts (the standard Java idiom:
  `GraphicsEnvironment.getAvailableFontFamilyNames()` filtered to families
  where `metrics.charWidth('i') == metrics.charWidth('W')`) and call
  `settings.writeString(...)` plus re-run `updateFonts()`.
- **`HexGridPanel`, `DiskImageSectorsDialog`, `MemoryInspectorPanel`** -
  Group A, completely untouched; always the workspace's native
  `ComputerFont`, for both the hex/address text and the ASCII/ATASCII
  preview column, exactly as today.
- **Any change to `drawGlyph`, `codePointBase`, or the two retro TTFs
  themselves.**
- **`GraphicPanel`** - shares `ComputerFont`'s glyph-caching *technique*
  conceptually (per that class's own javadoc reference) but does not
  actually consume `ComputerFont` - nothing to change there.

## Verification plan

1. With no preference set (`textFontFamily=""`), `textFont` is always
   exactly the `nativeFont` instance - confirm every Group B panel's
   rendering is pixel-identical to today's (a hand-computed-value
   regression check, not a new visible feature yet).
2. Manually set the new preference to a real installed mono-spaced family
   (e.g. `"Consolas"` on Windows) and confirm, via a real (non-headless)
   `TestRunner` run and a live smoke test: the disassembly listing, log
   panel, segment list, and XRef panel all render in the chosen font,
   antialiased and legible, while the memory inspector's grid (hex,
   address, and ASCII/ATASCII preview alike) is completely unaffected.
3. Add a hand-computed-value unit test for `PlainTextFont.get` (glyph
   width/height come out consistent with a directly-queried
   `FontMetrics` for the same family/point size, repeated calls are safe)
   alongside existing `ComputerFont`-adjacent coverage.
