package br.com.emulator.java.addressing;

import br.com.emulator.java.CPU68000;
import br.com.emulator.java.Size;
import br.com.emulator.java.instruction.Operation;

public class AddressRegisterIndirect implements AddressingMode {

	private CPU68000 cpu;
	
	public AddressRegisterIndirect(CPU68000 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		long addr = o.getAddress();
		long data = o.getData();

		cpu.bus.write(addr, (data & 0xFF), Size.BYTE);
	}

	@Override
	public void setWord(Operation o) {
		long addr = o.getAddress();
		long data = o.getData();

		cpu.bus.write(addr, (data & 0xFFFF), Size.WORD);
	}

	@Override
	public void setLong(Operation o) {
		long addr = o.getAddress();
		long data = o.getData();

		cpu.bus.write(addr, data, Size.LONG);
	}

	@Override
	public long getByte(Operation o) {
		long addr = o.getAddress();
		long data = cpu.bus.read(addr, Size.BYTE);
		
		return data;
	}

	@Override
	public long getWord(Operation o) {
		long addr = o.getAddress();
		long data = cpu.bus.read(addr, Size.WORD);
			 
		return data;
	}

	@Override
	public long getLong(Operation o) {
		long addr = o.getAddress();
		long data = cpu.bus.read(addr, Size.LONG);
			 
		return data;
	}

	@Override
	public void calculateAddress(Operation o, Size size) {
		long addr = cpu.getALong(o.getRegister());
		
		o.setAddress(addr);
	}

}
