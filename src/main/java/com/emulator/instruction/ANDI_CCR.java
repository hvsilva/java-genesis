package com.emulator.instruction;

import com.emulator.CPU68000;
import com.emulator.GenInstruction;
import com.emulator.Size;

//NAME
//ANDI to CCR -- Logical AND immediate to condition code register
//
//SYNOPSIS
//ANDI	#<data>,CCR
//
//Size = (Byte)
//
//FUNCTION
//Performs a bit-wise AND operation with the immediate data an	d
//the lower byte of the status register.
//
//FORMAT
//-----------------------------------------------------------------
//|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//| 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 1 | 1 | 1 | 1 | 0 | 0 |
//|---|---|---|---|---|---|---|---|-------------------------------|
//| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |     8 BITS IMMEDIATE DATA     |
//-----------------------------------------------------------------
//
//RESULT
//X - Cleared if bit 4 of immed. operand is zero. Unchanged otherwise.
//N - Cleared if bit 3 of immed. operand is zero. Unchanged otherwise.
//Z - Cleared if bit 2 of immed. operand is zero. Unchanged otherwise.
//V - Cleared if bit 1 of immed. operand is zero. Unchanged otherwise.
//C - Cleared if bit 0 of immed. operand is zero. Unchanged otherwise.

public class ANDI_CCR implements GenInstructionHandler {

	final CPU68000 cpu;

	public ANDI_CCR(CPU68000 cpu) {
		this.cpu = cpu;
	}

	@Override
	public void generate() {
		int opcode = 0x023C;
		GenInstruction ins = null;

		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				ANDICCR(opcode);
			}
		};

		cpu.addInstruction(opcode, ins);
	}

	private void ANDICCR(int opcode) {
		long toAnd = cpu.memory.read(cpu.PC + 2, Size.WORD);
		toAnd &= 0xFF;

		cpu.PC += 2;

		int res = (int) ((cpu.SR & 0xFFE0) | toAnd);
		cpu.SR = res;
	}

}
