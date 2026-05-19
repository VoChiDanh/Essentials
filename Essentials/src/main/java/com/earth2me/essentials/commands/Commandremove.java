package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Mob;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.VersionUtil;
import com.google.common.collect.Lists;
import net.ess3.api.TranslatableException;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ComplexLivingEntity;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Flying;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.WaterMob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

// This could be rewritten in a simpler form if we made a mapping of all Entity names to their types (which would also provide possible mod support)

public class Commandremove extends EssentialsCommand {
    public Commandremove() {
        super("remove");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        World world = user.getWorld();
        int radius = 0;
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        if (args.length >= 2) {
            try {
                radius = Integer.parseInt(args[1]);
            } catch (final NumberFormatException e) {
                world = ess.getWorld(args[1]);
            }
        }
        if (args.length >= 3) {
            // This is to prevent breaking the old syntax
            radius = 0;
            world = ess.getWorld(args[2]);
        }
        parseCommand(server, user.getSource(), args, world, radius);

    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final World world = ess.getWorld(args[1]);
        parseCommand(server, sender, args, world, 0);
    }

    private void parseCommand(final Server server, final CommandSource sender, final String[] args, final World world, final int radius) throws Exception {
        final List<String> types = new ArrayList<>();
        final List<String> customTypes = new ArrayList<>();

        if (world == null) {
            throw new TranslatableException("invalidWorld");
        }

        if (args[0].contentEquals("*") || args[0].contentEquals("all")) {
            types.add(0, "ALL");
        } else {
            for (final String entityType : args[0].split(",")) {
                ToRemove toRemove;
                try {
                    toRemove = ToRemove.valueOf(entityType.toUpperCase(Locale.ENGLISH));
                } catch (final Exception e) {
                    try {
                        toRemove = ToRemove.valueOf(entityType.concat("S").toUpperCase(Locale.ENGLISH));
                    } catch (final Exception ee) {
                        toRemove = ToRemove.CUSTOM;
                        customTypes.add(entityType);
                    }
                }
                types.add(toRemove.toString());
            }
        }
        removeHandler(sender, types, customTypes, world, radius);
    }

    private void removeHandler(final CommandSource sender, final List<String> types, final List<String> customTypes, final World world, int radius) {
        if (radius > 0) {
            radius *= radius;
        }
        final int radiusSquared = radius;
        final Location senderLocation = radiusSquared > 0 && sender.isPlayer() ? sender.getPlayer().getLocation() : null;

        final ArrayList<ToRemove> removeTypes = new ArrayList<>();
        final ArrayList<Mob> customRemoveTypes = new ArrayList<>();

        for (final String s : types) {
            removeTypes.add(ToRemove.valueOf(s));
        }

        boolean warnUser = false;

        for (final String s : customTypes) {
            final Mob mobType = Mob.fromName(s);
            if (mobType == null) {
                warnUser = true;
            } else {
                customRemoveTypes.add(mobType);
            }
        }

        if (warnUser) {
            sender.sendTl("invalidMob");
        }

        if (VersionUtil.isFoliaServer()) {
            removeFolia(sender, removeTypes, customRemoveTypes, world, radiusSquared, senderLocation);
            return;
        }

        int removed = 0;
        for (final Chunk chunk : world.getLoadedChunks()) {
            for (final Entity e : chunk.getEntities()) {
                if (radiusSquared > 0) {
                    if (senderLocation.distanceSquared(e.getLocation()) > radiusSquared) {
                        continue;
                    }
                }
                if (e instanceof HumanEntity) {
                    continue;
                }

                if (shouldRemove(e, removeTypes, customRemoveTypes)) {
                    e.remove();
                    removed++;
                }
            }
        }
        sender.sendTl("removed", removed);
    }

    private void removeFolia(final CommandSource sender, final ArrayList<ToRemove> removeTypes, final ArrayList<Mob> customRemoveTypes, final World world, final int radiusSquared, final Location senderLocation) {
        final Chunk[] chunks = world.getLoadedChunks();
        if (chunks.length == 0) {
            sender.sendTl("removed", 0);
            return;
        }

        final AtomicInteger removed = new AtomicInteger();
        final AtomicInteger remaining = new AtomicInteger(chunks.length);

        for (final Chunk chunk : chunks) {
            ess.runTaskAtChunk(world, chunk.getX(), chunk.getZ(), () -> {
                for (final Entity entity : chunk.getEntities()) {
                    if (radiusSquared > 0 && senderLocation.distanceSquared(entity.getLocation()) > radiusSquared) {
                        continue;
                    }
                    if (entity instanceof HumanEntity) {
                        continue;
                    }
                    if (shouldRemove(entity, removeTypes, customRemoveTypes)) {
                        entity.remove();
                        removed.incrementAndGet();
                    }
                }
                if (remaining.decrementAndGet() == 0) {
                    ess.scheduleSyncDelayedTask(() -> sender.sendTl("removed", removed.get()));
                }
            });
        }
    }

