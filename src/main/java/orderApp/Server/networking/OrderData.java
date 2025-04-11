package orderApp.Server.networking;

import java.util.ArrayList;

public class OrderData {
    public ArrayList<Order> orders;
    public OrderData(ArrayList<Order> orders) {
        // Copy everything so it doesn't accidentally break if this is changed.
        this.orders = new ArrayList<>();
        for (Order order: orders) {
            this.orders.add(new Order(order));
        }
    }
    public OrderData(OrderData data) {
        this.orders = new ArrayList<>();
        // Make a copy of every order, so it can't be affected by the original
        for (Order order: data.orders) {
            orders.add(new Order(order));
        }
    }
}
