/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * A workspace: the currently loaded disassembly project.
 * <p>
 * Ported from Workspace.h / Workspace.cpp, currently only as much as
 * {@link SegmentList} and {@link Pass1} need (the system/user equate
 * lists, the instruction sets, and the segment list). The rest - the
 * computer system, profile, disassembly result, and XML persistence via
 * wudsn-base's {@code XMLUtility} - is ported later.
 *
 * @author Peter Dell
 */
public final class Workspace {

	private final EquateList systemEquateList = new EquateList(WorkspaceProperty.SYSTEM_EQUATES);
	private final EquateList userEquateList = new EquateList(WorkspaceProperty.USER_EQUATES);

	private final InstructionSet instructionSetMOS6502 = new InstructionSetMOS6502("MOS 6502");
	private final InstructionSet instructionSetMOS65C02 = new InstructionSetMOS65C02("MOS 65C02");

	private final SegmentList segmentList = new SegmentList(this);

	public SegmentList getSegmentList() {
		return segmentList;
	}

	public EquateList getSystemEquateList() {
		return systemEquateList;
	}

	public EquateList getUserEquateList() {
		return userEquateList;
	}

	public InstructionSet getInstructionSet(ProcessorType processorType) {
		switch (processorType) {
		case MOS6502:
			return instructionSetMOS6502;
		case MOS65C02:
			return instructionSetMOS65C02;
		default:
			throw new IllegalArgumentException("Invalid processor type: " + processorType + ".");
		}
	}
}
