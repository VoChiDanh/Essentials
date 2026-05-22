package net.ess3.provider.providers;

import net.ess3.provider.SchedulingProvider;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class BukkitSchedulingProvider implements SchedulingProvider {
    private final Plugin plugin;

    public BukkitSchedulingProvider(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public BukkitTask runTaskAsynchronously(final Runnable run) {
        return plugin.getServer().getScheduler().runTaskAsynchronously(plugin, run);
    }

    @Override
    public BukkitTask runTaskLaterAsynchronously(final Runnable run, final long delay) {
        return plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, run, delay);
    }

    @Override
    public BukkitTask runTaskTimerAsynchronously(final Runnable run, final long delay, final long period) {
        return plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, run, delay, period);
    }

    @Override
    public void runTaskForEntity(final Entity entity, final Runnable run) {
        runSync(run);
    }

    @Override
    public BukkitTask runTaskLaterForEntity(final Entity entity, final Runnable run, final long delay) {
        return plugin.getServer().getScheduler().runTaskLater(plugin, run, delay);
    }

    @Override
    public BukkitTask runTaskTimerForEntity(final Entity entity, final Runnable run, final long delay, final long period) {
        return runTaskTimerAsynchronously(() -> runSync(run), delay, period);
    }

    @Override
    public void runTaskAtLocation(final Location location, final Runnable run) {
        runSync(run);
    }

    @Override
    public BukkitTask runTaskLaterAtLocation(final Location location, final Runnable run, final long delay) {
        return plugin.getServer().getScheduler().runTaskLater(plugin, run, delay);
    }

    @Override
    public void runTaskAtChunk(final World world, final int chunkX, final int chunkZ, final Runnable run) {
        runSync(run);
    }

    @Override
    public int scheduleSyncDelayedTask(final Runnable run) {
        return plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, run);
    }

    @Override
    public int scheduleSyncDelayedTask(final Runnable run, final long delay) {
        return plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, run, delay);
    }

    @Override
    public int scheduleSyncRepeatingTask(final Runnable run, final long delay, final long period) {
        return plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, run, delay, period);
    }

    @Override
    public void cancelTask(final int taskId) {
        plugin.getServer().getScheduler().cancelTask(taskId);
    }

    private void runSync(final Runnable run) {
        if (plugin.getServer().isPrimaryThread()) {
            run.run();
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, run);
    }
}
