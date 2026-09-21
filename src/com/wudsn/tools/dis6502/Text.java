/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import com.wudsn.tools.base.repository.NLS;

/**
 * Text repository: the application's user-visible message/title strings,
 * ported from the Windows resource string table (Resource.h's {@code IDS_*}
 * constants, with their text taken from the {@code STRINGTABLE} block of
 * dis6502.rc).
 * <p>
 * Follows the same {@code com.wudsn.tools.base.repository.NLS} pattern as
 * {@link Actions} and {@code com.wudsn.tools.thecartstudio.Texts}: each
 * field is populated reflectively from the matching key in {@code
 * Text.properties} when this class is loaded, via the inherited {@code
 * initializeClass} call below. Unlike the C++ source, where {@code IDS_*} are opaque
 * resource IDs requiring a {@code Text::Get(TextID)}/{@code
 * Application::GetText} round trip through the Windows resource table at
 * runtime, each field here directly holds its resolved text - there is no
 * separate {@code Get} method to port. A parameterized text (one containing
 * {@code {0}}, {@code {1}}, ...) is filled in with {@code
 * com.wudsn.tools.base.common.TextUtility#format(String, String...)},
 * matching what {@code Text::Format} did in C++.
 * <p>
 * Only the {@code IDS_*} (string table) constants are ported here - the
 * {@code IDC_*}/{@code IDM_*}/{@code ID_*}/{@code IDI_*}/{@code IDB_*}
 * constants in Resource.h identify dialog controls, menu commands, icons,
 * and bitmaps, not texts, and belong with the (not yet ported) UI layer
 * that uses them. UI text this port introduces itself, with no real
 * resource to mirror (such as {@code AboutDialog}'s own body text), lives in
 * {@link Texts} instead, not here. A structured, severity-typed message
 * (one consumed via {@code com.wudsn.tools.base.repository.Message}/{@link
 * Application#sendMessage}, not a plain {@code String}) lives in {@link
 * Messages} instead, even if - like {@link Messages#I001}, moved from here
 * (formerly {@code IDS_LOG_BETA_MESSAGE}) - it does trace back to a real
 * ported C++ resource; {@link Messages}'s own javadoc explains why its
 * field naming diverges from this class's {@code IDS_*} convention.
 * <p>
 * The single {@code STRINGTABLE} block in dis6502.rc is English-only, so
 * there is only a {@code Text.properties}, no locale-specific variant.
 *
 * @author Peter Dell
 */
public final class Text extends NLS {

	public static String IDS_COMPUTER_MEM_ERR;
	public static String IDS_COMPUTER_NOT_FOUND;
	public static String IDS_COMPUTER_NOT_LOADED;
	public static String IDS_DEFAULT_FOLDERS_DIALOG_SUB_TITLE;
	public static String IDS_DEFAULT_FOLDERS_DIALOG_TITLE;
	public static String IDS_DIS_POPUP_MENU_ADDR_RANGE_DEF;
	public static String IDS_DIS_POPUP_MENU_ADDR_RANGE_REF;
	public static String IDS_DIS_POPUP_MENU_FIND_DEF;
	public static String IDS_DIS_POPUP_MENU_FIND_REF1;
	public static String IDS_DIS_POPUP_MENU_FIND_REF2;
	public static String IDS_DIS_POPUP_MENU_REN_DEF;
	public static String IDS_DIS_POPUP_MENU_REN_REF;
	public static String IDS_DIS_TITLE;
	public static String IDS_DUMP_TITLE_SEGMENT;
	public static String IDS_DUMP_TITLE_SEGMENT_NO_SEGMENT_SELECTED;
	public static String IDS_DUMP_TITLE_SELECTION;
	public static String IDS_EQUATES_CONFIRM_CLEAR_SYSTEM_EQUATES;
	public static String IDS_EQUATES_CONFIRM_CLEAR_USER_EQUATES;
	public static String IDS_ERR_ATARI_FILE;
	public static String IDS_ERR_FNT_NOT_FOUND;
	public static String IDS_ERR_LABEL_OVEFLOW;
	public static String IDS_ERR_NO_GUESS_MEMORY;
	public static String IDS_ERR_NO_LABEL_MEMORY;
	public static String IDS_EXPORT_USER_EQU;
	public static String IDS_FILE_IO_ERR_WRITING_FILE;
	public static String IDS_FILE_IO_EX_OPENING_FILE_FOR_READ_ACCESS;
	public static String IDS_FILE_IO_EX_OPENING_FILE_FOR_READ_WRITE_ACCESS;
	public static String IDS_FILE_IO_EX_OPENING_FILE_FOR_WRITE_ACCESS;
	public static String IDS_FILE_SYSTEM_LOGIC_OPEN_FILE_TITLE;
	public static String IDS_FILE_SYSTEM_LOGIC_SAVE_FILE_TITLE;
	public static String IDS_FIND_STRING_DIALOG_STRING_NOT_FOUND_MESSAGE;
	public static String IDS_FIND_STRING_DIALOG_TITLE;
	public static String IDS_LOG_DISASSEMBLY_PROGRESS_MONITOR_INFO;
	public static String IDS_LOG_DISASSEMBLY_PROGRESS_MONITOR_PASS;
	public static String IDS_LOG_DISASSEMBLY_PROGRESS_MONITOR_SEGMENT;
	public static String IDS_LOG_TITLE;
	public static String IDS_MAIN_FILE_NEW_WORKSPACE_MESSAGE;
	public static String IDS_MAIN_FILE_NEW_WORKSPACE_TITLE;
	public static String IDS_MAIN_FILE_OPEN_WORKSPACE_FILE_MESSAGE;
	public static String IDS_MAIN_FILE_OPEN_WORKSPACE_FILE_TITLE;
	public static String IDS_MAIN_WINDOW_TITLE;
	public static String IDS_MAIN_WINDOW_TITLE_NO_WORKSPACE_LOADED;
	public static String IDS_SEGMENT_TITLE;
	public static String IDS_SEGMENT_TITLE_NO_SEGMENTS_LOADED;
	public static String IDS_SPRITE_ANTIC_8;
	public static String IDS_SPRITE_ANTIC_9;
	public static String IDS_SPRITE_ANTIC_A;
	public static String IDS_SPRITE_ANTIC_B;
	public static String IDS_SPRITE_ANTIC_C;
	public static String IDS_SPRITE_ANTIC_D;
	public static String IDS_SPRITE_ANTIC_E;
	public static String IDS_SPRITE_ANTIC_F;
	public static String IDS_XREF_TITLE_LABEL_REFERENCE;
	public static String IDS_XREF_TITLE_LABEL_REFERENCES;
	public static String IDS_XREF_TITLE_NO_LABEL_SELECTED;

	static {
		initializeClass(Text.class, null);
	}
}
