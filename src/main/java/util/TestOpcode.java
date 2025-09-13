package util;

public class TestOpcode {

	public static void main(String[] args) {
		int opcode = 0x42B8;
		
		System.out.println("Opcode " + Integer.toHexString(opcode) + " = " + OpcodeDecoder.decode(opcode));		
		
//	    System.out.println("Opcode " + Integer.toHexString(opcode) + " = " + M68kOpcodeQuickDecoder.decode(opcode));
	}

}
