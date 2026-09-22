This file is the README.txt of the last Windows version of DIS6502 before the Java port.
The bug below may, or may not, be fixed in the Java version already.

Features
  Support form Atari 400/800/XL/XE ROM image files of type "*.car" with "CART" header
  Support for Atari 5200 ROM image files of size 4k, 8k, 16k, 32k. No ".car" or 40k or more support yet.


Known Issues in DIS6502 BETA
============================
- Properties dialog for Segments does not work
Classifying bytes as "Code with Low Byte"/"Code with High Byte" does not trigger the generation of an "<L1234" label. This does works in SDX_STABLE.
- https://atariage.com/forums/messenger/439626/

Open Bugs:
- if the profile is changed from default to MAC65 while an (old) workspace is loaded, the program crashes with "disassembling - please wait, pass 4 segment 1"
- http://atariage.com/forums/topic/286803-dis6502-the-interactive-6502-disassembler/?p=4212900
 sorry, it was actually the graphics mode pull down menu that was not functioning within "select sprites", the scroll bars were still functioning the same as the old version. It will be another welcome fix for the scroll bar dragging however
- http://atariage.com/forums/topic/286803-dis6502-the-interactive-6502-disassembler/?p=4220425
  The 3.0 beta just crashes if you try to open a 64K binary., but it will load it as a raw file.
- http://atariage.com/forums/topic/286803-dis6502-the-interactive-6502-disassembler/?p=4225728
  I found another bug, when searching in the hexadecimal editor (control + f) it does not look for anything when searching by hex for example C921 
- http://atariage.com/forums/topic/286803-dis6502-the-interactive-6502-disassembler/?p=4227686
  I really like the feature of graying out of unused equates.  It does seem to do it with most equates on the binary file I'm testing with, but all the IO EQUATES and the DISPLAY LIST EQUATES are still showing although they are not referenced in the binary file.  Is this by design or possibly a bug?

Open Feature Requests:
- the only basic improvement I can see that was missed is "CBYTE" handling. 
  by: https://atariage.com/forums/profile/17384-kenames99/
  at: https://atariage.com/forums/topic/286803-dis6502-the-interactive-6502-disassembler/page/3/?tab=comments#comment-4539232
- copy and paste log not working in the log
- loading multiple (user) equate files does not work (feature request)
- have freely selectable font size for main window/source font
- Explicit comment feature (implemented, to be tested)
- Have separate profile settings for WORDNumberOfEquatesPerLine
  See this->wDirectiveWORDNumberOfWordsPerLine = 1; // Because WORDs often mean labels and lables now have "S01L0000" format


Change list for working version of DIS6502
==========================================

TODO:
- support saving Segments from Segment List for all platforms + error handling
- makemax label size >12
- TAB instead of space
- The only bug that I've come across is when you select the number of bytes per line in "OPTIONS > Output Format" to more than 32, the program crashes

TODO:
Replace "illegal instructions" checkbox by "Instruction Set" dropdown
"Dann wären da noch die erweiterten Befehle des 65C02 wie BRA, STZ,  BS0-7, BR0-7, SB0-7, RB0-7.
Dann hätten wir endlich den ersten Disassembler, der auch Speedy 1050 Programmcode disassemblieren könnte.""
Könnte man an- und abschaltbar machen wie die "illegal instructions".

TODO:
Add own type "Label-1 (Return Address)"

TODO:
Disassemble again when segment label prefix is changed
Add one space character between the dump ASCII display and the scroll bar

FIXED:

Albert:
Code with lo/high bytes does not implicitly create an "Lnnnn" and "LDA #>Lnnnn"

Ehrhard:
Wenn man in einer Zeile einen Kommentar bearbeiten oder einfügen will, 
wird statt dessen eine Zeile oberhalb der markierten Zeile eingefügt.
Eine solche Zeile kann man auch nicht wieder löschen.

2) Man sollte Definitionen (Equates) on-the-fly laden, speichern und
entladen können. Dafür sollte es so 3-5 Slots geben:

- OS Equates
- DOS / SpartaDOS (....) Equates
- Equates für die Firmware eines Peripheriegerätes
- programmspezifische Equates
- manuell bei der Disassemblierung erstelle Equates

3) Die Systemordner sollte man optional relativ definieren können:

- D:\....\ -> absolut
- ..\MyProject\ -> relativ (zum Ort, von wo aus DIS6502.EXE gestartet wurde)

Die Einstellungen sollten dann natürlich auch entsprechend relativ in
der .ini gepseichert werden (portable Benutzung)

4) Define/Edit User Equate from the Disassembly listing

Picture D1
- right clicking here should bring up the add/modify label menu
with the address already in the address field
(If this is not possible in the Disassembly window, it should be
possible in the Dump of segment window)

Picture D2
- right clicking here should bring up the add/modify label menu
despite the fact, that this is already a user defined one
(I may want to rename it again)

