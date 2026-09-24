# Session Memory

This file is a plain-text export of the durable, project-specific lessons
Claude has accumulated while working on this port, normally kept privately
in Claude's own per-project memory store (outside this repo). It exists here
so the same guidance is visible to anyone (human or AI) working in this
repository, not just a future Claude session. Written after the fact, so it
reads as guidance rather than a session transcript; a couple of entries
below note where a later decision superseded an earlier one recorded
elsewhere in Claude's memory.

## The porting phase is over (2026-09-21) - read this first

The user has stated explicitly, in these words: "The Java version will
from now on intentionally diverge the C++ original. This Java version is
the port. There will not be another attempt." Every entry below this one
was written while active fidelity to the C++ source was still the goal -
useful as a record of *why* the existing code looks the way it does, and
still generally sound Java/Swing engineering advice, but no longer a
mandate to keep matching C++. Concretely, going forward:

- Do not default to "match C++ behavior/structure" when making a design
  decision; decide what's right for the Java codebase on its own terms.
- Do not feel obliged to fix a bug in the C++ source when fixing it in
  Java, file a C++-side `TODO:`, or verify a C++-side build before
  committing a Java-side fix - see `plans/PORTING_GUIDE.md`'s now-historical
  section 4 for what the old policy required.
- A "genuine scope fork" is still worth asking the user about explicitly
  (per the process lesson later in this file) - it's just no longer framed
  as "match C++ or diverge from it," since diverging is now the norm, not
  a special case needing justification.
- This does not retroactively make already-completed work wrong; it's
  guidance for what comes next.

## Architecture and state ownership

### Give cross-cutting state its own class owned by Workspace, not flat fields on Workspace

A cohesive piece of state - even one that behaves like a workspace-wide
lock, blocking otherwise-unrelated commands across the whole application
while active - should get its own class, owned by `Workspace`
(`com.wudsn.tools.dis6502.model.Workspace`) as a `private final` attribute
constructed alongside `segmentList`/`profile`/`disassemblyResult`, not
spread out as flat primitive fields directly on `Workspace` itself.

This was learned in two steps while moving the Memory Inspector's in-place
hex/ASCII edit-mode state (cursor offset/pane, plus enter/quit/navigate/type
logic) out of the Swing `MemoryInspectorPanel` so it could be exercised by
headless unit tests:

1. The first attempt put the edit-mode fields and methods directly as flat
   fields/methods on `Workspace` (`isMemoryInspectorEditMode()`,
   `enterMemoryInspectorEditMode(...)`, etc.), reasoning that edit mode is a
   workspace-wide lock, not something tied to one selection object - and the
   user confirmed that framing (being conceptually workspace-wide is a real
   property of this state).
2. The user then rejected the flat-fields-on-`Workspace` *implementation* of
   that same idea: "I don't like that the memory inspector fields are now
   flat in the `Workspace` class." The fix was to give the state its own
   class - reusing/renaming the existing selection object (already a de
   facto global, workspace-scoped object, since every caller shares one
   instance) and merging the edit-mode fields/methods into it, with
   `Workspace` holding that single instance directly via
   `getMemoryInspectorState()` instead of either flattening the state onto
   `Workspace` or leaving it as a satellite object constructed separately by
   callers. That class was later split further into a `MemoryInspectorState`
   read-only interface plus a `MutableMemoryInspectorState` implementation,
   so UI code that only needs to *read* the state (e.g.
   `HexGridPanel`, then still named `MemoryInspectorGridPanel`) can depend
   on the narrower interface.

The durable takeaway: being conceptually "workspace-wide" does not mean a
class's fields should physically live on `Workspace`. `Workspace` should
stay a thin aggregate of cohesive sub-objects (`SegmentList`, `Profile`,
`DisassemblyResult`, `MutableMemoryInspectorState`, ...), each owning its
own related fields and methods; `Workspace` exposes them via `getXxx()`
accessors rather than absorbing their state directly.


### Which NLS base class to use

