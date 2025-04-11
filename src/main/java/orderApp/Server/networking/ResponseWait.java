package orderApp.Server.networking;




import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import orderApp.Server.networking.packets.Packet;

public class ResponseWait implements Delayed {
    public long time; // Use a long so we don't have overflow.
    public final Packet packet;
    public int attempt = 0;
    public ResponseWait(Packet packet, int waitTimeMs) {
        this.packet = packet;
        this.time = System.currentTimeMillis() + waitTimeMs;
    }
    public boolean isExpired() {
        return System.currentTimeMillis() > time;
    }
    public int timeToExpired() {
        // Int is safe here - the initial argument is an int wait time, and time doesn't go backwards (I hope)
        return (int) (time - System.currentTimeMillis());
    }
    public void incrementAttempt() {
        attempt++;
    }
    public int getAttempt() {
        return attempt;
    }
    public Packet getPacket() {
        return packet;
    }
    public void restart(int waitTimeMs) {
        this.time = System.currentTimeMillis() + waitTimeMs;
    }
    public String toString() {
        return packet.toString() + " target time: " + this.time + " attempt: " +this.attempt;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(time-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
    }
}
