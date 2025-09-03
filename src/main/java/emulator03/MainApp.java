package emulator03;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainApp extends Application {

	private Emulator emulator;
	private AtomicBoolean running = new AtomicBoolean(false);
	private TextArea disassemblyArea;

	// Preferences para armazenar caminho da última ROM
	private Preferences prefs = Preferences.userNodeForPackage(MainApp.class);

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Mega Drive - Emulator (JavaFX)");

		BorderPane root = new BorderPane();

		// Canvas principal para vídeo
		Canvas canvas = new Canvas(320, 224); // classic Megadrive resolution
		GraphicsContext gc = canvas.getGraphicsContext2D();

		// Painel de disassembly
		disassemblyArea = new TextArea();
		disassemblyArea.setEditable(false);
		disassemblyArea.setPrefWidth(400);

		SplitPane split = new SplitPane();
		split.getItems().addAll(canvas, disassemblyArea);
		split.setDividerPositions(0.5);

		root.setCenter(split);

		// Controles topo
		Button loadBtn = new Button("Load ROM");
		Button startBtn = new Button("Start");
		Button stopBtn = new Button("Stop");
		Label info = new Label("No ROM loaded");
		HBox top = new HBox(8, loadBtn, startBtn, stopBtn, info);
		root.setTop(top);

		Scene scene = new Scene(root, 1024, 480, Color.BLACK);
		primaryStage.setScene(scene);
		primaryStage.show();

		// --- Tenta carregar última ROM automaticamente ---
		String lastPath = prefs.get("lastRomPath", null);
		if (lastPath != null) {
			File lastFile = new File(lastPath);
			if (lastFile.exists()) {
				loadROM(lastFile, gc, info);
			}
		}

		// Load ROM e Disassemble
		loadBtn.setOnAction(ev -> {
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Select ROM (bin/rom)");
			File file = chooser.showOpenDialog(primaryStage);
			if (file != null) {
				loadROM(file, gc, info);
				// Salva o caminho nas preferências
				prefs.put("lastRomPath", file.getAbsolutePath());
			}
		});

		// Start/Stop Emulação
		startBtn.setOnAction(ev -> {
			if (emulator != null && !running.get()) {
				running.set(true);
				emulator.start();
			}
		});

		stopBtn.setOnAction(ev -> {
			if (emulator != null && running.get()) {
				running.set(false);
				emulator.stop();
			}
		});

		// Loop de renderização
		new AnimationTimer() {
			@Override
			public void handle(long now) {
				gc.setFill(Color.BLACK);
				gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

				if (emulator != null) {
					emulator.drawFrame();

//					String executed = emulator.getCpu().stepWithDisasm();
//					disassemblyArea.appendText(executed + "\n");
				}
			}
		}.start();
	}

	// Método utilitário para carregar ROM
	private void loadROM(File file, GraphicsContext gc, Label info) {
		try {
			Cartridge cart = new Cartridge(file.getAbsolutePath());
			Memory memory = new Memory(cart);
			emulator = new Emulator(memory, gc);
			info.setText("Loaded: " + file.getName() + " (" + cart.getSize() + " bytes)");

			// Disassemble didático
			String disasm = Disassembler68000.generateDisassembly(cart);
			disassemblyArea.setText(disasm);

		} catch (Exception ex) {
			ex.printStackTrace();
			info.setText("Failed to load ROM: " + ex.getMessage());
		}
	}
}