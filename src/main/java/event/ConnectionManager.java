package event;

import java.net.Socket;
import java.net.SocketException;

public class ConnectionManager {
    private static final int CONNECTION_TIMEOUT = 30000; // 30 seconds
    private static final int PING_INTERVAL = 5000; // 5 seconds

    public static void setupConnectionTimeouts(Socket socket) throws SocketException {
        socket.setSoTimeout(CONNECTION_TIMEOUT);
    }

    public static void startPingThread(ClientHandler client) {
        new Thread(() -> {
            while (client.isRunning()) {
                try {
                    Thread.sleep(PING_INTERVAL);
                    client.sendMessage(new NetworkMessage(NetworkMessage.Type.PING, null));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
}
