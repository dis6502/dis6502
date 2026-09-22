# Further Improvements (2026-09-22)

Every working C++ feature found in both audits
([`REMAINING_GAPS_OVERVIEW.md`](REMAINING_GAPS_OVERVIEW.md),
[`FINAL_GAP_ANALYSIS.md`](FINAL_GAP_ANALYSIS.md)) now has a Java counterpart
or a recorded decision not to port it. This is what is left: verification
and improvement, roughly in the order they are worth doing.

## 1. ~~Not yet verified on screen~~ - done 2026-09-22

The recent features had only been tested by calling the application's code
directly. They have since been exercised in the real, painted windows (real
AWT events, `Robot` screenshots, both windows kept always-on-top so nothing
on the shared desktop could cover them):

- the disassembly popup with Navigate Back to Previous Position (disabled
  before a navigation, enabled after, returning to the `jsr` line when
  clicked) and the "Change type of immediate byte to" submenu with its check
  mark on the current type;
- a real drag and drop through the OS from a second window exporting a file
  list, as the Explorer does: the file was opened (6 segments);
- Save Workspace As over an existing file: the chooser, the Confirm
  Overwrite prompt, and a real 94 KB save after Yes;
- the Default Folders dialog: an edited folder was persisted on OK.

The test entries in the Recent lists and the development-only default
folders (`target/classes`) those runs persisted were removed afterwards;
the settings hold nothing from the tests now.

Still not proofread: `C64.equ`'s 455 labels against a reference (the parser
and MADS accept every line, and the labels exercised in tests are right).

## 2. Localization is prepared but not finished

`Texts`, `Messages`, `Actions`, `DataTypes` and the six value sets
(`Encoding`, `FolderType`, `FileType`, `ComputerSystemType`,
`ProcessorType`, `GraphicMode`) read their texts from properties files.

- ~~About 15 hard-coded English UI strings remain~~ - done 2026-09-22: the
  last ones (`DisassemblyPanel`'s Find/Find Next buttons, `ProfileDialog`'s
  Load/Save Profile buttons and three group titles, `AssembleDialog`'s hint,
  `DisassemblyProgressDialog`'s "Please wait", `DiskImageSectorsDialog`'s
  heading, `MemoryInspectorFindStringDialog`'s "Scope") are `Actions` and
  `Texts` entries now. What a grep for `"..."` in `ui/` still finds is not
  text: a URL, `"0"`, `"-"`, a generated equate line.
- There is no translated properties file at all yet (no `*_de.properties`),
  so the localizable structure is not yet used. That is the remaining step.

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

- ~~There is no `.gitattributes`~~ - done 2026-09-22: `* text=auto` (LF in
  the repository, which the index already was throughout; platform line
  endings in the working tree, which is what Eclipse writes), explicit
  `binary` for the fixtures and resources, `.wrk`/`.prf`/`.exported` left to
  detection since each exists in a text and a binary form. The working tree
  was re-checked-out once so every text file is CRLF; the per-commit
  warnings are gone.
- `plans/REMAINING_GAPS_OVERVIEW.md` still has an "Explicitly unverified"
  section from the first audit; the second audit has since checked most of
  those items. Fold it into `FINAL_GAP_ANALYSIS.md` or retire it.
- `plans/PORTING_GUIDE.md` still describes the fidelity-era process. It
  could shrink to build/test instructions plus the conventions that still
  apply.

## 6. Improvements to consider

- ~~An "Open File..." that lists all supported files and guesses the type~~ - done 2026-09-22 ("Open Any File..."/"Add Any File..." at the top of the Open File/Add File submenus, filter for every readable type plus `.wrk`, type guessed by `openFile`) -
  the `ANY_FILE` path of `Dis6502.openFile` already does the work. Move the standard 
- A review of `Oric.equ` (only 36 labels).
- Persisting the window size and splitter positions (C++ does not do this
  either).

## Suggested order

1. ~~The on-screen pass (section 1) and cleaning the Recent lists.~~ Done.
2. ~~The remaining hard-coded strings (section 2).~~ Done.
3. ~~`.gitattributes` (section 5).~~ Done.

The rest is optional.
