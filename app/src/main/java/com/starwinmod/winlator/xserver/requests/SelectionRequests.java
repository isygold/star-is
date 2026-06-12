package com.starwinmod.winlator.xserver.requests;

import static com.starwinmod.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.starwinmod.winlator.xconnector.XInputStream;
import com.starwinmod.winlator.xconnector.XOutputStream;
import com.starwinmod.winlator.xconnector.XStreamLock;
import com.starwinmod.winlator.xserver.Atom;
import com.starwinmod.winlator.xserver.Window;
import com.starwinmod.winlator.xserver.XClient;
import com.starwinmod.winlator.xserver.errors.BadAtom;
import com.starwinmod.winlator.xserver.errors.BadWindow;
import com.starwinmod.winlator.xserver.errors.XRequestError;

import java.io.IOException;

public abstract class SelectionRequests {
    public static void setSelectionOwner(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        int atom = inputStream.readInt();
        int timestamp = inputStream.readInt();

        Window owner = client.xServer.windowManager.getWindow(windowId);
        if (owner == null) throw new BadWindow(windowId);
        if (!Atom.isValid(atom)) throw new BadAtom(atom);

        client.xServer.selectionManager.setSelection(atom, owner, client, timestamp);
    }

    public static void getSelectionOwner(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int atom = inputStream.readInt();
        if (!Atom.isValid(atom)) throw new BadAtom(atom);
        Window owner = client.xServer.selectionManager.getSelection(atom).owner;

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(owner != null ? owner.id : 0);
            outputStream.writePad(20);
        }
    }
}
