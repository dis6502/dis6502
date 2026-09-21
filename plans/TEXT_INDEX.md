# Text Index

Generated: 2026-09-21 03:56 (local time), commit `382ba4088144feca9cfe1a24f5e4894dd4953497`
Last updated: 2026-09-21 13:52 (local time), commit `1e4397c5f3cf31dc32c20729d01ed2c87fe12a16`

## Purpose

An inventory of every place in the Java source
(`src/com/wudsn/tools/dis6502/**/*.java`) that still hands a raw string
literal directly to a user-visible-text sink, instead of going through one
of the three repository classes documented in `PORTING_GUIDE.md`/`MEMORY.md`
(`Text.java` for faithfully-ported C++ `STRINGTABLE` text, `Texts.java` for
Java-invented UI text, `Messages.java` for severity-typed log/status/error
messages). This complements `build/find-text-matches.py`, which only finds
literals that happen to duplicate a real C++ resource string; this index is
broader - it lists every inline literal at a known text sink, matched or not,
ported or not.

**Methodology**: a regex scan (not a full Java parser) over every `.java`
file for a literal string argument to one of these sinks, including
multi-argument calls and `+`-concatenated fragments (so `"Could not open
workspace '" + file.getPath() + "'."` shows up as its two literal
fragments):

- `JOptionPane.show*Dialog(...)` - message and title arguments
- `setTitle(...)` / `setDialogTitle(...)`
- `setToolTipText(...)`
- `throw new IOException(...)` / `IllegalStateException(...)` /
  `IllegalArgumentException(...)` / `RuntimeException(...)`

Not exhaustive: it can't see arguments that are themselves the result of
another method call building the string elsewhere, and it doesn't cover
every possible text sink (e.g. `JLabel`/`JButton` literal constructors -
those are already covered by the `ElementFactory`/`DataTypes` convention
this codebase uses instead, so none turned up here).

**Status**: this list only shows what's still outstanding - entries that
have been migrated to `Text.java`/`Texts.java`/`Messages.java` are removed
once done rather than kept struck through (see git history for what moved
where and when: all 13 dialog `setTitle(...)` calls, every static
`JOptionPane` title, every static/parameterizable `JOptionPane` message,
and every static-literal `setDialogTitle(...)` call in group A have been
migrated as of the commit above). What remains below is either
out-of-scope by design (dynamic titles built from a variable, which can't
be a single static field; every `IllegalArgumentException`/
`IllegalStateException`/`RuntimeException` usage, explicitly flagged as a
programming-error guard rather than user- or log-facing text - see groups
C/D/E) or simply not yet done (group B's remaining `IOException`s were
never requested to move).

## A) User-facing dialog titles/messages still outstanding

Everything remaining here is a dynamically-built title (`"Add "`/`"Open "`
+ a variable, which can't become one static field - the only kind of
`setDialogTitle(...)` call still left, now that every static-literal one
has been migrated too) plus one deliberately-unported C++-parity literal.
The two `IllegalArgumentException`s this section used to list moved to
group C below.

### Dis6502.java

- `:542` `setDialogTitle` - `'Add '`/`'Open '` (+ suffix built elsewhere) - dynamic, out of scope
- `:559` `JOptionPane.showMessageDialog` - title `'Add '`/`'Open '` + `fileTypeDisplayName` - dynamic, out of scope (message already migrated to `Messages.E040`)
- `:587` `setDialogTitle` - `'Add '`/`'Open '` + `'Raw File'` - dynamic, out of scope
- `:644` `setDialogTitle` - `'Add '`/`'Open '` + `'Disk Image Executable File'` - dynamic, out of scope
- `:723` `JOptionPane.showMessageDialog` - title `'Add '`/`'Open '` + `'Disk Image Executable File'` - dynamic, out of scope (message already migrated, shares `Messages.E040`)
- `:750` `setDialogTitle` - `'Add '`/`'Open '` + `'Disk Image Boot Sectors'` - dynamic, out of scope
- `:815` `setDialogTitle` - `'Add '`/`'Open '` + `'Disk Image Sectors'` - dynamic, out of scope

### ui/SegmentWriteBootDiskDialog.java

- `:163` `IOException` - `'No directory entries found in the disk image.'` (deliberately not ported to a `Text`/`Messages` field - matches C++'s own `WriteBootDisk`, which throws this exact literal with a `// TODO: Error message` comment, i.e. C++ hasn't given it a `STRINGTABLE` entry either)

## B) Internal exception/invariant messages (21 hits, none migrated)

