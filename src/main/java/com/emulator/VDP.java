package com.emulator;

public class VDP {
	public static final int WIDTH = 320;
	public static final int HEIGHT = 224;

	enum VramMode {
		vramRead, cramRead, vsramRead, vramWrite, cramWrite, vsramWrite;
	}

	enum DmaMode {
		MEM_TO_VRAM, VRAM_FILL, VRAM_COPY;
	}

	VramMode vramMode;

	DmaMode dmaModo;

	// Memória de vídeo
	int[] vram = new int[0x10000];
	int[] cram = new int[0x80]; // A CRAM contém 128 bytes, endereços 0 a 7F
	int[] vsram = new int[0x50]; // A VSRAM contém 80 bytes, endereços 0 a 4F

	// FIFO Buffer 4 levels
	int[] fifoCode = new int[4];
	int[] fifoAddress = new int[4];
	int[] fifoData = new int[4];

	public int[] registers = new int[24];

	// Framebuffer (ARGB32)
	int[] framebuffer = new int[WIDTH * HEIGHT];

	// Controle de ciclos/linha/quadro
	private int totalCycles = 0;

	// Flags
	boolean vblank = false;
	boolean hblank = false;

	boolean addressSecondWrite = false;
	boolean vramWrite2 = false;
	boolean cramWrite2 = false;
	boolean vsramWrite2 = false;

	// Reg 0
	// Vertical Scroll Inhibit
	boolean vsi;
	// Horizontal Scroll Inhibit
	boolean hsi;
	// Left Column Blank
	boolean lcb;
	// Enable HINT
	boolean ie1;
	// Sprite Shift (mode 4) / HSync Mode (mode 5)
	boolean ssHsm;
	// Palette Select
	boolean ps;
	// HV Counter Latch
	boolean m2;
	// External Sync
	boolean es;
	// REG 1
	// Extended VRAM
	boolean evram;
	// Enable Display
	boolean disp;
	// Enable VINT
	boolean ie0;
	// Enable DMA
	boolean m1;
	// Enable V30 Mode
	boolean m3;
	// Enable Mode 5 (si esta inactivo, es mode = 4, compatibilidad con SMS)
	boolean m5;
	// Sprite Size
	boolean sz;
	// Sprite Zoom
	boolean mag;

	boolean dmaRecien = false;

	boolean vramFill = false;
	boolean memToVram = false;

	// REG 0xF
	int autoIncrementData;

	// reg 0x13
	int dmaLengthCounterLo;

	// reg 0x14
	int dmaLengthCounterHi;

	// reg 0x15
	int dmaSourceAddressLow;

	// reg 0x16
	int dmaSourceAddressMid;

	// reg 0x17
	int dmaSourceAddressHi;
	int dmaMode;

	long firstWrite;

	int dataPort;
	int addressPort;
	int autoIncrementTotal;

	int nextFIFOReadEntry;
	int nextFIFOWriteEntry;

// Status register:
//	15	14	13	12	11	10	9		8			7	6		5		4	3	2	1	0
//	0	0	1	1	0	1	EMPTY	FULL		VIP	SOVR	SCOL	ODD	VB	HB	DMA	PAL

//	EMPTY and FULL indicate the status of the FIFO.
//	When EMPTY is set, the FIFO is empty.
//	When FULL is set, the FIFO is full.
//	If the FIFO has items but is not full, both EMPTY and FULL will be clear.
//	The FIFO can hold 4 16-bit words for the VDP to process. If the M68K attempts to write another word once the FIFO has become full, it will be frozen until the first word can be delivered.
	int empty = 1;
	int full = 0;

//	VIP indicates that a vertical interrupt has occurred, approximately at line $E0. It seems to be cleared at the end of the frame.
	int vip;

//	SOVR is set when there are too many sprites on the current scanline. The 17th sprite in 32 cell mode and the 21st sprite on one scanline in 40 cell mode will cause this.
	int sovr;
//	SCOL is set when any sprites have non-transparent pixels overlapping. This is cleared when the Control Port is read.
	int scol;

//	ODD is set if the VDP is currently showing an odd-numbered frame while Interlaced Mode is enabled.
	int odd;
//	VB returns the real-time status of the V-Blank signal. It is presumably set on line $E0 and unset at $FF.
	int vb;
//	HB returns the real-time status of the H-Blank signal.
	int hb;

//	Direct Memory Access
//	DMA is set for the duration of a DMA operation. This is only useful for fills and copies, since the M68K is frozen during M68K to VRAM transfers.
	int dma;
//	PAL seems to be set when the system's display is PAL, and possibly reflects the state of having 240 line display enabled. The same information can be obtained from the version register.
	int pal;

	int spritesLine = 0;

	int spritesFrame = 0;

	long all;

	int line;

	// Aqui declaramos a lista de sprites
//	private final List<Sprite> sprites = new ArrayList<>();

	private Runnable frameReadyCallback;

	// Total de linhas do quadro (262 para NTSC, 312 para PAL)
	private final int totalLines = 262;
	
	int[][][] colorsCache = new int[8][8][8];
	
	public int[][] planeA = new int[320][256];
	public int[][] planeB = new int[320][256];
	public int[][] planeBack = new int[320][256];
	
	public boolean[][] planePrioA = new boolean[320][256];
	public boolean[][] planePrioB = new boolean[320][256];
	
	public int[][] planeIndexColorA = new int[320][256];
	public int[][] planeIndexColorB = new int[320][256];
	
	public int[][] window = new int[320][256];
	public int[][] windowIndex = new int[320][256];
	public boolean[][] windowPrio = new boolean[320][256];
	
	public int[][] sprites = new int[320][256];
	public int[][] spritesIndex = new int[320][256];
	public boolean[][] spritesPrio = new boolean[320][256];
	
	public int[][] screenData = new int[320][256];
	
	int[][] spritesPerLine = new int[256][80];
	int[] lastIndexes = new int[256];
	
	Emulator bus;
	
	public VDP(Emulator bus) {
		this.bus = bus;
	}

	/**
	 * Simula execução do VDP por um certo número de ciclos.
	 */
	public void run(int cycles) {
		totalCycles += cycles;
		
		System.out.printf("[LINE=%d (reg1=%s)]%n", line, registers[1]);

		if (totalCycles < 800) {
			hb = 0;
		} else if (totalCycles >= 800 && totalCycles <= 982) {
			hb = 1;
		} else if (totalCycles > 982) {
			if ((registers[1] & 0x40) == 0x40) {
				if (line < 0xE0) {
					spritesLine = 0;
					renderBack();     // cor fixa
					renderPlaneA();   // fundo
					renderPlaneB();   // cenário principal
					renderWindow();   // substitui parte do Plane A
					renderSprites();  // objetos/personagens
				}
			}
			if (line < 0xE0) {
				bus.hLinesPassed--;
				if (bus.hLinesPassed == -1) {
					bus.hintPending = true;
					bus.hLinesPassed = registers[0xA];
				}
			}
			line++;
			totalCycles = 0;
		}
		if (line > 0xFF) {
			line = 0;
			evaluateSprites();

			bus.hLinesPassed = registers[0xA];
		}
		if (line == 0xE0 && totalCycles == 0) {
			vip = 1;
			vb = 1;
			spritesFrame = 0;
			if ((registers[1] & 0x40) == 0x40) {
				compaginateImage();
				
				// depois que a tela está composta, envia para o Video
				bus.emu.renderScreen();
			}
		} else if (line < 0xE0 && ((registers[1] & 0x40) == 0x40)) {   // somente em 0 se o display estiver ligado (desligado está sempre em 1)													
			vb = 0;
		}
	}

	public void setFrameReadyCallback(Runnable cb) {
		this.frameReadyCallback = cb;
	}

	// ==============================
    // Camadas de Renderização
    // ==============================
    /** Renderiza cor de fundo / backdrop */
	private void renderBack() {
		int line = this.line;

		int regC = registers[0xC];
		boolean rs0 = bitTest(regC, 7);
		boolean rs1 = bitTest(regC, 0);

		int limitHorTiles;
		if (rs0 && rs1) {
			limitHorTiles = 40;
		} else {
			limitHorTiles = 32;
		}

		int backLine = (registers[7] >> 4) & 0x3;
		int backEntry = (registers[7]) & 0xF;
		int backIndex = (backLine * 32) + (backEntry * 2);
		int backColor = cram[backIndex] << 8 | cram[backIndex + 1];

		int r = (backColor >> 1) & 0x7;
		int g = (backColor >> 5) & 0x7;
		int b = (backColor >> 9) & 0x7;

		backColor = getColour(r, g, b);

		for (int pixel = 0; pixel < (limitHorTiles * 8); pixel++) {
			if (!disp) {
				planeBack[pixel][line] = 0;
			} else {
				planeBack[pixel][line] = backColor;
			}
		}
	}

