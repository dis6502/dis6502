# DIS6502 (Java port)

This is the Java/Swing successor (starting with version 4.0) to the C++/Win32
`DIS6502` disassembler, being ported class-by-class from the original source.

- **C++ source being ported**: `C:\jac\system\Windows\Programming\Tools\dis6502\src`
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
- [`plans/ACTIONS_ELEMENT_FACTORY_MIGRATION.md`](plans/ACTIONS_ELEMENT_FACTORY_MIGRATION.md) -
  history of adopting WUDSN Base's `Action`/`Actions`/`ElementFactory`
  pattern for menu items and dialog buttons (status: done).
- [`plans/POPUP_MENU_ACCELERATORS_PLAN.md`](plans/POPUP_MENU_ACCELERATORS_PLAN.md) -
  history of wiring up popup-menu keyboard accelerators via
  `InputMap`/`ActionMap` instead of relying on `JPopupMenu` item accelerators,
  which only fire while that specific popup is open (status: done).
- [`plans/MEMORY.md`](plans/MEMORY.md) - a plain-text export of the durable,
  project-specific lessons Claude has accumulated in its own memory while
  working on this port (state ownership, Swing UI conventions, porting
  fidelity rules), so the same guidance is visible to anyone working in this
  repository, not just a future Claude session.

New planning/guidance documents for future work should also go in `plans/`.
