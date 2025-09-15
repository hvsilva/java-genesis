package br.com.emulator.java.addressing;

import br.com.emulator.java.CPU68000;
import br.com.emulator.java.Size;
import br.com.emulator.java.instruction.Operation;

public class AddressRegisterIndirectPostIncrement implements AddressingMode {

	private CPU68000 cpu;
	
	public AddressRegisterIndirectPostIncrement(CPU68000 cpu) {
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
		
//		if (addr == 33140) {
//			System.out.println("modo 5");			
//		}
		
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
		int register = o.getRegister();
		long addr = cpu.getALong(register);
		o.setAddress(addr);
		
		if (size == Size.BYTE) {	//	byte
			if (register == 7) {	// stack pointer siempre alineado de a 2
				addr += 2;
			} else {
				addr += 1;
			}
			cpu.setALong(register, addr);
			
		} else if (size == Size.WORD) {	//	word
			addr += 2;
			cpu.setALong(register, addr);
			
		} else if (size == Size.LONG) {	//	long
			addr += 4;
			cpu.setALong(register, addr);
			
		}
	}

}