	private void renderPlaneA() {
		int nameTableLocation = registers[2] & 0x38; // bit 6 para modo extendido de vram, no lo emulo
		nameTableLocation *= 0x400;

		int tileLocator = nameTableLocation;

		int line = this.line;

		int reg10 = registers[0x10];
		int horScrollSize = reg10 & 3;
		int verScrollSize = (reg10 >> 4) & 3;

		int horPixelsSize = 0;
		if (horScrollSize == 0) {
			horPixelsSize = 32;
		} else if (horScrollSize == 1) {
			horPixelsSize = 64;
		} else {
			horPixelsSize = 128;
		}

		int regC = registers[0xC];
		boolean rs0 = bitTest(regC, 7);
		boolean rs1 = bitTest(regC, 0);

		int limitHorTiles;
		if (rs0 && rs1) {
			limitHorTiles = 40;
		} else {
			limitHorTiles = 32;
		}

		int regD = registers[0xD];
		int hScrollBase = regD & 0x3F; // bit 6 = mode 128k
		hScrollBase *= 0x400;

		int regB = registers[0xB];
		int HS = regB & 0x3;
		int VS = (regB >> 2) & 0x1;

		int vertTileScreen = (line / 8);
		int scrollMap = 0;
		int scrollDataVer = 0;
		if (VS == 0) { // full screen scrolling
			scrollDataVer = vsram[0] << 8;
			scrollDataVer |= vsram[1];

			if (verScrollSize == 0) { // 32 tiles (0x20)
				scrollMap = (scrollDataVer + line) & 0xFF; // 32 * 8 lineas = 0x100
				if (horScrollSize == 0) {
					tileLocator += ((scrollMap / 8) * (0x40));
				} else if (horScrollSize == 1) {
					tileLocator += ((scrollMap / 8) * (0x80));
				} else {
					tileLocator += ((scrollMap / 8) * (0x100));
				}

			} else if (verScrollSize == 1) { // 64 tiles (0x40)
				scrollMap = (scrollDataVer + line) & 0x1FF; // 64 * 8 lineas = 0x200
				tileLocator += ((scrollMap / 8) * 0x80);

			} else {
				scrollMap = (scrollDataVer + line) & 0x3FF; // 128 * 8 lineas = 0x400
				tileLocator += ((scrollMap / 8) * 0x100);
			}

		} else { // 16 columns (2 tiles) scrolling
			System.out.println();
		}

		long scrollDataHor = 0;
		long scrollTile = 0;
		if (HS == 0b00) { // entire screen is scrolled at once by one longword in the horizontal scroll
							// table
			scrollDataHor = vram[hScrollBase] << 8;
			scrollDataHor |= vram[hScrollBase + 1];

			if (horScrollSize == 0) { // 32 tiles
				scrollDataHor &= 0xFF;
				if (scrollDataHor != 0) {

					scrollDataHor = 0x100 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}

			} else if (horScrollSize == 1) { // 64 tiles
				scrollDataHor &= 0x1FF;
				if (scrollDataHor != 0) {

					scrollDataHor = 0x200 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}
			} else {
				scrollDataHor &= 0xFFF; // 128 tiles

				if (scrollDataHor != 0) {
					scrollDataHor = 0x1000 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}
			}

		} else if (HS == 0b10) { // long scrolls 8 pixels
			int scrollLine = hScrollBase + ((line / 8) * 32); // 32 bytes por 8 scanlines

			scrollDataHor = vram[scrollLine] << 8;
			scrollDataHor |= vram[scrollLine + 1];

			if (scrollDataHor != 0) {
				if (horScrollSize == 0) { // 32 tiles
					scrollDataHor &= 0xFF;

					scrollDataHor = 0x100 - scrollDataHor;
					scrollTile = scrollDataHor / 8;

				} else if (horScrollSize == 1) { // 64 tiles
					scrollDataHor &= 0x1FF;

					scrollDataHor = 0x200 - scrollDataHor;
					scrollTile = scrollDataHor / 8;

				} else { // 128 tiles
					scrollDataHor &= 0x3FF;

					scrollDataHor = 0x400 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}
			}

		} else if (HS == 0b11) { // scroll one scanline
			int scrollLine = hScrollBase + ((line) * 4); // 4 bytes por 1 scanline

			scrollDataHor = vram[scrollLine] << 8;
			scrollDataHor |= vram[scrollLine + 1];

			if (horScrollSize == 0) { // 32 tiles
				scrollDataHor &= 0xFF;

				if (scrollDataHor != 0) {
					scrollDataHor = 0x100 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}

			} else if (horScrollSize == 1) { // 64 tiles
				scrollDataHor &= 0x1FF;

				if (scrollDataHor != 0) {
					scrollDataHor = 0x200 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}

			} else {
				scrollDataHor &= 0x3FF;

				if (scrollDataHor != 0) {
					scrollDataHor = 0x400 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}
			}

		}

		int loc = tileLocator;
		for (int pixel = 0; pixel < (limitHorTiles * 8); pixel++) {
			loc = (int) (((pixel + scrollDataHor)) % (horPixelsSize * 8)) / 8;

			int vertOffset = 0;
			if (VS == 1) {
				int scrollLine = (pixel / 16) * 4; // 32 bytes por 8 scanlines

				scrollDataVer = vsram[scrollLine] << 8;
				scrollDataVer |= vsram[scrollLine + 1];

				if (verScrollSize == 0) { // 32 tiles (0x20)
					scrollMap = (scrollDataVer + line) & 0xFF; // 32 * 8 lineas = 0x100
					if (horScrollSize == 0) {
						vertOffset += ((scrollMap / 8) * (0x40));
					} else if (horScrollSize == 1) {
						vertOffset += ((scrollMap / 8) * (0x80));
					} else {
						vertOffset += ((scrollMap / 8) * (0x100));
					}

				} else if (verScrollSize == 1) { // 64 tiles (0x40)
					scrollMap = (scrollDataVer + line) & 0x1FF; // 64 * 8 lineas = 0x200
					vertOffset += ((scrollMap / 8) * 0x80);

				} else {
					scrollMap = (scrollDataVer + line) & 0x3FF; // 128 * 8 lineas = 0x400
					vertOffset += ((scrollMap / 8) * 0x100);
				}
			}

			loc = tileLocator + (loc * 2);
			loc += vertOffset;

			int nameTable = vram[loc] << 8;
			nameTable |= vram[loc + 1];

//			An entry in a name table is 16 bits, and works as follows:
//			15			14 13	12				11		   			10 9 8 7 6 5 4 3 2 1 0
//			Priority	Palette	Vertical Flip	Horizontal Flip		Tile Index
			int tileIndex = (nameTable & 0x07FF); // cada tile ocupa 32 bytes

			boolean horFlip = bitTest(nameTable, 11);
			boolean vertFlip = bitTest(nameTable, 12);
			int paletteLineIndex = (nameTable >> 13) & 0x3;
			boolean priority = bitTest(nameTable, 15);

			int paletteLine = paletteLineIndex * 32; // 16 colores por linea, 2 bytes por color

			tileIndex *= 0x20;

			int filas = (scrollMap % 8);

			int pointVert;
			if (vertFlip) {
				pointVert = (filas - 7) * -1;
			} else {
				pointVert = filas;
			}

			int pixelInTile = (int) ((pixel + scrollDataHor) % 8);

			int point = pixelInTile;
			if (horFlip) {
				point = (pixelInTile - 7) * -1;
			}

			if (!disp) {
				planeA[pixel][line] = 0;
				planePrioA[pixel][line] = false;
				planeIndexColorA[pixel][line] = 0;
			} else {
				point /= 2;

				int grab = (tileIndex + point) + (pointVert * 4);
				int data = vram[grab];

				int pixel1;
				if ((pixelInTile % 2) == 0) {
					if (horFlip) {
						pixel1 = data & 0x0F;
					} else {
						pixel1 = (data & 0xF0) >> 4;
					}
				} else {
					if (horFlip) {
						pixel1 = (data & 0xF0) >> 4;
					} else {
						pixel1 = data & 0x0F;
					}
				}

				int colorIndex1 = paletteLine + (pixel1 * 2);

				int color1 = cram[colorIndex1] << 8 | cram[colorIndex1 + 1];

				int r = (color1 >> 1) & 0x7;
				int g = (color1 >> 5) & 0x7;
				int b = (color1 >> 9) & 0x7;

				int theColor1 = getColour(r, g, b);

				planeA[pixel][line] = theColor1;
				planePrioA[pixel][line] = priority;
				planeIndexColorA[pixel][line] = pixel1;
			}
		}
	}

