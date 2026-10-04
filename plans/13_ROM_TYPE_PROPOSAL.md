# A system-independent ROMType instead of CartridgeType

Status: Done (2026-10-04), see "Result".

## Problem

The cartridge import ([`12_CARTRIDGE_IMPORT_PLAN.md`](12_CARTRIDGE_IMPORT_PLAN.md))
lets WUDSN Base's Atari-specific `CartridgeType` leak out of the Atari
`ComputerSystem` subclasses into the system-independent parts of dis6502:

| Class | Leak |
|---|---|
| `model.system.ComputerSystem` | `getCartridgeTypeCandidates` returns `List<CartridgeType>`; `readFile` and `readROMFile` take a `CartridgeType` |
| `model.WorkspaceLogic` | `addFile(..., CartridgeType)` |
| `Dis6502` | `openReadableFile` holds and passes a `CartridgeType` |
| `ui.CartridgeTypeDialog` | lists `CartridgeType`s and formats their CART type number |
| `ui.DialogTextsTest` | fills the dialog with `CartridgeType`s |

`RULES_SYSTEM_SUBPACKAGES_PLAN.md` keeps system-specific code in
`model.system.<name>`; a C64 or Oric cartridge format would otherwise have
to squeeze into an Atari value set.

## Proposal

### `ROMType` in `model.system`

A small, immutable, system-independent value class, in the shared
`model.system` package next to `ComputerSystemType`, since it is part of
the `ComputerSystem` API:

```java
/** A type of ROM image a computer system offers for a raw ROM image of ambiguous type. */
public final class ROMType {
	public ROMType(ComputerSystemType computerSystemType, String id, String text); // By the subclasses only.
	public ComputerSystemType getComputerSystemType(); // The leading part of the identity.
	public String getId();   // Unique within the system, e.g. "CARTRIDGE_XEGS_64".
	public String getText(); // Display text, complete and localized, e.g. "XEGS 64 KB (13)".
	// equals/hashCode by computer system type and id, toString returns the text.
}
```

- **Identity: computer system type and id.** An id is only unique within
  its system: `CartridgeType`'s ids happen to be globally unique, but a
  C64 or Oric format could use the same id as an Atari one. With the system
  as the leading part, two systems' types can never be confused.

- **Not a WUDSN `ValueSet`:** a value set has a fixed, statically declared
  set of values with texts in a properties file. The ROM types are the
  values of each system's own type list (for the Atari systems,
  `CartridgeType`), so they are created by the system at run time.
- **The text is complete.** The system decides how a type is shown,
  including the CART type number in parentheses. The
  `CartridgeTypeDialog_CartridgeTypeText` text ("{0} ({1})") therefore
  moves from the dialog to the Atari side
  (`AtariCartridgeReader`), and the dialog shows `getText()` as is.
- **The id maps back.** The Atari systems use the `CartridgeType` id
  (`CartridgeType.getInstance(String)` resolves it). A system first checks
  `romType.getComputerSystemType() == getType()` and rejects a foreign
  `ROMType` with an `IllegalArgumentException`: a programming error, since
  only the same system's candidates are ever passed back.

### ComputerSystem API

| Today | Proposed |
|---|---|
| `List<CartridgeType> getCartridgeTypeCandidates(File)` | `List<ROMType> getROMTypes(File)` |
| `List<CartridgeType> getCartridgeTypeCandidates(long, byte[])` | `List<ROMType> getROMTypes(long, byte[])` |
| `readFile(..., CartridgeType)` | `readFile(..., ROMType)` |
| `readROMFile(..., CartridgeType)` | `readROMFile(..., ROMType)` |

- `ComputerSystem` no longer imports anything from `com.wudsn.tools.base.atari`.
- `Atari800`/`Atari5200` convert in both directions through two helpers in
  `AtariCartridgeReader` (same package as the rest of the Atari cartridge
  code): `toROMTypes(List<CartridgeType>)` and
  `toCartridgeType(Platform, ROMType)`.

### Callers

- **`WorkspaceLogic.addFile(..., ROMType)`**, **`Dis6502.openReadableFile`**:
  only the type changes.
- **The dialog** becomes `ROMTypeDialog` with `ROMType` in its list and
  generic texts: title "Choose ROM Type", list label "ROM &Type:". The
  repository entries are renamed accordingly (`ROMTypeDialog_*` in
  `Texts`, `DataTypes` and `Actions`).

### Unchanged

- Everything inside `model.system.atari*`, including
  `AtariCartridgeReaderTest`, keeps using `CartridgeType` - that is
  where it belongs.
- Behavior: same candidates, same order, same texts in the list.

### Tests

- **`ROMTypeTest`** (`model.system`): equality by computer system type and
  id - the same id of two systems is not equal - and `toString`.
- **`AtariCartridgeReaderTest`**: the candidates via `getROMTypes`
  (system, ids and texts), reading with a `ROMType`, and the rejection of
  a `ROMType` of the other Atari system.
- **`DialogTextsTest`**: the dialog filled with plain `ROMType`s - no
  WUDSN Base Atari class any more.
- A check that no class outside `model.system.atari*` imports
  `com.wudsn.tools.base.atari` - as a test (`grep`-like scan of the
  sources in `TestRunner`), so the leak cannot return unnoticed. See open
  question 2.

## Steps

1. `ROMType` with test.
2. `ComputerSystem`, `Atari800`, `Atari5200`, `AtariCartridgeReader`,
   `WorkspaceLogic`, `Dis6502` switched over.
3. Dialog renamed and switched over, texts adjusted.
4. Tests, docs (`RULES_SYSTEM_SUBPACKAGES_PLAN.md`: the rule that
   system-specific types never appear in a `ComputerSystem` signature).

## Decisions (2026-10-04)

1. **Dialog texts:** generic - "Choose ROM Type", "ROM &Type:".
2. **Guard test:** yes - a source scan in the test suite fails when a class
   outside `model.system.atari*` imports `com.wudsn.tools.base.atari`.
3. **Identity:** a `ROMType` is identified by its computer system type
   (leading) and its id, so it lives in `model.system`.

## Result (2026-10-04)

Implemented as planned:

- `model.system.ROMType`, identified by computer system type and id.
- `ComputerSystem.getROMTypes(File)`/`getROMTypes(long, byte[])` and the
  `ROMType` parameter of `readFile`/`readROMFile`; `ComputerSystem`,
  `WorkspaceLogic` and `Dis6502` no longer refer to WUDSN Base's Atari
  package.
- `AtariCartridgeReader.getROMTypes` and `toCartridgeType` convert in both
  directions; `toCartridgeType` rejects a `ROMType` of another system or
  platform with an `IllegalArgumentException`. The list text "{0} ({1})"
  is now `Texts.AtariCartridgeReader_ROMTypeText`.
- `CartridgeTypeDialog` became `ROMTypeDialog` (title "Choose ROM Type",
  label "ROM &Type:"), showing `ROMType.getText()`.
- Tests: `ROMTypeTest`, `SystemIsolationTest` (fails when a source outside
  the Atari system packages refers to `com.wudsn.tools.base.atari` -
  checked by adding such an import), `AtariCartridgeReaderTest` (ROM types
  of both systems, reading with a `ROMType`, rejection of foreign ones),
  `DialogTextsTest` (the dialog with plain `ROMType`s).
- `RULES_SYSTEM_SUBPACKAGES_PLAN.md` has the rule.
