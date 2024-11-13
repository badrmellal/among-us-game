package event;

import java.io.Serializable;
import java.util.*;

public class GameState implements Serializable {
    private Map<String, Player> players;
    private Map<String, TaskProgress> taskProgress;
    private boolean gameInProgress;
    private GamePhase currentPhase;
    private VotingSession currentVoting;
    private SabotageState sabotageState;
    private long gameStartTime;

    public enum GamePhase {
        LOBBY,
        TASKS,
        EMERGENCY_MEETING,
        VOTING,
        GAME_OVER
    }

    public enum GameEndReason {
        IMPOSTOR_VICTORY,
        CREWMATE_VICTORY,
        INSUFFICIENT_PLAYERS,
        TIMEOUT
    }

    public GameState() {
        this.players = new HashMap<>();
        this.taskProgress = new HashMap<>();
        this.gameInProgress = false;
        this.currentPhase = GamePhase.LOBBY;
        this.sabotageState = new SabotageState();
    }

    public void startGame() {
        gameInProgress = true;
        currentPhase = GamePhase.TASKS;
        gameStartTime = System.currentTimeMillis();
    }

    public void endGame(GameEndReason reason) {
        gameInProgress = false;
        currentPhase = GamePhase.GAME_OVER;
    }

    public void startEmergencyMeeting(EmergencyData data) {
        currentPhase = GamePhase.EMERGENCY_MEETING;
        currentVoting = new VotingSession(data.reporterId, data.type);
    }

    public void registerVote(VoteData data) {
        if (currentVoting != null) {
            currentVoting.registerVote(data.voterId, data.votedId);
        }
    }

    public void completeTask(TaskData data) {
        TaskProgress progress = taskProgress.get(data.playerId);
        if (progress != null) {
            progress.completeTask(data.taskId);
        }
    }

    public void registerKill(KillData data) {
        Player victim = players.get(data.victimId);
        if (victim != null) {
            victim.kill();
        }
    }

    public void triggerSabotage(SabotageData data) {
        sabotageState.startSabotage(data.type, data.duration);
    }

    public boolean shouldImpostorsWin() {
        long aliveCrewmates = players.values().stream()
                .filter(p -> !p.isImpostor() && !p.isDead())
                .count();
        long aliveImpostors = players.values().stream()
                .filter(p -> p.isImpostor() && !p.isDead())
                .count();

        return aliveImpostors >= aliveCrewmates;
    }

    public boolean shouldCrewmatesWin() {
        boolean allTasksComplete = taskProgress.values().stream()
                .allMatch(TaskProgress::isComplete);

        boolean allImpostorsDead = players.values().stream()
                .filter(Player::isImpostor)
                .allMatch(Player::isDead);

        return allTasksComplete || allImpostorsDead;
    }

    public boolean isGameInProgress() {
        return gameInProgress;
    }

    public boolean isVotingComplete() {
        return currentVoting != null && currentVoting.isComplete();
    }

    public boolean areAllTasksComplete() {
        return taskProgress.values().stream()
                .allMatch(TaskProgress::isComplete);
    }

    // Getters
    public Map<String, Player> getPlayers() { return players; }
    public GamePhase getCurrentPhase() { return currentPhase; }
    public VotingSession getCurrentVoting() { return currentVoting; }
    public SabotageState getSabotageState() { return sabotageState; }
    public long getGameStartTime() { return gameStartTime; }
}