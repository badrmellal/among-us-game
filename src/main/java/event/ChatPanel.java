package event;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class ChatPanel extends JPanel {
    private JTextPane chatArea;
    private JTextField messageField;
    private JComboBox<ChatMessage.ChatType> chatTypeCombo;
    private DefaultStyledDocument document;
    private NetworkManager networkManager;
    private Player localPlayer;
    private List<ChatMessage> messageHistory;
    private boolean isInMeeting;

    // Style constants
    private static final Color GLOBAL_COLOR = new Color(255, 255, 255);
    private static final Color IMPOSTOR_COLOR = new Color(255, 0, 0);
    private static final Color GHOST_COLOR = new Color(150, 150, 255);
    private static final Color SYSTEM_COLOR = new Color(255, 255, 0);
    private static final Font CHAT_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    public ChatPanel(NetworkManager networkManager, Player localPlayer) {
        this.networkManager = networkManager;
        this.localPlayer = localPlayer;
        this.messageHistory = new ArrayList<>();
        this.isInMeeting = false;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        initializeComponents();
    }

    private void initializeComponents() {
        // Chat area
        chatArea = new JTextPane();
        document = new DefaultStyledDocument();
        chatArea.setDocument(document);
        chatArea.setEditable(false);
        chatArea.setFont(CHAT_FONT);
        chatArea.setBackground(new Color(30, 30, 30));

        // Scroll pane for chat area
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(300, 400));

        // Input panel
        JPanel inputPanel = createInputPanel();

        // Add components
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Add automatic scrolling
        chatArea.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                scrollPane.getVerticalScrollBar().setValue(
                        scrollPane.getVerticalScrollBar().getMaximum()
                );
            }
        });
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        // Message input field
        messageField = new JTextField();
        messageField.setFont(CHAT_FONT);
        messageField.addActionListener(e -> sendMessage());

        // Chat type selector
        chatTypeCombo = new JComboBox<>(getChatTypes());
        chatTypeCombo.setRenderer(new ChatTypeRenderer());

        // Send button
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        // Add components to panel
        panel.add(chatTypeCombo, BorderLayout.WEST);
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        return panel;
    }

    private ChatMessage.ChatType[] getChatTypes() {
        if (localPlayer.isDead()) {
            return new ChatMessage.ChatType[]{ChatMessage.ChatType.GHOST};
        } else if (localPlayer.isImpostor()) {
            return new ChatMessage.ChatType[]{
                    ChatMessage.ChatType.GLOBAL,
                    ChatMessage.ChatType.IMPOSTOR
            };
        } else {
            return new ChatMessage.ChatType[]{ChatMessage.ChatType.GLOBAL};
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) return;

        // Create chat message
        ChatMessage chatMessage = new ChatMessage(
                localPlayer.getId(),
                message,
                (ChatMessage.ChatType) chatTypeCombo.getSelectedItem()
        );

        // Send message through network
        networkManager.sendMessage(new NetworkMessage(
                NetworkMessage.Type.CHAT,
                chatMessage
        ));

        // Clear input field
        messageField.setText("");
    }

    public void addMessage(ChatMessage message) {
        messageHistory.add(message);

        // Check if player can see this message
        if (canSeeMessage(message)) {
            appendMessageToChat(message);
        }
    }

    private boolean canSeeMessage(ChatMessage message) {
        switch (message.type) {
            case GLOBAL:
                return !localPlayer.isDead() || isInMeeting;
            case IMPOSTOR:
                return localPlayer.isImpostor();
            case GHOST:
                return localPlayer.isDead();
            case SYSTEM:
                return true;
            default:
                return false;
        }
    }

    private void appendMessageToChat(ChatMessage message) {
        try {
            // Create timestamp
            String timestamp = TIME_FORMAT.format(new Date(message.timestamp));

            // Get player name
            String playerName = getPlayerName(message.senderId);

            // Create message style
            Style style = chatArea.addStyle(null, null);
            StyleConstants.setForeground(style, getMessageColor(message.type));

            // Build message text
            StringBuilder messageText = new StringBuilder();
            messageText.append("[").append(timestamp).append("] ");

            if (message.type != ChatMessage.ChatType.SYSTEM) {
                messageText.append(playerName).append(": ");
            }

            messageText.append(message.message).append("\n");

            // Insert message
            document.insertString(
                    document.getLength(),
                    messageText.toString(),
                    style
            );

            // Add visual effects for certain message types
            if (message.type == ChatMessage.ChatType.IMPOSTOR) {
                addImpostorEffect(document.getLength() - messageText.length());
            } else if (message.type == ChatMessage.ChatType.GHOST) {
                addGhostEffect(document.getLength() - messageText.length());
            }

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private Color getMessageColor(ChatMessage.ChatType type) {
        switch (type) {
            case GLOBAL:
                return GLOBAL_COLOR;
            case IMPOSTOR:
                return IMPOSTOR_COLOR;
            case GHOST:
                return GHOST_COLOR;
            case SYSTEM:
                return SYSTEM_COLOR;
            default:
                return GLOBAL_COLOR;
        }
    }

    private void addImpostorEffect(int startOffset) {
        // Add subtle red glow effect for impostor messages
        Style style = chatArea.addStyle(null, null);
        StyleConstants.setBackground(style, new Color(100, 0, 0, 30));

        try {
            int endOffset = document.getLength() - 1;
            ((DefaultStyledDocument) chatArea.getDocument()).setCharacterAttributes(
                    startOffset,
                    endOffset - startOffset,
                    style,
                    false
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addGhostEffect(int startOffset) {
        // Add ghost effect with slightly transparent text
        Style style = chatArea.addStyle(null, null);
        StyleConstants.setForeground(style, new Color(150, 150, 255, 180));
        StyleConstants.setItalic(style, true);

        try {
            int endOffset = document.getLength() - 1;
            ((DefaultStyledDocument) chatArea.getDocument()).setCharacterAttributes(
                    startOffset,
                    endOffset - startOffset,
                    style,
                    false
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getPlayerName(String playerId) {
        // This should be implemented to get the player name from the game state
        return "Player " + playerId;
    }

    public void setInMeeting(boolean inMeeting) {
        this.isInMeeting = inMeeting;
        updateChatState();
    }

    private void updateChatState() {
        chatTypeCombo.setModel(new DefaultComboBoxModel<>(getChatTypes()));
        messageField.setEnabled(isInMeeting || localPlayer.isDead());

        // Clear chat if not in meeting and player is alive
        if (!isInMeeting && !localPlayer.isDead()) {
            clearChat();
        } else {
            // Restore relevant message history
            refreshChat();
        }
    }

    private void clearChat() {
        try {
            document.remove(0, document.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void refreshChat() {
        clearChat();
        for (ChatMessage message : messageHistory) {
            if (canSeeMessage(message)) {
                appendMessageToChat(message);
            }
        }
    }

    // Inner class for chat type renderer
    private class ChatTypeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ChatMessage.ChatType) {
                ChatMessage.ChatType type = (ChatMessage.ChatType) value;
                setForeground(getMessageColor(type));
                setText(type.toString());
            }

            return this;
        }
    }
}