# Proposal: 8x8 character set files instead of the TTF glyphs, selectable in the memory inspector

**Status: done 2026-10-01, as designed below. Verified with the full test
suite (new `CharacterSetTest`, pixel-exact glyph checks in `RenderingTest`)
and a live run with an Atari 800 file and a C64 workspace. Decided
2026-10-01: character sets are stored apart
from the computer systems; the value set always offers every character
set; a computer system only names its default (ATASCII Standard for Oric
and Unknown); the selection is kept like the screen-code toggle; the
field has no label; the whole grid renders from the character set and
the TTFs are removed (decision 1a).** Two details settled during
implementation: `ComputerFont` no longer implements `TextFont` (it has no
AWT font behind it; the disassembly grid's placeholder font is now a
`PlainTextFont`), and the selection is `Workspace.getViewCharacterSet()`
with a new `WorkspaceProperty.CHARACTER_SET` event, so the field and the
fonts follow a system change. Typing into the text column in edit mode
still writes the typed ASCII/ATASCII byte, as before.

## Request

The four files in `src/fonts/` replace today's glyph definition, which is
derived from the two TTF fonts (Atari Classic, C64 Classic). A value set
field right of the "Display as Screen Code" toggle in the memory
inspector's header selects the character set.

## What the files are

All four are 2048 bytes: 256 characters of 8 bytes, one byte per pixel
row, bit 7 the leftmost pixel, ordered by screen code (the code the video
hardware uses, not the ATASCII/PETSCII byte value):

| File | System | Order | Characters 128-255 |
|---|---|---|---|
| `ATASCII-standard.chr` | Atari 800/5200 | ANTIC internal code (`0x21` = 'A') | exact inverse of 0-127 |
| `ATASCII-international.chr` | Atari 800/5200 | ANTIC internal code | exact inverse of 0-127 |
| `PETSCII-uppercase.chr` | C64 | screen code (`0x01` = 'A') | reversed set, not a bit-exact inverse |
| `PETSCII-lowercase.chr` | C64 | screen code (`0x01` = 'a') | reversed set |

## How glyphs are drawn today

- `HexGridPanel` draws every byte of the ASCII/ATASCII column with
  `ComputerFont.drawGlyph(value)`, and the address, hex digits and the
  `|` separator with `ComputerFont.drawText`.
- `ComputerFont` rasterizes a TTF character into a cached, 1:1 blitted
  bitmap. The byte value is shifted into the TTF's private range
  (`0xE000 + value` for Atari, `0x100 + value` for C64).
- "Display as Screen Code" converts the byte with
  `HexGridPanel.toInternalCode` before drawing. That table is the Atari
  internal-to-ATASCII conversion and is applied for every system, so the
  toggle is wrong for C64 today (the C64 TTF range is already screen-code
  ordered, and C64 bytes are PETSCII).

## Proposed design

### 1. `CharacterSet` value set (model)

A WUDSN Base `ValueSet` in `model` (not `model.system` - character sets
are stored apart from the computer systems), texts in
`ValueSets.properties` (per the rule that user-visible types are value
sets, not enums):

| Value | Text | File | Byte encoding |
|---|---|---|---|
| `ATASCII_STANDARD` | ATASCII Standard | `ATASCII-standard.chr` | ATASCII |
| `ATASCII_INTERNATIONAL` | ATASCII International | `ATASCII-international.chr` | ATASCII |
| `PETSCII_UPPERCASE` | PETSCII Uppercase | `PETSCII-uppercase.chr` | PETSCII |
| `PETSCII_LOWERCASE` | PETSCII Lowercase | `PETSCII-lowercase.chr` | PETSCII |

The names follow the byte encoding, not the computer (renamed from
"Atari"/"C64" on 2026-10-01, since the sets are independent of the computer
system and PETSCII is not C64-specific). The files themselves are still in
screen-code order, not in ATASCII/PETSCII order.

