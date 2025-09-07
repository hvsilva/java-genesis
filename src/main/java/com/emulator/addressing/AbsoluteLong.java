package com.emulator.addressing;

import com.emulator.CPU68000;
import com.emulator.Size;
import com.emulator.instruction.Operation;

public class AbsoluteLong implements AddressingMode {

	private CPU68000 cpu;
	
	public AbsoluteLong(CPU68000 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		long address = o.getAddress();
		long data = o.getData();
		
		cpu.memory.write(address, data, Size.BYTE);
	}

	@Override
	public void setWord(Operation o) {
		long address = o.getAddress();
		long data = o.getData();
		
		cpu.memory.write(address, data, Size.WORD);
	}

	@Override
	public void setLong(Operation o) {
		long address = o.getAddress();
		long data = o.getData();
		
		cpu.memory.write(address, data, Size.LONG);
	}
	
	@Override
	public long getByte(Operation o) {
		long addr = o.getAddress();
		long data = cpu.memory.read(addr, Size.BYTE) & 0xFF;
		
		return data;
	}

	@Override
	public long getWord(Operation o) {
		long addr = o.getAddress();
		long data = cpu.memory.read(addr, Size.WORD);
		
		return data;
	}

	@Override
	public long getLong(Operation o) {
		long addr = o.getAddress();
		long data = cpu.memory.read(addr, Size.LONG);
		
		return data;
	}

	@Override
	public void calculateAddress(Operation o, Size size) {
		long addr = cpu.memory.read(cpu.PC + 2, Size.LONG);
		o.setAddress(addr);
		
		cpu.PC += 4;
	}

}
