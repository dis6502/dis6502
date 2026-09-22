The proposal is implemented in the suggested order. Six commits, 1289149 through ffb2d12; the working tree is clean, and mvn -o test now runs all 27 tests and passes.

1. Settings isolation (1289149)
- Application keeps its settings under a Preferences node given to its constructor. By default that is the user's node, or the child named by the new system property dis6502.settingsNode.
- TestRunner.run() sets that property to test before anything loads and removes the node afterwards, so even tests that start the real application write nowhere else.
- I checked your preferences after a full run: empty. The leftovers from before the isolation (an ATARI800 node with development paths and some empty nodes) are also gone.

2. DialogTextsTest (f769061, e4587f4)
- Constructs every dialog and panel and the main menu, walks the component trees, and rejects empty texts, unresolved property keys and unfilled {0} templates. It also checks the four ValueSetField dropdowns for their items, order and round trip.
- One finding: nothing in the ui package loads without a display. WUDSN Base's Actions asks the toolkit for the menu shortcut mask at class load. So all three UI tests skip when headless; the proposal's "headless where possible" was not possible.

3. UIWiringTest (22367ed)
- Starts the application once through Dis6502.main and runs the eight scenarios from the proposal against it, clicking real menu items and answering real dialogs and choosers.
- Dis6502 exposes its instance, workspace and main window to the package, plus a few perform methods, instead of reflection.
- Two timing lessons went into it: the disassembly's progress dialog pumps events, so a scenario waits for the outcome rather than for "one more event".

4. RenderingTest (3028900)
- The popup a synthesized right-click builds, on a jsr line and on an immediate line, including the submenu's check mark.
- Both grids painted into an off-screen image: the highlight colour in the right rows and nowhere else.

5. TestRunner in the Maven build (ffb2d12)
- TestRunnerTest is a one-method JUnit 3 TestCase that runs TestRunner.run(). JUnit 3 is the only framework whose Surefire provider exists offline. mvn test and mvn package now run the suite and fail when a test does; I verified both directions.
- Two guards for the CI build, which runs on three operating systems: the MADS round trip skips off Windows, and the UI tests skip with -Ddis6502.skipUITests=true, which the release workflow now passes. That is the one change to your CI; the Windows runner has a desktop but nobody watching, and I did not want a release to depend on it.

plans/PORTING_GUIDE.md has the new build and test instructions; the proposal document records what came out differently. The scratch programs in C:\TEMP are no longer needed.