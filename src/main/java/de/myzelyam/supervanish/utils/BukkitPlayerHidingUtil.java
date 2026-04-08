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

    public static boolean isNewPlayerHidingAPISupported(SuperVanish plugin) {

        return plugin.getVersionUtil().isOneDotXOrHigher(19);

    }

}
