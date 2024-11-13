package event;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import java.io.Serializable;


public class Task implements Serializable {
    private String id;
    private String name;
    private TaskType type;
    private Point2D.Double position;
    private boolean isCompleted;
    private boolean isLongTask;
    private double progress;
    private String roomName;

    // Visual properties
    private transient Image taskIcon;
    private static final int ICON_SIZE = 32;

    public Task(String name, Point2D.Double position, TaskType type) {
        this.id = generateTaskId();
        this.name = name;
        this.position = position;
        this.type = type;
        this.isCompleted = false;
        this.progress = 0.0;
        this.isLongTask = isLongTaskType(type);
        loadTaskIcon();
    }

    private String generateTaskId() {
        return "task_" + System.currentTimeMillis() + "_" +
                Math.round(Math.random() * 1000);
    }

    private boolean isLongTaskType(TaskType type) {
        return switch (type) {
            case UPLOAD, SCAN, CALIBRATE -> true;
            default -> false;
        };
    }

    private void loadTaskIcon() {
        String iconPath = switch (type) {
            case WIRES -> "/icons/wires.png";
            case UPLOAD -> "/icons/upload.png";
            case SCAN -> "/icons/scan.png";
            case CALIBRATE -> "/icons/calibrate.png";
            case PRIME_SHIELDS -> "/icons/shields.png";
            case CLEAN_FILTER -> "/icons/filter.png";
            case FUEL_ENGINE -> "/icons/fuel.png";
            case CHART_COURSE -> "/icons/navigation.png";
            default -> "/icons/default_task.png";
        };

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));
            taskIcon = icon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            // Create default icon if image loading fails
            taskIcon = createDefaultIcon();
        }
    }

    private Image createDefaultIcon() {
        BufferedImage icon = new BufferedImage(ICON_SIZE, ICON_SIZE,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(4, 4, ICON_SIZE - 8, ICON_SIZE - 8);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(4, 4, ICON_SIZE - 8, ICON_SIZE - 8);
        g2d.dispose();
        return icon;
    }

    public void drawIcon(Graphics2D g2d, double x, double y) {
        if (taskIcon != null) {
            g2d.drawImage(taskIcon,
                    (int)(x - ICON_SIZE/2),
                    (int)(y - ICON_SIZE/2),
                    null);

            // Draw progress indicator for long tasks
            if (isLongTask && !isCompleted && progress > 0) {
                drawProgressIndicator(g2d, x, y);
            }

            // Draw completion indicator
            if (isCompleted) {
                drawCompletionMark(g2d, x, y);
            }

            // Draw interaction hint when nearby
            if (isNearby()) {
                drawInteractionHint(g2d, x, y);
            }
        }
    }

    private void drawProgressIndicator(Graphics2D g2d, double x, double y) {
        // Draw progress bar background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(
                (int)(x - ICON_SIZE/2),
                (int)(y + ICON_SIZE/2 + 2),
                ICON_SIZE,
                4
        );

        // Draw progress bar
        g2d.setColor(Color.GREEN);
        g2d.fillRect(
                (int)(x - ICON_SIZE/2),
                (int)(y + ICON_SIZE/2 + 2),
                (int)(ICON_SIZE * progress),
                4
        );
    }

    private void drawCompletionMark(Graphics2D g2d, double x, double y) {
        g2d.setColor(new Color(0, 255, 0, 200));
        g2d.setStroke(new BasicStroke(2));

        // Draw checkmark
        g2d.drawLine(
                (int)(x - 8), (int)(y),
                (int)(x - 3), (int)(y + 5)
        );
        g2d.drawLine(
                (int)(x - 3), (int)(y + 5),
                (int)(x + 8), (int)(y - 6)
        );
    }

    private void drawInteractionHint(Graphics2D g2d, double x, double y) {
        // Draw key hint
        String hint = "Space";
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(hint);

        // Draw background
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(
                (int)(x - textWidth/2 - 4),
                (int)(y - ICON_SIZE - 20),
                textWidth + 8,
                16,
                8,
                8
        );

        // Draw text
        g2d.setColor(Color.WHITE);
        g2d.drawString(hint,
                (int)(x - textWidth/2),
                (int)(y - ICON_SIZE - 8)
        );
    }

    public void updateProgress(double newProgress) {
        if (!isCompleted) {
            this.progress = Math.min(1.0, Math.max(0.0, newProgress));
            if (progress >= 1.0) {
                complete();
            }
        }
    }

    public void complete() {
        this.isCompleted = true;
        this.progress = 1.0;
    }

    public boolean isNearby() {
        // This should be implemented to check if a player is near the task
        // You would typically pass in the player's position and check the distance
        return true; // Placeholder implementation
    }

    public boolean isInteractable() {
        return !isCompleted && isNearby();
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public TaskType getType() { return type; }
    public Point2D.Double getPosition() { return position; }
    public boolean isCompleted() { return isCompleted; }
    public boolean isLongTask() { return isLongTask; }
    public double getProgress() { return progress; }
    public String getRoomName() { return roomName; }

    // Setters
    public void setRoomName(String roomName) { this.roomName = roomName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Task[%s: %s] - %s",
                type, name, isCompleted ? "Completed" :
                        isLongTask ? String.format("%.0f%%", progress * 100) : "Pending"
        );
    }
}