	private void renderPlaneB() {
		int nameTableLocation = (registers[4] & 0x7) << 3; // bit 3 para modo extendido de vram, no lo emulo
		nameTableLocation *= 0x400;

		int tileLocator = nameTableLocation;

		int reg10 = registers[0x10];
		int horScrollSize = reg10 & 3;
		int verScrollSize = (reg10 >> 4) & 3;

		int horPixelsSize = 0;
		if (horScrollSize == 0) {
			horPixelsSize = 32;
		} else if (horScrollSize == 1) {
			horPixelsSize = 64;
		} else {
			horPixelsSize = 128;
		}

		int regC = registers[0xC];
		boolean rs0 = bitTest(regC, 7);
		boolean rs1 = bitTest(regC, 0);

		int limitHorTiles;
		if (rs0 && rs1) {
			limitHorTiles = 40;
		} else {
			limitHorTiles = 32;
		}

		int line = this.line;

		int regD = registers[0xD];
		int hScrollBase = regD & 0x3F; // bit 6 = mode 128k
		hScrollBase *= 0x400;

		int regB = registers[0xB];
		int HS = regB & 0x3;
		int VS = (regB >> 2) & 0x1;

		int vertTileScreen = (line / 8);
		int scrollMap = 0;
		int scrollDataVer = 0;
		if (VS == 0) { // full screen scrolling
			scrollDataVer = vsram[2] << 8;
			scrollDataVer |= vsram[3];

			if (verScrollSize == 0) { // 32 tiles (0x20)
				scrollMap = (scrollDataVer + line) & 0xFF; // 32 * 8 lineas = 0x100
				if (horScrollSize == 0) {
					tileLocator += ((scrollMap / 8) * (0x40));
				} else if (horScrollSize == 1) {
					tileLocator += ((scrollMap / 8) * (0x80));
				} else {
					tileLocator += ((scrollMap / 8) * (0x100));
				}

			} else if (verScrollSize == 1) { // 64 tiles (0x40)
				scrollMap = (scrollDataVer + line) & 0x1FF; // 64 * 8 lineas = 0x200
				tileLocator += ((scrollMap / 8) * 0x80);

			} else {
				scrollMap = (scrollDataVer + line) & 0x3FF; // 128 * 8 lineas = 0x400
				tileLocator += ((scrollMap / 8) * 0x100);
			}

		} else { // 16 columns (2 tiles) scrolling
//			throw new RuntimeException();
			System.out.println("16 vert scroll");
		}

		long scrollDataHor = 0;
		long scrollTile = 0;
		if (HS == 0b00) { // entire screen is scrolled at once by one longword in the horizontal scroll
							// table
			scrollDataHor = vram[hScrollBase + 2] << 8;
			scrollDataHor |= vram[hScrollBase + 3];

			if (horScrollSize == 0) { // 32 tiles
				scrollDataHor &= 0xFF;
				if (scrollDataHor != 0) {

					scrollDataHor = 0x100 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}

			} else if (horScrollSize == 1) { // 64 tiles
				scrollDataHor &= 0x1FF;
				if (scrollDataHor != 0) {

					scrollDataHor = 0x200 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}
			} else {
				scrollDataHor &= 0xFFF; // 128 tiles

				if (scrollDataHor != 0) {
					scrollDataHor = 0x1000 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}
			}

		} else if (HS == 0b10) { // long scrolls 8 pixels
			int scrollLine = hScrollBase + ((line / 8) * 32); // 32 bytes por 8 scanlines

			scrollDataHor = vram[scrollLine + 2] << 8;
			scrollDataHor |= vram[scrollLine + 3];

			if (scrollDataHor != 0) {
				if (horScrollSize == 0) { // 32 tiles
					scrollDataHor &= 0xFF;

					scrollDataHor = 0x100 - scrollDataHor;
					scrollTile = scrollDataHor / 8;

				} else if (horScrollSize == 1) { // 64 tiles
					scrollDataHor &= 0x1FF;

					scrollDataHor = 0x200 - scrollDataHor;
					scrollTile = scrollDataHor / 8;

				} else { // 128 tiles
					scrollDataHor &= 0x3FF;

					scrollDataHor = 0x400 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}
			}

		} else if (HS == 0b11) { // scroll one scanline
			int scrollLine = hScrollBase + ((line) * 4); // 4 bytes por 1 scanline

			scrollDataHor = vram[scrollLine + 2] << 8;
			scrollDataHor |= vram[scrollLine + 3];

			if (horScrollSize == 0) { // 32 tiles
				scrollDataHor &= 0xFF;

				if (scrollDataHor != 0) {
					scrollDataHor = 0x100 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}

			} else if (horScrollSize == 1) { // 64 tiles
				scrollDataHor &= 0x1FF;

				if (scrollDataHor != 0) {
					scrollDataHor = 0x200 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}

			} else {
				scrollDataHor &= 0x3FF;

				if (scrollDataHor != 0) {
					scrollDataHor = 0x400 - scrollDataHor;
					scrollTile = scrollDataHor / 8;
				}
			}
		}

		int loc = tileLocator;
		for (int pixel = 0; pixel < (limitHorTiles * 8); pixel++) {
			loc = (int) (((pixel + scrollDataHor)) % (horPixelsSize * 8)) / 8;

			int vertOffset = 0;
			if (VS == 1) {
				int scrollLine = (pixel / 16) * 4; // 32 bytes por 8 scanlines

				scrollDataVer = vsram[scrollLine + 2] << 8;
				scrollDataVer |= vsram[scrollLine + 3];

				if (verScrollSize == 0) { // 32 tiles (0x20)
					scrollMap = (scrollDataVer + line) & 0xFF; // 32 * 8 lineas = 0x100
					if (horScrollSize == 0) {
						vertOffset += ((scrollMap / 8) * (0x40));
					} else if (horScrollSize == 1) {
						vertOffset += ((scrollMap / 8) * (0x80));
					} else {
						vertOffset += ((scrollMap / 8) * (0x100));
					}

				} else if (verScrollSize == 1) { // 64 tiles (0x40)
					scrollMap = (scrollDataVer + line) & 0x1FF; // 64 * 8 lineas = 0x200
					vertOffset += ((scrollMap / 8) * 0x80);

				} else {
					scrollMap = (scrollDataVer + line) & 0x3FF; // 128 * 8 lineas = 0x400
					vertOffset += ((scrollMap / 8) * 0x100);
				}
			}

			loc = tileLocator + (loc * 2);
			loc += vertOffset;

			int nameTable = vram[loc] << 8;
			nameTable |= vram[loc + 1];

//			An entry in a name table is 16 bits, and works as follows:
//			15			14 13	12				11		   			10 9 8 7 6 5 4 3 2 1 0
//			Priority	Palette	Vertical Flip	Horizontal Flip		Tile Index
			int tileIndex = (nameTable & 0x07FF); // cada tile ocupa 32 bytes

			boolean horFlip = bitTest(nameTable, 11);
			boolean vertFlip = bitTest(nameTable, 12);
			int paletteLineIndex = (nameTable >> 13) & 0x3;
			boolean priority = bitTest(nameTable, 15);

			int paletteLine = paletteLineIndex * 32; // 16 colores por linea, 2 bytes por color

			tileIndex *= 0x20;

			int filas = (scrollMap % 8);

			int pointVert;
			if (vertFlip) {
				pointVert = (filas - 7) * -1;
			} else {
				pointVert = filas;
			}

			int pixelInTile = (int) ((pixel + scrollDataHor) % 8);

			int point = pixelInTile;
			if (horFlip) {
				point = (pixelInTile - 7) * -1;
			}

			if (!disp) {
				planeB[pixel][line] = 0;
				planePrioB[pixel][line] = false;
				planeIndexColorB[pixel][line] = 0;
			} else {
				point /= 2;

				int grab = (tileIndex + point) + (pointVert * 4);
				int data = vram[grab];

				int pixel1;
				if ((pixelInTile % 2) == 0) {
					if (horFlip) {
						pixel1 = data & 0x0F;
					} else {
						pixel1 = (data & 0xF0) >> 4;
					}
				} else {
					if (horFlip) {
						pixel1 = (data & 0xF0) >> 4;
					} else {
						pixel1 = data & 0x0F;
					}
				}

				int colorIndex1 = paletteLine + (pixel1 * 2);
				int color1 = cram[colorIndex1] << 8 | cram[colorIndex1 + 1];

				int r = (color1 >> 1) & 0x7;
				int g = (color1 >> 5) & 0x7;
				int b = (color1 >> 9) & 0x7;

				int theColor1 = getColour(r, g, b);

				planeB[pixel][line] = theColor1;
				planePrioB[pixel][line] = priority;
				planeIndexColorB[pixel][line] = pixel1;
			}
		}
	}

