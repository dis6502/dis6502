/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import com.wudsn.tools.base.repository.NLS;

/**
 * UI text repository for strings this port introduces itself - dialog
 * titles, labels, body text - with no real Win32 {@code STRINGTABLE}/{@code
 * IDS_*} resource to mirror, unlike {@link Text}. Fields are named per
 * dialog ({@code <DialogName>_<Purpose>}, e.g. {@link #AboutDialog_Title}),
 * following the same convention {@code com.wudsn.tools.thecartstudio.Texts}
 * uses for its own dialog-local strings (its {@code AboutDialog_Content}/
 * {@code AboutDialog_URL} fields) - including that class's own {@code
 * com.wudsn.tools.base.repository.NLS} base, the same one {@link Text}/
 * {@link Actions} use.
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

	/** {@link Dis6502#openRecentWorkspace}/{@link Dis6502#performOpenWorkspace}'s "could not open workspace" error dialog title. */
	public static String Dis6502_OpenWorkspaceTitle;
	/** {@link Dis6502#openRecentFile}'s "could not open file" error dialog title. */
	public static String Dis6502_OpenFileTitle;
	/** {@link Dis6502#performClearEquates}'s confirmation dialog title. */
	public static String Dis6502_ClearEquatesTitle;
	/** {@link com.wudsn.tools.dis6502.ui.ProfileDialog#performLoadProfile}'s "could not load profile" error dialog title. */
	public static String ProfileDialog_LoadTitle;
	/** {@link Dis6502#performSetMemoryInspectorLoHiType}'s error dialog title. */
	public static String Dis6502_SetTypeTitle;
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
	 * {@link Dis6502#performOpenRawFile}'s {@code JFileChooser} title for
	 * {@code add == true}; the {@code add == false} case reuses {@link
	 * com.wudsn.tools.dis6502.ui.RawFileDialog}'s own {@link
	 * com.wudsn.tools.dis6502.Texts#RawFileDialog_Title} ("Open Raw File" -
	 * identical text).
	 */
	public static String Dis6502_AddRawFileTitle;
	/**
	 * {@link Dis6502#performOpenDiskImageExecutableFile}'s {@code
	 * JFileChooser}/error-dialog title for {@code add == true}; the {@code
	 * add == false} case reuses {@link
	 * com.wudsn.tools.dis6502.Texts#DiskImageExecutableFileDialog_Title}
	 * ("Open Disk Image Executable File" - identical text).
	 */
	public static String Dis6502_AddDiskImageExecutableFileTitle;
	/** {@link Dis6502#performOpenDiskImageBootSectors}'s {@code JFileChooser} title for {@code add == true}. */
	public static String Dis6502_AddDiskImageBootSectorsTitle;
	/** {@link Dis6502#performOpenDiskImageBootSectors}'s {@code JFileChooser} title for {@code add == false}. */
	public static String Dis6502_OpenDiskImageBootSectorsTitle;
	/**
	 * {@link Dis6502#performOpenDiskImageSectors}'s {@code JFileChooser}
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

	static {
		initializeClass(Texts.class, null);
	}
}
