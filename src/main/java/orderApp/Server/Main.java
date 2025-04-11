package orderApp.Server;

import orderApp.Server.networking.Network;
import orderApp.Server.networking.Order;
import orderApp.Server.networking.OrderData;
import orderApp.Server.networking.SessionData;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Objects;

public class Main {
    public static final String red = "\u001B[31m";
    public static final String clear = "\u001B[0m";
    public static final short DEVICE_TYPE = 1; // 1 for server
    public static final String commandPrompt = "Enter command -> ";
    private static ArrayList<Integer> quantities = new ArrayList<>();
    private static ArrayList<String> names = new ArrayList<>();
    public static OrderData orders = new OrderData(new ArrayList<>());
    public static boolean running = true;
    public static boolean debug = false;
    public static LineReader reader;
    public static String deviceName = "No name set";

    public static void main(String[] args) throws IOException {
        reader = initialiseConsole();
        // Initialise names and quantities
        for (int index = 0; index < 8; index++) {
            quantities.add(0);
            names.add("");
        }

        while (running) {
            String command = reader.readLine(commandPrompt);
            CommandRegistry.handleCommand(command);
        }
        System.exit(0);


    }
    protected synchronized static void end(String[] ignored) {
        System.out.println("Ending session");
        Network.endSession();
    }
    protected synchronized static void exit(String[] ignored) {
        end(null);
        running = false;
    }
    protected synchronized static void start(String[] ignored) {
        System.out.println("Starting session...");
        Network.startSession();
    }
    protected synchronized static void debug(String[] args) {
        if (Objects.equals(args[0], "on")) {
            debug = true;
        } else if (Objects.equals(args[0], "off")) {
            debug = false;
        } else {
            System.err.println("Expected either \"on\" or \"off\"");
        }
    }
    private synchronized static void showData() {
        assert quantities.size() == names.size();
        for (int index = 0; index < quantities.size(); index++) {
            System.out.printf("\t%-18s: %d%n", names.get(index), quantities.get(index));
        }
    }
    private synchronized static void setData(String[] args) {
        if (args.length > 9) {
            System.err.println("Aborting: currently requires no more than 8 items");
            return;
        }
        ArrayList<String> newNames = new ArrayList<>();
        ArrayList<Integer> newQuantities = new ArrayList<>();
        try {
            for (int index = 1; index < args.length; index++) {
                String arg = args[index];
                String[] pieces = arg.split(",");
                String name = pieces[0];
                int quantity = Integer.parseInt(pieces[1]);
                newNames.add(name);
                newQuantities.add(quantity);
            }
            if (newNames.size() != 8) {
                int difference = 8 - newNames.size();
                for (int index = 0; index < difference; index++) {
                    newNames.add("");
                    newQuantities.add(0);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
        names = newNames;
        quantities = newQuantities;
        showData();
    }
    protected synchronized static void data(String[] args) {
        try {
            if (Objects.equals(args[0], "get")) {
                showData();
            } else if (Objects.equals(args[0], "set")) {
                setData(args);
            }
        } catch (IndexOutOfBoundsException e) {
            reader.printAbove("Takes at least 1 argument but got none: get|set");
        }
    }
    protected synchronized static void setName(String[] args) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < args.length; index++) {
            builder.append(args[index]);
            if (index < args.length-1) {
                builder.append(" ");
            }
        }
        deviceName = builder.toString();
    }
    public static LineReader initialiseConsole() throws IOException{
        Terminal terminal = TerminalBuilder.builder().dumb(false).build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
        // Override System.out to use JLine's printAbove(), so we don't interfere with user input
        System.setOut(new PrintStream(System.out) {
            @Override
            public void println(String x) {
                if (debug) {
                    reader.printAbove(x); // Redirects System.out.println()
                }
            }

            @Override
            public void println(Object x) {
                if (debug) {
                    reader.printAbove(String.valueOf(x));
                }
            }
        });

        System.setErr(new PrintStream(System.err) {
            @Override
            public void println(String x) {
                reader.printAbove(red+x+clear);
            }
            @Override
            public void println(Object x) {
                reader.printAbove(red + x+clear);
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
        for (Order.OrderItem item: order.items) {
            quantities.set(item.itemID, quantities.get(item.itemID)- item.quantity);
        }
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