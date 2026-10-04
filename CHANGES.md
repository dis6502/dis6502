# Changes

## Version 4.0 (Java)

Version 4.0 is a rewrite of the Windows-only C++/Win32 version 3.6.1 in
Java/Swing. Everything 3.6.1 could do is still there; this list covers
what is new, changed or fixed compared with it. Known limits are in
[`plans/FURTHER_IMPROVEMENTS.md`](plans/FURTHER_IMPROVEMENTS.md).

### Platforms

- Runs on Windows, Linux and macOS, each for x64 and ARM64, as a
  portable application with its own Java runtime - nothing to install.
- Uses the native look and feel of the operating system, and renders
  sharply on high-resolution displays (HiDPI, display scaling).
- Application icon on all platforms, and a new About dialog with a
  clickable link.

### Opening files

- **File > Open Any File... / Add Any File...** detect the file type
  themselves. The command line, drag and drop and Recent Files use the
  same detection; a file that matches no type is offered as raw file.
- **Atari cartridges:** every Atari 800 and Atari 5200 cartridge type of
  atari800's `cart.txt` up to type 75 can be imported, raw or as `.car`
  file, up to 4 MB and except AST 32 KB - bank-switched ones included
  (XEGS, MegaCart, OSS, SpartaDOS X, Williams, Atarimax, SIC!, Bounty Bob,
  Atrax (decoded automatically), 5200 Super Cart and many more). Each bank
  becomes a segment at its address, titled with the type and bank number
  and with its own label prefix (`B3_`), so the listing still assembles.
  Version 3.6.1 read only plain 4, 8 and 16 KB images (Atari 800) and up
  to 32 KB (Atari 5200).
  - The cartridge header (run address, flags, init address) of the bank
    visible after power-on is typed automatically, at `$9FFA` for the
    right slot; for the Atari 5200, the title and vectors.
  - A raw image whose size several cartridge types share - or that may be
    plain data - opens the new **Choose ROM Type** dialog: pick the type,
    open the file as raw file instead, or cancel.
  - Clear messages for an unknown, unsupported, wrong-platform, too large
    or wrongly sized cartridge.
  - Fixed: a 4 KB `.car` file and a raw 40 KB Atari 5200 image (Bounty
    Bob) did not load.
- Fixed: **legacy profiles** (`DIS6502PRF10` to `PRF17`) did not load:
  loading always failed partway through, the hex notation prefix was lost,
  and the bytes per `.BYTE` line were taken from another setting.

### Disassembly listing

- **Find** is a permanent field above the listing instead of a dialog.
  It is case-insensitive (`lda` finds `LDA`), Ctrl+Shift+F puts the cursor
  into the field, and Find/Find Next are disabled when there is nothing to
  search.
- A byte range selected in the memory inspector selects every listing line
  it covers. The address comment of a selected line (`; $XXXX`) no longer
  overwrites text from column 35 on; it is appended to the end instead.
- Fixed: **Find Next** stuck at the first match instead of moving on.
- Fixed: clicking the XRef entry of a label's own definition line did
  nothing for lines without a segment, e.g. system equates.
- Fixed: "Show ZP Absolute as Byte" wrote its bytes with four hex digits
  (`.byte $00AD,$0080,$0000`) instead of two.
- Fixed: ATASCII output accepted character codes up to 2555 instead of
  255.
- Fixed: an unreferenced system label at address `$0000` (e.g. the C64's
  `D6510`) was written into every listing.

### Memory inspector

- **Character sets:** the grid is drawn from authentic 8x8 character sets -
  ATASCII Standard and International, PETSCII Uppercase and Lowercase, and
  the Oric's ASCII set from its ROM - selectable in the inspector's header.
  Each computer system starts with its own default.
- **Typing in the text column** writes the byte that shows the typed
  character in the selected character set (e.g. PETSCII), or its screen
  code in screen-code mode.
- **Display as Screen Code** moved from the View menu into a button in the
  inspector's header. The toolbar is gone; every command is in the popup
  menu, with its keyboard shortcut.
- **Select Sprites** is now called **Select Graphics**.
- Fixed: **Delete, Cut and Paste Selection** were broken; they now really
  remove and insert bytes.

### Segment list

- Several segments can be selected and deleted at once.

### Computer systems

- **C64** support is real: `.prg` files become binary segments and are
  disassembled right away, and the code vectors are the C64's.
- **Oric:** the Oric ASCII character set is its default.
- Fixed: `C64.equ` was a copy of the Atari file. The new one holds 455
  genuine C64 labels (VIC-II, SID, CIAs, KERNAL, BASIC, zero page).

### Equates

- Fixed: in **Edit User Equates**, Add/Modify always appended a new line
  instead of replacing the selected one.

### Options and appearance

- **View > Options...** with two groups, each with a live preview:
  - **Text Font:** any installed mono-spaced font and point size for the
    listing, the log, the segment list, the XRef panel and the panel
    headers.
  - **Memory Inspector Font:** the pixel size of the authentic computer
    font in the memory inspector (8 to 64 pixels).
  - **Restore Defaults** resets the dialog; OK applies it.

  View > Double Font Height is replaced by these options.
- Error lines in the log are shown in dark red.
