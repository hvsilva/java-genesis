package com.emulator;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Video extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private BufferedImage frame;

    public Video(int width, int height) {
        this.frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        setPreferredSize(new Dimension(width * 2, height * 2)); // escala 2x
    }

    public void draw(int[] framebuffer) {
        // copia pixels para BufferedImage
        frame.setRGB(0, 0, frame.getWidth(), frame.getHeight(), framebuffer, 0, frame.getWidth());
        // repaint precisa rodar no EDT
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(frame, 0, 0, getWidth(), getHeight(), null);
    }
}