	private void renderWindow() {
		int reg12 = registers[0x12];
		int windowVert = reg12 & 0x1F;
		boolean down = ((reg12 & 0x80) == 0x80) ? true : false;

		if (windowVert != 0) {

			int line = this.line;
			int vertTile = (line / 8);

			int vertLimit = (windowVert * 8);

			if (!down) {
				if (line >= vertLimit) {
					return;
				}
			} else {
				if (line < vertLimit) {
					return;
				}
			}

			int regC = registers[0xC];
			boolean rs0 = bitTest(regC, 7);
			boolean rs1 = bitTest(regC, 0);

			int limitHorTiles;
			int nameTableLocation;
			int tileLocator;
			if (rs0 && rs1) {
				nameTableLocation = registers[0x3] & 0x3C;  // WD11 is ignored if the display resolution is 320px wide
															// (H40), which limits the Window nametable address to
															// multiples of $1000.
				nameTableLocation *= 0x400;

				limitHorTiles = 40; // H40 mode

				tileLocator = nameTableLocation + (128 * vertTile);
			} else {
				nameTableLocation = registers[0x3] & 0x3E; // bit 6 = 128k mode
				nameTableLocation *= 0x400;

				limitHorTiles = 32; // H32 mode

				tileLocator = nameTableLocation + (64 * vertTile);
			}

			for (int horTile = 0; horTile < limitHorTiles; horTile++) {
				int loc = tileLocator;

				int nameTable = vram[loc] << 8;
				nameTable |= vram[loc + 1];

				tileLocator += 2;

//				An entry in a name table is 16 bits, and works as follows:
//				15			14 13	12				11		   			10 9 8 7 6 5 4 3 2 1 0
//				Priority	Palette	Vertical Flip	Horizontal Flip		Tile Index
				int tileIndex = (nameTable & 0x07FF); // cada tile ocupa 32 bytes

				boolean horFlip = bitTest(nameTable, 11);
				boolean vertFlip = bitTest(nameTable, 12);
				int paletteLineIndex = (nameTable >> 13) & 0x3;
				boolean priority = bitTest(nameTable, 15);

				int paletteLine = paletteLineIndex * 32; // 16 colores por linea, 2 bytes por color

				tileIndex *= 0x20;

				int filas = (line % 8);

				int pointVert;
				if (vertFlip) {
					pointVert = (filas - 7) * -1;
				} else {
					pointVert = filas;
				}
				for (int k = 0; k < 4; k++) {
					int point;
					if (horFlip) {
						point = (k - 3) * -1;
					} else {
						point = k;
					}

					int po = horTile * 8 + (k * 2);

					if (!disp) {
						window[po][line] = 0;
						window[po + 1][line] = 0;

						windowPrio[po][line] = false;
						windowPrio[po + 1][line] = false;

						windowIndex[po][line] = 0;
						windowIndex[po + 1][line] = 0;
					} else {
						int grab = (tileIndex + point) + (pointVert * 4);
						int data = vram[grab];

						int pixel1, pixel2;
						if (horFlip) {
							pixel1 = data & 0x0F;
							pixel2 = (data & 0xF0) >> 4;
						} else {
							pixel1 = (data & 0xF0) >> 4;
							pixel2 = data & 0x0F;
						}

						int colorIndex1 = paletteLine + (pixel1 * 2);
						int colorIndex2 = paletteLine + (pixel2 * 2);

						int color1 = cram[colorIndex1] << 8 | cram[colorIndex1 + 1];
						int color2 = cram[colorIndex2] << 8 | cram[colorIndex2 + 1];

						int r = (color1 >> 1) & 0x7;
						int g = (color1 >> 5) & 0x7;
						int b = (color1 >> 9) & 0x7;

						int r2 = (color2 >> 1) & 0x7;
						int g2 = (color2 >> 5) & 0x7;
						int b2 = (color2 >> 9) & 0x7;

						int theColor1 = getColour(r, g, b);
						int theColor2 = getColour(r2, g2, b2);

						window[po][line] = theColor1;
						window[po + 1][line] = theColor2;

						windowPrio[po][line] = priority;
						windowPrio[po + 1][line] = priority;

						windowIndex[po][line] = pixel1;
						windowIndex[po + 1][line] = pixel2;
					}
				}
			}
		}
	}

	private void renderSprites() {
		int spriteTableLoc = registers[0x5] & 0x7F; // AT16 is only valid if 128 KB mode is enabled, and allows for
													// rebasing the Sprite Attribute Table to the second 64 KB of VRAM.
		int spriteTable = spriteTableLoc * 0x200;

		long linkData = 0xFF;
		long verticalPos;

		int line = this.line;

		long baseAddress = spriteTable;
		int[] spritesInLine = spritesPerLine[line];
		int ind = 0;
		int currSprite = spritesInLine[0];

		int[] priors = new int[320];

		while (currSprite != -1) {
			baseAddress = spriteTable + (currSprite * 8);

			int byte0 = vram[(int) (baseAddress)];
			int byte1 = vram[(int) (baseAddress + 1)];
			int byte2 = vram[(int) (baseAddress + 2)];
			int byte3 = vram[(int) (baseAddress + 3)];
			int byte4 = vram[(int) (baseAddress + 4)];
			int byte5 = vram[(int) (baseAddress + 5)];
			int byte6 = vram[(int) (baseAddress + 6)];
			int byte7 = vram[(int) (baseAddress + 7)];

			linkData = byte3 & 0x7F;
			verticalPos = ((byte0 & 0x1) << 8) | byte1; // bit 9 interlace mode only

//			if (linkData == 0) {
//				return;
//			}

			int horSize = (byte2 >> 2) & 0x3;
			int verSize = byte2 & 0x3;

			int horSizePixels = (horSize + 1) * 8;
			int verSizePixels = (verSize + 1) * 8;

			int nextSprite = (int) ((linkData * 8) + spriteTable);
			baseAddress = nextSprite;

			int realY = (int) (verticalPos - 128);

			spritesFrame++;
			spritesLine++;
			if (spritesLine >= 20) {
				return;
			}

			int pattern = ((byte4 & 0x7) << 8) | byte5;
			int palette = (byte4 >> 5) & 0x3;

			boolean priority = ((byte4 >> 7) & 0x1) == 1 ? true : false;
			boolean verFlip = ((byte4 >> 4) & 0x1) == 1 ? true : false;
			boolean horFlip = ((byte4 >> 3) & 0x1) == 1 ? true : false;

			int horizontalPos = ((byte6 & 0x1) << 8) | byte7;
			int horOffset = horizontalPos - 128;

			int spriteLine = (int) ((line - realY) % verSizePixels);

			int pointVert;
			if (verFlip) {
				pointVert = (spriteLine - (verSizePixels - 1)) * -1;
			} else {
				pointVert = spriteLine;
			}

			for (int cellHor = 0; cellHor < (horSize + 1); cellHor++) {
				// 16 bytes por cell de 8x8
				// cada linea dentro de una cell de 8 pixeles, ocupa 4 bytes (o sea, la mitad
				// del ancho en bytes)
				int currentVerticalCell = pointVert / 8;
				int vertLining = (currentVerticalCell * 32) + ((pointVert % 8) * 4);

				int cellH = cellHor;
				if (horFlip) {
					cellH = (cellHor * -1) + horSize;
				}
				int horLining = vertLining + (cellH * ((verSize + 1) * 32));
				for (int i = 0; i < 4; i++) {
					int sliver = i;
					if (horFlip) {
						sliver = (i * -1) + 3;
					}

					int grab = (pattern * 0x20) + (horLining) + sliver;
					if (grab < 0) {
						continue; // FIXME guardar en cache de sprites yPos y otros atrib
					}
					int data = vram[grab];

					int pixel1, pixel2;
					if (horFlip) {
						pixel1 = data & 0x0F;
						pixel2 = (data & 0xF0) >> 4;
					} else {
						pixel1 = (data & 0xF0) >> 4;
						pixel2 = data & 0x0F;
					}

					int paletteLine = palette * 32;

					int colorIndex1 = paletteLine + (pixel1 * 2);
					int colorIndex2 = paletteLine + (pixel2 * 2);

					int color1;
					if (pixel1 == 0) {
						if (horOffset >= 0 && horOffset < 320) {
							if (spritesIndex[horOffset][line] == 0) { // solo pisa si la prioridad anterior era 0
								spritesIndex[horOffset][line] = pixel1;
								spritesPrio[horOffset][line] = priority;
							}
						}
					} else {
						if (horOffset >= 0 && horOffset < 320) {
							if (priors[horOffset] == 0 || (priors[horOffset] == 1 && priority)) {
								if (priority) {
									priors[horOffset] = 1;
								}

								color1 = cram[colorIndex1] << 8 | cram[colorIndex1 + 1];

								int r = (color1 >> 1) & 0x7;
								int g = (color1 >> 5) & 0x7;
								int b = (color1 >> 9) & 0x7;

								int theColor1 = getColour(r, g, b);

								sprites[horOffset][line] = theColor1;
								spritesIndex[horOffset][line] = pixel1;
								spritesPrio[horOffset][line] = priority;
							}
						}
					}

					int color2;
					int horOffset2 = horOffset + 1;
					if (pixel2 == 0) {
						if (horOffset2 >= 0 && horOffset2 < 320) {
							if (spritesIndex[horOffset2][line] == 0) { // solo pisa si la prioridad anterior era 0
								spritesIndex[horOffset2][line] = pixel2;
								spritesPrio[horOffset2][line] = priority;
							}
						}
					} else {
						if (horOffset2 >= 0 && horOffset2 < 320) {
							if (priors[horOffset2] == 0 || (priors[horOffset2] == 1 && priority)) {
								if (priority) {
									priors[horOffset2] = 1;
								}

								color2 = cram[colorIndex2] << 8 | cram[colorIndex2 + 1];

								int r2 = (color2 >> 1) & 0x7;
								int g2 = (color2 >> 5) & 0x7;
								int b2 = (color2 >> 9) & 0x7;

								int theColor2 = getColour(r2, g2, b2);

								sprites[horOffset2][line] = theColor2;
								spritesIndex[horOffset2][line] = pixel2;
								spritesPrio[horOffset2][line] = priority;
							}
						}
					}

					horOffset += 2;
				}
			}

			ind++;
			currSprite = spritesInLine[ind];
		}
	}
	
