package orderApp.Server.networking;

public class SessionData {
    public final SessionItem[] items;
    public SessionData(SessionItem[] items) {
        this.items = items;
    }
    public static class SessionItem {
        public final String name;
        public final int quantity;
        public SessionItem(String name, int quantity) {
            this.name = name;
            this.quantity = quantity;
        }
    }

}
