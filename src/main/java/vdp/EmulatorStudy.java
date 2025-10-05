package vdp;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class EmulatorStudy extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private final Video video;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread emuThread;

    private int[][] framebuffer = new int[320][224];
    private int[][] planeA = new int[320][224];
    private int[][] planeB = new int[320][224];
    private int[][] window = new int[320][224];
    private int[][] sprites = new int[320][224];
    

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
        if (running.get()) return;
        running.set(true);

        emuThread = new Thread(this::loop, "EmuThread");
        emuThread.start();
    }

    private void stop() {
        running.set(false);
        if (emuThread != null) emuThread.interrupt();
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
        renderBack();    //pinta o fundo
        renderPlaneB();  //desenha o plano de fundo B
        renderPlaneA();  //desenha o plano de fundo A
        renderWindow();  //desenha a janela fixa
        renderSprites(); //desenha personagens
        composeFrame();  //junta tudo no framebuffer.

        int[] pixels = video.getPixels();
        int width = 320, height = 224;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = framebuffer[x][y];
            }
        }

        video.repaint();
    }

    // === Camadas ===
    private void renderBack() {
        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 320; x++) {
                framebuffer[x][y] = 0x000000; // preto
            }
        }
    }

    //desenha o plano de fundo B (ex.: montanhas).
    private void renderPlaneB() {
        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 320; x++) {
                planeB[x][y] = (x / 32 + y / 32) % 2 == 0 ? 0x004488 : 0x008844;
            }
        }
    }

    //desenha o plano de fundo A (ex.: chão, cenários na frente).
    private void renderPlaneA() {
        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 320; x++) {
                planeA[x][y] = (x / 16 + y / 16) % 2 == 0 ? 0xFFFFFF : 0xAAAAAA;
            }
        }
    }

    //desenha a janela fixa (ex.: placar ou HUD).
    private void renderWindow() {
        for (int y = 50; y < 100; y++) {
            for (int x = 100; x < 220; x++) {
                window[x][y] = 0xFF0000; // vermelho
            }
        }
    }

    //desenha personagens (Sonic, inimigos, itens).
    private void renderSprites() {
        for (int y = 120; y < 160; y++) {
            for (int x = 140; x < 180; x++) {
                sprites[x][y] = 0x0000FF; // azul
            }
        }
    }

    //junta tudo no framebuffer.
    private void composeFrame() {
        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 320; x++) {
                int color = framebuffer[x][y]; // fundo

                if (planeB[x][y] != 0)  color = planeB[x][y];
                if (planeA[x][y] != 0)  color = planeA[x][y];
                if (window[x][y] != 0)  color = window[x][y];
                if (sprites[x][y] != 0) color = sprites[x][y];

                framebuffer[x][y] = color;
            }
        }
    }
}
