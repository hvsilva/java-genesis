package emulator01;

//Loop principal
public class Emulator {

	public static void main(String[] args) {
		Emulator emu = new Emulator("D:\\PROJETOS_EMULADOR\\java-genesis\\roms\\SONIC1.BIN");
		emu.run();
	}

	private CPU68000 cpu;
	private Z80 z80;
	private VDP vdp;
	private Memory memory;
	private Cartridge cart;

	public Emulator(String romPath) {
		cart = new Cartridge(romPath);
		memory = new Memory(cart);
		cpu = new CPU68000(memory);
//		z80 = new Z80(memory);
		vdp = new VDP(memory);
	}

	public void run() {
		System.out.println("Iniciando emulador Mega Drive...");

		while (true) {
			// Executa 1 instrução 68k
			cpu.step();

			// Executa 1 instrução Z80 (sincronizar ciclos depois!)
//			z80.step();

			// Atualiza vídeo
			vdp.render();

			// Debug simples
			if (cpu.getPC() == 0x00FF0000) {
				System.out.println("Debug: PC chegou em endereço especial!");
			}
		}
	}

}
