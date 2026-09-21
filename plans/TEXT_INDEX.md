# Text Index

Generated: 2026-09-21 03:56 (local time)
Commit: `382ba4088144feca9cfe1a24f5e4894dd4953497`

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
this codebase uses instead, so none turned up here). 112 hits total, split
below into two groups by what they're worth doing about.

## A) User-facing dialog titles/messages (50 hits)

These are what a user actually sees - dialog titles, `JOptionPane` message
text, tooltips. Most are Java-invented text with no C++ resource (dialog
titles the port itself chose), so per `Text.java`/`Texts.java`'s split these
belong in `Texts.java` if kept as plain text, or in `Messages.java` if they
ever need a severity (error dialogs whose message is also worth logging).
A few (flagged inline) are ported `IDS_ERR_*`/`IDS_LOG_*`-style messages
built by hand via concatenation instead of a `Text`/`Messages` field with a
`{0}` placeholder - worth cross-checking against `build/find-text-matches.py`'s
section D output.

### Dis6502.java

- `:227` `IllegalArgumentException` - `"Parameter 'args' must not be null."`
- `:432` `JOptionPane.showMessageDialog` - `"Could not open workspace '"` + path + `"'. See the log for details."`, title `'Open Workspace'`
- `:456` `JOptionPane.showMessageDialog` - `"Could not open file '"` + path + `"'. See the log for details."`, title `'Open File'`
- `:510` `JOptionPane.showMessageDialog` - `"Could not open workspace '"` + path + `"'. See the log for details."`, title `'Open Workspace'`
- `:542` `setDialogTitle` - `'Add '`/`'Open '` (+ suffix built elsewhere)
- `:559` `JOptionPane.showMessageDialog` - `'Could not '` + `'add'`/`'open'` + `" file '"` + name + `"'. See the log for details."`, title `'Add '`/`'Open '`
- `:587` `setDialogTitle` - `'Add '`/`'Open '` + `'Raw File'`
- `:644` `setDialogTitle` - `'Add '`/`'Open '` + `'Disk Image Executable File'`
- `:704` `JOptionPane.showMessageDialog` - `'File in disk image is empty.'`, title `'Open Disk Image Executable File'`
- `:723` `JOptionPane.showMessageDialog` - `'Could not '` + `'add'`/`'open'` + `" file '"` + name + `"'. See the log for details."`, title `'Add '`/`'Open '` + `'Disk Image Executable File'`
- `:750` `setDialogTitle` - `'Add '`/`'Open '` + `'Disk Image Boot Sectors'`
- `:815` `setDialogTitle` - `'Add '`/`'Open '` + `'Disk Image Sectors'`
- `:899` `IllegalArgumentException` - `"Parameter 'fileType' has unsupported value "` + value + `'.'`
- `:911` `JOptionPane.showConfirmDialog` - title `'Clear Equates'`
- `:952` `setDialogTitle` - `'Open User Equates File'`
- `:969` `setDialogTitle` - `'Export User Equates File'`/`'Save User Equates File'`
- `:1007` `setDialogTitle` - `'Save Segment'`
- `:1027` `setDialogTitle` - `'Save All Segments'`
- `:1095` `setDialogTitle` - `'Save Selection (With Header)'`/`'Save Selection (No Header)'`
- `:1157`, `:1161`, `:1168` `JOptionPane.showMessageDialog` - title `'Set Type'` (message now `Messages.E025`/`E031`/`E032`, see the recent `IDS_ERR_*` migration - only the title literal remains here)
- `:1465` `setDialogTitle` - `'Save Workspace File As'`
- `:1496` `setDialogTitle` - `'Save Disassembly Files'`

### ui/AssembleDialog.java

- `:75` `setTitle` - `'Assemble'`

### ui/CommentDialog.java

- `:57` `setTitle` - `'Comment'`

### ui/DiskImageExecutableFileDialog.java

- `:69` `setTitle` - `'Open Disk Image Executable File'`

### ui/DiskImageSectorsDialog.java

- `:110` `setTitle` - `'Open Disk Image Sectors'`

### ui/EquateDialog.java

- `:216` `setTitle` - `'Edit Equates'`/`'Display Equates'`

### ui/EquateRangeDialog.java