    /** Renderiza o fundo (background color) */
//    private void renderBack() {
//        for (int y = 0; y < 224; y++) {
//            for (int x = 0; x < 320; x++) {
//                screenData[x][y] = bgColor;
//            }
//        }
//    }

    /** Renderiza o plano A (tilemap) */
//    private void renderPlaneA() {
//        // TODO: buscar no VRAM a tilemap de plano A e desenhar
//        // Exemplo simplificado (faixa azul na tela):
//        for (int y = 50; y < 100; y++) {
//            for (int x = 0; x < 320; x++) {
//                screenData[x][y] = 0x0000FF;
//            }
//        }
//    }

    /** Renderiza o plano B (tilemap secundário) */
//    private void renderPlaneB() {
//        // TODO: buscar no VRAM a tilemap de plano B e desenhar
//        // Exemplo simplificado (faixa vermelha):
//        for (int y = 120; y < 160; y++) {
//            for (int x = 0; x < 320; x++) {
//                screenData[x][y] = 0xFF0000;
//            }
//        }
//    }

    /** Renderiza a janela (window) */
//    private void renderWindow() {
//        // TODO: verificar registradores para posição da janela
//        // Exemplo simplificado (quadrado verde canto superior esquerdo):
//        for (int y = 0; y < 50; y++) {
//            for (int x = 0; x < 100; x++) {
//                screenData[x][y] = 0x00FF00;
//            }
//        }
//    }

    /** Renderiza os sprites */
//    private void renderSprites() {
//        // TODO: buscar Sprite Attribute Table no VRAM e renderizar
//        // Exemplo simplificado (um pixel branco no centro da tela):
//        screenData[160][112] = 0xFFFFFF;
//    }
	
	
	public void initColorsCache() {
		for (int r = 0; r < 8; r++) {
			for (int g = 0; g < 8; g++) {
				for (int b = 0; b < 8; b++) {
					int red = r;
					if (r != 0) {
						red = ((r + 1) * 32) - 1;
					}

					int green = g;
					if (g != 0) {
						green = ((g + 1) * 32) - 1;
					}

					int blue = b;
					if (b != 0) {
						blue = ((b + 1) * 32) - 1;
					}

					int color = red << 16 | green << 8 | blue;

					colorsCache[r][g][b] = color;
				}
			}
		}
	}

	
	// The VDP has a complex system of priorities that can be used to achieve
	// several complex effects. The priority order goes like follows, with the least
	// priority being the first item in the list:
	//
	// Backdrop Colour
	// Plane B with priority bit clear
	// Plane A with priority bit clear
	// Sprites with priority bit clear
	// Window Plane with priority bit clear
	// Plane B with priority bit set
	// Plane A with priority bit set
	// Sprites with priority bit set
	// Window Plane with priority bit set
	private void compaginateImage() {
		int regC = registers[0xC];
		boolean rs0 = bitTest(regC, 7);
		boolean rs1 = bitTest(regC, 0);

		int limitHorTiles;
		if (rs0 && rs1) {
			limitHorTiles = 40;
		} else {
			limitHorTiles = 32;
		}
		// TODO 256 en modo pal
		for (int j = 0; j < 224; j++) {
			for (int i = 0; i < limitHorTiles * 8; i++) {
				int backColor = planeBack[i][j];

				boolean aPrio = planePrioA[i][j];
				boolean bPrio = planePrioB[i][j];
				boolean sPrio = spritesPrio[i][j];
				boolean wPrio = windowPrio[i][j];

				int aColor = planeIndexColorA[i][j];
				int bColor = planeIndexColorB[i][j];
				int wColor = windowIndex[i][j];
				int spriteIndex = spritesIndex[i][j];

				boolean aDraw = (aColor != 0);
				boolean bDraw = (bColor != 0);
				boolean sDraw = (spriteIndex != 0);
				boolean wDraw = (wColor != 0);

				boolean W = (wDraw && ((wPrio) // TODO comtenmplar que si dibuja W, no dibuje A en ese lugar
						|| (!wPrio && (!sDraw || (sDraw && !sPrio)) && (!aDraw || (aDraw && !aPrio))
								&& (!bDraw || (bDraw && !bPrio)))));

				int pix = 0;
				if (W) {
					pix = window[i][j];
					window[i][j] = 0;
					windowIndex[i][j] = 0;
				} else {
					boolean S = (sDraw && ((sPrio) || (!sPrio && !aPrio && !bPrio) || (!sPrio && aPrio && !aDraw)
							|| (!bDraw && bPrio && !sPrio && !aPrio)));
					if (S) {
						pix = sprites[i][j];
						sprites[i][j] = 0;
						spritesIndex[i][j] = 0;
					} else {
						boolean A = (aDraw && aPrio) || (aDraw && ((!bPrio) || (!bDraw)));
						if (A) {
							pix = planeA[i][j];
						} else if (bDraw) {
							pix = planeB[i][j];
						} else {
							pix = backColor;
						}
					}
				}
				screenData[i][j] = pix;

				window[i][j] = 0;
				windowIndex[i][j] = 0;
				sprites[i][j] = 0;
				spritesIndex[i][j] = 0;
			}
		}
	}

	/**
	 * Retorna os sprites que aparecem na linha 'line'.
	 */
//	private List<Sprite> spritesOnLine(int line) {
//		List<Sprite> result = new ArrayList<>();
//		for (Sprite s : sprites) {
//			int top = s.y;
//			int bottom = s.y + s.height * 8;
//
//			if (line >= top && line < bottom) {
//				result.add(s);
//			}
//		}
//		return result;
//	}

//    private int planePriorityAt(int x, int y) {
//        // Lê o atributo de prioridade do plano A/B na posição
//        // Retorna 0 = baixa, 1 = alta
//        int tileInfo = getTileInfoAt(x, y); 
//        return (tileInfo >> 15) & 1; // bit de prioridade
//    }

	private int getTileInfoAt(int x, int y) {
		// Exemplo simples: lê só do plano A
		int ntAddress = (registers[3] & 0x38) << 10;

		int tileX = x / 8;
		int tileY = y / 8;
		int tileIndex = (tileY * 64 + tileX) * 2;

		return vram[ntAddress + tileIndex] | (vram[ntAddress + tileIndex + 1] << 8);
	}

	/**
	 * Avalia os sprites na VRAM e prepara para o próximo frame.
	 */
	private void evaluateSprites() {
		int spriteTableLoc = registers[0x5] & 0x7F; // AT16 is only valid if 128 KB mode is enabled, and allows for
													// rebasing the Sprite Attribute Table to the second 64 KB of VRAM.
		int spriteTable = spriteTableLoc * 0x200;

		int currSprite = 0;
		for (int i = 0; i < 256; i++) {
			lastIndexes[i] = 0;
			for (int j = 0; j < 80; j++) {
				spritesPerLine[i][j] = -1;
			}
		}

		int regC = registers[0xC];
		boolean rs0 = bitTest(regC, 7);
		boolean rs1 = bitTest(regC, 0);

		int maxSprites = 64;
		if (rs0 && rs1) {
			maxSprites = 80;
		}

		for (int i = 0; i < maxSprites; i++) {
			long baseAddress = spriteTable + (i * 8);

			int byte0 = vram[(int) (baseAddress)];
			int byte1 = vram[(int) (baseAddress + 1)];
			int byte2 = vram[(int) (baseAddress + 2)];
			int byte3 = vram[(int) (baseAddress + 3)];
			int byte4 = vram[(int) (baseAddress + 4)];
			int byte5 = vram[(int) (baseAddress + 5)];
			int byte6 = vram[(int) (baseAddress + 6)];
			int byte7 = vram[(int) (baseAddress + 7)];

			int linkData = byte3 & 0x7F;

			int verticalPos = ((byte0 & 0x1) << 8) | byte1;
			int verSize = byte2 & 0x3;

			int verSizePixels = (verSize + 1) * 8;
			int realY = (int) (verticalPos - 128);
			for (int j = realY; j < realY + verSizePixels; j++) {
				if (j < 0 || j > 255) {
					continue;
				}

				int last = lastIndexes[j];
				spritesPerLine[j][last] = i;
				lastIndexes[j] = last + 1;
			}

			if (linkData == 0) {
				return;
			}
		}
	}

