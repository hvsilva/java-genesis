package vdp;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class EmulatorStudy extends JFrame {

	private static final long serialVersionUID = 1L;

	private final Video video;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private Thread emuThread;

	  // Tela (320x224)
    private static final int WIDTH = 320;
    private static final int HEIGHT = 224;

    private final int[][] framebuffer = new int[WIDTH][HEIGHT];

    // Camadas (planos)
    private final int[][] planeA = new int[WIDTH][HEIGHT];
    private final int[][] planeB = new int[WIDTH][HEIGHT];
    private final int[][] window = new int[WIDTH][HEIGHT];

    // Sprite (personagem simples)
    private int spriteX = 0;
    private int spriteY = 160;
    private int spriteSize = 16;
    private int spriteDirection = 2; // velocidade horizontal

	public static void main(String[] args) {
		SwingUtilities.invokeLater(EmulatorStudy::new);
	}

	public EmulatorStudy() {
		super("Emulator Study - Planes Demo");
		video = new Video(320, 224);

		JButton startBtn = new JButton("Start");
		JButton stopBtn = new JButton("Stop");

		startBtn.addActionListener(e -> start());
		stopBtn.addActionListener(e -> stop());

		JPanel top = new JPanel();
		top.add(startBtn);
		top.add(stopBtn);

		setLayout(new java.awt.BorderLayout());
		add(top, java.awt.BorderLayout.NORTH);
		add(video, java.awt.BorderLayout.CENTER);

		pack();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void start() {
		if (running.get())
			return;
		running.set(true);

		emuThread = new Thread(this::loop, "EmuThread");
		emuThread.start();
	}

	private void stop() {
		running.set(false);
		if (emuThread != null)
			emuThread.interrupt();
	}

	private void loop() {
		while (running.get()) {
			renderFrame();
			try {
				Thread.sleep(16); // ~60 FPS
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public void renderFrame() {
		renderBack();     // pinta o fundo
		renderPlaneB();   // representa o fundo (background): céu, floresta e chão — como se fosse o cenário fixo do jogo.
		renderPlaneA();   // (se desejar) pode desenhar detalhes adicionais por cima, como nuvens ou árvores.
		//renderWindow(); // overlay fixo (HUD, por ex.)
		renderSprites();  // desenha personagens e objetos móveis por cima de tudo.
		composeFrame();   // junta as camadas no framebuffer, e o video.repaint() mostra o resultado na tela.

		  // Copia framebuffer → pixels do vídeo
        int[] pixels = video.getPixels();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                pixels[y * WIDTH + x] = framebuffer[x][y];
            }
        }


		video.repaint();
	}

	  // --- Fundo (limpa tudo) ---
    private void renderBack() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                framebuffer[x][y] = 0x000000; // preto
            }
        }
    }

    // --- Plano de fundo B (céu, vegetação, chão) ---
    private void renderPlaneB() {
        for (int y = 0; y < HEIGHT; y++) {
            int color;
            if (y < 80) {
                color = 0x87CEEB; // azul claro - céu
            } else if (y < 160) {
                color = 0x228B22; // verde floresta - vegetação
            } else {
                color = 0x8B4513; // marrom - chão
            }
            for (int x = 0; x < WIDTH; x++) {
                planeB[x][y] = color;
            }
        }
    }

    // --- Plano A (exemplo: transparência leve) ---
    private void renderPlaneA() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                planeA[x][y] = (x % 32 == 0 || y % 32 == 0) ? 0x444444 : 0x000000;
            }
        }
    }

    // --- Janela (fixa no canto superior esquerdo) ---
    private void renderWindow() {
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 120; x++) {
                window[x][y] = 0x222222; // cinza escuro
            }
        }
    }

	// desenha personagens (Sonic, inimigos, itens).
	private void renderSprites() {
		
		   int color = 0xFF0000; // vermelho

	        for (int y = 0; y < spriteSize; y++) {
	            for (int x = 0; x < spriteSize; x++) {
	                int px = spriteX + x;
	                int py = spriteY + y;
	                if (px >= 0 && px < WIDTH && py >= 0 && py < HEIGHT) {
	                    framebuffer[px][py] = color;
	                }
	            }
	        }

	        // animação horizontal
	        spriteX += spriteDirection;
	        if (spriteX <= 0 || spriteX + spriteSize >= WIDTH) {
	            spriteDirection *= -1;
	        }
	}

	 // --- Composição final (ordem dos planos) ---
    private void composeFrame() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int color = planeB[x][y]; // plano B base
                if (planeA[x][y] != 0x000000) color = planeA[x][y];
                if (window[x][y] != 0x000000) color = window[x][y];
                framebuffer[x][y] = color;
            }
        }
    }
}