`Texts.java` and `Messages.java` both extend
`com.wudsn.tools.base.repository.NLS` and initialize with
`initializeClass(ClassName.class, null)` in a static initializer, with a
matching `.properties` file next to the class on the classpath
(package-relative path, e.g. `com/wudsn/tools/dis6502/Texts.properties`).
This is the same base class `com.wudsn.tools.thecartstudio.Texts` and this
project's own `Actions.java` already use.

This was not the original choice: an earlier decision had both classes
extend `org.eclipse.osgi.util.NLS` instead (the
`com.wudsn.tools.base.hello.standalone` pattern), following an explicit
redirect from the user at the time. The user later reversed that decision
and had both classes switched to `com.wudsn.tools.base.repository.NLS`,
matching `Actions.java`. Do not reintroduce `org.eclipse.osgi.util.NLS`
here - the `org.eclipse.osgi` Maven dependency was removed entirely once
nothing in the project still needed it (it was also a signed jar, which had
needed a dedicated shade-plugin filter to strip its `META-INF` signature
files when building the uber-jar - that filter was removed too once the
dependency was gone).

### For a enum-like type whose values are shown to the user, use the WUDSN Base `ValueSet` pattern

When the Profile dialog's encoding drop-down showed raw enum names (`UTF8`)
and an `EncodingInfo` lookup class was proposed, the user pointed to the
established mechanism instead: "Check the 'ValuesSets' pattern in 'WUDSN
Base' for handling enums with texts."

How to apply: the type is a class extending
`com.wudsn.tools.base.repository.ValueSet` (not a Java `enum`) with `public
static final` instances, a static `getValues()`, and a static initializer
ending in `initializeClass(TheType.class, ValueSets.class)` -
`com.wudsn.tools.base.atari.Platform` is the model, `model/Encoding.java` the
first one here. Texts live in the one shared `ValueSets.properties` next to
this project's `ValueSets` container class, keyed `SimpleClassName_ID`
(`Encoding_UTF8=UTF-8`); `toString()` returns the text, and
`com.wudsn.tools.base.gui.ValueSetField` is the ready-made combo box. A
`ValueSet` cannot be used in a `switch` or an `EnumMap` - use an `if` chain on
identity and a `HashMap`. The persistence key is the `id`, never the text.
The repository loader takes every `public static final` field of a value set
class for a value and fails at startup on anything else (a `String`
constant, say) - keep such constants private or turn them into methods.
String concatenation with a value now yields its display text; use
`getKey()` where the technical name is meant. More generally: before inventing
a text-lookup helper, look in WUDSN Base for an existing repository pattern
(`ValueSet`, `DataType`, `Action`, `Message`, `Texts`).

### Display texts never go into a type as string literals

