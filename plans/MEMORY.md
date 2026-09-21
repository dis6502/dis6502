# Session Memory

This file is a plain-text export of the durable, project-specific lessons
Claude has accumulated while working on this port, normally kept privately
in Claude's own per-project memory store (outside this repo). It exists here
so the same guidance is visible to anyone (human or AI) working in this
repository, not just a future Claude session. Written after the fact, so it
reads as guidance rather than a session transcript; a couple of entries
below note where a later decision superseded an earlier one recorded
elsewhere in Claude's memory.

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

### Split user-visible text into `Text.java` vs. `Texts.java` vs. `Messages.java` by provenance and shape

User-visible strings are split across three separate repository classes,
based on where the string actually comes from and whether it needs a
severity.

`Text.java` (singular) holds only real ported `IDS_*` constants: strings
that mirror an entry in the C++ source's Windows `STRINGTABLE` resource
block (`Resource.h`'s `IDS_*` IDs, with text from `dis6502.rc`). Every field
there should trace back to a real C++ resource ID.

`Texts.java` (plural, a separate class/file) holds UI text this Java port
introduces itself, with no real C++ resource to mirror - dialog titles,
labels, and body text that exist only because a Swing dialog needed them
(such as `AboutDialog`'s window title, bold product-name label, subtitle,
and body text). Fields there are named per dialog using a
`<DialogName>_<Purpose>` convention (e.g. `AboutDialog_WindowTitle`,
`AboutDialog_Title`, `AboutDialog_Subtitle`, `AboutDialog_Text`), matching
the same convention the sibling WUDSN tool `com.wudsn.tools.thecartstudio.Texts`
already uses for its own dialog-local strings (its `AboutDialog_Content`/
`AboutDialog_URL` fields).

`Messages.java` (a third class/file, added later than the other two) holds
`com.wudsn.tools.base.repository.Message`-typed fields, not plain `String`s,
for a message that needs a severity attached (status/info/error) - the same
pattern `com.wudsn.tools.base.Messages` already uses elsewhere in the WUDSN
ecosystem. A field's own leading letter encodes its severity at class-load
time (`S`=`Message.STATUS`, `I`=`Message.INFO`, `E`=`Message.ERROR`),
followed by a plain sequence number (e.g. `I001`) shared across every
severity - the numbering does not restart at 1 per letter, it just
continues on from whatever number came before it regardless of severity
(so the six `E`-prefixed fields added right after `I001` are numbered
`E002`-`E007`, not `E001`-`E006`) - not a ported C++ resource ID, even for
a message whose text did originally come from one (`Messages.I001` itself
was moved here from a real ported `Text.IDS_LOG_BETA_MESSAGE`, and
`Messages.E002`-`E007` from six real ported `Text.IDS_ERR_*` constants,
on explicit user instruction each time, once the message needed its
severity to actually drive dispatch). Send a `Messages.*` field via
`Application.sendMessage(Message, String...)`, which reads
`message.getSeverity()` to pick the right log method itself, instead of the
caller choosing `sendInfoMessage`/`sendErrorMessage`.

When adding a new dialog or new user-visible text to this project going
forward: if the string is a faithful port of a real C++ `STRINGTABLE`
entry and doesn't need a severity, it belongs in `Text.java`/`Text.properties`.
If it's new text this Java port itself introduces (including replacing a
literal string that used to be hardcoded in a dialog's constructor), it
belongs in `Texts.java`/`Texts.properties`, named `<DialogName>_<Purpose>`
(or `<ClassName>_<Purpose>` for a non-dialog owner). If it's a log/status/
error message that needs a severity, it belongs in
`Messages.java`/`Messages.properties` instead, named with the
severity-letter-plus-number convention above, regardless of whether the
text traces back to a C++ resource.

### Which NLS base class to use

`Text.java`, `Texts.java`, and `Messages.java` all extend
`com.wudsn.tools.base.repository.NLS` and initialize with
`initializeClass(ClassName.class, null)` in a static initializer, with a
matching `.properties` file next to the class on the classpath
(package-relative path, e.g. `com/wudsn/tools/dis6502/Text.properties`).
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

### No visible border/bevel decoration on part panels inside `JSplitPane`s

The main window's part panels (`SegmentListPanel`, `MemoryInspectorPanel`,
`DisassemblyPanel`, `XRefPanel`, `LogPanel` - the panels `MainWindow` nests
inside `JSplitPane`s) should not draw their own visible border or
bevel/etched box decoration. The `JSplitPane` dividers already separate
these panels visually, so a `JScrollPane`'s look-and-feel-default border (an
etched/sunken line) or a `BorderFactory.createTitledBorder(String)`'s
look-and-feel-default box (a bevel look, on this project's native Windows
look and feel) just duplicates that separation with a redundant line right
next to each divider.

