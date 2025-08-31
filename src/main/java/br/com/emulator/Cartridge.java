package br.com.emulator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

//Loader de ROM
public class Cartridge {

	private byte[] romData;

	public Cartridge(String path) throws IOException {
		romData = Files.readAllBytes(Path.of(path));
	}

	public byte read(int address) {
		return romData[address];
	}

	public int getSize() {
		return romData.length;
	}

}
