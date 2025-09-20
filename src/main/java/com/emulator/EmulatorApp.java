package com.emulator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.emulator.instruction.ABCD;
import com.emulator.instruction.ADD;
import com.emulator.instruction.ADDQ;
import com.emulator.instruction.ADDX;
import com.emulator.instruction.ANDI;
import com.emulator.instruction.ANDI_CCR;
import com.emulator.instruction.ANDI_SR;
import com.emulator.instruction.BCC;
import com.emulator.instruction.BTST;
import com.emulator.instruction.CLR;
import com.emulator.instruction.CMP;
import com.emulator.instruction.CMPI;
import com.emulator.instruction.DBcc;
import com.emulator.instruction.JSR;
import com.emulator.instruction.LEA;
import com.emulator.instruction.MOVE;
import com.emulator.instruction.MOVEA;
import com.emulator.instruction.MOVEM;
import com.emulator.instruction.MOVEP;
import com.emulator.instruction.MOVEQ;
import com.emulator.instruction.MOVE_FROM_SR;
import com.emulator.instruction.MOVE_TO_CCR;
import com.emulator.instruction.MOVE_TO_FROM_USP;
import com.emulator.instruction.MOVE_TO_SR;
import com.emulator.instruction.OR;
import com.emulator.instruction.ORI;
import com.emulator.instruction.ORI_CCR;
import com.emulator.instruction.ORI_SR;
import com.emulator.instruction.RTS;
import com.emulator.instruction.SUBI;
import com.emulator.instruction.SUBQ;
import com.emulator.instruction.Scc;
import com.emulator.instruction.TST;

import br.com.emulator.java.FileLoader;


public class EmulatorApp extends JFrame {

	private static final long serialVersionUID = 1L;	
	
    private Emulator emulator;
    private Memory memory;
    CPU68000 cpu;
    
    private Thread emuThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
	private final Preferences prefs = Preferences.userNodeForPackage(EmulatorApp.class);
	private final JLabel info;
	private final Video video;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(EmulatorApp::new);
	}

	public EmulatorApp() {
		super("Emulador Didático Mega Drive");		
		
		memory = new Memory();
		cpu = new CPU68000(emulator);
			

		// --- Área de vídeo ---
		video = new Video(320, 224);
		
		JCheckBoxMenuItem usaBios;
		JCheckBoxMenuItem eurBios;
		JCheckBoxMenuItem japBios;

		// --- Barra superior ---
		JButton loadBtn = new JButton("Load ROM");
		JButton startBtn = new JButton("Start");
		JButton stopBtn = new JButton("Stop");
		info = new JLabel("No ROM loaded");

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(loadBtn);
		top.add(startBtn);
		top.add(stopBtn);
		top.add(info);	
	
		// --- Layout principal ---
		setLayout(new BorderLayout());
		add(top, BorderLayout.NORTH);
		add(video, BorderLayout.CENTER);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(640, 480);
		setLocationRelativeTo(null);
		setVisible(true);

		// --- Tenta carregar última ROM ---
		String lastPath = prefs.get("lastRomPath", null);
		if (lastPath != null) {
			File lastFile = new File(lastPath);
			if (lastFile.exists()) {
				loadROM(lastFile);
			}
		}

		// --- Listeners ---
		loadBtn.addActionListener(ev -> {
			JFileChooser chooser = new JFileChooser();
			int result = chooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				File file = chooser.getSelectedFile();
				loadROM(file);
				prefs.put("lastRomPath", file.getAbsolutePath());
			}
		});
		
		new ABCD(cpu).generate();
		new ADD(cpu).generate();
//		new ADDA(this).generate();
//		new ADDI(this).generate();
		new ADDQ(cpu).generate();
		new ADDX(cpu).generate();
//		new AND(this).generate();
		new ANDI(cpu).generate();
		new ANDI_CCR(cpu).generate();
		new ANDI_SR(cpu).generate();
//		new ASL(this).generate();
//		new ASR(this).generate();
		new BCC(cpu).generate();
//		new BCHG(this).generate();
//		new BCLR(this).generate();
//		new BSET(this).generate();
		new BTST(cpu).generate();
		new CLR(cpu).generate();
		new CMP(cpu).generate();
