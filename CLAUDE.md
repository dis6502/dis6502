# DIS6502 (Java port)

This is the Java/Swing successor (starting with version 4.0) to the C++/Win32
`DIS6502` disassembler, being ported class-by-class from the original source.

- **C++ source being ported**: `C:\jac\system\Windows\Programming\Repositories\dis6502\src`
- **Shared WUDSN Java library** (`com.wudsn.tools.base`/`.base.atari`, a
  separate Maven module this project depends on): `C:\jac\system\Java\Programming\Repositories\WUDSN-Base`

## Planning and guidance documents

The [`plans/`](plans/) folder holds every planning/guidance Markdown file
written while working on this port. Read these before starting nontrivial
work here - they capture hard-won process lessons, established conventions,
and the reasoning behind past architectural decisions, so they don't have to
be rediscovered:

- [`plans/PORTING_GUIDE.md`](plans/PORTING_GUIDE.md) - the main one. Guidance
  for redoing/continuing this port: strategy, environment/tooling setup
  (build and test commands live here), coding conventions to carry forward,
  the bug-handling policy for when the C++ source itself looks wrong, testing
  strategy, and a process lesson on when to ask before deciding silently.
- [`plans/RULES_WUDSN_BASE.md`](plans/RULES_WUDSN_BASE.md) - standing rules
  for using WUDSN Base's repository/`Action`/`ElementFactory` pattern for
  menu items, buttons, and other labeled Swing components.
- [`plans/POPUP_MENU_ACCELERATORS_PLAN.md`](plans/POPUP_MENU_ACCELERATORS_PLAN.md) -
  history of wiring up popup-menu keyboard accelerators via
  `InputMap`/`ActionMap` instead of relying on `JPopupMenu` item accelerators,
  which only fire while that specific popup is open (status: done).
- [`plans/MEMORY.md`](plans/MEMORY.md) - a plain-text export of the durable,
  project-specific lessons Claude has accumulated in its own memory while
  working on this port (state ownership, Swing UI conventions, porting
  fidelity rules), so the same guidance is visible to anyone working in this
  repository, not just a future Claude session.

- [`plans/FINAL_GAP_ANALYSIS.md`](plans/FINAL_GAP_ANALYSIS.md) - the second,
  entry-point-driven audit of C++ features missing in the port; every gap
  in it is closed (status: done).
- [`plans/FURTHER_IMPROVEMENTS.md`](plans/FURTHER_IMPROVEMENTS.md) - what is
  left now that porting is over: localization, known limits, test coverage
  gaps, housekeeping, possible improvements. Completed items are removed
  from it, not struck through.
- [`plans/UI_SMOKE_TESTS_PROPOSAL.md`](plans/UI_SMOKE_TESTS_PROPOSAL.md) -
  which of the throw-away UI smoke programs became repository tests, and
  what that needed (settings isolation, test access to `Dis6502`)
  (status: done).
- [`plans/REMOVE_CPP_PROVENANCE_PLAN.md`](plans/REMOVE_CPP_PROVENANCE_PLAN.md) -
  removing "Ported from"/C++-comparison framing from code comments now that
  the port is the final codebase, while preserving the design rationale
  some of those comments carry (status: done).
- [`plans/RULES_SYSTEM_SUBPACKAGES_PLAN.md`](plans/RULES_SYSTEM_SUBPACKAGES_PLAN.md) -
  standing rules for where computer-system-specific code lives: the
  `model.system`/`model.system.<name>` subpackage split (mirroring the
  C++ source's `systems/<name>/` folders), what stays in `model/` instead,
  why `ui/` stays flat, and how to add a new computer system.
- [`plans/HEADLESS_ACTIONS_TEST_PROPOSAL.md`](plans/HEADLESS_ACTIONS_TEST_PROPOSAL.md) -
  expanding headless test coverage now that WUDSN Base's `KeyStroke` no
  longer throws when loading `Actions` on a display-less JVM: `MainMenu`
  and every panel now get checked (including menu-mnemonic uniqueness)
  on every CI run, not just with a display (status: done).
- [`plans/TESTRUNNER_NAME_REFLECTION_PROPOSAL.md`](plans/TESTRUNNER_NAME_REFLECTION_PROPOSAL.md) -
  whether `TestRunner`'s `runTest("ValueSetsTest", ValueSetsTest::testValueSets)`
  pattern can drop the redundant string via reflection; options considered
  and a `SerializedLambda` proof-of-concept, decided against changing it
  (status: decided, kept as-is).
- [`plans/ELEMENTFACTORY_DATATYPE_BUTTONS_PROPOSAL.md`](plans/ELEMENTFACTORY_DATATYPE_BUTTONS_PROPOSAL.md) -
  moving every `DataType`-keyed self-labeling helper
  (`createCheckBox`/`createRadioButton`/both `applyLabel` overloads) into
  `ElementFactory`, removing `ElementUtilities`'s local reimplementations
  and three duplicated wrapper methods across 26 call sites in
  `ProfileDialog`/`SegmentPropertiesDialog`/`MemoryInspectorFindStringDialog`/
  `LowHighByteDialog` (status: done).

New planning/guidance documents for future work should also go in `plans/`.
