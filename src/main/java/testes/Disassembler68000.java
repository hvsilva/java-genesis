package testes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Disassembler68000 {

    private byte[] rom;
    
    // --- Main de teste ---
    public static void main(String[] args) throws Exception {
        Disassembler68000 dis = new Disassembler68000();
        dis.loadROM("D:\\PROJETOS_EMULADOR\\java-genesis\\roms\\SONIC1.BIN");
            
        
        // 1) Dump vetores
        dis.dumpVectors();

        // 2) Achar Reset PC
        int resetAddr = dis.readLong(4);
        System.out.printf("\nReset vector aponta para: %06X\n", resetAddr);

        // 3) Disassemble a partir do Reset
        dis.disassemble(resetAddr, 50);
    }

    public void loadROM(String path) throws IOException {
        rom = Files.readAllBytes(Paths.get(path));
        System.out.println("ROM carregada: " + path + " (" + rom.length + " bytes)");
    }

    private int readWord(int addr) {
        return ((rom[addr] & 0xFF) << 8) | (rom[addr + 1] & 0xFF);        
    }

    private int readLong(int addr) {
        int hi = readWord(addr);
        int lo = readWord(addr + 2);
        return (hi << 16) | lo;
    }

    // --- 1. Mostra tabela de vetores ---
    public void dumpVectors() {
        System.out.println("\n=== Tabela de Vetores ===");
        for (int i = 0; i < 0x400; i += 4) {
            int value = readLong(i);
            String name;
            switch (i) {
                case 0x000: name = "Initial SP"; break;
                case 0x004: name = "Reset PC"; break;
                case 0x008: name = "Bus Error"; break;
                case 0x00C: name = "Address Error"; break;
                case 0x010: name = "Illegal Instr"; break;
                case 0x014: name = "Divide by Zero"; break;
                default:    name = "Vector"; break;
            }
            System.out.printf("%06X: %08X   (%s)\n", i, value, name);
        }
    }

    // --- 2. Disassembler simples ---
    public void disassemble(int startAddr, int instrCount) {
        System.out.println("\n=== Código a partir do Reset PC ===");
        int pc = startAddr;
        for (int i = 0; i < instrCount && pc < rom.length; i++) {
            int opcode = readWord(pc);
            String instr = decode(opcode, pc);
            System.out.printf("%06X: %04X  %s\n", pc, opcode, instr);
            pc += 2;
        }
    }

    // --- 3. Decoder mais rico ---
    private String decode(int opcode, int pc) {
        int top = opcode & 0xF000;

        switch (top) {
            case 0x1000: return "MOVE.B";
            case 0x2000: return "MOVE.L";
            case 0x3000: return "MOVE.W";
            case 0x4000: return "NEG/CLR";
            case 0x5000: return "ADDQ/SUBQ";
            case 0x7000: return "MOVEQ #" + (opcode & 0xFF);
            case 0x8000: return "OR";
            case 0x9000: return "SUB";
            case 0xB000: return "CMP";
            case 0xC000: return "AND";
            case 0xD000: return "ADD";
            case 0xE000: return "SHIFT/ROT";
        }

        // BRA/BSR/Bcc
        if ((top & 0xF000) == 0x6000) {
            int cond = (opcode >> 8) & 0xF;
            int disp = opcode & 0xFF;
            if (disp == 0) disp = readWord(pc + 2); // word displacement
            if ((disp & 0x8000) != 0) disp |= 0xFFFF0000; // sinal extend
            int target = pc + 2 + disp;
            String[] conds = {
                "BRA","BSR","BHI","BLS","BCC","BCS","BNE","BEQ",
                "BVC","BVS","BPL","BMI","BGE","BLT","BGT","BLE"
            };
            return conds[cond] + " " + String.format("%06X", target);
        }

        // instruções específicas
        if ((opcode & 0xFFF8) == 0x4E70) {
            switch (opcode & 0xFF) {
                case 0x71: return "NOP";
                case 0x75: return "RTS";
                case 0x77: return "RTR";
                case 0x73: return "RTE";
            }
        }
        if ((opcode & 0xFFC0) == 0x4EC0) return "JMP";
        if ((opcode & 0xFFC0) == 0x4E80) return "JSR";

        return "???";
    }

}