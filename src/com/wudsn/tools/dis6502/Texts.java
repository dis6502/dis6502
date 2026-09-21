/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import com.wudsn.tools.base.repository.NLS;

/**
 * UI text repository: every field is named per owning class ({@code
 * <ClassName>_<Purpose>}, e.g. {@link #AboutDialog_Title}), following the
 * same convention {@code com.wudsn.tools.thecartstudio.Texts} uses for its
 * own dialog-local strings (its {@code AboutDialog_Content}/{@code
 * AboutDialog_URL} fields) - including that class's own {@code
 * com.wudsn.tools.base.repository.NLS} base, the same one {@link Actions}
 * uses.
 * <p>
 * Most fields are UI text this port introduces itself, with no real Win32
 * {@code STRINGTABLE}/{@code IDS_*} resource to mirror. Some, however, were
 * moved here from the former {@code Text.java} (a separate faithfully-
 * ported-C++-resource repository this project used earlier, since retired)
 * once every one of its remaining constants was renamed away from its
 * {@code IDS_*} name and merged in - those fields' javadoc notes the
 * original C++ resource name they came from for traceability, but they are
 * otherwise ordinary fields here like any other.
 *
 * @author Peter Dell
 */
public final class Texts extends NLS {

	/** {@link com.wudsn.tools.dis6502.ui.AboutDialog}'s window title. */
	public static String AboutDialog_WindowTitle;
	/** {@link com.wudsn.tools.dis6502.ui.AboutDialog}'s bold product-name label. */
	public static String AboutDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.AboutDialog}'s subtitle label. */
	public static String AboutDialog_Subtitle;
	/** {@link com.wudsn.tools.dis6502.ui.AboutDialog}'s body text, one line per {@code \n} - split back into individual lines there. */
	public static String AboutDialog_Text;

	/**
	 * The version number shown in {@link Dis6502#run}'s startup warning
	 * message ({@link Messages#I001}'s {@code {0}} argument). A plain
	 * literal updated by hand per release, same as {@link
	 * #Dis6502_VersionDate}/{@code AboutDialog_Text}'s copyright years
	 * above.
	 */
	public static String Dis6502_Version;

	/**
	 * The build date shown in {@link Dis6502#run}'s startup warning message
	 * ({@link Messages#I001}'s {@code {1}} argument). The C++
	 * source fills this from the {@code __DATE__}/{@code __TIME__} compiler
	 * macros at build time ({@code Main.cpp}'s {@code WIDE1(__DATE__),
	 * WIDE1(__TIME__)}); Java has no build-time-macro equivalent, so this
	 * is a plain literal updated by hand per release instead, same as
	 * {@link #Dis6502_Version}/{@code AboutDialog_Text}'s copyright years
	 * above.
	 */
	public static String Dis6502_VersionDate;

	/** {@link com.wudsn.tools.dis6502.ui.AssembleDialog}'s window title. */
	public static String AssembleDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.CommentDialog}'s window title. */
	public static String CommentDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.DiskImageExecutableFileDialog}'s window title. */
	public static String DiskImageExecutableFileDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.DiskImageSectorsDialog}'s window title. */
	public static String DiskImageSectorsDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.EquateDialog}'s window title when editing ({@code editable == true}). */
	public static String EquateDialog_EditTitle;
	/** {@link com.wudsn.tools.dis6502.ui.EquateDialog}'s window title when read-only ({@code editable == false}). */
	public static String EquateDialog_DisplayTitle;
	/** {@link com.wudsn.tools.dis6502.ui.EquateRangeDialog}'s window title. */
	public static String EquateRangeDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.LowHighByteDialog}'s window title. */
	public static String LowHighByteDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.MemoryInspectorFindStringDialog}'s window title. */
	public static String MemoryInspectorFindStringDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.ProfileDialog}'s window title. */
	public static String ProfileDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.RawFileDialog}'s window title. */
	public static String RawFileDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.SegmentPropertiesDialog}'s window title. */
	public static String SegmentPropertiesDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.SegmentWriteBootDiskDialog}'s window title. */
	public static String SegmentWriteBootDiskDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.SelectGraphicsDialog}'s window title. */
	public static String SelectGraphicsDialog_Title;

	/** {@link Dis6502#openRecentWorkspace}/{@link Dis6502#openWorkspaceFile}'s "could not open workspace" error dialog title. */
	public static String Dis6502_OpenWorkspaceTitle;
	/** {@link Dis6502#openRecentFile}'s "could not open file" error dialog title. */
	public static String Dis6502_OpenFileTitle;
	/** {@link Dis6502#performClearEquates}'s confirmation dialog title. */
	public static String Dis6502_ClearEquatesTitle;
	/** {@link com.wudsn.tools.dis6502.ui.ProfileDialog#performLoadProfile}'s "could not load profile" error dialog title. */
	public static String ProfileDialog_LoadTitle;
	/** {@link Dis6502#performSetMemoryInspectorLoHiType}'s error dialog title. */
	public static String Dis6502_SetTypeTitle;
	/** {@link Dis6502#performPasteMemoryInspectorSelection}'s error dialog title. */
	public static String Dis6502_PasteSelectionTitle;
	/** {@link Dis6502#performOpenUserEquates}'s {@code JFileChooser} title. */
	public static String Dis6502_OpenUserEquatesFileTitle;
	/** {@link Dis6502#performSaveUserEquates}'s {@code JFileChooser} title when exporting to xasm format ({@code xasm == true}). */
	public static String Dis6502_ExportUserEquatesFileTitle;
	/** {@link Dis6502#performSaveUserEquates}'s {@code JFileChooser} title when saving in the native format ({@code xasm == false}). */
	public static String Dis6502_SaveUserEquatesFileTitle;
	/** {@link Dis6502#performSaveSegment}'s {@code JFileChooser} title. */
	public static String Dis6502_SaveSegmentTitle;
	/** {@link Dis6502#performSaveAllSegments}'s {@code JFileChooser} title. */
	public static String Dis6502_SaveAllSegmentsTitle;
	/** {@link Dis6502#performSaveMemoryInspectorSelection}'s {@code JFileChooser} title when saving with a header ({@code withHeader == true}). */
	public static String Dis6502_SaveSelectionWithHeaderTitle;
	/** {@link Dis6502#performSaveMemoryInspectorSelection}'s {@code JFileChooser} title when saving without a header ({@code withHeader == false}). */
	public static String Dis6502_SaveSelectionNoHeaderTitle;
	/** {@link Dis6502#performSaveWorkspaceAs}'s {@code JFileChooser} title. */
	public static String Dis6502_SaveWorkspaceFileAsTitle;
	/** {@link Dis6502#performSaveDisassemblyFiles}'s {@code JFileChooser} title. */
	public static String Dis6502_SaveDisassemblyFilesTitle;
	/** {@link com.wudsn.tools.dis6502.ui.ProfileDialog#performLoadProfile}'s {@code JFileChooser} title. */
	public static String ProfileDialog_LoadFileTitle;
	/** {@link com.wudsn.tools.dis6502.ui.ProfileDialog#performSaveProfile}'s {@code JFileChooser} title. */
	public static String ProfileDialog_SaveFileTitle;
	/**
	 * {@link Dis6502#openRawFile}'s {@code JFileChooser} title for
	 * {@code add == true}; the {@code add == false} case reuses {@link
	 * com.wudsn.tools.dis6502.ui.RawFileDialog}'s own {@link
	 * com.wudsn.tools.dis6502.Texts#RawFileDialog_Title} ("Open Raw File" -
	 * identical text).
	 */
	public static String Dis6502_AddRawFileTitle;
	/**
	 * {@link Dis6502#openDiskImageExecutableFile}'s {@code
	 * JFileChooser}/error-dialog title for {@code add == true}; the {@code
	 * add == false} case reuses {@link
	 * com.wudsn.tools.dis6502.Texts#DiskImageExecutableFileDialog_Title}
	 * ("Open Disk Image Executable File" - identical text).
	 */
	public static String Dis6502_AddDiskImageExecutableFileTitle;
	/** {@link Dis6502#openDiskImageBootSectors}'s {@code JFileChooser} title for {@code add == true}. */
	public static String Dis6502_AddDiskImageBootSectorsTitle;
	/** {@link Dis6502#openDiskImageBootSectors}'s {@code JFileChooser} title for {@code add == false}. */
	public static String Dis6502_OpenDiskImageBootSectorsTitle;
	/**
	 * {@link Dis6502#openDiskImageSectors}'s {@code JFileChooser}
	 * title for {@code add == true}; the {@code add == false} case reuses
	 * {@link com.wudsn.tools.dis6502.Texts#DiskImageSectorsDialog_Title}
	 * ("Open Disk Image Sectors" - identical text).
	 */
	public static String Dis6502_AddDiskImageSectorsTitle;
	/** {@link Dis6502#performOpenFile}'s {@code JFileChooser}/error-dialog title for {@link com.wudsn.tools.dis6502.model.FileType#EXECUTABLE_FILE}, {@code add == true}. */
	public static String Dis6502_AddExecutableFileTitle;
	/** {@link Dis6502#performOpenFile}'s {@code JFileChooser}/error-dialog title for {@link com.wudsn.tools.dis6502.model.FileType#EXECUTABLE_FILE}, {@code add == false}. */
	public static String Dis6502_OpenExecutableFileTitle;
	/** {@link Dis6502#performOpenFile}'s {@code JFileChooser}/error-dialog title for {@link com.wudsn.tools.dis6502.model.FileType#ROM_IMAGE_FILE}, {@code add == true}. */
	public static String Dis6502_AddRomImageFileTitle;
	/** {@link Dis6502#performOpenFile}'s {@code JFileChooser}/error-dialog title for {@link com.wudsn.tools.dis6502.model.FileType#ROM_IMAGE_FILE}, {@code add == false}. */
	public static String Dis6502_OpenRomImageFileTitle;
	/** {@link Dis6502#performOpenFile}'s {@code JFileChooser}/error-dialog title for {@link com.wudsn.tools.dis6502.model.FileType#CASSETTE_IMAGE_FILE}, {@code add == true}. */
	public static String Dis6502_AddCassetteImageFileTitle;
	/** {@link Dis6502#performOpenFile}'s {@code JFileChooser}/error-dialog title for {@link com.wudsn.tools.dis6502.model.FileType#CASSETTE_IMAGE_FILE}, {@code add == false}. */
	public static String Dis6502_OpenCassetteImageFileTitle;

	/** {@link com.wudsn.tools.dis6502.ui.DefaultFoldersDialog}'s {@code JFileChooser} sub-title - moved from {@code Text.IDS_DEFAULT_FOLDERS_DIALOG_SUB_TITLE}. */
	public static String DefaultFoldersDialog_SubTitle;
	/** {@link com.wudsn.tools.dis6502.ui.DefaultFoldersDialog}'s window title - moved from {@code Text.IDS_DEFAULT_FOLDERS_DIALOG_TITLE}. */
	public static String DefaultFoldersDialog_Title;

	/** {@link com.wudsn.tools.dis6502.ui.DisassemblyPanel}'s header text - moved from {@code Text.IDS_DIS_TITLE}. */
	public static String DisassemblyPanel_Title;

	/** {@link com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s header text when a segment is selected - moved from {@code Text.IDS_DUMP_TITLE_SEGMENT}. */
	public static String MemoryInspectorPanel_SegmentTitle;
	/** {@link com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s header text when nothing is selected - moved from {@code Text.IDS_DUMP_TITLE_SEGMENT_NO_SEGMENT_SELECTED}. */
	public static String MemoryInspectorPanel_NoSegmentSelectedTitle;
	/** {@link com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s header text when a byte range is selected - moved from {@code Text.IDS_DUMP_TITLE_SELECTION}. */
	public static String MemoryInspectorPanel_SelectionTitle;

	/** {@link Dis6502#performClearEquates}'s confirmation message for {@code WorkspaceProperty#SYSTEM_EQUATES} - moved from {@code Text.IDS_EQUATES_CONFIRM_CLEAR_SYSTEM_EQUATES}. */
	public static String Dis6502_ConfirmClearSystemEquatesMessage;
	/** {@link Dis6502#performClearEquates}'s confirmation message for {@code WorkspaceProperty#USER_EQUATES} - moved from {@code Text.IDS_EQUATES_CONFIRM_CLEAR_USER_EQUATES}. */
	public static String Dis6502_ConfirmClearUserEquatesMessage;

	/**
	 * {@link com.wudsn.tools.dis6502.ui.MemoryInspectorFindStringDialog#performOK}'s
	 * "string not found" alert message, reused by {@link
	 * Dis6502#performMemoryInspectorFindNext}/{@link
	 * Dis6502#performFindInDisassembly} - moved from {@code
	 * Text.IDS_FIND_STRING_DIALOG_STRING_NOT_FOUND_MESSAGE}.
	 */
	public static String MemoryInspectorFindStringDialog_StringNotFoundMessage;
	/**
	 * The "string not found" alert's title (distinct from {@link
	 * #MemoryInspectorFindStringDialog_Title}, the dialog's own window
	 * title) - moved from {@code Text.IDS_FIND_STRING_DIALOG_TITLE}.
	 */
	public static String MemoryInspectorFindStringDialog_NotFoundTitle;

	/** {@link com.wudsn.tools.dis6502.ui.LogPanel}'s header text - moved from {@code Text.IDS_LOG_TITLE}. */
	public static String LogPanel_Title;

	/**
	 * {@link com.wudsn.tools.dis6502.ui.WorkspaceDialog}'s window title,
	 * reused by {@link Dis6502#confirmClearWorkspace}'s confirm dialog -
	 * moved from {@code Text.IDS_MAIN_FILE_NEW_WORKSPACE_TITLE}.
	 */
	public static String WorkspaceDialog_Title;
	/** {@link Dis6502#confirmClearWorkspace}'s confirmation message - moved from {@code Text.IDS_MAIN_FILE_NEW_WORKSPACE_MESSAGE}. */
	public static String Dis6502_NewWorkspaceMessage;

	/** {@link Dis6502#openWorkspaceFile}'s {@code JFileChooser} title - moved from {@code Text.IDS_MAIN_FILE_OPEN_WORKSPACE_FILE_TITLE}. */
	public static String Dis6502_OpenWorkspaceFileTitle;
	/** {@link Dis6502#updateTitle}'s main window title once a file is open - moved from {@code Text.IDS_MAIN_WINDOW_TITLE}. */
	public static String Dis6502_WindowTitle;
	/** {@link Dis6502#updateTitle}'s main window title with no file open - moved from {@code Text.IDS_MAIN_WINDOW_TITLE_NO_WORKSPACE_LOADED}. */
	public static String Dis6502_WindowTitleNoWorkspaceLoaded;

	/** {@link com.wudsn.tools.dis6502.ui.SegmentListPanel}'s header text when segments are loaded - moved from {@code Text.IDS_SEGMENT_TITLE}. */
	public static String SegmentListPanel_Title;
	/** {@link com.wudsn.tools.dis6502.ui.SegmentListPanel}'s header text when empty - moved from {@code Text.IDS_SEGMENT_TITLE_NO_SEGMENTS_LOADED}. */
	public static String SegmentListPanel_NoSegmentsLoadedTitle;

	/** {@link com.wudsn.tools.dis6502.ui.GraphicMode#ANTIC_8}'s display name - moved from {@code Text.IDS_SPRITE_ANTIC_8}. */
	public static String GraphicMode_Antic8;
	/** {@link com.wudsn.tools.dis6502.ui.GraphicMode#ANTIC_9}'s display name - moved from {@code Text.IDS_SPRITE_ANTIC_9}. */
	public static String GraphicMode_Antic9;
	/** {@link com.wudsn.tools.dis6502.ui.GraphicMode#ANTIC_A}'s display name - moved from {@code Text.IDS_SPRITE_ANTIC_A}. */
	public static String GraphicMode_AnticA;
	/** {@link com.wudsn.tools.dis6502.ui.GraphicMode#ANTIC_B}'s display name - moved from {@code Text.IDS_SPRITE_ANTIC_B}. */
	public static String GraphicMode_AnticB;
	/** {@link com.wudsn.tools.dis6502.ui.GraphicMode#ANTIC_C}'s display name - moved from {@code Text.IDS_SPRITE_ANTIC_C}. */
	public static String GraphicMode_AnticC;
	/** {@link com.wudsn.tools.dis6502.ui.GraphicMode#ANTIC_D}'s display name - moved from {@code Text.IDS_SPRITE_ANTIC_D}. */
	public static String GraphicMode_AnticD;
	/** {@link com.wudsn.tools.dis6502.ui.GraphicMode#ANTIC_E}'s display name - moved from {@code Text.IDS_SPRITE_ANTIC_E}. */
	public static String GraphicMode_AnticE;
	/** {@link com.wudsn.tools.dis6502.ui.GraphicMode#ANTIC_F}'s display name - moved from {@code Text.IDS_SPRITE_ANTIC_F}. */
	public static String GraphicMode_AnticF;

	/** {@link com.wudsn.tools.dis6502.ui.XRefPanel}'s header text for exactly one reference - moved from {@code Text.IDS_XREF_TITLE_LABEL_REFERENCE}. */
	public static String XRefPanel_LabelReferenceTitle;
	/** {@link com.wudsn.tools.dis6502.ui.XRefPanel}'s header text for more than one reference - moved from {@code Text.IDS_XREF_TITLE_LABEL_REFERENCES}. */
	public static String XRefPanel_LabelReferencesTitle;
	/** {@link com.wudsn.tools.dis6502.ui.XRefPanel}'s header text when no label is selected - moved from {@code Text.IDS_XREF_TITLE_NO_LABEL_SELECTED}. */
	public static String XRefPanel_NoLabelSelectedTitle;

	// {@link com.wudsn.tools.dis6502.model.FileTypeInfo}'s texts, one pair per {@link com.wudsn.tools.dis6502.model.FileType}:
	// _Text is the singular display text, _FilterText the plural a file chooser's filter shows (which appends
	// the " (*.xex, ...)" suffix itself).
	public static String FileType_UNKNOWN_FILE_Text;
	public static String FileType_UNKNOWN_FILE_FilterText;
	public static String FileType_RAW_FILE_Text;
	public static String FileType_RAW_FILE_FilterText;
	public static String FileType_EXECUTABLE_FILE_Text;
	public static String FileType_EXECUTABLE_FILE_FilterText;
	public static String FileType_ROM_IMAGE_FILE_Text;
	public static String FileType_ROM_IMAGE_FILE_FilterText;
	public static String FileType_CASSETTE_IMAGE_FILE_Text;
	public static String FileType_CASSETTE_IMAGE_FILE_FilterText;
	public static String FileType_DISK_IMAGE_EXECUTABLE_FILE_Text;
	public static String FileType_DISK_IMAGE_EXECUTABLE_FILE_FilterText;
	public static String FileType_DISK_IMAGE_BOOT_SECTORS_Text;
	public static String FileType_DISK_IMAGE_BOOT_SECTORS_FilterText;
	public static String FileType_DISK_IMAGE_SECTORS_Text;
	public static String FileType_DISK_IMAGE_SECTORS_FilterText;
	public static String FileType_WORKSPACE_FILE_Text;
	public static String FileType_WORKSPACE_FILE_FilterText;
	public static String FileType_EQUATES_FILE_Text;
	public static String FileType_EQUATES_FILE_FilterText;
	public static String FileType_PROFILE_FILE_Text;
	public static String FileType_PROFILE_FILE_FilterText;
	public static String FileType_DISASSEMBLY_FILE_Text;
	public static String FileType_DISASSEMBLY_FILE_FilterText;

	/** {@link com.wudsn.tools.dis6502.ui.FileChoosers}' "file exists, overwrite?" confirmation. */
	public static String FileChoosers_OverwriteTitle;
	public static String FileChoosers_OverwriteMessage;

	static {
		initializeClass(Texts.class, null);
	}
}
