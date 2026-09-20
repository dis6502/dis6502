/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import org.w3c.dom.Element;

import com.wudsn.tools.base.common.HexUtility;

/**
 * One entry read from (or to be written to) an equates file: an empty line,
 * a comment line, or a label definition line ("LABEL = $1234 ; comment").
 * <p>
 * Ported from Equate.h / Equate.cpp. The workspace-version-1X binary
 * Load1X/Save1X format is not ported yet; deferred to when {@code
 * Workspace}'s own binary-format loading is ported.
 * <p>
 * Only {@link EquateList} and this class's own {@link #readFrom} may create
 * and initialize instances (it was a C++ {@code friend class}); the
 * constructor and {@link #init} are package-private for the same reason.
 *
 * @author Peter Dell
 */
public final class Equate implements Xml.Serializable {

	/**
	 * The result of parsing one line of an equates file with {@link #readFrom}.
	 * {@link #equate} is the parsed, initialized (but not yet
	 * list-attached) equate, or {@code null} if the line could not be
	 * parsed ({@link #error} non-empty) or was of {@link EquateType#UNKNOWN}.
	 */
	public static final class ReadResult {
		public final EquateType equateType;
		public final String label;
		public final int labelAccess;
		public final int address;
		public final String comment;
		public final String error;
		public final Equate equate;

		ReadResult(EquateType equateType, String label, int labelAccess, int address, String comment,
				String error) {
			this.equateType = equateType;
			this.label = label;
			this.labelAccess = labelAccess;
			this.address = address;
			this.comment = comment;
			this.error = error;
			this.equate = createEquate(equateType, label, labelAccess, address, comment, error);
		}
	}

	private static Equate createEquate(EquateType equateType, String label, int labelAccess, int address,
			String comment, String error) {
		if (!error.isEmpty() || equateType == EquateType.UNKNOWN) {
			return null;
		}
		Equate equate = new Equate();
		equate.init(equateType, label, labelAccess, address, comment);
		return equate;
	}

	private EquateType equateType;
	private String label;
	private int labelAccess; // Supported types of access.
	private int labelValue;
	private String comment;

	private String baseLabel; // Transient.
	private boolean defined; // Transient.
	private int referencedLabelAccess; // Transient, referenced types of access.

	Equate() {
		clear();
	}

	/**
	 * Extracts the 4 character hexadecimal address from a label name ending in
	 * "Lnnnn" (uppercase hex digits only), "" otherwise.
	 */
	public static String extractAddress(String label) {
		if (label.length() < 5) {
			return "";
		}
		int start = label.length() - 5;
		if (label.charAt(start++) != 'L') {
			return "";
		}
		for (int i = start; i < start + 4; i++) {
			char c = label.charAt(i);
			if ((c < '0' || c > '9') && (c < 'A' || c > 'F')) {
				return "";
			}
		}
		return label.substring(start);
	}

	public static boolean isAutomaticLabel(String label) {
		return !extractAddress(label).isEmpty();
	}

	public static boolean isLabelWithOffset(String label) {
		return label.indexOf('+') >= 0 || label.indexOf('-') >= 0;
	}

	void clear() {
		equateType = EquateType.UNKNOWN;
		label = "";
		labelAccess = LabelAccess.UNKNOWN;
		labelValue = 0;
		comment = "";

		initTransientFields();
	}

	private void initTransientFields() {
		switch (equateType) {
		case UNKNOWN:
		case EMPTY:
		case COMMENT:
			if (!label.isEmpty()) {
				throw new IllegalStateException("Label specified.");
			}
			break;

		case LABEL: {
			if (labelAccess == LabelAccess.UNKNOWN) {
				throw new IllegalStateException("Invalid access.");
			}
			if (label.isEmpty()) {
				throw new IllegalStateException("No label specified.");
			}
			int index = label.indexOf('+');
			if (index < 0) {
				index = label.indexOf('-');
			}
			if (index >= 0) {
				baseLabel = label.substring(0, index);
			}
			break;
		}

		default:
			throw new IllegalStateException("Invalid equate type.");
		}

		defined = false;
		referencedLabelAccess = LabelAccess.UNKNOWN;
	}

	void init(EquateType equateType, String label, int labelAccess, int labelValue, String comment) {
		this.equateType = equateType;
		this.label = label;
		this.labelAccess = labelAccess;
		this.labelValue = labelValue;
		this.comment = comment;

		initTransientFields();
	}

