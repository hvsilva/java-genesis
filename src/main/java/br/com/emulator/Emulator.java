package br.com.emulator;

import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.util.concurrent.atomic.AtomicBoolean;

//Loop principal
public class Emulator {

	private CPU68000 cpu;
	private Memory memory;
	private VDP vdp;
	private AtomicBoolean running = new AtomicBoolean(false);
	private Thread emuThread;
	private GraphicsContext gc;

	public Emulator(Memory memory, GraphicsContext gc) {
		this.memory = memory;
		this.gc = gc;
		this.cpu = new CPU68000(memory);
		this.vdp = new VDP(memory);
	}
	
	
	public void start() {
		running.set(true);
		emuThread = new Thread(() -> {
			// naive loop: step CPU and update VDP
			while (running.get()) {
				try {
					// run several CPU cycles per frame for demo
					for (int i = 0; i < 1000; i++) {
						cpu.step();
					}

					vdp.step();

					// sleep a bit to avoid pegging the CPU in this demo
					Thread.sleep(16);
				} catch (Exception e) {
					e.printStackTrace();
					running.set(false);
				}
			}
		}, "EmuThread");
		emuThread.setDaemon(true);
		emuThread.start();
	}
	
	
	public void stop() {
		running.set(false);
		try {
			if (emuThread != null)
				emuThread.join(1000);
		} catch (InterruptedException ignored) {
		}
	}
	
	// Called from JavaFX thread to draw the latest VDP framebuffer (if any)
	public void drawFrame() {
		final int width = 320;
		final int height = 224;
		PixelWriter pw = gc.getPixelWriter();
		int[] fb = vdp.getFrameBuffer();

		// simple scaling to canvas size
		double scaleX = gc.getCanvas().getWidth() / width;
		double scaleY = gc.getCanvas().getHeight() / height;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb = fb[y * width + x];
				int r = (rgb >> 16) & 0xFF;
				int g = (rgb >> 8) & 0xFF;
				int b = rgb & 0xFF;
				pw.setColor(x, y, Color.rgb(r, g, b));
			}
		}

		gc.save();
		gc.scale(scaleX, scaleY);
		gc.drawImage(gc.getCanvas().snapshot(null, null), 0, 0);
		gc.restore();
	}

}
