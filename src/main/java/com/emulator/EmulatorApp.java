package com.emulator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import br.com.emulator.java.FileLoader;

public class EmulatorApp extends JFrame {

	private static final long serialVersionUID = 1L;

	Emulator emulator;
	Memory memory;
	VDP vdp;
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
		emulator = new Emulator(memory, this);

		cpu = emulator.getCpu();
		vdp = emulator.getVdp();			

		// --- Área de vídeo ---
		video = new Video(320, 256);

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

		startBtn.addActionListener(ev -> {
			if (emulator != null)
				start();
		});

		stopBtn.addActionListener(ev -> {
			if (emulator != null)
				stop();
		});
	}

	public void start() {
		if (running.get())
			return;
		if (emulator == null) {
			System.out.println("Nenhuma ROM carregada!");
			return;
		}

		running.set(true);

		emuThread = new Thread(() -> {
			cpu.reset();
			cpu.initialize();
			vdp.init();
			loop(); // aqui roda o loop principal
		}, "EmuThread");

		emuThread.setDaemon(true);
		emuThread.start();
	}

//	private void loop() {
//	    try {
//	        while (running.get()) {   // usa running como condição
//	            if (!emulator.getCpu().stop) {
//	                emulator.getCpu().runInstruction(false);
//	            }
//
//	            emulator.checkInterrupts();
//	            emulator.getVdp().run(13);   // Roda 13 ciclos do VDP (ajustável para sincronismo)
//	            emulator.getVdp().dmaFill(); // DMA do VDP (preenchimento de memória de vídeo)
//	            emulator.getVdp().dmaFill(); // DMA do VDP (executado duas vezes por ciclo)
//	        }
//	    } catch (RuntimeException e) {
//	        e.printStackTrace();
//	    }
//	}	
	
	private void loop() {
	    long targetFrameTime = 16; 
	    
	    try {
	        while (running.get()) {
	            long frameStartTime = System.currentTimeMillis();

	            // Roda o processamento até o VDP completar um frame (262 linhas)
	            for (int i = 0; i < 262; i++) {
//	                int currentLine = emulator.getVdp().line;
//	                
//	                // Enquanto o VDP não mudar de linha, a CPU continua trabalhando
//	                // Adicionamos um limite de segurança (1000) para não travar
//	                int safety = 0;
//	                while (emulator.getVdp().line == currentLine && safety < 1000) {
//	                    if (!emulator.getCpu().stop) {
//	                        emulator.getCpu().runInstruction(false);
//	                    }
//	                    emulator.checkInterrupts();
//	                    emulator.getVdp().run(13); // Esses 13 ciclos fazem a 'line' subir eventualmente
//	                    emulator.getVdp().dmaFill();
//	                    emulator.getVdp().dmaFill();
//	                    safety++;
//	                }
	            	
	                for (int j = 0; j < 45; j++) { // Executa ~45 instruções por linha
		                if (!emulator.getCpu().stop) {
		                    emulator.getCpu().runInstruction(false);
		                }
		                emulator.checkInterrupts();
		                emulator.getVdp().run(13);   // 13 ciclos de VDP por instrução
		                emulator.getVdp().dmaFill(); // DMA do VDP (preenchimento de memória de vídeo)
	                    emulator.getVdp().dmaFill(); // DMA do VDP (executado duas vezes por ciclo)
		               
		            }
	            }
	
	            // O contador de FPS e o Sleep continuam aqui embaixo...
	            long timeSpent = System.currentTimeMillis() - frameStartTime;
	            if (timeSpent < targetFrameTime) {
	                Thread.sleep(targetFrameTime - timeSpent);
	            }
	        }
	    } catch (Exception e) { e.printStackTrace(); }
	}
	
	public void stop() {
		running.set(false);
		if (emuThread != null) {
			emuThread.interrupt();
		}
	}

	private void loadROM(File file) {
		try {
			memory.cartridge = FileLoader.readFile(file);
			info.setText("Loaded: " + file.getName() + " (" + memory.cartridge.length + " bytes)");
		} catch (Exception ex) {
			ex.printStackTrace();
			info.setText("Failed to load ROM: " + ex.getMessage());
		}
	}
	
	public void renderScreen() {
		video.render(vdp.screenData);
	}
}