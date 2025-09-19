package com.emulator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Cartridge {

	private final int[] rom;

	public Cartridge(String filePath) throws IOException {
		byte[] data = Files.readAllBytes(Path.of(filePath));
		rom = new int[data.length];
		for (int i = 0; i < data.length; i++) {
			rom[i] = data[i] & 0xFF; // garante valores 0–255
		}
	}

	public int[] getROMData() {
		return rom;
	}

	public int getSize() {
		return rom.length;
	}
}
