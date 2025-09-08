package com.emulator.instruction;

import com.emulator.CPU68000;
import com.emulator.GenInstruction;
import com.emulator.Size;

//NAME
//ABCD -- Add binary coded decimal
//
//SYNOPSIS
//ABCD	Dy,Dx
//ABCD	-(Ay),-(Ax)
//
//Size = (Byte)
//
//FUNCTION
//Adiciona o operando de origem ao operando de destino junto com
//o bit de extensão e armazena o resultado no local de destino.
//A adição é realizada usando aritmética decimal codificada em binário.
//Os operandos, que são números BCD empacotados, podem ser endereçados em
//duas maneiras diferentes:
//
//1. Registro de dados para registro de dados: Os operandos estão contidos no
//registradores de dados especificados na instrução.
//
//2. Memória para memória: Os operandos são endereçados com o pré-decremento
//modo de endereçamento usando os registradores de endereço especificados no
//instrução.
//
//Esta operação é apenas uma operação de bytes.
//
//Normalmente o bit do código de condição Z é definido via programação antes do
//início de uma operação. Isso permite testes bem-sucedidos com zero resultados
//após a conclusão das operações de precisão múltipla.
//
//FORMAT
//-----------------------------------------------------------------
//|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//|---|---|---|---|-----------|---|---|---|---|---|---|-----------|
//| 1 | 1 | 0 | 0 |    Rx     | 1 | 0 | 0 | 0 | 0 |R/M|    Ry     |
//-----------------------------------------------------------------
//
//R/M = 0 -> data register
//R/M = 1 -> address register
//Rx:   destination register
//Ry:   source register
//
//RESULT
//X - Set the same as the carry bit.
//N - Undefined
//Z - Cleared if the result is non-zero. Unchanged otherwise.
//V - Undefined
//C - Set if a decimal carry was generated. Cleared otherwise.
public class ABCD implements GenInstructionHandler {
	
	final CPU68000 cpu;
	
	public ABCD(CPU68000 cpu) {
		this.cpu = cpu;
	}

	@Override
	public void generate() {
		generateDataOperation();
		generateAddressOperation();		
	}
	
	
	private void generateDataOperation() {
		int base = 0xC100;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				ABCDDataByte(opcode);
			}

//			@Override
//			public int getCycles(int opcode) {
//				 return 6; // ABCD Dx,Dy: 6 ciclos
//			}
		};
				
		for (int rx = 0; rx < 8; rx++) {
			for (int ry = 0; ry < 8; ry++) {
				int opcode = base | (rx << 9) | ry;
				cpu.addInstruction(opcode, ins);
			}
		}
	}
	
	private void generateAddressOperation() {
		int base = 0xC108;
		GenInstruction ins = null;

		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				ABCDAddressByte(opcode);
			}

//			@Override
//			public int getCycles(int opcode) {
//				 return 18; // ABCD -(Ax),-(Ay): 18 ciclos
//			}
		};
			
		for (int rx = 0; rx < 8; rx++) {
			for (int ry = 0; ry < 8; ry++) {
				int opcode = base | (rx << 9) | ry;
				cpu.addInstruction(opcode, ins);
			}
		}
	}
	
	private void ABCDDataByte(int opcode) {
		int rx = (opcode >> 9) & 0x7;
		int ry = (opcode & 0x7);
		
		long data = cpu.getDByte(ry);
		long toAdd = cpu.getDByte(rx);
		
		long tot = doCalc(data, toAdd);
		cpu.setDByte(rx, tot);
	}
	
	private void ABCDAddressByte(int opcode) {
		int rx = (opcode >> 9) & 0x7;
		int ry = (opcode & 0x7);
		
		long source = cpu.getALong(ry);
		long dest = cpu.getALong(rx);
		
		source--;
		dest--;
		
		cpu.setALong(ry, source);
		cpu.setALong(rx, dest);
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, 0b010, ry);	//	address indirect
		long data = o.getAddressingMode().getByte(o);
		
		Operation o2 = cpu.resolveAddressingMode(Size.BYTE, 0b010, rx);	//	address indirect
		long toAdd = o2.getAddressingMode().getByte(o2);
		
		long tot = doCalc(data, toAdd);
		cpu.writeKnownAddressingMode(o2, tot, Size.BYTE);
	}
	
	protected final long doCalc(long data, long toAdd) {
		int x = (cpu.isX() ? 1 : 0);
		int c;

		long lo = (data & 0x0F) + (toAdd & 0x0F) + x;
		if (lo > 9) {
			lo -= 10;
			c = 1;
		} else {
			c = 0;
		}

		long hi = ((data >> 4) & 0x0F) + ((toAdd >> 4) & 0x0F) + c;
		if (hi > 9) {
			hi -= 10;
			c = 1;
		} else {
			c = 0;
		}

		long result = (hi << 4) + lo;

		if (c != 0) {
			cpu.setC();
			cpu.setX();
		} else {
			cpu.clearC();
			cpu.clearX();
		}

		if (result != 0) {
			cpu.clearZ();
		}

		return result;
	}

}