These are internal `IOException`s for malformed input data - not shown to
the user through any dialog and with no C++ `STRINGTABLE` counterpart;
they exist only as developer-facing diagnostics if a file/stream turns out
malformed. Per `Text.java`'s own scope rule ("every field must trace back
to a real C++ resource ID") and `Texts.java`'s rule ("UI text this port
introduces"), none of these belong in either class as currently scoped;
listed here for completeness since the original request was for every
inline text-sink location, not just user-visible ones. No work has been
done on this group. (Every `IllegalArgumentException` that used to be
listed here moved to group C, every `IllegalStateException` to group D,
and every `RuntimeException` to group E - see those sections for why.)

- `model/Atari5200.java:73` `IOException` - `'Only 32k ROMs are supported.'`
- `model/Atari800.java:181` `IOException` - `'Unsupported file header '` + value + `'.'`
- `model/Atari800.java:295` `IOException` - `'Segment end address is lower than segment start address.'`
- `model/Atari800.java:301` `IOException` - `'Stream has '` + n + `' bytes left and is too short for segment of size '` + n + `'.'`
- `model/Atari800.java:425` `IOException` - `'Invalid stream header. Stream is not a CART stream.'`
- `model/Atari800.java:443` `IOException` - `'Unsupported cartridge size '` + n + `'.'`
- `model/Atari800.java:488` `IOException` - `'Invalid file header. Stream is not a FUJI stream.'`
- `model/Atari800.java:656` `IOException` - `"Length of SDX symbol '"` + name + `"' exceeds maximum length "` + n + `'.'`
- `model/Atari800.java:694` `IOException` - `'Computed remaining length of stream of '` + n + `' is smaller than requested amount of '` + n + `' to read.'`
- `model/Atari800.java:733` same as above (second call site)
- `model/C64.java:57` `IOException` - `'File size of '` + n + `' bytes exceeds the maximum file size of executable files on C64.'`
- `model/C64.java:75` `IOException` - `'Executable files on C64 can only have one segment.'`
- `model/DisassemblyResultWriter.java:46` `IOException` - `'Cannot write files if encoding is unknown.'`
- `model/DisassemblyResultWriter.java:136` `IOException` - `'Cannot write strings if encoding is binary.'`
- `model/DisassemblyResultWriter.java:145` `IOException` - `"Character '"` + c + `"' ("` + code + `') at position '` + pos + `" of string '"` + s + `"' is no ASCII character and cannot be written in ASCII encoding mode."`
- `model/DisassemblyResultWriter.java:160` same shape, ATASCII encoding mode
- `model/Oric.java:80` `IOException` - `'Unsupported file header '` + value + `'.'`
- `model/Oric.java:105` `IOException` - `'Segment end address is lower than segment start address.'`
- `model/ProfileLogic.java:49` `IOException` - `"File '"` + path + `"' is empty."`
- `model/Xml.java:134` `IOException` - `"Mismatched root element: expected '"` + a + `"' but found '"` + b + `"'."`
- `ui/ComputerFont.java:216` `IOException` - `'Font resource not found: '` + name

## C) IllegalArgumentException usages - not relevant for text externalization

