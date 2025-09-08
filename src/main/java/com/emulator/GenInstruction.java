package com.emulator;

public abstract class GenInstruction {
	 /**
     * Executa a instrução correspondente ao opcode.
     */
    public abstract void run(int opcode);

    /**
     * Retorna o número de ciclos consumidos por essa instrução para o opcode informado.
     * Use as tabelas oficiais do Motorola 68000 para precisão.
     */
//    public abstract int getCycles(int opcode);
}