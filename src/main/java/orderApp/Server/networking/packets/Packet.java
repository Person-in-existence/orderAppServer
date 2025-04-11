package orderApp.Server.networking.packets;



import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Packet {
    private SendListener listener = null;
    public abstract Header getHeader();
    public void send(DataOutputStream out) throws IOException {
        // Send the header
        getHeader().send(out);
        // Pass body to child class
        sendBody(out);
    }
    protected abstract void sendBody(DataOutputStream out) throws IOException;
    public void sent(boolean success) {
        if (listener != null) {
            listener.onSuccessfulSend(success);
        }
    }
    public void setSendListener(SendListener listener) {
        this.listener = listener;
    }

    public interface SendListener {
        void onSuccessfulSend(boolean success);
    }
    public String toString() {
        return getHeader().toString();
    }
}