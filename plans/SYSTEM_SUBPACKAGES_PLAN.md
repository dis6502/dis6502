# Plan: split computer-system-specific model classes into subpackages

**Status: done, 2026-09-23.** Implemented exactly as decided below: `model.system`
(singular), `.equ` resources moved next to their class, `ui/` left flat,
`test/` mirrors the split. Verified with a clean `mvn compile`/`test-compile`
and a full green `TestRunner` run (27/27) on the final state.

## Goal

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

The Java port currently keeps all of this flat inside
`com.wudsn.tools.dis6502.model` (plus a `model/systems/*.equ` resource
folder that isn't a Java package at all, just a place for the classpath
resources). This plan proposes splitting the per-system Java classes into
subpackages that mirror the C++ layout, so a reader can tell at a glance
which classes are shared model infrastructure and which belong to one
specific computer system.

## Proposed package layout

New package `com.wudsn.tools.dis6502.model.system` holds the shared
infrastructure - the exact three classes the C++ `systems/` folder itself
holds directly, not inside a per-system subfolder:

- `ComputerSystem.java` (abstract base)
- `ComputerSystemFactory.java`
- `ComputerSystemType.java`

Five subpackages, one per concrete system, hold everything specific to
that system:

- `com.wudsn.tools.dis6502.model.system.atari5200`
  - `Atari5200.java`
  - `Atari5200.equ` (resource, moved from `model/systems/`)
- `com.wudsn.tools.dis6502.model.system.atari800`
  - `Atari800.java`, `Atari800.equ`
  - `AtariDOS.java`, `AtariDisk.java`, `AtariError.java`, `AtariFile.java`
    (the DOS 2.x file-system driver - C++'s `AtariDOS.h/.cpp`, which lives
    inside `systems/atari800/` even though the class names don't say
    "800")
  - `DiskImage.java`, `ImgInfo.java`, `ImgError.java`, `ImgRWPacket.java`
    (the low-level `.atr`/`.xfd` sector reader/writer - C++'s
    `AtariDiskImage.h/.cpp`, also inside `systems/atari800/`; renamed to
    the shorter `DiskImage` earlier in this port)
- `com.wudsn.tools.dis6502.model.system.c64`
  - `C64.java`, `C64.equ`
- `com.wudsn.tools.dis6502.model.system.oric`
  - `Oric.java`, `Oric.equ`
- `com.wudsn.tools.dis6502.model.system.unknown`
  - `Unknown.java` (no `.equ` - the unknown system has none in C++ either)

Confirmed by checking every reference: `Atari5200.java`/`C64.java`/
`Oric.java` never touch `DiskImage`/`AtariDOS`/`AtariDisk` - disk-image
support really is Atari800-only in this application, exactly as the C++
folder layout implies.

### What stays in `model/` (not moved)

Classes used across every system, or that the `Segment`/`Workspace` layer
owns regardless of which system a segment came from, stay exactly where
they are - this matches C++ too: `FileHeader.h/.cpp`, for example, lives
at the top of `src/`, not inside `systems/`, even though some of its
constants (`ATARI_BINARY`, `ORIC_BINARY`) are system-tagged:

- `FileHeader.java` - `Segment`'s header-type enum; `RAW` covers C64/
  Atari 5200/ROM images, `ATARI_BINARY`/`SDX_*` cover Atari800 SpartaDOS X,
  `ORIC_BINARY` covers Oric, all in one enum used by every segment
  regardless of system.
- `FileType.java`, `FolderType.java` - generic value sets every system's
  `supportedFileTypes` draws from.
- Everything else: `Segment*`, `Workspace*`, `Disassembly*`, `Equate*`,
  `MemoryBlock`/`MemoryType`, etc.

## Cross-cutting changes this needs (not just moving files)

