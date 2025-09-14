package com.emulator;

import java.util.ArrayList;
import java.util.List;

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
	private final List<Sprite> sprites = new ArrayList<>();

	private Runnable frameReadyCallback;

	// Total de linhas do quadro (262 para NTSC, 312 para PAL)
	private final int totalLines = 262;

	/**
	 * Simula execução do VDP por um certo número de ciclos.
	 */
	public void run(int cycles) {
		totalCycles += cycles;

		System.out.printf("[LINE=%d (reg1=%s)]%n", line, registers[1]);

		if (totalCycles < 800) {
			hblank = false;
		} else if (totalCycles >= 800 && totalCycles <= 982) {
			hblank = true;
		}

		while (totalCycles > 982) {
			// Renderiza apenas se o display estiver habilitado e a linha for visível
			if ((registers[1] & 0x40) != 0 && line < 0xE0) {
				spritesLine = 0;
				renderBackground(line);
				renderPlaneA(line);
				renderPlaneB(line);
				renderWindow(line);
				renderSprites(line);
			}

			line++;
			totalCycles -= 982;
			hblank = false;

			// Início do VBlank
			if (line == 0xE0) {
				vblank = true;
				vip = 1;
				spritesFrame = 0;
				// Se o display estiver ativo, dispara o callback
				if ((registers[1] & 0x40) != 0) {
					if (frameReadyCallback != null) {
						frameReadyCallback.run();
					}
				}
			}

			// Reinicia a contagem de linhas e avalia os sprites para o próximo quadro
			if (line >= totalLines) {
				line = 0;
				evaluateSprites();
				vblank = false; // Fim do VBlank
			}
		}
	}

	public void setFrameReadyCallback(Runnable cb) {
		this.frameReadyCallback = cb;
	}

	// Primeiro render: só pinta a tela com a cor 0 da CRAM
	public void renderFrame() {
		int baseColor = cramToArgb(cram[0] & 0xFFFF); // cor de fundo
		for (int i = 0; i < framebuffer.length; i++) {
			framebuffer[i] = baseColor;
		}
	}

	// Converte entrada da CRAM (9-bit BGR) para 32-bit ARGB
	private int cramToArgb(int cramValue) {
		int r = ((cramValue >> 1) & 0x07) * 36;
		int g = ((cramValue >> 5) & 0x07) * 36;
		int b = ((cramValue >> 9) & 0x07) * 36;
		return (0xFF << 24) | (r << 16) | (g << 8) | b;
	}

	/**
	 * Junta todas as camadas em um único framebuffer (um frame inteiro).
	 */
