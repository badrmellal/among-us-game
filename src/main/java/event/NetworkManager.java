package event;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.Gson;

public class NetworkManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerId;
    private GameState gameState;
    private boolean isHost;
    private boolean isConnected;

    private ExecutorService messageProcessor;
    private BlockingQueue<NetworkMessage> messageQueue;
    private List<NetworkEventListener> eventListeners;
    private Map<String, PlayerState> playerStates;

    private Gson gson;

    // Constants
    private static final int PORT = 8080;
    private static final int RECONNECT_DELAY = 5000; // 5 seconds
    private static final int MAX_RECONNECT_ATTEMPTS = 3;

    public NetworkManager(String playerId, boolean isHost) {
        this.playerId = playerId;
        this.isHost = isHost;
        this.gameState = new GameState();
        this.messageQueue = new LinkedBlockingQueue<>();
        this.eventListeners = new ArrayList<>();
        this.playerStates = new ConcurrentHashMap<>();
        this.gson = new Gson();

        initializeNetworking();
    }

    private void initializeNetworking() {
        messageProcessor = Executors.newSingleThreadExecutor();
        messageProcessor.submit(this::processMessages);

        if (isHost) {
            startServer();
        }
    }

    public void connect(String host) {
        try {
            socket = new Socket(host, PORT);
            setupStreams();
            isConnected = true;

            // Start message receiver
            new Thread(this::receiveMessages).start();

            // Send initial connection message
            sendConnectionMessage();

        } catch (IOException e) {
            handleConnectionError(e);
        }
    }

    private void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    handleNewConnection(clientSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupStreams() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void handleNewConnection(Socket clientSocket) {
        // Create new client handler thread
        ClientHandler handler = new ClientHandler(clientSocket, this);
        new Thread(handler).start();
    }

    private void sendConnectionMessage() {
        NetworkMessage message = new NetworkMessage(
                NetworkMessage.Type.CONNECT,
                new ConnectionData(playerId, isHost)
        );
        sendMessage(message);
    }

    public void sendMessage(NetworkMessage message) {
        if (!isConnected) return;

        try {
            String jsonMessage = gson.toJson(message);
            out.println(jsonMessage);
        } catch (Exception e) {
            handleSendError(e);
        }
    }

    private void receiveMessages() {
        while (isConnected) {
            try {
                String jsonMessage = in.readLine();
                if (jsonMessage == null) {
                    handleDisconnection();
                    break;
                }

                NetworkMessage message = gson.fromJson(
                        jsonMessage,
                        NetworkMessage.class
                );
                messageQueue.put(message);

            } catch (IOException e) {
                handleReceiveError(e);
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                NetworkMessage message = messageQueue.take();
                handleMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleMessage(NetworkMessage message) {
        switch (message.getType()) {
            case CONNECT:
                handleConnect(message);
                break;
            case DISCONNECT:
                handleDisconnect(message);
                break;
            case PLAYER_UPDATE:
                handlePlayerUpdate(message);
                break;
            case GAME_STATE:
                handleGameState(message);
                break;
            case CHAT:
                handleChat(message);
                break;
            case EMERGENCY_MEETING:
                handleEmergencyMeeting(message);
                break;
            case VOTE:
                handleVote(message);
                break;
            case TASK_COMPLETE:
                handleTaskComplete(message);
                break;
            case KILL:
                handleKill(message);
                break;
            case SABOTAGE:
                handleSabotage(message);
                break;
        }

        // Notify event listeners
        notifyEventListeners(message);
    }

    private void handleConnect(NetworkMessage message) {
        ConnectionData data = (ConnectionData) message.getData();
        playerStates.put(data.playerId, new PlayerState());

        if (isHost) {
            // Send current game state to new player
            sendGameState(data.playerId);
        }
    }

    private void handleDisconnect(NetworkMessage message) {
        String disconnectedId = (String) message.getData();
        playerStates.remove(disconnectedId);
        updateGameState();
    }

    private void handlePlayerUpdate(NetworkMessage message) {
        PlayerUpdateData data = (PlayerUpdateData) message.getData();
        PlayerState state = playerStates.get(data.playerId);
        if (state != null) {
            state.update(data);
        }
        updateGameState();
    }

    private void handleGameState(NetworkMessage message) {
        GameState newState = (GameState) message.getData();
        gameState.update(newState);
    }

    private void handleChat(NetworkMessage message) {
        ChatMessage chatMessage = (ChatMessage) message.getData();
        notifyEventListeners(message);
    }

    private void handleEmergencyMeeting(NetworkMessage message) {
        EmergencyData data = (EmergencyData) message.getData();
        gameState.startEmergencyMeeting(data);
        updateGameState();
    }

    private void handleVote(NetworkMessage message) {
        VoteData data = (VoteData) message.getData();
        gameState.registerVote(data);
        updateGameState();
    }

    private void handleTaskComplete(NetworkMessage message) {
        TaskData data = (TaskData) message.getData();
        gameState.completeTask(data);
        updateGameState();
    }

    private void handleKill(NetworkMessage message) {
        KillData data = (KillData) message.getData();
        gameState.registerKill(data);
        updateGameState();
    }

    private void handleSabotage(NetworkMessage message) {
        SabotageData data = (SabotageData) message.getData();
        gameState.triggerSabotage(data);
        updateGameState();
    }

    private void updateGameState() {
        NetworkMessage message = new NetworkMessage(
                NetworkMessage.Type.GAME_STATE,
                gameState
        );
        broadcastMessage(message);
    }

    public void broadcastMessage(NetworkMessage message) {
        if (isHost) {
            // Send to all connected clients
            for (ClientHandler handler : getClientHandlers()) {
                handler.sendMessage(message);
            }
        } else {
            // Send to server
            sendMessage(message);
        }
    }

    private void handleConnectionError(Exception e) {
        isConnected = false;
        notifyEventListeners(new NetworkMessage(
                NetworkMessage.Type.ERROR,
                "Connection error: " + e.getMessage()
        ));

        // Attempt to reconnect
        attemptReconnect();
    }

    private void handleSendError(Exception e) {
        notifyEventListeners(new NetworkMessage(
                NetworkMessage.Type.ERROR,
                "Send error: " + e.getMessage()
        ));
    }

    private void handleReceiveError(Exception e) {
        notifyEventListeners(new NetworkMessage(
                NetworkMessage.Type.ERROR,
                "Receive error: " + e.getMessage()
        ));
    }

    private void handleDisconnection() {
        isConnected = false;
        notifyEventListeners(new NetworkMessage(
                NetworkMessage.Type.DISCONNECT,
                playerId
        ));

        // Attempt to reconnect
        attemptReconnect();
    }

    private void attemptReconnect() {
        new Thread(() -> {
            for (int i = 0; i < MAX_RECONNECT_ATTEMPTS; i++) {
                try {
                    Thread.sleep(RECONNECT_DELAY);
                    connect(socket.getInetAddress().getHostAddress());
                    if (isConnected) {
                        break;
                    }
                } catch (Exception e) {
                    // Continue trying
                }
            }
        }).start();
    }

    public void addEventListener(NetworkEventListener listener) {
        eventListeners.add(listener);
    }

    public void removeEventListener(NetworkEventListener listener) {
        eventListeners.remove(listener);
    }

    private void notifyEventListeners(NetworkMessage message) {
        for (NetworkEventListener listener : eventListeners) {
            listener.onNetworkEvent(message);
        }
    }

    private List<ClientHandler> getClientHandlers() {
        // This would be implemented to return all active client handlers
        return new ArrayList<>();
    }

    public void shutdown() {
        isConnected = false;
        messageProcessor.shutdown();

        try {
            if (socket != null) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getters
    public boolean isConnected() { return isConnected; }
    public boolean isHost() { return isHost; }
    public String getPlayerId() { return playerId; }
    public GameState getGameState() { return gameState; }
}
