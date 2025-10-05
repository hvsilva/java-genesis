package vdp;

import java.util.Arrays;

public class VDPStudy {

	private final int WIDTH = 320;
	private final int HEIGHT = 224;

	private final int[][] framebuffer = new int[WIDTH][HEIGHT];
	private final int[][] planeA = new int[WIDTH][HEIGHT];
	private final int[][] planeB = new int[WIDTH][HEIGHT];
	private final int[][] window = new int[WIDTH][HEIGHT];
	private final int[][] sprites = new int[WIDTH][HEIGHT];

	private final Video video;

	// posição e direção do sprite
	private int spriteX = 150;
	private int spriteY = 140;
	private int dir = 1; // 1 = direita, -1 = esquerda

	private int spriteSize = 16;
	private int spriteDirection = 2; // velocidade


	public VDPStudy(Video video) {
		this.video = video;
	}

	/** Renderiza todos os planos e compõe o frame final */
	public void renderFrame() {
		renderBack();
		renderPlaneB();
		renderPlaneA();
		renderWindow();
		renderSprites();
		composeFrame();

		int[] pixels = video.getPixels();

		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				pixels[y * WIDTH + x] = framebuffer[x][y];
			}
		}

		video.repaint();

	}

	// =======================================================================
	// Camadas
	// =======================================================================
	private void renderBack() {
		for (int y = 0; y < HEIGHT; y++) {
			Arrays.fill(framebuffer[y], 0x000040); // azul escuro
		}
	}

	private void renderPlaneB() {
		for (int y = 0; y < HEIGHT; y++) {
			int color;
			if (y < 80) {
				color = 0x87CEEB; // céu
			} else if (y < 160) {
				color = 0x228B22; // floresta
			} else {
				color = 0x8B4513; // chão
			}
			for (int x = 0; x < WIDTH; x++) {
				planeB[x][y] = color;
			}
		}
	}

	private void renderPlaneA() {
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				planeA[x][y] = 0;
			}
		}

		for (int i = 50; i < WIDTH; i += 80) {
			for (int y = 120; y < 160; y++) {
				for (int x = i; x < i + 8; x++) {
					planeA[x][y] = 0x8B4513; // tronco
				}
			}
			for (int y = 100; y < 120; y++) {
				for (int x = i - 8; x < i + 16; x++) {
					planeA[x][y] = 0x006400; // copa
				}
			}
		}
	}

	private void renderWindow() {
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < WIDTH; x++) {
				window[x][y] = 0x202020; // barra HUD
			}
		}
	}

	// Sprite (quadrado vermelho que se move na tela)
	private void renderSprites() {

	    int color = 0xFF0000; // vermelho puro (personagem)

	    // Desenha o sprite: um quadrado 16x16 na posição spriteX/spriteY
	    for (int y = 0; y < spriteSize; y++) {
	        for (int x = 0; x < spriteSize; x++) {
	            int px = spriteX + x;
	            int py = spriteY + y;

	            // Evita desenhar fora da tela (limites 320x224)
	            if (px >= 0 && px < 320 && py >= 0 && py < 224) {
	                framebuffer[px][py] = color;
	            }
	        }
	    }

	    // --- Animação: movimento horizontal simples ---

	    // move o sprite para direita ou esquerda conforme a direção atual
	    spriteX += spriteDirection;

	    // se bater na borda esquerda (0) ou direita (320 - tamanho), inverte o sentido
	    if (spriteX <= 0 || spriteX + spriteSize >= 320) {
	        spriteDirection *= -1;
	    }
	}

	private void composeFrame() {
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				int color = planeB[x][y];
				if (planeA[x][y] != 0)
					color = planeA[x][y];
				if (window[x][y] != 0)
					color = window[x][y];
				if (sprites[x][y] != 0)
					color = sprites[x][y];
				framebuffer[x][y] = color;
			}
		}
	}

	// =======================================================================
	// Movimento simples do sprite
	// =======================================================================
	private void updateSprite() {
		spriteX += dir * 2;

		if (spriteX > WIDTH - 20 || spriteX < 0) {
			dir *= -1; // inverte direção ao bater na borda
		}
	}
}