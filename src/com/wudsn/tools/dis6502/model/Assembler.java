/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Parses one line of 6502 assembly source (as produced when a disassembly
 * listing line is hand-edited) back into an {@link Instruction} plus its
 * operand value.
 * <p>
 * Uses single-element arrays for parameters that need to be written back to
 * the caller (position/value/error), the same technique used elsewhere in
 * this codebase. Every place the arithmetic needs to wrap at 16 bits is
 * given an explicit {@code & 0xFFFF} mask, since a Java {@code int} would
 * otherwise not wrap that way.
 *
 * @author Peter Dell
 */
public final class Assembler {

	private Assembler() {
	}

	public static final class Result {
		public Instruction instruction;
		public int value;
		public String comment = "";
		public String error = "";
	}

	public static Result parseLine(Workspace workspace, String fullLine, int address, ProcessorType processorType) {
		Result result = new Result();

		// Everything from the first ';' to the end of the line is a comment, regardless of
		// whether the code before it parses successfully.
		int semicolonPos = fullLine.indexOf(';');
		String line = semicolonPos < 0 ? fullLine : fullLine.substring(0, semicolonPos);
		result.comment = semicolonPos < 0 ? "" : fullLine.substring(semicolonPos).trim();

		int[] pos = { 0 };
		OperandMode operandMode = OperandMode.Unknown;

		while (peek(line, pos[0]) == ' ') {
			pos[0]++;
		}

		StringBuilder instructionNameBuilder = new StringBuilder();
		for (int index = 0; index < 3 && peek(line, pos[0]) != '\0'; index++) {
			instructionNameBuilder.append(line.charAt(pos[0]++));
		}
		String instructionName = instructionNameBuilder.toString();

		while (peek(line, pos[0]) == ' ') {
			pos[0]++;
		}

		int[] value = { 0 };
		String[] error = { "" };

		if (peek(line, pos[0]) == '\0') {
			operandMode = OperandMode.Implied;
		} else if (peek(line, pos[0]) == '#') {
			operandMode = OperandMode.Immediate;
			pos[0]++;
			getExpressionValue(workspace, line, pos, value, error);
		} else if ((peek(line, pos[0]) == 'a' || peek(line, pos[0]) == 'A')
				&& (peek(line, pos[0] + 1) == ' ' || peek(line, pos[0] + 1) == '\0')) {
			operandMode = OperandMode.Accumulator;
			pos[0]++;

			while (peek(line, pos[0]) == ' ') {
				pos[0]++;
			}

			if (peek(line, pos[0]) != '\0') {
				error[0] = "ERROR: garbage at end of line";
			}
		} else if (peek(line, pos[0]) == '(') {
			pos[0]++;
			getExpressionValue(workspace, line, pos, value, error);
			if (peek(line, pos[0]) == ')') {
				pos[0]++;

				while (peek(line, pos[0]) == ' ') {
					pos[0]++;
				}

				if (peek(line, pos[0]) == '\0') {
					operandMode = OperandMode.Indirect;
				} else if (peek(line, pos[0]) == ',') {
					pos[0]++;

					if (peek(line, pos[0]) == 'Y' || peek(line, pos[0]) == 'y') {
						operandMode = OperandMode.IndirectIndexed;
						pos[0]++;

						while (peek(line, pos[0]) == ' ') {
							pos[0]++;
						}

						if (peek(line, pos[0]) != '\0') {
							error[0] = "ERROR: garbage at end of line";
						}
					}
				}
			} else if (peek(line, pos[0]) == ',') {
				pos[0]++;

				while (peek(line, pos[0]) == ' ') {
					pos[0]++;
				}

				if (peek(line, pos[0]) == 'X' || peek(line, pos[0]) == 'x') {
					pos[0]++;

					while (peek(line, pos[0]) == ' ') {
						pos[0]++;
					}

					if (peek(line, pos[0]) == ')') {
						operandMode = OperandMode.IndexedIndirect;
						pos[0]++;

						while (peek(line, pos[0]) == ' ') {
							pos[0]++;
						}

						if (peek(line, pos[0]) != '\0') {
							error[0] = "ERROR: garbage at end of line";
						}
					} else {
						error[0] = "ERROR: missing ')'";
					}
				}
			}
		} else if (peek(line, pos[0]) == '$' || Character.isLetterOrDigit(peek(line, pos[0]))) {
			getExpressionValue(workspace, line, pos, value, error);

			if (peek(line, pos[0]) == '\0') {
				if (instructionName.equalsIgnoreCase("BEQ") || instructionName.equalsIgnoreCase("BNE")
						|| instructionName.equalsIgnoreCase("BCS") || instructionName.equalsIgnoreCase("BCC")
						|| instructionName.equalsIgnoreCase("BPL") || instructionName.equalsIgnoreCase("BMI")
						|| instructionName.equalsIgnoreCase("BVC") || instructionName.equalsIgnoreCase("BVS")) {
					operandMode = OperandMode.Relative;

					int relative = value[0] - address - 2;
					if (relative < -127 || relative > 127) {
						error[0] = "ERROR: address out of range";
					} else {
						value[0] = relative & 0xFFFF;
					}
				} else {
					operandMode = value[0] < 256 ? OperandMode.ZeroPage : OperandMode.Absolute;
				}
			} else if (peek(line, pos[0]) == ',') {
				pos[0]++;

				if (peek(line, pos[0]) == 'X' || peek(line, pos[0]) == 'x') {
					pos[0]++;

					while (peek(line, pos[0]) == ' ') {
						pos[0]++;
					}

					operandMode = value[0] < 256 ? OperandMode.ZeroPageX : OperandMode.AbsoluteX;

					if (peek(line, pos[0]) != '\0') {
						error[0] = "ERROR: garbage at end of line";
					}
				} else if (peek(line, pos[0]) == 'Y' || peek(line, pos[0]) == 'y') {
					pos[0]++;

					while (peek(line, pos[0]) == ' ') {
						pos[0]++;
					}

					if (value[0] < 256 && !instructionName.equalsIgnoreCase("LDA")) {
						operandMode = OperandMode.ZeroPageY;
					} else {
						operandMode = OperandMode.AbsoluteY;
					}

					if (peek(line, pos[0]) != '\0') {
						error[0] = "ERROR: garbage at end of line";
					}
				} else {
					error[0] = "ERROR: X or Y expected";
				}
			} else {
				error[0] = "ERROR: bad character after address";
			}
		} else if (peek(line, pos[0]) == '+' || peek(line, pos[0]) == '-') {
			char sign = peek(line, pos[0]);
			pos[0]++;

			getExpressionValue(workspace, line, pos, value, error);
			if (peek(line, pos[0]) == '\0') {
				operandMode = OperandMode.Relative;

				if (sign == '-') {
					value[0] = (0 - value[0]) & 0xFFFF;
				}
			} else {
				error[0] = "ERROR: garbage at end of line";
			}
		}

		result.value = value[0];
		result.error = error[0];

		if (result.error.isEmpty()) {
			InstructionSet instructionSet = workspace.getInstructionSet(processorType);
			for (Instruction instruction : instructionSet.getInstructions()) {
				if (instruction.getName().equalsIgnoreCase(instructionName) && instruction.getOperandMode() == operandMode
						&& !instruction.isUnsupportedInstruction()) {
					result.instruction = instruction;
				}
			}
		}

		return result;
	}

