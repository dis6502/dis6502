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

## Open - unchanged

| Location | TODO and finding |
|---|---|
| [`Oric.java:68`](../src/com/wudsn/tools/dis6502/model/system/oric/Oric.java) | Are all zero-page addresses really base addresses on the Oric? Needs research. |
| [`Oric.java:78`](../src/com/wudsn/tools/dis6502/model/system/oric/Oric.java) | File type detection should support the Orix header format (https://orix.oric.org/orix-header/) and the tape header format (https://forum.defence-force.org/viewtopic.php?t=201). |
| [`DisassemblyResultFile.java:191`](../src/com/wudsn/tools/dis6502/model/DisassemblyResultFile.java) | `saveListing` should return the include files it wrote. No caller would use the list yet (e.g. for a success message). |
