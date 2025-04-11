package orderApp.Server.networking.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import orderApp.Server.networking.Network;
import orderApp.Server.networking.Order;
import orderApp.Server.networking.OrderData;

public class Type11 extends Packet {
    private final Header header;
    private final OrderData data;
    public Type11(Header header, OrderData data) {
        this.header = header;
        this.data = new OrderData(data);
    }
    public Type11(Header header, DataInputStream in) throws IOException {
        this.header = header;

        // number of orders - int
        int numOrders = in.readInt();

        ArrayList<Order> orders = new ArrayList<>();
        for (int index = 0; index < numOrders; index++) {
            // order ID - long
            long orderID = in.readLong();

            // Customer name - string
            String customerName = Network.readString(in);

            // number of items - short
            short numItems = in.readShort();

            ArrayList<Order.OrderItem> items = new ArrayList<>();

            for (int itemIndex = 0; itemIndex < numItems; itemIndex++) {
                // Item ID - short
                short ID = in.readShort();

                // quantity - short
                int quantity = in.readInt();

                items.add(new Order.OrderItem(ID, quantity));
            }
            orders.add(new Order(items, customerName, orderID));
        }
        this.data = new OrderData(orders);
    }
    public OrderData getData() {
        return data;
    }
    @Override
    public Header getHeader() {
        return header;
    }
    @Override
    protected void sendBody(DataOutputStream out) throws IOException {
        // Number of orders - int
        int numOrders = data.orders.size();
        out.writeInt(numOrders);

        for (Order order: data.orders) {
            // orderID - long
            out.writeLong(order.orderID);

            // customer name - string
            Network.writeString(order.customerName, out);

            // number of items - short
            short numItems = (short) order.items.size();
            out.writeShort(numItems);

            for (Order.OrderItem item: order.items) {
                // Item ID - short
                out.writeShort(item.itemID);

                // Item quantity - int
                out.writeInt(item.quantity);
            }
        }
    }
}