	/**
	 * Decodifica o índice de cor usando a paleta correta.
	 */
	private int decodeColorARGB(int colorIndex, int palette) {
		int cramAddress = (palette * 16) + colorIndex;
		int value = cram[cramAddress] & 0xFFFF;

		int r = (value & 0x000E) >> 1; // 3 bits
		int g = (value & 0x00E0) >> 5; // 3 bits
		int b = (value & 0x0E00) >> 9; // 3 bits

		r = (r * 255) / 7;
		g = (g * 255) / 7;
		b = (b * 255) / 7;

		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	void init() {
		empty = 1;
		vb = 1;

		for (int i = 0; i < cram.length; i++) {
			if (i % 2 == 0) {
				cram[i] = 0x0E;
			} else {
				cram[i] = 0xEE;
			}
		}
		for (int i = 0; i < vsram.length; i++) {
			if (i % 2 == 0) {
				vsram[i] = 0x07;
			} else {
				vsram[i] = 0xFF;
			}
		}
	}

	public void writeDataPort(int data, Size size) {
		this.dataPort = data;

		if (size == Size.BYTE) {
			if (vramFill) {
				if (vramMode == VramMode.vramWrite) {
					vramWriteByte(data);
				} else {
					System.out.println("que hace ? otros modos ?");
				}

				autoIncrementTotal = 1;

				if (m1) {
					dma = 1;
					vramFill = false;
					dataPort = (data << 8) | data;
					return;
				} else {
					System.out.println("M1 should be 1 in the DMA transfer. otherwise we can't guarantee the operation.");							
				}

			} else if (vramMode == VramMode.vramWrite) {
				vramWriteByte(data);

			} else if (vramMode == VramMode.cramWrite) {
				throw new RuntimeException("NOT IMPL !");
			} else if (vramMode == VramMode.vsramWrite) {
				throw new RuntimeException("NOT IMPL !");
			} else {
				System.out.println("Write pero mando read, Modo video: " + vramMode.toString());
//				throw new RuntimeException("NOT IMPL !");
			}

		} else if (size == Size.WORD) {
			if (vramFill) {
//				Performing a DMA fill does perform a normal VRAM write. After the VRAM write has been processed however, a DMA fill operation is triggered immediately after. Normal VRAM writes are always 16-bit, so the first write that is carried out when you try and start a DMA fill will always be 16-bit. The DMA fill operation that follows will perform 8-bit writes.
				if (vramMode == VramMode.vramWrite) {
					vramWriteWord(data);
				} else {
					System.out.println("que hace ? otros modos ?");
				}

				autoIncrementTotal = 1;

				if (m1) {
					dma = 1;
					vramFill = false;
					dataPort = data;
					return;
				} else {
					System.out.println("M1 should be 1 in the DMA transfer. otherwise we can't guarantee the operation.");							
				}

			} else if (vramMode == VramMode.vramWrite) {
				vramWriteWord(data);

			} else if (vramMode == VramMode.cramWrite) {
				cramWriteWord(data);

			} else if (vramMode == VramMode.vsramWrite) {
				vsramWriteWord(data);

			} else {
				System.out.println("Write pero mando read, Modo video: " + vramMode.toString());
//				throw new RuntimeException("NOT IMPL !");
			}

		} else { // LONG
			if (vramFill) {
				if (m1) {
					dma = 1;
					vramFill = false;
					dataPort = data;
					return;
				} else {
					System.out
							.println("M1 should be 1 in the DMA transfer. otherwise we can't guarantee the operation.");
				}

			} else if (vramMode == VramMode.vramWrite) {
				vramWriteWord(data >> 16);
				vramWriteWord(data & 0xFFFF);

			} else if (vramMode == VramMode.cramWrite) {
				cramWriteWord(data >> 16);
				cramWriteWord(data & 0xFFFF);

			} else if (vramMode == VramMode.vsramWrite) {
				vsramWriteWord(data >> 16);
				vsramWriteWord(data & 0xFFFF);

			} else {
				System.out.println("Write pero mando read, Modo video: " + vramMode.toString());
//				throw new RuntimeException("NOT IMPL !");
			}
		}
	}

// https://wiki.megadrive.org/index.php?title=VDP_Ports#Write_2_-_Setting_RAM_address
//	First word
//	Bit	15	14	13	12	11	10	9	8	7	6	5	4	3	2	1	0
//	Def	CD1-CD0	A13		-										   A0

//	Second word
//	Bit	15	14	13	12	11	10	9	8	7	6	5	4	3	2	1	0
//	Def	0	 0	 0	 0	 0	 0	0	0	CD5		- CD2	0	A15	 -A14

//	Access mode	CD5	CD4	CD3	CD2	CD1	CD0
//	VRAM Write	0	0	0	0	0	1
//	CRAM Write	0	0	0	0	1	1
//	VSRAM Write	0	0	0	1	0	1
//	VRAM Read	0	0	0	0	0	0
//	CRAM Read	0	0	1	0	0	0
//	VSRAM Read	0	0	0	1	0	0

//	DMA Mode		CD5	CD4
//	Memory to VRAM	1	0
//	VRAM Fill		1	0
//	VRAM Copy		1	1
	public void writeControlPort(long data) {
		long mode = (data >> 13);

		if (!addressSecondWrite && mode == 0b100) { // Write 1 - Setting Register
			writeRegister(data);

		} else { // Write 2 - Setting RAM address
			writeRamAddress(data);
		}
	}

	private void vramWriteByte(int data) {
		int index = nextFIFOReadEntry;
		int address = addressPort;

		long first = all >> 16;
		long second = all & 0xFFFF;

		int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
		int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 14));

		int offset = addr + autoIncrementTotal;

		data = data & 0xFF;

		// hack por si se pasa
		if (offset > 0xFFFF) {
			return;
		}

		writeVramByte(offset, data);

//		System.out.println("addr: " + Integer.toHexString(offset) + " - data: " + Integer.toHexString(data1));
//		System.out.println("addr: " + Integer.toHexString(offset + 1) + " - data: " + Integer.toHexString(data2));

		fifoAddress[index] = offset;
		fifoCode[index] = code;
		fifoData[index] = data;

		int incrementOffset = autoIncrementTotal + autoIncrementData;

		address = address + incrementOffset; // FIXME wrap
		offset = offset + incrementOffset;
		index = (index + 1) % 4;

		nextFIFOReadEntry = index;
		nextFIFOWriteEntry = index;
		autoIncrementTotal = incrementOffset;
	}

	private void vramWriteWord(int data) {
		int word = data;

		int index = nextFIFOReadEntry;
		int address = addressPort;

		long first = all >> 16;
		long second = all & 0xFFFF;

		int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
		int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 14));

		int offset = addr + autoIncrementTotal;

		int data1 = (word >> 8) & 0xFF;
		int data2 = word & 0xFF;

		// hack por si se pasa
		if (offset > 0xFFFE) {
			return;
		}

		writeVramByte(offset, data1);
		writeVramByte(offset + 1, data2);

//		System.out.println("addr: " + Integer.toHexString(offset) + " - data: " + Integer.toHexString(data1));
//		System.out.println("addr: " + Integer.toHexString(offset + 1) + " - data: " + Integer.toHexString(data2));

		fifoAddress[index] = offset;
		fifoCode[index] = code;
		fifoData[index] = word;

		int incrementOffset = autoIncrementTotal + autoIncrementData;

		address = address + incrementOffset; // FIXME wrap
		offset = offset + incrementOffset;
		index = (index + 1) % 4;

		nextFIFOReadEntry = index;
		nextFIFOWriteEntry = index;
		autoIncrementTotal = incrementOffset;
	}

//https://emu-docs.org/Genesis/sega2f.htm
//The CRAM contains 128 bytes, addresses 0 to 7FH.  For word wide writes to the CRAM, use:
// D15 ~ D0 are valid when we use word for data set. If the writes are byte
// wide, write the high byte to $C00000 and the low byte to $C00001. A long
// word wide access is equivalent to two sequential word wide accesses.
// Place the first data in D31 - D16 and the second data in D15 - D0. The
// date may be written sequentially; the address is incremented by the value
// of REGISTER #15 after every write, independent of whether the width is
// byte of word.
//Note that A0 is used in the increment but not in address decoding, resulting in some interesting side-effects if writes are attempted at odd addresses.
	private void cramWriteWord(int data) {
//			if (!cramWrite2) {
//				cramWriteData = data;
//				cramWrite2 = true;
//			} else {
//				cramWrite2 = false;
//				int word = (cramWriteData << 16) | data;

		int word = data;

		int index = nextFIFOReadEntry;
		int address = addressPort;

		long first = all >> 16;
		long second = all & 0xFFFF;

		int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
		int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 13));

		int offset = address + autoIncrementTotal;

		int data1 = (word >> 8) & 0xFF;
		int data2 = word & 0xFF;

		writeCramByte(offset, data1);
		writeCramByte(offset + 1, data2);

		fifoAddress[index] = offset;
		fifoCode[index] = code;
		fifoData[index] = (data1 << 8) | data2;

		int incrementOffset = autoIncrementTotal + autoIncrementData;

		address = address + incrementOffset; // FIXME wrap
		index = (index + 1) % 4;
		fifoAddress[index] = address;
		fifoCode[index] = code;

		nextFIFOReadEntry = (index + 1) % 4;
		nextFIFOWriteEntry = (index + 1) % 4;
		autoIncrementTotal = incrementOffset;
