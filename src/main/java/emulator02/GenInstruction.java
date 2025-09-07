package emulator02;

@FunctionalInterface
public interface GenInstruction {
    void run(int opcode);
}