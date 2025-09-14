package com.emulator;

import java.util.Random;

public class Memory {

	private final byte[] rom;
	private final byte[] ram; // Work RAM
	private final byte[] sram; // Save RAM (SRAM)
	private final VDP vdp;

	boolean writeSram;

	int[] cartridge;

	public Memory(byte[] rom) {
		this.rom = rom;
		this.ram = new byte[64 * 1024];  // 64KB RAM
		this.sram = new byte[32 * 1024]; // 32KB SRAM (padrão comum)
		this.vdp = new VDP();            // placeholder do vídeo
	}

	// Construtor que aceita Cartridge
//	public Memory(Cartridge cart) {
//		this(cart.getROMData());
//	}

	// =======================
	// ======= READ ==========
	// =======================
	public long read(long address, Size size) {

		System.out.println("[CALL READ]: " + Long.toHexString(address) + " - " + size );

		address &= 0xFF_FFFF; // 24-bit mask
		long data = 0;

		// ======================
	    // 1. Cartridge ROM (0x000000 – 0x3FFFFF)
	    // ======================
	    if (address <= 0x3FFFFF) {
	        // Usando nova função com wrapping
	        data = RomReader.safeReadBytes(rom, address, size);
	        return data;
	    }

		// ======================
		// 2. SRAM (0x200000 – 0x20FFFF)
		// ======================
		if (address >= 0x200000 && address <= 0x20FFFF) {
			int offset = (int) (address - 0x200000);
			return safeReadBytes(sram, offset, size, "SRAM");
		}

		// -----------------------------
		// VDP Ports
		// -----------------------------
		if (address == 0xC00000 || address == 0xC00002) { // Data Port (high/word/long)
			if (size == Size.BYTE)
				return (vdp.readDataPort(size) >> 8);
			if (size == Size.WORD)
				return vdp.readDataPort(size);
			return (vdp.readDataPort(Size.WORD) << 16) | vdp.readDataPort(Size.WORD);

		} else if (address == 0xC00001 || address == 0xC00003) { // Data Port (low byte)
			return vdp.readDataPort(size) & 0xFF;

		} else if (address == 0xC00004 || address == 0xC00006) { // Control Port
			data = vdp.readControl();
			if (size == Size.WORD)
				return data;
			if (size == Size.BYTE)
				return data >> 8;
			throw new RuntimeException("Invalid long read from VDP control");

		} else if (address == 0xC00005 || address == 0xC00007) { // Control Port (low byte)
			data = vdp.readControl();
			if (size == Size.BYTE)
				return data & 0xFF;
			throw new RuntimeException("Invalid word/long read from VDP control");

		} else if (address == 0xC00008 || address == 0xC00009) { // HV Counter
			int v = vdp.line; // linha atual
			int h = new Random().nextInt(256); // stub do H-counter
			if (size == Size.WORD)
				return (v << 8) | h;
			return (address == 0xC00008) ? v : h;
			
		} else if (address >= 0xA10000 && address <= 0xA1001F) {		  
			
			int offset = (int)(address - 0xA10000);

		    switch (offset) {
		        case 0x00: // Joypad 1
		            return 0xFFFF; // nenhum botão pressionado (padrão alto)
		        case 0x02: // Joypad 2
		            return 0xFFFF;
		        case 0x08: // I/O Control
		            return 0x00; // ou 0xFF dependendo do que você quer simular
		        default:
		            System.out.printf("Read I/O port: %06X%n", address);
		            return 0xFFFF;
		    }
		}    

		// ======================
		// 4. Work RAM (0xFF0000 – 0xFFFFFF)
		// ======================
		if (address >= 0xFF0000) {
			int offset = (int) (address - 0xFF0000);
			return safeReadBytes(ram, offset, size, "RAM");
		}

		// ======================
		// Default: not mapped
		// ======================
		System.err.printf("Read unmapped: %06X%n", address);
		return 0xFFFFFFFFL;
	}

