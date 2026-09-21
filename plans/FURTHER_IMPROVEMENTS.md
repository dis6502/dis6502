# Further Improvements (2026-09-22)

Every working C++ feature found in both audits
([`REMAINING_GAPS_OVERVIEW.md`](REMAINING_GAPS_OVERVIEW.md),
[`FINAL_GAP_ANALYSIS.md`](FINAL_GAP_ANALYSIS.md)) now has a Java counterpart
or a recorded decision not to port it. This is what is left: verification
and improvement, roughly in the order they are worth doing.

## 1. Not yet verified on screen

The recent features were tested by calling the application's code directly
(reflection into the running `Dis6502` instance), because clicking on the
shared development desktop was unreliable. Nobody has yet clicked through:

- the two new disassembly popup entries: Navigate Back to Previous Position
  (Backspace) and the "Change type of immediate byte to" submenu with its
  check marks;
- a real drag from the Windows Explorer onto the window (the drop handler
  was fed a file list directly);
- the overwrite confirmation and an actual save through the new
  `FileChoosers`;
- the Default Folders dialog saving on OK, and the per-system switch of
  default folders.

The live checks ran the real application, so the Recent Files and Recent
Workspaces lists contain test entries (`HelloWorld.prg`, `autorun.xex`,
`autorun.wrk`, a junk `.dat` file) that may have pushed out real ones.

`C64.equ` was written from memory of "Mapping the Commodore 64". The parser
and MADS accept every line, and the labels exercised in tests are right, but
nobody has proofread all 455 labels against a reference.

## 2. Localization is prepared but not finished

`Texts`, `Messages`, `Actions`, `DataTypes` and the six value sets
(`Encoding`, `FolderType`, `FileType`, `ComputerSystemType`,
`ProcessorType`, `GraphicMode`) read their texts from properties files, but:

- about 15 hard-coded English UI strings remain, e.g. the "Find"/"Find Next"
  buttons in `DisassemblyPanel`, "Load Profile..."/"Save Profile..." and the
  three group titles in `ProfileDialog`, "Scope" in
  `MemoryInspectorFindStringDialog`, "Disassembling - Please wait..." in
  `DisassemblyProgressDialog`, the hint line in `AssembleDialog`, one label
  in `DiskImageSectorsDialog` (find them with a grep for `new JButton("`,
  `new JLabel("`, `createTitledBorder("`);
- there is no translated properties file at all yet (no `*_de.properties`),
  so the localizable structure is not yet used.

## 3. Known limits shared with C++

Not port gaps - identical in C++ - but limits a user will meet:

- Atari 5200 bank-switched cartridges and `.CAR` files are not supported.
- SpartaDOS X relocation with base address `$0000` does not work (test
  `unit005`, disabled in C++ as well).
- The very old `DIS6502WRK10` workspace format cannot be loaded (`WRK14`
  can) - see `Workspace1X`'s javadoc for why.
- C64 `.prg` detection is deliberately permissive: any load address from
  `$0200` that fits into memory counts.
- 37 `TODO` comments remain in the source, mostly questions carried over
  from the C++ author ("Why?" in `Disassembly`, "Are these really base
  addresses on Oric?" in `Oric`).

## 4. Test coverage gaps

- `ReassemblyRoundTripTest` runs one notation variant per fixture; C++
  sweeps 16 (`ExecuteVariant`).
- The live checks are scratch programs in `C:\TEMP\claude\dis6502-smoketest`
  and not part of the suite. The reflection harness they use could become a
  real "UI smoke" test in `TestRunner`.
- There is no Oric or Atari 5200 fixture with a reassembly check, and no
  test for the Default Folders save/reload per computer system.

## 5. Housekeeping

- There is no `.gitattributes`. Every commit warns about LF versus CRLF, and
  one mechanical edit silently rewrote the line endings of eight files
  (restored before committing). A single `* text=auto` rule, or `eol=crlf`
  for `*.java`, would end that.
- `plans/REMAINING_GAPS_OVERVIEW.md` still has an "Explicitly unverified"
  section from the first audit; the second audit has since checked most of
  those items. Fold it into `FINAL_GAP_ANALYSIS.md` or retire it.
- `plans/PORTING_GUIDE.md` still describes the fidelity-era process. It
  could shrink to build/test instructions plus the conventions that still
  apply.

## 6. Improvements to consider

- An "Open File..." that lists all supported files and guesses the type -
  the `ANY_FILE` path of `Dis6502.openFile` already does the work.
- A review of `Oric.equ` (only 36 labels).
- Persisting the window size and splitter positions (C++ does not do this
  either).

## Suggested order

1. The on-screen pass (section 1) and cleaning the Recent lists.
2. The remaining hard-coded strings (section 2).
3. `.gitattributes` (section 5).

The rest is optional.
