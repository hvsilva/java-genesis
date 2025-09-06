package br.com.emulator.java.instruction;

import br.com.emulator.java.addressing.AddressingMode;

public class Operation {

	AddressingMode addressingMode;
	long address;
	long data;
	int register;

	public AddressingMode getAddressingMode() {
		return addressingMode;
	}

	public void setAddressingMode(AddressingMode addressingMode) {
		this.addressingMode = addressingMode;
	}

	public long getAddress() {
		return address;
	}

	public void setAddress(long address) {
		this.address = address;
	}

	public long getData() {
		return data;
	}

	public void setData(long data) {
		this.data = data;
	}

	public int getRegister() {
		return register;
	}

	public void setRegister(int register) {
		this.register = register;
	}
	
}
