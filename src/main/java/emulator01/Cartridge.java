package emulator01;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

//Loader de ROM
public class Cartridge {
	private byte[] romData;

	public Cartridge(String path) {
		try {
			romData = Files.readAllBytes(Paths.get(path));
			System.out.println("ROM carregada: " + romData.length + " bytes");
		} catch (IOException e) {
			throw new RuntimeException("Erro ao carregar ROM: " + path, e);
		}
	}

	public byte read(int address) {
		return romData[address];
	}

	public int getSize() {
		return romData.length;
	}
}
