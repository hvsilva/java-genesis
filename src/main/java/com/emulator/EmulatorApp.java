package com.emulator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class EmulatorApp extends JFrame {

	private static final long serialVersionUID = 1L;	
	
	private Emulator emulator;
	private final Preferences prefs = Preferences.userNodeForPackage(EmulatorApp.class);
	private final JLabel info;
	private final Video video;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(EmulatorApp::new);
	}

	public EmulatorApp() {
		super("Emulador Didático Mega Drive");

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

		startBtn.addActionListener(ev -> {
			if (emulator != null)
				emulator.start();
		});

		stopBtn.addActionListener(ev -> {
			if (emulator != null)
				emulator.stop();
		});
	}

	private void loadROM(File file) {
		try {
			Cartridge cart = new Cartridge(file.getAbsolutePath());
			Memory memory = new Memory(cart);
			emulator = new Emulator(memory);

			info.setText("Loaded: " + file.getName() + " (" + cart.getSize() + " bytes)");
		} catch (Exception ex) {
			ex.printStackTrace();
			info.setText("Failed to load ROM: " + ex.getMessage());
		}
	}
}