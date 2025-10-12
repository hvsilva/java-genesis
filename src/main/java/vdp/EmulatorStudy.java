package vdp;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class EmulatorStudy extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private static final int WIDTH = 320;
	private static final int HEIGHT = 224;

	private final Video video;
	private final VDPStudy vdp;

	public EmulatorStudy() {
		
		super("Emulator Study - Integrado ao VDPStudy");

		// Cria o painel de vídeo
		video = new Video(WIDTH, HEIGHT);
		vdp = new VDPStudy(video); // passa a referência para o VDP

		// Layout principal
		setLayout(new BorderLayout());
		add(video, BorderLayout.CENTER);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);

		// Timer para atualizar a tela a ~30 FPS
		Timer timer = new Timer(33, e -> vdp.renderFrame());
		timer.start();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(EmulatorStudy::new);
	}
}
