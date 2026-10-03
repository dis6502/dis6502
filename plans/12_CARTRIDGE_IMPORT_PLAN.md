# Importing every Atari 800 and Atari 5200 cartridge type

Status: Done (2026-10-04) - all six steps.

## Goal

`Atari800.readROMFile` reads only plain 4, 8 and 16 KB images (raw or with
a `CART` header), and `Atari5200.readROMFile` reads only raw images of up
to 32 KB. A new class shall import every cartridge type that WUDSN Base's
`CartridgeType` (`com.wudsn.tools.base.atari`, already on the classpath)
defines for the respective system, so both `readROMFile` methods become a
call to it.

## Current state

- **Detection:** `ComputerSystem.guessFileType(File)` reads only the first
  4 bytes, so the type number in a `CART` header (bytes 4-7) is never seen.
  Both systems decide by file size alone.
- **Existing bugs:**
  - Atari 800 recognizes a 4 KB `.car` file (`SIZE_4K_CAR`) in
    `guessFileType`, but `readROMFile` skips the header only for 8 and 16
    KB, so a 4 KB `.car` fails with E055.
  - Atari 5200 recognizes a 40 KB raw file (`0xA000`) in `guessFileType`,
    but `readROMFile` rejects everything above 32 KB with E050.
- **Trailer typing:** Atari 800 types the last 6 bytes of the ROM
  (run address, flags, init address); that is the open TODO at
  `Atari800.java:463`. Atari 5200 types the 20-byte title and the two
  vectors at the end and sets `Segment.title` from the title.
- **What `CartridgeType` provides per type:** numeric id (= `CART` header
  type), `Platform` (`ATARI_800`/`ATARI_5200`), file size in KB, bank
  size, and the initial bank's file offset, CPU address and number, plus a
  display name in `ValueSets.properties`
  (e.g. "OSS two chip 16 KB (034M)").
