package net.ess3.provider.providers;

import net.ess3.provider.SchedulingProvider;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FoliaSchedulingProvider implements SchedulingProvider {
    private final Plugin plugin;
    private final AtomicInteger taskIds = new AtomicInteger(Integer.MIN_VALUE);
    private final Map<Integer, FoliaBukkitTask> tasks = new ConcurrentHashMap<>();

    public FoliaSchedulingProvider(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public BukkitTask runTaskAsynchronously(final Runnable run) {
        final int taskId = nextTaskId();
        final FoliaBukkitTask task = registerTask(taskId, false);
        task.setTask(plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> runOnce(taskId, run)));
        return task;
    }

    @Override
    public BukkitTask runTaskLaterAsynchronously(final Runnable run, final long delay) {
        final int taskId = nextTaskId();
        final FoliaBukkitTask task = registerTask(taskId, false);
        task.setTask(plugin.getServer().getAsyncScheduler().runDelayed(plugin, scheduledTask -> runOnce(taskId, run), ticksToMillis(delay), TimeUnit.MILLISECONDS));
        return task;
    }

    @Override
    public BukkitTask runTaskTimerAsynchronously(final Runnable run, final long delay, final long period) {
        final int taskId = nextTaskId();
        final FoliaBukkitTask task = registerTask(taskId, false);
        task.setTask(plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> run.run(), ticksToMillis(delay), ticksToMillis(period), TimeUnit.MILLISECONDS));
        return task;
    }

    @Override
    public void runTaskForEntity(final Entity entity, final Runnable run) {
        entity.getScheduler().execute(plugin, run, null, 1L);
    }

    @Override
    public BukkitTask runTaskTimerForEntity(final Entity entity, final Runnable run, final long delay, final long period) {
        final int taskId = nextTaskId();
        final FoliaBukkitTask task = registerTask(taskId, true);
        task.setTask(entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> run.run(), null, Math.max(1L, delay), Math.max(1L, period)));
        return task;
    }

    @Override
    public void runTaskAtLocation(final Location location, final Runnable run) {
        plugin.getServer().getRegionScheduler().execute(plugin, location, run);
    }

    @Override
    public void runTaskAtChunk(final World world, final int chunkX, final int chunkZ, final Runnable run) {
        plugin.getServer().getRegionScheduler().execute(plugin, world, chunkX, chunkZ, run);
    }

    @Override
    public int scheduleSyncDelayedTask(final Runnable run) {
        final int taskId = nextTaskId();
        final FoliaBukkitTask task = registerTask(taskId, true);
        task.setTask(plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> runOnce(taskId, run)));
        return taskId;
    }

    @Override
    public int scheduleSyncDelayedTask(final Runnable run, final long delay) {
        final int taskId = nextTaskId();
        final FoliaBukkitTask task = registerTask(taskId, true);
        task.setTask(plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> runOnce(taskId, run), Math.max(1L, delay)));
        return taskId;
    }

    @Override
    public int scheduleSyncRepeatingTask(final Runnable run, final long delay, final long period) {
        final int taskId = nextTaskId();
        final FoliaBukkitTask task = registerTask(taskId, true);
        task.setTask(plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> run.run(), Math.max(1L, delay), Math.max(1L, period)));
        return taskId;
    }

    @Override
    public void cancelTask(final int taskId) {
        final FoliaBukkitTask task = tasks.remove(taskId);
        if (task != null) {
            task.cancel();
        }
    }

    private int nextTaskId() {
        return taskIds.getAndIncrement();
    }

    private FoliaBukkitTask registerTask(final int taskId, final boolean sync) {
        final FoliaBukkitTask task = new FoliaBukkitTask(taskId, plugin, sync, () -> tasks.remove(taskId));
        tasks.put(taskId, task);
        return task;
    }

    private void runOnce(final int taskId, final Runnable run) {
        try {
            run.run();
        } finally {
            tasks.remove(taskId);
        }
    }

    private long ticksToMillis(final long ticks) {
        return Math.max(1L, ticks) * 50L;
    }
}
