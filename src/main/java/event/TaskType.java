package event;

public enum TaskType {
    WIRES("Fix Wiring", "Connect matching colored wires", 30),
    UPLOAD("Upload Data", "Transfer data to headquarters", 60),
    SCAN("Submit Scan", "Complete medical scanning", 45),
    CALIBRATE("Calibrate Distributor", "Align power distributor", 30),
    PRIME_SHIELDS("Prime Shields", "Activate ship shields", 20),
    CLEAN_FILTER("Clean O2 Filter", "Remove debris from filter", 25),
    FUEL_ENGINE("Fuel Engines", "Fill engine tanks", 40),
    CHART_COURSE("Chart Course", "Set navigation path", 35);

    private final String displayName;
    private final String description;
    private final int duration; // in seconds

    TaskType(String displayName, String description, int duration) {
        this.displayName = displayName;
        this.description = description;
        this.duration = duration;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public boolean isLongTask() {
        return this == UPLOAD || this == SCAN;
    }

    public String getIconPath() {
        return "/icons/" + name().toLowerCase() + ".png";
    }

    public static TaskType fromString(String text) {
        for (TaskType type : TaskType.values()) {
            if (type.displayName.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No task type found for text: " + text);
    }

    public interface TaskCompletionCallback {
        void onTaskCompleted(TaskType type);
    }
}