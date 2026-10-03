# dis6502 - Development Guide

How to build and test this repository, and the conventions that apply to
new work. The port from the C++ original is finished (2026-09-21): this Java
codebase is the final one and diverges from C++ wherever that serves the
Java code, so match the existing Java code, not the C++. The fidelity-era
porting process this file used to describe is in its git history.

More standing rules: [`MEMORY.md`](MEMORY.md) (state ownership, NLS and
`ValueSet` patterns, persisted preferences, file conventions),
[`RULES_WUDSN_BASE.md`](RULES_WUDSN_BASE.md),
[`RULES_LOCALIZATION.md`](RULES_LOCALIZATION.md) and
[`RULES_SYSTEM_SUBPACKAGES_PLAN.md`](RULES_SYSTEM_SUBPACKAGES_PLAN.md).

## 1. Build and test

- **Build:** `mvn -o compile` / `mvn -o test-compile` from the repository
  root.
- **Test:** `mvn -o test` (and every phase after it, so `package` too) runs
  the whole `TestRunner` suite through `TestRunnerTest`, a one-method JUnit 3
  bridge, and fails the build when a test does.
  - The UI tests need a display. They skip themselves without one, or with
    `-Ddis6502.skipUITests=true`, which the release workflow passes.
  - The MADS reassembly round trip (`ReassemblyRoundTripTest`) skips itself
    off Windows.
  - The tests write their settings to a throw-away `test` preferences node
    (`TestRunner.run`), never to the user's.
- **Running `TestRunner` directly** is faster for iteration. The classpath
  needs `target/classes`, `target/test-classes` and the two WUDSN Base jars
  from the local Maven repository:

  ```
  java -cp "target/classes;target/test-classes;C:/Users/JAC/.m2/repository/com/wudsn/tools/com.wudsn.tools.base/1.0.0-SNAPSHOT/com.wudsn.tools.base-1.0.0-SNAPSHOT.jar;C:/Users/JAC/.m2/repository/com/wudsn/tools/com.wudsn.tools.base.atari/1.0.0-SNAPSHOT/com.wudsn.tools.base.atari-1.0.0-SNAPSHOT.jar" com.wudsn.tools.dis6502.TestRunner
  ```

  Add `-Ddis6502.skipUITests=true` for a headless run. Run both variants
  before committing a UI change.
- **Git Bash classpaths must use Windows-style paths** (`C:/...`), not
  `/c/...`. A Unix-style entry fails silently to resolve classes instead of
  reporting an error.
- **A WUDSN Base source change needs a reinstall** before dis6502 sees it.
  dis6502 uses `com.wudsn.tools.base`/`.base.atari` as jars from the local
  Maven repository, so run `mvn -o install -DskipTests` in
  `WUDSN-Base/com.wudsn.tools.base` (and `.base.atari` if it changed).
  Forgetting this silently keeps testing against the stale jar.
- **Release builds** run in GitHub Actions (`.github/workflows/release.yml`,
  six-way x64/ARM64 matrix for Windows, Linux and macOS - see
  [`11_RELEASE_MATRIX_PROPOSAL.md`](11_RELEASE_MATRIX_PROPOSAL.md)).
  `build/trigger-release-workflow.ps1` starts a test build or republishes a
  release without pushing a tag.

## 2. Testing practice

- Run the full suite after every change, not just at the end of a feature.
- Assert **hand-computed expected values** (exact pixel colors, byte
  offsets, formatted strings), not just "doesn't throw". A no-crash test
  passes on subtly wrong logic.
- When a test fails, check whether the *test's* assumption is wrong before
  assuming the code is - e.g. a test that assumes a trace stops at a
  boundary the real algorithm doesn't stop at.
- Real-display tests and ad hoc smoke tests run under the **native look and
  feel**, like the application (`Dis6502.setNativeLookAndFeel()`, called by
  `TestRunner.run()`; a smoke test that starts the app via `Dis6502.main`
  gets it for free). Under Swing's "Metal" default, screenshots don't match
  the real screen, and L&F-specific bugs stay hidden.
- **Ad hoc smoke tests** belong in a scratch directory outside the
  repository and are never committed. They cover interactive behavior the
  suite can't reach; the ones worth keeping became repository tests (see
  [`04_UI_SMOKE_TESTS_PROPOSAL.md`](04_UI_SMOKE_TESTS_PROPOSAL.md)).
- **AWT clipboard access is blocked in a sandboxed shell.** Treat
  `HeadlessException`/`AWTError`/`IllegalStateException` from the clipboard
  as an environment limit there, and verify the logic that feeds the
  clipboard separately.

## 3. Code conventions

- **UI wiring:** a panel or menu exposes its items as public fields; the
  controller (`Dis6502`) attaches the listeners and performs the file,
  dialog and disassembly-refresh side effects. Panels stay free of
  application-level orchestration. A dialog does its own work in its
  `performOK()` instead of needing a separate controller class.
- **Enablement in one place per menu:** each menu or popup has one method
  that sets every item's enabled state (`Dis6502.updateFileMenuState()`,
  `MemoryInspectorPanel.updatePopupMenuItemsState()`, ...), instead of
  checks scattered across the action handlers. That keeps it auditable as
  commands are added.
- **Idiomatic Swing over literal structure:** prefer one generic loop over
  near-identical per-case code, and standard components (`JScrollBar`,
  `JSpinner`) over custom controls that reimplement them. Judge by the
  behavior the user sees.
- **Document scope decisions next to the code:** when a subsystem
  deliberately leaves something out or simplifies it, say what and why in
  the class javadoc, so it isn't re-decided for every command.

## 4. Process

At a genuine scope fork - a design decision with more than one defensible
answer - ask explicitly instead of deciding silently, and treat the answer
as durable guidance for that whole feature area unless told otherwise.
