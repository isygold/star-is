package com.starwinmod.winlator.alsaserver;

import com.starwinmod.winlator.xconnector.Client;
import com.starwinmod.winlator.xconnector.ConnectionHandler;

public class ALSAClientConnectionHandler implements ConnectionHandler {
    @Override
    public void handleNewConnection(Client client) {
        client.createIOStreams();
        client.setTag(new ALSAClient());
    }

    @Override
    public void handleConnectionShutdown(Client client) {
        ((ALSAClient)client.getTag()).release();
    }
}
