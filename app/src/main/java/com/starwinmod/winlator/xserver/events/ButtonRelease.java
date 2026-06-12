package com.starwinmod.winlator.xserver.events;

import com.starwinmod.winlator.xserver.Bitmask;
import com.starwinmod.winlator.xserver.Window;

public class ButtonRelease extends InputDeviceEvent {
    public ButtonRelease(byte detail, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state) {
        super(5, detail, root, event, child, rootX, rootY, eventX, eventY, state);
    }
}
