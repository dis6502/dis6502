# Further Improvements

Every working C++ feature found in both audits
([`REMAINING_GAPS_OVERVIEW.md`](REMAINING_GAPS_OVERVIEW.md),
[`FINAL_GAP_ANALYSIS.md`](FINAL_GAP_ANALYSIS.md)) has a Java counterpart or a
recorded decision not to port it. This is what is left, roughly in the order
it is worth doing. Items that get done are removed from this file; git
history has their write-ups.

## 1. Not yet verified

- `C64.equ`'s 455 labels have not been proofread against a reference. It
  was written from memory of "Mapping the Commodore 64"; the parser and MADS
  accept every line, and the labels exercised in tests are right.

## 2. Localization is prepared but not finished

`Texts`, `Messages`, `Actions`, `DataTypes` and the six value sets
(`Encoding`, `FolderType`, `FileType`, `ComputerSystemType`,
`ProcessorType`, `GraphicMode`) read every user-visible text from properties
files, but there is no translated properties file at all yet (no
`*_de.properties`), so the localizable structure is not yet used.

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

- There is no Oric or Atari 5200 fixture with a reassembly check, and no
  test for the Default Folders save/reload per computer system.

## 5. Housekeeping

- `plans/PORTING_GUIDE.md` still describes the fidelity-era process. It
  could shrink to build/test instructions plus the conventions that still
  apply.

## 6. Improvements to consider

- A review of `Oric.equ` (only 36 labels).
- Persisting the window size and splitter positions (C++ does not do this
  either).
- `ProfileLogic.loadDefaultProfile` (reopen the last-used profile) is ported
  but never called - identical in C++, where `LoadDefaultProfile` has no
  caller either. Possibly a feature worth finishing.
- Disassembly Add/Edit Comment uses the clicked line's own offset/size
  instead of snapping to the enclosing instruction
  (`findOffsetAtStartOfInstruction` is ported but not wired) - differs only
  for label/equate-only lines.
- Plain Home/End in the two grids: C++ scrolls to top/bottom; Java inherits
  `JScrollPane`'s Ctrl+Home/Ctrl+End.
