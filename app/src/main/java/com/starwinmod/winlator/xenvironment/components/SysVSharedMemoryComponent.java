package com.starwinmod.winlator.xenvironment.components;

import com.starwinmod.winlator.sysvshm.SysVSHMConnectionHandler;
import com.starwinmod.winlator.sysvshm.SysVSHMRequestHandler;
import com.starwinmod.winlator.sysvshm.SysVSharedMemory;
import com.starwinmod.winlator.xconnector.UnixSocketConfig;
import com.starwinmod.winlator.xconnector.XConnectorEpoll;
import com.starwinmod.winlator.xenvironment.EnvironmentComponent;
import com.starwinmod.winlator.xserver.SHMSegmentManager;
import com.starwinmod.winlator.xserver.XServer;

public class SysVSharedMemoryComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    public final UnixSocketConfig socketConfig;
    private SysVSharedMemory sysVSharedMemory;
    private final XServer xServer;

    public SysVSharedMemoryComponent(XServer xServer, UnixSocketConfig socketConfig) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        if (connector != null) return;
        sysVSharedMemory = new SysVSharedMemory();
        connector = new XConnectorEpoll(socketConfig, new SysVSHMConnectionHandler(sysVSharedMemory), new SysVSHMRequestHandler());
        connector.start();

        xServer.setSHMSegmentManager(new SHMSegmentManager(sysVSharedMemory));
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.stop();
            connector = null;
        }

        sysVSharedMemory.deleteAll();
    }
}
