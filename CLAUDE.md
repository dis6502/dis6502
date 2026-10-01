# DIS6502 (Java port)

This is the Java/Swing successor (starting with version 4.0) to the C++/Win32
`DIS6502` disassembler, being ported class-by-class from the original source.

- **C++ source being ported**: `C:\jac\system\Windows\Programming\Repositories\dis6502\src`
- **Shared WUDSN Java library** (`com.wudsn.tools.base`/`.base.atari`, a
  separate Maven module this project depends on): `C:\jac\system\Java\Programming\Repositories\WUDSN-Base`

## Plan files

All plan files (e.g. files created via plan mode, task breakdowns, design plans,
proposals) must be placed in the [`plans/`](plans/) subfolder at the repository
root. Batch plans carry a two-digit prefix in creation order
(`09_CUSTOM_TEXT_FONT_PROPOSAL.md`); a new plan takes the next free number and
gets a row in [`plans/README.md`](plans/README.md), the index with each plan's
purpose and status. Read the relevant ones before starting nontrivial work here -
they capture hard-won process lessons, established conventions, and the
reasoning behind past architectural decisions.

The standing documents stay unnumbered:

- [`plans/PORTING_GUIDE.md`](plans/PORTING_GUIDE.md) - the main one. Guidance
  for redoing/continuing this port: strategy, environment/tooling setup
  (build and test commands live here), coding conventions to carry forward,
  the bug-handling policy for when the C++ source itself looks wrong, testing
  strategy, and a process lesson on when to ask before deciding silently.
- [`plans/MEMORY.md`](plans/MEMORY.md) - a plain-text export of the durable,
  project-specific lessons Claude has accumulated in its own memory while
  working on this port (state ownership, Swing UI conventions, porting
  fidelity rules), so the same guidance is visible to anyone working in this
  repository, not just a future Claude session.
- [`plans/RULES_WUDSN_BASE.md`](plans/RULES_WUDSN_BASE.md) - standing rules
  for using WUDSN Base's repository/`Action`/`ElementFactory` pattern for
  menu items, buttons, and other labeled Swing components.
- [`plans/RULES_LOCALIZATION.md`](plans/RULES_LOCALIZATION.md) - standing
  rules for texts and messages: where they go, punctuation, quoting.
- [`plans/RULES_SYSTEM_SUBPACKAGES_PLAN.md`](plans/RULES_SYSTEM_SUBPACKAGES_PLAN.md) -
  standing rules for where computer-system-specific code lives: the
  `model.system`/`model.system.<name>` subpackage split (mirroring the
  C++ source's `systems/<name>/` folders), what stays in `model/` instead,
  why `ui/` stays flat, and how to add a new computer system.
- [`plans/FURTHER_IMPROVEMENTS.md`](plans/FURTHER_IMPROVEMENTS.md) - what is
  left now that porting is over: localization, known limits, test coverage
  gaps, housekeeping, possible improvements. Completed items are removed
  from it, not struck through.
- [`plans/DIS6502_WIN32_TODOS.md`](plans/DIS6502_WIN32_TODOS.md) - the
  README of the last Windows version, with its known issues and open bugs.
