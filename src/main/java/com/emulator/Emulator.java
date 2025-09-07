package com.emulator;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.scene.canvas.GraphicsContext;

public class Emulator {

	private final CPU68000 cpu;
    private final VDP vdp;
    private final Memory memory;

    //private final GraphicsContext gc; // vem do EmulatorApp
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread emuThread;

    public Emulator(Memory memory) {
        this.memory = memory;
        this.cpu = new CPU68000(memory);
        this.vdp = new VDP();
    }

    /** Inicia emulação em uma thread */
    public void start() {
        if (running.get()) return;
        running.set(true);

        emuThread = new Thread(() -> {
            cpu.reset();
            while (running.get()) {
                // executa instrução da CPU
                int cycles = cpu.runInstruction();

                // avança o VDP com base nos ciclos
                vdp.run(cycles);

                // Simulação simplificada ~60fps
                try {
                    Thread.sleep(1); // ajustável conforme velocidade
                } catch (InterruptedException ignored) {
                }
            }
        }, "EmuThread");

        emuThread.setDaemon(true);
        emuThread.start();
    }

    /** Para a execução */
    public void stop() {
        running.set(false);
        if (emuThread != null) {
            try {
                emuThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
