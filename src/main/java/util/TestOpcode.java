package util;

public class TestOpcode {

	public static void main(String[] args) {
		int opcode = 0x4E75;
		
		System.out.println("Opcode " + Integer.toHexString(opcode) + " = " + OpcodeDecoder.decode(opcode));		
		
//	    System.out.println("Opcode " + Integer.toHexString(opcode) + " = " + M68kOpcodeQuickDecoder.decode(opcode));
	}

}