- **What it does not provide:** where the *other* banks appear in memory.
  For most types that is the same window as the initial bank, but not for
  all of them. For XEGS the last bank is fixed at `$A000` and the others
  switch at `$8000`, and Bounty Bob has two 4 KB windows plus a fixed 8 KB
  bank. The Atrax 48, 49 and 68 images store address and data bits
  interleaved, and AST 32 shows its ROM through a 256-byte window. This
  information has to come from atari800's `DOC/cart.txt`, the reference
  `CartridgeType` itself cites, and live in the new class (see "Bank
  layout").

## Redesign (2026-10-04): metadata in CartridgeType, logic in CartridgeReader

Step 1 put everything into `AtariCartridgeReader`: the window address of
each type, the standard type for a raw size, header parsing, detection,
the bank expansion, and the dis6502 segments. Only the last part is
specific to dis6502. The rest is split off:

| Part | Where | Content |
|---|---|---|
| **Bank metadata** | `CartridgeType` (WUDSN Base) | For every type, where its banks appear in memory (see "Bank regions") and whether the image is Atrax-interleaved |
| **Reading logic** | `CartridgeReader` (dis6502 for now, `com.wudsn.tools.base.atari` later) | Header parsing, detection, candidate types, Atrax decoding, expanding the bank regions into banks; depends only on the JDK and WUDSN Base |
| **Error messages** | New `Messages` class in `com.wudsn.tools.base.atari` (WUDSN Base) | The reading errors, with German translations like the rest of WUDSN Base |
| **dis6502 import** | `AtariCartridgeReader` (dis6502, keeps its name) | The 4 MB limit, one segment per bank with title and label prefix |

### Bank regions in CartridgeType

`CartridgeType` gets one new piece of data per type, a list of bank
regions:

```java
/** A part of the image whose banks of bankSize bytes appear at addresses, bank i at addresses.get(i % addresses.size()). */
public static final class BankRegion { // getOffset(), getSize(), getBankSize(), getAddresses(), getMirrorAddresses()
}

public List<BankRegion> getBankRegions(); // Empty: no known mapping (AST 32).
public boolean isAtraxInterleaved();      // 48, 49, 68.
```

This one shape covers every layout in `cart.txt`:

| Layout | Types | Regions (offset, size, bank size, addresses) |
|---|---|---|
| Single window | standard, Williams, Express, Diamond, SDX, Phoenix, Blizzard, Atarimax, Turbosoft, Ultracart, aDawliah, MegaCart, MegaMax, The!Cart, 5200 standard and Super Cart | One region over the whole image, one address |
| SIC! | 54-56 | One region, 8 KB banks, addresses `$8000`, `$A000` alternating |
| Fixed plus switchable | XEGS, switchable XEGS, XEGS 8F, DB 32 | Switchable banks at `$8000`, the last 8 KB bank at `$A000` |
| OSS | 3, 45 / 15, 44 | 4 KB banks: bank 3 (3, 45) or bank 0 (15, 44) at `$B000`, the others at `$A000` |
| Bounty Bob | 18 / 7 (5200) | Four 4 KB banks at `$8000` / `$4000`, four at `$9000` / `$5000`, the last 8 KB at `$A000` |
| 5200 two chip | 6 | First 8 KB at `$4000`, second at `$A000` |
| Mirrors | 20, 46 (4 KB), 19 (5200 8 KB), 16 (5200 16 KB), 6, 7 | Address `$B000` (or the one ending at `$BFFF`), the other mirrors in `mirrorAddresses` |

- **Existing fields stay as they are.** `getInitialBankAddress()` is what
  TheCartStudio's `CartridgeTypeSampleCreator` builds its sample images
  from, so changing it for types 20 and 46 would change TheCartStudio's
  output. The regions carry the corrected `$B000` instead; the TODOs at
  types 20 and 46 decide whether `getInitialBankAddress()` follows.
- **Mirrors are recorded, not yet used.** With `mirrorAddresses` in the
  data, the importer can later resolve code reached through a mirror (the
  `$A000` TODO in `AtariCartridgeReader`).

### CartridgeReader

```java
public final class CartridgeReader {
	public static boolean hasCartridgeHeader(long fileSize, byte[] header);
	public static CartridgeType detectCartridgeType(Platform platform, long fileSize, byte[] header);
	public static List<CartridgeType> getCandidateTypes(Platform platform, long fileSize);
	public static boolean isSupported(Platform platform, CartridgeType cartridgeType);
	public static Cartridge readCartridge(Platform platform, CartridgeType cartridgeType, InputStream inputStream,
			long fileSize, long maximumSize) throws IOException;
}

/** The plain (header-less, decoded) image and its banks. */
public final class Cartridge { // getCartridgeType(), getContent(), getBanks(), getInitialBank()
}

public final class Bank { // getNumber(), getOffset(), getSize(), getAddress()
}
```

- **Java 8 source level.** Like all of WUDSN Base, `CartridgeReader` must
  compile at Java 8: the Eclipse projects of WUDSN Base and of its
  dependents (TheCartStudio, AtariROMMaker, AtariROMChecker at 1.8, older
  ones below) still use that level, even though Maven compiles at
  `--release 21`. So no records, `List.of`, `InputStream.readNBytes` or
  other API after Java 8 - although dis6502 itself is at 21. `Cartridge` and
  `Bank` are plain final classes with getters.

- **Standard types for raw sizes** (Atari 800 2-16 KB, 5200 4-32 KB) move
  here from `AtariCartridgeReader`: a reading policy, not dis6502's.
- **The size limit is a parameter.** 4 MB is dis6502's choice, so the
  importer passes it.
- **Messages:** from the new `com.wudsn.tools.base.atari.Messages`
  (`extends NLS`, like `com.wudsn.tools.base.Messages`), so
  `CartridgeReader` has no dis6502 dependency from the start and moves to
  WUDSN Base unchanged. Its numbers start at E700, clear of WUDSN Base's
  own 200-304 and the applications' ranges (up to 503 in TheCartStudio).
  The six reading errors (unsupported raw size, unknown header type, wrong
  platform, unsupported type, size mismatch, too large) move there from
  dis6502's E055 and E094-E098, which are removed.

### AtariCartridgeReader (dis6502)

What remains in dis6502: calls `readCartridge` with the 4 MB limit and
turns each `Bank` into a segment with title and label prefix (see
"Segments"), returning the initial bank's segment for `Atari800`/
`Atari5200` to type their trailer.

## Design

### Segments

- **One segment per bank:**
  - `wBegin`: the bank's window address; `wEnd`: `wBegin + bankSize - 1`.
  - `bBinary = true`, `FileHeader.RAW`.
  - **Title:** the type's display name plus the bank, e.g.
    `XEGS 128 KB - bank 3`. The fixed or initial bank is marked as such.
- **Label prefix per bank** (`Segment.labelPrefix`, e.g. `B3_`). Banks
  share addresses, so without a prefix the listing would define `LA000`
  once per bank and not assemble. Label resolution across banks stays
  approximate: a reference to `$A123` resolves to the first segment that
  contains it (`SegmentList.findSegmentByFixedAddr`), which is the best
  any static disassembler can do without knowing the active bank.
- **Single-bank types** (standard 2-16 KB, 5200 4-32 KB) still produce
  exactly one segment without a prefix, so today's behavior and
  workspaces stay as they are.

### Atrax descrambling

The Atrax types 48, 49 (Atrax SDX) and 68 (Atrax 128) store the image with
the address and data lines swapped. The bit mappings already exist in
TheCartStudio's `CartridgeTypeSampleCreator`
(`createInterleavedAtraxSDXContent`, `createInterleavedAtrax128Content`),
which *encodes* a plain image. dis6502 needs the inverse: for every byte of
the encoded image, compute its plain address by the inverse address
permutation and its plain value by the inverse data permutation. Both
permutations are bijections on the image, so decoding an encoded image
must give back the plain image exactly - which is also the test.

Both directions move into WUDSN Base's `CartridgeFileUtility`, next to the
other `.car` helpers, so TheCartStudio and dis6502 share one definition of
the bit mappings:

```java
public static byte[] encodeAtraxContent(CartridgeType cartridgeType, byte[] content);
public static byte[] decodeAtraxContent(CartridgeType cartridgeType, byte[] content);
```

Both accept `CARTRIDGE_ATRAX_SDX_64`, `CARTRIDGE_ATRAX_SDX_128` and
`CARTRIDGE_ATRAX_128` and throw `IllegalArgumentException` for any other
type. The encoding is moved unchanged from TheCartStudio's
`createInterleavedAtraxSDXContent`/`createInterleavedAtrax128Content`; the
decoding is its inverse. WUDSN Base gets the round-trip test. After the
change, WUDSN Base must be reinstalled before dis6502 sees it (see
`DEVELOPMENT_GUIDE.md`). Switching TheCartStudio's
`CartridgeTypeSampleCreator` to the new methods is a separate follow-up in
that repository, not part of this plan.

### Detection

- `ComputerSystem.guessFileType(File)` reads 16 bytes instead of 4
  (`CartridgeFileUtility.CART_HEADER_SIZE`), padding short files with
  zeros. Every system's `guessFileType(long, byte[])` keeps working, since
  it only reads the first 4.
- Both Atari systems return `ROM_IMAGE_FILE` when
  `detectCartridgeType` finds a supported type for their platform.
  - **Mismatch:** a `CART` file of the other platform gets a specific error
    ("... is an Atari 5200 cartridge"), not "unsupported size".
  - **Raw files:** the sizes read today keep their current type without
    asking (Atari 800: 4, 8 and 16 KB as standard cartridges, plus 2 KB;
    Atari 5200: 4, 8, 16 and 32 KB standard, 40 KB Bounty Bob). Some of
    them also match other types (e.g. 8 KB: Phoenix, OSS, right slot),
    but a standard cartridge is what they almost always are. Any other
    size that matches at least one supported type is a cartridge
    candidate, resolved by the dialog below.

### Cartridge type dialog for raw images

A raw image (no `CART` header) of e.g. 64 KB matches several types (XEGS,
switchable XEGS, Williams, Express, Diamond, SDX, MegaCart, Turbosoft,
aDawliah, ...) and cannot be classified from its content. The user
chooses, as in emulators:

- **`CartridgeTypeDialog`** in `ui/` (flat, per the subpackage rules): the
  file name and size, and a list of `getCandidateTypes` with their display
  names, sorted by name. Plus a choice "Not a cartridge - open as raw file",
  which falls back to today's `RawFileDialog` path. OK/Cancel and texts
  follow `RULES_WUDSN_BASE.md` and `RULES_LOCALIZATION.md`.
- **Flow:** `Dis6502.openFile` gets `ROM_IMAGE_FILE` from `guessFileType`
  (or from File > Open ROM Image). For a raw file with more than one
  candidate, it shows the dialog before reading, then passes the chosen
  type down: `WorkspaceLogic.addFile` and `ComputerSystem.readFile` get a
  `CartridgeType` parameter (`null` everywhere else), which `readROMFile`
  forwards to `readCartridge`. `CartridgeType` comes from WUDSN Base,
  which dis6502 already depends on; systems without cartridges ignore it.
- **Command line and drag and drop** take the same path, since both go
  through `openFile`.
- **Not persisted:** the chosen type only shapes the segments; a saved
  workspace contains the segments, not the type.

### Callers

- **`Atari800.readROMFile`:** `readCartridge(Platform.ATARI_800, ...)`,
  then types the 6-byte trailer (`$BFFA-$BFFF`) of the initial bank,
  provided that bank ends at `$BFFF`. This resolves the TODO at
  `Atari800.java:463`.
- **`Atari5200.readROMFile`:** `readCartridge(Platform.ATARI_5200, ...)`,
  then types the title and vectors of the bank that ends at `$BFFF`, and
  sets the title from it, as today.
- The `SIZE_*`/`CAR_HEADER_SIZE` constants and messages E050, E054 and E055
  are replaced by the new class's checks and messages (unsupported type,
  wrong platform, size does not match type, truncated file), added to
  `Messages`.

