package emulator02;

public class CPU68000 {
    private int[] registers = new int[16]; // D0-D7, A0-A7
    private int pc; // Program Counter
    private Memory memory;

    public CPU68000(Memory mem) {
        this.memory = mem;
        this.pc = 0; // endereço inicial
    }

    public void step() {
        int opcode = memory.readWord(pc);
        pc += 2;
        decodeAndExecute(opcode);
    }

    private void decodeAndExecute(int opcode) {
        switch (opcode & 0xF000) {
            case 0x1000: // MOVE imediato -> Dx
                executeMOVE(opcode);
                break;

            case 0x2000: // ADD imediato -> Dx
                executeADD(opcode);
                break;

            case 0x3000: // SUB imediato -> Dx
                executeSUB(opcode);
                break;

            case 0x4000: // JMP absoluto
                executeJMP(opcode);
                break;

            default:
                System.out.println("Opcode não implementado: " + Integer.toHexString(opcode));
        }
    }

    /** MOVE #imediato, Dx */
    private void executeMOVE(int opcode) {
        int reg = opcode & 0x0007; // usa os 3 bits mais baixos para indicar Dx
        int value = memory.readWord(pc);
        pc += 2;
        registers[reg] = value;
        System.out.printf("MOVE #%d -> D%d%n", value, reg);
    }

    /** ADD #imediato, Dx */
    private void executeADD(int opcode) {
        int reg = opcode & 0x0007;
        int value = memory.readWord(pc);
        pc += 2;
        registers[reg] += value;
        System.out.printf("ADD #%d -> D%d (novo valor: %d)%n", value, reg, registers[reg]);
    }

    /** SUB #imediato, Dx */
    private void executeSUB(int opcode) {
        int reg = opcode & 0x0007;
        int value = memory.readWord(pc);
        pc += 2;
        registers[reg] -= value;
        System.out.printf("SUB #%d -> D%d (novo valor: %d)%n", value, reg, registers[reg]);
    }

    /** JMP endereço absoluto */
    private void executeJMP(int opcode) {
        int addr = memory.readWord(pc);
        pc = addr;
        System.out.printf("JMP %04X%n", addr);
    }

    public int getPC() {
        return pc;
    }

    public int[] getRegisters() {
        return registers;
    }
}
