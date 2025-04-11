package orderApp.Server.networking.packets;

import java.io.DataOutputStream;
import java.io.IOException;

public class Type7 extends Packet {
    private final Header header;
    public Type7(Header header) {
        this.header = header;
    }
    public Header getHeader() {
        return header;
    }
    @Override
    protected void sendBody(DataOutputStream out) throws IOException {
        // No body to send
    }
}
