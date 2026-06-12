package com.starwinmod.winlator.xserver;

public interface XLock extends AutoCloseable {
    @Override
    void close();
}
