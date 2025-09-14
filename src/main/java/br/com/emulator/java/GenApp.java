package br.com.emulator.java;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import br.com.emulator.java.addressing.AbsoluteLong;
import br.com.emulator.java.addressing.AbsoluteShort;
import br.com.emulator.java.addressing.AddressRegisterDirect;
import br.com.emulator.java.addressing.AddressRegisterIndirect;
import br.com.emulator.java.addressing.AddressRegisterIndirectPostIncrement;
import br.com.emulator.java.addressing.AddressRegisterIndirectPreDecrement;
import br.com.emulator.java.addressing.AddressRegisterWithDisplacement;
import br.com.emulator.java.addressing.AddressRegisterWithIndex;
import br.com.emulator.java.addressing.AddressingMode;
import br.com.emulator.java.addressing.DataRegisterDirect;
import br.com.emulator.java.addressing.ImmediateData;
import br.com.emulator.java.addressing.PCWithDisplacement;
import br.com.emulator.java.addressing.PCWithIndex;
import br.com.emulator.java.instruction.ABCD;
import br.com.emulator.java.instruction.ADD;
import br.com.emulator.java.instruction.ADDA;
import br.com.emulator.java.instruction.ADDI;
import br.com.emulator.java.instruction.ADDQ;
import br.com.emulator.java.instruction.ADDX;
import br.com.emulator.java.instruction.AND;
import br.com.emulator.java.instruction.ANDI;
import br.com.emulator.java.instruction.ANDI_CCR;
import br.com.emulator.java.instruction.ANDI_SR;
import br.com.emulator.java.instruction.ASL;
import br.com.emulator.java.instruction.ASR;
import br.com.emulator.java.instruction.BCC;
import br.com.emulator.java.instruction.BCHG;
import br.com.emulator.java.instruction.BCLR;
import br.com.emulator.java.instruction.BSET;
import br.com.emulator.java.instruction.BTST;
import br.com.emulator.java.instruction.CLR;
import br.com.emulator.java.instruction.CMP;
import br.com.emulator.java.instruction.CMPA;
import br.com.emulator.java.instruction.CMPI;
import br.com.emulator.java.instruction.CMPM;
import br.com.emulator.java.instruction.DBcc;
import br.com.emulator.java.instruction.DIVS;
import br.com.emulator.java.instruction.DIVU;
import br.com.emulator.java.instruction.EOR;
import br.com.emulator.java.instruction.EORI;
import br.com.emulator.java.instruction.EORI_CCR;
import br.com.emulator.java.instruction.EORI_SR;
import br.com.emulator.java.instruction.EXG;
import br.com.emulator.java.instruction.EXT;
import br.com.emulator.java.instruction.JMP;
import br.com.emulator.java.instruction.JSR;
import br.com.emulator.java.instruction.LEA;
import br.com.emulator.java.instruction.LINK;
import br.com.emulator.java.instruction.LSL;
import br.com.emulator.java.instruction.LSR;
import br.com.emulator.java.instruction.MOVE;
import br.com.emulator.java.instruction.MOVEA;
import br.com.emulator.java.instruction.MOVEM;
import br.com.emulator.java.instruction.MOVEP;
import br.com.emulator.java.instruction.MOVEQ;
import br.com.emulator.java.instruction.MOVE_FROM_SR;
import br.com.emulator.java.instruction.MOVE_TO_CCR;
import br.com.emulator.java.instruction.MOVE_TO_FROM_USP;
import br.com.emulator.java.instruction.MOVE_TO_SR;
import br.com.emulator.java.instruction.MULS;
import br.com.emulator.java.instruction.MULU;
import br.com.emulator.java.instruction.NBCD;
import br.com.emulator.java.instruction.NEG;
import br.com.emulator.java.instruction.NOP;
import br.com.emulator.java.instruction.NOT;
import br.com.emulator.java.instruction.OR;
import br.com.emulator.java.instruction.ORI;
import br.com.emulator.java.instruction.ORI_CCR;
import br.com.emulator.java.instruction.ORI_SR;
import br.com.emulator.java.instruction.PEA;
import br.com.emulator.java.instruction.ROR;
import br.com.emulator.java.instruction.ROXL;
import br.com.emulator.java.instruction.ROXR;
import br.com.emulator.java.instruction.RTE;
import br.com.emulator.java.instruction.RTR;
import br.com.emulator.java.instruction.RTS;
import br.com.emulator.java.instruction.SBCD;
import br.com.emulator.java.instruction.STOP;
import br.com.emulator.java.instruction.SUB;
import br.com.emulator.java.instruction.SUBA;
import br.com.emulator.java.instruction.SUBI;
import br.com.emulator.java.instruction.SUBQ;
import br.com.emulator.java.instruction.SWAP;
import br.com.emulator.java.instruction.Scc;
import br.com.emulator.java.instruction.TRAP;
import br.com.emulator.java.instruction.TST;
import br.com.emulator.java.instruction.UNLK;

