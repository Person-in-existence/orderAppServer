package orderApp.Server.networking.packets;

import java.io.DataOutputStream;

public class Type10 extends Packet {
    private final Header header;
    public Type10(Header header) {
        this.header = header;
    }
    @Override
    public Header getHeader() {
        return header;
    }
    @Override
    protected void sendBody(DataOutputStream out) {
        // Do nothing - empty body
    }
}
