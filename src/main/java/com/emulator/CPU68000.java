package com.emulator;

import com.emulator.addressing.AddressingMode;
import com.emulator.instruction.ABCD;
import com.emulator.instruction.Operation;

public class CPU68000 {	
	
	GenInstruction[] instructions = new GenInstruction[0x10000];
	AddressingMode addressingModes[];
    private final Memory memory;   

	private long[] D = new long[8]; // D0-D7
	private long[] A = new long[8]; // A0-A7 (A7 = USP/SSP)	 
    
    public long PC;   // Program Counter
	public long SSP;  // Stack Pointer (supervisor)
	public long USP;  // Stack Pointer (user) 
	public int  SR;   // Status Register

    // Flags simplificadas
    private boolean flagZ; // Zero
    private boolean flagN; // Negative
    private boolean flagC; // Carry
    private boolean flagV; // Overflow
    private boolean flagX; // Extend (necessário para BCD)

    public CPU68000(Memory mem) {
        this.memory = mem;
        initInstructions();
        reset();
        initialize();
    }
    
    /** Inicializa mapa de instruções */
    private void initInstructions() {
    	new ABCD(this).generate();
    }
    
    /** Reset realista (SP e PC vêm da ROM) */
	public void reset() {
		SSP = 0;
		PC = 0;
	}
	
	public void initialize() {
		// the processor fetches an initial stack pointer from locations $000000-$000003
		SSP = memory.read(0, Size.LONG) & 0xFFFFFFFFL;

		USP = 0xFFFF_FFFFL;

		// initial PC specified by locations $000004-$000007
		PC = memory.read(4, Size.LONG) & 0xFFFFFFFFL;

		for (int i = 0; i < A.length; i++) {
			A[i] = 0xFFFF_FFFFL;
			D[i] = 0xFFFF_FFFFL;
		}
		A[7] = SSP;
		SR = 0x7FFF;
	}
	
    /** Executa uma instrução e retorna ciclos gastos */
    public int runInstruction() {
		int opcode = memory.read(PC, Size.WORD); // pega do bus
		PC = (PC + 2) & 0xFFFFFF;

        GenInstruction instr = instructions[opcode];
        int cycles = 0;
        if (instr != null) {
            instr.run(opcode);
            dumpState(opcode);   
            cycles = instr.getCycles(opcode); 
        } else {
            System.out.printf("Opcode não implementado: %04X%n", opcode);
            cycles = estimateCycles(opcode);
        }
        return cycles;
    }

    /** Reset realista (SP e PC vêm da ROM) */
//    public void reset() {
//        int initialSP = memory.readLong(0); // Stack Pointer (supervisor) inicial
//        int initialPC = memory.readLong(4); // PC inicial
//        
//        System.out.println("Initial SP: " + pad4(initialSP) + " - Initial PC: " + pad4(initialPC));
//        
//        setSP(initialSP);
//        setPC(initialPC);
//    }


	/** Estimativa simplificada de ciclos */
	private int estimateCycles(int opcode) {
	    if ((opcode & 0xF000) == 0x1000) return 8; // MOVE.B
	    if ((opcode & 0xF000) == 0x2000) return 8; // MOVE.L
	    if ((opcode & 0xF000) == 0xD000) return 4; // ADD
	    if ((opcode & 0xF000) == 0x9000) return 4; // SUB
	    if (opcode == 0x4E71) return 4; // NOP
	    return 8; // fallback
	}    

    private void dumpState(int opcode) {
        System.out.printf("PC=%08X  OPCODE=%04X%n", PC, opcode);
        for (int i = 0; i < 8; i++)
            System.out.printf("D%d=%02X ", i, getDByte(i));
        System.out.println();
        for (int i = 0; i < 8; i++)
            System.out.printf("A%d=%08X ", i, getALong(i));
        System.out.printf("SSP=%08X%n", SSP);
        System.out.printf("Flags [X=%b Z=%b N=%b C=%b V=%b]%n", flagX, flagZ, flagN, flagC, flagV);
    }
    
    int totalInstructions = 0;
	public void addInstruction(int opcode, GenInstruction ins) {
		GenInstruction instr = instructions[opcode];
		if (instr != null) {
			throw new RuntimeException(pad4(opcode) + " - " + instr.getClass().toGenericString());
		}
		totalInstructions++;
		instructions[opcode] = ins;
	}
	
	public final String pad4(int reg) {
		String s = Integer.toHexString(reg).toUpperCase();
		while (s.length() < 4) {
			s = "0" + s;
		}
		return s;
	}

    // ========== Registradores ==========
	public long getDByte(int register) {
		return D[register] & 0xFF;
	}
	
	public void setDByte(int register, long data) {
		long reg = D[register];
		D[register] = ((reg & 0xFFFF_FF00) | (data & 0xFF));
	}
	
	public long getALong(int register) {
		return A[register] & 0xFFFF_FFFFL;
	}
	
	public void setALong(int register, long data) {
		A[register] = data & 0xFFFF_FFFFL;
		if (register == 7) {
			if ((SR & 0x2000) == 0x2000) {
				SSP = A[7];
			} else {
				USP = A[7];
			}
		}
	}

    // ========== Flags ==========
	public boolean isX() {
		return flagX;
	}

	public void setX() {
		flagX = true;
	}

	public void clearX() {
		flagX = false;
	}

	public void setC() {
		flagC = true;
	}

	public void clearC() {
		flagC = false;
	}

	public void clearZ() {
		flagZ = false;
	}

	public void setZ() {
		flagZ = true;
	}
	
	public Operation resolveAddressingMode(Size size, int mode, int register) {
		return resolveAddressingMode(PC + 2, size, mode, register);
	}
	
	public Operation resolveAddressingMode(long offset, Size size, int mode, int register) {
		AddressingMode addressing = getAddressingMode(mode, register);
		Operation oper = new Operation();
		oper.setRegister(register);
		oper.setAddressingMode(addressing);

		addressing.calculateAddress(oper, size);

		return oper;
	}
	
	private AddressingMode getAddressingMode(int mode, int register) {
		AddressingMode addr;
		if (mode < 7) {
			addr = addressingModes[mode];
		} else {
			addr = addressingModes[mode + register];
		}
		if (addr == null) {
			throw new RuntimeException("ADDR MODE NOT ! " + mode + " " + register);
		}
		return addr;
	}
	
	public void writeKnownAddressingMode(Operation o, long data, Size size) {
		AddressingMode addressing = o.getAddressingMode();

		o.setData(data);

		if (Size.BYTE == size) {
			addressing.setByte(o);
		} else if (Size.WORD == size) {
			addressing.setWord(o);
		} else if (Size.LONG == size) {
			addressing.setLong(o);
		}
	}
}
