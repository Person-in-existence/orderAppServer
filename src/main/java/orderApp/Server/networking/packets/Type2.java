package orderApp.Server.networking.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import orderApp.Server.networking.Network;
import orderApp.Server.networking.SessionData;

public class Type2 extends Packet {
    private final Header header;
    private final SessionData data;
    public Type2(Header header, DataInputStream in) throws IOException {
        this.header = header;

        // Number of items
        short numItems = in.readShort();

        // Create an array to store them in
        SessionData.SessionItem[] items = new SessionData.SessionItem[numItems];
        for (int index = 0; index < numItems; index++) {
            // Item name
            String name = Network.readString(in);

            // Item quantity
            int quantity = in.readInt();
            // Make the item at position
            items[index] = new SessionData.SessionItem(name, quantity);
        }
        this.data = new SessionData(items);
    }
    public Type2(Header header, SessionData data) {
        this.header = header;
        this.data = data;
    }

    public SessionData getData() {
        return data;
    }

    @Override
    public Header getHeader() {
        return header;
    }

    @Override
    protected void sendBody(DataOutputStream out) throws IOException {
        // short number of items
        short numItems = (short) data.items.length;
        out.writeShort(numItems);

        for (SessionData.SessionItem item : data.items) {
            // String item name
            String name = item.name;
            Network.writeString(name, out);

            // int item quantity
            out.writeInt(item.quantity);
        }

    }
}
