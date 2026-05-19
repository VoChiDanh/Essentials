package net.ess3.provider.providers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class FoliaBukkitTask implements BukkitTask {
    private final int taskId;
    private final Plugin owner;
    private final boolean sync;
    private final Runnable cleanup;
    private final AtomicReference<ScheduledTask> task = new AtomicReference<>();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    FoliaBukkitTask(final int taskId, final Plugin owner, final boolean sync, final Runnable cleanup) {
        this.taskId = taskId;
        this.owner = owner;
        this.sync = sync;
        this.cleanup = cleanup;
    }

    void setTask(final ScheduledTask scheduledTask) {
        task.set(scheduledTask);
        if (cancelled.get()) {
            scheduledTask.cancel();
        }
    }

    @Override
    public int getTaskId() {
        return taskId;
    }

    @Override
    public Plugin getOwner() {
        return owner;
    }

    @Override
    public boolean isSync() {
        return sync;
    }

    @Override
    public void cancel() {
        cancelled.set(true);
        try {
            final ScheduledTask scheduledTask = task.get();
            if (scheduledTask != null) {
                scheduledTask.cancel();
            }
        } finally {
            cleanup.run();
        }
    }

    @Override
    public boolean isCancelled() {
        final ScheduledTask scheduledTask = task.get();
        return cancelled.get() || scheduledTask != null && scheduledTask.isCancelled();
    }
}
