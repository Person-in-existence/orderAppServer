package orderApp.Server.networking;



import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;

import orderApp.Server.networking.packets.Header;
import orderApp.Server.networking.packets.Packet;
import orderApp.Server.networking.packets.Type0;
import orderApp.Server.networking.packets.Type1;
import orderApp.Server.networking.packets.Type10;
import orderApp.Server.networking.packets.Type11;
import orderApp.Server.networking.packets.Type2;
import orderApp.Server.networking.packets.Type3;
import orderApp.Server.networking.packets.Type4;
import orderApp.Server.networking.packets.Type5;
import orderApp.Server.networking.packets.Type6;
import orderApp.Server.networking.packets.Type8;
import orderApp.Server.networking.packets.Type9;

class Connection extends Thread {
    public static final int maxAttempts = 10;
    public static final int socketTimeOut=500;
    public static final int packetWaitTimeMs = 1000;
    public static final int reconnectTries = 10; // 10 produces maximum delay of 1024 ms - about a second.
    private volatile ConcurrentLinkedQueue<Packet> queue = new ConcurrentLinkedQueue<>();
    private final ArrayList<Integer> receivedIdempotencies;
    private final DelayQueue<ResponseWait> waiting = new DelayQueue<>();
    private Socket socket;
    private volatile boolean open = true;
    protected final InetAddress ip;
    private int currentIdempotency;
    private DataOutputStream out;
    private DataInputStream in;
    private final CloseListener listener;
    private boolean triesReconnect;
    private boolean acceptNewReconnect = false;

