# Final Gap Analysis: C++ dis6502 vs. Java port (2026-09-21)

Second and final audit of working C++ features that were missing in the
Java port, done before retiring the C++ version. It complements
[`REMAINING_GAPS_OVERVIEW.md`](REMAINING_GAPS_OVERVIEW.md) (the first audit).

**Status**: every gap this audit found is closed (2026-09-21/22) and, per
this project's convention, removed from this file - git history has each
one's write-up. For the record, they were: A system equates never loaded
(and, with it, a genuine `C64.equ`, the C64 stubs shared with C++, and the
disassembler omitting symbolic offset labels like `ICCOM`); B every open
resetting the computer system to Atari 800; C the command line; D drag and
drop; E Navigate Back to Previous Position; F the immediate-type submenu; G
default folders, per-type chooser filters and the overwrite prompt; H File
menu gating; I Recent Files bypassing the per-type dialogs. What remains
worth doing is in [`FURTHER_IMPROVEMENTS.md`](FURTHER_IMPROVEMENTS.md).

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

## Verified clean in this pass (nothing to do)

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

## Not gaps - deliberate or shared with C++

- **"Paste (insert after selection)"** - dead code in C++ (see gap #3's
  history in `REMAINING_GAPS_OVERVIEW.md`).
- **`Workspace1X.Load10`** (`DIS6502WRK10`) - deliberately not ported; the
  C++ author's own TODOs doubt it ever worked and no fixture exists.
- **`Save14`, `AboutDialog`'s module version list, `/DEBUG`, the
  `hPrevInstance` single-instance check** (always null on Win32, so dead) -
  Win32-specific or obsolete.