### Size limit

Cartridges larger than 4 MB are not imported (a clear "too large" message,
not "unsupported type"). 4 MB covers every type except The!Cart 32, 64
and 128 MB (65, 66, 62); `MEGA_4096` is exactly 4 MB and included. That is
at most 512 segments of 8 KB or 256 of 16 KB.

## Tests

- **Model tests** in `test/.../model/system/atari/AtariCartridgeReaderTest`:
  - **Fixtures:** synthetic `.car` images built in the test with
    `CartridgeFileUtility.createCartridgeHeaderWithCheckSum`. Every byte of
    bank *n* holds *n*, so each segment's address and content can be
    asserted with hand-computed values.
  - **Coverage:** one case per layout row, plus every single-bank type.
  - **Errors:** wrong platform, wrong size, truncated file, an unsupported
    type.
- **Detection tests:** `guessFileType` for a raw file, a `CART` file, a
  `CART` file of the wrong platform, a file above 4 MB, and the two
  existing bugs above; `getCandidateTypes` for each raw size.
- **Atrax:** in WUDSN Base, encode a plain synthetic image, decode it, and
  compare with the original, for all three types. In dis6502, import an
  encoded synthetic image and check the bank contents.
- **Dialog:** `DialogTextsTest`/`UIWiringTest` coverage like the other
  dialogs (texts, mnemonics, OK/Cancel/raw-file choice).