	// =======================
	// ======= WRITE =========
	// =======================
	public long write(long address, long data, Size size) {
	
		System.out.println("[CALL WRITE]: " + Long.toHexString(address) + " - " + size + " - " + Long.toHexString(data));

		long addressL = (address & 0xFFFFFF); // 24-bit mask (68k bus)

		// Ajusta valor conforme tamanho
		if (size == Size.BYTE) {
			data &= 0xFF;
		} else if (size == Size.WORD) {
			data &= 0xFFFF;
		} else {
			data &= 0xFFFFFFFFL;
		}

		// ----------------------------------------------------------------
		// Cartridge ROM / SRAM
		// ----------------------------------------------------------------
		if (addressL <= 0x3FFFFF) {
			if (addressL >= 0x200000 && addressL <= 0x20FFFF && writeSram) {
				int offset = (int) (addressL - 0x200000);

				if (size == Size.BYTE) {
					sram[offset] = (byte) data;
				} else if (size == Size.WORD) {
					sram[offset] = (byte) ((data >> 8) & 0xFF);
					sram[offset + 1] = (byte) (data & 0xFF);
				} else {
					sram[offset] = (byte) ((data >> 24) & 0xFF);
					sram[offset + 1] = (byte) ((data >> 16) & 0xFF);
					sram[offset + 2] = (byte) ((data >> 8) & 0xFF);
					sram[offset + 3] = (byte) (data & 0xFF);
				}
			} else {
				System.out.println("write cart rom ram ? " + Integer.toHexString((int) addressL));
			}
		}

		// ----------------------------------------------------------------
		// Z80 addressing space
		// ----------------------------------------------------------------
		else if (addressL >= 0xA00000 && addressL <= 0xA0FFFF) {
			int z80addr = (int) (addressL - 0xA00000);
			if (size == Size.BYTE) {
//                z80.writeByte(z80addr, data);
			} else if (size == Size.WORD) {
//                z80.writeWord(z80addr, data);
			} else {
//                z80.writeWord(z80addr, (data >> 16) & 0xFFFF);
//                z80.writeWord(z80addr + 2, data & 0xFFFF);
			}
		}

		// ----------------------------------------------------------------
		// Joypad / I/O Ports
		// ----------------------------------------------------------------
		else if (addressL >= 0xA10000 && addressL <= 0xA1001F) {
			data = getRegion();
			if (size == Size.BYTE) {
				return data;
			} else {
				return data << 8 | data;
			}
		}

		// ----------------------------------------------------------------
		// Z80 Bus Request / Reset
		// ----------------------------------------------------------------
		else if (addressL == 0xA11100 || addressL == 0xA11101) {
			if (data == 0x0100 || data == 0x01) {
//                z80.requestBus();
//                emu.runZ80 = false;
			} else if (data == 0x0000) {
//                z80.unrequestBus();
//                if (!z80.reset) emu.runZ80 = true;
			}
		} else if (addressL == 0xA11200 || addressL == 0xA11201) {
//            if (data == 0x0000) {
//                z80.reset();
//                z80.initialize();
//                emu.runZ80 = false;
//            } else if (data == 0x0100 || data == 0x01) {
//                z80.disableReset();
//                if (!z80.busRequested) emu.runZ80 = true;
//            }
		}

		// ----------------------------------------------------------------
		// SRAM Enable Register
		// ----------------------------------------------------------------
		else if (addressL == 0xA130F1) {
			writeSram = (data != 0);

			// ----------------------------------------------------------------
			// VDP Data / Control Ports
			// ----------------------------------------------------------------
		} else if (addressL >= 0xC00000 && addressL <= 0xC00003) {
			vdp.writeDataPort((int) data, size);
		} else if (addressL >= 0xC00004 && addressL <= 0xC00007) {
			if (size == Size.BYTE) {
				throw new RuntimeException("VDP control write with BYTE not allowed");
			} else if (size == Size.WORD) {
				vdp.writeControlPort(data);
			} else {
				vdp.writeControlPort((data >> 16) & 0xFFFF);
				vdp.writeControlPort(data & 0xFFFF);
			}

			// ----------------------------------------------------------------
			// Work RAM (0xFF0000 - 0xFFFFFF)
			// ----------------------------------------------------------------
		} else if (addressL >= 0xFF0000) {
			int offset = (int) (addressL - 0xFF0000);
			safeWriteBytes(ram, offset, data, size, "RAM");
		}

		// ----------------------------------------------------------------
		// Default: não mapeado
		// ----------------------------------------------------------------
		else {
			System.err.printf("Write unmapped: %06X (data=%X)%n", addressL, data);
		}

		return 0;
	}

