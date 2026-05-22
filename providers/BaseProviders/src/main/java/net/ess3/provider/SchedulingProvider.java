package net.ess3.provider;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

public interface SchedulingProvider extends Provider {
    BukkitTask runTaskAsynchronously(Runnable run);

    BukkitTask runTaskLaterAsynchronously(Runnable run, long delay);

    BukkitTask runTaskTimerAsynchronously(Runnable run, long delay, long period);

    void runTaskForEntity(Entity entity, Runnable run);

    BukkitTask runTaskLaterForEntity(Entity entity, Runnable run, long delay);

    BukkitTask runTaskTimerForEntity(Entity entity, Runnable run, long delay, long period);

    void runTaskAtLocation(Location location, Runnable run);

    BukkitTask runTaskLaterAtLocation(Location location, Runnable run, long delay);

    void runTaskAtChunk(World world, int chunkX, int chunkZ, Runnable run);

    int scheduleSyncDelayedTask(Runnable run);

    int scheduleSyncDelayedTask(Runnable run, long delay);

    int scheduleSyncRepeatingTask(Runnable run, long delay, long period);

    void cancelTask(int taskId);
}
