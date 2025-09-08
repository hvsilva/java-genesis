package com.emulator;

import java.io.File;
import java.util.prefs.Preferences;

// JavaFX UI
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class EmulatorApp extends Application {
	
	private Emulator emulator;

	// Preferences para armazenar caminho da última ROM
	private Preferences prefs = Preferences.userNodeForPackage(EmulatorApp.class);

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Emulador Didático Mega Drive");

		BorderPane root = new BorderPane();
		Canvas canvas = new Canvas(320, 224);
		GraphicsContext gc = canvas.getGraphicsContext2D();

		Button loadBtn = new Button("Load ROM");
		Button startBtn = new Button("Start");
		Button stopBtn = new Button("Stop");
		Label info = new Label("No ROM loaded");

		HBox top = new HBox(8, loadBtn, startBtn, stopBtn, info);
		root.setTop(top);
		root.setCenter(canvas);

		Scene scene = new Scene(root, 640, 480, Color.BLACK);
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

		startBtn.setOnAction(ev -> {
			if (emulator != null)
				emulator.start();
		});

		stopBtn.setOnAction(ev -> {
			if (emulator != null)
				emulator.stop();
		});

//		new AnimationTimer() {
//			@Override
//			public void handle(long now) {
//				gc.setFill(Color.BLACK);
//				gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
//				if (emulator != null)
//					emulator.drawFrame();
//			}
//		}.start();
	}

	// Método utilitário para carregar ROM
	private void loadROM(File file, GraphicsContext gc, Label info) {
		try {
			Cartridge cart = new Cartridge(file.getAbsolutePath());
//			Cartridge cart = new Cartridge(file);
			Memory memory = new Memory(cart);
			emulator = new Emulator(memory);
			info.setText("Loaded: " + file.getName() + " (" + cart.getSize() + " bytes)");

		} catch (Exception ex) {
			ex.printStackTrace();
			info.setText("Failed to load ROM: " + ex.getMessage());
		}
	}
}