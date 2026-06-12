package com.starwinmod.winlator.xserver.extensions;

import com.starwinmod.winlator.xconnector.XInputStream;
import com.starwinmod.winlator.xconnector.XOutputStream;
import com.starwinmod.winlator.xserver.XClient;
import com.starwinmod.winlator.xserver.errors.XRequestError;

import java.io.IOException;

public interface Extension {
    String getName();

    byte getMajorOpcode();

    byte getFirstErrorId();

    byte getFirstEventId();

    void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError;
}