//		new CMPA(this).generate();
		new CMPI(cpu).generate();
//		new CMPM(this).generate();
		new DBcc(cpu).generate();
//		new DIVS(this).generate();
//		new DIVU(this).generate();
//		new EOR(this).generate();
//		new EORI(this).generate();
//		new EORI_CCR(this).generate();
//		new EORI_SR(this).generate();
//		new EXG(this).generate();
//		new EXT(this).generate();
//		new JMP(this).generate();
		new JSR(cpu).generate();
		new LEA(cpu).generate();
//		new LINK(this).generate();
//		new LSL(this).generate();
//		new LSR(this).generate();
		new MOVE(cpu).generate();
		new MOVEA(cpu).generate();
		new MOVE_FROM_SR(cpu).generate();
		new MOVE_TO_CCR(cpu).generate();
		new MOVE_TO_SR(cpu).generate();
		new MOVE_TO_FROM_USP(cpu).generate();
		new MOVEM(cpu).generate();
		new MOVEP(cpu).generate();
		new MOVEQ(cpu).generate();
//		new MULS(this).generate();
//		new MULU(this).generate();
//		new NBCD(this).generate();
//		new NEG(this).generate();
//		new NOP(this).generate();
//		new NOT(this).generate();
		new OR(cpu).generate();
		new ORI(cpu).generate();
		new ORI_CCR(cpu).generate();
		new ORI_SR(cpu).generate();
//		new PEA(this).generate();
//		new ROR(this).generate();
//		new ROXL(this).generate();
//		new ROXR(this).generate();
//		new RTE(this).generate();
//		new RTR(this).generate();
		new RTS(cpu).generate();
//		new SBCD(this).generate();
		new Scc(cpu).generate();
//		new STOP(this).generate();
//		new SUB(this).generate();
//		new SUBA(this).generate();
		new SUBI(cpu).generate();
		new SUBQ(cpu).generate();
//		new SWAP(this).generate();
//		new TRAP(this).generate();
		new TST(cpu).generate();
//		new UNLK(this).generate();  	

	}
	
	   public void start() {
	        if (running.get()) return;
	        running.set(true);

	        emuThread = new Thread(() -> {
	            emulator.getCpu().reset();
	            emulator.getCpu().initialize();
	            emulator.getVdp().init();
	            loop();   // aqui roda o loop principal
	        }, "EmuThread");

	        emuThread.setDaemon(true);
	        emuThread.start();
	    }
	   
	   private void loop() {
		    try {
		        while (running.get()) {
		        	
		            // Execução do Z80 (se existir no seu estudo, senão pode comentar)
//		            if (emulator.hasZ80() && emulator.getZ80().isRunning()) {
//		                int opcode = emulator.getZ80().readMemory(emulator.getZ80().getPC());
//		                emulator.getZ80().setPC((emulator.getZ80().getPC() + 1) & 0xFFFF);
//		                emulator.getZ80().executeInstruction(opcode);
//		            }

		            // Execução da CPU 68000 (principal)
		            if (!emulator.getCpu().isStopped()) {
		                emulator.getCpu().runInstruction(false);
		            }

		            // Checagem de interrupções no barramento
		            emulator.checkInterrupts();

		            // VDP - executa alguns ciclos por iteração
		            emulator.getVdp().run(13);

		            // DMA do VDP
		            emulator.getVdp().dmaFill();
		            emulator.getVdp().dmaFill();
		        }
		    } catch (RuntimeException e) {
		        e.printStackTrace();
		        throw e;
		    }
		}
	   
	   public void stop() {
	        running.set(false);
	        if (emuThread != null) {
	            emuThread.interrupt();
	        }
	    }

	private void loadROM(File file) {
		try {
//			Cartridge cart = new Cartridge(file.getAbsolutePath());
//			emulator = new Emulator(cart.getROMData());
			
			memory.cartridge = FileLoader.readFile(file);

			info.setText("Loaded: " + file.getName() + " (" +  memory.cartridge.length + " bytes)");
			
			
		} catch (Exception ex) {
			ex.printStackTrace();
			info.setText("Failed to load ROM: " + ex.getMessage());
		}
	}
}