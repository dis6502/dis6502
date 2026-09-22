# Porting dis6502 (C++) to jdis6502 (Java) - Guidance for a Future Attempt

**Status (2026-09-21): the porting phase is over.** The user has stated
explicitly that this Java codebase is the finished port - there will not be
another attempt, and from now on the Java version is expected to
**intentionally diverge** from the C++ original where that serves the Java
codebase's own quality/design, rather than treating C++ behavior as the
default answer to defer to. This reverses this document's own framing
(written when active fidelity to C++ was still the goal) and, most
concretely, section 4's bug-handling policy below, which assumed every
divergence needed to be justified against - and often mirrored back into -
the C++ source. Going forward:

- Do not treat matching C++ behavior/structure as the default; make the
  Java-appropriate design call on its own merits.
- Do not feel obliged to also fix a bug in the C++ source when fixing it in
  Java, or to file a C++-side `TODO:` before diverging.
- The environment/tooling setup (section 2), coding conventions (section 3,
  most of which describe good Java/Swing practice independent of C++), and
  testing strategy (section 5) below remain useful. Section 4's C++-mirrored
  bug-handling policy and this document's overall "port faithfully" framing
  are kept only as historical record of the convention that applied during
  the actual porting effort - not current guidance.

This document is guidance for redoing this port from scratch, written after
completing a first full attempt. It captures the process, conventions, and
pitfalls worth carrying forward - not a log of specific bugs that have
already been found and fixed (see each repo's commit history for those).

## 1. Strategy

- **Port the model/logic layer completely, with unit tests, before starting
  on `ui/`.** Every UI class should be able to assume the model layer
  underneath it is correct and tested. This attempt did that naturally, but
  it's worth stating as a hard rule up front rather than discovering it by
  experience.
- **Decide feature scope per subsystem up front**, not ad hoc as each
  dialog/command comes up. Before starting a subsystem (e.g. "the memory
  inspector's popup menu"), enumerate every command it offers and decide,
  for each: faithful port, deliberate simplification (and why it's
  equivalent), or intentionally skipped (and why). Write that decision down
  next to the code (a class-level javadoc paragraph works well) instead of
  re-deciding it mid-stream for each command individually.
- Match C++ source folders to Java packages 1:1 where practical, so a
  reviewer can always find "the C++ file this came from" without guessing.

## 2. Environment / tooling setup

Rediscovering these costs real time - confirm them again at the start of
the next attempt, since paths/versions may have drifted:

- **C++ build (verify a C++ fix actually compiles):**
  `& "C:\Program Files\Microsoft Visual Studio\<version>\Community\MSBuild\Current\Bin\MSBuild.exe" "<repo>\dis6502.sln" -t:dis6502 -p:Configuration=Debug -p:Platform=Win32 -m -v:minimal`
- **Java build:** `mvn -o compile` / `mvn -o test-compile` from the jdis6502
  repo root. **`mvn -o test`** (and every build phase after it, so `package`
  too) runs the whole `TestRunner` suite through `TestRunnerTest`, a
  one-method JUnit 3 bridge, and fails the build when a test does (since
  2026-09-22). The UI tests (`DialogTextsTest`, `RenderingTest`,
  `UIWiringTest`) need a display and skip themselves without one, or with
  `-Ddis6502.skipUITests=true` (what the CI workflow passes); the MADS round
  trip skips itself off Windows.
- **Running the unit test suite directly** (bypassing Maven's test runner,
  useful for fast iteration) needs a classpath with the right dependency
  jars, `target/classes`, and `target/test-classes` - confirm the exact jar
  names/versions in the local `.m2` repo before assuming an old command
  still works. The tests write their settings to a throw-away `test`
  preferences node (`TestRunner.run`), never to the user's.
- **Git Bash / `javac`/`java` classpaths must use Windows-style paths**
  (`C:/...`), not Unix-style (`/c/...`) - a Unix-style classpath entry
  silently fails to resolve classes instead of erroring clearly.
- **AWT clipboard access is blocked in the sandboxed Bash tool
  environment.** A smoke test that touches `java.awt.datatransfer.Clipboard`
  needs to catch `HeadlessException`/`AWTError`/`IllegalStateException` and
  treat that as an environment limitation, not a code bug - verify the
  logic feeding the clipboard independently instead.
- **Ad hoc smoke tests belong in a scratch directory outside both repos**
  (e.g. `C:\TEMP\claude\jdis6502-smoketest\`) and should never be committed.
  They're for verifying interactive/GUI-adjacent classes that the unit test
  suite can't reach headlessly.

## 3. Coding/architectural conventions to carry forward

- **NLS/text constants pattern** - see the `wudsn-texts-nls-pattern` memory
  for which base class to extend.
- **Preserve original C++ comments when porting** - see the
  `jdis6502-preserve-cpp-comments` memory.
- **UI wiring shape:** a `*Panel` class exposes its buttons as public
  `JButton` fields; a separate top-level controller class (`Dis6502` in
  this port) attaches `ActionListener`s and performs file/dialog/
  disassembly-refresh side effects. Keeps the panel itself free of
  application-level orchestration.
- **Fold simple C++ controller classes into the Dialog/Panel's own
  `performOK()`/action handler** rather than porting them as separate
  controller classes, when the C++ controller's only job was to drive that
  one dialog.
- **One consolidated button-enablement method** (this port's
  `updateActionButtonsState()`) covering every action's enabled/disabled
  state, rather than scattering enablement checks across each action
  handler. Much easier to audit for consistency as commands are added.
- **Prefer idiomatic Swing substitutions over mechanical 1:1 translation**
  when they produce identical behavior/output with meaningfully less code -
  for example, a custom Win32 control that repeats near-identical logic in
  a per-case `switch` should usually become one generic loop parameterized
  by the varying values, and a custom control that owns its own scrollbars
  should usually become external `JScrollBar`/`JSpinner` controls that push
  values in. Judge this by "does it produce the same behavior for the
  user," not by "does the code structure match the original."

## 4. Bug-handling policy (historical - see the status note at the top)

When the Java port's behavior would diverge from a literal translation of
the C++ because the C++ itself looks wrong:

- If the C++'s own intent is provable without running it - e.g. a `.rc`
  resource's button labels contradict what the code does, or the code's own
  comment describes a check it never implements - fix it in **both**
  languages. Verify the C++ fix with a real MSBuild build before committing
  it, and verify the Java side with the full test suite plus a targeted
  smoke test.
- If confirming the divergence would require interactive GUI testing that
  isn't available, leave a `TODO:` comment in the C++ source describing the
  issue precisely (grep-able, not just prose), and implement the
  evidently-correct behavior in Java without waiting on the C++ fix.
- Keep C++ fixes and Java changes as separate commits with detailed
  messages explaining the evidence for the bug, not just what changed.
- The two repos use different attribution conventions on commits - confirm
  current convention for each repo before committing (check recent commit
  history / any session-level instructions).

## 5. Testing strategy

- Run the full unit test suite after every change, not just at the end of a
  feature slice.
- For UI-adjacent or interactive classes, write smoke tests against
  **hand-computed expected values** (exact pixel colors, byte offsets,
  formatted strings), not just "doesn't throw an exception." A test that
  only checks for no-crash will pass on subtly wrong logic.
- Watch for smoke tests that bake in an assumption narrower than what the
  real algorithm actually does (e.g. assuming a trace/search stops at a
  boundary it doesn't actually stop at). When a test fails, check whether
  the *test's* assumption is wrong before assuming the *port* is wrong.

## 6. Process lesson

At a genuine scope fork - a design decision with more than one defensible
answer - ask explicitly rather than deciding silently, and treat the answer
as durable guidance for the rest of that feature area (not just the one
instance being discussed) unless told otherwise. (This used to be phrased
specifically around "what to port, how to handle a broken or questionable
upstream feature" - see the status note at the top: the porting phase is
over, so a fork is no longer "match C++ or diverge," it's just an ordinary
Java-side design decision.)
