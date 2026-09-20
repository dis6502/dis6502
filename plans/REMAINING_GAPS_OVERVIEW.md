# Remaining Gaps: C++ dis6502 vs. Java jdis6502 Port

This document inventories what still works in the C++ `dis6502` project
(`C:\jac\system\Windows\Programming\Repositories\dis6502`) but is missing,
incomplete, or broken in this Java port, as of 2026-09-21. It exists so a
future porting session can pick up where this snapshot left off without
re-auditing both codebases from scratch.

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

## Confirmed gaps - model/logic layer

### 1. `DisassemblyWriter` hardcodes the Atari800 return character for every computer system

- **Java**: `DisassemblyWriter.java:35` -
  `private final int returnCharacter = 0x9B; // TODO: Read from Workspace's ComputerSystem once it is ported.`
  The class javadoc (lines 24-27) says this is a placeholder "since
  `ComputerSystem` is not ported yet."
- **That's now stale.** `ComputerSystem`/`ComputerSystemFactory` are fully
  ported and already wired into `Workspace.getComputerSystem()`
  (`Workspace.java:153`), and `ComputerSystem.getReturnCharacter()` already
  returns the correct per-system value: Atari800/Atari5200 = `0x9B`,
  **C64 = `0x0D`, Oric = `0x0D`, Unknown = `0x0A`** (`ComputerSystem.java:55`,
  `Atari800.java:78`, `C64.java:31`, `Oric.java:46`, `Atari5200.java:34`,
  `Unknown.java:21`).
- C++'s `DisassemblyWriter.cpp:15` does
  `returnCharacter = workspace.GetComputerSystem()->GetReturnCharacter();` -
  this one-line wiring was simply never followed up in Java after
  `ComputerSystem` landed.
- **Impact**: `isByteAllowedInString()` (line 54) uses this wrong constant to
  decide whether a byte can render as an ASCII string character vs. must be
  shown as a raw hex byte in the disassembly listing. For C64/Oric/Unknown
  workspaces this produces incorrect listing output, since Atari's `0x9B`
  end-of-line byte is not their line-end byte.
- **Related, same root cause**: line 58's
  `TODO: This actually depends on the character set of the computer system`
  (the `showNonASCIIChararactersAsBytes` check) is not computer-system-aware
  either and should be fixed alongside the above.
- **Fix shape**: read the return character (and the ASCII-allowed check) from
  `workspace.getComputerSystem()` instead of a hardcoded field, mirroring the
  C++ line above.

### 2. `EquateList.addEquate()` silently swallows parse errors instead of logging them

