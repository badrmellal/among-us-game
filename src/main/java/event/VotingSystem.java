package event;

import javax.swing.*;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class VotingSystem {
    private List<Player> players;
    private Map<String, String> votes; // PlayerID -> VotedPlayerID
    private Timer votingTimer;
    private boolean isVotingTime;
    private String bodyReporter;
    private String emergencyButton;
    private JPanel votingPanel;
    private VotingDialog votingDialog;
    private NetworkManager networkManager;

    // Constants
    private static final int VOTING_TIME = 120; // seconds
    private static final int DISCUSSION_TIME = 30; // seconds
    private int timeRemaining;

    public VotingSystem(List<Player> players) {
        this.players = players;
        this.votes = new HashMap<>();
        this.isVotingTime = false;
        initializeVotingPanel();
    }

    private void initializeVotingPanel() {
        votingPanel = new JPanel();
        votingPanel.setLayout(new BorderLayout());
        votingPanel.setBorder(BorderFactory.createTitledBorder("Emergency Meeting"));
        updateVotingPanel();
    }

    public void startEmergencyMeeting(String reporterId, String reason) {
        if (isVotingTime) return;

        this.bodyReporter = reporterId;
        this.emergencyButton = reason;
        this.votes.clear();
        this.isVotingTime = true;
        this.timeRemaining = DISCUSSION_TIME + VOTING_TIME;

        // Start voting timer
        startVotingTimer();

        // Create and show voting dialog
        showVotingDialog();

        // Update voting panel
        updateVotingPanel();

        // Notify network
        if (networkManager != null) {
            networkManager.sendEmergencyMeeting(reporterId, reason);
        }
    }

    private void startVotingTimer() {
        if (votingTimer != null) {
            votingTimer.stop();
        }

        votingTimer = new Timer(1000, e -> {
            timeRemaining--;
            updateTimerDisplay();

            if (timeRemaining <= 0) {
                endVoting();
            }
        });
        votingTimer.start();
    }

    private void updateTimerDisplay() {
        if (votingDialog != null) {
            votingDialog.updateTimer(timeRemaining);
        }
    }

    private void showVotingDialog() {
        votingDialog = new VotingDialog(players, bodyReporter, emergencyButton);
        votingDialog.setVisible(true);
    }

    private void updateVotingPanel() {
        votingPanel.removeAll();

        if (isVotingTime) {
            // Add timer display
            JLabel timerLabel = new JLabel(formatTime(timeRemaining));
            timerLabel.setHorizontalAlignment(SwingConstants.CENTER);
            timerLabel.setFont(new Font("Arial", Font.BOLD, 20));
            votingPanel.add(timerLabel, BorderLayout.NORTH);

            // Add voting results
            JPanel resultsPanel = createVotingResultsPanel();
            votingPanel.add(resultsPanel, BorderLayout.CENTER);
        } else {
            // Show emergency button
            JButton emergencyButton = new JButton("Call Emergency Meeting");
            emergencyButton.addActionListener(e -> startEmergencyMeeting("local", "Emergency Button"));
            votingPanel.add(emergencyButton, BorderLayout.CENTER);
        }

        votingPanel.revalidate();
        votingPanel.repaint();
    }

    private JPanel createVotingResultsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Sort players by vote count
        Map<String, Long> voteCounts = votes.values().stream()
                .collect(Collectors.groupingBy(
                        v -> v,
                        Collectors.counting()
                ));

        List<Player> sortedPlayers = players.stream()
                .sorted((p1, p2) -> {
                    long count1 = voteCounts.getOrDefault(p1.getId(), 0L);
                    long count2 = voteCounts.getOrDefault(p2.getId(), 0L);
                    return Long.compare(count2, count1);
                })
                .collect(Collectors.toList());

        // Add player vote bars
        for (Player player : sortedPlayers) {
            if (!player.isDead()) {
                panel.add(createVoteBar(player, voteCounts));
                panel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }

        return panel;
    }

    private JPanel createVoteBar(Player player, Map<String, Long> voteCounts) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(200, 30));

        // Player color indicator
        JPanel colorIndicator = new JPanel();
        colorIndicator.setBackground(player.getColor());
        colorIndicator.setPreferredSize(new Dimension(20, 20));
        panel.add(colorIndicator, BorderLayout.WEST);

        // Player name
        JLabel nameLabel = new JLabel(player.getName());
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        panel.add(nameLabel, BorderLayout.CENTER);

        // Vote count
        long voteCount = voteCounts.getOrDefault(player.getId(), 0L);
        JLabel countLabel = new JLabel(String.valueOf(voteCount));
        countLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(countLabel, BorderLayout.EAST);

        return panel;
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    public void castVote(String voterId, String votedId) {
        if (!isVotingTime) return;

        votes.put(voterId, votedId);
        updateVotingPanel();

        // Check if all votes are in
        if (votes.size() == getAlivePlayers().size()) {
            endVoting();
        }

        // Notify network
        if (networkManager != null) {
            networkManager.sendVote(voterId, votedId);
        }
    }

    private void endVoting() {
        if (votingTimer != null) {
            votingTimer.stop();
        }

        // Calculate results
        String ejectedPlayerId = calculateVotingResults();

        // Show results dialog
        showVotingResults(ejectedPlayerId);

        // Reset voting state
        isVotingTime = false;
        votes.clear();

        // Update UI
        updateVotingPanel();

        if (votingDialog != null) {
            votingDialog.dispose();
            votingDialog = null;
        }
    }

    private String calculateVotingResults() {
        // Count votes
        Map<String, Long> voteCounts = votes.values().stream()
                .collect(Collectors.groupingBy(
                        v -> v,
                        Collectors.counting()
                ));

        // Find player with most votes
        return voteCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private void showVotingResults(String ejectedPlayerId) {
        if (ejectedPlayerId != null) {
            Player ejectedPlayer = players.stream()
                    .filter(p -> p.getId().equals(ejectedPlayerId))
                    .findFirst()
                    .orElse(null);

            if (ejectedPlayer != null) {
                String message = ejectedPlayer.getName() + " was ejected.\n";
                message += ejectedPlayer.isImpostor() ?
                        "They were The Impostor." :
                        "They were not The Impostor.";

                JOptionPane.showMessageDialog(null,
                        message,
                        "Voting Results",
                        JOptionPane.INFORMATION_MESSAGE);

                ejectedPlayer.kill();
            }
        }
    }

    private List<Player> getAlivePlayers() {
        return players.stream()
                .filter(p -> !p.isDead())
                .collect(Collectors.toList());
    }

    public JPanel getVotingPanel() {
        return votingPanel;
    }

    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public boolean isVotingTime() {
        return isVotingTime;
    }

    // Inner class for voting dialog
    private class VotingDialog extends JDialog {
        private JLabel timerLabel;
        private Map<String, JButton> playerButtons;

        public VotingDialog(List<Player> players, String reporterId, String reason) {
            setTitle("Emergency Meeting");
            setModal(false);
            setSize(500, 400);
            setLocationRelativeTo(null);

            // Create main panel
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout(10, 10));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Add header
            JPanel headerPanel = createHeaderPanel(reporterId, reason);
            mainPanel.add(headerPanel, BorderLayout.NORTH);

            // Add player buttons
            JPanel playerPanel = createPlayerButtonsPanel(players);
            mainPanel.add(playerPanel, BorderLayout.CENTER);

            // Add timer
            timerLabel = new JLabel(formatTime(timeRemaining));
            timerLabel.setHorizontalAlignment(SwingConstants.CENTER);
            timerLabel.setFont(new Font("Arial", Font.BOLD, 24));
            mainPanel.add(timerLabel, BorderLayout.SOUTH);

            add(mainPanel);
        }

        private JPanel createHeaderPanel(String reporterId, String reason) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            String headerText = reason.equals("Emergency Button") ?
                    reporterId + " called an Emergency Meeting!" :
                    reporterId + " reported a dead body!";

            JLabel headerLabel = new JLabel(headerText);
            headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            headerLabel.setFont(new Font("Arial", Font.BOLD, 18));

            JLabel discussLabel = new JLabel("Discuss!");
            discussLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            discussLabel.setFont(new Font("Arial", Font.PLAIN, 16));

            panel.add(headerLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(discussLabel);

            return panel;
        }

        private JPanel createPlayerButtonsPanel(List<Player> players) {
            JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
            playerButtons = new HashMap<>();

            for (Player player : getAlivePlayers()) {
                JButton button = createPlayerButton(player);
                playerButtons.put(player.getId(), button);
                panel.add(button);
            }

            // Add skip vote button
            JButton skipButton = new JButton("Skip Vote");
            skipButton.addActionListener(e -> castVote("local", "skip"));
            panel.add(skipButton);

            return panel;
        }

        private JButton createPlayerButton(Player player) {
            JButton button = new JButton(player.getName()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON
                    );

                    // Draw background
                    g2d.setColor(player.getColor());
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                    // Draw text
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(getFont());
                    FontMetrics fm = g2d.getFontMetrics();
                    g2d.drawString(getText(),
                            (getWidth() - fm.stringWidth(getText())) / 2,
                            (getHeight() + fm.getAscent()) / 2
                    );
                }
            };

            button.addActionListener(e -> castVote("local", player.getId()));
            return button;
        }

        public void updateTimer(int timeRemaining) {
            timerLabel.setText(formatTime(timeRemaining));
        }
    }
}