# Open TODOs in the source

A standing inventory of every `TODO` comment left in `src/` and `test/`
(there are none in `test/`), grouped by what the analysis found. Taken and
analyzed against the code on 2026-10-03. Keep it current: when a TODO is
resolved, remove it from the code and from this list; when one is added,
add it here.

Not counted: the `; $XXXX` format text in `DisassemblyGridPanel`.

## Open - found during the cartridge import (2026-10-04)

| Location | TODO and finding |
|---|---|
| [`AtariCartridgeReader.java`](../src/com/wudsn/tools/dis6502/model/system/atari/AtariCartridgeReader.java) (`readCartridge`) | Some cartridges appear at several mirrors, e.g. Blizzard 4 KB (46) and 5200 4 KB (20) at `$A000` and `$B000`; each bank becomes a segment at one address only, the one with the vectors. Code reached through another mirror - e.g. the init vector of the sample images points to `$A000` - is not traced. The mirrors are in `CartridgeType.BankRegion.getMirrorAddresses()`. |

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
| [`Disassembly.java:1119`](../src/com/wudsn/tools/dis6502/model/Disassembly.java) | Comments on an instruction's operand bytes never appear in the listing: only the opcode byte is looked up (size 1, once `getOpcodeLength(by)`). A comment placed on an operand byte, e.g. through a memory inspector selection, is lost. Writing them would change listings, so it needs a decision: before the instruction, or not allowed at all. |
| [`SegmentWriteBootDiskDialog.java:51`](../src/com/wudsn/tools/dis6502/ui/SegmentWriteBootDiskDialog.java) | `DiskImage.writeAbsoluteSector` stores the write result (e.g. `WRITE_PROTECT`, `OUT_OF_RANGE`) in `sector.result` and discards it, so the dialog can report success after writing nothing. Fix: return the `ImgError` and throw on failure. |
| [`SegmentList.java:426`](../src/com/wudsn/tools/dis6502/model/SegmentList.java) | Redundant code for addresses and address labels. The two branches also differ: the second returns a user equate's label without setting `defined[0] = true`, which looks unintended. |
| [`EquateList.java:181`](../src/com/wudsn/tools/dis6502/model/EquateList.java) | `findEquateByAddress` hardcodes SDX page 7 (`$0700-$07FF`). System equates should be marked instead. |
| [`EquateList.java:223`](../src/com/wudsn/tools/dis6502/model/EquateList.java) | `addRange` should take a label access; ranges are always `READ_WRITE`. |
| [`Disassembly.java:298`](../src/com/wudsn/tools/dis6502/model/Disassembly.java) | The referenced check does not distinguish the access type, so if `$80` is referenced, the zero-page and display-list constants count as referenced alike. |
| [`Oric.java:68`](../src/com/wudsn/tools/dis6502/model/system/oric/Oric.java) | Are all zero-page addresses really base addresses on the Oric? Needs research. |
| [`Oric.java:78`](../src/com/wudsn/tools/dis6502/model/system/oric/Oric.java) | File type detection should support the Orix header format (https://orix.oric.org/orix-header/) and the tape header format (https://forum.defence-force.org/viewtopic.php?t=201). |
| [`SegmentList.java:578`](../src/com/wudsn/tools/dis6502/model/SegmentList.java) | Is `getUserComment`'s per-byte comment lookup over the line's size correct? It looks right; unconfirmed. |
| [`DisassemblyResultWriter.java:84`](../src/com/wudsn/tools/dis6502/model/DisassemblyResultWriter.java) | A note rather than a task: listing lines are printed with `useAlignment=false`; only the inserted directives use `true`. Could become a plain comment. |
| [`DisassemblyResultFile.java:191`](../src/com/wudsn/tools/dis6502/model/DisassemblyResultFile.java) | `saveListing` should return the include files it wrote. No caller would use the list yet (e.g. for a success message). |
