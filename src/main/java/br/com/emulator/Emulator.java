package br.com.emulator;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

//Loop principal
public class Emulator {

	  private Memory memory;
	    private GraphicsContext gc;
	    private boolean running;

	    public Emulator(Memory memory, GraphicsContext gc) {
	        this.memory = memory;
	        this.gc = gc;
	    }

	    public void start() {
	        running = true;
	        new Thread(() -> {
	            while (running) {
	                stepCPU();
	                renderFrame();
	                try { Thread.sleep(16); } catch (InterruptedException ignored) {} // ~60fps
	            }
	        }).start();
	    }

	    public void stop() {
	        running = false;
	    }

	    private void stepCPU() {
	        // Aqui você processaria instruções do 68000
	        // memory.fetch(), decode, execute...
	    }

	    private void renderFrame() {
	        // Desenhar algo na tela usando gc
	        // Por enquanto, exemplo simples:
	        gc.setFill(Color.DARKBLUE);
	        gc.fillRect(50, 50, 220, 120);
	    }

	    public void drawFrame() {
	        // Pode ser chamado a cada AnimationTimer para atualizar o canvas
	        // Pode chamar renderFrame() se quiser
	    }

}
