package br.com.emulator;

//Vídeo Display Processor
public class VDP {

	private int width = 320;
	private int height = 224;
	private int[] frameBuffer;
	private Memory memory;

	public VDP(Memory memory) {
		this.memory = memory;
		this.frameBuffer = new int[width * height];
		// Fill with a test pattern initially
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				frameBuffer[y * width + x] = ((x ^ y) & 0xFF) << 16; // red pattern
			}
		}
	}

	public void step() {
		// For demo: animate a simple pattern using memory or counters
		// In a real VDP we'd render tiles/sprites based on VRAM
		for (int i = 0; i < frameBuffer.length; i++) {
			// simple moving pattern
			frameBuffer[i] = ((i + (int) (System.currentTimeMillis() / 50)) & 0xFF) << 8; // green shift
		}
	}

	public int[] getFrameBuffer() {
		return frameBuffer;
	}

}
