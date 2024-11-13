package event;

import java.awt.geom.Point2D;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.Gson;
import java.util.Map;
import java.util.HashMap;


public class GameServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private Map<String, Player> players;
    private GameState gameState;
    private boolean isRunning;
    private ExecutorService clientExecutor;
    private MessageHandler messageHandler;
    private Gson gson;

    // Game settings
    private static final int MAX_PLAYERS = 10;
    private static final int MIN_PLAYERS = 4;
    private static final int IMPOSTOR_COUNT = 2;
    private static final long GAME_START_DELAY = 5000; // 5 seconds

    public GameServer(int port) {
        this.clients = new CopyOnWriteArrayList<>();
        this.players = new ConcurrentHashMap<>();
        this.gameState = new GameState();
        this.gson = new Gson();
        this.clientExecutor = Executors.newCachedThreadPool();

        initializeServer(port);
    }

    private void initializeServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("Server started on port " + port);

            // Start accepting clients
            startAcceptingClients();

        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }

    private void startAcceptingClients() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleNewClient(clientSocket);
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting client: " + e.getMessage());
                    }
                }
            }
        }).start();
    }

    private void handleNewClient(Socket clientSocket) {
        if (clients.size() >= MAX_PLAYERS) {
            rejectClient(clientSocket, "Server is full");
            return;
        }

        ClientHandler handler = new ClientHandler(clientSocket, this);
        clients.add(handler);
        clientExecutor.execute(handler);

        // Notify all clients about new player
        broadcastPlayerList();
    }

    private void rejectClient(Socket clientSocket, String reason) {
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            NetworkMessage rejectMessage = new NetworkMessage(
                    NetworkMessage.Type.ERROR,
                    reason
            );
            out.println(gson.toJson(rejectMessage));
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error rejecting client: " + e.getMessage());
        }
    }

    public void handleMessage(ClientHandler client, NetworkMessage message) {
        switch (message.getType()) {
            case CONNECT:
                handleConnect(client, message);
                break;
            case DISCONNECT:
                handleDisconnect(client);
                break;
            case PLAYER_UPDATE:
                handlePlayerUpdate(message);
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
    }

    private void handleConnect(ClientHandler client, NetworkMessage message) {
        ConnectionData data = (ConnectionData) message.getData();
        Player newPlayer = new Player(data.playerName, false);
        players.put(data.playerId, newPlayer);

        // Send current game state to new player
        client.sendMessage(new NetworkMessage(
                NetworkMessage.Type.GAME_STATE,
                gameState
        ));

        // Check if we can start the game
        checkGameStart();
    }

    private void handleDisconnect(ClientHandler client) {
        clients.remove(client);
        players.remove(client.getPlayerId());
        broadcastPlayerList();

        // Check if game should end due to too few players
        checkGameEnd();
    }

    private void handlePlayerUpdate(NetworkMessage message) {
        PlayerUpdateData data = (PlayerUpdateData) message.getData();
        Player player = players.get(data.playerId);
        if (player != null) {
            player.updateFromData(data);
            broadcastToAll(message);
        }
    }

    private void handleChat(NetworkMessage message) {
        ChatMessage chatMessage = (ChatMessage) message.getData();

        // Filter messages based on type
        List<ClientHandler> recipients = clients.stream()
                .filter(client -> canReceiveMessage(client, chatMessage))
                .toList();

        broadcastToClients(message, recipients);
    }

    private void handleEmergencyMeeting(NetworkMessage message) {
        EmergencyData data = (EmergencyData) message.getData();
        gameState.startEmergencyMeeting(data);
        broadcastToAll(message);
    }

    private void handleVote(NetworkMessage message) {
        VoteData data = (VoteData) message.getData();
        gameState.registerVote(data);
        broadcastToAll(message);

        // Check if voting is complete
        if (gameState.isVotingComplete()) {
            handleVotingResults();
        }
    }

    private void handleTaskComplete(NetworkMessage message) {
        TaskData data = (TaskData) message.getData();
        gameState.completeTask(data);
        broadcastToAll(message);

        // Check if all tasks are complete
        if (gameState.areAllTasksComplete()) {
            handleCrewmateVictory();
        }
    }

    private void handleKill(NetworkMessage message) {
        KillData data = (KillData) message.getData();
        gameState.registerKill(data);
        broadcastToAll(message);

        // Check win conditions
        checkWinConditions();
    }

    private void handleSabotage(NetworkMessage message) {
        SabotageData data = (SabotageData) message.getData();
        gameState.triggerSabotage(data);
        broadcastToAll(message);
    }

    private void checkGameStart() {
        if (players.size() >= MIN_PLAYERS && !gameState.isGameInProgress()) {
            startGame();
        }
    }

    private void startGame() {
        // Assign impostors
        assignImpostors();

        // Assign tasks
        assignTasks();

        // Update game state
        gameState.startGame();

        // Notify all players
        broadcastToAll(new NetworkMessage(
                NetworkMessage.Type.GAME_STATE,
                gameState
        ));
    }

    private void assignImpostors() {
        List<String> playerIds = new ArrayList<>(players.keySet());
        Collections.shuffle(playerIds);

        for (int i = 0; i < IMPOSTOR_COUNT && i < playerIds.size(); i++) {
            Player player = players.get(playerIds.get(i));
            player.setImpostor(true);
        }
    }

    private void assignTasks() {
        for (Player player : players.values()) {
            if (!player.isImpostor()) {
                List<Task> tasks = generateTaskList();
                player.assignTasks(tasks);
            }
        }
    }

    private List<Task> generateTaskList() {
        // Generate a balanced list of tasks
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task("Fix Wiring", new Point2D.Double(0, 0), TaskType.WIRES));
        // Add more tasks based on game balance
        return tasks;
    }

    private void checkWinConditions() {
        if (gameState.shouldImpostorsWin()) {
            handleImpostorVictory();
        } else if (gameState.shouldCrewmatesWin()) {
            handleCrewmateVictory();
        }
    }

    private void handleImpostorVictory() {
        gameState.endGame(GameState.GameEndReason.IMPOSTOR_VICTORY);
        broadcastGameEnd("Impostors Win!");
    }

    private void handleCrewmateVictory() {
        gameState.endGame(GameState.GameEndReason.CREWMATE_VICTORY);
        broadcastGameEnd("Crewmates Win!");
    }

    private void broadcastGameEnd(String message) {
        NetworkMessage endMessage = new NetworkMessage(
                NetworkMessage.Type.GAME_STATE,
                new GameEndData(message)
        );
        broadcastToAll(endMessage);
    }

    private void checkGameEnd() {
        if (players.size() < MIN_PLAYERS && gameState.isGameInProgress()) {
            // End game due to insufficient players
            gameState.endGame(GameState.GameEndReason.INSUFFICIENT_PLAYERS);
            broadcastGameEnd("Game ended: Too few players");
        }
    }

    private boolean canReceiveMessage(ClientHandler client, ChatMessage message) {
        Player player = players.get(client.getPlayerId());
        if (player == null) return false;

        return switch (message.type) {
            case GLOBAL -> true;
            case IMPOSTOR -> player.isImpostor();
            case GHOST -> player.isDead();
            case SYSTEM -> true;
            default -> false;
        };
    }

    public void broadcastToAll(NetworkMessage message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private void broadcastToClients(NetworkMessage message, List<ClientHandler> recipients) {
        for (ClientHandler client : recipients) {
            client.sendMessage(message);
        }
    }

    private void broadcastPlayerList() {
        NetworkMessage message = new NetworkMessage(
                NetworkMessage.Type.PLAYER_LIST,
                new ArrayList<>(players.values())
        );
        broadcastToAll(message);
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        players.remove(client.getPlayerId());
        broadcastPlayerList();
    }

    public void shutdown() {
        isRunning = false;

        // Close all client connections
        for (ClientHandler client : clients) {
            client.stop();
        }

        // Shutdown executor
        clientExecutor.shutdown();

        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    public interface MessageHandler {
        void handleMessage(NetworkMessage message);
    }

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: 8080");
            }
        }
        new GameServer(port);
    }
}