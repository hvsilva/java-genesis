package com.emulator.addressing;

import com.emulator.CPU68000;
import com.emulator.Size;
import com.emulator.instruction.Operation;

public class PCWithDisplacement implements AddressingMode {

	private CPU68000 cpu;
	
	public PCWithDisplacement(CPU68000 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		throw new RuntimeException();
	}

	@Override
	public void setWord(Operation o) {
		throw new RuntimeException();
	}

	@Override
	public void setLong(Operation o) {
		throw new RuntimeException();
	}

	@Override
	public long getByte(Operation o) {
		long address = o.getAddress();
		long data = cpu.memory.read(address, Size.BYTE);
		
		return data;
	}

	@Override
	public long getWord(Operation o) {
		long address = o.getAddress();
		long data = cpu.memory.read(address, Size.WORD);
		
		return data;
	}

	@Override
	public long getLong(Operation o) {
		long address = o.getAddress();
		long data = cpu.memory.read(address, Size.LONG);
		
		return data;
	}

	@Override
	public void calculateAddress(Operation o, Size size) {
		long displacement = cpu.memory.read(cpu.PC + 2, Size.WORD);
		long addr;
		if ((displacement & 0x8000) > 0) {
			displacement = -displacement;
			displacement &= 0xFFFF;
			addr = cpu.PC + 2 - displacement;
		} else {
			addr = cpu.PC + 2 + displacement;
		}
		o.setAddress(addr);
		
		cpu.PC += 2;
	}

}
