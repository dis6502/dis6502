# Plan 15: Move the settings sections, the MRU list and its menu to WUDSN Base

Status: Planned (2026-10-04) - every design question is decided, see
"Decisions".

## Goal

`MRUEntry` and `MRUList` (`com.wudsn.tools.dis6502.model`) keep the "most
recently used" files of DIS6502's **File > Recent Workspaces/Recent
Files** menus. Nothing in them is specific to a disassembler, and other
WUDSN tools need the same thing - The!Cart Studio has its own hand-written
list of recent workbooks. They move to WUDSN Base as generic classes, where
each project supplies its own value set of file types, together with the
code that fills a Swing menu from such a list (decision 2).

They store themselves in an `ApplicationSettingsSection`, which moves to
WUDSN Base with them: The!Cart Studio's XML-based `Preferences` are to be
replaced by the same settings sections later (decision 1), so they are the
common way for WUDSN tools to keep settings, not a DIS6502 detail.

## What the classes depend on today

| Dependency | Where | Specific to DIS6502? |
|---|---|---|
| `FileType` (a WUDSN Base `ValueSet`) | the type of each entry; `getKey()`/`fromKey()` to store it | Yes - the project's own value set |
| `FileType.ANY_FILE` | `load()` drops entries whose stored type is unknown (`fromKey` returns `ANY_FILE` then) | Yes |
| `ApplicationSettingsSection` | where the list is stored: one `java.util.prefs.Preferences` node per section | No - only `java.util.prefs` |
| `Application.getSettingsSection` | creates and caches the sections under the settings root | The caching no; the root yes (see below) |
| `java.util` only | the list logic: move to the front, no duplicates (paths compared ignoring case), at most `maxEntries`, the last path of a type | No |

`MRUController` (`ui`) uses the list: two lists ("RecentWorkspaces",
"RecentFiles", 5 entries each), the split of workspaces from other files,
and the Swing menus - `fillMenu` makes one item per entry, numbered
"1 path", "2 path", ..., each calling a listener with its entry, and
disables the menu when the list is empty. `FileChoosers` asks it for the
last path of a type; `UIWiringTest` creates an `MRUEntry`.

The settings keys are `FilePath_1`..`FilePath_5` and `FileType_1`..
`FileType_5`, the value being the `FileType` ID (`WORKSPACE_FILE`, ...).

### The settings root stays the application's

DIS6502's `Application` takes its root node from
`Preferences.userNodeForPackage(Application.class)` - the node of the
package `com.wudsn.tools.dis6502` - or a child of it named by the system
property `dis6502.settingsNode`, which `TestRunner` sets so that tests never
touch the user's settings. That root must stay per application: computed in
WUDSN Base, it would be WUDSN Base's package node, shared by every WUDSN
tool, and DIS6502's users would lose their settings. So each application
keeps choosing its root and hands it to WUDSN Base.

## Design

### In WUDSN Base, package `com.wudsn.tools.base.common`

```java
/** The settings sections of one application, under its own root node. */
public final class ApplicationSettings {
    public ApplicationSettings(Preferences root)
    public ApplicationSettingsSection getSection(String name)  // created once, then cached
}

/** One named section of settings: key/value pairs in one Preferences node. */
public final class ApplicationSettingsSection {
    ApplicationSettingsSection(Preferences preferences)        // created by ApplicationSettings
    public String getString(String key, String defaultValue)
    public void writeString(String key, String value)
    public int getUnsignedInt(String key, int defaultValue)
    public void writeUnsignedInt(String key, int value)
    public void clear()
}

public final class MRUEntry<T extends ValueSet> {
    public MRUEntry(String filePath, T fileType)
    public String getFilePath()
    public T getFileType()
}

public final class MRUList<T extends ValueSet> {
    public MRUList(Class<T> fileTypeClass, ApplicationSettingsSection section, int maxEntries)
    public void clear()
    public void addFile(String filePath, T fileType)
    public List<MRUEntry<T>> getEntries()
    public String getLastFilePath(T fileType)   // "" if none
    public void load()
    public void save()
}
```

- **`ApplicationSettings`** is new: the section cache that is
  `Application.getSettingsSection` today. The name avoids WUDSN Base's
  existing `common.Application` (the version and update check singleton),
  which is a different thing.
- **`ApplicationSettingsSection`** moves unchanged. Methods The!Cart
  Studio's settings will need (booleans, colors, window bounds) are added
  when it is converted, not now.
- **The file type is any `ValueSet`** of the project. It is stored by its
  `getId()` and found again among `ValueSet.getValues(fileTypeClass)`. That
  method calls the value set's static `getValues()`, which `FileType`
  already has; the class javadoc states the requirement. An entry whose
  stored ID is no value (any more) is skipped on `load()`, as today.
- **The keys stay `FilePath_<n>` and `FileType_<n>`**, and the sections
  stay in the same nodes, so the recent lists and all other settings users
  have today survive the update.
- **One fix on the way:** `save()` writes only the existing entries, so
  after the list shrank (`clear()`), the old keys beyond its end come back
  on the next `load()`. `save()` then also writes `""` for the slots after
  the last entry up to `maxEntries`, which `load()` already skips.
- **Unchanged:** paths are compared ignoring case (correct on Windows,
  harmless elsewhere for a recent-files list); new entries go to the front;
  the oldest entry is dropped beyond `maxEntries`.
