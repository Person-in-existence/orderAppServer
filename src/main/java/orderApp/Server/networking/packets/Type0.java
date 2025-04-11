package orderApp.Server.networking.packets;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Type0 extends Packet {
    public static final short type = 0;
    private final Header header;
    public Type0(Header header) {
        // Check that header's type is correct (will show error message)

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
