package com.emulator;

public class Memory {

    private final byte[] rom;
    private final byte[] ram;   // Work RAM
    private final byte[] sram;  // Save RAM (SRAM)
    private final VDP vdp;
    
    boolean writeSram;
    
    int[] cartridge;

    public Memory(byte[] rom) {
        this.rom  = rom;
        this.ram  = new byte[64 * 1024]; // 64KB RAM
        this.sram = new byte[32 * 1024]; // 32KB SRAM (padrão comum)
        this.vdp  = new VDP(); // placeholder do vídeo
    }
    
    // Construtor que aceita Cartridge
    public Memory(Cartridge cart) {
        this(cart.getROMData());
    }

    // =======================
    // ======= READ ==========
    // =======================
    public long  read(long address, Size size) {
    	address = address & 0xFF_FFFF; // o mapa de memória 
    	long data;

        // 1. Cartridge ROM
        if (address < rom.length) {
            return safeReadBytes(rom, (int) address, size, "ROM");
        }

        // 2. SRAM
        else if (address >= 0x200000 && address < 0x200000 + sram.length) {
            int offset = (int) (address - 0x200000);
            return safeReadBytes(sram, offset, size, "SRAM");
        }

        // 3. VDP
        else if (address == 0xC00000 || address == 0xC00002) { // VDP Data
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
		} 

        // 4. RAM
        else if (address >= 0xFF0000) {
            int offset = (int) (address - 0xFF0000);
            return safeReadBytes(ram, offset, size, "RAM");
        }

        // 5. Default
        else {
            System.err.printf("Read unmapped: %06X%n", address);
            return 0xFFFFFFFFL; // valor "aberto" (sem bus)
        }
        
    	return 0;
    }

    // =======================
    // ======= WRITE =========
    // =======================
    public void write(long address, long data, Size size) {
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
//            joypad.write(addr, data, size);
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
    }

    // =======================
    // ===== Helpers =========
    // =======================
    private int safeReadBytes(byte[] mem, int offset, Size size, String region) {
        int max = mem.length;
        int bytes = size == Size.BYTE ? 1 : (size == Size.WORD ? 2 : 4);
        if (offset < 0 || offset + bytes > max) {
            System.err.printf("Read out of bounds (%s): offset=%06X size=%s mem.length=%d%n", region, offset, size, max);
            return 0;
        }
        return readBytes(mem, offset, size);
    }

    private void safeWriteBytes(byte[] mem, int offset,  long data, Size size, String region) {
        int max = mem.length;
        int bytes = size == Size.BYTE ? 1 : (size == Size.WORD ? 2 : 4);
        if (offset < 0 || offset + bytes > max) {
            System.err.printf("Write out of bounds (%s): offset=%06X size=%s mem.length=%d%n", region, offset, size, max);
            return;
        }
        writeBytes(mem, offset, data, size);
    }

    private int readBytes(byte[] mem, int offset, Size size) {
        switch (size) {
            case BYTE:
                return mem[offset] & 0xFF;
            case WORD:
                return ((mem[offset] & 0xFF) << 8) |
                       (mem[offset + 1] & 0xFF);
            case LONG:
                return ((mem[offset] & 0xFF) << 24) |
                       ((mem[offset + 1] & 0xFF) << 16) |
                       ((mem[offset + 2] & 0xFF) << 8) |
                       (mem[offset + 3] & 0xFF);
            default:
                return 0;
        }
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
			if (address >= cartridge.length) {	//	wrapping ? TODO confirmar
				address -= cartridge.length;
			}
			data  = cartridge[(int) address] << 8;
			data |= cartridge[(int) address + 1];
//		}
		return data;
	}

    // Getter do VDP (para ligar no Canvas do EmulatorApp)
    public VDP getVDP() {
        return vdp;
    }

}
