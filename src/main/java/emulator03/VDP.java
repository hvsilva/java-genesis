package emulator03;

public class VDP {
	
	 private int[] framebuffer = new int[320 * 224];

	    public void step() {
	        // Preenche com padrão para teste
	        for (int i = 0; i < framebuffer.length; i++) {
	            framebuffer[i] = ((i % 256) << 16); // vermelho gradiente
	        }
	    }

	    public int[] getFrameBuffer() {
	        return framebuffer;
	    }

}
