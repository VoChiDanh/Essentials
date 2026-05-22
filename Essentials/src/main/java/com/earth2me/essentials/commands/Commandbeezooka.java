package com.earth2me.essentials.commands;

import com.earth2me.essentials.Mob;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.VersionUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;

public class Commandbeezooka extends EssentialsCommand {

    public Commandbeezooka() {
        super("beezooka");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (VersionUtil.getServerBukkitVersion().isLowerThan(VersionUtil.v1_15_R01)) {
            user.sendTl("unsupportedFeature");
            return;
        }

        final Location spawnLocation = user.getBase().getEyeLocation();
        ess.runTaskAtLocation(spawnLocation, () -> {
            try {
                final Entity bee = Mob.BEE.spawn(user.getWorld(), server, spawnLocation);
                bee.setVelocity(spawnLocation.getDirection().multiply(2));

                ess.runTaskLaterForEntity(bee, () -> {
                    final Location loc = bee.getLocation();
                    bee.remove();
                    loc.getWorld().createExplosion(loc, 0F);
                }, 20);
            } catch (final Mob.MobException e) {
                user.sendTl("unableToSpawnMob");
            }
        });
    }

}
