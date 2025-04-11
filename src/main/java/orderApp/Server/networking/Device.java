package orderApp.Server.networking;

import java.net.InetAddress;

public class Device {
    public final String name;
    public final InetAddress ip;
    public final short deviceType;
    public final short version;
    public Device(String name, InetAddress ip, short deviceType, short version) {
        this.name = name;
        this.ip = ip;
        this.deviceType = deviceType;
        this.version = version;
    }
}
