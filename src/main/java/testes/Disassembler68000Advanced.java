package testes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Disassembler68000Advanced {

    private byte[] rom;
    private Set<Integer> branchTargets = new HashSet<>();
    
    
    public static void main(String[] args) {
        try {
            Disassembler68000Advanced dis = new Disassembler68000Advanced("D:\\PROJETOS_EMULADOR\\java-genesis\\roms\\SONIC1.BIN");
            dis.disassemble();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Disassembler68000Advanced(String romPath) throws IOException {
        File file = new File(romPath);
        rom = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(rom);
        }
    }

    public void disassemble() {
        System.out.println("ROM size: " + rom.length + " bytes\n");
        int pc = 0;

        while (pc < rom.length) {
            int opcode = ((rom[pc] & 0xFF) << 8) | (rom[pc + 1] & 0xFF);

            // Detecta strings ASCII simples
            if (isAsciiString(pc)) {
                String str = readAsciiString(pc);
                System.out.printf("%06X: ----  ASCII: \"%s\"\n", pc, str);
                pc += str.length();
                continue;
            }

            String instr = decodeInstruction(opcode, pc);
            System.out.printf("%06X: %04X  %s\n", pc, opcode, instr);
            pc += 2;
        }
    }

    private boolean isAsciiString(int pc) {
        int count = 0;
        while (pc + count < rom.length && rom[pc + count] >= 0x20 && rom[pc + count] <= 0x7E) {
            count++;
        }
        return count >= 4; // strings curtas ignoradas
    }

    private String readAsciiString(int pc) {
        int start = pc;
        StringBuilder sb = new StringBuilder();
        while (pc < rom.length && rom[pc] >= 0x20 && rom[pc] <= 0x7E) {
            sb.append((char) rom[pc]);
            pc++;
        }
        return sb.toString();
    }

    private String decodeInstruction(int opcode, int pc) {
        int hi = (opcode & 0xF000) >> 12;
        int lowByte = opcode & 0xFF;

        switch (hi) {
            case 0x1: return "MOVE";
            case 0x2: return "MOVE";
            case 0x3: return "MOVE";
            case 0x4: return "NEG/CLR";
            case 0x5: return "ADD";
            case 0x6: {
                int offset = lowByte;
                if ((offset & 0x80) != 0) offset -= 0x100; // signed offset
                int target = pc + 2 + offset;
                branchTargets.add(target);
                return String.format("BNE %06X", target);
            }
            case 0x7: return "MOVEQ";
            case 0x8: return "JSR/BSR";
            case 0xB: return "CMP";
            case 0xC: return "AND/OR";
            case 0xD: return "ADDQ/SUBQ";
            case 0xE: return "LSR/ASR/ROL/ROR";
            case 0xF: return "Misc/SHIFT/ROT";
            default: return "???";
        }
    }
}