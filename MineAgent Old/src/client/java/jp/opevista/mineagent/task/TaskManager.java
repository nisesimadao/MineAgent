package jp.opevista.mineagent.task;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class TaskManager {
    private final List<String> logs = new ArrayList<>();
    private AgentTask currentTask;

    public synchronized AgentTask startTask(String source, String user, String instruction) {
        currentTask = new AgentTask(source, user, instruction);
        log("instruction source=" + source + " user=" + user + " text=" + instruction);
        return currentTask;
    }

    public synchronized void stopCurrent(String reason) {
        if (currentTask != null && currentTask.state() == TaskState.RUNNING) {
            currentTask.state(TaskState.STOPPED);
        }
        log("task stopped reason=" + reason);
    }

    public synchronized AgentTask currentTask() {
        return currentTask;
    }

    public synchronized void log(String message) {
        logs.add("[" + LocalTime.now().withNano(0) + "] " + message);
        if (logs.size() > 500) {
            logs.removeFirst();
        }
    }

    public synchronized List<String> recentLogs(int limit) {
        int from = Math.max(0, logs.size() - Math.max(1, limit));
        return List.copyOf(logs.subList(from, logs.size()));
    }

    public synchronized String statusText() {
        if (currentTask == null) {
            return "MineAgent idle";
        }
        return "MineAgent task " + currentTask.id() + " " + currentTask.state() + " step=" + currentTask.step()
                + " instruction=" + currentTask.instruction();
    }
}