    protected Connection(Socket socket, boolean triesReconnect, CloseListener closeListener) {
        this.socket = socket;
        this.ip = socket.getInetAddress();
        this.triesReconnect = triesReconnect;
        this.listener = closeListener;
        this.receivedIdempotencies = new ArrayList<>();
        start();
    }
    protected Connection(Socket socket, boolean triesReconnect, CloseListener closeListener, int startIdempotency, ArrayList<Integer> receivedIdempotencies) {
        this.socket = socket;
        this.ip = socket.getInetAddress();
        this.triesReconnect = triesReconnect;
        this.listener = closeListener;
        this.currentIdempotency = startIdempotency;
        this.receivedIdempotencies = receivedIdempotencies;
        start();
    }
    public void run() {
        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("Making socket parts failed for connection IP: "+ socket.getInetAddress().toString());
            if (e.getMessage() != null) {
                System.out.println(e.getMessage());
            }
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        while (open) {
            try {
                try {
                    socket.setSoTimeout(socketTimeOut);
                    Header header = new Header(in);
                    System.out.println(ip + ": " + header);
                    if (header.versionNumber != Network.NETWORK_VERSION_NUMBER) {
                        // Version is incorrect, throw error message
                        System.out.println("Incorrect version number: Expected " + Network.NETWORK_VERSION_NUMBER + " but received " + header.versionNumber + ".");
                        continue;
                    }

                    // Handle the packet
                    handlePacket(header);

                    // Receive the idempotency (Do afterwards so we don't mess up packet handling)
                    receivedIdempotencies.add(header.idempotencyToken);
                    // Update our current idempotency so we don't cause a failure when we send something
                    // Check if it larger first, so a scan packet doesn't break everything
                    if (header.idempotencyToken > currentIdempotency) {
                        currentIdempotency = header.idempotencyToken;
                    }


                    // Remove from waiting responses if it exists
                    removeWaiting(header);
                    System.out.println(ip + ": packet handling complete");
                } catch (SocketTimeoutException ignored) {}

                // Check for unreceived expired packets (item is null if nothing is ready)
                ResponseWait item = waiting.poll();
                while (item != null) {
                    if (item.getAttempt() > maxAttempts) {
                        // Give up on packet
                        item.packet.sent(false);
                        item = waiting.poll();
                        continue;
                    }

                    // If we haven't given up, resend packet
                    item.packet.send(out);

                    // Redelay the packet so we don't immediately resend it, and increment the attempt
                    item.restart(packetWaitTimeMs);
                    item.incrementAttempt();
                    waiting.add(item);
                    item = waiting.poll();
                }

                // Send a waiting packet (only do 1 at a time to give chance to check response)
                // TODO: evaluate if that is necessary
                Packet toSend = queue.poll();
                // Check whether there is anything
                if (toSend != null) {
                    System.out.println("sending packet: " + toSend);
                    toSend.send(out);
                    // Only add waiting now - we don't want it to time out before being sent
                    waiting.add(new ResponseWait(toSend, packetWaitTimeMs));
                }

            } catch (IOException e) {
                System.out.println(e.getClass() + " Exception in connection with ip " + ip + " " + e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
                if (triesReconnect) {
                    System.out.println("Attempting reconnect.");
                    if (!reconnectBackoff()) {
                        // Failed: close and do warning
                        System.out.println("Reconnect failed - connection disconnected");
                        close(false);
                    } else {
                        System.out.println("Reconnect success!");
                    }
                } else {
                    // Close this - we aren't reconnecting so we don't want to keep reading from the socket.
                    open = false;
                    // Don't fire listener because it wasn't a planned close and we don't try reconnect
                    // Allow a new socket to reconnect to this
                    acceptNewReconnect = true;
                }

            }
        }
    }

    /**
     * Tries to reconnect the connection using backoff. Blocks until a connection is made.
     * Tries a maximum of reconnectTries times.
     * @return Whether it was successful. If it tries reconnectTries times with no result, it returns false, otherwise true.
     */
    private boolean reconnectBackoff() {
        int delayMs = 1;
        int tries = 0;
        while (tries < reconnectTries) {
            // Try reconnection
            try {
                reconnect();
                return true;
            } catch (IOException ignored) {}
            // Exponential backoff - double delay each time (using bit shift)
            delayMs<<=1;

            // Wait for delay
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {}

            ++tries;
        }
        return false;
    }
    protected void removeWaiting(Header header) {
        for (ResponseWait packet: waiting) {
            // Only care about idempotency - this header is the sent one, not expected
            if (packet.packet.getHeader().idempotencyToken == header.idempotencyToken) {
                waiting.remove(packet);
                // Send the "successful send" message
                packet.packet.sent(true);
                // Exit here - there should only be one.
                return;
            }
        }
    }
    protected void sendPacket(Packet packet) {
        queue.add(packet);
        // Don't add to waiting now - it could expire before we actually send it
    }

    protected void reconnect() throws IOException {
        try {
            socket.close();
        } catch (Exception e) {
            System.out.println("Socket close failed");
            if (e.getMessage() != null) {
                System.out.println(e.getMessage());
            }
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        // Open a new socket
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, Network.PORT));
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    protected int getIdempotency() {
        currentIdempotency++;
        return currentIdempotency;
    }
    protected void close(boolean sendType4) {
        System.out.println("Close called on connection with IP: " + ip);
        // Stop it from reconnecting - we have intentionally closed it
        triesReconnect = false;
        if (sendType4) {
            try {
                Packet packet = new Type4(new Header(Network.NETWORK_VERSION_NUMBER, (short) 4, getIdempotency()));
                packet.send(out);
            } catch (Exception e) {
                System.err.println("Error while sending type 4 closing packet");
                if (e.getMessage() != null) {
                    System.err.println(e.getMessage());
                }
                System.err.println("Error in closing connection: " + Arrays.toString(e.getStackTrace()));
            }
        }
        open = false;
        try {
            socket.close();
        } catch (Exception e) {
            System.err.println("Closing socket failed");
        }
        // Notify the close listener
        listener.listen(this);
        // Unsend all waiting and queue packets
        for (Packet packet: queue) {
            packet.sent(false);
        }
        for (ResponseWait packet: waiting) {
            packet.packet.sent(false);
        }
    }

    private void handleType0(Header header) throws IOException {
        // Send a type 2 packet
        SessionData data = Network.getSessionData();
        Type2 packet = new Type2(new Header(Network.NETWORK_VERSION_NUMBER, (short) 2, header.idempotencyToken), data);

        sendPacket(packet);
    }
    private void handleType1(Header header) throws IOException {
        Type1 packet = new Type1(header, in);
        Order order = packet.getOrder();
        // Check whether we have had the idempotency before we do anything
        if (!receivedIdempotencies.contains(packet.getHeader().idempotencyToken)) {
            Network.addOrderAndUpdate(order, this);
        } else {
            System.err.println("Duplicate idempotency token from IP: " + ip);
        }
        // Send a response if everything worked (type 3, true)
        Type3 confirmPacket = new Type3(new Header(Network.NETWORK_VERSION_NUMBER, (short) 3, header.idempotencyToken), true);
        confirmPacket.send(out);
    }
    private void handleType2(Header header) throws IOException {
        Type2 packet = new Type2(header, in);
        SessionData data = packet.getData();

        Network.setSessionData(data);

        // Send a confirmation packet
        Type3 confirmPacket = new Type3(new Header(Network.NETWORK_VERSION_NUMBER, (short)3, header.idempotencyToken), true);
        confirmPacket.send(out);
    }
    private void handleType3(Header header) throws IOException {
        // Remove it from the stream.
        new Type3(header, in);
    }
    private void handleType4(Header header){
        // Close also triggers listener
        close(false);
    }
    private void handleType5(Header header) throws IOException {
        Type5 packet = new Type5(header, in);
        long orderID = packet.getOrderID();

        Network.removeOrderByID(orderID, this);
        // Send confirmation
        Type3 confirmPacket = new Type3(new Header(Network.NETWORK_VERSION_NUMBER, (short) 3, header.idempotencyToken), true);
        confirmPacket.send(out);
    }
    private void handleType6(Header header) throws IOException {
        Type6 packet = new Type6(header, in);

        boolean success;
        if (packet.getIsAdd()) {
            success = Network.addOrderChecksum(packet.getOrder(),packet.getChecksum());
        } else {
            success = Network.removeOrderChecksum(packet.getOrder().orderID, packet.getChecksum(), this);
        }

        if (!success) {
            // If we failed, send a type 10 to request information
            Type10 request = new Type10(new Header(Network.NETWORK_VERSION_NUMBER, (short) 10, header.idempotencyToken));
            sendPacket(request);
        }
    }
    private void handleType7(Header header) throws IOException {
        System.out.println("Type 7 received from IP " + ip);
        Type8 response = new Type8(new Header(Network.NETWORK_VERSION_NUMBER, (short) 8,header.idempotencyToken), Network.getDeviceType(), Network.getDeviceName());
        response.send(out);
    }

    private void handleType9(Header header) throws IOException {
        // Only do actions if idempotency isn't received
        if (!receivedIdempotencies.contains(header.idempotencyToken)) {
            Type9 packet = new Type9(header, in);

            boolean success = Network.addRemoveItemsByAmount(packet.getOrder(), packet.getChecksum());

            if (!success) {
                // Create a type 0 request for information
                Type0 informationRequest = new Type0(new Header(Network.NETWORK_VERSION_NUMBER, (short) 0, getIdempotency()));
                sendPacket(informationRequest);
            }
        } else {
            System.err.println("Received duplicate idempotency - no action taken - from IP " + ip);
        }
        // Send a confirmation
        Type3 confirmationPacket = new Type3(new Header(Network.NETWORK_VERSION_NUMBER, (short)3, header.idempotencyToken), true);
        confirmationPacket.send(out);
    }
    private void handleType10(Header header) throws IOException {
        OrderData data = Network.getOrderData();
        Type11 dataPacket = new Type11(new Header(Network.NETWORK_VERSION_NUMBER, (short) 11, header.idempotencyToken), data);
        dataPacket.send(out);
    }
    private void handleType11(Header header) throws IOException {
        Type11 packet = new Type11(header, in);
        Network.setOrderData(packet.getData());
    }
    private void handlePacket(Header header) throws IOException {
        switch (header.type) {
            case 0:
                handleType0(header);
                break;
            case 1:
                handleType1(header);
                break;
            case 2:
                handleType2(header);
                break;
            case 3:
                handleType3(header);
                break;
            case 4:
                handleType4(header);
                break;
            case 5:
                handleType5(header);
                break;
            case 6:
                handleType6(header);
                break;
            case 7:
                handleType7(header);
                break;
            case 8:
                System.err.println("Was sent a type 8 packet by " + ip.toString() + " - this shouldn't happen in an established connection.");
                break;
            case 9:
                handleType9(header);
                break;
            case 10:
                handleType10(header);
                break;
            case 11:
                handleType11(header);
                break;
            default:
                System.err.println("Unrecognised packet type in handlePacket of connection to IP " + ip.toString() + " - " + header.type);
        }
    }
    protected interface CloseListener {
        void listen(Connection connection);
    }
    public boolean isOpen() {
        return open;
    }
    protected ArrayList<Integer> getReceivedIdempotencies() {
        return receivedIdempotencies;
    }
}
