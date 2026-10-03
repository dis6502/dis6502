# Plans

The plan files of this repository, one per batch of work, numbered in the
order they were created. A new plan gets the next free number. Each plan
carries its status in its first lines; this table is the overview.

The standing documents are not numbered: `DEVELOPMENT_GUIDE.md`, `MEMORY.md`,
`RULES_WUDSN_BASE.md`, `RULES_LOCALIZATION.md`,
`RULES_SYSTEM_SUBPACKAGES_PLAN.md`, `FURTHER_IMPROVEMENTS.md` and
`DIS6502_WIN32_TODOS.md` - see `CLAUDE.md` at the repository root.

| No. | Plan | Purpose | Status |
|---|---|---|---|
| 01 | [POPUP_MENU_ACCELERATORS_PLAN](01_POPUP_MENU_ACCELERATORS_PLAN.md) | Popup-menu keyboard accelerators via `InputMap`/`ActionMap`, since `JPopupMenu` item accelerators only fire while that popup is open | Done |
| 02 | [REMAINING_GAPS_OVERVIEW](02_REMAINING_GAPS_OVERVIEW.md) | First inventory of C++ features missing, incomplete or broken in the port | Done 2026-09-21 |
| 03 | [FINAL_GAP_ANALYSIS](03_FINAL_GAP_ANALYSIS.md) | Second, entry-point-driven audit of C++ features missing in the port | Done 2026-09-22 |
| 04 | [UI_SMOKE_TESTS_PROPOSAL](04_UI_SMOKE_TESTS_PROPOSAL.md) | Which throw-away UI smoke programs became repository tests, and what that needed (settings isolation, test access to `Dis6502`) | Done 2026-09-22 |
| 05 | [FABLE](05_FABLE.md) | Implementation report for 04: the commits, what each test covers, the findings | Report |
| 06 | [REMOVE_CPP_PROVENANCE_PLAN](06_REMOVE_CPP_PROVENANCE_PLAN.md) | Removing "Ported from"/C++-comparison framing from code comments while keeping the design rationale | Done 2026-09-23 |
| 07 | [HEADLESS_ACTIONS_TEST_PROPOSAL](07_HEADLESS_ACTIONS_TEST_PROPOSAL.md) | Headless test coverage of `MainMenu` and every panel, including menu-mnemonic uniqueness | Done 2026-09-23 |
| 08 | [ELEMENTFACTORY_DATATYPE_BUTTONS_PROPOSAL](08_ELEMENTFACTORY_DATATYPE_BUTTONS_PROPOSAL.md) | `DataType`-keyed self-labeling helpers moved into `ElementFactory` | Done 2026-09-23 |
| 09 | [CUSTOM_TEXT_FONT_PROPOSAL](09_CUSTOM_TEXT_FONT_PROPOSAL.md) | User-chosen text font and separate memory inspector font size in `View > Options...` | Done 2026-10-01 |
| 10 | [CHARACTER_SET_FONTS_PROPOSAL](10_CHARACTER_SET_FONTS_PROPOSAL.md) | 8x8 `.chr` character sets instead of the TTF glyphs, selectable in the memory inspector header | Done 2026-10-01 |
| 11 | [RELEASE_MATRIX_PROPOSAL](11_RELEASE_MATRIX_PROPOSAL.md) | x64 and ARM64 release builds for all three systems, following RASTER-Music-Tracker, plus application icons | Done 2026-10-02 |
| 12 | [SOURCE_TODOS](12_SOURCE_TODOS.md) | Inventory of the `TODO` comments left in the source, grouped by topic | Open |
