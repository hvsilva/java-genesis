package com.emulator;

public class Sprite {
    int x, y;
    int tileIndex;
    int width, height;
    int palette;
    boolean hFlip, vFlip;
    int priority;
    int tile;

    Sprite(int x, int y, int tileIndex, int width, int height,
           int palette, boolean hFlip, boolean vFlip, int priority) {
        this.x = x;
        this.y = y;
        this.tileIndex = tileIndex;
        this.width = width;
        this.height = height;
        this.palette = palette;
        this.hFlip = hFlip;
        this.vFlip = vFlip;
        this.priority = priority;
    }

    /**
     * Retorna a cor (ARGB) de um pixel dentro do sprite.
     */
    public int getPixel(int x, int y, int[] vram, int[] cram) {
        // Cada sprite é formado por tiles de 8x8 pixels
        int tileRow = y / 8;
        int tileCol = x / 8;

        // Índice do tile dentro do sprite
        int tileIndex = tile + (tileRow * width) + tileCol;

        // Offset dentro do tile
        int tileLine = (y % 8) * 4;   // 8 pixels -> 4 bytes
        int pixelX   = x % 8;

        // Endereço do dado na VRAM
        int tileAddress = (tileIndex * 32) + tileLine + (pixelX / 2);

        // Lê byte da VRAM (está guardado em int[], mas só os 8 bits importam)
        int tileData = vram[tileAddress] & 0xFF;

        // Extrai a cor (4 bits por pixel)
        int colorIndex;
        if ((pixelX & 1) == 0) {
            colorIndex = (tileData >> 4) & 0xF;
        } else {
            colorIndex = tileData & 0xF;
        }

        // Cor 0 é transparente
        if (colorIndex == 0) return -1;

        // Decodifica usando CRAM (paleta)
        return cram[(palette * 16) + colorIndex];
    }
}
