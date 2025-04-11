package orderApp.Server;

import orderApp.Server.networking.Order;
import orderApp.Server.networking.OrderData;
import orderApp.Server.networking.SessionData;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class Main {
    public static short DEVICE_TYPE = 1; // 1 for server
    private static ArrayList<Integer> quantities = new ArrayList<>();
    private static ArrayList<String> names = new ArrayList<>();
    public static OrderData orders;
    public static void main(String[] args) throws IOException {
        LineReader reader = initialiseConsole();


    }
    public static LineReader initialiseConsole() throws IOException{
        Terminal terminal = TerminalBuilder.builder().dumb(false).build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
        // Override System.out to use JLine's printAbove(), so we don't interfere with user input

        // Redirect System.out to use JLine's printAbove()
        System.setOut(new PrintStream(System.out) {
            @Override
            public void println(String x) {
                reader.printAbove(x); // Redirects System.out.println()
            }

            @Override
            public void println(Object x) {
                reader.printAbove(String.valueOf(x)); // Handles objects too
            }
        });

        System.setErr(new PrintStream(System.err) {
            @Override
            public void println(String x) {
                reader.printAbove(x);
            }
            @Override
            public void println(Object x) {
                reader.printAbove(String.valueOf(x)); // Handles objects too
            }
        });
        return reader;
    }
    public synchronized static int makeChecksum() {
        int total = 0;
        for (int index = 0; index < quantities.size(); index++) {
            total += (int) (Math.pow(7, index) * quantities.get(index));
        }
        return total;
    }
    public synchronized static void addOrder(Order order) {
        orders.orders.add(order);
    }
    public synchronized static void setOrderData(OrderData orderData) {
        orders = orderData;
    }
    public synchronized static void removeOrderByID(long orderID) {
        orders.orders.removeIf(order -> order.orderID == orderID);
    }
    public synchronized static SessionData getSessionData() {
        assert quantities.size() == names.size();
        SessionData.SessionItem[] data = new SessionData.SessionItem[names.size()];
        for (int index = 0; index < names.size(); index++) {
            data[index] = new SessionData.SessionItem(names.get(index), quantities.get(index));
        }
        return new SessionData(data);
    }
    public synchronized static void setSessionData(SessionData sessionData) {
        ArrayList<String> newNames = new ArrayList<>();
        ArrayList<Integer> newQuantities = new ArrayList<>();
        for (SessionData.SessionItem item: sessionData.items) {
            newNames.add(item.name);
            newQuantities.add(item.quantity);
        }
        names = newNames;
        quantities = newQuantities;
    }
    public static synchronized boolean isOrderWithID(long orderID) {
        for (Order order: orders.orders) {
            if (order.orderID == orderID) {
                return true;
            }
        }
        return false;
    }
}