- **Java 8 source level**, like all of WUDSN Base: no `List.of`, no
  records.

### In WUDSN Base, package `com.wudsn.tools.base.gui`

```java
/** Fills a menu with the entries of an MRU list. */
public final class MRUMenu {
    public interface SelectionListener<T extends ValueSet> {
        void onSelect(MRUEntry<T> entry);
    }

    public static <T extends ValueSet> void fill(JMenu menu, MRUList<T> list,
            SelectionListener<T> listener)
}
```

- `fill` is `MRUController.fillMenu` without DIS6502's choice of list:
  it removes the menu's items, adds one per entry ("1 path", "2 path",
  ...), each calling `listener` with its entry, and disables the menu when
  the list is empty. A static helper like `ElementFactory`, since it keeps
  no state.
- The item texts are the number and the path only - no texts from a
  repository, so `MRUMenu` needs no `.properties` entries.

### In DIS6502

- Delete `ApplicationSettingsSection.java`, `model/MRUEntry.java` and
  `model/MRUList.java`; import the WUDSN Base classes instead (9 files
  use `ApplicationSettingsSection` or `getSettingsSection`, among them
  `Dis6502` for the font settings and `DefaultFoldersLogic`).
- `Application` keeps its root (`getDefaultSettingsRoot()`, the
  `dis6502.settingsNode` property, the test constructor) and holds an
  `ApplicationSettings` for it; `getSettingsSection(name)` delegates to
  `ApplicationSettings.getSection(name)`, so its callers do not change.
- `MRUController` creates `new MRUList<>(FileType.class,
  application.getSettingsSection("RecentWorkspaces"), MRU_MAX_ENTRIES)`
  (and the same for "RecentFiles") and uses `MRUEntry<FileType>`. It never
  adds an entry of `FileType.ANY_FILE` - that check moves here from
  `MRUList.load()`, so a file opened as "any file" stays out of the menus
  as it does today.
- `FileType.getKey()`/`fromKey()` existed only for `MRUList`: remove them,
  use `getId()` in the three exception messages in `Dis6502`, and update
  `FileType`'s javadoc.
- `Dis6502.openRecentWorkspace`/`openRecentFile` and `UIWiringTest` take
  `MRUEntry<FileType>`.

- `MRUController.fillMenu` keeps choosing the workspace or the file list
  and calls `MRUMenu.fill`; its `MRUEntrySelectionListener` is replaced by
  `MRUMenu.SelectionListener<FileType>`.

`MRUController` itself stays in DIS6502: the split into workspaces and
other files and the section names are DIS6502's.

### Later, not in this plan: The!Cart Studio

Its `Preferences` (XML file) becomes an `ApplicationSettings` under its own
root node, its recent workbooks an `MRUList` over a value set of its file
types, its "Open Recent" menu an `MRUMenu`. Its settings would start empty once, unless it imports the old XML
file on first start - to be decided in its own plan.

## Tests

- **WUDSN Base:** `com.wudsn.tools.base` has no tests yet. It gets the same
  JUnit 5 setup as `com.wudsn.tools.base.atari` (`test` folder,
  `junit-jupiter` dependency) and tests on a throw-away `Preferences` node,
  removed afterwards:
  - `ApplicationSettingsTest`: a section is created once and cached,
    values round-trip, `clear()` restores the defaults.
  - `MRUListTest` with a small test value set: front insertion, no
    duplicates (also with different case), the limit, `getLastFilePath`
    per type, the `save()`/`load()` round trip, the keys used, an unknown
    stored ID being skipped, and the stale-slot fix after `clear()`.
  - `MRUMenuTest`: the items' texts and order, the menu disabled when the
    list is empty and enabled again when it is not, refilling replacing the
    old items, and an item's action passing its entry to the listener.
    Swing menus can be built without a display, so it runs headless too.
- **DIS6502:** `TestRunner` (both modes) must pass. The Recent menus and
  the other settings (fonts, default folders) must still show what the
  previous version saved - a check by hand with the real settings, since
  nodes and keys are unchanged.

## Steps

1. **WUDSN Base:** test setup for `com.wudsn.tools.base`,
   `ApplicationSettings`, `ApplicationSettingsSection`, `MRUEntry`,
   `MRUList`, `MRUMenu` and their tests; check with `javac --release 8`; `mvn -o
   install`; commit in WUDSN Base.
2. **DIS6502:** switch to the WUDSN Base classes as described, delete the
   old ones, remove `FileType.getKey()`/`fromKey()`; run both test modes;
   check the Recent menus (items, disabled when empty, opening an entry)
   and settings by hand.
3. **Docs:** this plan's status, `plans/README.md`, `plans/MEMORY.md` where
   it names `ApplicationSettingsSection`'s package. No `CHANGES.md` entry -
   nothing changes for the user.

## Decisions (2026-10-04)

1. **Settings sections, not a storage interface:** The!Cart Studio's
   XML-based preferences are to be replaced by `ApplicationSettingsSection`
   later. So `ApplicationSettingsSection` moves to WUDSN Base and
   `MRUList` stores itself in one directly, instead of through an
   interface each application would implement.
2. **The menu moves too:** `MRUController.fillMenu`'s generic part becomes
   `MRUMenu` in `com.wudsn.tools.base.gui` now, not only when The!Cart
   Studio is converted.
