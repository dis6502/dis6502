/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.wudsn.tools.base.common.HexUtility;
import com.wudsn.tools.dis6502.Messages;

/**
 * XML serialization framework: attribute get/set helpers for the format
 * existing saved workspace/profile files already use (hexadecimal with a
 * "0x" prefix for byte/word/size values, decimal for plain ints, UTF-8 text
 * for strings, {@code "true"}/{@code "false"} for booleans), plus top-level
 * load/save. Uses the JDK's built-in {@code org.w3c.dom} API.
 * <p>
 * Every {@code getXxxAttribute} method takes the field's current value as
 * its {@code defaultValue} and returns that unchanged if the attribute is
 * missing or malformed, so a caller writes {@code field =
 * Xml.getXxxAttribute(element, "Name", field);} - the field is only ever
 * updated by a present, well-formed attribute.
 *
 * @author Peter Dell
 */
public final class Xml {

	private Xml() {
	}

	/** Implemented by any model type with XML persistence. */
	public interface Serializable {
		void serializeTo(Element element);

		void deserializeFrom(Element element);
	}

	public static Element addChildElement(Element element, String elementName) {
		Element child = element.getOwnerDocument().createElement(elementName);
		element.appendChild(child);
		return child;
	}

	/** The first child that is itself an element (skipping text/comment nodes), or {@code null}. */
	public static Element getFirstChildElement(Element element) {
		org.w3c.dom.Node node = element.getFirstChild();
		while (node != null && node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
			node = node.getNextSibling();
		}
		return (Element) node;
	}

	/** The first child with the given tag name that is itself an element, or {@code null}. */
	public static Element getFirstChildElement(Element element, String tagName) {
		Element child = getFirstChildElement(element);
		while (child != null && !child.getTagName().equals(tagName)) {
			child = getNextSiblingElement(child);
		}
		return child;
	}

	/** The next sibling that is itself an element (skipping text/comment nodes), or {@code null}. */
	public static Element getNextSiblingElement(Element element) {
		org.w3c.dom.Node node = element.getNextSibling();
		while (node != null && node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
			node = node.getNextSibling();
		}
		return (Element) node;
	}

	/** The next sibling with the given tag name that is itself an element, or {@code null}. */
	public static Element getNextSiblingElement(Element element, String tagName) {
		Element sibling = getNextSiblingElement(element);
		while (sibling != null && !sibling.getTagName().equals(tagName)) {
			sibling = getNextSiblingElement(sibling);
		}
		return sibling;
	}

	public static void save(Serializable serializable, String elementName, File filePath) throws IOException {
		Document document = newDocument();
		Element root = document.createElement(elementName);
		document.appendChild(root);
		serializable.serializeTo(root);

		transform(document, new StreamResult(filePath));
	}

	public static void save(Serializable serializable, String elementName, OutputStream outputStream)
			throws IOException {
		Document document = newDocument();
		Element root = document.createElement(elementName);
		document.appendChild(root);
		serializable.serializeTo(root);

		transform(document, new StreamResult(outputStream));
	}

	public static void load(Serializable serializable, String elementName, File filePath) throws IOException {
		Document document;
		try {
			document = newDocumentBuilder().parse(filePath);
		} catch (SAXException e) {
			throw new IOException(e);
		}
		Element root = document.getDocumentElement();
		if (!root.getTagName().equals(elementName)) {
			throw new IOException(Messages.E065.format(elementName, root.getTagName()));
		}
		serializable.deserializeFrom(root);
	}

	private static Document newDocument() throws IOException {
		return newDocumentBuilder().newDocument();
	}

