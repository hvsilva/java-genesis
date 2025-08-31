package br.com.emulator;

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


import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;


public class EmulatorApp extends Application {
	
	private Emulator emulator;
	private AtomicBoolean running = new AtomicBoolean(false);
	
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Mega Drive - Emulator (JavaFX)");

		BorderPane root = new BorderPane();

		Canvas canvas = new Canvas(320, 224); // classic Megadrive resolution (320x224)
		GraphicsContext gc = canvas.getGraphicsContext2D();

		// Top controls
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

		loadBtn.setOnAction(ev -> {
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Select ROM (bin/rom)");
			File file = chooser.showOpenDialog(primaryStage);
			if (file != null) {
				try {
					Cartridge cart = new Cartridge(file.getAbsolutePath());
					Memory memory = new Memory(cart);
					emulator = new Emulator(memory, gc);
					info.setText("Loaded: " + file.getName() + " (" + cart.getSize() + " bytes)");
				} catch (Exception ex) {
					ex.printStackTrace();
					info.setText("Failed to load ROM: " + ex.getMessage());
				}
			}
		});

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

		// Simple render loop to update canvas even if emulator not running (for UI
		// responsiveness)
		new AnimationTimer() {
			@Override
			public void handle(long now) {
				gc.setFill(Color.BLACK);
				gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

				if (emulator != null) {
					emulator.drawFrame();
				}
			}
		}.start();

	}

}
