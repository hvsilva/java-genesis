package emulator02;

public class VDP {

	private int[] framebuffer = new int[320 * 224];

	public void step(int[] vram) {
		for (int i = 0; i < framebuffer.length; i++) {
			int color = (i < vram.length) ? vram[i] & 0xFF : 0;
			framebuffer[i] = (color << 16) | (color << 8) | color; // RGB simples
		}
	}

	public int[] getFrameBuffer() {
		return framebuffer;
	}
}
