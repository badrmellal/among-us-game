package event;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;


public class GameEndData implements Serializable {
    private String message;
    private GameState.GameEndReason reason;
    private long gameDuration;
    private Map<String, PlayerStats> playerStats;

    public GameEndData(String message) {
        this.message = message;
        this.gameDuration = System.currentTimeMillis();
        this.playerStats = new HashMap<>();
    }

    public GameEndData(String message, GameState.GameEndReason reason, long startTime) {
        this.message = message;
        this.reason = reason;
        this.gameDuration = System.currentTimeMillis() - startTime;
        this.playerStats = new HashMap<>();
    }

    public void addPlayerStats(String playerId, PlayerStats stats) {
        playerStats.put(playerId, stats);
    }

    // Getters
    public String getMessage() { return message; }
    public GameState.GameEndReason getReason() { return reason; }
    public long getGameDuration() { return gameDuration; }
    public Map<String, PlayerStats> getPlayerStats() { return playerStats; }

    // Inner class for player statistics
    public static class PlayerStats implements Serializable {
        private int tasksCompleted;
        private int killCount;
        private int meetingsCalled;
        private boolean wasImpostor;
        private boolean survived;

        public PlayerStats(boolean wasImpostor) {
            this.wasImpostor = wasImpostor;
            this.survived = true;
        }

        // Getters and incrementers
        public void incrementTasks() { tasksCompleted++; }
        public void incrementKills() { killCount++; }
        public void incrementMeetings() { meetingsCalled++; }
        public void setDead() { survived = false; }

        public int getTasksCompleted() { return tasksCompleted; }
        public int getKillCount() { return killCount; }
        public int getMeetingsCalled() { return meetingsCalled; }
        public boolean wasImpostor() { return wasImpostor; }
        public boolean survived() { return survived; }
    }
}
