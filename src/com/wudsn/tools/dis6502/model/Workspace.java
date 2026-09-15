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
 * {@link SegmentList}, {@link Pass1}, and {@link DisassemblyLineWriter}
 * need (the system/user equate lists, the instruction sets, the segment
 * list, the profile, and the disassembly result). The rest - the computer
 * system and XML persistence via wudsn-base's {@code XMLUtility} - is
 * ported later.
 *
 * @author Peter Dell
 */
public final class Workspace {

	private final EquateList systemEquateList = new EquateList(WorkspaceProperty.SYSTEM_EQUATES);
	private final EquateList userEquateList = new EquateList(WorkspaceProperty.USER_EQUATES);

	private final InstructionSet instructionSetMOS6502 = new InstructionSetMOS6502("MOS 6502");
	private final InstructionSet instructionSetMOS65C02 = new InstructionSetMOS65C02("MOS 65C02");

	private final SegmentList segmentList = new SegmentList(this);
	private final Profile profile = new Profile();
	private final DisassemblyResult disassemblyResult = new DisassemblyResult();

	public SegmentList getSegmentList() {
		return segmentList;
	}

	public Profile getProfile() {
		return profile;
	}

	public DisassemblyResult getDisassemblyResult() {
		return disassemblyResult;
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
