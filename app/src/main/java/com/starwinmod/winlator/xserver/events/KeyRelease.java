package com.starwinmod.winlator.xserver.events;

import com.starwinmod.winlator.xserver.Bitmask;
import com.starwinmod.winlator.xserver.Window;

public class KeyRelease extends InputDeviceEvent {
    public KeyRelease(byte keycode, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state) {
        super(3, keycode, root, event, child, rootX, rootY, eventX, eventY, state);
    }
}