	@Override
	public void serializeTo(Element element) {
		Xml.setStringAttribute(element, "EquateType", equateType.name());
		switch (equateType) {
		case UNKNOWN:
			throw new IllegalStateException("Invalid equate type.");

		case EMPTY:
			break;

		case COMMENT:
			Xml.setStringAttribute(element, "Comment", comment);
			break;

		case LABEL:
			Xml.setStringAttribute(element, "Label", label);
			Xml.setStringAttribute(element, "LabelAccess", LabelAccess.getKey(labelAccess));
			if (labelValue < 0x100) {
				Xml.setByteAttributeHex(element, "LabelValue", labelValue);
			} else {
				Xml.setWordAttributeHex(element, "LabelValue", labelValue);
			}
			if (!comment.isEmpty()) {
				Xml.setStringAttribute(element, "Comment", comment);
			}
			break;
		}
	}

	@Override
	public void deserializeFrom(Element element) {
		clear();
		String equateTypeString = Xml.getStringAttribute(element, "EquateType", "");
		try {
			equateType = EquateType.valueOf(equateTypeString);
		} catch (IllegalArgumentException e) {
			equateType = EquateType.UNKNOWN;
		}
		label = Xml.getStringAttribute(element, "Label", label);
		labelAccess = LabelAccess.fromKey(Xml.getStringAttribute(element, "LabelAccess", ""));
		labelValue = Xml.getWordAttribute(element, "LabelValue", labelValue);
		comment = Xml.getStringAttribute(element, "Comment", comment);
		initTransientFields();
	}

	public EquateType getType() {
		return equateType;
	}

	public String getLabel() {
		return label;
	}

	public boolean equalsLabel(String label) {
		return this.label.equals(label);
	}

	public String getBaseLabel() {
		return baseLabel;
	}

	public boolean isRange() {
		return baseLabel != null && !baseLabel.isEmpty();
	}

	public int getLabelAccess() {
		return labelAccess;
	}

	public boolean isLabelAccessSupported(int labelAccess) {
		return LabelAccess.isSupported(this.labelAccess, labelAccess);
	}

	public int getLabelValue() {
		return labelValue;
	}

	public String getComment() {
		return comment;
	}

	public void clearDefinition() {
		defined = false;
	}

	public boolean hasDefinition() {
		return defined;
	}

	public void addDefinition() {
		defined = true;
	}

	public void clearReferences() {
		referencedLabelAccess = LabelAccess.UNKNOWN;
	}

	public void addLabelReference(int labelAccess) {
		referencedLabelAccess = referencedLabelAccess | labelAccess;
	}

	public int getReferencedLabelAccess() {
		return referencedLabelAccess;
	}

	public boolean hasReferences() {
		return referencedLabelAccess != LabelAccess.UNKNOWN;
	}

	public boolean hasReferencedLabelAccess(int labelAccess) {
		return LabelAccess.isSupported(referencedLabelAccess, labelAccess);
	}

	/** Returns true, and leaves index[0] at string.length(), if the rest of the string is blank. */
	private static boolean skipBlanks(String string, int[] index) {
		while (index[0] < string.length() && Character.isWhitespace(string.charAt(index[0]))) {
			index[0]++;
		}
		return index[0] == string.length();
	}

	/**
	 * Parses the leading unsigned digits of {@code string} starting at
	 * {@code index[0]} in the given radix. On success, updates
	 * {@code index[0]} past the parsed digits and returns the parsed value;
	 * on failure (no digits found), leaves {@code index[0]} unchanged and
	 * returns -1.
	 */
	private static int parseUnsignedInt(String string, int[] index, int radix) {
		int start = index[0];
		int i = start;
		while (i < string.length() && Character.digit(string.charAt(i), radix) >= 0) {
			i++;
		}
		if (i == start) {
			return -1;
		}
		int value = Integer.parseInt(string.substring(start, i), radix);
		index[0] = i;
		return value;
	}

