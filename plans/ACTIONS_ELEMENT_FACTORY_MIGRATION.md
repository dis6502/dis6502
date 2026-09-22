# Adopting WUDSN Base's Action / Actions / ElementFactory Pattern

**Status: main menu and all three popup menus (`SegmentListPanel`,
`DisassemblyPanel`, `MemoryInspectorPanel`) are done** - see each panel's
own class javadoc and `com.wudsn.tools.dis6502.Actions`' javadoc for the
concrete result, including how the popup migration diverged from the plan
below in two ways (accelerators, mnemonics for items with none in the C++
source) - both decided explicitly, not silently. What follows is this
effort's original, main-menu-only plan, left as-is for the historical
rationale/pattern explanation; the "Out of scope" section at the end has
been updated to reflect what is now actually done vs. still open.

## Purpose and scope

Today, `MainMenu.java` builds every `JMenu`/`JMenuItem` by hand: a literal
string label (`new JMenuItem("New Workspace")`), no mnemonic on any leaf
item (only the four top-level menus have none either), and an accelerator
set imperatively, item by item, only where someone remembered to add one
(`newWorkspaceMenuItem.setAccelerator(...)`). Labels, tooltips and
accelerators are Java string/code literals with no connection to a
translatable resource and no validation that a label actually has a
mnemonic character.

`com.wudsn.tools.base` (already a Maven dependency of this project) has an
established pattern other WUDSN Java tools use instead: every menu/button's
static properties (label text with an embedded mnemonic marker, tooltip,
accelerator) live as one `Action` value in a per-application `Actions`
repository class, populated reflectively from a `.properties` file at class
load; components are then built by handing that `Action` to
`ElementFactory`, which applies the mnemonic/tooltip/accelerator
consistently and fails fast if a label is missing its mnemonic marker.

