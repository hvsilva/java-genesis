package com.emulator;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Video extends JPanel {

	private static final long serialVersionUID = 1L;

	private final int width;
	private final int height;
	private int multiplier = 2; // padrão (2x)
	private final BufferedImage frame;
	private final int[] pixels;

	public Video(int width, int height) {
		this.width = width;
		this.height = height;
		this.frame = new BufferedImage(width * multiplier, height * multiplier, BufferedImage.TYPE_INT_RGB);
		this.pixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
		setPreferredSize(new Dimension(width * multiplier, height * multiplier));
	}

	public void setMultiplier(int multiplier) {
		this.multiplier = multiplier;
	}

	public void render(int[][] screenData) {
		int w = width;
		int h = height;

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int color = screenData[x][y];
				// replica o pixel no frame escalado
				for (int dy = 0; dy < multiplier; dy++) {
					for (int dx = 0; dx < multiplier; dx++) {
						int pos = ((y * multiplier + dy) * (w * multiplier)) + (x * multiplier + dx);
						pixels[pos] = color;
					}
				}
			}
		}
		repaint(); // pede atualização visual
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(frame, 0, 0, getWidth(), getHeight(), null);
	}
}