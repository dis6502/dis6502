# Open TODOs in the source

A standing inventory of every `TODO` comment left in `src/` and `test/`
(there are none in `test/`), grouped by what the analysis found. Taken and
analyzed against the code on 2026-10-03. Keep it current: when a TODO is
resolved, remove it from the code and from this list; when one is added,
add it here.

Not counted: the `; $XXXX` format text in `DisassemblyGridPanel`.

## Open - bugs found during the analysis

| Location | TODO and finding |
|---|---|
| [`SegmentPropertiesDialog.java:119`](../src/com/wudsn/tools/dis6502/ui/SegmentPropertiesDialog.java) | "Will not work with >64K." The overflow check is dead: `begin` is masked to 16 bits, but `begin + size - 1` is a Java `int` that never wraps, so `begin > end` is never true, E037 is never shown, and a `wEnd` above `$FFFF` can be stored. Fix: check `end > 0xFFFF`. |
| [`Disassembly.java:1119`](../src/com/wudsn/tools/dis6502/model/Disassembly.java) | "Why was this getOpcodeLength(by)?" The user-comment offset is computed from the *current* segment's `wBegin`. If the byte just read was a segment's last and another binary segment follows, `segmentIndex` has already advanced, the offset is negative, and that byte's comment is silently dropped. Fix: use `opcodeSegment.wBegin`. |
| [`Dis6502.java:1761`](../src/com/wudsn/tools/dis6502/Dis6502.java) | "This is Atari specific." Wider than this line: the whole File > Write Boot Disk feature is Atari DOS-only, but it is enabled for every computer system (`Dis6502.java:584` checks only `notEditing && hasSegments`). |

## Open - partly done, or now easy to finish

| Location | TODO and finding |
|---|---|
| [`EquateDialog.java:153`](../src/com/wudsn/tools/dis6502/ui/EquateDialog.java) | Report why the equate failed to parse. The model side exists: `EquateList.addEquate(String)` returns an `EquateResult` with a non-empty `error`; only the dialog's message box is missing. |
| [`DisassemblyWriter.java:51`](../src/com/wudsn/tools/dis6502/model/DisassemblyWriter.java) | Which bytes `showNonASCIIChararactersAsBytes` treats as non-ASCII depends on the character set. Now possible through `CharacterSet`; the ranges are still hardcoded. |
| [`EquateList.java:256`](../src/com/wudsn/tools/dis6502/model/EquateList.java) | Consider the parent equate list and recursion. One level was added since (the `IOCB0+ICCOM` offset label, lines 267-272); the parent list and deeper recursion are missing. |
| [`Disassembly.java:555`](../src/com/wudsn/tools/dis6502/model/Disassembly.java) | "Why `pc++`?" Probably to keep `pc` pointing past the last byte read when the last segment runs out, matching the normal `else` branch. Likely, not verified. |

## Open - unchanged

| Location | TODO and finding |
|---|---|
| [`SegmentWriteBootDiskDialog.java:51`](../src/com/wudsn/tools/dis6502/ui/SegmentWriteBootDiskDialog.java) | `DiskImage.writeAbsoluteSector` stores the write result (e.g. `WRITE_PROTECT`, `OUT_OF_RANGE`) in `sector.result` and discards it, so the dialog can report success after writing nothing. Fix: return the `ImgError` and throw on failure. |
| [`SegmentList.java:426`](../src/com/wudsn/tools/dis6502/model/SegmentList.java) | Redundant code for addresses and address labels. The two branches also differ: the second returns a user equate's label without setting `defined[0] = true`, which looks unintended. |
| [`EquateList.java:181`](../src/com/wudsn/tools/dis6502/model/EquateList.java) | `findEquateByAddress` hardcodes SDX page 7 (`$0700-$07FF`). System equates should be marked instead. |
| [`EquateList.java:223`](../src/com/wudsn/tools/dis6502/model/EquateList.java) | `addRange` should take a label access; ranges are always `READ_WRITE`. |
| [`Disassembly.java:298`](../src/com/wudsn/tools/dis6502/model/Disassembly.java) | The referenced check does not distinguish the access type, so if `$80` is referenced, the zero-page and display-list constants count as referenced alike. |
| [`Atari800.java:463`](../src/com/wudsn/tools/dis6502/model/system/atari800/Atari800.java) | The typing of a ROM's last 6 bytes (run address, flags, init address) should be one method shared by all Atari ROM formats. (The similar typing at lines 536 and 603 is the cassette and boot-sector header, not a ROM trailer.) Planned in [`12_CARTRIDGE_IMPORT_PLAN.md`](12_CARTRIDGE_IMPORT_PLAN.md). |
| [`Oric.java:68`](../src/com/wudsn/tools/dis6502/model/system/oric/Oric.java) | Are all zero-page addresses really base addresses on the Oric? Needs research. |
| [`Oric.java:78`](../src/com/wudsn/tools/dis6502/model/system/oric/Oric.java) | File type detection should support the Orix header format (https://orix.oric.org/orix-header/) and the tape header format (https://forum.defence-force.org/viewtopic.php?t=201). |
| [`SegmentList.java:578`](../src/com/wudsn/tools/dis6502/model/SegmentList.java) | Is `getUserComment`'s per-byte comment lookup over the line's size correct? It looks right; unconfirmed. |
| [`DisassemblyResultWriter.java:84`](../src/com/wudsn/tools/dis6502/model/DisassemblyResultWriter.java) | A note rather than a task: listing lines are printed with `useAlignment=false`; only the inserted directives use `true`. Could become a plain comment. |
| [`DisassemblyResultFile.java:191`](../src/com/wudsn/tools/dis6502/model/DisassemblyResultFile.java) | `saveListing` should return the include files it wrote. No caller would use the list yet (e.g. for a success message). |
