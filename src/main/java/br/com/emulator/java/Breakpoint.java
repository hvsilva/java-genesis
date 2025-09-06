package br.com.emulator.java;

public class Breakpoint {

	enum Type {
		PC, VRAM, CRAM, RAM
	}

	Type type;
	int address;
	int value; // se -1, ignora valor

	Breakpoint(Type type, int address, int value) {
		this.type = type;
		this.address = address;
		this.value = value;
	}
}
