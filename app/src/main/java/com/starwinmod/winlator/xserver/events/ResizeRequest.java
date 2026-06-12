package com.starwinmod.winlator.xserver.events;

import com.starwinmod.winlator.xconnector.XOutputStream;
import com.starwinmod.winlator.xconnector.XStreamLock;
import com.starwinmod.winlator.xserver.Window;

import java.io.IOException;

public class ResizeRequest extends Event {
    private final Window window;
    private final short width;
    private final short height;

    public ResizeRequest(Window window, short width, short height) {
        super(25);
        this.window = window;
        this.width = width;
        this.height = height;
    }

    @Override
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(code);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(sequenceNumber);
            outputStream.writeInt(window.id);
            outputStream.writeShort(width);
            outputStream.writeShort(height);
            outputStream.writePad(20);
        }
    }
}
