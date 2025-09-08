package com.emulator;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.scene.canvas.GraphicsContext;

public class Emulator {

	private final CPU68000 cpu;
    private final VDP vdp;
    private final Memory memory;
    
    boolean vintPending;
	boolean hintPending;

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
    	        cpu.initialize();
    	        vdp.init();
    	        loop();
    	    }, "EmuThread");

    	    emuThread.setDaemon(true);
    	    emuThread.start();
    }
    
    public void loop() {
        try {
            while (running.get()) {
                // Z80 (se você já tiver implementado)
//                if (runZ80 && z80 != null) {
//                    int opcode = z80.readMemory(z80.PC);
//                    z80.PC = (z80.PC + 1) & 0xFFFF;
//                    z80.executeInstruction(opcode);
//                }

                // Execução da CPU 68000 (principal)
                if (!cpu.stop) {
                    int cycles = cpu.runInstruction();
                    
                    // Checagem de interrupções do barramento (como GenApp faz)
                    checkInterrupts();

                    // Avança o VDP de acordo com os ciclos da CPU
                    vdp.run(13);

                    // DMA do VDP (pode rodar mais de uma vez por ciclo)
                    vdp.dmaFill();
                    vdp.dmaFill();
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    /** Checa e dispara interrupções (VBlank/HBlank) */
    public void checkInterrupts() {
    	if (vdp.ie0) { // vint on
			if (vdp.vip == 1) { // level 6 interrupt
				vintPending = true;
			}
		}

		if (vdp.ie1) {
//			int intLine = hLinesPassed;
//			if (intLine != 0 && vdp.line == hLinesPassed) {
//				hintPending = true;
//				hLinesPassed = vdp.registers[0xA];
//			}
		}

		int mask = cpu.getInterruptMask();

		if (vintPending && mask < 0x6) {
			cpu.stop = false;

			long oldPC = cpu.PC;
			int oldSR = cpu.SR;
			long ssp = cpu.SSP;

			ssp--;
			memory.write(ssp, oldPC & 0xFF, Size.BYTE);
			ssp--;
			memory.write(ssp, (oldPC >> 8) & 0xFF, Size.BYTE);
			ssp--;
			memory.write(ssp, (oldPC >> 16) & 0xFF, Size.BYTE);
			ssp--;
			memory.write(ssp, (oldPC >> 24), Size.BYTE);

			ssp--;
			memory.write(ssp, oldSR & 0xFF, Size.BYTE);
			ssp--;
			memory.write(ssp, (oldSR >> 8) & 0xFF, Size.BYTE);

			long address = readInterruptVector(0x78);
			cpu.PC = address;
			cpu.SR = (cpu.SR & 0xF8FF) | 0x0600;

			cpu.SR |= 0x2000; // force supervisor mode

			cpu.setALong(7, ssp);

			vdp.vip = 0;

			vintPending = false;

			return;
		}

		if (hintPending && vdp.ie1 && mask < 0x4) {
			cpu.stop = false;

			long oldPC = cpu.PC;
			int oldSR = cpu.SR;
			long ssp = cpu.SSP;

			System.out.println("HINT ! Line: " + Integer.toHexString(vdp.line));

			ssp--;
			memory.write(ssp, oldPC & 0xFF, Size.BYTE);
			ssp--;
			memory.write(ssp, (oldPC >> 8) & 0xFF, Size.BYTE);
			ssp--;
			memory.write(ssp, (oldPC >> 16) & 0xFF, Size.BYTE);
			ssp--;
			memory.write(ssp, (oldPC >> 24), Size.BYTE);

			ssp--;
			memory.write(ssp, oldSR & 0xFF, Size.BYTE);
			ssp--;
			memory.write(ssp, (oldSR >> 8) & 0xFF, Size.BYTE);

			long address = readInterruptVector(0x70);
			cpu.PC = address;
			cpu.SR = (cpu.SR & 0xF8FF) | 0x0400;

			cpu.SR |= 0x2000; // force supervisor mode

			cpu.setALong(7, ssp);

			hintPending = false;
		}
    }
    
    public long readInterruptVector(long vector) {
		long address = memory.readCartridgeWord(vector) << 16;
		address |= memory.readCartridgeWord(vector + 2);
		return address;
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
