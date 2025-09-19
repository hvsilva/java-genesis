package com.emulator.instruction;

import com.emulator.CPU68000;
import com.emulator.GenInstruction;
import com.emulator.Size;

//NAME
//ORI to SR -- Logical OR immediated to the status register (PRIVILEGED)
//
//SYNOPSIS
//ORI	#<data>,SR
//
//Size = (Word)
//
//FUNCTION
//Performs an OR operation on the status register with
//the source operand, and leaves the result in the status
//register.
//
//FORMAT
//-----------------------------------------------------------------
//|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 1 | 1 | 1 | 1 | 0 | 0 |
//|---|---|---|---|---|---|---|---|-------------------------------|
//|                     16 BITS IMMEDIATE DATA                    |
//-----------------------------------------------------------------
//
//RESULT
//X - Set if bit 4 of the source is set, cleared otherwise.
//N - Set if bit 3 of the source is set, cleared otherwise.
//Z - Set if bit 2 of the source is set, cleared otherwise.
//V - Set if bit 1 of the source is set, cleared otherwise.
//C - Set if bit 0 of the source is set, cleared otherwise.
public class ORI_SR implements GenInstructionHandler {

	final CPU68000 cpu;
	
	public ORI_SR(CPU68000 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void generate() {
		int opcode = 0x007C;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				ORISR(opcode);
			}
		};
		
		cpu.addInstruction(opcode, ins);
	}
	
	private void ORISR(int opcode) {
		long toOr = cpu.memory.read(cpu.PC + 2, Size.WORD);
		
	 	cpu.PC += 2;
		
	 	int oldSR = cpu.SR;
	 	
		long res = cpu.SR | toOr;
		cpu.SR = (int) res;
		
		if (((oldSR & 0x2000) ^ (res & 0x2000)) != 0) {	//	si cambio el supervisor bit
			if ((res & 0x2000) == 0x2000) {
				cpu.setALong(7, cpu.SSP);
			} else {
				cpu.setALong(7, cpu.USP);
			}	
		}
	}
	
}