- **Java**: `EquateList.java:296-299` - javadoc says *"Unlike the C++
  version, a parse error is not yet reported anywhere (the C++ version sends
  it to the application's message log) - this is deferred until
  application-level logging is ported."*
- **That's also stale.** `application.sendInfoMessage(...)` /
  `application.sendErrorMessage(...)` is already fully wired and used
  elsewhere in the model layer for exactly this kind of file-parse
  error/info reporting: `EquateListLogic.java` (lines 54/63/67/75/92),
  `ProfileLogic.java`, `WorkspaceLogic.java`.
- **Impact**: a malformed line in an equates file is dropped with no
  user-visible feedback at all (no log entry, no error dialog), unlike C++.
- **Fix shape**: call `application.sendErrorMessage(...)` from
  `addEquate()`'s error branch (`EquateList.java:303-306`), the same way the
  sibling `*Logic` classes already do.

## Confirmed gaps - UI layer

### 3. Memory Inspector Delete/Cut/Paste Selection - not ported at all (deliberately, tracked as an open TODO)

- `MemoryInspectorPanel.java:102-115` documents the decision: the C++
  `MemoryInspector::DeleteSelection` never actually shrinks the segment's
  byte/type arrays (admitted in the C++ source's own comment), and
  `MemoryInspector::PasteAtSelection` is explicitly broken in C++ (the code
  that applies the paste buffer is commented out there). Porting either
  faithfully would just carry the brokenness forward.
- Fixing this for real needs actual segment-buffer resizing support, which
  `com.wudsn.tools.dis6502.model.MemoryBlock` doesn't have yet - this is a
  model-layer prerequisite for a UI-layer feature.
- Status: open TODO, not a closed/reviewed decision - worth scoping
  explicitly (per the porting guide's "decide feature scope per subsystem up
  front" rule) rather than leaving it as a standing comment indefinitely.

### 4. `LogPanel` is a plain `JTextArea`, not the C++'s multi-column, severity-colored list view

- `LogListWindow.cpp`'s C++ log window is a real list view: one row per log
  entry, columns, and severity-based coloring.
- `LogPanel.java`'s own javadoc says explicitly: "simplified... for this
  first pass - the C++ version's per-entry severity coloring/columns are not
  ported yet." It's currently a single scrolling `JTextArea`.
- Impact is cosmetic/usability (harder to scan for errors/warnings at a
  glance among info messages) rather than a functional loss - all messages
  are still logged and visible.

### 5. No Java equivalent of `MainUITest.cpp`'s interactive self-test harness

- C++ has a command-line self-test mode (`/TEST:DEEP`, `/TEST:FAST`),
  invoked by `build\test-dis6502-DEEP.bat` / `test-dis6502-FAST.bat`, that
  drives the UI programmatically in a loop.
- No Java class resembles this (no `/TEST:` argument handling, nothing under
  `test/` that mirrors an interactive/console self-test loop).
- The Java port does have a real JUnit suite, which is arguably a better
  substitute for regression coverage - but the specific interactive
  self-test-loop UX (useful for manual soak-testing against real program
  data while iterating) is absent. Worth an explicit "won't port, JUnit
  supersedes it" decision if that's the intent, rather than leaving it
  implicit.

### 6. `DiskImageSectorsDialog`'s byte-range picker is Start/End offset fields, not a drag-selectable hex view

- The C++ dialog lets you drag-select a byte range directly in a hex-dump
  view (built on `MemoryInspectorControl`).
- The Java `DiskImageSectorsDialog.java:50` javadoc notes this depends on a
  reusable, generic version of the memory inspector's grid control that
  isn't ported yet (distinct from `MemoryInspectorGridPanel`, which exists
  but is specific to the main Memory Inspector panel, not reusable as a
  generic embeddable control). The dialog instead uses explicit numeric
  Start/End Offset fields.
- Sector navigation also uses a `JSpinner` instead of a native scrollbar -
  likely UX-equivalent, low risk, not called out as a functional gap.

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

## Explicitly unverified - needs a follow-up pass

These were not checked deeply enough to classify as either "fine" or "a
gap" - flag them for a dedicated follow-up rather than assuming either way:

- **Opcode/addressing-mode table parity** between C++'s `InstructionSet.cpp`
  and Java's `InstructionSetMOS6502.java`/`InstructionSetMOS65C02.java` -
  only confirmed the classes exist and are split sensibly; the actual opcode
  tables were not diffed entry-by-entry. Given this is the disassembler's
  core correctness surface, this is probably the highest-value follow-up.
- **`Workspace1X`/`Profile1X`** (legacy-format loaders) - classes exist with
  plausible content, but not every legacy field/quirk C++ handles was
  verified as preserved.
- **`ImgError`/`ImgInfo`/`ImgRWPacket`/`AtariError`/`AtariFile`** - these
  Java classes have no like-named C++ header; they appear to be the Java
  port's own decomposition of `AtariDiskImage`/`AtariDOS`'s C-style
  output-parameter structs into proper value objects (an architecture
  improvement), but 1:1 field/error-code parity against the C++
  enums/structs was not verified.
- **`Application.h`/`ApplicationSettingsSection.h`** (the abstract C++ app
  class: settings-file sections, `SendInfoMessage`/`SendErrorMessage`/
  `ThrowErrorMessage` with Text IDs) has no `model`-package Java file, but an
  `application.sendInfoMessage(...)`/`sendErrorMessage(...)` abstraction
  clearly exists and is actively used (see gap #2 above) - it must live in
  the `ui` package. Full parity (settings persistence, every message
  variant) was not verified.
- **`OperatingSystem.h`** (`ExecuteCommand` - shell/process execution) has no
  Java model counterpart; likely superseded by `java.awt.Desktop`/
  `ProcessBuilder` somewhere in `ui`, not verified.
- **`DiskImageExecutableFileDialog`** and several of the "fully ported"
  dialogs listed above were confirmed present and non-trivial in size/content
  but not verified command-by-command against the C++ `.rc` resource /
  `ProcessCommand` switch.
- **`MainWindowMenu.cpp`'s per-item icon loading** (`AddIconToMenu`, 26
  call sites) appears present in `MainMenu.java` (29 `setIcon`/`Icon(`
  occurrences) but wasn't spot-verified icon-by-icon.

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

## Suggested priority order for closing these

1. **Gap #1** (`DisassemblyWriter` return character) - small, mechanical,
   high-impact correctness bug for non-Atari systems; the fix is already
   spelled out by the C++ one-liner it's missing.
2. **Gap #2** (`EquateList` swallowed parse errors) - small, mechanical, the
   logging plumbing it needs already exists elsewhere in the codebase.
3. **Opcode table parity check** (unverified item above) - core correctness
   surface; worth a dedicated diff pass even though no discrepancy is known
   yet.
4. **Gap #6** (`DiskImageSectorsDialog` drag-select) - needs the reusable
   generic hex-grid control extracted first; larger effort.
5. **Gap #3** (Memory Inspector Delete/Cut/Paste Selection) - explicitly
   blocked on `MemoryBlock` gaining real resize support; needs a scoping
   decision (faithful-but-broken port vs. a fixed reimplementation) before
   any code is written, per the porting guide's process-lesson rule.
6. **Gap #4** (`LogPanel` columns/coloring) - cosmetic, low risk, low
   urgency.
7. **Gap #5** (`MainUITest.cpp` self-test harness) - needs an explicit
   "superseded by JUnit, won't port" decision recorded somewhere (this
   document or a class javadoc) rather than staying an implicit gap.