//			}
	}

	private void vsramWriteWord(int data) {
		int word = data;

		int index = nextFIFOReadEntry;
		int address = addressPort;

		address = address & 0xFF; // no decodifica todo, arregla scroll vertical en 16 zhang mahjong intro

		long first = all >> 16;
		long second = all & 0xFFFF;

		int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
		int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 13));

		int offset = address + autoIncrementTotal;

		int data1 = (word >> 8) & 0xFF;
		int data2 = word & 0xFF;

		if (offset < 0x50) {
			vsram[offset] = data1;
		}
		if (offset < 0x50) {
			vsram[offset + 1] = data2;
		}

		fifoAddress[index] = offset;
		fifoCode[index] = code;
		fifoData[index] = (data1 << 8) | data2;

//			int data3 = (word >> 8) & 0xFF;
//			int data4 = (word >> 0) & 0xFF;
//			
//			vsram[offset + 2] = data3;
//			vsram[offset + 3] = data4;

		int incrementOffset = autoIncrementTotal + autoIncrementData;

//			address = address + incrementOffset;	// FIXME wrap
//			index = (index + 1) % 4;
//			fifoAddress[index] = address;
//			fifoCode[index] = code;
//			fifoData[index] = (data3 << 8) | data4;

		nextFIFOReadEntry = (index + 1) % 4;
		nextFIFOWriteEntry = (index + 1) % 4;
		autoIncrementTotal = incrementOffset;
	}

	public long readDataPort(Size size) {
		if (vramMode == VramMode.vramRead) {
			long data = readVram(size);
			return data;

		} else if (vramMode == VramMode.cramRead) {
			long data = readCram(size);
			return data;

		} else if (vramMode == VramMode.vsramRead) {
			long data = readVsram(size);
			return data;

		} else {
			System.out.println("Comando de leitura, mas gravação, modo de vídeo: " + vramMode.toString());
//			throw new RuntimeException("Modo video: " + vramMode.toString());
		}
		return 0;
	}

	private void writeRegister(long data) {
		int dataControl = (int) (data & 0x00FF);
		int reg = (int) ((data >> 8) & 0x1F);

		System.out.println("[REG: " + pad(reg) + " - data: " + pad(dataControl) + "]");

		cramWrite2 = false;
		vramWrite2 = false;
		vsramWrite2 = false;

		System.out.printf("[WRITE REGISTER] : regIndx=%d registers[reg]=%04X%n", reg, registers[reg]);

		registers[reg] = dataControl;
		
		System.out.printf("[WRITE REGISTER NEW] : regIndx=%d dataDec=%d (dataHex=%04X)  registers[reg]=%04X%n", reg, dataControl, dataControl, registers[reg]);
		
		if (registers[1] == 116) {
			System.out.println("modo 5");			
		}

		if (reg == 0x00) {
			vsi = ((data >> 7) & 1) == 1;
			hsi = ((data >> 6) & 1) == 1;
			lcb = ((data >> 5) & 1) == 1;
			ie1 = ((data >> 4) & 1) == 1;
			ssHsm = ((data >> 3) & 1) == 1;
			ps = ((data >> 2) & 1) == 1;
			m2 = ((data >> 1) & 1) == 1;
			es = ((data >> 0) & 1) == 1;

		} else if (reg == 0x01) {
			if ((disp) && ((data & 0x40) == 0)) { // el display estaba prendido pero se apago
				vb = 1;
			} else if ((!disp) && ((data & 0x40) == 0x40)) { // el display se prende
				vb = 0;
			}

			evram = ((data >> 7) & 1) == 1;
			disp = ((data >> 6) & 1) == 1;
			ie0 = ((data >> 5) & 1) == 1;
			m1 = ((data >> 4) & 1) == 1;
			m3 = ((data >> 3) & 1) == 1;
			m5 = ((data >> 2) & 1) == 1;
			sz = ((data >> 1) & 1) == 1;
			mag = ((data >> 0) & 1) == 1;

		} else if (reg == 0x0F) {
			autoIncrementData = (int) (data & 0xFF);

		} else if (reg == 0x13) {
			dmaLengthCounterLo = (int) (data & 0xFF);

		} else if (reg == 0x14) {
			dmaLengthCounterHi = (int) (data & 0xFF);

		} else if (reg == 0x15) {
			dmaSourceAddressLow = (int) (data & 0xFF);

		} else if (reg == 0x16) {
			dmaSourceAddressMid = (int) (data & 0xFF);

		} else if (reg == 0x17) {
			dmaSourceAddressHi = (int) (data & 0x3F);
			dmaMode = (int) ((data >> 6) & 0x3);
		}
	}

//	 Registers 19, 20, specify how many 16-bit words to transfer:
//
//	 #19: L07 L06 L05 L04 L03 L02 L01 L00
//	 #20: L15 L14 L13 L12 L11 L10 L08 L08
//
//	 Note that a length of 7FFFh equals FFFFh bytes transferred, and a length
//	 of FFFFh = 1FFFF bytes transferred.
//
//	 Registers 21, 22, 23 specify the source address on the 68000 side:
//
//	 #21: S08 S07 S06 S05 S04 S03 S02 S01
//	 #22: S16 S15 S14 S13 S12 S11 S10 S09
//	 #23:  0  S23 S22 S21 S20 S19 S18 S17
//
//	 If the source address goes past FFFFFFh, it wraps to FF0000h.
//	 (Actually, it probably wraps at E00000h, but there's no way to tell as
//	  the two addresses are functionally equivelant)
//
//	 When doing a transfer to CRAM, the operation is aborted once the address
//	 register is larger than 7Fh. The only known game that requires this is
//	 Batman & Robin, which will have palette corruption in levels 1 and 3
//	 otherwise. This rule may possibly apply to VSRAM transfers as well.

