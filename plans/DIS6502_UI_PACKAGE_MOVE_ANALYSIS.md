# Analysis: moving Dis6502 into the ui package

Status: analysis, 2026-09-23. Recommendation: don't move it. Nothing
below is implemented; this records the investigation and why.

## The premise, checked

`Dis6502.java` does reference `ui/` heavily - 25 `import
com.wudsn.tools.dis6502.ui.*` lines, 104 occurrences of a `ui` class name
in the file body. But it references `model` (including
`model.system`/`model.system.atari800`) even more heavily: 34 import
lines, 202 occurrences in the body. By both measures, Dis6502 leans
slightly *more* on the model layer than on `ui` - "many UI references" is
true, but it isn't the dominant relationship, so moving toward `ui`
specifically doesn't better reflect where the bulk of the class's
dependencies actually point.

`Dis6502.java` is also, by a wide margin, the single largest file in the
project: 1870 lines, versus 1108 for the largest `ui/` file
(`MemoryInspectorPanel.java`) and 327 for the largest other top-level
file (`Messages.java`).

## What a move would actually require

- **Import churn in `Dis6502.java` itself**: lose the 25 now-redundant
  `ui.*` imports (same package after the move), gain 3 new imports for
  the top-level siblings it actually uses from outside `ui` today
  (`Messages` - 27 uses, `Texts` - 46 uses, `CommandLineArguments` - 1
  use). Net *-22 import lines* - a smaller import block, not a smaller or
  simpler class; `Actions`, `Application`, `ApplicationSettingsSection`,
  `DataTypes`, and `ValueSets` are all unused by `Dis6502` despite living
  in the same package today, so there's nothing to gain there either.
- **~50 dangling javadoc links.** `Messages.java` and `Texts.java` carry
  roughly fifty unqualified `{@link Dis6502#method}` javadoc references
  (e.g. `{@link Dis6502#performOpenFile}`), which resolve today only
  because `Dis6502` is in the same package. Moving it would silently turn
  every one of them into an unresolvable link - not a compile error (as
  established earlier this session, `javac` doesn't validate `{@link}`
  targets by default), so nothing would flag the breakage; someone would
  need to add an import (or fully qualify, or switch to `{@code}`) in
  both files across all fifty call sites to keep the documentation
  genuinely useful.
- **Two build-config edits.** `pom.xml`'s `<mainClass>` and
  `.github/workflows/release.yml`'s `--main-class` (the macOS app-bundle
  step) both hardcode `com.wudsn.tools.dis6502.Dis6502` and would need the
  new FQCN. (`dependency-reduced-pom.xml` also has it, but it's
  gitignored/generated, so it needs no manual edit.) A `Main-Class`
  living inside a `ui` subpackage also reads a little oddly to anyone
  skimming the build config for the first time - a minor but real
  convention cost.
- **One test import.** `test/com/wudsn/tools/dis6502/UIWiringTest.java`
  is the only test referencing `Dis6502` (same package today); it would
  need one new import.

## What a move would *not* simplify

The hoped-for simplification would be: package-private access replacing
some of today's `public` fields once `Dis6502` sits inside `ui`. That
doesn't materialize:

- Every popup/menu item `Dis6502` wires is **already** a deliberately
  `public` field on its owning panel, specifically so a class outside
  `ui` can wire it directly - see `MemoryInspectorPanel`'s own class
  javadoc: "every item is a public `JMenuItem` field wired directly by
  `Dis6502`, never a hidden `JButton`...". This was a considered design
  choice already, not an accident of Dis6502's current package.
- Those same fields are read from **outside `ui/` a second time**, by
  `UIWiringTest` (`test/com/wudsn/tools/dis6502/UIWiringTest.java`, e.g.
  `menu.newWorkspaceMenuItem.isEnabled()`). Even if `Dis6502` moved into
  `ui` and some field were narrowed to package-private, that test - which
  has its own good reason to live in the top-level test package, not
  `ui`'s - would immediately need it to be public again. There is no
  member `Dis6502` reaches today that a move would let anyone narrow.
- No package-private member of any top-level sibling (`Messages`,
  `Texts`, `CommandLineArguments`) blocks `Dis6502` today either - every
  field it uses from them is already `public`. Checked directly: neither
  class has a single non-public static field.

## The layering argument

Nothing in `ui/` imports or references `Dis6502` (checked directly: zero
hits) - the dependency is entirely one-directional, `Dis6502` depending
on `ui`, never the reverse. That is exactly the shape a clean
application-controller-over-view split should have. `UIApplication`
already lives in `ui/` legitimately, but it's a narrow, genuinely
UI-specific specialization (routing log messages to a `LogPanel`);
`Dis6502` is a different kind of thing entirely - command wiring, file
I/O, command-line argument parsing, MRU handling, and model mutation
across the whole application, with `ui` as one of several things it
orchestrates alongside `model`. Moving the orchestrator into the package
it orchestrates blurs a distinction that costs nothing to keep today and
that the rest of the codebase (including the just-established
`UIWiringTest`/`ui`-test-package split) already relies on staying clear.

## Recommendation

Don't move it. The premise ("many UI references, so maybe things can be
simplified") doesn't hold up under measurement - model references
outnumber UI ones - and the move's only concrete effect is shrinking
`Dis6502.java`'s own import block by ~22 lines, at the cost of ~50
javadoc links to fix, two build-config edits, and a real (if modest) step
away from the codebase's existing controller/view separation.
