package com.emulator;

public class Memory {

    private final byte[] rom;
    private final byte[] ram;   // Work RAM
    private final byte[] sram;  // Save RAM (SRAM)
    private final VDP vdp;

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
    public int read(long address, Size size) {
        address &= 0xFFFFFF; // barramento 24 bits
        int data = 0;

        // ROM 0x000000 – 0x3FFFFF
        if (address < rom.length) {
            data = safeReadBytes(rom, (int) address, size, "ROM");
        }
        // SRAM 0x200000 – 0x20FFFF
        else if (address >= 0x200000 && address < 0x200000 + sram.length) {
            int offset = (int) (address - 0x200000);
            data = safeReadBytes(sram, offset, size, "SRAM");
        }
        // VDP 0xC00000 – 0xC0001F
        else if (address >= 0xC00000 && address <= 0xC0001F) {
            data = vdp.read(address, size);
        }
        // RAM 0xFF0000 – 0xFFFFFF
        else if (address >= 0xFF0000) {
            int offset = (int) (address - 0xFF0000);
            data = safeReadBytes(ram, offset, size, "RAM");
        } else {
            System.err.printf("Read unmapped: %06X%n", address);
        }

        return data;
    }

    // =======================
    // ======= WRITE =========
    // =======================
    public void write(long address, long data, Size size) {
        address &= 0xFFFFFF;

        // SRAM
        if (address >= 0x200000 && address < 0x200000 + sram.length) {
            int offset = (int) (address - 0x200000);
            safeWriteBytes(sram, offset, data, size, "SRAM");
        }
        // VDP
        else if (address >= 0xC00000 && address <= 0xC0001F) {
            vdp.write(address, data, size);
        }
        // RAM
        else if (address >= 0xFF0000) {
            int offset = (int) (address - 0xFF0000);
            safeWriteBytes(ram, offset, data, size, "RAM");
        }
        // ROM (read-only)
        else if (address < rom.length) {
            System.err.printf("Write to ROM ignored: %06X%n", address);
        } else {
            System.err.printf("Write unmapped: %06X%n", address);
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

    // Getter do VDP (para ligar no Canvas do EmulatorApp)
    public VDP getVDP() {
        return vdp;
    }

}