Each value knows its file and its byte-to-screen-code mapping (ATASCII to
ANTIC internal, PETSCII to C64 screen code). The mapping belongs to the
character set, not to the computer system, since every set can be shown
for every system. The 2048 bytes are loaded lazily from the classpath.

The `.chr` files stay in `src/fonts/` where they are now, which puts them
at `/fonts/` on the classpath, independent of any system package. A new
character set is one new file plus one new value.

A computer system only names its default:
`ComputerSystem.getDefaultCharacterSet()` returns `ATASCII_STANDARD`, and
`C64` overrides it with `PETSCII_UPPERCASE`. So Atari 800, Atari 5200, Oric
and Unknown default to ATASCII Standard.

### 2. `ComputerFont` renders from the character set

`ComputerFont.get(CharacterSet, pixelHeight)` replaces
`get(ComputerSystemType, pixelHeight)`. A glyph bitmap is built directly
from the 8 bytes: each set bit becomes a `zoom` x `zoom` block
(`zoom = pixelHeight / 8`), in the requested color. This is pixel
replication of the real 8x8 data - crisp at every size by construction,
with no rasterizer, antialiasing or hinting involved. The per-character
bitmap cache and the 1:1 unscaled `drawImage` stay, so the HiDPI fix is
kept.

- `drawGlyph(value, screenCode)` maps the byte through the set's encoding
  unless `screenCode` is set, then draws that glyph. This replaces
  `HexGridPanel.toInternalCode` and fixes the C64 screen-code toggle.
- `drawText(text)` (address, hex digits, `|`) maps each ASCII character
  through the same encoding, so the whole grid uses one font. Only `0-9`,
  `A-F`, `|` and space are ever needed there; all four sets contain them.

The two TTF files, `loadFont`, `codePointBase` and the TTF-specific
javadoc are deleted.

### 3. Selection in the memory inspector header

`MemoryInspectorPanel` gets `public final ValueSetField<CharacterSet>
characterSetField` with all values, no label, added to the header's button
panel right of `displayAsScreenCodeButton`. `Dis6502` wires it like the
toggle button:

- The workspace holds the current character set
  (`Workspace.get/setViewCharacterSet`), next to `viewDisplayAsScreenCode`
  and kept the same way: workspace state, not saved in the `.wrk` file.
- On a computer system change (which includes a new or opened workspace),
  the workspace's character set becomes the system's
  `getDefaultCharacterSet()`, the field shows it, and `updateFonts()` runs.
- On a selection change, `Dis6502` sets the workspace value and calls
  `updateFonts()`, which builds the native font from
  `workspace.getViewCharacterSet()` and `getNativeFontSize()`.
- `DiskImageSectorsDialog` and the Options dialog's memory inspector
  preview use the workspace's current set too.

### 4. Tests

- New headless `CharacterSetTest`: each file is 2048 bytes; known glyph rows
  (Atari internal `0x21` = `00 18 3C 66 66 7E 66 00`, C64 screen code
  `0x01` in the uppercase set); mapping spot checks (ATASCII `0x41` to
  internal `0x21`, PETSCII `0x41` to screen code `0x01`); every system's
  `getDefaultCharacterSet()`.
- `RenderingTest`: assert exact pixels of one rendered glyph at 8px and
  16px (the 16px one must be the 8px one with every pixel doubled).
- `PanelTextsTest`/`DialogTextsTest` cover the new field's texts.

## Open decisions

1. ~~Scope of the switch~~ - decided (a): the whole grid - glyph column,
   address and hex digits - renders from the selected character set and
   the TTFs are removed.
2. ~~Oric and Unknown~~ - decided: all sets offered, ATASCII Standard default.
3. ~~Persistence~~ - decided: like the screen-code toggle.
4. ~~Label~~ - decided: no label for a start.

## Verification plan

Full `TestRunner` suite headless and with a display, plus a live run:
switch sets for an Atari and a C64 file, toggle screen code with each,
check sizes 8/16/24 in the Options dialog, and compare the grid against
the bit patterns of the `.chr` files.
