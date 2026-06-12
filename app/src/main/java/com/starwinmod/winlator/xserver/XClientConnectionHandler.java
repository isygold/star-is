package com.starwinmod.winlator.xserver;

import com.starwinmod.winlator.xconnector.Client;
import com.starwinmod.winlator.xconnector.ConnectionHandler;

public class XClientConnectionHandler implements ConnectionHandler {
    private final XServer xServer;

    public XClientConnectionHandler(XServer xServer) {
        this.xServer = xServer;
    }

    @Override
    public void handleNewConnection(Client client) {
        client.createIOStreams();
        client.setTag(new XClient(xServer, client.getInputStream(), client.getOutputStream()));
    }

    @Override
    public void handleConnectionShutdown(Client client) {
        ((XClient)client.getTag()).freeResources();
    }
}