- **Real images:** spot checks against real cartridge images from the
  Atari ROM Maker test data
  (`Productions/com.wudsn.productions.atari800.atarirommaker/tst/rom/ROM-Correct`,
  types 1-67) as an ad hoc smoke test outside the repository. That folder
  is 410 MB and holds third-party ROMs, so none of it is committed.
- **Reassembly round trip** (`ReassemblyRoundTripTest`) for one multi-bank
  type, to prove the label prefixes keep the listing assemblable.

## Steps

1. Done: `AtariCartridgeReader` with the single-window layouts, detection
   and the 4 MB limit, both `readROMFile` methods switched over.
2. WUDSN Base: add `BankRegion`, `getBankRegions()` and
   `isAtraxInterleaved()` to `CartridgeType` with the regions of all 75
   types from `cart.txt`, plus `encodeAtraxContent`/`decodeAtraxContent`
   in `CartridgeFileUtility`, and the new `com.wudsn.tools.base.atari.Messages`
   with the reading errors (English and German), each with tests; commit
   and reinstall.
3. dis6502: split `CartridgeReader` off `AtariCartridgeReader`, using
   the regions and the new WUDSN Base messages. That adds every layout of
   the table at once (XEGS, OSS, Bounty Bob, SIC!, 5200 two chip, Atrax),
   only AST 32 stays unsupported. Tests per layout row.
