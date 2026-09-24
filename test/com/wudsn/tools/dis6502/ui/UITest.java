/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.border.TitledBorder;

import com.wudsn.tools.dis6502.model.Assert;

/**
 * Shared support for every test that touches the {@code ui} package: is
 * there a real, showing top-level window available ({@link
 * #isHeadless()}), and does a constructed component tree show real texts
 * ({@link #checkTexts}/{@link #checkText}) - used by {@link
 * DialogTextsTest}, {@link PanelTextsTest}, {@link RenderingTest}, and
 * {@link com.wudsn.tools.dis6502.UIWiringTest}.
 *
 * @author Peter Dell
 */
public final class UITest {

	/** A property key that did not resolve looks like {@code Dis6502_OpenFileTitle}: word characters and underscores, no space. */
	private static final Pattern KEY_LIKE = Pattern.compile("^[A-Za-z0-9]+(_[A-Za-z0-9]+)+$");

	private UITest() {
	}

	/**
	 * Whether such a test must skip itself: there is no display, so a
	 * {@link javax.swing.JDialog}/{@link javax.swing.JFrame} cannot be
	 * constructed - or the build asked for it ({@code
	 * -Ddis6502.skipUITests=true}, e.g. on a CI runner that has a desktop but
	 * nobody to look at the windows the tests open). A plain {@link
	 * JComponent} (a panel, a menu) needs no such check.
	 */
	public static boolean isHeadless() {
		return GraphicsEnvironment.isHeadless() || Boolean.getBoolean("dis6502.skipUITests");
	}

	/** Walks the component tree under {@code root} and checks every visible text on it. */
	public static void checkTexts(String name, Container root) {
		for (Component component : root.getComponents()) {
			if (component.getClass().getName().contains(".plaf.")) {
				// Look-and-feel internals: a combo box's arrow button, a scroll bar's
				// buttons - javax.swing.plaf.* under the cross-platform "Metal"
				// default, com.sun.java.swing.plaf.<lf>.* under a native one (e.g.
				// com.sun.java.swing.plaf.windows.WindowsScrollBarUI$WindowsArrowButton).
				continue;
			}
			if (component instanceof AbstractButton) {
				checkText(name + " " + component.getClass().getSimpleName(), ((AbstractButton) component).getText());
			} else if (component instanceof JLabel) {
				String text = ((JLabel) component).getText();
				if (text != null && text.trim().length() > 1) { // " ", "-" and "" are legitimate placeholders.
					checkText(name + " JLabel", text);
				}
			}
			if (component instanceof JComponent && ((JComponent) component).getBorder() instanceof TitledBorder) {
				checkText(name + " group", ((TitledBorder) ((JComponent) component).getBorder()).getTitle());
			}
			if (component instanceof JMenu) {
				checkTexts(name, ((JMenu) component).getPopupMenu());
			}
			if (component instanceof Container && !(component instanceof Window)) {
				checkTexts(name, (Container) component);
			}
		}
	}

	/** Fails if {@code text} is empty, looks like an unresolved property key, or still has an unfilled {@code "{0}"} template. */
	public static void checkText(String where, String text) {
		if (text == null || text.trim().isEmpty()) {
			Assert.fail(where + " has no text.");
		} else if (KEY_LIKE.matcher(text).matches()) {
			Assert.fail(where + " shows a property key instead of a text: '" + text + "'.");
		} else if (text.contains("{0}")) {
			Assert.fail(where + " shows an unfilled template: '" + text + "'.");
		}
	}
}
