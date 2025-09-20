package com.emulator;

import java.util.Random;

public class Emulator {

	CPU68000 cpu;
    VDP vdp;
    Memory memory;
    
	boolean writeSram;
	
	int hLinesPassed = 0;
	private boolean vintPending;
	boolean hintPending;

	int[] sram = new int[0x200];

	// https://emu-docs.org/Genesis/ssf2.txt
	boolean ssf2Mapper = false;
    
	int[] banks = new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };


    public Emulator(Memory memory, VDP vdp, CPU68000 cpu) {
        this.memory = memory;     	
        this.vdp = vdp;    
        this.cpu = cpu;
    }
    
    // =======================
 	// ======= READ ==========
 	// =======================
 	public long read(long address, Size size) {

 	    System.out.println("[CALL READ]: " + " [ADDRESS] : " + Long.toHexString(address) + " [SIZE] " + size);

 	    address &= 0xFF_FFFF; // máscara 24-bit
 	    long data = 0;

 		if (ssf2Mapper && address >= 0x080000 && address <= 0x3FFFFF) {
 			if (address >= 0x080000 && address <= 0x0FFFFF) {
 				address = (banks[1] * 0x80000) + (address - 0x80000);
 			} else if (address >= 0x100000 && address <= 0x17FFFF) {
 				address = (banks[2] * 0x80000) + (address - 0x100000);
 			} else if (address >= 0x180000 && address <= 0x1FFFFF) {
 				address = (banks[3] * 0x80000) + (address - 0x180000);
 			} else if (address >= 0x200000 && address <= 0x27FFFF) {
 				address = (banks[4] * 0x80000) + (address - 0x200000);
 			} else if (address >= 0x280000 && address <= 0x2FFFFF) {
 				address = (banks[5] * 0x80000) + (address - 0x280000);
 			} else if (address >= 0x300000 && address <= 0x37FFFF) {
 				address = (banks[6] * 0x80000) + (address - 0x300000);
 			} else if (address >= 0x380000 && address <= 0x3FFFFF) {
 				address = (banks[7] * 0x80000) + (address - 0x380000);
 			}

 			if (size == Size.BYTE) {
 				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
 					address = address - 0x200000;
 					if (address < 0x200) {
 						data = sram[(int) address];
 					} else {
 						data = 0;
 					}

 				} else {
 					data = memory.readCartridgeByte(address);
 				}

 			} else if (size == Size.WORD) {
 				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
 					address = address - 0x200000;
 					data =  sram[(int) address] << 8;
 					data |= sram[(int) address + 1];
 				} else {
 					data = memory.readCartridgeWord(address);
 				}

 			} else {
 				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
 					address = address - 0x200000;
 					data =  sram[(int) address] << 24;
 					data |= sram[(int) address + 1] << 16;
 					data |= sram[(int) address + 2] << 8;
 					data |= sram[(int) address + 3];

 				} else {
 					data = memory.readCartridgeWord(address) << 16;
 					data |= memory.readCartridgeWord(address + 2);
 				}
 			}
 			return data;
 		}
 		if (address <= 0x3F_FFFF) {
 			if (size == Size.BYTE) {
 				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
 					address = address - 0x200000;
 					if (address < 0x200) {
 						data = sram[(int) address];
 					} else {
 						data = 0;
 					}

 				} else {
 					data = memory.readCartridgeByte(address);
 				}

 			} else if (size == Size.WORD) {
 				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
 					address = address - 0x200000;
 					data =  sram[(int) address] << 8;
 					data |= sram[(int) address + 1];
 				} else {
 					data = memory.readCartridgeWord(address);
 				}

 			} else {
 				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
 					address = address - 0x200000;
 					data =  sram[(int) address] << 24;
 					data |= sram[(int) address + 1] << 16;
 					data |= sram[(int) address + 2] << 8;
 					data |= sram[(int) address + 3];

 				} else {
 					data = memory.readCartridgeWord(address) << 16;
 					data |= memory.readCartridgeWord(address + 2);

 				}
 			}
 			return data;

 		} else if (address >= 0xA00000 && address <= 0xA0FFFF) { // Z80 addressing space
// 			return z80.readMemory((int) (address - 0xA00000));

 		} else if (address == 0xA10000 || address == 0xA10001) { // Version register (read-only word-long)
 			data = getRegion();
 			if (size == Size.BYTE) {
 				return data;
 			} else {
 				return data << 8 | data;
 			}

 		} else if (address == 0xA10002 || address == 0xA10003) { // Controller 1 data
// 			return joypad.readDataRegister1();

 		} else if (address == 0xA10004 || address == 0xA10005) { // Controller 2 data
// 			return joypad.readDataRegister2();

 		} else if (address == 0xA10006 || address == 0xA10007) { // Expansion data
// 			return joypad.readDataRegister3();

 		} else if (address == 0xA1000C || address == 0xA1000D) { // Expansion Port Control
 			if (address == 0xA1000C) {
 				return 0;
 			} else if (address == 0xA1000D) {
 				return 0;
 			}

 		} else if (address == 0xA10008 || address == 0xA10009) { // Controller 1 control
 			if (size == Size.BYTE) {
// 				return joypad.readControlRegister1() & 0xFF;
 			} else {
// 				return joypad.readControlRegister1();
 			}

 		} else if (address == 0xA1000A || address == 0xA1000B) { // Controller 2 control
 			if (size == Size.BYTE) {
// 				return joypad.readControlRegister2() & 0xFF;
 			} else {
// 				return joypad.readControlRegister2();
 			}

 		} else if (address == 0xA11100 || address == 0xA11101) { // Z80 bus request
// 			return (z80.busRequested && !z80.reset) ? 0 : 1;
 			return new Random().nextBoolean() ? 1 : 0;
// 			return 0;	//	FIXME hacer esto bien

 		} else if (address == 0xC00000 || address == 0xC00002) { // VDP Data
 			if (size == Size.BYTE) {
 				return (vdp.readDataPort(size) >> 8);
 			} else if (size == Size.WORD) {
 				return (vdp.readDataPort(size));
 			} else {
 				data = vdp.readDataPort(size) << 16;
 				data |= vdp.readDataPort(size);
 			}

 		} else if (address == 0xC00001 || address == 0xC00003) { // VDP Data
 			return (vdp.readDataPort(size) & 0xFF);

 		} else if (address == 0xC00004 || address == 0xC00006) { // VDP Control
 			data = vdp.readControl();
 			if (size == Size.WORD) {
 				return data;
 			} else if (size == Size.BYTE) {
 				return data >> 8;
 			} else {
 				throw new RuntimeException();
 			}

 		} else if (address == 0xC00005 || address == 0xC00007) {
 			data = vdp.readControl();
 			if (size == Size.BYTE) {
 				return data & 0xFF;
 			} else {
 				throw new RuntimeException("");
 			}

 		} else if (address == 0xC00008 || address == 0xC00009) {
 			int v = vdp.line;
 			int h = new Random().nextInt(256);
 			if (size == Size.WORD) {
 				return (v << 8) | h; // VDP HV counter
 			} else if (size == Size.BYTE) {
 				if (address == 0xC00008) {
 					return v;
 				} else {
 					return h;
 				}
 			}

 		} else if (address >= 0xFF0000) {
 			if (size == Size.BYTE) {
 				return memory.readRam(address);
 			} else if (size == Size.WORD) {
 				data = memory.readRam(address) << 8;
 				data |= memory.readRam(address + 1);
 				return data;
 			} else {
 				data =  memory.readRam(address) << 24;
 				data |= memory.readRam(address + 1) << 16;
 				data |= memory.readRam(address + 2) << 8;
 				data |= memory.readRam(address + 3);
 				return data;
 			}

 		} else {
// 			System.out.println("NOT MAPPED: " + pad4(address) + " - " + pad4(cpu.PC));
 			System.err.printf("NOT MAPPED: %06X%n", address);
 		}

 		return 0;
 	}

	// =======================
	// ======= WRITE =========
	// =======================
	public long write(long address, long data, Size size) {

		System.out.println("[CALL WRITE]: " + Long.toHexString(address) + " - " + size + " - " + Long.toHexString(data));				

		long addressL = (address & 0xFFFFFF); // 24-bit mask (68k bus)

		// Normaliza os dados conforme o tamanho
	    if (size == Size.BYTE) {
	        data &= 0xFF;
	    } else if (size == Size.WORD) {
	        data &= 0xFFFF;
	    } else {
	        data &= 0xFFFFFFFFL;
	    }

	    // =================================================
	    // 1. Cartridge ROM / SRAM (0x200000 – 0x20FFFF)
	    // =================================================
	    if (addressL <= 0x3FFFFF) {
	        if (addressL >= 0x200000 && addressL <= 0x20FFFF && writeSram) {
	              addressL = addressL - 0x200000;
				
				if (size == Size.BYTE) {
					if (address < 0x200) {
						sram[(int) addressL] = (int) data;
					}
					
				} else if (size == Size.WORD) {
					sram[(int) addressL] = (int) (data >> 8) & 0xFF;
					sram[(int) addressL + 1] = (int) data & 0xFF;
				} else {
					sram[(int) addressL] = (int) (data >> 24) & 0xFF;
					sram[(int) addressL + 1] = (int) (data >> 16) & 0xFF;
					sram[(int) addressL + 2] = (int) (data >> 8) & 0xFF;
					sram[(int) addressL + 3] = (int) data & 0xFF;
				}
	        } else {
	            System.out.println("Ignored write to ROM: " + Long.toHexString(addressL));
	        }
	    }

	    // =================================================
	    // 2. Z80 Address Space (stub)
	    // =================================================
	    else if (addressL >= 0xA00000 && addressL <= 0xA0FFFF) {
	        int z80addr = (int) (addressL - 0xA00000);
	        // TODO: implementar integração Z80
	    }

	    // =================================================
	    // 3. Joypad / I/O Ports
	    // =================================================
	    else if (addressL >= 0xA10000 && addressL <= 0xA1001F) {
	        data = getRegion(); // stub
	        if (size == Size.BYTE) {
	            return data;
	        } else {
	            return (data << 8) | data;
	        }
	    }

	    // =================================================
	    // 4. Z80 Bus Request / Reset
	    // =================================================
	    else if (addressL == 0xA11100 || addressL == 0xA11101) {
	        // TODO: controlar bus request
	    } else if (addressL == 0xA11200 || addressL == 0xA11201) {
	        // TODO: controlar reset
	    }

	    // =================================================
	    // 5. SRAM Enable Register
	    // =================================================
	    else if (addressL == 0xA130F1) {
	        writeSram = (data != 0);
	    }

	    // =================================================
	    // 6. VDP Data / Control Ports
	    // =================================================
	    else if (addressL >= 0xC00000 && addressL <= 0xC00003) {
	        vdp.writeDataPort((int) data, size);

	    } else if (addressL >= 0xC00004 && addressL <= 0xC00007) {
	        if (size == Size.BYTE) {
	            throw new RuntimeException("VDP control write with BYTE not allowed");
	        } else if (size == Size.WORD) {
	        	
				System.out.println("[ADDRESS] : " + Long.toHexString(addressL) + " [DATA] : " + data + " [DATA HEX] : " + Long.toHexString(data));
	        	
				System.out.println("[VDP Control Port write (registers) ] " + vdp.registers[1]);	

			   if (data == 33140) {
					System.out.println("[VDP Control Port write]  data: " + data);					
				}
			
				vdp.writeControlPort(data);				
				
				if (vdp.registers[1] == 116) {
					System.out.println("VDP Control Port write (registers)");
				}				
				
	        } else {
	            vdp.writeControlPort((data >> 16) & 0xFFFF);
	            vdp.writeControlPort(data & 0xFFFF);
	        }
	    }

	    // =================================================
	    // 7. Work RAM (0xFF0000 – 0xFFFFFF)
	    // =================================================
	    else if (addressL >= 0xFF0000) {	    	
	    	if (size == Size.BYTE) {
	    		memory.writeRam(addressL, data);
			} else if (size == Size.WORD) {
				memory.writeRam(addressL, (data >> 8));
				memory.writeRam(addressL + 1, (data & 0xFF));
			} else if (size == Size.LONG) {
				memory.writeRam(addressL, (data >> 24) & 0xFF);
				memory.writeRam(addressL + 1, (data >> 16) & 0xFF);
				memory.writeRam(addressL + 2, (data >> 8) & 0xFF);
				memory.writeRam(addressL + 3, (data & 0xFF));
			}
	    }

	    // =================================================
	    // Default
	    // =================================================
	    else {
	        System.err.printf("Write unmapped: %06X (data=%X)%n", addressL, data);
	    }

	    return 0;
	}

    /** Checa e dispara interrupções (VBlank/HBlank) */
    public void checkInterrupts() {
    	if (vdp.ie0) { // vint on
			if (vdp.vip == 1) { // level 6 interrupt
				vintPending = true;
			}
		}

		if (vdp.ie1) {
//			int intLine = hLinesPassed;
//			if (intLine != 0 && vdp.line == hLinesPassed) {
//				hintPending = true;
//				hLinesPassed = vdp.registers[0xA];
//			}
		}

		int mask = cpu.getInterruptMask();

		if (vintPending && mask < 0x6) {
			cpu.stop = false;

			long oldPC = cpu.PC;
			int oldSR = cpu.SR;
			long ssp = cpu.SSP;

			ssp--;
			write(ssp, oldPC & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldPC >> 8) & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldPC >> 16) & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldPC >> 24), Size.BYTE);

			ssp--;
			write(ssp, oldSR & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldSR >> 8) & 0xFF, Size.BYTE);

			long address = readInterruptVector(0x78);
			cpu.PC = address;
			cpu.SR = (cpu.SR & 0xF8FF) | 0x0600;

			cpu.SR |= 0x2000; // force supervisor mode

			cpu.setALong(7, ssp);

			vdp.vip = 0;

			vintPending = false;

			return;
		}

		if (hintPending && vdp.ie1 && mask < 0x4) {
			cpu.stop = false;

			long oldPC = cpu.PC;
			int oldSR = cpu.SR;
			long ssp = cpu.SSP;

			System.out.println("HINT ! Line: " + Integer.toHexString(vdp.line));

			ssp--;
			write(ssp, oldPC & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldPC >> 8) & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldPC >> 16) & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldPC >> 24), Size.BYTE);

			ssp--;
			write(ssp, oldSR & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldSR >> 8) & 0xFF, Size.BYTE);

			long address = readInterruptVector(0x70);
			cpu.PC = address;
			cpu.SR = (cpu.SR & 0xF8FF) | 0x0400;

			cpu.SR |= 0x2000; // force supervisor mode

			cpu.setALong(7, ssp);

			hintPending = false;
		}
    }
    
	public long readInterruptVector(long vector) {
		long address = memory.readCartridgeWord(vector) << 16;
		address |= memory.readCartridgeWord(vector + 2);
		return address;
	}
	
	public void renderScreen() {		
		
	}
	
	
	// US: A0A0 rev 0 o A1A1 rev 1
	// EU: C1C1
	// JP: ????
	// US SEGA CD: 8181
	public long getRegion() {
		return 0;
	}
	
	 // Getters
    public CPU68000 getCpu() {
        return cpu;
    }

    public VDP getVdp() {
        return vdp;
    }

    public Memory getMemory() {
        return memory;
    }
 	
}
