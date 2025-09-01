package emulator01;

//Vídeo Display Processor
public class VDP {
	
	private Memory memory;

    public VDP(Memory mem) {
        this.memory = mem;
    }

    public void render() {
        // Aqui iria a lógica de desenhar scanlines e sprites
        // Por enquanto só um print
        // Em produção usaria JavaFX ou LWJGL (OpenGL)
        System.out.println("Renderizando frame...");
    }

}