This document describes what that pattern actually is (verified by reading
the WUDSN base source and two independent real applications that already
use it) and the concrete steps to apply it to dis6502's **main menu only**
(`MainMenu.java`/`Dis6502.java`'s main-menu wiring). The popup menus
(`MemoryInspectorPanel`, `DisassemblyPanel`, `SegmentListPanel`), any future
toolbar, and dialogs are explicitly out of scope for this first phase - see
"Out of scope" at the end.

## The pattern, as it actually works

Three classes are involved, all in `com.wudsn.tools.base` (already on
dis6502's classpath via `pom.xml`):

### 1. `com.wudsn.tools.base.repository.Action`

A plain, small data holder - nothing else:

```java
public final class Action {
    private KeyStroke accelerator;
    private String label;
    private String toolTip;

    public Action(int keyCode, int modifiers) { ... }               // accelerator only; label/toolTip filled in later
    public Action(String label, String toolTip, KeyStroke accelerator) { ... }  // fully specified
    ...
}
```

**There is no icon field, no command id, no enablement state, and no
listener.** `Action` only ever carries label/tooltip/accelerator - nothing
in this pattern handles icons (see "Gaps" below).

### 2. `com.wudsn.tools.base.repository.NLS`

A reflection-based static-field populator, **different from
`org.eclipse.osgi.util.NLS`** (which `com.wudsn.tools.dis6502.Text` already
uses - see `wudsn-texts-nls-pattern` memory). A class extending this `NLS`
declares `public static` (non-final) fields; calling
`initializeClass(MyClass.class, null)` in a static initializer loads
`MyClass.properties` (plus locale-suffixed variants, e.g. `_de`, falling
back to the base file) from the classpath next to the class, and for every
`Action`-typed field looks up `<fieldName>.label` (mandatory) and
`<fieldName>.toolTip` (optional) and replaces the field's value with a new,
fully-populated `Action` - **preserving the accelerator** if the field was
already pre-initialized with one in Java source (`new Action(keyCode,
modifiers)`), or leaving it `null` if it wasn't. A missing mandatory
`.label` key, or a class that isn't `public`, aborts the process
(`System.exit(-1)`) - this is a fail-fast, load-time-checked mechanism, not
a silent-fallback one.

Both `String` and `Action` fields are supported (plus `DataType`/
`ValueSet`/`Message`, not relevant here).

### 3. `com.wudsn.tools.base.Actions` (framework-level, reusable across apps)

A ready-made `Actions` repository already exists in `com.wudsn.tools.base`
itself, with generic, app-independent entries any Swing app can reuse
as-is:

```java
public final class Actions extends NLS {
    public static Action ButtonBar_OK;
    public static Action ButtonBar_Cancel;
    ...
    public static Action MainMenu_File;
    public static Action MainMenu_Edit;
    public static Action MainMenu_Tools;
    public static Action MainMenu_Help;
    public static Action MainMenu_Help_HelpContents = new Action(KeyEvent.VK_F1, 0);

    static { initializeClass(Actions.class, null); }
}
```

with `Actions.properties`:

```properties
MainMenu_File.label=&File
MainMenu_Edit.label=&Edit
MainMenu_Tools.label=&Tools
MainMenu_Help.label=&Help
MainMenu_Help_HelpContents.label=&Help Contents
MainMenu_Help_HelpContents.toolTip=Show online help contents
```

Every app that already uses this pattern (`TheCartStudio`, `AtariROMChecker`)
reuses `com.wudsn.tools.base.Actions.MainMenu_File`/`_Edit`/`_Tools`/`_Help`
for its top-level menu labels, and defines its **own**, app-specific
`Actions` class (same shape, own package, own `.properties` file) for every
leaf item and any top-level menu the shared class doesn't cover:

```java
// com.wudsn.tools.thecartstudio.Actions
public final class Actions extends NLS {
    public static Action MainMenu_File_New = new Action(KeyEvent.VK_N, KeyStroke.M1);
    public static Action MainMenu_File_Open = new Action(KeyEvent.VK_O, KeyStroke.M1);
    public static Action MainMenu_File_OpenFolder;   // no accelerator
    ...
    static { initializeClass(Actions.class, null); }
}
```

```properties
MainMenu_File_New.label=&New
MainMenu_File_New.toolTip=Create a new workbook
MainMenu_File_OpenFolder.label=Open &Folder
MainMenu_File_OpenFolder.toolTip=Open folder that contains the workbook and data folder
```

`com.wudsn.tools.base.gui.KeyStroke.M1`/`M2`/`M3` are the platform-
independent modifier constants used for accelerators (`M1` = Ctrl on
Windows/Linux, Cmd on macOS; `M2` = Shift; `M3` = Alt/Option) - use these
instead of `InputEvent.CTRL_DOWN_MASK` etc. directly, so accelerators are
correct on macOS too.

**The `&` mnemonic marker is the same convention the C++ `.rc` menu
resource already uses** (`MENUITEM "&New Workspace\tCtrl+N", ID_FILE_NEW`) -
the `.properties` label values can be copied directly from
`dis6502.rc`'s `MAIN_MENU` block with the `&` kept exactly where it is
and the `\tAccelerator` suffix dropped (the accelerator becomes a
`KeyStroke` on the `Action`, not text in the label).

### 4. `com.wudsn.tools.base.gui.ElementFactory`

A pure Swing-component factory that reads an `Action` and produces a
correctly-configured component - **and nothing else** (no listener
attachment, no icon):

```java
public static JMenu createMenu(Action action)                          // label + mnemonic (+ tooltip)
public static JMenuItem createMenuItem(Action action, String actionCommand)  // label + mnemonic + tooltip + accelerator + actionCommand
public static JButton createButton(Action action, boolean withMnemonic)
public static void setButtonTextAndMnemonic(AbstractButton button, Action action)  // for a component ElementFactory has no create-method for
```

`createMenu`/`createMenuItem` both hard-code `withMnemonic = true` and
**throw a `RuntimeException` at menu-build time if the label has no `&`
in it** - this is deliberate fail-fast validation, not a bug to route
around; every label supplied to these two methods must contain a mnemonic
marker (dis6502's C++ `.rc` menu already satisfies this for every single
item, so this is a non-issue as long as the `.properties` labels are copied
from there).

`createMenuItem` additionally pads the label with trailing spaces if the
action has an accelerator, so the accelerator hint Swing paints on the
right doesn't collide visually with the label text.

## Key decision: keep dis6502's existing wiring architecture

Both real-world call sites (`TheCartStudio.ui.MainMenu`,
`atariromchecker.ui.MainMenu`) pass one **shared** `ActionListener` into
their `MainMenu` constructor and wire every item to that same instance;
dispatch happens via a `public final class Commands` of
`public static final String` constants (passed as `createMenuItem`'s
second parameter) and a single, large
`if (command.equals(Commands.X)) { ... } else if (...) { ... }` chain in
the main app class's `actionPerformed(ActionEvent)`.

**This project should not adopt that dispatch style.** dis6502 already has
its own, already-battle-tested convention throughout this whole port: every
menu item is a public field, and `Dis6502` wires it directly with its own
listener (`item.addActionListener(e -> performXxx())`) - see the
`jdis6502-avoid-hidden-doclick-indirection` memory and every existing
`Dis6502.java` wiring line. That convention is unrelated to, and fully
compatible with, adopting `Action`/`ElementFactory` for the visual
properties: `ElementFactory.createMenuItem(action, actionCommand)` still
returns a plain `JMenuItem` with no listener attached, so `Dis6502.java`'s
existing per-item `addActionListener` lines do not need to change **at
all** - only the *construction* of each `JMenuItem` inside `MainMenu.java`
changes. The `actionCommand` string `createMenuItem` requires is therefore
just inert metadata under this project's convention; reuse the field name
(e.g. `"newWorkspaceMenuItem"`) for it so it stays useful for logging/
debugging without implying a dispatch mechanism that isn't actually used.

## Gaps found (handle locally, don't route around them upstream)

- **No icon support anywhere in `Action`/`ElementFactory`.** Not relevant
  for this phase (dis6502's main menu has no icons), but if a future
  toolbar phase wants icons, that will need a new, separate mechanism -
  confirmed by reading every `Action`/`ElementFactory` usage in the
  codebase; none of them set an icon this way.
- **No `createCheckBoxMenuItem`.** dis6502's View menu has three
  `JCheckBoxMenuItem`s (`displayAsScreenCodeMenuItem`, `noDisassemblyMenuItem`,
  `doubleFontHeightMenuItem`). `ElementFactory` has no factory method for
  them, but the exact idiom for this case already exists elsewhere in WUDSN
  base itself (`AttributeTableColumnChooser.java:150-152`):
  ```java
  JCheckBoxMenuItem item = new JCheckBoxMenuItem();
  ElementFactory.setButtonTextAndMnemonic(item, action);
  ```
  Use this for the three checkbox items; call `.setAccelerator(...)`
  manually afterward only if the corresponding `Action` ends up with one
  (none of the three currently have one in the C++ `.rc`, so this is moot
  today).
- **Dynamic (MRU) submenu items stay untouched.** "Recent Workspaces"/
  "Recent Files" are static `JMenu` headers (→ `ElementFactory.createMenu`),
  but their child items are generated at runtime by `MRUController` from
  the current MRU list - there is no fixed label to put in a `.properties`
  file for those. `TheCartStudio`'s own recent-files submenu is built the
  same way (plain `new JMenuItem(text, mnemonicChar)` inside a
  `MenuListener`, not through `ElementFactory`) - leave
  `MRUController.fillMenu` exactly as it is.
- **`com.wudsn.tools.dis6502.Text` must not be touched or confused with the
  new class.** `Text` extends `org.eclipse.osgi.util.NLS` (Eclipse's own
  NLS, string-only, different loading mechanism); the new `Actions` class
  must extend `com.wudsn.tools.base.repository.NLS` instead. They are
  unrelated, coexist without conflict, and need no dependency changes
  (`com.wudsn.tools.base` is already declared in `pom.xml`).

## Step-by-step migration plan

1. **Create `com.wudsn.tools.dis6502.Actions`** (new file,
   `src/com/wudsn/tools/dis6502/Actions.java`), extending
   `com.wudsn.tools.base.repository.NLS`, one `public static Action` field
   per main-menu item that needs one - top-level menus this app defines
   itself (`MainMenu_Equates`, `MainMenu_View` - `File` and `Help` can reuse
   `com.wudsn.tools.base.Actions.MainMenu_File`/`MainMenu_Help` as-is, the
   same way both example apps do) plus every leaf item and every non-MRU
   submenu header (`MainMenu_File_OpenFile`, `MainMenu_File_AddFile`,
   `MainMenu_File_RecentWorkspaces`, `MainMenu_File_RecentFiles`, etc.).
   Field naming: `MainMenu_<TopMenu>_<Item>` (nested submenus flatten to one
   more `_<SubMenu>` segment, matching the examples' own convention).
   Pre-initialize every field whose C++ `.rc` entry has a `\tAccelerator`
   hint with `new Action(KeyEvent.VK_X, KeyStroke.M1 [| KeyStroke.M2])`;
   leave every other field as a bare `public static Action Name;`
   declaration. End with:
   ```java
   static { initializeClass(Actions.class, null); }
   ```

2. **Create `src/com/wudsn/tools/dis6502/Actions.properties`**, one
   `<FieldName>.label=` (and `.toolTip=` where useful) line per field,
   copying the label text and `&` placement verbatim from `dis6502.rc`'s
   `MAIN_MENU` block (drop the `\tAccelerator` suffix - that's now data on
   the `Action`, not the label). No locale variant needed (matching
   `Text.properties`, which is English-only for the same reason).

3. **Rewrite `MainMenu.java`'s four `createXxxMenu()` methods** to build
   each component via `ElementFactory.createMenu(action)`/
   `ElementFactory.createMenuItem(action, actionCommand)` instead of
   `new JMenu("...")`/`new JMenuItem("...")`, assigning the result to the
   **same existing public field** (`newWorkspaceMenuItem = ElementFactory
   .createMenuItem(Actions.MainMenu_File_NewWorkspace, "newWorkspaceMenuItem");`).
   Field types, field names, and their public visibility do not change, so
   **`Dis6502.java`'s entire menu-wiring block requires zero changes** -
   verify this by diffing before/after; if any wiring line needs to change,
   something about the field's type or name drifted and should be fixed
   back rather than accepted.

4. **Build the three `JCheckBoxMenuItem`s manually** per the "Gaps"
   section above, still assigned to the same existing fields.

5. **Leave `recentWorkspacesMenu`/`recentFilesMenu` as `JMenu`s built via
   `ElementFactory.createMenu(...)`** (their header text is static) but
   change nothing about how `MRUController.fillMenu` populates their
   children.

6. **Remove `MainMenu.java`'s now-redundant manual `.setAccelerator(...)`
   calls** - `createMenuItem` sets the accelerator from the `Action` now.

## Verification

- `mvn -o compile` / `mvn -o test-compile` - this is a UI-construction-only
  change with no model-layer impact, so the existing `TestRunner` suite is
  unaffected and does not need new tests.
- A throwaway smoke test (per this project's established convention, in
  `C:\TEMP\claude\jdis6502-smoketest\`, never committed) that constructs a
  real `MainMenu`, walks every `JMenu`/`JMenuItem` in `menuBar`, and asserts:
  every leaf item's `getText()` contains no stray `&`, every item that had
  an accelerator in `dis6502.rc` has a non-null `getAccelerator()` matching
  it, and every item's `getMnemonic()` is set to the character that
  followed `&` in its source label - this directly catches the two most
  likely mistakes (a missing `&` causing the fail-fast `RuntimeException`
  at construction, or a copy-paste mismatch between the `.properties` label
  and the intended mnemonic).
- Visual check: run the app, open every top-level menu, confirm mnemonic
  underlines and accelerator hints render as expected and match
  `dis6502.rc`'s `MAIN_MENU`/`ACCELERATORS` tables.
- Confirm `Dis6502.java` needed no changes (see step 3) - if it did, that's
  a sign the field contract was broken, not that `Dis6502.java` needed
  "updating" to match.

## Out of scope

- The three popup menus were done afterwards, the same way (see `Actions`'
  class javadoc for the two decisions taken: popup items keep only the
  accelerators that were live, and items without an `&` in the C++ source
  got a newly chosen mnemonic).
- Any toolbar (icons - `Action`/`ElementFactory` have no icon support at
  all, as noted above) - still open, no toolbar exists in this port yet.
- Dialogs (`ButtonBar_OK`/`ButtonBar_Cancel`/etc. from the shared
  `com.wudsn.tools.base.Actions` are ready to reuse there, matching
  `SimpleDialog`/`StandardDialog`/`ModalDialog`'s own existing usage in
  `com.wudsn.tools.base.gui`) - still open.