//	MEMORY MAP:	https://en.wikibooks.org/wiki/Genesis_Programming
public class GenApp {

	GenMemory memory;
	GenVdp vdp;
	GenEmulator bus;
	GenZ80 z80;
	CPU68000 cpu;
	GenJoypad joypad;

	private static int[] pixels;

	int debugMemoryChangedAddress;
	int debugMemoryChangedData;

	boolean romCargo = false;

	int CLOCKSPEED = 4194304;

	final JFrame jframe = new JFrame("JAVA MEGADRIVE");
	private Thread currentGameThread;
	private MyRunnable currentRunna;
	private boolean isRomOpened;

	JCheckBoxMenuItem usaBios;
	JCheckBoxMenuItem eurBios;
	JCheckBoxMenuItem japBios;

	StringBuilder lineLog = new StringBuilder(300);

	static BufferedImage img = new BufferedImage(320, 256, BufferedImage.TYPE_INT_RGB);
	
	// Preferences para armazenar caminho da última ROM
	private Preferences prefs = Preferences.userNodeForPackage(GenApp.class);
	
	final JLabel label = new JLabel(new ImageIcon(img));

	public static void main(String[] args) throws Exception {
		// Create the frame on the event dispatching thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new GenApp();
			}
		});
	}

	GenApp() {
		this(false);
	}

	GenApp(boolean debug) {
		bus = new GenEmulator(this, null, null, null, null, null);
		memory = new GenMemory();
		vdp = new GenVdp(bus);
		z80 = new GenZ80(bus);
		cpu = new CPU68000(bus);
		joypad = new GenJoypad();

		bus.memory = memory;
		bus.vdp = vdp;
		bus.z80 = z80;
		bus.joypad = joypad;
		bus.cpu = cpu;

		new ABCD(cpu).generate();
		new ADD(cpu).generate();
		new ADDA(cpu).generate();
		new ADDI(cpu).generate();
		new ADDQ(cpu).generate();
		new ADDX(cpu).generate();
		new AND(cpu).generate();
		new ANDI(cpu).generate();
		new ANDI_CCR(cpu).generate();
		new ANDI_SR(cpu).generate();
		new ASL(cpu).generate();
		new ASR(cpu).generate();
		new BCC(cpu).generate();
		new BCHG(cpu).generate();
		new BCLR(cpu).generate();
		new BSET(cpu).generate();
		new BTST(cpu).generate();
		new CLR(cpu).generate();
		new CMP(cpu).generate();
		new CMPA(cpu).generate();
		new CMPI(cpu).generate();
		new CMPM(cpu).generate();
		new DBcc(cpu).generate();
		new DIVS(cpu).generate();
		new DIVU(cpu).generate();
		new EOR(cpu).generate();
		new EORI(cpu).generate();
		new EORI_CCR(cpu).generate();
		new EORI_SR(cpu).generate();
		new EXG(cpu).generate();
		new EXT(cpu).generate();
		new JMP(cpu).generate();
		new JSR(cpu).generate();
		new LEA(cpu).generate();
		new LINK(cpu).generate();
		new LSL(cpu).generate();
		new LSR(cpu).generate();
		new MOVE(cpu).generate();
		new MOVEA(cpu).generate();
		new MOVE_FROM_SR(cpu).generate();
		new MOVE_TO_CCR(cpu).generate();
		new MOVE_TO_SR(cpu).generate();
		new MOVE_TO_FROM_USP(cpu).generate();
		new MOVEM(cpu).generate();
		new MOVEP(cpu).generate();
		new MOVEQ(cpu).generate();
		new MULS(cpu).generate();
		new MULU(cpu).generate();
		new NBCD(cpu).generate();
		new NEG(cpu).generate();
		new NOP(cpu).generate();
		new NOT(cpu).generate();
		new OR(cpu).generate();
		new ORI(cpu).generate();
		new ORI_CCR(cpu).generate();
		new ORI_SR(cpu).generate();
		new PEA(cpu).generate();
		new ROR(cpu).generate();
		new ROXL(cpu).generate();
		new ROXR(cpu).generate();
		new RTE(cpu).generate();
		new RTR(cpu).generate();
		new RTS(cpu).generate();
		new SBCD(cpu).generate();
		new Scc(cpu).generate();
		new STOP(cpu).generate();
		new SUB(cpu).generate();
		new SUBA(cpu).generate();
		new SUBI(cpu).generate();
		new SUBQ(cpu).generate();
		new SWAP(cpu).generate();
		new TRAP(cpu).generate();
		new TST(cpu).generate();
		new UNLK(cpu).generate();

		System.out.println("[CPU.TOTALINSTRUCTIONS] :  " + cpu.totalInstructions);

		cpu.addressingModes = new AddressingMode[] { 
				new DataRegisterDirect(cpu), 
				new AddressRegisterDirect(cpu),
				new AddressRegisterIndirect(cpu), 
				new AddressRegisterIndirectPostIncrement(cpu),
				new AddressRegisterIndirectPreDecrement(cpu), 
				new AddressRegisterWithDisplacement(cpu),
				new AddressRegisterWithIndex(cpu),
				new AbsoluteShort(cpu), 
				new AbsoluteLong(cpu), 
				new PCWithDisplacement(cpu), 
				new PCWithIndex(cpu),
				new ImmediateData(cpu), //somente se for um operando fonte TODO, se estiver escrevendo é StatusRegisterOperand
		};

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}

		pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

		Graphics g = img.getGraphics();
		g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
		g.dispose();

		JMenuBar bar = new JMenuBar();

		JMenu menu = new JMenu("File");
		bar.add(menu);

		JMenu menuBios = new JMenu("Region");
		bar.add(menuBios);

		usaBios = new JCheckBoxMenuItem("USA", true);
