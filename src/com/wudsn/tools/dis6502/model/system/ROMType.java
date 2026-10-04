/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system;

import java.util.Objects;

/**
 * A type of ROM image that a {@link ComputerSystem} offers for a raw ROM image
 * whose type it cannot tell, e.g. one of several cartridge types of the same
 * size. Keeps every system-specific type list, such as WUDSN Base's Atari
 * {@code CartridgeType}, inside the {@link ComputerSystem} subclasses: the rest
 * of dis6502 only sees {@code ROMType}s.
 * <p>
 * Identified by its computer system type (the leading part) and an id that is
 * unique within that system; the text is complete and localized, ready to be
 * shown. Created only by the {@link ComputerSystem} subclasses, which also
 * resolve it back to their own type.
 *
 * @author Peter Dell
 */
public final class ROMType {

	private final ComputerSystemType computerSystemType;
	private final String id;
	private final String text;

	public ROMType(ComputerSystemType computerSystemType, String id, String text) {
		this.computerSystemType = Objects.requireNonNull(computerSystemType, "computerSystemType");
		this.id = Objects.requireNonNull(id, "id");
		this.text = Objects.requireNonNull(text, "text");
	}

	/** @return the computer system type this ROM type belongs to, the leading part of its identity */
	public ComputerSystemType getComputerSystemType() {
		return computerSystemType;
	}

	/** @return the id, unique within the computer system type */
	public String getId() {
		return id;
	}

	/** @return the complete, localized display text */
	public String getText() {
		return text;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof ROMType other && computerSystemType == other.computerSystemType
				&& id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(computerSystemType, id);
	}

	@Override
	public String toString() {
		return text;
	}
}
