package testes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class RomGenerator {

	 public static void main(String[] args) {
	        try {
	            // Arquivo de saída no diretório atual do projeto
	            File file = new File("D:\\PROJETOS_EMULADOR\\java-genesis\\roms\\rom_test.bin");

	            // Programa didático: MOVE, ADD, SUB, JMP
	            int[] program = {
	                0x1000, 0x0032, // MOVE #50 -> D0
	                0x2000, 0x000A, // ADD #10 -> D0
	                0x3000, 0x0005, // SUB #5  -> D0
	                0x4000, 0x0000  // JMP 0x0000
	            };

	            try (FileOutputStream fos = new FileOutputStream(file)) {
	                for (int word : program) {
	                    // 68k é big-endian → salva primeiro o byte alto
	                    fos.write((word >> 8) & 0xFF);
	                    fos.write(word & 0xFF);
	                }
	            }

	            System.out.println("ROM gerada com sucesso!");
	            System.out.println("Local do arquivo: " + file.getAbsolutePath());

	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
}