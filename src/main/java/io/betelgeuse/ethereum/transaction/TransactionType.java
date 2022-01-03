package io.betelgeuse.ethereum.transaction;

public enum TransactionType {
    LEGACY(null),
    EIP1559(((byte) 0x02));

    Byte type;

    TransactionType(final Byte type) {
        this.type = type;
    }

    public Byte getRlpType() {
        return type;
    }
}
