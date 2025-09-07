package com.emulator.addressing;

import com.emulator.CPU68000;
import com.emulator.Size;
import com.emulator.instruction.Operation;

public class AbsoluteShort implements AddressingMode {

	private CPU68000 cpu;
	
	public AbsoluteShort(CPU68000 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		long addr = o.getAddress();
		long data = o.getData();
		cpu.memory.write(addr, data, Size.BYTE);
	}

	@Override
	public void setWord(Operation o) {
		long addr = o.getAddress();
		long data = o.getData();
		cpu.memory.write(addr, data, Size.WORD);
	}

	@Override
	public void setLong(Operation o) {
		long addr = o.getAddress();
		long data = o.getData();
		cpu.memory.write(addr, data, Size.LONG);
	}
	
	@Override
	public long getByte(Operation o) {
		long addr = o.getAddress();
		long data = cpu.memory.read(addr, Size.BYTE);
		
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
		long addr = cpu.memory.read(cpu.PC + 2, Size.WORD);
		if ((addr & 0x8000) > 0) {
			addr |= 0xFFFF_0000L;
		}
		o.setAddress(addr);
		
		cpu.PC += 2;
	}

}