1. **Resource lookup.** `ComputerSystem.openResourceByExtension` currently
   resolves `"systems/" + computerSystemType.getFileName() + extension`
   relative to `ComputerSystem.class` - i.e. relative to the `model/`
   package, which is where both `ComputerSystem.class` and the
   `model/systems/*.equ` files live today. Once each `.equ` file moves to
   sit next to its own concrete class (e.g.
   `model/system/atari800/Atari800.equ`), this needs to resolve relative
   to the *concrete* class instead (`getClass().getResourceAsStream(...)`
   from an instance method, or a `Class<?>` parameter), and the resource
   name simplifies to just `<FileNameBase><extension>` since the package
   itself now plays the role the `"systems/"` prefix used to.
2. **Imports everywhere.** `ComputerSystemFactory` constructs all five
   concrete types directly; `Dis6502.java`, `Messages.java`,
   `WorkspaceLogic.java`, and three `ui/` dialogs
   (`DiskImageSectorsDialog`, `DiskImageExecutableFileDialog`,
   `SegmentWriteBootDiskDialog`) reference `DiskImage`/`AtariDOS`/
   `AtariDisk`/`AtariError`/`AtariFile`/`ImgInfo`/`ImgRWPacket` directly.
   All of these need new `import` lines; nothing needs a behavior change.
3. **Visibility check.** Spot-checked the model classes the concrete
   systems call into most (`Segment`, `SegmentList`, `SegmentListInserter`,
   `Workspace`) for package-private members that only work today because
   `Atari800`/`C64`/`Oric`/etc. sit in the same package - found none;
   everything they use is `public` or `protected`. Worth a full grep
   sweep during implementation rather than trusting this spot check alone.
4. **Test mirror.** `test/.../model/ComputerSystemTest.java`,
   `Atari800Test.java`, and `AtariDiskImageTest.java` would move to
   `test/.../model/system/` and `test/.../model/system/atari800/` to
   match, and `TestRunner.java`'s imports update accordingly.
5. **Maven.** No `pom.xml` change needed - `<sourceDirectory>src</sourceDirectory>`
   already treats the whole tree as one root, and non-`.java` files (the
   `.equ` resources) are already picked up as classpath resources
   wherever they sit in it.

## Explicitly out of scope for this plan

- **`ui/`'s three Atari-disk-image dialogs** (`DiskImageSectorsDialog`,
  `DiskImageExecutableFileDialog`, `SegmentWriteBootDiskDialog`) are
  Atari800-only in practice, but C++'s own `ui/` folder is completely
  flat - no per-system subfolders at all, for these or anything else - so
  this plan leaves `ui/` untouched by default. See the open question
  below.
- **`ComputerFont`'s TTF resources** (`ui/fonts/AtariClassic-Regular.ttf`,
  `ui/fonts/C64Classic-Regular.ttf`). C++ keeps a `.fon` file inside each
  system's own `systems/<name>/` folder, but this port already made a
  deliberate, unrelated departure here: one shared `ComputerFont` class
  loads both TTFs and holds each system's small glyph-mapping table
  itself, rather than being split per system at all. Reorganizing this
  would mean undoing that design, not just moving files, so it is left
  alone unless asked for separately.

## Decisions

1. **Package name: `model.system`** (singular), matching this codebase's
   existing singular-subpackage precedent (`model.version22`) over C++'s
   literal plural folder name.
2. **`.equ` resources move next to their class**, one per subpackage
   (e.g. `model/system/atari800/Atari800.equ`) - the resource-lookup code
   change described above is in scope.
3. **`ui/` stays flat.** The three Atari800-only disk-image dialogs
   (`DiskImageSectorsDialog`, `DiskImageExecutableFileDialog`,
   `SegmentWriteBootDiskDialog`) are not moved, matching C++'s own
   unsplit `ui/` folder.
4. **`test/` mirrors the split.** `ComputerSystemTest.java` moves to
   `test/.../model/system/`; `Atari800Test.java`/`AtariDiskImageTest.java`
   move to `test/.../model/system/atari800/`.
