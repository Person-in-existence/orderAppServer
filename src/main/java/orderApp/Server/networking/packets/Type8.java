package orderApp.Server.networking.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import orderApp.Server.networking.Network;

public class Type8 extends Packet {
    private final Header header;
    private final short deviceType;
    private final String name;
    public Type8(Header header, DataInputStream in) throws IOException {
        this.header = header;
        this.deviceType = in.readShort();
        this.name = Network.readString(in);
    }
    public Type8(Header header, short deviceType, String name) {
        this.header = header;
        this.deviceType = deviceType;
        this.name = name;
    }
    public short getDeviceType() {
        return deviceType;
    }
    public String getName() {
        return name;
    }
    @Override
    public Header getHeader() {
        return header;
    }
    @Override
    protected void sendBody(DataOutputStream out) throws IOException {
        out.writeShort(deviceType);
        Network.writeString(name, out);
    }
}