- `:58` `setTitle` - `'Define Address Range'`
- `:132` `JOptionPane.showMessageDialog` - `'Invalid start address.'`
- `:134` `JOptionPane.showMessageDialog` - `'Invalid end address.'`
- `:136` `JOptionPane.showMessageDialog` - `'Start address greater than end address.'`
- `:138` `JOptionPane.showMessageDialog` - `'No base equate selected.'`
- `:142` `JOptionPane.showMessageDialog` - `'Equate address is inside range.'`

### ui/LowHighByteDialog.java

- `:55` `setTitle` - `'Low/High Byte'`

### ui/MemoryInspectorFindStringDialog.java

- `:71` `setTitle` - `'Find String in Dump Window'`

### ui/ProfileDialog.java

- `:140` `setTitle` - `'Profile'`
- `:512` `setDialogTitle` - `'Load Profile File'`
- `:527` `JOptionPane.showMessageDialog` - `"Could not load profile '"` + path + `"'. See the log for details."`, title `'Load Profile'`
- `:535` `setDialogTitle` - `'Save Profile File'`

### ui/RawFileDialog.java

- `:75` `setTitle` - `'Open Raw File'`
- `:220` `JOptionPane.showMessageDialog` - `'File is empty.'`, title `'Open Raw File'`

### ui/SegmentPropertiesDialog.java

- `:62` `setTitle` - `'Segment Properties'`
- `:131` `JOptionPane.showMessageDialog` - title `'Segment Properties'` (message is now `Messages.E037`)

### ui/SegmentWriteBootDiskDialog.java

- `:76` `setTitle` - `'Write Boot Disk'`
- `:141` `setDialogTitle` - `'Write Boot Disk'`
- `:150` `JOptionPane.showMessageDialog` - title `'Write Boot Disk'`
- `:163` `IOException` - `'No directory entries found in the disk image.'` (deliberately not ported to a `Text`/`Messages` field - matches C++'s own `WriteBootDisk`, which throws this exact literal with a `// TODO: Error message` comment, i.e. C++ hasn't given it a `STRINGTABLE` entry either)

### ui/SelectGraphicsDialog.java

- `:79` `setTitle` - `'Select Graphics'`

## B) Internal exception/invariant messages (62 hits)

