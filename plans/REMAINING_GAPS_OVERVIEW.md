# Remaining Gaps: C++ dis6502 vs. Java jdis6502 Port

This document inventories what still works in the C++ `dis6502` project
(`C:\jac\system\Windows\Programming\Repositories\dis6502`) but is missing,
incomplete, or broken in this Java port, as of 2026-09-21. It exists so a
future porting session can pick up where this snapshot left off without
re-auditing both codebases from scratch.

**Status**: every gap this document tracked (#1-#9) is fixed and removed
from it, per this project's convention - git history has each write-up
(gaps #4 and #5 had a factually wrong original description, corrected in
their commits: `LogPanel` coloring, and `MainUITest.cpp` being a headless
console suite rather than a UI driver). The "explicitly unverified" items of
the first pass were all settled by the second audit
([`FINAL_GAP_ANALYSIS.md`](FINAL_GAP_ANALYSIS.md), which also verified every
menu command and icon one by one) or since: the legacy loaders are tested
against real `WRK14`/`PRF17` fixtures, `ImgError`/`AtariError` match the C++
enums one to one, `Application`'s settings and messages are exercised by the
suite, and C++'s `OperatingSystem::ExecuteCommand` was only ever used by its
test harness. Numbers are kept as originally assigned, so references
elsewhere (commit messages, `plans/MEMORY.md`) stay valid.

**Note (2026-09-21):** the porting phase itself is now over - see
`plans/PORTING_GUIDE.md`'s status note and `plans/MEMORY.md`'s top section.
The Java codebase is the finished port and is expected to intentionally
diverge from C++ going forward, so this document's framing ("what's
missing relative to C++") no longer drives new work the way it did while
the gaps below were being closed - it remains useful as a historical record
and for any of the specific items still listed below that are still worth
deciding on their own merits.

## Methodology and confidence level

This was compiled by two passes - one over the model/logic layer
(`src/*.cpp`/`src/systems/` vs. `com.wudsn.tools.dis6502.model`), one over the
UI layer (`src/ui/*.cpp` vs. `com.wudsn.tools.dis6502.ui`) - each matching
C++ files to Java classes by name/package correspondence, spot-checking
method/field lists and javadocs for stated limitations, and grepping for
`TODO`/`FIXME`/stub markers. This is **not** a full command-by-command diff
against every C++ `.rc` resource or a line-by-line diff of every method
body; several items below are flagged explicitly as unverified rather than
guessed at. Treat "fully ported" entries as spot-checked, not proven, and
re-verify against the current C++ source before relying on any single line
here - both repos continue to change.

## Divergences where the Java port fixed a real C++ bug (not a Java gap)

Per the porting guide's bug-handling policy, these are intentional
divergences, not regressions - listed here so they aren't mistaken for
inconsistencies between the two codebases:

- **`DiskImageSectorsDialog` / `MainFile::OpenDiskImageSectors`**: an
  out-of-bounds read past an undersized buffer was found in the C++ logic.
  Java implements the evidently-correct behavior; the C++ side was left
  unchanged because confirming the fix needs interactive GUI testing that
  wasn't available (per the policy's "leave a TODO in C++, implement the
  correct behavior in Java" branch) - check `MainFile.cpp` for a `TODO:`
  comment describing this before assuming it's unnoticed.
- **`SegmentPropertiesDialog`**: carries forward a pre-existing C++ TODO
  about not supporting segments >64K faithfully (not a new Java bug).
- **`SegmentWriteBootDiskDialog`**: carries forward a pre-existing C++ TODO
  that `WriteAbsoluteSector` never reports write failure to the caller -
  documented per the bug-handling policy, not fixed (needs interactive GUI
  confirmation).

## Deliberate simplifications / architecture differences (not gaps)

Recorded so a future session doesn't "fix" something that was already a
reviewed, intentional decision:

- **`DisassemblyFindStringDialog.cpp`** has no direct Java counterpart - its
  modal find dialog was replaced by an always-visible inline find
  field/button directly on `DisassemblyPanel`
  (`performFindInDisassembly`/`performFindNextInDisassembly` in
  `Dis6502.java`). Functionally equivalent, deliberately different UI shape.
- **`FindStringDialog.java`** keeps the C++ name but is now a shared plain
  value/conversion helper (ASCII&#8596;hex sync) composed into
  `MemoryInspectorFindStringDialog`, not a dialog itself - mirrors how C++
  composes the same class into both `MemoryInspectorFindStringDialog` and
  `DisassemblyFindStringDialog`.
- **`Layout.cpp`** has no Java `Layout` class - its `Layout::Compute` logic
  was folded directly into `MainWindow.java`'s nested `JSplitPane`
  construction (see `plans/MEMORY.md`'s note on this).
- **`WorkspaceFont.cpp`** has no dedicated Java class - folded into
  `Dis6502.java`'s font-update logic (`setComputerFont` calls,
  `Dis6502.java:377`).
- **`MessageBoxDialog.cpp`** has no dedicated Java class - plain
  `JOptionPane` calls are used directly at each call site.
- **`SelectSpritesDialog.cpp` / `SpriteControl(Impl).cpp`** were deliberately
  renamed to `SelectGraphicsDialog` / `GraphicPanel` / `GraphicMode` (the
  renaming is explained in `SelectGraphicsDialog`'s own javadoc). This one
  appears fully and carefully ported, including mouse-drag row selection -
  not a gap despite the "sprite" name sounding exotic/rare.
- **`DiskImageSectorsController.cpp`, `EquateListController.cpp`,
  `ProfilesController.cpp`, `MainController.cpp`**'s dialog-scoped duties
  were folded into their owning dialog's own `performOK()`/handler methods,
  per the porting guide's explicit convention for simple, single-dialog
  controllers.
- **Win32 control/window wrapper base classes** (`Button.cpp`,
  `CheckBox.cpp`, `ComboBox.cpp`, `Control.cpp`, `DC.cpp`, `Dialog.cpp`,
  `EditControl.cpp`, `Font.cpp`, `ListBox.cpp`, `Menu.cpp`, `PartWindow.cpp`,
  `PopupMenu.cpp`, `RadioButtonGroup.cpp`, `ScrollBar.cpp`, `TextLabel.cpp`,
  `Window.cpp`) have no Java counterparts - Swing provides these natively.
  Not gaps.
- **`utils.cpp`** (1994 Microsoft sample-code DIB/palette utilities used only
  by the Win32 sprite bitmap rendering) is superseded by `GraphicPanel`'s
  `BufferedImage`-based approach. Not a gap.
- **`MRUMenu.cpp`** was folded into `MRUController.java` +
  `MainMenu.java`'s `recentFilesMenu`/`recentWorkspacesMenu`, fully wired in
  `Dis6502.java` - verified complete.
- **`ByteArray.h`/`ByteSequence.h`/`Byte.h`** &rarr; native Java `byte[]`
  throughout.
- **`File.h`/`FileIO.h`/`FileInputStream.h`/`InputStream.h`/`OutputStream.h`/
  `Stream.h`/`Writer.h`** &rarr; `java.io.*` plus WUDSN-Base's
  `com.wudsn.tools.base.common.FileUtility`.
- **`DisassemblyBuffer.h`** (`DIS_BUFFER`, a manual linked-list byte-packing
  scheme) and **`DisassemblyResultIterators.h`** (custom iterators over it)
  &rarr; confirmed superseded: `DisassemblyResult.java` uses a plain
  `List<DisassemblySectionType>` and standard Java collections; no
  packed-buffer structure exists anywhere in the Java model.
- **`MemoryInspectorSelection.h`** &rarr; merged into
  `MemoryInspectorState`/`MutableMemoryInspectorState.getByteSequence()`
  (returns `byte[]` directly) - matches the merge already documented in
  `plans/MEMORY.md`.
- **`DatatypeUtility.h`** (centralized hex-string conversion) is not
  centralized in Java; equivalent hex-string logic is distributed across
  `Memory.java`, `Segment.java`, `Equate.java`, `Disassembly*.java`,
  `Xml.java` individually. Functionally covered, architecturally different -
  a divergence from the usual 1:1 file match, not a functional gap.
- **`XRef.h`** is a trivial one-line typedef (`LineNumber = size_t`) with no
  porting need.
- **`GuessCodeLogic.java`** has no matching C++ model file by name - its
  logic actually originates from `ui/MemoryInspector.cpp`'s "guess code"
  feature; the Java port deliberately relocated it from the UI layer into
  the model layer. Not a gap.

## Fully ported, spot-verified

**Model/logic layer**: `AddressLabel(List)`, `Assembler`, `Comment`,
`ComputerSystem`/`ComputerSystemFactory`/`ComputerSystemType(Info)`,
`DefaultFolders(Logic)`, `Disassembly`, `DisassemblyLine`/`LineWriter`/
`OpcodeBuffer`/`ProgressMonitor`/`Result`/`ResultFile`/`ResultWriter`/
`Section(Type)`/`Writer`, `DiskImage`, `Encoding`, `Equate`/`EquateList`/
`EquateListLogic`/`EquateType`, `FileHeader`/`FileType`/`FolderType`,
`Fixup`/`FixupType`, `InstructionSet` (split into `InstructionSetMOS6502` /
`InstructionSetMOS65C02` - elaboration, not a gap), `LabelAccess`,
`MRUEntry`/`MRUList`, `Memory`/`MemoryBlock`/`MemoryBlockIterator`,
`MemoryInspectorStack`, `Pass1`, `ProcessorType`, `Profile`/`Profile1X`/
`ProfileLogic`, `Segment`/`SegmentList`/`SegmentListInserter`, `Symbol`,
`Workspace`/`Workspace1X`/`WorkspaceLogic`, `Xml`. All per-computer-system
classes (`Atari800`, `AtariDOS`, `AtariDiskImage`, `C64`, `Oric`, `Unknown`,
`Atari5200`) are present with method counts roughly matching their C++
counterparts.

**UI layer**: `AboutDialog`, `AssembleDialog`, `CommentDialog`,
`ComputerFont`, `DefaultFoldersDialog`, `DisassemblyProgressDialog`,
`DiskImageExecutableFileDialog` (looks complete, not deep-diffed
field-by-field), `EquateDialog`, `EquateRangeDialog`, `LowHighByteDialog`,
`MRUController`, `MainMenu`, `MainWindow` (layout verified against
`Layout::Compute` per `plans/MEMORY.md`), `ProfileDialog`, `RawFileDialog`,
`SegmentPropertiesDialog`, `SegmentWriteBootDiskDialog`, `UIApplication`,
`WorkspaceDialog`. The completed Actions/ElementFactory and popup-menu
accelerator migrations (`plans/ACTIONS_ELEMENT_FACTORY_MIGRATION.md`,
`plans/POPUP_MENU_ACCELERATORS_PLAN.md`) were spot-checked clean - no
literal `JButton("OK")`-style stragglers, no direct `setAccelerator` calls
bypassing the `InputMap`/`ActionMap` pattern.

**Opcode/addressing-mode table parity check - completed 2026-09-21, no gap
found.** `InstructionSetMOS6502.java` and `InstructionSetMOS65C02.java` were
diffed entry-by-entry (all 512 opcode rows combined) against C++'s
`InstructionSet.cpp`: mnemonic, `illegal` flag, `LabelAccess`, and
`OperandMode` all match exactly for every opcode in both tables. The
supporting `Instruction` class (`getLength()`/`isImmediateMode()`/
`isUnsupportedInstruction()`) also matches C++'s `Instruction::GetLength()`
etc. one-for-one, including a shared quirk: `GetLength()`'s `switch` never
gained cases for 65C02-only `OperandMode`s (`ZeroPageIndirect`,
`ZeroPageRelative`, `IndexedIndirectAbsolute`, `ReservedNop1Byte`/`2Byte`/
`3Byte`), falling through to its `default: return 1` for all of them in
*both* languages - but this is harmless in both, since actual instruction
length for those modes is computed by dedicated `OperandMode` switches
directly in `Disassembly.cpp`/`Disassembly.java` (confirmed present for all
six modes in both files), never through `Instruction::GetLength()`/
`getLength()`. Not a gap - `InstructionSet`'s own `GetLength()` is simply
unused for these modes in both codebases.

## Shared C++ limitations - not Java-specific gaps

These exist in the C++ source itself, so their presence/absence in Java
isn't a porting gap - don't "fix" the Java side to add something C++ itself
doesn't support, without a separate decision to add a genuinely new feature:

- **Atari5200 bank-switched cartridges/.CAR files** are not supported in
  C++ either - `Atari5200.cpp:79` and `Atari5200.java:59` carry the
  identical comment.
- **`tst/suite/disassembly/unit005`** (an SDX relocation-base test) is
  disabled in C++ itself - `MainTest.cpp:330` comments it out with
  `// TODO SDX does not work yet because of using $0000 as relocation base address`.
  Its absence from Java test coverage isn't a port gap.
- **`ComputerSystemTest`** in both languages only exercises Atari800 + C64 -
  C++'s own `ComputerSystemTest.cpp:10-11` never calls a
  `TestOric`/`TestAtari5200`/`TestUnknown`. Java parity confirmed
  (`ComputerSystemTest.java` also only has `testC64`); not a Java-specific
  coverage gap.
- **C++-only test classes `CommonTest`, `StreamTest`, `FileIOTest`** have no
  Java counterpart - consistent with the I/O-abstraction supersession noted
  above (`java.io.*` + WUDSN-Base), not a gap.
