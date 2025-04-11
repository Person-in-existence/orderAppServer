package orderApp.Server.networking.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Type3 extends Packet{
    private final Header header;
    private final boolean success;
    public Type3(Header header, DataInputStream in) throws IOException {
        this.header = header;
        this.success = in.readBoolean();
    }
    public Type3(Header header, boolean success) {
        this.header = header;
        this.success = success;
    }
    public boolean getSuccess() {
        return success;
    }
    public Header getHeader() {
        return header;
    }
    @Override
    protected void sendBody(DataOutputStream out) throws IOException {
        out.writeBoolean(success);
    }
}