These are defensive/programming-error messages (`IllegalStateException`,
`IllegalArgumentException`, internal `IOException`s for malformed input
data) that are not shown to the user through any dialog and have no C++
`STRINGTABLE` counterpart - they exist only as developer-facing diagnostics
if an invariant is ever violated. Per `Text.java`'s own scope rule ("every
field must trace back to a real C++ resource ID") and `Texts.java`'s rule
("UI text this port introduces"), none of these belong in either class as
currently scoped; listed here for completeness since the request was for
every inline text-sink location, not just user-visible ones.

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
- `model/ComputerSystemFactory.java:71` `IllegalArgumentException` - `'Unknown computer system type: '` + type + `'.'`
- `model/ComputerSystemFactory.java:88` `IllegalArgumentException` - `'Invalid computer system type: '` + type + `'.'`
- `model/Disassembly.java:408` `IllegalStateException` - `'addLine() must only be called in pass 4/5/6.'`
- `model/Disassembly.java:730`, `:914`, `:1288` `IllegalStateException` - `'Operand mode is unknown.'` (three call sites)
- `model/Disassembly.java:1009` `IllegalStateException` - `'Invalid access.'`
- `model/DisassemblyLine.java:53` `IllegalStateException` - `'Line number not yet set.'`
- `model/DisassemblyLineWriter.java:130` `IllegalArgumentException` - `'Address is not on zero page.'`
- `model/DisassemblyOpcodeBuffer.java:51` `IllegalArgumentException` - `'Invalid opcode size: '` + n + `'.'`
- `model/DisassemblyResultFile.java:40` `IllegalArgumentException` - `'File number exceeds 3 digits.'`
- `model/DisassemblyResultWriter.java:46` `IOException` - `'Cannot write files if encoding is unknown.'`
- `model/DisassemblyResultWriter.java:76` `IllegalStateException` - `'No output stream opened.'`
- `model/DisassemblyResultWriter.java:133` `IllegalStateException` - `'Invalid encoding.'`
- `model/DisassemblyResultWriter.java:136` `IOException` - `'Cannot write strings if encoding is binary.'`
- `model/DisassemblyResultWriter.java:145` `IOException` - `"Character '"` + c + `"' ("` + code + `') at position '` + pos + `" of string '"` + s + `"' is no ASCII character and cannot be written in ASCII encoding mode."`
- `model/DisassemblyResultWriter.java:160` same shape, ATASCII encoding mode
- `model/DisassemblySection.java:37` `IllegalArgumentException` - `'Invalid disassemblySectionType: '` + type + `'.'`
- `model/DisassemblySection.java:52` `IllegalArgumentException` - `'Invalid index: '` + index + `'.'`
- `model/DisassemblySection.java:67` `IllegalArgumentException` - `'Undefined disassemblySectionType: '` + type + `'.'`
- `model/Equate.java:124` `IllegalStateException` - `'Label specified.'`
- `model/Equate.java:130` `IllegalStateException` - `'Invalid access.'`
- `model/Equate.java:133` `IllegalStateException` - `'No label specified.'`
- `model/Equate.java:146`, `:168` `IllegalStateException` - `'Invalid equate type.'`
- `model/Equate.java:435` `IllegalStateException` - `'Unsupported equate type.'`
- `model/InstructionSet.java:22` `IllegalArgumentException` - `"Parameter 'instructions' must have exactly 256 entries. Actual length is "` + n + `'.'`
- `model/Memory.java:72` `IllegalArgumentException` - `'Size '` + n + `' exceeds maximum size of '` + n + `'.'`
- `model/Memory.java:87` `IllegalArgumentException` - `'Offset '` + n + `' exceeds maximum size of '` + n + `'.'`
- `model/Memory.java:101` `IllegalArgumentException` - `'Address '` + n + `' exceeds maximum size of '` + n + `'.'`
- `model/MemoryBlock.java:144` `IllegalStateException` - `'Attribute "Data" of memory block is missing.'`
- `model/MemoryBlock.java:147` `IllegalStateException` - `'Size of content array is different from size of memory block.'`
- `model/MemoryBlock.java:152` `IllegalStateException` - `'Size of type array is different from size of memory block.'`
- `model/MemoryBlockIterator.java:52` `IllegalStateException` - `'End of memory block reached.'`
- `model/MutableMemoryInspectorState.java:123` `IllegalStateException` - `'No segment selected yet. Cannot set selection range.'`
- `model/Oric.java:80` `IOException` - `'Unsupported file header '` + value + `'.'`
- `model/Oric.java:105` `IOException` - `'Segment end address is lower than segment start address.'`
- `model/ProfileLogic.java:49` `IOException` - `"File '"` + path + `"' is empty."`
- `model/Segment.java:174` `IllegalArgumentException` - `'Invalid offset.'`
- `model/Segment.java:215` `IllegalStateException` - `'Merged segments must not exceed 64kb.'`
- `model/Segment.java:270` `IllegalStateException` - `'End must not be before begin.'`
- `model/Segment.java:459` `IllegalStateException` - `'Cannot allocate symbol in empty segment.'`
- `model/Workspace.java:164` `IllegalArgumentException` - `'Invalid processor type: '` + type + `'.'`
- `model/Xml.java:134` `IOException` - `"Mismatched root element: expected '"` + a + `"' but found '"` + b + `"'."`
- `model/SegmentListInserter.java` - none (its "no free segment" message now goes through `Messages.E035.format()`, see recent migration - not a hit here since it's no longer a raw literal)
- `ui/ComputerFont.java:216` `IOException` - `'Font resource not found: '` + name
- `ui/ElementUtilities.java:70`, `:102` `RuntimeException` - `"No '&' contained in label text '"` + text + `"'."`
- `ui/ElementUtilities.java:75`, `:107` `RuntimeException` - `"Mnemonic character '"` + c + `"' contained in label text '"` + text + `"' is not between 'A' and 'Z'."`
- `ui/GraphicMode.java:82` `IllegalArgumentException` - `'Invalid ANTIC mode: '` + mode + `'.'`

## Summary

| Group | Count |
|---|---|
| A) User-facing dialog titles/messages | 50 |
| B) Internal exception/invariant messages | 62 |
| **Total sink hits found** | **112** |
