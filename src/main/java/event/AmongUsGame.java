package event;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class AmongUsGame extends JFrame {
    private static final String GAME_TITLE = "Among Us Clone";
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;

    private GamePanel gamePanel;
    private ChatPanel chatPanel;
    private Player localPlayer;
    private List<Player> players;
    private GameServer gameServer;
    private TaskManager taskManager;
    private VotingSystem votingSystem;

    public AmongUsGame() {
        setupWindow();
        initializeGame();
        setupNetworking();
    }

    private void setupWindow() {
        setTitle(GAME_TITLE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Set up layout
        setLayout(new BorderLayout());
    }

    private void initializeGame() {
        // Initialize game components
        players = new ArrayList<>();
        localPlayer = new Player("Player" + System.currentTimeMillis() % 1000, false);
        players.add(localPlayer);

        // Create game panels
        gamePanel = new GamePanel(localPlayer, players);
        chatPanel = new ChatPanel();
        taskManager = new TaskManager();
        votingSystem = new VotingSystem(players);

        // Layout setup
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                gamePanel,
                createSidePanel()
        );
        splitPane.setResizeWeight(0.8);
        add(splitPane, BorderLayout.CENTER);

        // Add key listeners for player movement
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyRelease(e);
            }
        });

        setFocusable(true);
    }

    private JPanel createSidePanel() {
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(300, WINDOW_HEIGHT));

        // Add task list at the top
        JPanel taskListPanel = taskManager.getTaskPanel();
        taskListPanel.setPreferredSize(new Dimension(300, 200));
        sidePanel.add(taskListPanel, BorderLayout.NORTH);

        // Add chat panel in the middle
        sidePanel.add(chatPanel, BorderLayout.CENTER);

        // Add voting panel at the bottom
        JPanel votingPanel = votingSystem.getVotingPanel();
        votingPanel.setPreferredSize(new Dimension(300, 200));
        sidePanel.add(votingPanel, BorderLayout.SOUTH);

        return sidePanel;
    }

    private void setupNetworking() {
        gameServer = new GameServer();
        gameServer.setMessageHandler(message -> {
            handleNetworkMessage(message);
        });

        // Start server connection
        gameServer.connect("localhost", 8080);
    }

    private void handleNetworkMessage(NetworkMessage message) {
        switch (message.getType()) {
            case PLAYER_JOIN:
                handlePlayerJoin(message);
                break;
            case PLAYER_LEAVE:
                handlePlayerLeave(message);
                break;
            case PLAYER_MOVE:
                handlePlayerMove(message);
                break;
            case CHAT_MESSAGE:
                handleChatMessage(message);
                break;
            case VOTE_CAST:
                handleVoteCast(message);
                break;
            case TASK_COMPLETE:
                handleTaskComplete(message);
                break;
        }
    }

    private void handleKeyPress(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                localPlayer.setMovingUp(true);
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                localPlayer.setMovingDown(true);
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                localPlayer.setMovingLeft(true);
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                localPlayer.setMovingRight(true);
                break;
            case KeyEvent.VK_SPACE:
                handleInteraction();
                break;
        }

        // Send movement update to server
        gameServer.sendMessage(new NetworkMessage(
                NetworkMessage.Type.PLAYER_MOVE,
                localPlayer.getPosition()
        ));
    }

    private void handleKeyRelease(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                localPlayer.setMovingUp(false);
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                localPlayer.setMovingDown(false);
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                localPlayer.setMovingLeft(false);
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                localPlayer.setMovingRight(false);
                break;
        }
    }

    private void handleInteraction() {
        // Check for nearby tasks or players
        if (taskManager.hasNearbyTask(localPlayer.getPosition())) {
            taskManager.startTask();
        } else if (votingSystem.isVotingTime() && votingSystem.canVote()) {
            votingSystem.showVotingDialog();
        }
    }

    private void handlePlayerJoin(NetworkMessage message) {
        Player newPlayer = (Player) message.getData();
        players.add(newPlayer);
        gamePanel.repaint();
    }

    private void handlePlayerLeave(NetworkMessage message) {
        String playerId = (String) message.getData();
        players.removeIf(p -> p.getId().equals(playerId));
        gamePanel.repaint();
    }

    private void handlePlayerMove(NetworkMessage message) {
        PlayerPosition pos = (PlayerPosition) message.getData();
        players.stream()
                .filter(p -> p.getId().equals(pos.getPlayerId()))
                .findFirst()
                .ifPresent(p -> p.setPosition(pos.getX(), pos.getY()));
        gamePanel.repaint();
    }

    private void handleChatMessage(NetworkMessage message) {
        chatPanel.addMessage((ChatMessage) message.getData());
    }

    private void handleVoteCast(NetworkMessage message) {
        VoteData voteData = (VoteData) message.getData();
        votingSystem.registerVote(voteData);
    }

    private void handleTaskComplete(NetworkMessage message) {
        TaskData taskData = (TaskData) message.getData();
        taskManager.completeTask(taskData.getTaskId());
    }

    // Game loop
    public void startGameLoop() {
        Thread gameLoop = new Thread(() -> {
            while (true) {
                updateGame();
                try {
                    Thread.sleep(16); // Approximately 60 FPS
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        gameLoop.start();
    }

    private void updateGame() {
        // Update player positions
        localPlayer.update();

        // Check for task completion
        taskManager.checkTaskProgress();

        // Update voting system
        votingSystem.update();

        // Repaint game panel
        gamePanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AmongUsGame game = new AmongUsGame();
            game.setVisible(true);
            game.startGameLoop();
        });
    }
}