4. Add `CartridgeTypeDialog` and the `CartridgeType` parameter through
   `openFile`/`addFile`/`readFile`.
5. Remove the cartridge limit from `FURTHER_IMPROVEMENTS.md`.
6. Move `CartridgeReader` to WUDSN Base's `com.wudsn.tools.base.atari`.

## Progress

**Step 1 (2026-10-04):** `model.system.atari.AtariCartridgeReader` with
the single-window layouts of 44 types (36 Atari 800, 8 Atari 5200),
detection from the CART header or the raw size, the 4 MB limit, and both
`readROMFile` methods switched over. Both existing bugs are fixed: a 4 KB
`.car` file is read, and a raw 40 KB 5200 image is no longer detected as a
ROM image until Bounty Bob is supported. `ComputerSystem.guessFileType`
reads 16 bytes. Messages E050 and E054 were removed, E094-E098 added.
`AtariCartridgeReaderTest` covers detection, both platforms and every
error. `RULES_SYSTEM_SUBPACKAGES_PLAN.md` has the rule for the shared
subpackage, and the `Atari800.java:463` TODO is resolved.

Findings while checking each layout against `cart.txt`:

- **Wrong addresses in `CartridgeType`:** types 20 (5200 4 KB) and 46
  (Blizzard 4 KB) appear at both `$A000` and `$B000`. `CartridgeType` says
  `$A000`, but only `$B000` contains the vectors at the end, so the import
  uses `$B000`. The window addresses are listed per type in the reader
  instead of taken from `getInitialBankAddress()`.
- **SIC! (54-56)** got its own layout row above; it is not a single window.
- **Mirrors:** the smoke test's sample images of types 20 and 46 have their
  init vector at `$A000`, the other mirror. Code reached through it is not
  traced, since the segment is at `$B000`. Real cartridges may use either
  mirror; not changed for now.
