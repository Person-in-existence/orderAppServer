package orderApp.Server.networking.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import orderApp.Server.networking.Order;
import orderApp.Server.networking.Network;

public class Type1 extends Packet {
    public static final short type = 1;
    private final Header header;
    private final Order order;

    /**
     * Reads in a type 1 packet from the Input Stream provided. This creates an Order object in the packet. Note that this object is created
     * with Network.getNewOrderID() as the order ID, as it is a new order from client to server and therefore does not have an order ID already.
     * @param header The header of the packet. This should have been pre-read, given that a type 1 packet was made, so we get it in here
     * @param in The data input stream to read the data from.
     * @throws IOException If an IOException occurs during reading.
     */
    public Type1(Header header, DataInputStream in) throws IOException {
        // Read the header
        this.header = header;

        // Read the order in
        // Customer name
        String customerName = Network.readString(in);

        // Number of non-0 items
        short numItems = in.readShort();
        ArrayList<Order.OrderItem> items = new ArrayList<>();

        // For every non-0 item:
        for (int index = 0; index < numItems; index++) {
            // Item ID
            short itemID = in.readShort();
            // Quantity
            int quantity = in.readInt();
            Order.OrderItem item = new Order.OrderItem(itemID, quantity);
            items.add(item);
        }

        // Create a new orderID here
        this.order = new Order(items, customerName, Network.getNewOrderID());
    }
    public Order getOrder() {
        return order;
    }
    @Override
    public void sendBody(DataOutputStream out) throws IOException {
        // Send the customer name
        String customerName = order.customerName;
        Network.writeString(customerName, out);

        // Send the number of non-0 items
        short numItems = (short) order.items.size();
        out.writeShort(numItems);

        for (Order.OrderItem item : order.items) {
            // send ItemID
            out.writeShort(item.itemID);
            // Send item quantity
            out.writeInt(item.quantity);
        }
    }
    public Type1(Order order, Header header) {
        this.header = header;
        this.order = order;
    }
    public Header getHeader() {
        return header;
    }
}