	/**
	 * Parses one line of an equates file, e.g. "RTCLOK+1 = $13" or a comment
	 * or empty line. Returns a {@link ReadResult} with a non-empty
	 * {@code error} if the line cannot be parsed.
	 */
	public static ReadResult readFrom(String line) {
		EquateType equateType = EquateType.UNKNOWN;
		int address = 0;
		int labelAccess = LabelAccess.UNKNOWN;
		StringBuilder label = new StringBuilder();
		String comment = "";

		int[] index = { 0 };
		if (skipBlanks(line, index)) {
			return new ReadResult(EquateType.EMPTY, "", labelAccess, address, comment, "");
		}

		// If we have a comment line, we add it to the disassembly listing.
		if (line.charAt(index[0]) == ';') {
			index[0]++;
			if (!skipBlanks(line, index)) {
				comment = line.substring(index[0]).trim();
			}
			return new ReadResult(EquateType.COMMENT, "", labelAccess, address, comment, "");
		}

		// We must have a label name starting with a letter or "_".
		char c = line.charAt(index[0]);
		if (!Character.isLetter(c) && c != '_') {
			String error = "Character '" + c + "' at position " + index[0]
					+ " is not a valid start character for a label name.";
			return new ReadResult(equateType, "", labelAccess, address, comment, error);
		}
		equateType = EquateType.LABEL;

		// Compose the label name. Label names can contain offsets, e.g. "RTCLOK+1 = $13".
		while (index[0] < line.length() && (Character.isLetterOrDigit(c) || c == '_' || c == '+' || c == '-')) {
			label.append(c);
			index[0]++;
			if (index[0] < line.length()) {
				c = line.charAt(index[0]);
			}
		}

		if (skipBlanks(line, index)) {
			return new ReadResult(equateType, label.toString(), labelAccess, address, comment,
					"No access qualifier specified.");
		}

		c = line.charAt(index[0]);
		// Now we must have an equal (=, <, > or #) sign.
		switch (c) {
		case '=':
			labelAccess = LabelAccess.READ_WRITE;
			break;
		case '<':
			labelAccess = LabelAccess.READ;
			break;
		case '>':
			labelAccess = LabelAccess.WRITE;
			break;
		case '#':
			labelAccess = LabelAccess.IMMEDIATE;
			break;
		default:
			String error = "Character '" + c + "' at position " + (index[0] + 1)
					+ " is not an access qualifier. Use '=', '<', '>' or '#'.";
			return new ReadResult(equateType, label.toString(), labelAccess, address, comment, error);
		}
		index[0]++;

		if (skipBlanks(line, index)) {
			return new ReadResult(equateType, label.toString(), labelAccess, address, comment, "No value specified.");
		}

		// Now we must have an address (hex or decimal).
		if (line.charAt(index[0]) == '$') {
			index[0]++;
			int start = index[0];
			address = parseUnsignedInt(line, index, 16);
			if (address < 0) {
				String error = "Characters '" + line.substring(start) + "' at position " + (start + 1)
						+ " cannot be interpreted as a hexadecimal number.";
				return new ReadResult(equateType, label.toString(), labelAccess, 0, comment, error);
			}
		} else {
			int start = index[0];
			address = parseUnsignedInt(line, index, 10);
			if (address < 0) {
				String error = "Characters '" + line.substring(start) + "' at position " + (start + 1)
						+ " cannot be interpreted as a decimal number.";
				return new ReadResult(equateType, label.toString(), labelAccess, 0, comment, error);
			}
		}

		if (skipBlanks(line, index)) {
			return new ReadResult(equateType, label.toString(), labelAccess, address, comment, "");
		}

		// Scan for line comment.
		c = line.charAt(index[0]);
		if (c == ';') {
			index[0]++;
			if (!skipBlanks(line, index)) {
				comment = line.substring(index[0]).trim();
			}
			return new ReadResult(equateType, label.toString(), labelAccess, address, comment, "");
		}

		String error = "Invalid character '" + c + "' after value found. Line end or comment expected.";
		return new ReadResult(equateType, label.toString(), labelAccess, address, comment, error);
	}

	@Override
	public String toString() {
		switch (equateType) {
		case EMPTY:
			return "";

		case COMMENT:
			return "; " + comment;

		case LABEL:
			String value = HexUtility.getLongValueHexString(labelValue, 4);
			if (!comment.isEmpty()) {
				return label + " " + LabelAccess.getQualifier(labelAccess) + " $" + value + "; " + comment;
			}
			return label + " " + LabelAccess.getQualifier(labelAccess) + " $" + value;

		default:
			throw new IllegalStateException("Unsupported equate type.");
		}
	}
}