	private static DocumentBuilder newDocumentBuilder() throws IOException {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		}
	}

	private static void transform(Document document, StreamResult result) throws IOException {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(document), result);
		} catch (TransformerException e) {
			throw new IOException(e);
		}
	}

	// ------------------------------------------------------------------
	// Attribute helpers.
	// ------------------------------------------------------------------

	public static void setBoolAttribute(Element element, String name, boolean value) {
		element.setAttribute(name, value ? "true" : "false");
	}

	public static boolean getBoolAttribute(Element element, String name, boolean defaultValue) {
		String value = element.getAttribute(name);
		if ("true".equals(value)) {
			return true;
		}
		if ("false".equals(value)) {
			return false;
		}
		return defaultValue;
	}

	public static void setIntAttribute(Element element, String name, int value) {
		element.setAttribute(name, Integer.toString(value));
	}

	public static int getIntAttribute(Element element, String name, int defaultValue) {
		if (!element.hasAttribute(name)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(element.getAttribute(name));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static void setStringAttribute(Element element, String name, String value) {
		element.setAttribute(name, value);
	}

	public static String getStringAttribute(Element element, String name, String defaultValue) {
		return element.hasAttribute(name) ? element.getAttribute(name) : defaultValue;
	}

	/** Sets a byte value (0-255) as a plain decimal attribute. */
	public static void setByteAttribute(Element element, String name, int value) {
		element.setAttribute(name, Integer.toString(value & 0xFF));
	}

	public static int getByteAttribute(Element element, String name, int defaultValue) {
		return getUnsignedAttribute(element, name, defaultValue, 0xFF);
	}

	/** Sets a byte value (0-255) as a "0x"-prefixed, 2-digit hexadecimal attribute. */
	public static void setByteAttributeHex(Element element, String name, int value) {
		element.setAttribute(name, "0x" + HexUtility.getByteValueHexString(value));
	}

	/** Sets a word/address value (0-65535) as a plain decimal attribute. */
	public static void setWordAttribute(Element element, String name, int value) {
		element.setAttribute(name, Integer.toString(value & 0xFFFF));
	}

	public static int getWordAttribute(Element element, String name, int defaultValue) {
		return getUnsignedAttribute(element, name, defaultValue, 0xFFFF);
	}

	/** Sets a word/address value (0-65535) as a "0x"-prefixed, 4-digit hexadecimal attribute. */
	public static void setWordAttributeHex(Element element, String name, int value) {
		element.setAttribute(name, "0x" + HexUtility.getLongValueHexString(value & 0xFFFF, 4));
	}

	/** Sets a size value as a "0x"-prefixed hexadecimal attribute (as many digits as needed). */
	public static void setSizeAttributeHex(Element element, String name, long value) {
		element.setAttribute(name, "0x" + Long.toHexString(value).toUpperCase());
	}

	/** Sets a byte array as a "0x"-prefixed hexadecimal attribute, 2 digits per byte. */
	public static void setByteArrayAttributeHex(Element element, String name, byte[] value) {
		StringBuilder buffer = new StringBuilder("0x");
		for (byte b : value) {
			buffer.append(HexUtility.getByteValueHexString(b & 0xFF));
		}
		element.setAttribute(name, buffer.toString());
	}

	/** Parses a "0x"-prefixed hexadecimal byte array attribute, or {@code null} if missing or malformed. */
	public static byte[] getByteArrayAttribute(Element element, String name) {
		if (!element.hasAttribute(name)) {
			return null;
		}
		String value = element.getAttribute(name);
		if (value.length() < 2 || value.charAt(0) != '0' || (value.charAt(1) != 'x' && value.charAt(1) != 'X')) {
			return null;
		}
		String hex = value.substring(2);
		if (hex.length() % 2 != 0) {
			return null;
		}
		byte[] result = new byte[hex.length() / 2];
		try {
			for (int i = 0; i < result.length; i++) {
				result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
			}
		} catch (NumberFormatException e) {
			return null;
		}
		return result;
	}

	public static long getSizeAttribute(Element element, String name, long defaultValue) {
		if (!element.hasAttribute(name)) {
			return defaultValue;
		}
		try {
			return parseUnsigned(element.getAttribute(name));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static int getUnsignedAttribute(Element element, String name, int defaultValue, int maxValue) {
		if (!element.hasAttribute(name)) {
			return defaultValue;
		}
		try {
			long value = parseUnsigned(element.getAttribute(name));
			return value >= 0 && value <= maxValue ? (int) value : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/** Parses a decimal or "0x"-prefixed hexadecimal number. */
	private static long parseUnsigned(String value) {
		if (value.startsWith("0x") || value.startsWith("0X")) {
			return Long.parseLong(value.substring(2), 16);
		}
		return Long.parseLong(value);
	}
}
