package de.myzelyam.supervanish.utils;

import de.myzelyam.supervanish.SuperVanish;

import org.bukkit.entity.Player;

public class BukkitPlayerHidingUtil {

    private BukkitPlayerHidingUtil() {

    }

    public static void hidePlayer(Player player, Player viewer, SuperVanish plugin) {

        try {

            if (isNewPlayerHidingAPISupported(plugin)) {

                Player.class.getMethod("hidePlayer", org.bukkit.plugin.Plugin.class, Player.class).invoke(viewer,
                        plugin, player);

            } else {

                Player.class.getMethod("hidePlayer", Player.class).invoke(viewer, player);

            }

        } catch (ReflectiveOperationException e) {

            throw new RuntimeException("Failed to hide player " + player.getName() + " from " + viewer.getName(), e);

        }

    }

    public static void showPlayer(Player player, Player viewer, SuperVanish plugin) {

        try {

            if (isNewPlayerHidingAPISupported(plugin)) {

                Player.class.getMethod("showPlayer", org.bukkit.plugin.Plugin.class, Player.class).invoke(viewer,
                        plugin, player);

            } else {

                Player.class.getMethod("showPlayer", Player.class).invoke(viewer, player);

            }

        } catch (ReflectiveOperationException e) {

            throw new RuntimeException("Failed to show player " + player.getName() + " to " + viewer.getName(), e);

        }

    }

    /**
     * Nuclear show: clears every hide ref that Vanish-OG could plausibly be
     * responsible for on this (viewer, player) pair. CraftPlayer tracks hides
     * per-plugin in {@code invertedVisibilityEntities}, and an entity stays hidden
     * until *all* refs in its set are gone. Essentials'
     * {@code User#setVanished(true)} and
     * {@code EssentialsPlayerListener#onPlayerJoin} both call the deprecated
     * {@code hidePlayer(Player)} form, which registers a {@code null} plugin ref
     * that Vanish-OG's normal {@code showPlayer(Plugin, Player)} cannot remove — so
     * {@code /sv cleanup} must also call the deprecated {@code showPlayer(Player)}
     * to drop it. Without this step, players stay invisible to each other even
     * after cleanup wipes KeyDB and Vanish-OG's own hide map.
     */
    public static void showPlayerAggressive(Player player, Player viewer, SuperVanish plugin) {

        showPlayer(player, viewer, plugin);
        if (isNewPlayerHidingAPISupported(plugin)) {

            try {

                Player.class.getMethod("showPlayer", Player.class).invoke(viewer, player);

            } catch (ReflectiveOperationException e) {

                throw new RuntimeException("Failed to legacy-show " + player.getName() + " to " + viewer.getName(), e);

            }

        }

    }

    public static boolean isNewPlayerHidingAPISupported(SuperVanish plugin) {

        return plugin.getVersionUtil().isOneDotXOrHigher(19);

    }

}
