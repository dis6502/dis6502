# Rules: using WUDSN Base's repository/`Action`/`ElementFactory` pattern

`com.wudsn.tools.base` (a Maven dependency of this project) has an
established pattern for menu items, buttons, and other labeled Swing
components: static properties (label text with an embedded mnemonic
marker, tooltip, accelerator) live as one `Action` value in a repository
class, populated reflectively from a `.properties` file at class load;
components are then built by handing that `Action` to `ElementFactory`,
which applies the mnemonic/tooltip/accelerator consistently and fails
fast if a label is missing its mnemonic marker.

This project uses it for the main menu and all three popup menus
(`SegmentListPanel`, `DisassemblyPanel`, `MemoryInspectorPanel`) - see
`com.wudsn.tools.dis6502.Actions`' javadoc for the concrete result. A
future toolbar or dialog can reuse the same pattern.

## The pieces

- **`com.wudsn.tools.base.repository.Action`** - a plain data holder:
  label, tooltip, accelerator. Nothing else - no icon field, no command
  id, no enablement state, no listener.
- **`com.wudsn.tools.base.repository.NLS`** - a reflection-based
  static-field populator. A class extending it declares `public static`
  (non-final) fields (`String`, `Action`, `DataType`, `Message`, or
  `ValueSet`); calling `initializeClass(MyClass.class, null)` in a static
  initializer loads `MyClass.properties` (plus locale-suffixed variants)
  from the classpath next to the class, and for every `Action`-typed
  field looks up `<fieldName>.label` (mandatory) and `<fieldName>.toolTip`
  (optional), replacing the field with a fully-populated `Action` -
  **preserving the accelerator** if the field was already pre-initialized
  with one in Java source (`new Action(keyCode, modifiers)`). A missing
  mandatory `.label`, or a class that isn't `public`, aborts at load time
  - a fail-fast, load-time-checked mechanism, not a silent fallback.
  Every repository class in this project (`Actions`, `Texts`, `DataTypes`,
  `Messages`) extends this same `NLS` base and follows this same shape.
- **`com.wudsn.tools.base.Actions`** - a ready-made, app-independent
  repository with generic entries any Swing app can reuse as-is
  (`ButtonBar_OK`, `MainMenu_File`, etc.). This project's own
  `com.wudsn.tools.dis6502.Actions` supplies everything app-specific,
  same shape (own package, own `.properties` file), reusing the shared
  class's fields for the few top-level items that need nothing app-specific.
- **`com.wudsn.tools.base.gui.ElementFactory`** - builds an actual Swing
  component from an `Action`, and nothing else (no listener attachment,
  no icon):
  ```java
  public static JMenu createMenu(Action action)
  public static JMenuItem createMenuItem(Action action, String actionCommand)
  public static JCheckBoxMenuItem createCheckBoxMenuItem(Action action)
  public static JButton createButton(Action action, boolean withMnemonic)
  public static JToggleButton createToggleButton(Action action, boolean withMnemonic)
  public static void setButtonTextAndMnemonic(AbstractButton button, Action action)
  public static JLabel createLabel(DataType dataType, JComponent field)
  public static JCheckBox createCheckBox(DataType dataType)
  public static JRadioButton createRadioButton(DataType dataType)
  public static void applyLabel(JLabel label, DataType dataType, JComponent field)
  public static void applyLabel(AbstractButton button, DataType dataType)
  ```
  `createLabel`/`createCheckBox`/`createRadioButton`/`applyLabel` are the
  methods here keyed by `DataType` (a paired field's label text, or a
  self-labeled control's own text) rather than `Action` - use these, not a
  hand-rolled `&`-mnemonic parser, for any new self-labeled `JCheckBox`/
  `JRadioButton`, or to re-label an existing `JLabel`/button in place
  (e.g. `LowHighByteDialog`'s Low/High Byte swap) rather than constructing
  a new one.
  `createMenu`/`createMenuItem` require the label to contain a mnemonic
  marker and **throw at build time if it doesn't** - this is deliberate
  fail-fast validation, not something to route around. `createMenuItem`
  also pads the label with trailing spaces when the action has an
  accelerator, so Swing's accelerator hint doesn't collide with the text.
- **`com.wudsn.tools.base.gui.KeyStroke.M1`/`M2`/`M3`** - the
  platform-independent modifier constants for accelerators (`M1` = Ctrl
  on Windows/Linux, Cmd on macOS; `M2` = Shift; `M3` = Alt/Option). Use
  these instead of `InputEvent` masks directly, so accelerators are
  correct on macOS too.

## Standing rules

- **Keep this project's own wiring architecture.** Every menu/button item
  stays a public field, wired directly by its owning class with its own
  listener (`item.addActionListener(e -> performXxx())`). Do not adopt a
  shared-`ActionListener`-plus-command-dispatch style. The
  `actionCommand` string `createMenuItem` requires is therefore just
  inert metadata under this project's convention - reuse the field name
  for it (e.g. `"newWorkspaceMenuItem"`) so it stays useful for
  logging/debugging without implying a dispatch mechanism that isn't
  actually used.
- **No icon support anywhere in `Action`/`ElementFactory`.** A future
  toolbar that wants icons needs a separate mechanism.
- **Dynamic, runtime-generated items have no place in this pattern.** A
  submenu whose children are generated at runtime from changing data (an
  MRU list, for example) has no fixed label to put in a `.properties`
  file - build those items directly (`new JMenuItem(text, mnemonicChar)`)
  inside whatever listener regenerates them, not through `ElementFactory`.
  The submenu's own static header label still goes through
  `ElementFactory.createMenu(...)` as usual.