    private boolean shouldRemove(final Entity entity, final ArrayList<ToRemove> removeTypes, final ArrayList<Mob> customRemoveTypes) {
        // We should skip any animals tamed by players unless we are specifically targeting them.
        if (entity instanceof Tameable && ((Tameable) entity).isTamed() && (((Tameable) entity).getOwner() instanceof Player || ((Tameable) entity).getOwner() instanceof OfflinePlayer) && !removeTypes.contains(ToRemove.TAMED)) {
            return false;
        }

        // We should skip any named animals unless we are specifically targeting them.
        if (entity instanceof LivingEntity && entity.getCustomName() != null && !removeTypes.contains(ToRemove.NAMED)) {
            return false;
        }

        for (final ToRemove toRemove : removeTypes) {
            switch (toRemove) {
                case TAMED:
                    if (entity instanceof Tameable && ((Tameable) entity).isTamed()) {
                        return true;
                    }
                    break;
                case NAMED:
                    if (entity instanceof LivingEntity && entity.getCustomName() != null) {
                        return true;
                    }
                    break;
                case DROPS:
                    if (entity instanceof Item) {
                        return true;
                    }
                    break;
                case ARROWS:
                    if (entity instanceof Projectile) {
                        return true;
                    }
                    break;
                case BOATS:
                    if (entity instanceof Boat) {
                        return true;
                    }
                    break;
                case MINECARTS:
                    if (entity instanceof Minecart) {
                        return true;
                    }
                    break;
                case XP:
                    if (entity instanceof ExperienceOrb) {
                        return true;
                    }
                    break;
                case PAINTINGS:
                    if (entity instanceof Painting) {
                        return true;
                    }
                    break;
                case ITEMFRAMES:
                    if (entity instanceof ItemFrame) {
                        return true;
                    }
                    break;
                case ENDERCRYSTALS:
                    if (entity instanceof EnderCrystal) {
                        return true;
                    }
                    break;
                case AMBIENT:
                    if (entity instanceof Flying) {
                        return true;
                    }
                    break;
                case HOSTILE:
                case MONSTERS:
                    if (entity instanceof Monster || entity instanceof ComplexLivingEntity || entity instanceof Flying || entity instanceof Slime) {
                        return true;
                    }
                    break;
                case PASSIVE:
                case ANIMALS:
                    if (entity instanceof Animals || entity instanceof NPC || entity instanceof Snowman || entity instanceof WaterMob || entity instanceof Ambient) {
                        return true;
                    }
                    break;
                case MOBS:
                    if (entity instanceof Animals || entity instanceof NPC || entity instanceof Snowman || entity instanceof WaterMob || entity instanceof Monster || entity instanceof ComplexLivingEntity || entity instanceof Flying || entity instanceof Slime || entity instanceof Ambient) {
                        return true;
                    }
                    break;
                case ENTITIES:
                case ALL:
                    return true;
                case CUSTOM:
                    for (final Mob type : customRemoveTypes) {
                        if (entity.getType() == type.getType()) {
                            return true;
                        }
                    }
                    break;
            }
        }
        return false;
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = Lists.newArrayList();
            for (final ToRemove toRemove : ToRemove.values()) {
                options.add(toRemove.name().toLowerCase(Locale.ENGLISH));
            }
            return options;
        } else if (args.length == 2) {
            final List<String> worlds = Lists.newArrayList();
            for (final World world : server.getWorlds()) {
                worlds.add(world.getName());
            }
            return worlds;
        } else {
            return Collections.emptyList();
        }
    }

    private enum ToRemove {
        DROPS,
        ARROWS,
        BOATS,
        MINECARTS,
        XP,
        PAINTINGS,
        ITEMFRAMES,
        ENDERCRYSTALS,
        HOSTILE,
        MONSTERS,
        PASSIVE,
        ANIMALS,
        AMBIENT,
        MOBS,
        ENTITIES,
        ALL,
        CUSTOM,
        TAMED,
        NAMED
    }
}
