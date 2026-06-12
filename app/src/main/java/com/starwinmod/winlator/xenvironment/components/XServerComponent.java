package com.starwinmod.winlator.xenvironment.components;

import com.starwinmod.winlator.xenvironment.EnvironmentComponent;
import com.starwinmod.winlator.xconnector.XConnectorEpoll;
import com.starwinmod.winlator.xconnector.UnixSocketConfig;
import com.starwinmod.winlator.xserver.XClientConnectionHandler;
import com.starwinmod.winlator.xserver.XClientRequestHandler;
import com.starwinmod.winlator.xserver.XServer;

public class XServerComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    private final XServer xServer;
    private final UnixSocketConfig socketConfig;

    public XServerComponent(XServer xServer, UnixSocketConfig socketConfig) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        if (connector != null) return;
        connector = new XConnectorEpoll(socketConfig, new XClientConnectionHandler(xServer), new XClientRequestHandler());
        connector.setInitialInputBufferCapacity(262144);
        connector.setCanReceiveAncillaryMessages(true);
        connector.start();
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.stop();
            connector = null;
        }
    }

    public XServer getXServer() {
        return xServer;
    }
}
