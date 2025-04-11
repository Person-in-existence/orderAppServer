package orderApp.Server.networking.packets;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import orderApp.Server.networking.Network;
import orderApp.Server.networking.Order;

public class Type6 extends Packet {
    private final Header header;
    private final Order order;
    private final int checksum;
    private final boolean isAdd;
    public Type6(Header header, DataInputStream in) throws IOException {
        this.header = header;

        long orderID = in.readLong();
        this.isAdd = in.readBoolean();
        this.checksum = in.readInt();

        if (isAdd) {
            String customerName = Network.readString(in);
            short numItems = in.readShort();
            // Use arraylist because that is what order takes
            ArrayList<Order.OrderItem> items = new ArrayList<>();
            for (int index = 0; index < numItems; index++) {
                // Item ID - short
                short itemID = in.readShort();

                // Item quantity - int
                int quantity = in.readInt();
                items.add(new Order.OrderItem(itemID, quantity));
            }
            // Make the order
            this.order = new Order(items, customerName, orderID);
        } else {
            // Make a null order, with just the ID
            this.order = new Order(null, null, orderID);
        }
    }
    public Type6(Header header, Order order, int checksum, boolean isAdd) {
        this.header = header;
        this.order = new Order(order); // Make a copy here, just in case the order changes after this is created.
        this.checksum = checksum;
        this.isAdd = isAdd;
    }
    public Order getOrder() {
        return order;
    }
    public int getChecksum() {
        return checksum;
    }
    public boolean getIsAdd() {
        return isAdd;
    }
    @Override
    public Header getHeader() {
        return header;
    }
    @Override
    protected void sendBody(DataOutputStream out) throws IOException {
        // Order ID - long
        out.writeLong(order.orderID);

        // isAdd - boolean
        out.writeBoolean(isAdd);

        // checksum - int
        out.writeInt(checksum);

        // if isAdd - the order data
        if (isAdd) {
            // customer name - string
            Network.writeString(order.customerName, out);

            // Number of items - short
            out.writeShort(order.items.size());

            for (Order.OrderItem item: order.items) {
                // Item ID - short
                out.writeShort(item.itemID);

                // Item quantity - int
                out.writeInt(item.quantity);
            }
        }
    }
}
