# Equates and label access

Status: Planned (2026-10-04) - every design question is decided, see
"Decisions".

Four of the source TODOs ([`SOURCE_TODOS.md`](SOURCE_TODOS.md)) concern which
equates a listing uses and writes. They overlap, so they are planned
together. Each finding below was checked with a small experiment on the
real Atari 800 system equates (listing lines as written with "omit
unreferenced system labels").

## Background

- **Label access:** every equate has an access - `=` read/write, `<` read,
  `>` write, `#` immediate (`LabelAccess`). A reference only resolves to an
  equate that supports its access, so `sta $02` finds `CASINI = $02`, while
  `ldx #$02` (an IOCB offset) finds `ICCOM # $02`.
- **Shared addresses:** `Atari800.equ` has 62 addresses with more than one
  label (mostly zero-page labels and `#` constants, e.g. `CASINI`/`ICCOM` at
  `$02`), `Atari5200.equ` 5, `C64.equ` and `Oric.equ` none.
- **Ranges:** Equates > Define Address Range creates one user equate per
  address, `BASE+n`, based on a system or user equate. Range equates are not
  written themselves; the listing relies on the base label being defined.

## Findings

### 1. A user range based on a system equate does not assemble (bug)

`EquateList.setBaseLabelsReferenced` (TODO "Consider parent equate list.
Also consider recursion.") marks the base of every referenced range label,
but looks the base up only in the range's own list. A user range based on a
system equate never marks it:

```
              org $2000
              sta COLPF0+1     ; User range COLPF0+1..+2 based on the system's COLPF0.
              rts
```

`COLPF0` is defined nowhere - the listing does not assemble. Also, the
marking runs once in list order, so a base that is itself a range label
(e.g. `CASINI+1` in `Atari800.equ`) only passes the mark on if it comes
later in the list.

### 2. Unreferenced system equates at a shared address are written (clutter)

`Disassembly.setSystemEquateLinesReferencedBySystemAddress` (TODO "the
referenced check does not distinguish the type of access") marks a system
equate line as referenced if *any* equate at its address was referenced:

```
ICCOM       equ $0002          ; Not used - written because CASINI shares $02.
CASINI      equ $0002
              org $2000
              sta CASINI
```

Harmless for assembling, but the listing contains labels nothing uses.

### 3. Range equates always get read/write access

`EquateList.addRange` (TODO "Add labelAccess.") creates every range equate
as `READ_WRITE`, whatever the base equate's access. A range based on a `#`
constant (e.g. an IOCB offset) therefore also matches memory accesses at
those addresses, and a range based on a read-only register matches writes.

### 4. SDX page 7 is hard-coded

`EquateList.findEquateByAddress` (TODO "We must mark the system equates in
a certain way instead of hardcoding page 7") never matches `$0700-$07FF`
outside SpartaDOS X segments, because `Atari800.equ` has SDX's own labels
there that are wrong for other programs. The generic `EquateList` thus
knows an Atari/SDX detail. And the rule is incomplete:

- **The SDX labels:** the section "SpartaDos X equates" at the end of
  `Atari800.equ` (line 841 on) holds 57 labels: 51 on page 7 (`S_FLAG`,
  `SDXVER`, `JKERNEL`, ...) and 6 jump vectors at `$FFC0-$FFD5` (`JGETTD`,
  `JSETTD`, `JTDON`, `JFMTTD`, `JXCOMLI`, `JKEYON`). The six are not on page
  7, so a normal program referencing the XL/XE OS ROM there gets SDX names.
- **Where `sdx` comes from:** it is not a setting. `Segment.isSDX()` is true
  for the five SDX block headers (`$FFFA-$FFFE`), which
  `Atari800.readExecutableFile` takes from the file and the workspace stores
  with the segment. `SegmentList` passes the disassembled segment's
  `isSDX()`; every user equate lookup passes `true`.
- **The same problem with the OS:** `Atari800.equ` holds the XL/XE OS
  variables ("OS VARIABLES FOR XL/XE"). Equates of OS-A/B (400/800) would
  collide with them at the same addresses, just as the SDX labels collide
  with normal page-7 use. SDX is one case of a general need: equates that
  apply only in a certain context.
- **The same constant in different contexts:** a `#` constant such as `$02`
  means `ICCOM` as an IOCB offset, but something else elsewhere - today the
  first matching equate in list order wins.
- **Feature request #60** (SourceForge, 2012, open): "Load multiple label
  equates files" - Atari OS, SpartaDOS, Happy 1050 firmware and project
  labels, all saved with the workspace. Contexts give those label sets their
  own namespaces, also within one file.

Unused on the way: `Workspace.findEquateByAddress(address, labelAccess)` has
no callers.

### Side finding

`LabelAccess.isSupported`'s javadoc says "all bits ... are set", but the
code checks for *any* common bit. The code is what the callers rely on
(`READ_WRITE` supports a `READ` reference); only the javadoc is wrong.

## Proposed changes

| # | Change | Effect on listings |
|---|---|---|
| 1 | `setBaseLabelsReferenced` looks a base label up in the range's own list, then in the system list (the "parent"), and repeats until nothing new is marked | Bases of user ranges on system equates are written - such listings assemble |
| 2 | Each system equate line knows its own `Equate`; it is referenced if that equate is (plus, as today, the nearest line for `LABEL+n`) | Fewer lines: no unused labels at shared addresses |
| 3 | `addRange` takes the base equate's access; `EquateRangeDialog` passes it | New ranges resolve only for the base's kind of access. Ranges in existing workspaces keep their stored access |
| 4 | Equate contexts instead of the hard-coded page, see "Equate contexts" | The six SDX vectors at `$FFC0-$FFD5` are no longer used outside SDX segments; otherwise unchanged. No Atari/SDX knowledge left in `EquateList` |
| - | Correct `LabelAccess.isSupported`'s javadoc; delete the unused `Workspace.findEquateByAddress` | - |

### Equate contexts

A **context** is a namespace for equates, a general concept for system and
user equates alike. Each equate belongs to **0 to n contexts**, given per
equate in its line; `Atari800.equ` stays one file. An equate without
contexts is in the global namespace and always applies; one with contexts
applies where at least one of them is **active**.

- **Syntax:** a bracketed list after the value, before the comment;
  several contexts comma-separated, spaces around names allowed:

  ```
  S_FLAG   =   $0700 [SDX]        ; SpartaDOS X kernel flag
  JGETTD   =   $FFC0 [SDX]
  ICCOM    #   $02   [IOCB]       ; Later: the IOCB offset constants
  CASINI   =   $02   [OS-AB, OS-XL]
  ```

  The parser reads the names into the equate (new parse errors for an
  unclosed bracket or an empty name, in `Messages`); equates without contexts
  are written and read exactly as today. Older DIS6502 versions and other
  tools reading `.equ` files reject lines with contexts. The workspace stores
  an equate's contexts with it.
- **Model:** `Equate.getContexts()` (a set of names, empty for the global
  namespace); `EquateList` lists all context names its equates use.
- **Active contexts** for an address, from three sources, merged:
  1. **The workspace:** a set of active contexts, chosen in the workspace
     dialog from the context names the loaded equates use, stored in the
     workspace. Default: none - only the global namespace, i.e. today's
     behavior until contexts are used.
  2. **The segment:** the computer system derives contexts from a segment -
     `Atari800` returns `SDX` for SDX segments (`Segment.isSDX()`), so
     `EquateList` and `SegmentList` stay free of SDX knowledge.
  3. **Later, per address:** a context selected for an address range, like
     a memory type - the step that lets the same constant translate
     differently in different places. The lookup is built so that this
     source can be added without changing it again.
- **Lookup:** `findEquateByAddress` takes the active contexts instead of the
  `sdx` flag. Among the equates at the address with a matching access, one
  of an active context comes before one of the global namespace; equates of
  inactive contexts are skipped. This preference is what later resolves
  the same constant per context.
- **Listing:** writes an equate only if it is global or in a context active
  somewhere in the listing - a listing never defines labels its program
  cannot use.
- **Data in this plan:** the 57 SDX equates in `Atari800.equ` get `[SDX]` -
  that replaces the hard-coded page 7. Contexts for the OS (OS-A/B vs.
  XL/XE), IOCB constants, Happy 1050 or other label sets are later data
  work on the same mechanism.

## Tests

- **One listing test per finding**, on the real Atari 800 system equates,
  like the experiments above: the user range's base is written (and the
  listing assembles with MADS on Windows, in `ReassemblyRoundTripTest`
  style); `ICCOM` is not written for `sta $02`; a range on a `#` constant
  does not match `sta`; the SDX labels (page 7 and `$FFC0-$FFD5`) appear
  only in SDX segments.
- **Contexts:** `EquateTest` parses and writes lines with 0, 1 and 2
  contexts; the workspace round trip keeps an equate's contexts and the
  active contexts; the lookup prefers an equate of an active context over a
  global one at the same address and skips one of an inactive context.
- **Fixtures:** the existing reassembly round trips must still pass. Where
  a fixture's reference listing changes (fewer unused labels, finding 2), the
  change is checked line by line and the reference updated.

## Steps

1. Findings 1 and 3 (`EquateList`, `EquateRangeDialog`) with tests.
2. Finding 2 (`DisassemblyLine`, `Disassembly`) with tests; update reference
   listings that lose unused labels.
3. Finding 4: equate contexts - syntax and parser, `Equate`/`EquateList`
   and workspace persistence, the context-aware lookup and listing, the
   segment contexts of `Atari800`, the active contexts in the workspace and
   its dialog, `[SDX]` on the 57 SDX equates; the javadoc fix and the unused
   method.
4. Later, not in this plan: contexts per address range, and context data
   for the OS, IOCB constants and further label sets.
5. `SOURCE_TODOS.md`, `FURTHER_IMPROVEMENTS.md` (TODO count), `CHANGES.md`
   (finding 1 is a fix: the logic came unchanged from 3.6.1).

## Decisions (2026-10-04)

1. **Contexts per equate**, 0 to n each, not per file: `Atari800.equ` stays
   one file.
2. **Contexts are a general namespacing concept** for all equates, not only
   for the OS or system equates. In the long run, the context for resolving
   equates becomes selectable per address, so the same constant can
   translate differently in different contexts (see feature request #60).
3. **Syntax:** a bracketed list after the value, several contexts
   comma-separated - `S_FLAG = $0700 [SDX]`.
4. **Active contexts of the workspace:** chosen in the workspace dialog from
   the context names of the loaded equates; default none.
5. **Ranges in existing workspaces** (finding 3) keep their stored
   `READ_WRITE` access; only new ranges take their base's access.