	private static void getExpressionValue(Workspace workspace, String line, int[] pos, int[] value, String[] error) {
		value[0] = 0;

		while (peek(line, pos[0]) == ' ') {
			pos[0]++;
		}

		char c = peek(line, pos[0]);
		if (c == '<') {
			pos[0]++;
			getExpressionValue(workspace, line, pos, value, error);
			value[0] = value[0] & 0xFF;
		} else if (c == '>') {
			pos[0]++;
			getExpressionValue(workspace, line, pos, value, error);
			value[0] = (value[0] >> 8) & 0xFF;
		} else if (Character.isLetter(c) || c == '_' || c == '@') {
			StringBuilder label = new StringBuilder();
			label.append(Character.toUpperCase(line.charAt(pos[0]++)));

			while (label.length() < 50 && (Character.isLetterOrDigit(peek(line, pos[0])) || peek(line, pos[0]) == '_'
					|| peek(line, pos[0]) == '@')) {
				label.append(Character.toUpperCase(line.charAt(pos[0]++)));
			}

			Equate equate = workspace.getEquateByLabel(label.toString());
			if (equate != null) {
				value[0] = equate.getLabelValue();
			} else {
				error[0] = "ERROR: Unknown label";
			}
		} else if (c == '$') {
			pos[0]++;

			StringBuilder hex = new StringBuilder();
			while (hex.length() < 4 && isHexDigit(peek(line, pos[0]))) {
				hex.append(line.charAt(pos[0]++));
			}

			if (hex.length() > 0) {
				value[0] = Integer.parseInt(hex.toString(), 16);
			}
		} else {
			StringBuilder decimal = new StringBuilder();
			while (decimal.length() < 5 && Character.isDigit(peek(line, pos[0]))) {
				decimal.append(line.charAt(pos[0]++));
			}

			if (decimal.length() > 0) {
				value[0] = Integer.parseInt(decimal.toString()) & 0xFFFF;
			}
		}

		while (peek(line, pos[0]) == ' ') {
			pos[0]++;
		}

		c = peek(line, pos[0]);
		if (c == '+') {
			pos[0]++;
			int[] val2 = { 0 };
			getExpressionValue(workspace, line, pos, val2, error);
			value[0] = (value[0] + val2[0]) & 0xFFFF;
		} else if (c == '-') {
			pos[0]++;
			int[] val2 = { 0 };
			getExpressionValue(workspace, line, pos, val2, error);
			value[0] = (value[0] - val2[0]) & 0xFFFF;
		} else if (c == '&') {
			pos[0]++;
			int[] val2 = { 0 };
			getExpressionValue(workspace, line, pos, val2, error);
			value[0] &= val2[0];
		} else if (c == '|') {
			pos[0]++;
			int[] val2 = { 0 };
			getExpressionValue(workspace, line, pos, val2, error);
			value[0] |= val2[0];
		}
	}

	private static boolean isHexDigit(char c) {
		return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}

	/** Returns the character at {@code pos}, or {@code '\0'} past the end of the string. */
	private static char peek(String line, int pos) {
		return pos < line.length() ? line.charAt(pos) : '\0';
	}
}