User-visible texts must stay localizable. When the C++ `FileTypeInfo` table
was first folded into fields of a Java `enum FileType` with hard-coded English
texts, the user corrected it ("Keep separate FileType and FileTypeInfo to
support localization of the texts"); the `ValueSet` pattern above then turned
out to be the intended way to have both - one type, localizable texts - and
`FileType`, `FolderType`, `Encoding`, `ComputerSystemType`, `ProcessorType`
and `ui/GraphicMode` were all converted to it (with
`FileType`'s default extension, filter extensions and folder type as extra
attributes of each value, and its filter text simply being its
`FolderType`'s text). The rule that remains: no new hard-coded English UI
strings in model classes - texts come from `ValueSets.properties` (values of a
type), `Texts.properties`, `Messages.properties`, `Actions.properties` or
`DataTypes.properties`. Texts nobody selects from - error descriptions like
`AtariError.getErrorText()` - are not value sets but `Messages` entries.
Which enums deliberately stay enums: those that are never shown as text and
are used in `switch` statements or as file format values (`MemoryType`,
`FileHeader`, `OperandMode`, `FixupType`, `EquateType`,
`DisassemblySectionType`, the event kinds `WorkspaceProperty`/
`SegmentList.Property`, and internal state enums).

### Keep every persisted preference's key names and coded defaults in one dedicated class

A persisted user preference's settings-section name, preference key
name(s), and coded default value(s) belong in one dedicated class (e.g.
`Options`, `src/com/wudsn/tools/dis6502/Options.java`) - not as private
constants inside `Dis6502` (the class that reads/writes them via
`ApplicationSettingsSection`), and not duplicated as a separate constant
inside whatever dialog exposes the preference in the UI. Both the
persistence/lookup code and any UI code that needs to know a control's
coded default (e.g. a dialog resetting itself to defaults) read the same
fields off that one class, so the two can never drift apart; adding a
future preference then means adding one key/default pair there, not
touching multiple files.

This was learned when `TextFontFamily`/`TextFontSize` (the preference
`OptionsDialog` manages) initially had their settings-section name, key
names, and default value living as private constants in `Dis6502`, plus
a separate `DEFAULT_POINT_SIZE` constant duplicated in `OptionsDialog`
itself; the user asked for all of it to move into one new `Options`
class instead. Apply the same pattern to any future persisted
preference added to this project.

## Swing UI conventions

### Wire popup menu items directly - no hidden `doClick()` indirection

When porting a dis6502 C++ command to the Swing UI, do not wire a popup menu
item to trigger the same command by calling `someButton.doClick()` on a
corresponding toolbar button - not even when that button is real and
visible. This came up in `MemoryInspectorPanel`: first for items whose
toolbar buttons had been removed entirely (leaving them as pointless hidden
`JButton`s kept alive only to be `doClick()`d), then, once fixed there, for
every other popup item that *did* still have a visible, wired toolbar
button - the fix was wanted there too, not just for the hidden-button case.

The fix is to make the popup menu item itself a public field (e.g.
`JMenuItem editCommentMenuItem`) and have `Dis6502` attach the exact same
`ActionListener` (or call the exact same method) to it that it attaches to
the toolbar button - both trigger the real action directly, side by side,
rather than one forwarding to the other through Swing's event dispatch.
Enabled/disabled state should likewise be set directly on both the button
and the menu item together (e.g. in one `updateActionButtonsState` method),
not mirrored from one to the other. For a popup item with no single fixed
target - like a submenu where each item represents a different value
(`MemoryInspectorPanel`'s Change Type submenu, one item per `MemoryType`) -
expose a small listener/callback interface instead (matching the pattern
`XRefPanel` uses for its own selection callback), so the panel reports what
was picked without routing through a shared combo box or button at all.


### Dis602 stays in a package "model" and "ui"

Reason: It is the orchestrator that uses both packages equally.

## Testing conventions

### Real-display tests must use the native Look & Feel, same as the real app

The user stated this explicitly: "Tests shall also use the native Look &
Feel. there were situations when the test's screen capture didn't look
like the real screen." `Dis6502.main` already switches to the native L&F
via `setNativeLookAndFeel()`, so a smoke test that starts the whole app
that way gets this for free - but most of the repository's own
real-display tests (`PanelTextsTest`, `RenderingTest`,
`PopupStructureTest`, etc.) build their Swing components directly, so
they silently stayed on Swing's cross-platform "Metal" default, and a
screenshot taken during one of them did not look like what a real user
actually sees. Fixed by widening `setNativeLookAndFeel()` to
package-private and having `TestRunner.run()` call it once before
running any test.

This was not just cosmetic: switching to native L&F surfaced a real bug
in `UITest.checkTexts`'s look-and-feel-internals skip filter, which
matched only the `javax.swing.plaf` prefix (true for Metal's internals)
and missed `com.sun.java.swing.plaf.<lf>` (true for Windows/GTK/etc.'s
native internals, e.g. `WindowsScrollBarUI$WindowsArrowButton`) - fixed
by matching the `.plaf.` substring instead of a fixed prefix. General
lesson: a test environment that doesn't match the real runtime
environment can hide real bugs, not just produce a cosmetically wrong
screenshot - closing that gap is worth it even when it surfaces
short-term test failures. Apply the same native-L&F setup to any future
test that renders real Swing UI.

