package com.emulator;

public class VDP {
	public static final int WIDTH = 320;
    public static final int HEIGHT = 224;

    // Memória de vídeo
    public byte[] vram = new byte[0x10000];
    public short[] cram = new short[0x40]; // 9-bit colors (Mega Drive usa 0bbbgggrrr)
    public int[] registers = new int[32];

    // Framebuffer (ARGB32)
    private int[] framebuffer = new int[WIDTH * HEIGHT];

    // Controle de ciclos/linha/quadro
    private int totalCycles = 0;
    private int line = 0;

    // Flags
    private boolean vblank = false;
    private boolean hblank = false;

    /**
     * Simula execução do VDP por um certo número de ciclos.
     */
    public void run(int cycles) {
        totalCycles += cycles;

        if (totalCycles < 800) {
            hblank = false;
        } else if (totalCycles >= 800 && totalCycles <= 982) {
            hblank = true;
        } else if (totalCycles > 982) {
            // Fim da linha → renderiza scanline
            if ((registers[1] & 0x40) != 0) { // display enable bit
                if (line < HEIGHT) {
                    renderScanline(line);
                }
            }

            line++;
            totalCycles = 0;
            hblank = false;
        }

        // Fim do quadro (VBlank ON)
        if (line >= 0xE0 && !vblank) {
            vblank = true;
            // Aqui poderíamos sinalizar IRQ de VBlank para CPU
        }

        // Reinicia quadro
        if (line > 0xFF) {
            line = 0;
            vblank = false;
        }
    }

    /**
     * Renderiza uma linha da tela para o framebuffer.
     */
    private void renderScanline(int y) {
        for (int x = 0; x < WIDTH; x++) {
            int colorIndex = getPixelColor(x, y);
            framebuffer[y * WIDTH + x] = decodeColorARGB(colorIndex);
        }
    }

    /**
     * Decodifica índice de cor de VRAM.
     * Aqui é bem simplificado, sem planos/tiles/sprites reais.
     */
    private int getPixelColor(int x, int y) {
        int addr = (y * WIDTH + x) % vram.length;
        return vram[addr] & 0x3F; // 6 bits = índice de cor (simples)
    }

    /**
     * Traduz índice da CRAM para cor ARGB.
     */
    private int decodeColorARGB(int colorIndex) {
        int value = cram[colorIndex % cram.length] & 0xFFFF;

        int r = (value & 0x000E) >> 1;  // 3 bits
        int g = (value & 0x00E0) >> 5;  // 3 bits
        int b = (value & 0x0E00) >> 9;  // 3 bits

        r = (r * 255) / 7;
        g = (g * 255) / 7;
        b = (b * 255) / 7;

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Retorna framebuffer atual (ARGB).
     */
    public int[] getFrameBuffer() {
        return framebuffer;
    }

    public boolean isVBlank() {
        return vblank;
    }

    public boolean isHBlank() {
        return hblank;
    }
}
