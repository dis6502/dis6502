# Final Gap Analysis: C++ dis6502 vs. Java port (2026-09-21)

Second and final audit of working C++ features that are missing in the Java
port, done before retiring the C++ version. It complements
[`REMAINING_GAPS_OVERVIEW.md`](REMAINING_GAPS_OVERVIEW.md) (the first audit,
all of whose gaps #1-#9 are closed).

## Methodology - how this differs from the first audit

The first audit matched C++ files to Java classes by name and spot-checked
them. That finds missing *classes*, but not missing *behavior* inside a class
that exists. This pass instead walked the C++ application from its entry
points inward:

1. `dis6502.rc`: every `MENUITEM`, every `ACCELERATORS` row, every dialog,
   compared against `Actions.java`/`Actions.properties`.
2. `Main.cpp`: `WinMain`/`InitApplication` (command line), `WM_INITMENU`
   (menu gating), `WM_COMMAND` dispatch, `HandleWorkspaceChanged`,
   `WM_DROPFILES`, `WM_OPENCMDLINE`.
3. `MainFile.cpp`: the open/add/save flows, compared against the
   `Dis6502.performXxx` methods.
4. Custom controls (`MemoryInspectorControlImpl`, `DisassemblyControlImpl`):
   every handled window message and key.
5. Persistence: XML attribute/element names and enum keys of `Workspace`,
   `Segment`, `Profile`, `Equate`; INI settings sections.
6. Every `not ported`/`TODO` marker in the Java source, each re-verified
   against the current code (several turned out to be stale javadoc, some
   turned out to be real).

## Confirmed gaps, by user impact

### A. ~~System equates are never loaded~~ - FIXED 2026-09-21

**Fix**: `Atari800.equ`, `Atari5200.equ` and `Oric.equ` now ship as classpath
resources (`model/systems/`), read through the new
`ComputerSystem.openResourceByExtension` and
`WorkspaceLogic.loadSystemEquates`. `Dis6502.loadSystemEquatesIfEmpty` calls
it at startup, on a computer system change, after the workspace is cleared
for a new/opened file, and after a workspace load - "if empty" so that a
`.wrk` carrying its own system equates keeps them.

Two findings along the way:
- **`C64.equ` was written from scratch, not ported.** The C++ file is a stale
  copy of `Atari800.equ` (16 differing lines) - it would label the VIC-II's
  `$D01A` as Atari's `COLBK`. Same mislabeled-asset problem as `C64.fon`. The
  new file (455 labels: 6510 port, BASIC/KERNAL zero page, pages 2-3 incl.
  all indirect vectors, VIC-II, SID, CIA #1/#2, common BASIC ROM routines,
  the KERNAL jump table, hardware vectors) uses the KERNAL/BASIC source label
  names as published in "Mapping the Commodore 64". Verified: every line
  parses, MADS accepts every label (no reserved-word collisions), and a
  hand-built C64 routine disassembles to `sta EXTCOL`/`jsr CHROUT`/
  `sta CINV+1`/`jmp SYSIRQ` and reassembles byte-exactly.
- **Fixed a disassembler defect C++ only works around.** With the default
  profile (`omitUnreferencedSystemLabels = true`), `STA IOCB0+ICCOM,X` marked
  `IOCB0` as referenced but not the constant `ICCOM`, so `ICCOM` was omitted
  and the listing did not assemble - which is why C++'s own test suite forces
  the option off (`// TODO Make them work instead`). `Equate` now records a
  symbolic offset label and `EquateList.setBaseLabelsReferenced` marks it
  too. `ReassemblyRoundTripTest` no longer needs the override: all four
  fixtures reassemble byte-exactly with real system equates and the default
  profile.

Verified with the full `TestRunner` suite (new
`WorkspaceLogicTest.testLoadSystemEquates`) and live against the running
app. Original finding:

C++ loads `systems/<system>/<System>.equ` (`Atari800.equ`, `Atari5200.equ`,
`C64.equ`, `Oric.equ`) into the workspace's system equate list whenever the
computer system type changes (`Main::HandleWorkspaceChanged`,
`COMPUTER_SYSTEM_TYPE` case) and after a file open clears the workspace
(`Main::PromptToClearWorkspace`). That is what makes a disassembly read
`STA COLBK` instead of `STA $D01A`.

Java: `WorkspaceLogic.loadSystemEquates` does not exist, and the `.equ` files
are not shipped - the only copy is `test-resources/equates/Atari800.equ`.
The whole consuming side *is* ported (`Workspace.getSystemEquateList`,
`Disassembly.generateEquates(SYSTEM_EQUATES)`, Equates > Display/Clear System
Equates, `Profile.omitUnreferencedSystemLabels`), so the list is simply always
empty unless a loaded `.wrk` happens to carry one. "Display System Equates"
shows an empty dialog; "Clear System Equates" is permanently disabled.

The two blockers named in `WorkspaceLogic`'s javadoc are both stale:
`EquateListLogic` has been ported for a while, and the install-folder lookup
(`GetModuleFilePath`) is unnecessary in Java - the `.equ` files can be
classpath resources, exactly like the `.ttf` fonts already are.
`ReassemblyRoundTripTest` currently has to force
`omitUnreferencedSystemLabels = false` to work around this.

### B. ~~Opening a file always resets the computer system to Atari 800~~ - FIXED 2026-09-21

**Fix**: removed the six forced `setComputerSystemTypeID("ATARI800")` calls
from the open paths; `Workspace.init()` already leaves the computer system
untouched, so only the startup default remains. Verified live: with the
workspace set to C64, opening `HelloWorld.prg` keeps C64 and yields the
expected `$0801` segment. Original finding:

`Dis6502.java` calls `workspace.setComputerSystemTypeID("ATARI800")` in every
non-"Add" open path (lines 456, 556, 613, 713, 787, 845) and at startup
(239). C++'s `ClearWorkspace` leaves the computer system alone; only File >
New Workspace (via `WorkspaceDialog`) or the command line changes it.

Effect: after New Workspace > C64, "Open Executable File..." silently flips
the workspace back to Atari 800 before parsing, so a C64 `.prg`/Oric `.tap`
is parsed with the Atari loader. C64, Oric and Atari 5200 are only reachable
through "Add File" or an existing `.wrk`.

### C. Command line is ignored - MEDIUM

C++ accepts `dis6502.exe [/SYSTEMID] [file]`: the system ID (`/ATARI800`,
`/ATARI5200`, `/C64`, `/ORIC`, ...) selects the initial computer system, and
the file is opened with automatic type detection. This is what makes
file-type association / "Open with" / a desktop shortcut per system work.
Java's `run(String[] args)` only null-checks `args`.

The detection itself (`ComputerSystem.guessFileType`, all five systems) is
fully ported - it just has no caller. (`/TEST:` and `/DEBUG` are
deliberately out of scope - see gap #5's history.)

### D. No drag and drop - MEDIUM

C++ accepts a single file dropped on the segment list
(`MainSegment::DropFilesProc`) and opens it with automatic type detection -
the same `OpenFile(path, UNKNOWN_FILE, false)` entry point as C. Java has no
`TransferHandler`/`DropTarget` anywhere. C and D share one missing method: a
Java `openFile(path, FileType.UNKNOWN_FILE, add)` dispatcher equivalent to
`MainFile::OpenFile`.

### E. Disassembly popup: "Navigate Back to Previous Position" - MEDIUM

C++ keeps a jump history in the disassembly control; Navigate to Definition
(Return/double click) pushes onto it and Backspace / the popup item pops.
Java has Navigate to Definition but no history, no popup item and no
Backspace binding (`Dis6502.java:1366` says so explicitly). Following a `JSR`
is one-way.

### F. Disassembly popup: "Change type of immediate byte to" - MEDIUM

C++ submenu (Code / Low Byte / High Byte / Char Constant / Unknown), enabled
when the right-clicked line is an immediate-mode instruction, with a check
mark on the current type. This is the quick way to turn `LDA #$40` into
`LDA #<LABEL`. Java's `DisassemblyPanel` javadoc claims the needed detection
"does not exist", but that is stale: `Disassembly.isInstructionWithImmediate`
is ported, and `performSetMemoryInspectorLoHiType` already does the mutation
for the memory inspector's equivalent. Only the menu items and wiring are
missing.

### G. Default folders have no effect, file choosers have no filters - MEDIUM

View > Default Folders... works as a dialog and persists per system, but
nothing reads the result (`FileDialogs::SetDefaultFolders` has no Java
counterpart, acknowledged at `Dis6502.java:1528`). All `JFileChooser`s start
in the current file's folder instead. C++ additionally starts each dialog at
the last file of that *file type* (`mruController->GetLastFilePath(fileType)`
- ported in `MRUController.getLastFilePath`, never called).

Related: C++ file dialogs filter by type (`.bin;.com;.exe;.sys;.xex`,
`.bin;.car;.rom`, `.cas`, `.atr;.xfd`, `.asm`, `.prf`) and append the default
extension on save. Java only filters `.wrk` and `.equ`; `FileType.java`'s own
javadoc notes the `FileTypeInfo` filter/extension/folder-type table is not
ported.

### H. Main menu is not gated by system or by edit mode - LOW/MEDIUM

C++ `WM_INITMENU`:
- enables each Open/Add item only if
  `computerSystem->IsSupportedFileType(...)` - e.g. no cassette/disk image
  items for C64. Java has `ComputerSystem.isSupportedFileType` but no caller
  in the UI; the items are always enabled and fail at read time instead.
- disables Open/Add/Save while the memory inspector is in edit mode, and
  `WM_COMMAND` passes `editMode` to every command handler. Java does not
  check `isEditMode()` anywhere in `Dis6502`, so a workspace can be replaced
  while an edit is in progress. Worth a targeted test before deciding whether
  that is actually harmful in the Java design.
- enables Save Workspace/Save As only when there are segments; Java leaves
  both always enabled.

### I. Recent Files bypasses the per-type dialogs - LOW/MEDIUM

C++ routes a Recent Files pick through the same `OpenFile` dispatcher as the
menu, so a raw file reopens via `RawFileDialog` (load address, header) and a
disk image via its file/sector picker. Java's `openRecentFile` calls
`workspaceLogic.addFile` directly for every file type. Not run-tested here -
verify what a recent `RAW_FILE`/`DISK_IMAGE_*` entry actually produces.

## C64 support was a stub in C++ too (found while writing `C64.equ`)

Not port gaps - identical in C++ - but fixed here, since the Java version is
the future one:
- ~~`C64.readExecutableFile` never sets `segment.bBinary`~~ - FIXED
  2026-09-21: a loaded `.prg` is now a binary segment and is disassembled
  without a detour through Segment Properties.
- ~~`C64.BASE_ADDRESSES`/`VECTOR_ADDRESSES` are a single `0x0200 /* DUMMY
  EXAMPLE */` entry~~ - FIXED 2026-09-21: the real C64 code vectors
  (`KEYLOG`, the `$0300`-`$0333` BASIC/KERNAL vectors, `USRADD`,
  `NMIVEC`/`RESVEC`/`IRQVEC`) and, as base addresses, additionally the data
  pointers `MEMSTR`/`MEMSIZK`. 16 bit hardware registers that are no
  addresses (CIA timers, SID frequency) are deliberately excluded.
- ~~Address lookups ignore every segment without an Atari file header~~ -
  FIXED 2026-09-21, and **not C64-specific**: `SegmentList.findByAddr`/
  `findSegmentByFixedAddr` and `Segment.isSplittable` tested for
  `ATARI_BINARY`/`SDX_FIXED_BLK`, so "Start code trace" silently did nothing
  and "Split at selection" was unavailable for every `RAW` or `ORIC_BINARY`
  segment - C64, Atari 5200, Oric, ROM images and raw files alike. Replaced
  by the new `Segment.hasFixedAddress()` (everything except the SDX
  relocatable/symbol/fix-up blocks). Found because the vector fix above had
  no effect without it. Atari behavior is unchanged (all four
  reassembly round-trips still byte-exact).
- Still open: `C64.guessFileType` always returns `UNKNOWN_FILE` (matters for
  gaps C/D).
- Cosmetic, still open: `DisassemblyLine.systemAddress == 0` doubles as the
  "not an address line, always write it" sentinel, so a system label at
  `$0000` (`D6510`) is written to every C64 listing even when unreferenced.

Verified by `ComputerSystemTest.testC64Addresses`/
`testC64CodeTraceFollowsVector` (a `.prg` whose IRQ handler is reachable only
through `LDA #< / STA CINV / LDA #> / STA CINV+1` is traced into the
handler, with the two loads retagged as low/high byte).

## Minor divergences (listed for completeness, no action suggested)

- **Home/End in the two grids**: C++ scrolls to top/bottom on plain Home/End
  (non-edit mode); Java inherits `JScrollPane`'s Ctrl+Home/Ctrl+End. PgUp/
  PgDn/Up/Down behave the same.
- **Disassembly Add/Edit Comment** uses the clicked line's own offset/size
  instead of snapping to the enclosing instruction
  (`findOffsetAtStartOfInstruction` is ported but not wired) - differs only
  for label/equate-only lines.
- **"Paste (insert after selection)"** - not a gap; dead code in C++ (see gap
  #3's history).
- **`Workspace1X.Load10`** (`DIS6502WRK10`) - deliberately not ported; the
  C++ author's own TODOs doubt it ever worked and no fixture exists.
- **`Save14`, `AboutDialog`'s module version list, `/DEBUG`, the
  `hPrevInstance` single-instance check** (always null on Win32, so dead) -
  Win32-specific or obsolete.
- **`ProfileLogic.loadDefaultProfile`** (reopen the last-used profile) is
  ported but never called - identical in C++, where `LoadDefaultProfile` also
  has no caller. Not a port gap; possibly a feature worth finishing.

## Verified clean in this pass

- Every main-menu item, segment-list popup item and memory-inspector popup
  item/accelerator in `dis6502.rc` has a wired Java counterpart (the
  Shift+D/Shift+A display-list/data-store hint mismatch is a documented C++
  resource bug).
- XML persistence: `Workspace`/`Segment`/`Comment`/`Profile`/`Equate`
  attribute names and `ProcessorType`/`Encoding`/`EquateType` keys match;
  symbols/fixups/address labels are transient in both (C++ serialization is
  commented out).
- Output encodings (ASCII/ATASCII/UTF-8 incl. `$9B` newline) and include-file
  splitting in `DisassemblyResultWriter`/`DisassemblyResultFile`.
- INI settings: MRU lists, per-system default folders, `LastProfile` all
  read/written. C++ does not persist window placement either.
- Menu icons: all 18 C++ `.ico` files have a Java `.png`.
- Stale "not ported yet" javadoc found on `WorkspaceLogic`, `MainMenu`
  (claims disabled menu items that are in fact all wired), `Workspace`,
  `Application`, `TestRunner`-adjacent classes, `Encoding`/`EquateType`/
  `ProcessorType`/`FileType`/`LabelAccess`, and `Disassembly.java:952`. These
  are documentation debt, not gaps, but they are what hid gap A.

## Suggested order

1. ~~**B**~~ - done.
2. ~~**A**~~ - done, including a genuine `C64.equ`.
3. **C + D together** - one `openFile(path, UNKNOWN_FILE, add)` dispatcher
   serves both, and fixes **I** for free if Recent Files is routed through
   it too.
4. **E, F** - disassembly navigation/retyping conveniences.
5. **G, H** - file dialog polish and menu gating.
6. Javadoc sweep for the stale "not ported" notes.
