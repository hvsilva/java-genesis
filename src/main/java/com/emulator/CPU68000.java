package com.emulator;

import com.emulator.addressing.AbsoluteLong;
import com.emulator.addressing.AbsoluteShort;
import com.emulator.addressing.AddressRegisterDirect;
import com.emulator.addressing.AddressRegisterIndirect;
import com.emulator.addressing.AddressRegisterIndirectPostIncrement;
import com.emulator.addressing.AddressRegisterIndirectPreDecrement;
import com.emulator.addressing.AddressRegisterWithDisplacement;
import com.emulator.addressing.AddressRegisterWithIndex;
import com.emulator.addressing.AddressingMode;
import com.emulator.addressing.DataRegisterDirect;
import com.emulator.addressing.ImmediateData;
import com.emulator.addressing.PCWithDisplacement;
import com.emulator.addressing.PCWithIndex;
import com.emulator.instruction.ABCD;
import com.emulator.instruction.BCC;
import com.emulator.instruction.MOVE;
import com.emulator.instruction.Operation;
import com.emulator.instruction.TST;

public class CPU68000 {	
	
	GenInstruction[] instructions = new GenInstruction[0x10000];
	AddressingMode addressingModes[];
	public Memory memory;   

	private long[] D = new long[8]; // D0-D7
	private long[] A = new long[8]; // A0-A7 (A7 = USP/SSP)	 
    
    public long PC;   // Program Counter
	public long SSP;  // Stack Pointer (supervisor)
	public long USP;  // Stack Pointer (user) 
	public int  SR;   // Status Register
	
	public boolean stop = false;

    // Flags simplificadas
    private boolean flagZ; // Zero
    private boolean flagN; // Negative
    private boolean flagC; // Carry
    private boolean flagV; // Overflow
    private boolean flagX; // Extend (necessário para BCD)

    public CPU68000(Memory mem) {
        this.memory = mem;
        initInstructions();        
        this.addressingModes = new AddressingMode[] {
            new DataRegisterDirect(this),                  // 0
            new AddressRegisterDirect(this),               // 1
            new AddressRegisterIndirect(this),             // 2
            new AddressRegisterIndirectPostIncrement(this),// 3
            new AddressRegisterIndirectPreDecrement(this), // 4
            new AddressRegisterWithDisplacement(this),     // 5
            new AddressRegisterWithIndex(this),            // 6
            new AbsoluteShort(this),                       // 7, reg=0
            new AbsoluteLong(this),                        // 7, reg=1
            new PCWithDisplacement(this),                  // 7, reg=2
            new PCWithIndex(this),                         // 7, reg=3
            new ImmediateData(this)                        // 7, reg=4 (normalmente só fonte)
            // Para reg=5 ou reg=6, pode ser modos reservados ou especiais, adicione se necessário!
            };
    }
    
    /** Inicializa mapa de instruções */
    private void initInstructions() {
    	new ABCD(this).generate();
    	new MOVE(this).generate();
    	new TST(this).generate();
    	new BCC(this).generate();
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
    public int runInstruction(boolean print) {
    	// Busca o opcode da memória (bus) na posição do PC (Program Counter)
    	long opcode = memory.read(PC, Size.WORD); 
		

        GenInstruction instr = instructions[(int) opcode];
        
		if (print) {
			StringBuilder sb = new StringBuilder();			
			printDebug(opcode, sb);
			System.out.println(sb.toString()); // Imprime estado se solicitado
		}
        
        
        int cycles = 0;
        if (instr != null) {
        	
//        	System.out.printf("[Instruction]: %s%n", instr);
//          System.err.printf("Opcode %04X não implementado em PC=%08X%n [Instruction]: %s%n", opcode, PC, instr);
        	
        	
            instr.run((int) opcode);  
            
            PC = (PC + 2) & 0xFFFFFF;
    
            
//            dumpState((int) opcode);   
//            cycles = instr.getCycles(opcode); 
        } else {
//            System.out.printf("Opcode não implementado: %04X%n", opcode);
            System.err.printf("Opcode %04X não implementado em PC=%08X%n ", opcode, PC);
            cycles = estimateCycles((int) opcode);
        }
        return cycles;
    }

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
            System.out.printf("A%d=%08X ", i, getAByte(i));
        System.out.printf("SSP=%08X%n", SSP);
        System.out.printf("Flags [X=%b Z=%b N=%b C=%b V=%b]%n", flagX, flagZ, flagN, flagC, flagV);
    }
    
