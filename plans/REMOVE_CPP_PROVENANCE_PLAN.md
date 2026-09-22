# Plan: remove "Ported from" and other C++-provenance references from the code

**Status: done, 2026-09-23.** Every file under `src/**/*.java` and
`test/**/*.java` is clean of "C++"/"Ported from" references (literal and
case-insensitive, including embedded `Class::Method` citations with no
trigger word), the "not ported"/"New (not ported)" classification
framing, and `IDS_`/`IDM_` breadcrumbs - verified with a final whole-tree
sweep and a clean `mvn -o compile`/`test-compile` plus a full green
`TestRunner` run (27/27) on the final state. Scope stayed as proposed
below (`plans/*.md`, `README.md`, `CLAUDE.md` excluded); Tier 3 got the
full rewrite-in-place treatment throughout, not a lighter touch. What
follows is the original plan, left as-is for the tier rules and rationale
it captures - still the reference for the same kind of cleanup in a
future file.

## Goal

The porting phase is over (`plans/MEMORY.md`'s top section, `porting-phase-over.md`):
this Java codebase is the final, standalone product, and C++ is retired. Its
source still explains itself largely in terms of a C++ project a future
reader will never see: "Ported from X.h / X.cpp", "Ported from
Class::Method", "the C++ version does Y", "unlike C++, ...". This plan
removes that framing so each file explains what the Java code does and why,
on its own terms - without deleting the real design rationale that some of
those sentences carry alongside the C++ mention.

## Scope

**In scope**: `src/**/*.java` and `test/**/*.java` - javadoc and inline
comments only. No behavior changes; nothing here touches a method body,
only what surrounds it. A clean `mvn -o compile`/`test-compile` and a green
`TestRunner` run (or `mvn test`, now that it runs the suite) are the
acceptance bar for every batch.

**Proposed out of scope, for confirmation**:
- `plans/*.md` - these are deliberately a historical record of the port
  (`REMAINING_GAPS_OVERVIEW.md`, `FINAL_GAP_ANALYSIS.md`,
  `ACTIONS_ELEMENT_FACTORY_MIGRATION.md`, `POPUP_MENU_ACCELERATORS_PLAN.md`,
  `PORTING_GUIDE.md`, `MEMORY.md`, `TEXT_INDEX.md`, `DIS6502_WIN32_TODOS.txt`)
  - their entire point is documenting C++-vs-Java differences and decisions,
    for exactly the kind of future reader who'd ask "why does this Java code
    do X" and finds the answer in git history or these files. Stripping C++
    mentions from them would gut their purpose. `plans/PORTING_GUIDE.md`
    shrinking to just build/test instructions is already a separate,
    already-tracked item in `FURTHER_IMPROVEMENTS.md`.
- `README.md`/`CLAUDE.md` - one and four mentions respectively, all
  legitimate ("ported from the C++ original", pointing at the source repo
  for anyone who does want the history). Low volume, different purpose from
  the in-code comments; leave as is unless asked.

## Scale

| | count |
|---|---|
| Java files with at least one reference | 142 of 159 |
| `"Ported from"` occurrences | 329 |
| `"C++"` mentions | 109 |
| `.cpp`/`.h` file-name citations | ~260 |
| `IDS_`/`IDM_`/`ID_`/`IDC_` Win32 resource ID citations | 139 |
| "not ported" / "unlike C++" / "matching C++" phrasings | ~300 |

This is not a small cleanup - it is a comment-rewriting pass over nearly
every file in the project, several hundred edit sites. `Dis6502.java` alone
has 92 `"Ported from"` occurrences in 2004 lines; `MemoryInspectorPanel`,
`ProfileDialog`, `Disassembly` and `GuessCodeLogic` are close behind. It is
sized for several sessions or several parallel subagents, not one turn.

## The four kinds of reference, and how each is handled

### Tier 1 - pure provenance, no other content

```java
/**
 * Ported from Comment.h / Comment.cpp.
 *
 * @author Peter Dell
 */
```

Delete the sentence outright. If it was the class/method's only description,
replace it with one plain sentence saying what the class/method actually
does (most of these already have that sentence right after the C++ one, so
often nothing needs adding). Mechanical; a scripted first pass (grep for the
exact `Ported from [A-Za-z0-9]+\.h / [A-Za-z0-9]+\.cpp\.` sentence pattern,
plus the method-level `Ported from Class::Method[.:]` pattern) can handle
most of this tier, with a spot-check that no file is left with an empty or
grammatically broken javadoc paragraph.

### Tier 2 - provenance fused into a sentence that has other content

```java
/**
 * A raw (headerless) file, via {@link RawFileDialog} for picking the byte
 * range and load address. Ported from MainFile::OpenRawFile: unlike
 * {@link #openReadableFile}, which routes through {@code
 * WorkspaceLogic.addFile}/{@code ComputerSystem.readFile}, this calls
 * {@code WorkspaceLogic.addRawSegment} directly, since a raw file has no
 * format for a {@link ComputerSystem} to parse.
 */
```

The great majority of these look like this example: the `Ported from
MainFile::OpenRawFile:` clause is a removable prefix, and the rest of the
sentence is already a same-codebase (Java-to-Java, `#openReadableFile` vs.
this method) comparison that reads correctly on its own. Strip the
provenance clause; keep the rest verbatim. Needs a human/agent read of each
occurrence (not safely scriptable, since the clause boundary varies), but is
usually a small, confident edit once read.

### Tier 3 - the C++ comparison *is* the substance

```java
// Unlike the C++ version, which C-style casts an uninitialized/unvalidated
// word into the FileHeader enum, this falls back to RAW if the attribute is
// missing or does not match a known header value.
```

```java
/**
 * Found and fixed a bug while porting {@link #save}: the C++ version's
 * non-XASM branch wrote each equate's {@code ToString()} one after another
 * with no separator, so every equate ended up concatenated onto a single
 * line instead of one per line - reloading such a file would then fail to
 * parse anything past the first equate...
 */
```

Here the comparison explains *why* the Java code is shaped the way it is -
losing it would leave a design decision unexplained. Rewrite to state the
Java-native rationale without the comparison: what the code does, and why,
in terms of Java's own behavior/inputs, dropping "unlike C++"/"the C++
version"/"found while porting" framing and any detail only meaningful
against the old code (upstream-fix cross-references, the old bug-handling
policy's "fixed there too" tail - that policy is itself retired per
`plans/MEMORY.md`). Example rewrite of the two above:

```java
// Falls back to RAW if the attribute is missing or does not match a known header value.
```

```java
/**
 * Writes one equate's {@code toString()} per line, since {@link #load}
 * splits strictly on newlines when reading them back.
 */
```

This is the tier that needs real editorial judgment, file by file. It is
also where most of the remaining value is - the actual reasoning belongs in
the code, just not framed as a diff against a project nobody will open
again.

### Tier 4 - `IDS_`/`IDM_` Windows resource ID breadcrumbs

```java
/** {@link Dis6502#run}'s startup warning message - moved from {@code Text.IDS_LOG_BETA_MESSAGE}. */
```

Concentrated in `Messages.java` (46), `Texts.java` (24) and scattered
elsewhere (139 `IDS_`/`IDM_`/`ID_`/`IDC_` citations total) - these are
traceability breadcrumbs from the already-completed `Text.java` retirement
(`plans/MEMORY.md`'s `text-repository-classes` note). They were already
judged obsolete once (the user rejected keeping `Text.java`'s
identity-tracing distinction); safe to delete outright, keeping only the
plain description before the "moved from" clause. Same treatment for
`dis6502.rc` menu/accelerator/mnemonic citations in `Actions.java`/
`MainMenu.java` (15+ sites) - reword any design rationale that currently
leans on "no `&` in `dis6502.rc`" etc. to state the current Java convention
instead (e.g. "no mnemonic - reused across every visible row simultaneously,
so a shared one would collide with itself").

## Execution strategy

Given the volume, propose doing this in per-package batches, each a
self-contained commit with its own build+test verification - not one
sweeping change:

1. `model/` (the largest package by file count, mostly Tier 1/3 - bug-fix
   rationale concentrated in `Segment`, `Workspace`, `WorkspaceLogic`,
   `MRUList`, `EquateListLogic`, `Memory`, `MemoryBlock`, `Disassembly`,
   `GuessCodeLogic`).
2. `ui/` (mostly Tier 1/2, high volume in `MemoryInspectorPanel`,
   `ProfileDialog`, `HexGridPanel`, `DisassemblyPanel`,
   `DisassemblyGridPanel`, `ComputerFont`).
3. Top-level package (`Dis6502.java` - the single densest file, `Actions`,
   `Messages`, `Texts`, `DataTypes` - the Tier 4 sweep).
4. `test/`.

Each batch: grep the tier-1 pattern and delete mechanically, then go
file-by-file for tier 2/3, rebuild and rerun the full suite, commit. Given
the size, this is a natural fit for parallel subagents (e.g. one per
package, or one per file cluster within `model/`/`ui/`) working from this
plan's tier rules, each reporting back files touched and anything it judged
ambiguous rather than silently dropping content - followed by a build/test
pass and a read-through of the diff before committing, the same way every
change this session has been verified.

## What must not be lost

The rewrite must preserve every piece of *behavioral* knowledge currently
riding on a C++ comparison, restated without the comparison - not just
delete the sentence. Concrete examples already in the codebase where this
matters:
- `AtariError`/`Segment`/`Workspace1X` field-layout comments verified
  byte-for-byte against a real fixture file.
- The printf-format state machine in `Segment.allocateSymbol`/
  `GuessCodeLogic`.
- The `DisassemblyLine.NO_SYSTEM_ADDRESS` sentinel's reason for existing
  (a system label can legitimately sit at address `$0000`).
- The ZP-as-byte hex-digit-count fix in `Disassembly.java` (already
  reworded once this session to explain itself without leaning on the C++
  diff - a model for the rest of this pass).
- Every "found and fixed a bug" note - the bug description and the fix
  reasoning stay; only the "and it's fixed upstream too"/cross-repository
  framing goes.

## Open questions

1. **Scope confirmation**: exclude `plans/*.md`, `README.md`, `CLAUDE.md`
   as proposed above?
2. **Tier 3 depth**: reword every substantive comparison (this plan's
   default), or is a lighter touch acceptable for the lowest-traffic
   classes (delete the C++ framing, accept a slightly less-explained
   comment) to control the total effort?
3. **Execution timing**: run this now across several turns/subagents, or
   queue it as a `plans/FURTHER_IMPROVEMENTS.md` backlog item done
   incrementally alongside other work?
4. **`@author Peter Dell`**: unaffected by this plan (not a C++ reference) -
   confirm that's correct and out of scope.
