# Importing every Atari 800 and Atari 5200 cartridge type

Status: In progress - step 1 done (2026-10-04), see "Progress".

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

## Design

### New class `AtariCartridgeReader`

Package: `com.wudsn.tools.dis6502.model.system.atari`, a new subpackage for
code shared by the Atari systems.

```java
public final class AtariCartridgeReader {

	/**
	 * The cartridge type of a file: from its CART header if it has one,
	 * otherwise from its size among the platform's standard types.
	 * Returns CartridgeType.UNKNOWN if neither applies.
	 */
	public static CartridgeType detectCartridgeType(Platform platform, long fileSize, byte[] header);

	/**
	 * The supported types of the platform whose size matches a raw file
	 * (no CART header), for the user to choose from. Empty if none matches.
	 */
	public static List<CartridgeType> getCandidateTypes(Platform platform, long fileSize);

	/** Whether a cartridge type can be imported for the platform. */
	public static boolean isSupported(Platform platform, CartridgeType cartridgeType);

	/**
	 * Reads a cartridge file (raw or with CART header) and inserts one
	 * segment per bank. For a CART file, cartridgeType may be null (the
	 * header decides); for a raw file, it is the type the user chose. Returns the result, which identifies the segment of
	 * the bank visible after power-on, so the caller can type its trailer.
	 */
	public static CartridgeImport readCartridge(Platform platform, CartridgeType cartridgeType,
			SegmentListInserter inserter, InputStream inputStream, long fileSize) throws IOException;
}
```

`CartridgeImport` is a small result record: the `CartridgeType`, the list
of inserted segments, and the initial bank's segment.

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

### Bank layout

A private table maps each supported type to a layout. Most layouts derive
from `CartridgeType`; only the exceptions are spelled out.

| Layout | Types | Mapping |
|---|---|---|
| Single window | standard, Williams, Express, Diamond, SDX, Phoenix, Blizzard, Atarimax, Turbosoft, Ultracart, aDawliah, MegaCart, MegaMax, The!Cart, 5200 Super Cart, 5200 standard | Every bank of `getBankSize()` at one window address |
| SIC! | 54, 55, 56 | 16 KB banks of two 8 KB halves: even 8 KB blocks at `$8000`, odd ones at `$A000` |
| Fixed plus switchable | XEGS, switchable XEGS, XEGS 8F, DB 32, OSS (3, 15, 44, 45) | The bank at `getInitialBankOffset()` fixed at `getInitialBankAddress()`, all other banks at the switchable window (`$8000` for XEGS/DB, `$A000` for OSS) |
| Bounty Bob | 18 (Atari 800), 7 (5200) | Two windows of four 4 KB banks each plus the fixed 8 KB bank at `getInitialBankOffset()` |
| 5200 two chip / mirrored | 6, 16 | Per `cart.txt`; mirrors are not imported as separate segments |
| Interleaved | Atrax 48, 49, 68 | Descramble address and data bits first (see "Atrax descrambling"), then like their plain equivalents (11, 43, 17) |
| Unsupported | AST 32 | Reported as unsupported, as before |

Every mapping is checked against `cart.txt` while implementing. The test
fixtures below verify it independently.

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

1. Create `model.system.atari` and `AtariCartridgeReader` with the
   single-window layout, detection and the 4 MB limit, and switch both
   `readROMFile` methods over; fix the two existing bugs on the way.
2. Add the fixed-plus-switchable and Bounty Bob layouts.
3. Add `encodeAtraxContent`/`decodeAtraxContent` with their test to WUDSN
   Base's `CartridgeFileUtility`, commit and reinstall it; then add the
   5200 two-chip layouts and the Atrax descrambling in dis6502.
4. Add `CartridgeTypeDialog` and the `CartridgeType` parameter through
   `openFile`/`addFile`/`readFile`.
5. Remove the "Atari 5200 bank-switched cartridges and `.CAR` files" limit
   from `FURTHER_IMPROVEMENTS.md`.

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
