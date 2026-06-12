package com.starwinmod.winlator.xconnector;

import java.io.IOException;

public interface XStreamLock extends AutoCloseable {
    void close() throws IOException;
}
