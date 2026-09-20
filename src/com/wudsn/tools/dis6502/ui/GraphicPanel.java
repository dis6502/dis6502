/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Renders a raw byte buffer as a picture in one of the 8 Atari ANTIC
 * graphics modes ({@link GraphicMode}), for visually picking out where a
 * graphic/character set/font starts and ends by eye - a byte range is
 * selected by clicking or dragging down through the row it ends at.
 * <p>
 * Ported from ui/SpriteControlImpl.h/.cpp's pixel-decoding and mouse-
 * selection logic ({@code PaintAll}/{@code LButtonDown}/{@code
 * MouseMove}/{@code LButtonUp}), simplified considerably: the C++ version
 * writes palette-indexed bytes directly into a Win32 DIB via 8 near-
 * identical per-mode {@code switch} cases (one call to one of two
 * bit-extraction macros, {@code SPRITE_GET_PIXEL_1}/{@code _2}, each);
 * this uses one generic loop parameterized by {@link
 * GraphicMode#bitsPerPixel}, painting directly into a {@link
 * BufferedImage} with real RGB colors instead of a mapped system
 * palette. The vertical scroll position ({@code wIndex}, the C++
 * control's own {@code WM_VSCROLL} handling) and the bytes-per-line
 * choice ({@code wNbBytes}, its {@code WM_HSCROLL} handling) are not
 * handled by this class at all - unlike the C++ control, which owns both
 * scrollbars itself, those are external {@link javax.swing.JScrollBar}/
 * {@link javax.swing.JSpinner} controls in {@link SelectGraphicsDialog},
 * which push their values in via {@link #setIndex}/{@link
 * #setNumberOfBytesPerLine} - the same "plain Swing controls instead of
 * a custom-painted control owning its own scrollbars" substitution this
 * port already uses elsewhere (e.g. {@link DiskImageSectorsDialog}).
 * <p>
 * The fixed 320x192 logical image this renders is the same size the
 * C++ control uses for every mode (each mode's full width/height in its
 * own pixels always maps to exactly 320x192 real pixels) - scaled up by
 * {@link #ZOOM} for on-screen display, since the C++ source's own
 * comment ({@code "TODO: Test this, the actual dialog is too small!"})
 * already flags that size as cramped.
 *
 * @author Peter Dell
 */
public final class GraphicPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int IMAGE_WIDTH = 320;
	private static final int IMAGE_HEIGHT = 192;
	private static final int ZOOM = 2;

	/** Matches dwSpriteColor[] in SpriteControlImpl.cpp: black, red, blue, white. */
	private static final Color[] PALETTE = { Color.BLACK, Color.RED, Color.BLUE, Color.WHITE };

	/** Matches SPRITE_NO_SELECTION. */
	public static final int NO_SELECTION = 0xFFFF;

	private final BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);

	private byte[] buffer = new byte[0];
	private GraphicMode mode = GraphicMode.ANTIC_F;
	private int numberOfBytesPerLine = GraphicMode.ANTIC_F.bytesPerLine;
	private int index;
	private int end = NO_SELECTION;

	private Runnable selectionChangedListener = () -> {
	};

	public GraphicPanel() {
		setPreferredSize(new Dimension(IMAGE_WIDTH * ZOOM, IMAGE_HEIGHT * ZOOM));
		setBackground(Color.BLACK);

		MouseAdapter mouseHandler = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				selectAtY(e.getY());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				selectAtY(e.getY());
			}
		};
		addMouseListener(mouseHandler);
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				selectAtY(e.getY());
			}
		});

		render();
	}

	public void setSelectionChangedListener(Runnable listener) {
		this.selectionChangedListener = listener;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
		render();
	}

	public GraphicMode getMode() {
		return mode;
	}

	/** Ported from SpriteControlImpl::SetMode, minus the wNbBytes clamping - see SelectGraphicsDialog's own use of this. */
	public void setMode(GraphicMode mode) {
		this.mode = mode;
		render();
	}

	public int getNumberOfBytesPerLine() {
		return numberOfBytesPerLine;
	}

	public void setNumberOfBytesPerLine(int numberOfBytesPerLine) {
		this.numberOfBytesPerLine = numberOfBytesPerLine;
		render();
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
		render();
	}

	public int getSelection() {
		return end;
	}

	public void setSelection(int end) {
		this.end = end;
		render();
	}

	/**
	 * Ported from SpriteControlImpl::LButtonDown/MouseMove/LButtonUp (all
	 * three do the same thing): selects from {@link #index} down through
	 * the row the given screen Y coordinate falls in.
	 */
	private void selectAtY(int screenY) {
		int row = screenY / (mode.pixelHeight * ZOOM);
		int newEnd = index + (row * numberOfBytesPerLine) - 1;
		if (newEnd != end) {
			end = newEnd;
			render();
			selectionChangedListener.run();
		}
	}

	/** Ported from SpriteControlImpl::PaintAll. */
	private void render() {
		Graphics2D g = image.createGraphics();
		try {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

			int bitsPerPixel = mode.bitsPerPixel();
			int bufferIndex = index;

			for (int screenY = 0; screenY < IMAGE_HEIGHT; screenY += mode.pixelHeight) {
				if (bufferIndex >= buffer.length) {
					break;
				}
				boolean selected = end != NO_SELECTION && bufferIndex <= end;

				int rowBytes = Math.min(numberOfBytesPerLine, buffer.length - bufferIndex);
				int screenX = 0;
				for (int column = 0; column < rowBytes; column++) {
					int value = buffer[bufferIndex + column] & 0xFF;
					if (selected) {
						value ^= 0xFF;
					}
					for (int pixel = 0; pixel < mode.pixelsPerByte; pixel++) {
						int colorIndex;
						if (bitsPerPixel == 1) {
							colorIndex = ((value >> (7 - pixel)) & 1) != 0 ? 3 : 0;
						} else {
							colorIndex = (value >> ((3 - pixel) * 2)) & 0x3;
						}
						g.setColor(PALETTE[colorIndex]);
						g.fillRect(screenX, screenY, mode.pixelWidth, mode.pixelHeight);
						screenX += mode.pixelWidth;
					}
				}

				// Fill the rest of the row's width too (even past what numberOfBytesPerLine
				// shows), matching PaintAll's own "fill the rest of the line" step.
				if (screenX < IMAGE_WIDTH) {
					g.setColor(selected ? Color.WHITE : Color.BLACK);
					g.fillRect(screenX, screenY, IMAGE_WIDTH - screenX, mode.pixelHeight);
				}

				bufferIndex += rowBytes;
			}
		} finally {
			g.dispose();
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g2.drawImage(image, 0, 0, IMAGE_WIDTH * ZOOM, IMAGE_HEIGHT * ZOOM, null);
	}
}
