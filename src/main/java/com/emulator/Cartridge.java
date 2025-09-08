package com.emulator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import br.com.emulator.java.FileLoader;

public class Cartridge {
	
	private byte[] data;
	
	int[] cartridge;

    public Cartridge(String filePath) throws IOException {
        data = Files.readAllBytes(Path.of(filePath));
    }
    
//    public Cartridge(File file) throws IOException {        
//    	cartridge =  FileLoader.readFile(file);
//    }
	
    public byte[] getROMData() {
        return data;
    }  

    public int getSize() {
        return data.length;
    }
   
//    public int[] getROMData() {
//        return cartridge;
//    }
    
    
//    public int getSize() {
//        return cartridge.length;
//    }

}
