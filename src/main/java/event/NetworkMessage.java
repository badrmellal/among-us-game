package event;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Map;

public class NetworkMessage implements Serializable {
    private Type type;
    private Object data;
    private long timestamp;

    public enum Type {
        CONNECT,
        DISCONNECT,
        PLAYER_UPDATE,
        GAME_STATE,
        CHAT,
        EMERGENCY_MEETING,
        VOTE,
        TASK_COMPLETE,
        KILL,
        SABOTAGE,
        ERROR
    }

    public NetworkMessage(Type type, Object data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public Type getType() { return type; }
    public Object getData() { return data; }
    public long getTimestamp() { return timestamp; }
}

// Data classes for different message types
class ConnectionData implements Serializable {
    String playerId;
    boolean isHost;
    String playerName;
    java.awt.Color playerColor;

    public ConnectionData(String playerId, boolean isHost) {
        this.playerId = playerId;
        this.isHost = isHost;
    }
}

class PlayerUpdateData implements Serializable {
    String playerId;
    Point2D.Double position;
    boolean isMoving;
    boolean isInVent;
    String currentRoom;

    public PlayerUpdateData(String playerId, Point2D.Double position) {
        this.playerId = playerId;
        this.position = position;
    }
}

class GameState implements Serializable {
    Map<String, PlayerState> players;
    GamePhase currentPhase;
    Map<String, TaskProgress> taskProgress;
    VotingSession currentVoting;
    SabotageState sabotageState;
    long gameStartTime;

    public enum GamePhase {
        LOBBY,
        TASKS,
        EMERGENCY_MEETING,
        VOTING,
        GAME_OVER
    }

    public void update(GameState newState) {
        this.players = newState.players;
        this.currentPhase = newState.currentPhase;
        this.taskProgress = newState.taskProgress;
        this.currentVoting = newState.currentVoting;
        this.sabotageState = newState.sabotageState;
    }
}

class ChatMessage implements Serializable {
    String senderId;
    String message;
    ChatType type;
    long timestamp;

    public enum ChatType {
        GLOBAL,
        IMPOSTOR,
        GHOST,
        SYSTEM
    }

    public ChatMessage(String senderId, String message, ChatType type) {
        this.senderId = senderId;
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
}

class EmergencyData implements Serializable {
    String reporterId;
    EmergencyType type;
    Point2D.Double location;
    String deadBodyId;
    long timestamp;

    public enum EmergencyType {
        BUTTON,
        DEAD_BODY,
        SABOTAGE
    }

    public EmergencyData(String reporterId, EmergencyType type) {
        this.reporterId = reporterId;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
}

class VoteData implements Serializable {
    String voterId;
    String votedId;
    boolean isSkip;
    long timestamp;

    public VoteData(String voterId, String votedId) {
        this.voterId = voterId;
        this.votedId = votedId;
        this.isSkip = votedId.equals("skip");
        this.timestamp = System.currentTimeMillis();
    }
}


class TaskData implements Serializable {
    String playerId;
    String taskId;
    TaskType taskType;
    boolean isComplete;

    public enum TaskType {
        WIRES,
        UPLOAD,
        SCAN,
        CALIBRATE,
        PRIME_SHIELDS,
        CLEAN_FILTER,
        FUEL_ENGINE,
        CHART_COURSE
    }

    public TaskData(String playerId, String taskId, TaskType taskType) {
        this.playerId = playerId;
        this.taskId = taskId;
        this.taskType = taskType;
    }
}

class KillData implements Serializable {
    String killerId;
    String victimId;
    Point2D.Double location;
    String room;
    long timestamp;

    public KillData(String killerId, String victimId, Point2D.Double location) {
        this.killerId = killerId;
        this.victimId = victimId;
        this.location = location;
        this.timestamp = System.currentTimeMillis();
    }
}

class SabotageData implements Serializable {
    SabotageType type;
    Map<String, Object> parameters;
    long duration;
    boolean isFixed;

    public enum SabotageType {
        LIGHTS,
        OXYGEN,
        REACTOR,
        COMMUNICATIONS,
        DOORS
    }

    public SabotageData(SabotageType type, long duration) {
        this.type = type;
        this.duration = duration;
        this.isFixed = false;
    }
}

class PlayerState implements Serializable {
    String playerId;
    String playerName;
    java.awt.Color color;
    Point2D.Double position;
    boolean isDead;
    boolean isImpostor;
    boolean isInVent;
    String currentRoom;
    Map<String, TaskProgress> tasks;

    public void update(PlayerUpdateData data) {
        this.position = data.position;
        this.isInVent = data.isInVent;
        this.currentRoom = data.currentRoom;
    }
}

class VotingSession implements Serializable {
    Map<String, String> votes;  // voterId -> votedId
    long startTime;
    long endTime;
    String reporterId;
    EmergencyData.EmergencyType emergencyType;
    boolean isComplete;
    String ejectedPlayerId;

    public VotingSession(String reporterId, EmergencyData.EmergencyType type) {
        this.reporterId = reporterId;
        this.emergencyType = type;
        this.startTime = System.currentTimeMillis();
        this.isComplete = false;
    }
}

class SabotageState implements Serializable {
    boolean isActive;
    SabotageData.SabotageType type;
    long startTime;
    long remainingTime;
    Map<String, Object> parameters;

    public boolean isTimeExpired() {
        return System.currentTimeMillis() - startTime > remainingTime;
    }
}