//	 The following events occur after the command word is written:
//
//		 - 68000 is frozen.
//		 - VDP reads a word from source address.
//		 - Source address is incremented by 2.
//		 - VDP writes word to VRAM, CRAM, or VSRAM.
//		   (For VRAM, the data is byteswapped if the address register has bit 0 set)
//		 - Address register is incremented by the value in register #15.
//		 - Repeat until length counter has expired.
//		 - 68000 resumes operation.
	private void dmaMem2Vram(long commandWord) {
		int dmaLength = (dmaLengthCounterHi << 8) | dmaLengthCounterLo;

		long sourceAddr = ((registers[0x17] & 0x7F) << 16) | (registers[0x16] << 8) | (registers[0x15]);
		long sourceTrue = sourceAddr << 1; // duplica, trabaja asi
		int destAddr = (int) (((commandWord & 0x3) << 14) | ((commandWord & 0x3FFF_0000L) >> 16));

		int index, data;
		while (dmaLength > 0) {

			int dataWord = (int) read(sourceTrue, Size.WORD);
			int data1 = dataWord >> 8;
			int data2 = dataWord & 0xFF;

			if (destAddr % 2 == 1) {
				System.out.println("IMPAR !");
			}
			if (destAddr > 0xFFFF) {
				return;
			}
			if (vramMode == VramMode.vramWrite) {
				writeVramByte(destAddr, data1);
				writeVramByte(destAddr + 1, data2);

			} else if (vramMode == VramMode.cramWrite) {
				writeCramByte(destAddr, data1);
				writeCramByte(destAddr + 1, data2);

			} else if (vramMode == VramMode.vsramWrite) {
				vsram[destAddr] = data1;
				vsram[destAddr + 1] = data2;

			} else {
				throw new RuntimeException("not");
			}

			sourceTrue += 2;
			destAddr += registers[15];

			dmaLength--;
		}

		int newSource = (int) (sourceTrue >> 1);
		registers[0x17] = ((registers[0x17] & 0x80) | ((newSource >> 16) & 0x7F));
		registers[0x16] = (newSource >> 8) & 0xFF;
		registers[0x15] = newSource & 0xFF;

		dmaLengthCounterHi = 0;
		dmaLengthCounterLo = 0;
		registers[0x14] = 0;
		registers[0x13] = 0;
	}

	public int read(long address, Size size) {
		// Endereço de registrador VDP
		int reg = (int) ((address - 0xC00000) / 2);
		if (reg >= 0 && reg < registers.length) {
			int value = registers[reg];
			switch (size) {
			case BYTE:
				return value & 0xFF;
			case WORD:
				return value & 0xFFFF;
			case LONG:
				return value & 0xFFFF;
			default:
				return 0;
			}
		} else {
			System.err.printf("VDP read: endereço inválido %06X\n", address);
			return 0;
		}
	}

	public void write(long address, long data, Size size) {
		// Exemplo: VDP registers mapeados em 0xC00000 - 0xC0001F
		int reg = (int) ((address - 0xC00000) / 2); // cada registrador tem 2 bytes

		if (reg >= 0 && reg < registers.length) {
			switch (size) {
			case BYTE:
				registers[reg] = (int) (data & 0xFF);
				break;
			case WORD:
				registers[reg] = (int) (data & 0xFFFF);
				break;
			case LONG:
				registers[reg] = (int) (data & 0xFFFF); // geralmente só WORD, mas pode adaptar
				break;
			}
		} else {
			System.err.printf("VDP write: endereço inválido %06X\n", address);
		}
	}

	private void writeRamAddress(long data) {
		if (!addressSecondWrite) {
			System.out.println("first");

			firstWrite = data;
			addressSecondWrite = true;

		} else {
			addressSecondWrite = false;

			long first = firstWrite;
			long second = data;
			all = (first << 16) | second;

			int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
			int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 14));

			System.out.println("second code " + Integer.toHexString(code));

			addressPort = addr;
			autoIncrementTotal = 0; // reset este acumulador

			// reset de los flags TODO confirmar que van aca
			cramWrite2 = false;
			vramWrite2 = false;
			vsramWrite2 = false;

			int addressMode = code & 0xF; // solo el primer byte, el bit 4 y 5 son para DMA
											// que ya fue contemplado arriba
			if (addressMode == 0b0000) { // VRAM Read
				vramMode = VramMode.vramRead;

			} else if (addressMode == 0b0001) { // VRAM Write
				vramMode = VramMode.vramWrite;

			} else if (addressMode == 0b1000) { // CRAM Read
				vramMode = VramMode.cramRead;

			} else if (addressMode == 0b0011) { // CRAM Write
				vramMode = VramMode.cramWrite;

			} else if (addressMode == 0b0100) { // VSRAM Read
				vramMode = VramMode.vsramWrite;

			} else if (addressMode == 0b0101) { // VSRAM Write
				vramMode = VramMode.vsramWrite;
			}

			System.out.println("Video mode: " + vramMode.toString());

			// https://wiki.megadrive.org/index.php?title=VDP_DMA
			if ((code & 0b100000) > 0) { // DMA
				int dmaBits = code >> 4;
				dmaRecien = true;

				if ((dmaBits & 0b10) > 0) { // VRAM Fill
					if ((registers[0x17] & 0x80) == 0x80) {
//						FILL mode fills with same data from free even VRAM address.
//						FILL for only VRAM.
						dmaModo = DmaMode.VRAM_FILL;
						vramFill = true;

					} else {
						dmaModo = DmaMode.MEM_TO_VRAM;
						memToVram = true;

						if (m1) {
							dmaMem2Vram(all);
						} else {
							System.out.println("DMA but no m1 set !!");
						}
					}

				} else if ((dmaBits & 0b11) > 0) { // VRAM Copy
					dmaModo = DmaMode.VRAM_COPY;
					throw new RuntimeException();
				}
			}
		}
	}

	// Converte CRAM 9-bit (BGR) para RGB888
//	private int cramToRgb(int index) {
//	    int word = cram[index] & 0xFFFF;
//	    int r = ((word >> 1) & 0x07) * 36;
//	    int g = ((word >> 5) & 0x07) * 36;
//	    int b = ((word >> 9) & 0x07) * 36;
//	    return (0xFF << 24) | (r << 16) | (g << 8) | b; // ARGB
//	}

	public void dmaFill() {
		if (dma == 1) {
			int dmaLength = (dmaLengthCounterHi << 8) | dmaLengthCounterLo;

			int index;
			long data;

			long first = all >> 16;
			long second = all & 0xFFFF;

			int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
			int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 13));

			int destAddr = (int) (((all & 0x3) << 14) | ((all & 0x3FFF_0000L) >> 16));

			if (destAddr % 2 == 1) {
				System.out.println("IMPAR !");
			}

			destAddr += autoIncrementTotal;

			int data1 = dataPort & 0xFF;

			destAddr = destAddr & 0xFFFF; // 16 Zhang Majhong hace DMA length 0xFFFF que es el doble del limite (hace el
											// doble de operaciones)

			if (vramMode == VramMode.vramWrite) {
				writeVramByte(destAddr, data1);
			} else {
				throw new RuntimeException("SOLO ESCRIBE EN VRAM !! pasa este caso ?");
			}

			dmaLength = (dmaLength - 1); // idem FIXME no es fijo
			if (dmaLength <= 0) {
				dma = 0;
				return;
			}

			autoIncrementTotal += registers[0xF];

			dmaLength = dmaLength & 0xFFFF;
			dmaLengthCounterHi = dmaLength >> 8;
			dmaLengthCounterLo = dmaLength & 0xFF;

			registers[0x14] = dmaLength >> 8;
			registers[0x13] = dmaLength & 0xFF;
		}
	}

	private void writeVramByte(int address, int data) {
		vram[address] = data;
	}

	private void writeCramByte(int address, int data) {
		if (address > 0x7F) {
			return;
		}
		cram[address] = data;
//		System.out.println(Integer.toHexString(address) + ": " + Integer.toHexString(data));
	}

	public final String pad(int reg) {
		String s = Integer.toHexString(reg).toUpperCase();
		if (s.length() == 1) {
			s = "0" + s;
		}
		return s;
	}

	int readControl() {
//		TODO When you do a 16-bit read of the status register, the upper 6 bits are not set by the VDP. The value assigned to these bits will be whatever value these bits were set to from the last read the M68000 performed. Writes from the M68000 don't affect these bits, only reads.
		int control = ((empty << 9) | (full << 8) | (vip << 7) | (sovr << 6) | (scol << 5) | (odd << 4) | (vb << 3)
				| (hb << 2) | (dma << 1) | (pal << 0));

		return control;
	}

	private long readVram(Size size) {
		int index = nextFIFOReadEntry;
		int address = addressPort;

		long first = all >> 16;
		long second = all & 0xFFFF;

		int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
		int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 14));

		int offset = addr + autoIncrementTotal;

		long data1 = vram[offset];
		long data2 = vram[offset + 1];

		long data = ((data1 << 8) | (data2));

		System.out.println("addr: " + Integer.toHexString(offset) + "-" + Integer.toHexString(offset + 1) + ": "
				+ Integer.toHexString((int) data));

		int incrementOffset = autoIncrementTotal + autoIncrementData;
		autoIncrementTotal = incrementOffset;

		return data;

	}

	private long readCram(Size size) {
		int index = nextFIFOReadEntry;
		int address = addressPort;

		long first = all >> 16;
		long second = all & 0xFFFF;

		int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
		int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 14));

		int offset = addr + autoIncrementTotal;

		if (offset > 0x80) {
			return 0;
		}

		long data1 = cram[offset] & 0xEEE;
		long data2 = cram[offset + 1] & 0xEEE;

		long data = ((data1 << 8) | (data2));

		System.out.println("addr: " + Integer.toHexString(offset) + "-" + Integer.toHexString(offset + 1) + ": "
				+ Integer.toHexString((int) data));

		int incrementOffset = autoIncrementTotal + autoIncrementData;
		autoIncrementTotal = incrementOffset;

		return data;

	}

	private long readVsram(Size size) {
		int index = nextFIFOReadEntry;
		int address = addressPort;

		long first = all >> 16;
		long second = all & 0xFFFF;

		int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
		int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 14));

		int offset = addr + autoIncrementTotal;

		long data1 = vsram[offset] & 0xEEE;
		long data2 = vsram[offset + 1] & 0xEEE;

		long data = ((data1 << 8) | (data2));

		System.out.println("addr: " + Integer.toHexString(offset) + "-" + Integer.toHexString(offset + 1) + ": "
				+ Integer.toHexString((int) data));

		int incrementOffset = autoIncrementTotal + autoIncrementData;
		autoIncrementTotal = incrementOffset;

		return data;
	}
	
	private int getColour(int red, int green, int blue) {
		int c = colorsCache[red][green][blue];

		return c;
	}
	
	public boolean bitTest(long address, int position) {
		return ((address & (1 << position)) != 0);
	}

	/**
	 * Retorna framebuffer atual (ARGB).
	 */
	public int[] getFrameBuffer() {
		return framebuffer;
	}

	public boolean isVBlank() {
		return vblank;
	}

	public boolean isHBlank() {
		return hblank;
	}
}
