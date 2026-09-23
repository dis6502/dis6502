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

**Status**: every entry originally found that was actually a text-
externalization candidate has now been migrated to `Text.java`/
`Texts.java`/`Messages.java` (see git history for what moved where and
when: all 13 dialog `setTitle(...)` calls, every static `JOptionPane`
title, every static/parameterizable `JOptionPane` message, every static-
literal `setDialogTitle(...)` call, every dynamically-built "Add "/"Open "
title, every internal `IOException`, and finally
`SegmentWriteBootDiskDialog`'s one deliberately-unported-in-C++-too
literal - the old groups A and B are both gone now that they're empty).
What remains below (groups C/D/E) is out of scope by design, not
outstanding work: every `IllegalArgumentException`/`IllegalStateException`/
`RuntimeException` usage, explicitly flagged as a programming-error guard
rather than user- or log-facing text.

## C) IllegalArgumentException usages - not relevant for text externalization

Flagged as out of scope: every `throw new IllegalArgumentException(...)`
site is a parameter-validation guard (a programming-contract check on a
method's own arguments), not a message meant to be read by a user or
logged for diagnosis. None of these have a C++ resource to trace back to,
none are ever shown in a dialog, and none should move to `Text.java`,
`Texts.java`, or `Messages.java` - they stay hardcoded
`IllegalArgumentException` messages permanently, not "not yet migrated."

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
| User-facing dialog titles/messages (formerly group A, now gone - all migrated) | 48 (of 50 originally found - 2 reclassified to group C) | 48 | 0 | - |
| `IOException` usages (formerly group B, now gone - all migrated) | 21 (of 62 originally found in the old group B - 15 reclassified to group C, 22 to group D, 4 to group E) | 21 | 0 | - |
| C) `IllegalArgumentException` usages | 17 (2 from the old group A, 15 from the old group B) | 0 | 0 | 17 (parameter-validation guards) |
| D) `IllegalStateException` usages | 22 (from the old group B) | 0 | 0 | 22 (programming errors - violated invariants) |
| E) `RuntimeException` usages | 4 (from the old group B) | 0 | 0 | 4 (programming errors - a caller's malformed input) |
| **Total** | **112** | **69** | **0** | **43** |