The fix is to give each `JScrollPane` an explicit
`BorderFactory.createEmptyBorder()` instead of leaving its border unset,
and to build any `TitledBorder` with an explicit empty base border
(`BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), title)`)
rather than the plain `BorderFactory.createTitledBorder(String)` overload,
which falls back to the L&F's default border at paint time. This keeps
title text where a panel has one, while dropping the surrounding box/line
entirely. Any new part panel added to the main window layout should follow
the same convention.

### Prefer a real font over custom bitmap painting, but verify it renders correctly on real screen output

When a UI component needs to render a computer system's authentic character
set (ATASCII, PETSCII, etc.), check whether a real, correctly-licensed
TrueType font is available before defaulting to custom bitmap-painting.
Earlier in this port, Atari/C64 character rendering was implemented by
hand-rasterizing the C++ source's legacy `.fon` files into PNG glyph atlases
and blitting tinted sub-images with custom `Graphics2D` code in every panel
that needed text. Switching to `Font.createFont` plus standard
`Graphics2D.drawString` calls (with a font-specific byte-to-codepoint
mapping worked out per font, verified by rendering and comparing against
known-correct references) turned out both simpler and more maintainable.

One caveat found later: a plain `component.setFont(...)` plus a default
Swing renderer is *not* sufficient on real screen output. Real on-screen
Windows ClearType/subpixel antialiasing blurs this small pixel-art font
into illegible dots, even though the same rendering looks fine on the
offscreen images used to develop and verify the font support. Every panel
that shows this font (`SegmentListPanel`, `XRefPanel`,
`HexGridPanel`, `DisassemblyGridPanel`, `LogPanel`) works around
this the same way: paint the text explicitly via `ComputerFont#drawText`
(which disables antialiasing before drawing) through a small custom cell
renderer (`ComputerFontListCellRenderer`, shared by the `JList`-based
panels) or an equivalent client property, instead of relying on the
default renderer's own text painting.

### Verify Swing panel arrangement against the C++ layout code, not a plausible-looking guess

Don't assume a superficially reasonable Swing layout (e.g., grouping two
panels into a `JTabbedPane` because they seem related) matches the
original. One mistake found this way: `MainWindow.java` had tabbed
`MemoryInspectorPanel` and `XRefPanel` together next to a full-height
`DisassemblyPanel`, but the actual C++ layout code (`ui/Layout.cpp`'s
`Layout::Compute`) stacks the segment list above the memory inspector in
one column and the disassembly view above the cross-reference list in
another - the memory inspector and cross-reference list are never tabbed
together; they occupy different corners and are always simultaneously
visible. The fix was to read `Layout::Compute` directly (which window
occupies which `left`/`top`/`width`/`height` relative to the others) and
replicate that stacking/adjacency structure with nested `JSplitPane`s,
rather than inventing a plausible-looking arrangement.

For any work that touches panel/dialog arrangement, check the corresponding
C++ layout code first and verify the result with a rendered screenshot, the
same way logic ports are verified against the C++ source and a real build.

## Porting fidelity

### Preserve original C++ comments when porting

Carry over comments from the original C++ source into the ported Java file
rather than dropping them during translation - this was flagged after
`AddressLabel.java` lost the C++ header's inline field comments (e.g.
`Memory::address address; // address of the label`), even though the
comments were short and might otherwise look like restatements of the
obvious.

This overrides the general "don't write comments unless the why is
non-obvious" default for this specific porting task: the goal of the port
is fidelity to the original source, so even a trivial-looking C++ comment
should be kept (translated to a Javadoc `@author`-style block, a leading
`/** */`, or a trailing `//` on the corresponding Java field/method,
whichever reads naturally) unless it is genuinely stale or contradicts the
ported code. This applies retroactively - already-ported files that dropped
comments should be revisited and fixed, not just future ports done going
forward.

### Atari/C64 character-set rendering is a correctness gap, not a cosmetic one

Do not categorize font/character-rendering differences between the C++
source and this port as merely cosmetic polish. The Atari 8-bit family
(ATASCII) and Commodore 64 (PETSCII) use custom bitmap character sets whose
graphics/inverse-video glyphs have no equivalent in any standard system
font - a byte value that maps to one of those glyphs literally cannot be
shown correctly by substituting a normal monospace font, unlike, say, a
font *size* or *family* preference. Any gap involving these systems'
character rendering (the memory inspector's ASCII/ATASCII column, the
disassembly panel's font, etc.) is a functional/correctness issue affecting
whether the tool can display real program data at all, not a "nice to
have" visual improvement, when scoping or prioritizing future work on this
port.
