package com.emulator;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.JPanel;

public class Video extends JPanel {
	
	private static final long serialVersionUID = 1L;

    private final int width;
    private final int height;

    private int multiplier = 2;

    private BufferedImage frame;   // sempre em 1x (ex: 320x256)
    private int[] pixels;          // pixels do frame

    public Video(int width, int height) {
        this.width = width;
        this.height = height;

        createFrameBuffer(); // cria frame 1x

        setMultiplier(2);    // define tamanho visual do painel
        setDoubleBuffered(true);
    }

    private void createFrameBuffer() {
        this.frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.pixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
    }

    public void setMultiplier(int multiplier) {
        if (multiplier < 1) multiplier = 1;
        this.multiplier = multiplier;

        setPreferredSize(new Dimension(width * multiplier, height * multiplier));
        revalidate();
        repaint();
    }


    public void render(int[][] screenData) {
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                pixels[row + x] = screenData[x][y];  
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Desenha escalado para o tamanho do painel
        g.drawImage(frame, 0, 0, width * multiplier, height * multiplier, null);
    }
}