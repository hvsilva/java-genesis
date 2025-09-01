package emulator02;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Cartridge {
	
	private byte[] data;

    public Cartridge(String filePath) throws IOException {
        data = Files.readAllBytes(Path.of(filePath));
    }

    public byte[] getROMData() {
        return data;
    }

    public int getSize() {
        return data.length;
    }

}
