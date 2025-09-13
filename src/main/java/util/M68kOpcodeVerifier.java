package util;

public class M68kOpcodeVerifier {

    public static class Instruction {
    	
        public String group;
        public String mnemonic;
        public String[] operands;
        public int size;

        public Instruction(String group, String mnemonic, String[] operands, int size) {
            this.group = group;
            this.mnemonic = mnemonic;
            this.operands = operands;
            this.size = size;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s %s (size=%d)",
                    group,
                    mnemonic,
                    operands != null ? String.join(", ", operands) : "",
                    size);
        }
    }
    
    
    public static void main(String[] args) {
        byte[] memory = {0x00, 0x00, 0x00, 0x00}; // memória de exemplo

        int opcode = 0xB088; // exemplo CMP/EOR
        Instruction instr = decode(opcode, memory, 0);
        System.out.println(instr);

        opcode = 0x7005; // MOVEQ
        instr = decode(opcode, memory, 0);
        System.out.println(instr);

        opcode = 0x6010; // BRA
        memory[2] = 0x00; memory[3] = 0x20;
        instr = decode(opcode, memory, 0);
        System.out.println(instr);
    }

    public static Instruction decode(int opcode, byte[] memory, int pc) {
        int main = (opcode >> 12) & 0xF;
        String group;

        switch (main) {
            case 0x0: group = "ORI/ANDI/SUBI/etc (Immediate)"; break;
            case 0x1: group = "MOVE.B <ea>,Dn"; break;
            case 0x2: group = "MOVE.L <ea>,Dn"; break;
            case 0x3: group = "MOVE.W <ea>,Dn"; break;
            case 0x4: group = "Misc (CHK, LEA, TST, etc)"; break;
            case 0x5: group = "ADDQ/SUBQ/Scc/DBcc/TRAPcc"; break;
            case 0x6: group = "Bcc/BSR/BRA"; break;
            case 0x7: group = "MOVEQ"; break;
            case 0x8: group = "OR/DIV/SBCD"; break;
            case 0x9: group = "SUB/SUBX"; break;
            case 0xA: group = "Unassigned / Line-A trap"; break;
            case 0xB: group = "CMP/EOR"; break;
            case 0xC: group = "AND/EXG/MUL/ABCD"; break;
            case 0xD: group = "ADD/ADDX"; break;
            case 0xE: group = "Shift/Rotate"; break;
            case 0xF: group = "Line-F trap"; break;
            default:  group = "Unknown"; break;
        }

        String mnemonic = "UNKNOWN";
        String[] operands = new String[]{};
        int size = 2; // tamanho default

        switch (main) {
            case 0x7: // MOVEQ
                mnemonic = "MOVEQ";
                operands = new String[]{"Dn", String.format("#0x%02X", opcode & 0xFF)};
                size = 2;
                break;

            case 0x6: // Bcc/BSR/BRA
                int offset = opcode & 0xFF;
                mnemonic = "BRA";
                if (offset == 0) { // word displacement
                    offset = ((memory[pc+2]&0xFF)<<8) | (memory[pc+3]&0xFF);
                    size = 4;
                } else {
                    size = 2;
                }
                operands = new String[]{String.format("0x%04X", offset)};
                break;

            case 0xB: // CMP/EOR
                int subOp = (opcode >> 6) & 0xF;
                if ((subOp & 0x8) != 0) {
                    mnemonic = "EOR";
                } else {
                    mnemonic = "CMP";
                }
                operands = new String[]{"Dn", "<ea>"}; // poderia expandir com <ea> real
                size = 2;
                break;

            case 0x0: // ORI/ANDI/SUBI
                int opType = (opcode >> 8) & 0xF;
                switch(opType) {
                    case 0:  mnemonic = "ORI"; break;
                    case 1:  mnemonic = "ANDI"; break;
                    case 2:  mnemonic = "SUBI"; break;
                    default: mnemonic = "IMM_OP"; break;
                }
                operands = new String[]{String.format("#0x%02X", opcode & 0xFF), "Dn"};
                size = 2;
                break;

            default:
                mnemonic = "OpGroup-"+Integer.toHexString(main);
                operands = new String[]{"Dn", "<ea>"};
                size = 2;
                break;
        }

        return new Instruction(group, mnemonic, operands, size);
    }

}
