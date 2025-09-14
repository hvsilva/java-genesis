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
	            // Verifica subgrupos
	            switch (opcode & 0xFFF8) {
	                case 0x4E70: return "RESET";
	                case 0x4E71: return "NOP";
	                case 0x4E72: return "STOP";
	                case 0x4E73: return "RTE";
	                case 0x4E74: return "RTD"; // em algumas variantes
	                case 0x4E75: return "RTS";
	                case 0x4E76: return "TRAPV";
	                case 0x4E77: return "RTR";
	            }
	            if ((opcode & 0xFFC0) == 0x4800) return "LINK";
	            if ((opcode & 0xFFC0) == 0x4E50) return "TRAP #n";
	            if ((opcode & 0xFFC0) == 0x4E40) return "TRAP #n";
	            if ((opcode & 0xFF00) == 0x4A00) return "TST";
	            if ((opcode & 0xF1C0) == 0x41C0) return "LEA";
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
