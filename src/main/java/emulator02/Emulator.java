package emulator02;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import java.util.concurrent.atomic.AtomicBoolean;

import emulator02.CPU68000;
import emulator02.Memory;
import emulator02.VDP;

public class Emulator {

	private final CPU68000 cpu;
    private final VDP vdp;
    private final Memory memory;

    private final GraphicsContext gc; // vem do EmulatorApp
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread emuThread;

    public Emulator(Memory memory, GraphicsContext gc) {
        this.memory = memory;
        this.gc = gc;
        this.cpu = new CPU68000(memory);
        this.vdp = new VDP(gc);
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

    /** Chamado pelo AnimationTimer do EmulatorApp */
    public void drawFrame() {
        vdp.renderScreen();
    }
}
