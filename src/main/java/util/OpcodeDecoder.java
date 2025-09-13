package util;

public class OpcodeDecoder {

	public static String decode(int opcode) {
		int main = (opcode >> 12) & 0xF;

		switch (main) {
		case 0x0:
			return "ORI/ANDI/SUBI/etc (Immediate to ...)";
		case 0x1:
			return "MOVE.B <ea>,Dn";
		case 0x2:
			return "MOVE.L <ea>,Dn";
		case 0x3:
			return "MOVE.W <ea>,Dn";
		case 0x4:
			return "Misc (CHK, LEA, TST, etc)";
		case 0x5:
			return "ADDQ/SUBQ/Scc/DBcc/TRAPcc";
		case 0x6:
			return "Bcc/BSR/BRA";
		case 0x7:
			return "MOVEQ";
		case 0x8:
			return "OR/DIV/SBCD";
		case 0x9:
			return "SUB/SUBX";
		case 0xA:
			return "Unassigned / Line-A trap";
		case 0xB:
			return "CMP/EOR";
		case 0xC:
			return "AND/EXG/MUL/ABCD";
		case 0xD:
			return "ADD/ADDX";
		case 0xE:
			return "Shift/Rotate";
		case 0xF:
			return "Line-F trap";
		default:
			return "Unknown";
		}
	}

}
