package event;

import java.net.*;
import java.io.*;
import com.google.gson.Gson;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private NetworkManager networkManager;
    private PrintWriter out;
    private BufferedReader in;
    private String clientId;
    private boolean isRunning;
    private Gson gson;

    public ClientHandler(Socket socket, NetworkManager networkManager) {
        this.clientSocket = socket;
        this.networkManager = networkManager;
        this.gson = new Gson();
        this.isRunning = true;
    }

    @Override
    public void run() {
        try {
            setupStreams();
            handleClient();
        } catch (IOException e) {
            handleError(e);
        } finally {
            cleanup();
        }
    }

    private void setupStreams() throws IOException {
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    private void handleClient() {
        try {
            String message;
            while (isRunning && (message = in.readLine()) != null) {
                NetworkMessage networkMessage = gson.fromJson(
                        message,
                        NetworkMessage.class
                );

                // Handle initial connection message
                if (networkMessage.getType() == NetworkMessage.Type.CONNECT) {
                    ConnectionData data = (ConnectionData) networkMessage.getData();
                    clientId = data.playerId;
                }

                // Forward message to network manager
                networkManager.broadcastMessage(networkMessage);
            }
        } catch (IOException e) {
            handleError(e);
        }
    }

    public String getPlayerId() {
        return clientId;
    }


    public void sendMessage(NetworkMessage message) {
        if (out != null && !clientSocket.isClosed()) {
            try {
                String jsonMessage = gson.toJson(message);
                out.println(jsonMessage);
            } catch (Exception e) {
                handleError(e);
            }
        }
    }

    private void handleError(Exception e) {
        System.err.println("Error handling client " + clientId + ": " + e.getMessage());
        NetworkMessage errorMessage = new NetworkMessage(
                NetworkMessage.Type.ERROR,
                "Client error: " + e.getMessage()
        );
        networkManager.broadcastMessage(errorMessage);
    }

    private void cleanup() {
        isRunning = false;

        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Notify network manager of disconnection
        if (clientId != null) {
            NetworkMessage disconnectMessage = new NetworkMessage(
                    NetworkMessage.Type.DISCONNECT,
                    clientId
            );
            networkManager.broadcastMessage(disconnectMessage);
        }
    }

    public String getClientId() {
        return clientId;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        isRunning = false;
        cleanup();
    }
}