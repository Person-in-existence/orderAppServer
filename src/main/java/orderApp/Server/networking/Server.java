package orderApp.Server.networking;




import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;

import orderApp.Server.Main;
import orderApp.Server.networking.packets.Header;
import orderApp.Server.networking.packets.Type2;
import orderApp.Server.networking.packets.Type6;
import orderApp.Server.networking.packets.Type9;

class Server extends Thread {
    private ServerSocket socket;
    private volatile boolean accepting = false;
    private volatile boolean running = false;
    private final ArrayList<Connection> connections = new ArrayList<>();
    private Connection serverConnection;
    protected Server() {
        try {
            socket = new ServerSocket(Network.PORT);

        } catch (IOException e) {
            System.err.println("IOException");
            if (e.getMessage() != null) {
                System.err.println(e.getMessage());
            }
        }
    }

    protected void setAccepting(boolean accepting) {
        this.accepting = accepting;
    }


    public void run() {
        running = true;
        while (running) {
            try {
                if (accepting) {
                    Socket s = socket.accept();
                    System.out.println("New connection accept " + s.getInetAddress());
                    Main.reader.printAbove("New connection from: " + s.getInetAddress().getHostAddress());

                    // Try and find an existing connection with the socket.
                    boolean foundExisting = false;
                    // Synchronize as another thread could break otherwise
                    synchronized (connections) {
                        for (Connection connection: connections) {
                            if (connection.ip.toString().equals(s.getInetAddress().toString())) {
                                System.out.println("Pre-existing connection found for reconnect, transferring to that");


                                Connection newConnection = new Connection(s, false, this::removeConnection, connection.getIdempotency(), connection.getReceivedIdempotencies());
                                connections.add(newConnection);

                                // Close the old connection (which removes it because of the closelistener)


                                foundExisting = true;
                                // Close the old connection
                                connection.close(false);
                                break;
                            }
                        }
                    }

                    if (!foundExisting) {
                        // Don't try reconnect - that is the connecting device's job.
                        Connection connection = new Connection(s, false, this::removeConnection);
                        connections.add(connection);
                    }

                } else {
                    socket.accept().close(); // Reject the connection
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Socket Timeout");
            } catch (Exception e) {
                System.out.println("Exception " + e.getClass());
                if (e.getMessage() != null) {
                    System.out.println(e.getMessage());
                }
                System.out.println(Arrays.toString(e.getStackTrace()));
            }
        }
    }

    /**
     * Sends a new type 2 to all current connections. This is used for when data is changed by the server.
     */
    public void resendInfo(SessionData data) {
        synchronized (connections) {
            for (Connection connection: connections) {
                Type2 packet = new Type2(new Header(Network.NETWORK_VERSION_NUMBER, (short) 2, connection.getIdempotency()), data);
                connection.sendPacket(packet); // This will want a confirmation and handle resending.
            }
        }
    }

    public void sendUpdates(Order order, int checksum, Connection noSend) {
        synchronized (connections) {
            for (Connection connection: connections) {
                // Don't send to the waiter that did the order - it has already updated
                if (connection == noSend) {
                    continue;
                }
                Type9 packet = new Type9(new Header(Network.NETWORK_VERSION_NUMBER, (short) 9, connection.getIdempotency()), order, checksum);
                connection.sendPacket(packet);

                // Kitchen one: it's dumb but we don't know device type so we just send both :D
                Type6 kitchenPacket = new Type6(new Header(Network.NETWORK_VERSION_NUMBER, (short) 6, connection.getIdempotency()), order, checksum, true);
                connection.sendPacket(kitchenPacket);
            }
        }
    }

    protected void sendOrderRemovedUpdates(Order order, int checksum, Connection noSend) {
        synchronized (connections) {
            for (Connection connection: connections) {
                // Dont send to the kitchen that did the remove
                if (connection == noSend) {
                    continue;
                }
                Type6 removePacket = new Type6(new Header(Network.NETWORK_VERSION_NUMBER, (short) 6, connection.getIdempotency()), order, checksum, false);
                connection.sendPacket(removePacket);
            }
        }
    }

    protected void removeConnection(Connection connection) {
        synchronized (connections) {
            connections.remove(connection);
            System.out.println("Connections: " + connections);
        }
    }

    protected void end() {
        setAccepting(false);
        running = false;

        // Close the server socket (So we don't accept anything etc)
        try {
            socket.close();
        } catch (IOException ignored) {}
        synchronized (connections) {
            for (Connection connection : connections) {
                connection.close(true);
            }
        }

    }
    public boolean isRunning() {
        return running;
    }
}
