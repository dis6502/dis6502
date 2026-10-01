# Proposal: x64 and ARM64 release builds, following RASTER-Music-Tracker

**Status: done 2026-10-02.** A manual test build (run 36935301633) built
all six images with the expected names.
Icon decision: (b) below - new multi-size icons built from the existing
PNGs, nothing redrawn.

## What RMT did (2026-10-01)

`RASTER-Music-Tracker/.github/workflows/release.yml` went from three to
six builds - every operating system on x64 and on ARM64 - with assets
named `rmt-java-<platform>-<arch>`. The reasons, from its `NOTES.md`:

- jpackage bundles the runner's own Java runtime and cannot cross-compile,
  so an app image runs only on the architecture it was built on. Each
  architecture needs its own runner.
- `macos-latest` has been Apple Silicon since 2024, so the "macOS" asset
  was ARM-only while its name said nothing.
- `upload-artifact` v4 fails when two parallel jobs upload under one name,
  so the artifact name must contain the architecture.
- Temurin 21 exists for all six combinations, and the runner labels were
  checked against the current runner images: `macos-13` is retired and
  `macos-14` deprecated, so the Intel image is `macos-15-intel`.

Its last run (36916509431) built all six and published the release.

RMT also passes `--description`, `--vendor` and a per-platform `--icon` to
jpackage, and stages user content (songs, instruments) next to the
application - the latter does not apply to dis6502, which ships no such
content.

## Where dis6502 stands

`.github/workflows/release.yml` builds three images on `windows-latest`,
`ubuntu-latest` and `macos-latest`. The macOS asset is named
`dis6502-macos-x64.tar.gz` but is built on Apple Silicon, so it is
actually ARM64 and does not run natively on an Intel Mac - the same
mislabel RMT fixed. There is no application icon, description or vendor.

## Proposed changes

1. **Matrix of six**, the same runners as RMT, each entry with an `arch`
   key:

   | Platform | Arch | Runner |
   |---|---|---|
   | windows | x64 | `windows-latest` |
   | windows | aarch64 | `windows-11-arm` |
   | linux | x64 | `ubuntu-latest` |
   | linux | aarch64 | `ubuntu-24.04-arm` |
   | macos | x64 | `macos-15-intel` |
   | macos | aarch64 | `macos-latest` |

   The ARM runners are free for public repositories; `dis6502/dis6502` is
   public.
2. **Asset names `dis6502-<platform>-<arch>`** (`.zip` for Windows,
   `.tar.gz` otherwise), and the same name for the uploaded artifact. The
   x64 Windows and Linux names stay as they are today; the current
   `dis6502-macos-x64` is renamed to what it really is,
   `dis6502-macos-aarch64`, and a real `dis6502-macos-x64` is added.
3. **jpackage metadata**: `--description "DIS6502 - The Interactive MOS 6502
   Disassembler"` and `--vendor "wudsn.com"`, as in RMT.
4. **Header comment** listing the runner table and the reasons above, the
   way RMT's does, so the next runner-image change is easy to check.

Unchanged: the `-Ddis6502.skipUITests=true` build flag, the tag and
`release_tag` triggers, the release job, and the macOS archive holding
`dis6502.app` at its top level.

When `release_tag` republishes an existing release, the old
`dis6502-macos-x64` asset there would stay next to the new ones; it should
be deleted from that release by hand once.

## Application icon (decided: built from the existing PNGs)

jpackage needs a `.ico` on Windows, accepts a `.png` on Linux and needs an
`.icns` on macOS. dis6502 had only `src/images/main-32x32.png`/`-48x48`/
`-64x64` (the main window's icon). The C++ program's `src/dis6502.ico` is
the same picture, but a single 32x32 image with 16 colors, which Windows
would scale up blockily.

`build/make-icons.py` repackages the three PNGs unchanged into
`build/icons/dis6502.ico` (32/48/64, PNG-compressed entries) and
`build/icons/dis6502.icns` (32 and 64 - icns has no PNG type for 48);
both files are committed and marked binary in `.gitattributes`. Linux
takes `main-64x64.png` directly. Rerun the script after changing the
PNGs. Windows' own icon API loads all three `.ico` sizes.

## Verification

Only a workflow run proves the new runners: a manual run with an empty
`release_tag` (a test build, nothing published), checking that all six
jobs succeed and each archive's runtime matches its architecture.