Flagged as out of scope: every `throw new IllegalArgumentException(...)`
site is a parameter-validation guard (a programming-contract check on a
method's own arguments), not a message meant to be read by a user or
logged for diagnosis the way group B's `IOException` entries are. None of
these have a C++ resource to trace back to, none are ever shown in a
dialog, and none should move to `Text.java`, `Texts.java`, or
`Messages.java` - they stay hardcoded `IllegalArgumentException` messages
permanently, not "not yet migrated."

- `Dis6502.java:227` - `"Parameter 'args' must not be null."`
- `Dis6502.java:899` - `"Parameter 'fileType' has unsupported value "` + value + `'.'`
- `model/ComputerSystemFactory.java:71` - `'Unknown computer system type: '` + type + `'.'`
- `model/ComputerSystemFactory.java:88` - `'Invalid computer system type: '` + type + `'.'`
- `model/DisassemblyLineWriter.java:130` - `'Address is not on zero page.'`
- `model/DisassemblyOpcodeBuffer.java:51` - `'Invalid opcode size: '` + n + `'.'`
- `model/DisassemblyResultFile.java:40` - `'File number exceeds 3 digits.'`
- `model/DisassemblySection.java:37` - `'Invalid disassemblySectionType: '` + type + `'.'`
- `model/DisassemblySection.java:52` - `'Invalid index: '` + index + `'.'`
- `model/DisassemblySection.java:67` - `'Undefined disassemblySectionType: '` + type + `'.'`
- `model/InstructionSet.java:22` - `"Parameter 'instructions' must have exactly 256 entries. Actual length is "` + n + `'.'`
- `model/Memory.java:72` - `'Size '` + n + `' exceeds maximum size of '` + n + `'.'`
- `model/Memory.java:87` - `'Offset '` + n + `' exceeds maximum size of '` + n + `'.'`
- `model/Memory.java:101` - `'Address '` + n + `' exceeds maximum size of '` + n + `'.'`
- `model/Segment.java:174` - `'Invalid offset.'`
- `model/Workspace.java:164` - `'Invalid processor type: '` + type + `'.'`
- `ui/GraphicMode.java:82` - `'Invalid ANTIC mode: '` + mode + `'.'`

## D) IllegalStateException usages - not relevant for text externalization

Flagged as out of scope, same reasoning as group C: every `throw new
IllegalStateException(...)` site indicates a programming error (an
invariant the calling code itself violated - calling a method out of
order, in the wrong pass, on the wrong object state), not a message meant
for a user or an operator reading a log. None of these have a C++
resource to trace back to, none are ever shown in a dialog, and none
should move to `Text.java`, `Texts.java`, or `Messages.java` - they stay
hardcoded `IllegalStateException` messages permanently.

- `model/Disassembly.java:408` - `'addLine() must only be called in pass 4/5/6.'`
- `model/Disassembly.java:730`, `:914`, `:1288` - `'Operand mode is unknown.'` (three call sites)
- `model/Disassembly.java:1009` - `'Invalid access.'`
- `model/DisassemblyLine.java:53` - `'Line number not yet set.'`
- `model/DisassemblyResultWriter.java:76` - `'No output stream opened.'`
- `model/DisassemblyResultWriter.java:133` - `'Invalid encoding.'`
- `model/Equate.java:124` - `'Label specified.'`
- `model/Equate.java:130` - `'Invalid access.'`
- `model/Equate.java:133` - `'No label specified.'`
- `model/Equate.java:146`, `:168` - `'Invalid equate type.'`
- `model/Equate.java:435` - `'Unsupported equate type.'`
- `model/MemoryBlock.java:144` - `'Attribute "Data" of memory block is missing.'`
- `model/MemoryBlock.java:147` - `'Size of content array is different from size of memory block.'`
- `model/MemoryBlock.java:152` - `'Size of type array is different from size of memory block.'`
- `model/MemoryBlockIterator.java:52` - `'End of memory block reached.'`
- `model/MutableMemoryInspectorState.java:123` - `'No segment selected yet. Cannot set selection range.'`
- `model/Segment.java:215` - `'Merged segments must not exceed 64kb.'`
- `model/Segment.java:270` - `'End must not be before begin.'`
- `model/Segment.java:459` - `'Cannot allocate symbol in empty segment.'`

## E) RuntimeException usages - not relevant for text externalization

Flagged as out of scope, same reasoning as groups C/D: both sites are
`ElementUtilities`' own defensive checks on a mnemonic-label string
supplied by other code in this project (a missing `&` marker, or a
mnemonic character outside `'A'`-`'Z'`) - a programming error in a
caller, not user- or log-facing text. None have a C++ resource to trace
back to, none are ever shown in a dialog, and none should move to
`Text.java`, `Texts.java`, or `Messages.java`.

- `ui/ElementUtilities.java:70`, `:102` - `"No '&' contained in label text '"` + text + `"'."`
- `ui/ElementUtilities.java:75`, `:107` - `"Mnemonic character '"` + c + `"' contained in label text '"` + text + `"' is not between 'A' and 'Z'."`

## Summary

| Group | Current membership | Migrated | Still outstanding | Excluded (not relevant) |
|---|---|---|---|---|
| A) User-facing dialog titles/messages | 48 (of 50 originally found - 2 reclassified to group C) | 40 | 8 (7 dynamic titles built from a variable - can't be a single static field, 1 deliberately-unported literal matching an un-fixed C++ `// TODO`) | - |
| B) Internal exception/invariant messages (`IOException` only) | 21 (of 62 originally found - 15 reclassified to group C, 22 to group D, 4 to group E) | 0 | 21 | - |
| C) `IllegalArgumentException` usages | 17 (2 from group A, 15 from group B) | 0 | 0 | 17 (parameter-validation guards) |
| D) `IllegalStateException` usages | 22 (from group B) | 0 | 0 | 22 (programming errors - violated invariants) |
| E) `RuntimeException` usages | 4 (from group B) | 0 | 0 | 4 (programming errors - a caller's malformed input) |
| **Total** | **112** | **40** | **29** | **43** |
