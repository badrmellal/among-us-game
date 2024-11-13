package event;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TaskManager {
    private List<Task> allTasks;
    private List<Task> playerTasks;
    private Task currentTask;
    private JPanel taskPanel;
    private JDialog taskDialog;
    private NetworkManager networkManager;
    private double taskProgress;

    // Task type distributions
    private static final Map<TaskType, Integer> TASK_DISTRIBUTION = Map.of(
            TaskType.WIRES, 3,
            TaskType.UPLOAD, 2,
            TaskType.SCAN, 1,
            TaskType.CALIBRATE, 2,
            TaskType.PRIME_SHIELDS, 1,
            TaskType.CLEAN_FILTER, 2,
            TaskType.FUEL_ENGINE, 2,
            TaskType.CHART_COURSE, 1
    );

    public TaskManager() {
        allTasks = new ArrayList<>();
        playerTasks = new ArrayList<>();
        initializeTasks();
        createTaskPanel();
    }

    private void initializeTasks() {
        // Create tasks based on distribution
        TASK_DISTRIBUTION.forEach((type, count) -> {
            for (int i = 0; i < count; i++) {
                allTasks.add(createTask(type));
            }
        });
    }

    private Task createTask(TaskType type) {
        return new Task(
                type.getName(),
                getTaskLocation(type),
                type
        );
    }

    private Point2D.Double getTaskLocation(TaskType type) {
        // Return predefined locations based on task type
        switch (type) {
            case WIRES:
                return new Point2D.Double(
                        200 + Math.random() * 100,
                        300 + Math.random() * 100
                );
            case UPLOAD:
                return new Point2D.Double(500, 400);
            case SCAN:
                return new Point2D.Double(800, 200);
            case CALIBRATE:
                return new Point2D.Double(700, 600);
            case PRIME_SHIELDS:
                return new Point2D.Double(900, 700);
            case CLEAN_FILTER:
                return new Point2D.Double(300, 500);
            case FUEL_ENGINE:
                return new Point2D.Double(200, 800);
            case CHART_COURSE:
                return new Point2D.Double(1000, 300);
            default:
                return new Point2D.Double(500, 500);
        }
    }

    private void createTaskPanel() {
        taskPanel = new JPanel();
        taskPanel.setLayout(new BoxLayout(taskPanel, BoxLayout.Y_AXIS));
        taskPanel.setBorder(BorderFactory.createTitledBorder("Tasks"));
        updateTaskPanel();
    }

    public void assignPlayerTasks() {
        // Randomly select tasks for the player
        playerTasks.clear();
        List<Task> availableTasks = new ArrayList<>(allTasks);
        Collections.shuffle(availableTasks);

        // Assign 5-8 random tasks
        int numTasks = 5 + new Random().nextInt(4);
        for (int i = 0; i < numTasks && i < availableTasks.size(); i++) {
            playerTasks.add(availableTasks.get(i));
        }

        updateTaskPanel();
    }

    private void updateTaskPanel() {
        taskPanel.removeAll();

        // Add progress bar
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue((int)(getTaskProgress() * 100));
        progressBar.setStringPainted(true);
        progressBar.setString(String.format("%.0f%%", getTaskProgress() * 100));
        taskPanel.add(progressBar);

        // Add spacing
        taskPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add task list
        for (Task task : playerTasks) {
            JPanel taskItem = createTaskListItem(task);
            taskPanel.add(taskItem);
            taskPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        taskPanel.revalidate();
        taskPanel.repaint();
    }

    private JPanel createTaskListItem(Task task) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        // Create status icon
        JLabel statusIcon = new JLabel(task.isCompleted() ? "✓" : "○");
        statusIcon.setForeground(task.isCompleted() ? new Color(0, 150, 0) : Color.GRAY);
        statusIcon.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        // Create task name label
        JLabel nameLabel = new JLabel(task.getName());
        nameLabel.setForeground(task.isCompleted() ? new Color(100, 100, 100) : Color.BLACK);

        panel.add(statusIcon, BorderLayout.WEST);
        panel.add(nameLabel, BorderLayout.CENTER);

        if (task.isCompleted()) {
            panel.setBackground(new Color(240, 240, 240));
        }

        return panel;
    }

    public void startTask(Task task) {
        if (task.isCompleted() || currentTask != null) {
            return;
        }

        currentTask = task;

        // Create task dialog
        taskDialog = new JDialog();
        taskDialog.setTitle(task.getName());
        taskDialog.setModal(true);
        taskDialog.setSize(400, 300);
        taskDialog.setLocationRelativeTo(null);

        // Create task-specific panel
        JPanel taskContent = createTaskContent(task);
        taskDialog.add(taskContent);

        // Add close button
        JButton closeButton = new JButton("Cancel");
        closeButton.addActionListener(e -> cancelTask());
        taskDialog.add(closeButton, BorderLayout.SOUTH);

        taskDialog.setVisible(true);
    }

    private JPanel createTaskContent(Task task) {
        switch (task.getType()) {
            case WIRES:
                return new WiringTaskPanel(this);
            case UPLOAD:
                return new UploadTaskPanel(this);
            case SCAN:
                return new ScannerTaskPanel(this);
            case CALIBRATE:
                return new CalibrationTaskPanel(this);
            case PRIME_SHIELDS:
                return new ShieldsTaskPanel(this);
            case CLEAN_FILTER:
                return new FilterTaskPanel(this);
            case FUEL_ENGINE:
                return new FuelTaskPanel(this);
            case CHART_COURSE:
                return new NavigationTaskPanel(this);
            default:
                return new JPanel(); // Empty panel as fallback
        }
    }

    public void completeTask(Task task) {
        if (currentTask == task) {
            task.complete();
            currentTask = null;

            // Close task dialog
            if (taskDialog != null) {
                taskDialog.dispose();
                taskDialog = null;
            }

            // Update UI
            updateTaskPanel();

            // Notify network if all tasks are complete
            if (areAllTasksComplete()) {
                networkManager.sendTaskCompletion();
            }
        }
    }

    public void cancelTask() {
        currentTask = null;
        if (taskDialog != null) {
            taskDialog.dispose();
            taskDialog = null;
        }
    }

    public double getTaskProgress() {
        if (playerTasks.isEmpty()) return 0.0;

        long completedTasks = playerTasks.stream()
                .filter(Task::isCompleted)
                .count();

        return (double) completedTasks / playerTasks.size();
    }

    public boolean areAllTasksComplete() {
        return playerTasks.stream().allMatch(Task::isCompleted);
    }

    public Task getCurrentTask() {
        return currentTask;
    }

    public List<Task> getPlayerTasks() {
        return new ArrayList<>(playerTasks);
    }

    public JPanel getTaskPanel() {
        return taskPanel;
    }

    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    // Task Type Enum
    public enum TaskType {
        WIRES("Fix Wiring"),
        UPLOAD("Upload Data"),
        SCAN("Submit Scan"),
        CALIBRATE("Calibrate Distributor"),
        PRIME_SHIELDS("Prime Shields"),
        CLEAN_FILTER("Clean O2 Filter"),
        FUEL_ENGINE("Fuel Engines"),
        CHART_COURSE("Chart Course");

        private final String name;

        TaskType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}