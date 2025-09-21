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
		emulator = new Emulator(memory);

		cpu = emulator.getCpu();
		vdp = emulator.getVdp();

		// --- Área de vídeo ---
		video = new Video(320, 224);

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

	private void loop() {
		try {
			for (;;) {
				if (!emulator.getCpu().stop) {
					emulator.getCpu().runInstruction(false); // executa próxima instrução da CPU 68000
				}

				// roda o VDP (ajuste o número de ciclos conforme necessário)
				emulator.checkInterrupts(); // se já implementado
				emulator.getVdp().run(13);
				emulator.getVdp().dmaFill();
				emulator.getVdp().dmaFill();
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
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
			memory.cartridge = FileLoader.readFile(file);
			info.setText("Loaded: " + file.getName() + " (" + memory.cartridge.length + " bytes)");
		} catch (Exception ex) {
			ex.printStackTrace();
			info.setText("Failed to load ROM: " + ex.getMessage());
		}
	}
}