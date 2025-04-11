package orderApp.Server.networking;


import java.util.ArrayList;



public class Order {
    public ArrayList<OrderItem> items;
    public String customerName;
    public long orderID;
    public Order(ArrayList<OrderItem> items, String customerName, long orderID) {
        this.items = items;
        this.customerName = customerName;
        this.orderID = orderID;
    }
    public Order(Order order) {
        this.customerName = order.customerName;
        this.orderID = order.orderID;
        this.items = new ArrayList<>();
        this.items.addAll(order.items);

    }
    public static class OrderItem {
        public final short itemID;
        public int quantity;
        public OrderItem(short itemID, int quantity) {
            this.itemID = itemID;
            this.quantity = quantity;

        }
    }
    public String getText(String[] names) {
        String text = customerName + ": ";
        int size = items.size();
        for (int index = 0; index < size; index++) {
            OrderItem orderItem = items.get(index);
            text += (orderItem.quantity + " " + names[orderItem.itemID]);
            if (index != size-1) {
                text += ", ";
            }
        }
        return text;
    }
}
