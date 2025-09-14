package com.emulator;

public enum Size {

	BYTE(0x80, 0xFF, 1),
    WORD(0x8000, 0xFFFF, 2),
    LONG(0x8000_0000L, 0xFFFF_FFFFL, 4);

    long msb;
    long max;
    int bytes;

    Size(long msb, long maxSize, int bytes) {
        this.msb = msb;
        this.max = maxSize;
        this.bytes = bytes;
    }

    public long getMsb() {
        return this.msb;
    }

    public long getMax() {
        return this.max;
    }

    public int getBytes() {
        return this.bytes;
    }
	
}