//    	usaBios.addActionListener(new ScreenListener(1));
		menuBios.add(usaBios);

		eurBios = new JCheckBoxMenuItem("Europe", false);
		menuBios.add(eurBios);

		japBios = new JCheckBoxMenuItem("Japan", false);
		menuBios.add(japBios);

//        JMenu viewMenu = new JMenu("View");
//        bar.add(viewMenu);
		JMenu helpMenu = new JMenu("Nefusto");
		bar.add(helpMenu);

		JMenuItem loadRomItem = new JMenuItem("Load ROM");
		loadRomItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openRomDialog();
			}
		});
		JMenuItem closeRomItem = new JMenuItem("Close ROM");
		closeRomItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
//                markForClose = true;
			}
		});
		JMenuItem loadItem = new JMenuItem("Quick load");
		loadItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
//                markForLoad = true;
			}
		});
		JMenuItem saveItem = new JMenuItem("Quick save");
		saveItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
//                markForSave = true;
			}
		});

//        for (int i = 1; i <= 4; i++) {
//        	JMenuItem zoomItem = new JMenuItem("x" + i);
//        	zoomItem.addActionListener(new ScreenListener(i));
//        	viewMenu.add(zoomItem);
//		}

		JCheckBoxMenuItem biosItem = new JCheckBoxMenuItem("Use BIOS", true);
		biosItem.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.DESELECTED) {
//            		loadBios = false;
				} else {
//            		loadBios = true;
				}
			}
		});
