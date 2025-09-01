package emulator02;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import java.util.concurrent.atomic.AtomicBoolean;

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
		this.vdp = new VDP();
	}

	public void start() {
		running.set(true);
		emuThread = new Thread(() -> {
			while (running.get()) {
				cpu.step(); // executa instrução
				vdp.step(); // atualiza framebuffer
				try {
					Thread.sleep(16);
				} catch (InterruptedException ignored) {
				}
			}
		}, "EmuThread");
		emuThread.setDaemon(true);
		emuThread.start();
	}

	public void stop() {
		running.set(false);
	}

	public void drawFrame() {
		PixelWriter pw = gc.getPixelWriter();
		int[] fb = vdp.getFrameBuffer();
		for (int y = 0; y < 224; y++) {
			for (int x = 0; x < 320; x++) {
				int rgb = fb[y * 320 + x];
				int r = (rgb >> 16) & 0xFF;
				int g = (rgb >> 8) & 0xFF;
				int b = rgb & 0xFF;
				pw.setColor(x, y, Color.rgb(r, g, b));
			}
		}
	}
}
