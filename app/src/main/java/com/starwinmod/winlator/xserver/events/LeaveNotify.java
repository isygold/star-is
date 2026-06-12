package com.starwinmod.winlator.xserver.events;

import com.starwinmod.winlator.xserver.Bitmask;
import com.starwinmod.winlator.xserver.Window;

public class LeaveNotify extends PointerWindowEvent {
    public LeaveNotify(Detail detail, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state, Mode mode, boolean sameScreenAndFocus) {
        super(8, detail, root, event, child, rootX, rootY, eventX, eventY, state, mode, sameScreenAndFocus);
    }
}
