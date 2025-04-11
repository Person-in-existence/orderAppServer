package orderApp.Server.networking.packets;

import java.io.DataOutputStream;

public class Type4 extends Packet {
    private final Header header;
    public Type4(Header header) {
        this.header = header;
    }
    @Override
    public Header getHeader() {
        return header;
    }
    protected void sendBody(DataOutputStream out) {
        // Empty body
    }
}
