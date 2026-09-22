# Rules: where computer-system-specific code lives

These rules govern every computer system this port supports today (Atari
800, Atari 5200, C64, Oric, the fallback Unknown system) and any system
added in the future. They mirror the C++ source's own `systems/<name>/`
folder layout - see the rationale section at the bottom for why, and for
what does *not* follow C++'s layout.

## The package layout

`com.wudsn.tools.dis6502.model.system` holds the shared infrastructure -
exactly the three classes the C++ `systems/` folder itself holds directly,
not inside a per-system subfolder:

- `ComputerSystem` (abstract base)
- `ComputerSystemFactory`
- `ComputerSystemType`

One subpackage per concrete system, `com.wudsn.tools.dis6502.model.system.<name>`
(lower-case system name), holds everything specific to that system and
nothing else:

- `com.wudsn.tools.dis6502.model.system.atari5200` - `Atari5200`, `Atari5200.equ`
- `com.wudsn.tools.dis6502.model.system.atari800` - `Atari800`, `Atari800.equ`,
  plus the Atari DOS 2.x disk-image subsystem (`AtariDOS`, `AtariDisk`,
  `AtariError`, `AtariFile`, `DiskImage`, `ImgInfo`, `ImgError`,
  `ImgRWPacket`) - Atari800-only in this application, so it lives here even
  though most of those class names don't say "800"
- `com.wudsn.tools.dis6502.model.system.c64` - `C64`, `C64.equ`
- `com.wudsn.tools.dis6502.model.system.oric` - `Oric`, `Oric.equ`
- `com.wudsn.tools.dis6502.model.system.unknown` - `Unknown` (no `.equ`:
  the unknown system has no equates)

`test/.../model/system/` and `test/.../model/system/<name>/` mirror this
exactly: `ComputerSystemTest` in the shared package, `Atari800Test`/
`AtariDiskImageTest` inside `atari800`.

## What stays in `model/`, not in a system subpackage

A class belongs in the shared `model/` package, not any `model.system*`
package, if it is used across every system or if the `Segment`/`Workspace`
layer owns it regardless of which system a segment came from - even if
some of its values are system-tagged. `FileHeader` is the clearest
example: `RAW` covers C64/Atari 5200/ROM images, `ATARI_BINARY`/`SDX_*`
cover Atari800 SpartaDOS X, `ORIC_BINARY` covers Oric, all in one enum
every segment uses regardless of system. `FileType`/`FolderType` (generic
value sets every system's `supportedFileTypes` draws from) and everything
else - `Segment*`, `Workspace*`, `Disassembly*`, `Equate*`,
`MemoryBlock`/`MemoryType`, etc. - follow the same rule.

## `ui/` stays flat

No computer system gets its own `ui` subpackage, even where a dialog is
only meaningful for one system in practice - the three Atari800-only
disk-image dialogs (`DiskImageSectorsDialog`, `DiskImageExecutableFileDialog`,
`SegmentWriteBootDiskDialog`) stay directly in `ui/`. This matches the C++
source, whose own `ui/` folder has no per-system subfolders at all.
`ComputerFont` also stays a single shared class holding every system's
small glyph-mapping table itself, rather than being split per system -
this was already a deliberate departure from C++ (which keeps a `.fon`
file per system folder) made earlier in the port, for reasons unrelated to
this package structure; do not undo it to force a per-system split here.

## Resource files

Each system's `.equ` file (and any future per-system resource) is a
classpath resource sitting directly next to its owning class, in that
system's subpackage - e.g. `model/system/atari800/Atari800.equ` next to
`Atari800.class`. `ComputerSystem#openResourceByExtension` resolves it via
`getClass().getResourceAsStream(...)` - relative to the *concrete*
subclass, not `ComputerSystem` itself - so this works automatically for
any new system as long as its resource file sits in the same package as
its class and is named `<ComputerSystemType.getFileName()><extension>`.
No `pom.xml` change is ever needed for this: the whole `src` tree is one
combined source/resource root, so a non-`.java` file is picked up as a
classpath resource wherever it sits.

## Adding a new computer system

1. Add a new `ComputerSystemType` constant (`model/system/ComputerSystemType.java`)
   with its file-name base.
2. Create `com.wudsn.tools.dis6502.model.system.<name>`, and put the new
   `ComputerSystem` subclass there, plus its `.equ` file (if it has one)
   named `<fileName>.equ` in the same package.
3. If the system needs its own supporting classes that nothing else
   touches (a disk/tape image format, a DOS-like file system, etc.), put
   them in the same subpackage too, regardless of what they're named -
   see Atari800's disk-image subsystem above for the precedent.
4. Register the new type in `ComputerSystemFactory`.
5. Add a mirroring `test/.../model/system/<name>/` test package if the
   system gets its own test class, following `Atari800Test`'s shape.
6. Leave `ui/` alone unless the system needs a UI dialog no other system
   would ever use *and* the project decides to depart from C++'s flat
   `ui/` convention deliberately - that is a real design fork, not a
   mechanical consequence of adding a system, and is worth asking about
   explicitly rather than deciding silently.

## Rationale

The C++ source keeps every computer-system-specific file inside its own
subfolder under `systems/`:

```
systems/
  ComputerSystem.h/.cpp
  ComputerSystemFactory.h/.cpp
  ComputerSystemType.h/.cpp
  ComputerSystemTest.h/.cpp
  atari5200/
    Atari5200.h/.cpp/.equ/.fon
  atari800/
    Atari800.h/.cpp/.equ/.fon, Atari800Test.h/.cpp
    AtariDOS.h/.cpp
    AtariDiskImage.h/.cpp
    AtariDiskImageTest.h/.cpp
    DiskImageFileInputStream.h/.cpp
  c64/
    C64.h/.cpp/.equ/.fon
  oric/
    Oric.h/.cpp/.equ/.fon
  unknown/
    Unknown.h/.cpp/.fon
```

The Java port mirrors this at the `model/` layer so a reader can tell at a
glance which classes are shared model infrastructure and which belong to
one specific computer system - but deliberately not everywhere C++ does:
`ui/` and `ComputerFont`'s font resources stay flat/shared (see above),
since porting-phase-over means matching C++ is no longer the default and
each of those already had its own, unrelated reason to diverge. Package
name is `model.system` (singular), matching this codebase's existing
singular-subpackage precedent (`model.version22`) over C++'s literal
plural folder name.

This structure was implemented in full on 2026-09-23, verified with a
clean `mvn compile`/`test-compile` and a full green `TestRunner` run
(27/27); see that commit for the mechanics (import fixups, the
`ComputerSystem#openResourceByExtension` change, etc.) if useful, but
those mechanics are not repeated here since this document's job is to
state the rule going forward, not narrate how it was first reached.
