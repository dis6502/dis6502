# Importing every Atari 800 and Atari 5200 cartridge type

Status: In progress - step 1 done, redesign decided (2026-10-04); next is
step 2.

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
/** A part of the image whose banks of bankSize bytes appear at addresses, bank i at addresses[i % addresses.length]. */
public record BankRegion(int offset, int size, int bankSize, int[] addresses, int[] mirrorAddresses) {
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
public record Cartridge(CartridgeType cartridgeType, byte[] content, List<Bank> banks, Bank initialBank) {
}

public record Bank(int number, int offset, int size, int address) {
}
```

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
6. Later: move `CartridgeReader` to WUDSN Base's `com.wudsn.tools.base.atari`.

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
