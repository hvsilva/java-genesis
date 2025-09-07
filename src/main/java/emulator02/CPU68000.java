package emulator02;

public class CPU68000 {	
	
	private final GenInstruction[] instructions = new GenInstruction[0x10000];

    private int[] registers = new int[16]; // D0-D7 = 0-7, A0-A7 = 8-15
    private int pc; // Program Counter
    private final Memory memory;

    // Flags (simplificadas)
    private boolean flagZ; // Zero
    private boolean flagN; // Negative
    private boolean flagC; // Carry
    private boolean flagV; // Overflow

    public CPU68000(Memory mem) {
        this.memory = mem;
        initInstructions();
        reset();
    }

    /** Reset realista (SP e PC vêm da ROM) */
    public void reset() {
        int initialSP = memory.readLong(0);
        int initialPC = memory.readLong(4);
        setSP(initialSP);
        setPC(initialPC);
    }

    /** Executa uma instrução e retorna ciclos gastos */
    public int runInstruction() {
        int opcode = memory.readWord(pc);
        pc += 2; // Avança o program counter (normalmente 2 bytes para 68000)

        GenInstruction instr = instructions[opcode];
        if (instr != null) {
            instr.run(opcode);
            dumpState(opcode);
        } else {
            System.out.printf("Opcode não implementado: %04X%n", opcode);
        }

        return estimateCycles(opcode);
    }

    /** Mantém compatibilidade: step() chama runInstruction() */
    public void step() {
        runInstruction();
    }

    /** Estimativa simplificada de ciclos */
    private int estimateCycles(int opcode) {
        if ((opcode & 0xF000) == 0x1000) return 4; // MOVE
        if ((opcode & 0xF000) == 0x2000) return 4; // ADD
        if ((opcode & 0xF000) == 0x3000) return 4; // SUB
        if ((opcode & 0xFFFF) == 0x4E71) return 4; // NOP
        return 8; // valor padrão
    }

    /** Inicializa mapa de instruções */
    private void initInstructions() {
        // MOVE imediato -> Dx
        for (int i = 0x1000; i <= 0x1FFF; i++) {
            instructions[i] = this::executeMOVE;
        }

        // ADD imediato -> Dx
        for (int i = 0x2000; i <= 0x2FFF; i++) {
            instructions[i] = this::executeADD;
        }

        // SUB imediato -> Dx
        for (int i = 0x3000; i <= 0x3FFF; i++) {
            instructions[i] = this::executeSUB;
        }

        // JMP absoluto
        instructions[0x4000] = this::executeJMP;

        // NOP
        instructions[0x4E71] = (opcode) -> {
            System.out.println("NOP");
        };
    }

    // ========== Implementação das instruções didáticas ==========

    private void executeMOVE(int opcode) {
        int reg = opcode & 0x0007;
        int value = memory.readWord(pc);
        pc += 2;
        setD(reg, value);
        updateFlags(value);
        System.out.printf("MOVE #%d -> D%d%n", value, reg);
    }

    private void executeADD(int opcode) {
        int reg = opcode & 0x0007;
        int value = memory.readWord(pc);
        pc += 2;
        int result = getD(reg) + value;
        setD(reg, result);
        flagC = (result & 0x10000) != 0;
        flagV = ((getD(reg) ^ value) >= 0) && ((getD(reg) ^ result) < 0);
        updateFlags(result);
        System.out.printf("ADD #%d -> D%d (novo valor: %d)%n", value, reg, getD(reg));
    }

    private void executeSUB(int opcode) {
        int reg = opcode & 0x0007;
        int value = memory.readWord(pc);
        pc += 2;
        int result = getD(reg) - value;
        setD(reg, result);
        flagC = (result < 0);
        flagV = ((getD(reg) ^ value) < 0) && ((getD(reg) ^ result) < 0);
        updateFlags(result);
        System.out.printf("SUB #%d -> D%d (novo valor: %d)%n", value, reg, getD(reg));
    }

    private void executeJMP(int opcode) {
        int addr = memory.readWord(pc);
        pc = addr;
        System.out.printf("JMP %04X%n", addr);
    }

    // ========== Flags e debug ==========

    private void updateFlags(int result) {
        flagZ = (result & 0xFFFF) == 0;
        flagN = (result & 0x8000) != 0;
    }

    private void dumpState(int opcode) {
        System.out.printf("PC=%04X  OPCODE=%04X%n", pc, opcode);
        for (int i = 0; i < 8; i++)
            System.out.printf("D%d=%04X ", i, getD(i));
        System.out.println();
        for (int i = 0; i < 8; i++)
            System.out.printf("A%d=%04X ", i, getA(i));
        System.out.printf("SP=%04X%n", getSP());
        System.out.printf("Flags [Z=%b N=%b C=%b V=%b]%n", flagZ, flagN, flagC, flagV);
    }

    // ========== Registradores ==========

    public int getD(int i) { return registers[i] & 0xFFFF; }
    public void setD(int i, int value) { registers[i] = value & 0xFFFF; }

    public int getA(int i) { return registers[8 + i] & 0xFFFF; }
    public void setA(int i, int value) { registers[8 + i] = value & 0xFFFF; }

    public int getPC() { return pc; }
    public void setPC(int addr) { pc = addr & 0xFFFF; }

    public int getSP() { return registers[15]; }
    public void setSP(int value) { registers[15] = value & 0xFFFF; }
}
