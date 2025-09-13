package util;

public class M68kOpcodeQuickDecoder {
	
	public static class Instruction {
        public String group;
        public String mnemonic;

        public Instruction(String group, String mnemonic) {
            this.group = group;
            this.mnemonic = mnemonic;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s", group, mnemonic);
        }
    }

    public static Instruction decode(int opcode) {
        int main = (opcode >> 12) & 0xF;
        String group;
        String mnemonic;

        switch (main) {
            case 0x0: group = "ORI/ANDI/SUBI/etc (Immediate)"; mnemonic = "ORI/ANDI/SUBI"; break;
            case 0x1: group = "MOVE.B <ea>,Dn"; mnemonic = "MOVE.B"; break;
            case 0x2: group = "MOVE.L <ea>,Dn"; mnemonic = "MOVE.L"; break;
            case 0x3: group = "MOVE.W <ea>,Dn"; mnemonic = "MOVE.W"; break;
            case 0x4: group = "Misc (CHK, LEA, TST, etc)"; mnemonic = "CHK/LEA/TST"; break;
            case 0x5: group = "ADDQ/SUBQ/Scc/DBcc/TRAPcc"; mnemonic = "ADDQ/SUBQ/etc"; break;
            case 0x6: group = "Bcc/BSR/BRA"; mnemonic = "Bcc/BSR/BRA"; break;
            case 0x7: group = "MOVEQ"; mnemonic = "MOVEQ"; break;
            case 0x8: group = "OR/DIV/SBCD"; mnemonic = "OR/DIV/SBCD"; break;
            case 0x9: group = "SUB/SUBX"; mnemonic = "SUB/SUBX"; break;
            case 0xA: group = "Unassigned / Line-A trap"; mnemonic = "Line-A trap"; break;
            case 0xB: group = "CMP/EOR"; mnemonic = "CMP/EOR"; break;
            case 0xC: group = "AND/EXG/MUL/ABCD"; mnemonic = "AND/EXG/MUL/ABCD"; break;
            case 0xD: group = "ADD/ADDX"; mnemonic = "ADD/ADDX"; break;
            case 0xE: group = "Shift/Rotate"; mnemonic = "Shift/Rotate"; break;
            case 0xF: group = "Line-F trap"; mnemonic = "Line-F trap"; break;
            default: group = "Unknown"; mnemonic = "UNKNOWN"; break;
        }

        return new Instruction(group, mnemonic);
    }
    
    public static void main(String[] args) {
        int opcode = 0xB088; // exemplo CMP/EOR
        Instruction instr = decode(opcode);
        System.out.println(instr);

        opcode = 0x7005; // exemplo MOVEQ
        instr = decode(opcode);
        System.out.println(instr);

        opcode = 0x6010; // exemplo BRA
        instr = decode(opcode);
        System.out.println(instr);
    }

}
