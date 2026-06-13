package jp.opevista.mineagent.task;

import java.time.Instant;
import java.util.UUID;

public final class AgentTask {
    private final String id = "task-" + UUID.randomUUID();
    private final Instant createdAt = Instant.now();
    private final String source;
    private final String user;
    private final String instruction;
    private TaskState state = TaskState.RUNNING;
    private int step;

    public AgentTask(String source, String user, String instruction) {
        this.source = source;
        this.user = user;
        this.instruction = instruction;
    }

    public String id() {
        return id;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String source() {
        return source;
    }

    public String user() {
        return user;
    }

    public String instruction() {
        return instruction;
    }

    public TaskState state() {
        return state;
    }

    public void state(TaskState state) {
        this.state = state;
    }

    public int step() {
        return step;
    }

    public int nextStep() {
        return ++step;
    }
}
