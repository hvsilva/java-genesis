package com.emulator;

import java.util.Random;

public class Memory {

	private final int[] rom;
//	private final byte[] ram;  // Work RAM
//	private final int[] sram;  // Save RAM (SRAM)
	private final VDP vdp;

	boolean writeSram;

	int[] cartridge;
	
	int[] sram = new int[0x200];
	int[] ram = new int[0x10000];
	
	// https://emu-docs.org/Genesis/ssf2.txt
	boolean ssf2Mapper = false;
	
	int[] banks = new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };

	public Memory(int[] rom, Emulator emulator) {
		this.rom = rom;
//		this.ram = new byte[64 * 1024]; // 64KB RAM
//		this.sram = new int[32 * 1024]; // 32KB SRAM (padrão comum)
		this.vdp = new VDP(emulator); // placeholder do vídeo
	}

	// =======================
	// ======= READ ==========
	// =======================
//	public long read(long address, Size size) {
//
//		address &= 0xFF_FFFF; // 24-bit mask
//		long data = 0;
//		
//		System.out.println("[CALL READ]: " + " [ADDRESS] : " + Long.toHexString(address) + " [SIZE] " + size);
//
//
//		// 1. Cartridge ROM
//	    if (address <= 0x3FFFFF) {
//	        if (size == Size.BYTE) {
//	            data = readCartridgeByte((int) address);
//	        } else if (size == Size.WORD) {
//	            data = readCartridgeWord((int) address);
//	        } else if (size == Size.LONG) {
//	            // monta manualmente com dois WORDs, big-endian
//	            int hi = readCartridgeWord((int) address);
//	            int lo = readCartridgeWord((int) address + 2);
//	            data = ((long) hi << 16) | (lo & 0xFFFF);
//	        }
//	        return data;
//	    }
//
//	    // 2. SRAM (0x200000 – 0x20FFFF)
//	    if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
//	        int offset = (int) (address - 0x200000);
//	        if (size == Size.BYTE) {
//	            return sram[offset] & 0xFF;
//	        } else if (size == Size.WORD) {
//	            return ((sram[offset] & 0xFF) << 8)
//	                 |  (sram[offset + 1] & 0xFF);
//	        } else {
//	            return ((sram[offset] & 0xFF) << 24)
//	                 | ((sram[offset + 1] & 0xFF) << 16)
//	                 | ((sram[offset + 2] & 0xFF) << 8)
//	                 |  (sram[offset + 3] & 0xFF);
//	        }
//	    }
//
//	    // 3. VDP (ports) → igual ao que você já tinha validado
//	    if (address == 0xC00000 || address == 0xC00002) { // Data Port (high/word/long)
//	        if (size == Size.BYTE)
//	            return (vdp.readDataPort(size) >> 8);
//	        if (size == Size.WORD)
//	            return vdp.readDataPort(size);
//	        return (vdp.readDataPort(Size.WORD) << 16) | vdp.readDataPort(Size.WORD);
//
//	    } else if (address == 0xC00001 || address == 0xC00003) { // Data Port (low byte)
//	        return vdp.readDataPort(size) & 0xFF;
//
//	    } else if (address == 0xC00004 || address == 0xC00006) { // Control Port
//	        data = vdp.readControl();
//	        if (size == Size.WORD)
//	            return data;
//	        if (size == Size.BYTE)
//	            return data >> 8;
//	        throw new RuntimeException("Invalid long read from VDP control");
//
//	    } else if (address == 0xC00005 || address == 0xC00007) { // Control Port (low byte)
//	        data = vdp.readControl();
//	        if (size == Size.BYTE)
//	            return data & 0xFF;
//	        throw new RuntimeException("Invalid word/long read from VDP control");
//
//	    } else if (address == 0xC00008 || address == 0xC00009) { // HV Counter
//	        int v = vdp.line; // linha atual
//	        int h = new Random().nextInt(256); // stub do H-counter
//	        if (size == Size.WORD)
//	            return (v << 8) | h;
//	        return (address == 0xC00008) ? v : h;
//
//	    } else if (address >= 0xA10000 && address <= 0xA1001F) {
//	        int offset = (int)(address - 0xA10000);
//	        switch (offset) {
//	            case 0x00: return 0xFFFF; // Joypad 1
//	            case 0x02: return 0xFFFF; // Joypad 2
//	            case 0x08: return 0x00;   // I/O Control
//	            default:
//	                System.out.printf("Read I/O port: %06X%n", address);
//	                return 0xFFFF;
//	        }
//	    }
//
//	    // 4. RAM interna (0xFF0000 – 0xFFFFFF)
//	    if (address >= 0xFF0000) {
//	        int offset = (int) (address - 0xFF0000);
//	        if (size == Size.BYTE) {
//	            return ram[offset] & 0xFF;
//	        } else if (size == Size.WORD) {
//	            return ((ram[offset] & 0xFF) << 8)
//	                 |  (ram[offset + 1] & 0xFF);
//	        } else {
//	            return ((ram[offset] & 0xFF) << 24)
//	                 | ((ram[offset + 1] & 0xFF) << 16)
//	                 | ((ram[offset + 2] & 0xFF) << 8)
//	                 |  (ram[offset + 3] & 0xFF);
//	        }
//	    }
//
//	    // 5. Não mapeado
//	    System.err.printf("Read unmapped: %06X%n", address);
//	    return 0;
//	}
	
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
					data = readCartridgeByte(address);
				}

			} else if (size == Size.WORD) {
				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
					address = address - 0x200000;
					data =  sram[(int) address] << 8;
					data |= sram[(int) address + 1];
				} else {
					data = readCartridgeWord(address);
				}

			} else {
				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
					address = address - 0x200000;
					data =  sram[(int) address] << 24;
					data |= sram[(int) address + 1] << 16;
					data |= sram[(int) address + 2] << 8;
					data |= sram[(int) address + 3];

				} else {
					data = readCartridgeWord(address) << 16;
					data |= readCartridgeWord(address + 2);
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
					data = readCartridgeByte(address);
				}

			} else if (size == Size.WORD) {
				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
					address = address - 0x200000;
					data =  sram[(int) address] << 8;
					data |= sram[(int) address + 1];
				} else {
					data = readCartridgeWord(address);
				}

			} else {
				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
					address = address - 0x200000;
					data =  sram[(int) address] << 24;
					data |= sram[(int) address + 1] << 16;
					data |= sram[(int) address + 2] << 8;
					data |= sram[(int) address + 3];

				} else {
					data = readCartridgeWord(address) << 16;
					data |= readCartridgeWord(address + 2);

				}
			}
			return data;

		} else if (address >= 0xA00000 && address <= 0xA0FFFF) { // Z80 addressing space
//			return z80.readMemory((int) (address - 0xA00000));

		} else if (address == 0xA10000 || address == 0xA10001) { // Version register (read-only word-long)
			data = getRegion();
			if (size == Size.BYTE) {
				return data;
			} else {
				return data << 8 | data;
			}

		} else if (address == 0xA10002 || address == 0xA10003) { // Controller 1 data
//			return joypad.readDataRegister1();

		} else if (address == 0xA10004 || address == 0xA10005) { // Controller 2 data
//			return joypad.readDataRegister2();

		} else if (address == 0xA10006 || address == 0xA10007) { // Expansion data
//			return joypad.readDataRegister3();

		} else if (address == 0xA1000C || address == 0xA1000D) { // Expansion Port Control
			if (address == 0xA1000C) {
				return 0;
			} else if (address == 0xA1000D) {
				return 0;
			}

		} else if (address == 0xA10008 || address == 0xA10009) { // Controller 1 control
			if (size == Size.BYTE) {
//				return joypad.readControlRegister1() & 0xFF;
			} else {
//				return joypad.readControlRegister1();
			}

		} else if (address == 0xA1000A || address == 0xA1000B) { // Controller 2 control
			if (size == Size.BYTE) {
//				return joypad.readControlRegister2() & 0xFF;
			} else {
//				return joypad.readControlRegister2();
			}

		} else if (address == 0xA11100 || address == 0xA11101) { // Z80 bus request
//			return (z80.busRequested && !z80.reset) ? 0 : 1;
			return new Random().nextBoolean() ? 1 : 0;
//			return 0;	//	FIXME hacer esto bien

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
				return readRam(address);
			} else if (size == Size.WORD) {
				data = readRam(address) << 8;
				data |= readRam(address + 1);
				return data;
			} else {
				data =  readRam(address) << 24;
				data |= readRam(address + 1) << 16;
				data |= readRam(address + 2) << 8;
				data |= readRam(address + 3);
				return data;
			}

		} else {
//			System.out.println("NOT MAPPED: " + pad4(address) + " - " + pad4(cpu.PC));
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
				writeRam(addressL, data);
			} else if (size == Size.WORD) {
				writeRam(addressL, (data >> 8));
				writeRam(addressL + 1, (data & 0xFF));
			} else if (size == Size.LONG) {
				writeRam(addressL, (data >> 24) & 0xFF);
				writeRam(addressL + 1, (data >> 16) & 0xFF);
				writeRam(addressL + 2, (data >> 8) & 0xFF);
				writeRam(addressL + 3, (data & 0xFF));
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

	// =======================
	// ===== Helpers =========
	// =======================
//	private long safeReadBytes(byte[] mem, int offset, Size size, String region) {
//		int max = mem.length;
//		int bytes = size == Size.BYTE ? 1 : (size == Size.WORD ? 2 : 4);
//		if (offset < 0 || offset + bytes > max) {
//			System.err.printf("Read out of bounds (%s): offset=%06X size=%s mem.length=%d%n", region, offset, size,
//					max);
//			return 0;
//		}
//		return readBytes(mem, offset, size);
//	}

	/**
	 * Lê um valor da memória no formato big-endian (68k).
	 *
	 * @param mem    Array de bytes da memória/ROM
	 * @param offset Posição de leitura
	 * @param size   Tamanho (BYTE, WORD, LONG)
	 * @return Valor lido (0..0xFF, 0..0xFFFF ou 0..0xFFFFFFFF)
	 */
//	private long readBytes(byte[] mem, int offset, Size size) {
//		// Proteção contra overflow
//		int required = offset + size.getBytes() - 1;
//		if (required >= mem.length || offset < 0) {
//			// "Open bus" (valor indefinido, alguns emuladores usam 0xFF ou 0)
//			return 0xFF;
//		}
//
//	switch (size) {
//		case BYTE:
//			return mem[offset] & 0xFFL;
//
//		case WORD:
//			return ((mem[offset] & 0xFFL) << 8) |
//					(mem[offset + 1] & 0xFFL);
//
//		case LONG:
//			return ((mem[offset] & 0xFFL) << 24) | 
//					((mem[offset + 1] & 0xFFL) << 16) | 
//					((mem[offset + 2] & 0xFFL) << 8) |
//					 (mem[offset + 3] & 0xFFL);
//
//		default:
//			throw new IllegalArgumentException("Tamanho inválido: " + size);
//		}
//	}

//	private void safeWriteBytes(byte[] mem, int offset, long data, Size size, String region) {
//		int max = mem.length;
//		int bytes = size == Size.BYTE ? 1 : (size == Size.WORD ? 2 : 4);
//		if (offset < 0 || offset + bytes > max) {
//			System.err.printf("Write out of bounds (%s): offset=%06X size=%s mem.length=%d%n", region, offset, size,
//					max);
//			return;
//		}
//		writeBytes(mem, offset, data, size);
//	}

//	private void writeBytes(byte[] mem, int offset, long data, Size size) {
//		switch (size) {
//		case BYTE:
//			mem[offset] = (byte) (data & 0xFF);
//			break;
//		case WORD:
//			mem[offset] = (byte) ((data >> 8) & 0xFF);
//			mem[offset + 1] = (byte) (data & 0xFF);
//			break;
//		case LONG:
//			mem[offset] = (byte) ((data >> 24) & 0xFF);
//			mem[offset + 1] = (byte) ((data >> 16) & 0xFF);
//			mem[offset + 2] = (byte) ((data >> 8) & 0xFF);
//			mem[offset + 3] = (byte) (data & 0xFF);
//			break;
//		}
//	}
	
//	public int readCartridgeByte(int address) {
//		return rom[(address & (rom.length - 1))] & 0xFF;
//	}
	
//	private int readCartridgeWord(int address) {
//		int hi = rom[address & (rom.length - 1)] & 0xFF; // byte alto
//		int lo = rom[(address + 1) & (rom.length - 1)] & 0xFF; // byte baixo
//		return (hi << 8) | lo;
//	}

//	private int readWord(byte[] mem, int offset) {
//	    int hi = mem[offset] & 0xFF;
//	    int lo = mem[offset + 1] & 0xFF;
//	    return (hi << 8) | lo;
//	}
//
//	private long readLong(byte[] mem, int offset) {
//	    return ((long) readWord(mem, offset) << 16) | readWord(mem, offset + 2);
//	}
	

	/**
	 * Lê um byte da ROM do cartucho (big-endian).
	 */
	public long readCartridgeByte(long address) {
		long data = 0;
		if (address <= 0x3FFFFF && rom != null) {
			while (address >= rom.length) { // wrap-around
				address -= rom.length;
			}
			data = rom[(int) address] & 0xFF;
		}
		return data;
	}
	
	/**
	 * Lê uma palavra (16 bits) da ROM do cartucho (big-endian).
	 */
	public long readCartridgeWord(long address) {
		long data = 0;
		if (address <= 0x3FFFFF && rom != null) {
			while (address >= rom.length) { // wrap-around
				address -= rom.length;
			}
			int indexHigh = (int) address;
			int indexLow = (int) ((address + 1) % rom.length); // wrap no segundo byte

			data = (rom[indexHigh] & 0xFF) << 8;
			data |= (rom[indexLow] & 0xFF);
		}
		return data;
	}
	
	
	long readRam(long address) {
		long data = 0;

		// mascara para 24 bits
		address &= 0xFFFFFF;

		if (address >= 0xFF0000 && ram != null) {
			int offset = (int) (address - 0xFF0000);

			// proteção extra contra estourar o array
			if (offset >= 0 && offset < ram.length) {
				data = ram[offset] & 0xFF;
			}
		}
		return data;
	}
	
	void writeRam(long address, long data) {
		address &= 0xFFFFFF;

	    if (address >= 0xFF0000 && ram != null) {
	        int offset = (int) (address - 0xFF0000);

	        if (offset >= 0 && offset < ram.length) {
	            ram[offset] = (byte) (data & 0xFF);
	        } else {
	            throw new RuntimeException(
	                String.format("RAM WRITE OUT OF RANGE: %06X", address)
	            );
	        }
	    } else {
	        throw new RuntimeException(
	            String.format("RAM WRITE NOT MAPPED: %06X", address)
	        );
	    }
	}
	
	
//	long readCartridgeWord(long address) {
//		long data = 0;
//		if (address >= cartridge.length) { // wrapping ? TODO confirmar
//			address -= cartridge.length;
//		}
//		data = cartridge[(int) address] << 8;
//		data |= cartridge[(int) address + 1];
//		return data;
//	}	

	// Getter do VDP (para ligar no Canvas do EmulatorApp)
	public VDP getVDP() {
		return vdp;
	}

	// US: A0A0 rev 0 o A1A1 rev 1
	// EU: C1C1
	// JP: ????
	// US SEGA CD: 8181
	public long getRegion() {
		return 0;
	}

}