- **Test data:** the `.car` files in the Atari ROM Maker test folder have
  header type numbers that do not match their names (e.g. "Blizzard 16 KB
  (40).car" has type 6), so the smoke test used the raw `.rom` files with
  the type from their names instead: all 37 of them with a supported type
  import, with plausible vectors.

**Step 2 (2026-10-04), WUDSN Base:** `CartridgeType.BankRegion`,
`getBankRegions()` for every type (empty for AST 32 and `UNKNOWN`),
`isAtraxInterleaved()` and `getSize()`;
`CartridgeFileUtility.encodeAtraxContent`/`decodeAtraxContent`, table-driven;
the new `com.wudsn.tools.base.atari.Messages` (E700-E705, English and
German). The `base.atari` module got a JUnit 5 test setup (`test/` folder,
surefire 3.2.5, Eclipse `.classpath`): `CartridgeTypeTest` checks that the
regions of every type cover its image and that the bank with the vectors
ends at `$BFFF`, plus the exact regions of one type per layout;
`CartridgeFileUtilityTest` compares the encoding with TheCartStudio's
original loops and checks the decoding as their inverse; `MessagesTest`
checks the English and German keys.

`BankRegion` was first a record, which broke the Eclipse build: the WUDSN
Base Eclipse projects compile at Java 1.8, and Maven, at 21, did not notice.
It is now a plain final class, and `List.of`/`Set.of` were replaced with
Java 8 API; the whole module and its tests compile with `javac --release 8`
(decision 10).

Finding: the bank holding the vectors is the one with the *last* byte of
the initial bank (`getInitialBankOffset() + getBankSize() - 1`), not the
first. For the two chip 5200 cartridge (6), `CartridgeType` describes the
initial bank as the whole 16 KB image at offset 0, but only the second
chip ends at `$BFFF`. `CartridgeReader` uses the same rule in step 3.

**Step 3 (2026-10-04), dis6502:** `CartridgeReader` split off
`AtariCartridgeReader`, at the Java 8 source level (checked with `javac
--release 8`), depending only on the JDK and WUDSN Base. It reads the
header, detects the type, decodes Atrax images and expands the bank regions
into `Bank`s, returning a `Cartridge`; its errors are WUDSN Base's E700-E705,
and dis6502's E055 and E094-E098 are removed. `AtariCartridgeReader` keeps
the 4 MB limit and creates the segments. Every layout is now supported:
XEGS, switchable XEGS, DB 32, OSS, Bounty Bob (both platforms, a raw 40 KB
5200 image is Bounty Bob again), SIC!, 5200 two chip and Atrax; only AST 32
and the The!Cart types above 4 MB are not. The explicit window addresses
and their TODO are gone. `AtariCartridgeReaderTest` checks each layout with
images whose 4 KB pages carry their page number, so every segment's file
offset is checked exactly. The smoke test imports all 63 sample `.rom`
files of a supported type, Atrax included, with plausible vectors.

**Steps 4 and 5 (2026-10-04), dis6502:** `ComputerSystem.getCartridgeTypeCandidates`
(empty by default; the Atari systems return the importable types of the
size of a raw image without a standard size), and a `CartridgeType`
parameter through `WorkspaceLogic.addFile`, `ComputerSystem.readFile` and
`readROMFile`, the old overloads passing `null`. Both Atari systems'
`guessFileType` return `ROM_IMAGE_FILE` for such an image - the Atari 800
only after the cassette and disk image checks. `Dis6502.openReadableFile`
shows the new `CartridgeTypeDialog` (`ui/`): the candidates with text and
type number, sorted by text ignoring case, the first selected; OK, "Open as
Raw File" (continues in `openRawFile`) and Cancel. Unlike planned, the
dialog also appears for a single candidate (e.g. a raw 40 KB Atari 800
image, only Bounty Bob): the image may just as well be plain data, and the
dialog is where the user says so. Tested in `AtariCartridgeReaderTest`
(candidates, reading as the chosen type) and `DialogTextsTest` (texts,
list, the three buttons); a live smoke test opened a raw 64 KB file in the
application and imported it as the chosen XEGS 64 KB. The cartridge entry
in `FURTHER_IMPROVEMENTS.md` now lists only the remaining limits.

**Step 6 (2026-10-04):** `CartridgeReader` moved unchanged, apart from its
package, to WUDSN Base's `com.wudsn.tools.base.atari`, with its own JUnit
test `CartridgeReaderTest` there (detection, candidates, the banks of one
type per layout, Atrax, every error including a truncated stream). dis6502
imports it from there; its `AtariCartridgeReaderTest` keeps the segment
level.

## Decisions (2026-10-03)

1. **Package:** a new subpackage `model.system.atari` for code shared by
   both Atari systems; `RULES_SYSTEM_SUBPACKAGES_PLAN.md` gets a rule for
   such family packages.
2. **Raw images of ambiguous size:** a dialog lets the user choose the
   cartridge type, or open the file as raw.
3. **Atrax 48, 49, 68:** descrambled and imported.
4. **Size limit:** 4 MB.
5. **Atrax permutation:** both directions in WUDSN Base's
   `CartridgeFileUtility`, shared with TheCartStudio.

## Decisions (2026-10-04)

6. **Split:** the bank metadata goes into `CartridgeType`, the reading
   logic into a separate `CartridgeReader`, to be moved to WUDSN Base
   later.
7. **Messages:** in a new `Messages` class of `com.wudsn.tools.base.atari`
   now, not in dis6502.
8. **Name:** the dis6502 part keeps the name `AtariCartridgeReader`, in
   line with the `read...` methods of `ComputerSystem`; the `Atari` prefix
   distinguishes it from `CartridgeReader`.
9. **Tests of WUDSN Base:** in WUDSN Base itself, with a new JUnit 5 test
   setup in the `base.atari` module.
10. **Java level of WUDSN Base:** the sources stay compilable at Java 8, the
   level of the Eclipse projects of WUDSN Base and its dependents; no
   records or newer API. This applies to `CartridgeReader` as well, since it
   moves to WUDSN Base.