//    private void compaginateImage() {
//        // limpa com a cor de fundo
//        int baseColor = cramToArgb(cram[0] & 0xFFFF);
//        for (int i = 0; i < framebuffer.length; i++) {
//            framebuffer[i] = baseColor;
//        }
//
//        // Renderiza camada por camada
//        for (int line = 0; line < HEIGHT; line++) {
//            renderBackground(line);
//            renderPlaneB(line);
//            renderPlaneA(line);
//            renderWindow(line);
//            renderSprites(line);
//        }
//    }

	/**
	 * Renderiza o plano de fundo (background) para a linha 'y'.
	 */
	private void renderBackground(int y) {
		// Pega a cor de fundo (border color) do CRAM, definida pelo registrador 0.
		int colorIndex = registers[0] & 0x3F;
		// Chama o método com o índice da cor e a paleta 0
		int backgroundColor = decodeColorARGB(colorIndex, 0);
		for (int x = 0; x < WIDTH; x++) {
			framebuffer[y * WIDTH + x] = backgroundColor;
		}
	}

	/**
	 * Renderiza o plano A para a linha 'y', sobrepondo o plano B. A lógica é
	 * similar ao plano B, mas com endereços e prioridades diferentes.
	 */
	private void renderPlaneA(int y) {
		// Endereço do 'Name Table' (mapa de tiles) do plano A
		int ntAddress = (registers[3] & 0x38) << 10;

		int tileY = y / 8;
		int tileLine = y % 8;

		for (int x = 0; x < WIDTH; x++) {
			int tileX = x / 8;
			int pixelX = x % 8;

			int tileIndex = (tileY * 64 + tileX) * 2;
			int tileInfo = vram[ntAddress + tileIndex] | (vram[ntAddress + tileIndex + 1] << 8);

			int tileNumber = tileInfo & 0x7FF;
			int palette = (tileInfo >> 13) & 0x7;
			boolean hFlip = ((tileInfo >> 11) & 1) == 1;

			if (hFlip) {
				pixelX = 7 - pixelX;
			}

			int tileDataAddress = tileNumber * 32 + (tileLine * 4) + (pixelX / 2);
			int tileData = vram[tileDataAddress];

			int colorIndex;
			if (pixelX % 2 == 0) {
				colorIndex = (tileData >> 4) & 0xF;
			} else {
				colorIndex = tileData & 0xF;
			}

			if (colorIndex != 0) {
				framebuffer[y * WIDTH + x] = decodeColorARGB(colorIndex, palette);
			}
		}
	}

	/**
	 * Renderiza o plano B para a linha 'y'. A lógica para desenhar os tiles a
	 * partir da VRAM precisa ser implementada aqui.
	 */
	private void renderPlaneB(int y) {
		// Endereço do 'Name Table' (mapa de tiles) do plano B
		int ntAddress = (registers[2] & 0x38) << 10;

		// As coordenadas Y do tile na tela
		int tileY = y / 8;

		// Deslocamento da linha do tile, dentro do tile
		int tileLine = y % 8;

		for (int x = 0; x < WIDTH; x++) {
			// As coordenadas X do tile na tela
			int tileX = x / 8;
			int pixelX = x % 8;

			// Pega o índice do tile e os atributos
			int tileIndex = (tileY * 64 + tileX) * 2; // O mapa tem 64 colunas
			int tileInfo = vram[ntAddress + tileIndex] | (vram[ntAddress + tileIndex + 1] << 8);

			int tileNumber = tileInfo & 0x7FF;
			int palette = (tileInfo >> 13) & 0x7;
			boolean hFlip = ((tileInfo >> 11) & 1) == 1;

			if (hFlip) {
				pixelX = 7 - pixelX;
			}

			// Pega o dado do tile na VRAM
			int tileDataAddress = tileNumber * 32 + (tileLine * 4) + (pixelX / 2);
			int tileData = vram[tileDataAddress];

			int colorIndex;
			if (pixelX % 2 == 0) {
				colorIndex = (tileData >> 4) & 0xF;
			} else {
				colorIndex = tileData & 0xF;
			}

			if (colorIndex != 0) {
				framebuffer[y * WIDTH + x] = decodeColorARGB(colorIndex, palette);
			}
		}
	}

	/**
	 * Renderiza a camada Window (se ativa) para a linha atual.
	 */
	private void renderWindow(int y) {
		// Registradores do VDP (valores dependem do mapeamento real)
		int winHPos = registers[17]; // posição X da janela
		int winVPos = registers[18]; // posição Y da janela

		if (y < winVPos)
			return; // ainda não alcançou a janela vertical

		int ntAddress = (registers[3] & 0x3C) << 10; // base da name table da window
		int tileY = (y - winVPos) / 8;
		int tileLine = (y - winVPos) % 8;

		for (int x = winHPos; x < WIDTH; x++) {
			int tileX = (x - winHPos) / 8;
			int pixelX = (x - winHPos) % 8;

			int tileIndex = (tileY * 64 + tileX) * 2;
			int tileInfo = vram[ntAddress + tileIndex] | (vram[ntAddress + tileIndex + 1] << 8);

			int tileNumber = tileInfo & 0x7FF;
			int palette = (tileInfo >> 13) & 0x7;
			boolean hFlip = ((tileInfo >> 11) & 1) == 1;

			if (hFlip) {
				pixelX = 7 - pixelX;
			}

			int tileDataAddress = tileNumber * 32 + (tileLine * 4) + (pixelX / 2);
			int tileData = vram[tileDataAddress];

			int colorIndex = (pixelX % 2 == 0) ? (tileData >> 4) & 0xF : tileData & 0xF;

			if (colorIndex != 0) {
				framebuffer[y * WIDTH + x] = decodeColorARGB(colorIndex, palette);
			}
		}
	}

	/**
	 * Renderiza os sprites para a linha 'y', sobrepondo os planos A e B. A lógica
	 * para desenhar os sprites a partir da VSRAM e VRAM precisa ser implementada
	 * aqui.
	 */
	private void renderSprites(int y) {
		List<Sprite> visible = spritesOnLine(line);

		for (Sprite s : sprites) {
			int sy = line - s.y;
			if (sy < 0 || sy >= s.height * 8)
				continue; // fora do sprite

			for (int sx = 0; sx < s.width * 8; sx++) {
				int color = s.getPixel(sx, sy, vram, cram);
				if (color != -1) {
					int px = s.x + sx;
					if (px >= 0 && px < WIDTH) {
						framebuffer[line * WIDTH + px] = color;
					}
				}
			}
		}
	}

	/**
	 * Retorna os sprites que aparecem na linha 'line'.
	 */
	private List<Sprite> spritesOnLine(int line) {
		List<Sprite> result = new ArrayList<>();
		for (Sprite s : sprites) {
			int top = s.y;
			int bottom = s.y + s.height * 8;

			if (line >= top && line < bottom) {
				result.add(s);
			}
		}
		return result;
	}

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
		sprites.clear();

		// endereço base da tabela de sprites
		int satAddress = (registers[5] & 0x7E) << 7;

		for (int i = 0; i < 80; i++) { // 80 sprites possíveis
			int entryAddr = satAddress + i * 8;

			int y = vram[entryAddr] & 0xFF;
			int size = vram[entryAddr + 1] & 0xFF;
			int tileIndex = vram[entryAddr + 2] | (vram[entryAddr + 3] << 8);
			int x = vram[entryAddr + 6] | ((vram[entryAddr + 7] & 1) << 8);

			// simplificado: assume 1x1 tile
			sprites.add(new Sprite(x, y, tileIndex, 1, 1, 0, false, false, 0));
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
