package com.starwinmod.winlator.xenvironment.components;

import com.starwinmod.winlator.alsaserver.ALSAClientConnectionHandler;
import com.starwinmod.winlator.alsaserver.ALSARequestHandler;
import com.starwinmod.winlator.xconnector.UnixSocketConfig;
import com.starwinmod.winlator.xconnector.XConnectorEpoll;
import com.starwinmod.winlator.xenvironment.EnvironmentComponent;

public class ALSAServerComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    private final UnixSocketConfig socketConfig;

    public ALSAServerComponent(UnixSocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        if (connector != null) return;
        connector = new XConnectorEpoll(socketConfig, new ALSAClientConnectionHandler(), new ALSARequestHandler());
        connector.setMultithreadedClients(true);
        connector.start();
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.stop();
            connector = null;
        }
    }
}
