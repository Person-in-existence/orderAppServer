package orderApp.Server.networking.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import orderApp.Server.networking.Order;

public class Type9 extends Packet {
    private final Header header;
    private final Order order;
    private final int checksum;
    public Type9(Header header, DataInputStream in) throws IOException {
        this.header = header;

        // number of items - short
        short numItems = in.readShort();
        ArrayList<Order.OrderItem> items = new ArrayList<>();
        for (int index = 0; index < numItems; index++) {
            // Item ID - short
            short ID = in.readShort();

            // Amount - int
            int quantity = in.readInt();
            items.add(new Order.OrderItem(ID, quantity));
        }
        this.order = new Order(items, null, -1);
        // Checksum
        checksum = in.readInt();
    }
    public Type9(Header header, Order order, int checksum) {
        this.header = header;
        this.order = new Order(order); // Copy the order in case it changes
        this.checksum = checksum;
    }
    public Order getOrder() {
        return order;
    }
    public int getChecksum() {
        return checksum;
    }
    @Override
    public Header getHeader() {
        return header;
    }
    @Override
    protected void sendBody(DataOutputStream out) throws IOException {
        // Number of items - short
        out.writeShort(order.items.size());

        for (Order.OrderItem item: order.items) {
            // Item ID - short
            out.writeShort(item.itemID);

            // quantity - int
            out.writeInt(item.quantity);
        }
        // Checksum - int
        out.writeInt(checksum);
    }
}
