package vdp;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Video extends JPanel {
	
	private final BufferedImage frame;
	private final int[] pixels;

	public Video(int width, int height) {
		this.frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		this.pixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
		setPreferredSize(new Dimension(width, height));
	}

	public int[] getPixels() {
		return pixels;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(frame, 0, 0, getWidth(), getHeight(), null);
	}
}