//        viewMenu.add(biosItem);

		JMenuItem aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null, "Nefusto, Nefusto... Barril sin fondo");
			}
		});

		menu.add(loadRomItem);
		menu.add(closeRomItem);
		menu.add(loadItem);
		menu.add(saveItem);

		helpMenu.add(aboutItem);

		jframe.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				keyPressedHandler(e);
			}

			@Override
			public void keyReleased(KeyEvent e) {
				keyReleasedHandler(e);
			}
		});

		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jframe.setLocation(400, 400);
		jframe.setResizable(false);
		jframe.setJMenuBar(bar);
		jframe.add(label);
		jframe.pack();
		jframe.setVisible(true);
	}

	public void printLog(String fullOpcode) {
//    	if (cpu.halted) {
//    		System.out.println("HALTED");
//    	} else {
//	    	lineLog.setLength(0);
//	        lineLog.append("af: ").append(cpu.pad(cpu.A)).append(cpu.pad(cpu.F)).append(" - bc: ").append(cpu.pad(cpu.B))
//	            .append(cpu.pad(cpu.C)).append(" - de: ").append(cpu.pad(cpu.D)).append(cpu.pad(cpu.E)).append(" - hl: ")
//	            .append(cpu.pad(cpu.H)).append(cpu.pad(cpu.L)).append("\npc: ").append(cpu.pad4(cpu.programCounter - 1))
//	            .append(" - sp: ").append(cpu.pad4(cpu.stackPointer)).append(" - opcode: ").append(fullOpcode)
//	            .append("\n").append("lcdc: ").append(cpu.pad(vdp.lcdControl)).append(" - stat: ")
//	            .append(cpu.pad(vdp.ff41)).append(" - ly: ").append(cpu.pad(vdp.ff44)).append(" - IE: ")
//	            .append(cpu.pad(cpu.ffff)).append(" - IF: ").append(cpu.pad(cpu.ff0f)).append("\n")
//	            .append("04: ").append(cpu.pad(timer.ff04)).append(" - 05: ").append(cpu.pad(timer.ff05))
//	            .append(" - 06: ").append(cpu.pad(timer.ff06)).append(" - 07: ").append(cpu.pad(timer.ff07))
//	            .append("\n").append("scanline: ").append(vdp.scanlineCounter).append(" - RomBank: ").append(cpu.pad(memory.currentROMBank))
//	            .append(" - Vram Bank: ").append(cpu.pad(vdp.currentVramBank))
//	            .append(" - Wram Bank: ").append(cpu.pad(memory.currentWramBank)).append("\n");
//	//        	.append("BGPal: ").append(pad(rom[0xFF68])).append(" - BGPal data: ").append(pad(rom[0xFF69]))
//	//        	.append(" - OBJPal: ").append(pad(rom[0xFF6A])).append(" - OBJPal data: ").append(pad(rom[0xFF6B])).append("\n");
//	        System.out.println(lineLog.toString());
//    	}
	}

	class ScreenListener implements ActionListener {

		int multiplier;

		public ScreenListener(int multi) {
			multiplier = multi;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			adjustScreen();
		}

		private void adjustScreen() {
			int multi = multiplier;
			currentMultiplier = multi;

			jframe.setSize(160 * multi, 144 * multi);
			img = new BufferedImage(160 * multi, 144 * multi, BufferedImage.TYPE_INT_RGB);
			pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

			Graphics g = img.getGraphics();
			g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
			g.dispose();

			jframe.remove(label);
			final JLabel newLabel = new JLabel(new ImageIcon(img));
			jframe.add(newLabel);

			jframe.pack();
		}
	}
	