	// =======================
	// ===== Helpers =========
	// =======================
	private long safeReadBytes(byte[] mem, int offset, Size size, String region) {
		int max = mem.length;
		int bytes = size == Size.BYTE ? 1 : (size == Size.WORD ? 2 : 4);
		if (offset < 0 || offset + bytes > max) {
			System.err.printf("Read out of bounds (%s): offset=%06X size=%s mem.length=%d%n", region, offset, size,
					max);
			return 0;
		}
		return readBytes(mem, offset, size);
	}
	
	/**
	 * Lê um valor da memória no formato big-endian (68k).
	 *
	 * @param mem    Array de bytes da memória/ROM
	 * @param offset Posição de leitura
	 * @param size   Tamanho (BYTE, WORD, LONG)
	 * @return Valor lido (0..0xFF, 0..0xFFFF ou 0..0xFFFFFFFF)
	 */
	private long readBytes(byte[] mem, int offset, Size size) {
	    // Proteção contra overflow
	    int required = offset + size.getBytes() - 1;
	    if (required >= mem.length || offset < 0) {
	        // "Open bus" (valor indefinido, alguns emuladores usam 0xFF ou 0)
	        return 0xFF;
	    }

	    switch (size) {
	        case BYTE:
	            return mem[offset] & 0xFFL;

	        case WORD:
	            return ((mem[offset] & 0xFFL) << 8)
	                 | (mem[offset + 1] & 0xFFL);

	        case LONG:
	            return ((mem[offset]     & 0xFFL) << 24)
	                 | ((mem[offset + 1] & 0xFFL) << 16)
	                 | ((mem[offset + 2] & 0xFFL) << 8)
	                 | (mem[offset + 3] & 0xFFL);

	        default:
	            throw new IllegalArgumentException("Tamanho inválido: " + size);
	    }
	}

	private void safeWriteBytes(byte[] mem, int offset, long data, Size size, String region) {
		int max = mem.length;
		int bytes = size == Size.BYTE ? 1 : (size == Size.WORD ? 2 : 4);
		if (offset < 0 || offset + bytes > max) {
			System.err.printf("Write out of bounds (%s): offset=%06X size=%s mem.length=%d%n", region, offset, size,
					max);
			return;
		}
		writeBytes(mem, offset, data, size);
	}

	private void writeBytes(byte[] mem, int offset, long data, Size size) {
		switch (size) {
		case BYTE:
			mem[offset] = (byte) (data & 0xFF);
			break;
		case WORD:
			mem[offset] = (byte) ((data >> 8) & 0xFF);
			mem[offset + 1] = (byte) (data & 0xFF);
			break;
		case LONG:
			mem[offset] = (byte) ((data >> 24) & 0xFF);
			mem[offset + 1] = (byte) ((data >> 16) & 0xFF);
			mem[offset + 2] = (byte) ((data >> 8) & 0xFF);
			mem[offset + 3] = (byte) (data & 0xFF);
			break;
		}
	}

	long readCartridgeWord(long address) {
		long data = 0;
//		if (address <= 0x3FFFFF) {
		if (address >= cartridge.length) { // wrapping ? TODO confirmar
			address -= cartridge.length;
		}
		data = cartridge[(int) address] << 8;
		data |= cartridge[(int) address + 1];
//		}
		return data;
	}

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
