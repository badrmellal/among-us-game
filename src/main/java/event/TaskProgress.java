package event;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskProgress implements Serializable {
    private Map<String, Boolean> completedTasks = new ConcurrentHashMap<>();
    private int totalTasks;
    private AtomicInteger completedCount = new AtomicInteger(0);

    public synchronized void completeTask(String taskId) {
        if (!completedTasks.containsKey(taskId)) {
            completedTasks.put(taskId, true);
            completedCount.incrementAndGet();
        }
    }

    public synchronized boolean isComplete() {
        return completedCount.get() >= totalTasks;
    }

    public double getCompletionPercentage() {
        return (double) completedCount.get() / totalTasks * 100;
    }
}