//  String basePath = "C:\\Users\\Zotac\\workspace\\raul\\src\\gen\\roms\\";
	String basePath = "D:\\PROJETOS_EMULADOR\\Gensis_ROMS\\";


	private void openRomDialog() {
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return "md and bin files";
			}

			@Override
			public boolean accept(File f) {
				String name = f.getName().toLowerCase();
				return f.isDirectory() || name.endsWith(".md") || name.endsWith(".bin");
			}
		});
		fileChooser.setCurrentDirectory(new File(basePath));
		int result = fileChooser.showOpenDialog(jframe);
		if (result == JFileChooser.APPROVE_OPTION) {
			if (isRomOpened) {
//                markForClose = true;
				try {
					currentGameThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			isRomOpened = true;
			File selectedFile = fileChooser.getSelectedFile();
			currentRunna = new MyRunnable(selectedFile);
			currentGameThread = new Thread(currentRunna);
			currentGameThread.start();
		}
	}

	class MyRunnable implements Runnable {
		File file;

		public MyRunnable(File file) {
			this.file = file;
		}

		@Override
		public void run() {
			if (file.getName().toLowerCase().endsWith(".zip")) {
//                memory.cartridgeMemory = GBFileLoader.readZipFile(file);
			} else if (file.getName().toLowerCase().endsWith(".md") || file.getName().toLowerCase().endsWith(".bin")) {
				memory.cartridge = FileLoader.readFile(file);
				System.out.println("Loaded: " + file.getName() + " (" + memory.cartridge.length + " bytes)");
			}

			String rom = file.getName();
			jframe.setTitle(jframe.getTitle() + " - " + rom);

			cpu.reset();
			cpu.initialize();
			joypad.initialize();
			vdp.init();
			z80.initialize();

			int[] ssf2Title = new int[] { 0x53, 0x55, 0x50, 0x45, 0x52, 0x20, 0x53, 0x54, 0x52, 0x45, 0x45, 0x54, 0x20,
					0x46, 0x49, 0x47, 0x48, 0x54, 0x45, 0x52, 0x32, 0x20, 0x54, 0x68, 0x65, 0x20, 0x4E, 0x65, 0x77,
					0x20, 0x43, 0x68, 0x61, 0x6C, 0x6C, 0x65, 0x6E, 0x67, 0x65, 0x72, 0x73, 0x20, 0x20, 0x20, 0x20,
					0x20, 0x20, 0x20 };

			int[] titanOverdrive2Title = new int[] { 0x4F, 0x56, 0x45, 0x52, 0x44, 0x52, 0x49, 0x56, 0x45, 0x20, 0x32,
					0x20, 0x20, 0x20, 0x20, 0x20, };

			boolean isSsf2Mapper = true;
			for (int i = 0; i < ssf2Title.length; i++) {
				if (memory.cartridge[0x150 + i] != ssf2Title[i]) {
					isSsf2Mapper = false;
				}
			}

			if (!isSsf2Mapper) {
				isSsf2Mapper = true;
				for (int i = 0; i < titanOverdrive2Title.length; i++) {
					if (memory.cartridge[0x150 + i] != titanOverdrive2Title[i]) {
						isSsf2Mapper = false;
					}
				}
			}

			bus.ssf2Mapper = isSsf2Mapper;
			if (isSsf2Mapper) {
				System.out.println("SSF2 Mapper!");
			}

			loop();
		}
	}
	
	private int currentMultiplier = 1;
	public boolean runZ80 = false;

	void loop() {
		try {
			for (;;) {
				if (runZ80) { // TODO fazer com que você use a velocidade correta e tenha um fio distinto
					int opcode = z80.readMemory(z80.PC);
					z80.PC = (z80.PC + 1) & 0xFFFF;
					z80.executeInstruction(opcode);
				}
				// Execução da CPU 68000 (principal)
				if (!cpu.stop) {
					cpu.runInstruction(true);// Executa próxima instrução da CPU principal
				}
				 // Checagem de interrupções do barramento
				bus.checkInterrupts();
				vdp.run(13);    // Roda 13 ciclos do VDP (ajustável para sincronismo)
				vdp.dmaFill();  // DMA do VDP (preenchimento de memória de vídeo)
				vdp.dmaFill();  // DMA do VDP (executado duas vezes por ciclo)
			}
		} catch (RuntimeException e) {
			throw e;
		}
	}

	void renderScreen() {
		int m = currentMultiplier;

		for (int i = 0; i < 256; i++) {
			for (int j = 0; j < 320; j++) {
				int color = vdp.screenData[j][i];

				int pos = ((i * m) * (320 * m)) + (j * m);

				pixels[pos] = color;
			}
		}

		jframe.repaint();
	}

//	PD5: Start or C
//	PD4: A or B
//	PD3: Right
//	PD2: Left
//	PD1: Down
//	PD0: Up
	private void keyPressedHandler(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_W:
			cpu.bus.joypad.U = 0;
			break;
		case KeyEvent.VK_A:
			cpu.bus.joypad.L = 0;
			break;
		case KeyEvent.VK_D:
			cpu.bus.joypad.R = 0;
			break;
		case KeyEvent.VK_S:
			cpu.bus.joypad.D = 0;
			break;
		case KeyEvent.VK_E:
			cpu.bus.joypad.S = 0;
			break;
		case KeyEvent.VK_T:
			cpu.bus.joypad.A = 0;
			break;
		case KeyEvent.VK_Y:
			cpu.bus.joypad.B = 0;
			break;
		case KeyEvent.VK_U:
			cpu.bus.joypad.C = 0;
			break;
		case KeyEvent.VK_ESCAPE:
			openRomDialog();
			break;
		}
	}

	private void keyReleasedHandler(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_W:
			cpu.bus.joypad.U = 1;
			break;
		case KeyEvent.VK_A:
			cpu.bus.joypad.L = 1;
			break;
		case KeyEvent.VK_D:
			cpu.bus.joypad.R = 1;
			break;
		case KeyEvent.VK_S:
			cpu.bus.joypad.D = 1;
			break;
		case KeyEvent.VK_E:
			cpu.bus.joypad.S = 1;
			break;
		case KeyEvent.VK_T:
			cpu.bus.joypad.A = 1;
			break;
		case KeyEvent.VK_Y:
			cpu.bus.joypad.B = 1;
			break;
		case KeyEvent.VK_U:
			cpu.bus.joypad.C = 1;
			break;
		}
	}

	// US: A0A0 rev 0 o A1A1 rev 1
	// EU: C1C1
	// JP: ????
	// US SEGA CD: 8181
	public long getRegion() {
		if (japBios.isSelected()) {
			return 0;
		} else if (eurBios.isSelected()) {
			return 0xC1;
		} else {
			return 0xA0;
		}
	}
}
