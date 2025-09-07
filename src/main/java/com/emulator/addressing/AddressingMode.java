package com.emulator.addressing;

import com.emulator.Size;
import com.emulator.instruction.Operation;

public interface AddressingMode {

	void setByte(Operation o);
	void setWord(Operation o);
	void setLong(Operation o);
	
	long getByte(Operation o);
	long getWord(Operation o);
	long getLong(Operation o);
	
	void calculateAddress(Operation o, Size size);
	
}
