package orderApp.Server.networking.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Type5 extends Packet {
    private final Header header;
    private final long orderID;
    public Type5(Header header, DataInputStream in) throws IOException {
        this.header = header;
        this.orderID = in.readLong();
    }
    public Type5(Header header, long orderID) {
        this.header = header;
        this.orderID = orderID;
    }
    public long getOrderID() {
        return orderID;
    }
    @Override
    public Header getHeader() {
        return header;
    }
    @Override
    protected void sendBody(DataOutputStream out) throws IOException {
        out.writeLong(orderID);
    }
}