	private void printDebug(long opcode, StringBuilder sb) {	
		
		// Monta informações de debug sobre o estado atual da CPU
		sb.append(pad4((int) PC) + " - Opcode: " + pad4((int) opcode) + " - SR: " + pad4(SR) + " - SSP: " + pad4((int) SSP) + " - USP: " + pad4((int) USP) + "\r\n");				
		
		for (int j = 0; j < 8; j++) {
			sb.append(" A" + j + ":" + Integer.toHexString((int) A[j]));
		}
		sb.append("\r\n");
		for (int j = 0; j < 8; j++) {
			sb.append(" D" + j + ":" + Integer.toHexString((int) D[j]));
		}
		sb.append("\r\n");
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
	
	public long getAByte(int register) {
		return A[register] & 0xFF;
	}
	
	public void setAByte(int register, long data) {
		long reg = A[register];
		A[register] = ((reg & 0xFFFF_FF00) | (data & 0xFF));

		if (register == 7) {
			if ((SR & 0x2000) == 0x2000) {
				SSP = (int) A[register];
			} else {
				USP = (int) A[register];
			}
		}
	}
	
	public void setDLong(int register, long data) {
		D[register] = data & 0xFFFF_FFFFL;
	}
	
	public long getDLong(int register) {
		return D[register] & 0xFFFF_FFFFL;
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
	
	public long getDWord(int register) {
		return D[register] & 0xFFFF;
	}
	
	public void setDWord(int register, long data) {
		long reg = D[register];
		D[register] = ((reg & 0xFFFF_0000) | (data & 0xFFFF));
	}
	
	public long getAWord(int register) {
		return A[register] & 0xFFFF;
	}
	
	public void setAWord(int register, long data) {
		long reg = A[register];
		A[register] = ((reg & 0xFFFF_0000) | (data & 0xFFFF));

		if (register == 7) {
			if ((SR & 0x2000) == 0x2000) {
				SSP = (int) A[register];
			} else {
				USP = (int) A[register];
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
	
	public void setN() {
		SR = bitSet(SR, 3);
	}
	
	public void clearN() {
		SR = bitReset(SR, 3);
	}
	
	public void clearV() {
		SR = bitReset(SR, 1);
	}
	
	public void setV() {
		SR = bitSet(SR, 1);
	}
	

	public boolean isC() {
		return bitTest(SR, 0);
	}
	
	public boolean isZ() {
		return bitTest(SR, 2);
	}
	
	public boolean isV() {
		return bitTest(SR, 1);
	}
	
	public boolean isN() {
		return bitTest(SR, 3);
	}
	
	public int bitSet(int address, int position) {
		return address | (1 << position);
	}
	
	public int bitReset(int address, int position) {
		return address & ~(1 << position);
	}
	
	public boolean bitTest(long address, int position) {
		return ((address & (1 << position)) != 0);
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
	
	public int getInterruptMask() {
	    return (SR >> 8) & 0x7; // bits 8-10 = interrupt mask
	}
	
//	Condition code 'cc' specifies one of the following:
//0000 F  False            Z = 1      1000 VC oVerflow Clear   V = 0
//0001 T  True             Z = 0      1001 VS oVerflow Set     V = 1
//0010 HI HIgh             C + Z = 0  1010 PL PLus             N = 0
//0011 LS Low or Same      C + Z = 1  1011 MI MInus            N = 1
//0100 CC Carry Clear      C = 0      1100 GE Greater or Equal N (+) V = 0
//0101 CS Carry Set        C = 1      1101 LT Less Than        N (+) V = 1
//0110 NE Not Equal        Z = 0      1110 GT Greater Than     Z + (N (+) V) = 0
//0111 EQ EQual            Z = 1      1111 LE Less or Equal    Z + (N (+) V) = 1
	public boolean evaluateBranchCondition(int cc, Size size) {
		boolean taken;

		switch (cc) {
		case 0b0000:
			taken = true;
			break;
		case 0b0001:
			// es un BSR
			long oldPC;
			if (size == Size.BYTE) {
				oldPC = PC + 2;
			} else if (size == Size.WORD) {
				oldPC = PC + 4;
			} else {
				throw new RuntimeException("");
			}

			taken = true;

			if ((SR & 0x2000) == 0x2000) {
				SSP--;
				memory.write(SSP, oldPC & 0xFF, Size.BYTE);
				SSP--;
				memory.write(SSP, (oldPC >> 8) & 0xFF, Size.BYTE);
				SSP--;
				memory.write(SSP, (oldPC >> 16) & 0xFF, Size.BYTE);
				SSP--;
				memory.write(SSP, (oldPC >> 24), Size.BYTE);

				setALong(7, SSP);
			} else {
				USP--;
				memory.write(USP, oldPC & 0xFF, Size.BYTE);
				USP--;
				memory.write(USP, (oldPC >> 8) & 0xFF, Size.BYTE);
				USP--;
				memory.write(USP, (oldPC >> 16) & 0xFF, Size.BYTE);
				USP--;
				memory.write(USP, (oldPC >> 24), Size.BYTE);

				setALong(7, USP);
			}

			break;
		case 0b0010: // C + Z = 0 the C and Z flags are both clear
			taken = !isC() && !isZ();
			break;
		case 0b0011: // C + Z = 1 the C or Z flag is set
			taken = isC() || isZ();
			break;
		case 0b0100:
			taken = !isC();
			break;
		case 0b0101:
			taken = isC();
			break;
		case 0b0110:
			taken = !isZ();
			break;
		case 0b0111:
			taken = isZ();
			break;
		case 0b1000:
			taken = !isV();
			break;
		case 0b1001:
			taken = isV();
			break;
		case 0b1010:
			taken = !isN();
			break;
		case 0b1011:
			taken = isN();
			break;
		case 0b1100: // BGE � Branch on Greater than or Equal 1) The N and V flags are both clear 2)
						// The N and V flags are both set
			taken = (!isN() && !isV()) || (isN() && isV());
			break;
		case 0b1101: // BLT � Branch on Lower Than N (+) V = 1 1) The N flag is clear, but the V flag
						// is set 2) The N flag is set, but the V flag is clear
			taken = (!isN() && isV()) || (isN() && !isV());
			break;
		case 0b1110: // BGT Greater Than Z + (N (+) V) = 0 1) The Z, N and V flags are all clear 2)
						// The Z flag is clear, but the N and V flags are both set
			taken = (!isZ() && !isN() && !isV()) || (!isZ() && isN() && isV());
			break;
		case 0b1111: // BLE Less or Equal Z + (N (+) V) = 1 1) The Z flag is clear 2) The N flag is
						// clear, but the V flag is set 3) The N flag is set, but the V flag is clear
			taken = isZ() || (!isZ() && !isN() && isV()) || (!isZ() && isN() && !isV());
			break;
		default:
			throw new RuntimeException("not impl " + cc);
		}

		return taken;
	}
}
