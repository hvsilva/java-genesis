package com.emulator.addressing;

import com.emulator.CPU68000;
import com.emulator.Size;
import com.emulator.instruction.Operation;

public class AddressRegisterWithDisplacement implements AddressingMode {

	private CPU68000 cpu;
	
	public AddressRegisterWithDisplacement(CPU68000 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		long addr = o.getAddress();
		long data = o.getData();

		cpu.memory.write(addr, data & 0xFF, Size.BYTE);
	}

	@Override
	public void setWord(Operation o) {
		long addr = o.getAddress();
		long data = o.getData();

		cpu.memory.write(addr, data & 0xFFFF, Size.WORD);
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
		long base = cpu.getALong(o.getRegister());
		long displac = cpu.memory.read(cpu.PC + 2, Size.WORD);
		
		cpu.PC += 2;
		
		long displacement = (long) displac;
		if ((displacement & 0x8000) > 0) {
			displacement |= 0xFFFF_0000L;	// sign extend 32 bits
		}
		long addr = (int) (base + displacement);	// TODO verificar esto, al pasarlo a int hace el wrap bien parece
		
		o.setAddress(addr);	
	}

}
