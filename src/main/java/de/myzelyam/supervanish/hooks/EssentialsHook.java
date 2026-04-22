/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.hooks;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import de.myzelyam.api.vanish.PlayerHideEvent;
import de.myzelyam.api.vanish.PostPlayerShowEvent;
import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.commands.CommandAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class EssentialsHook extends PluginHook {

    private final Set<UUID> preVanishHiddenPlayers = new HashSet<>();
    private Essentials essentials;
    private BukkitRunnable forcedInvisibilityRunnable = new BukkitRunnable() {

        @Override
        public void run() {

            try {

                if (!Bukkit.getPluginManager().isPluginEnabled("Essentials"))
                    return;
                for (UUID uuid : superVanish.getVanishStateMgr().getOnlineVanishedPlayers()) {

                    Player p = Bukkit.getPlayer(uuid);
                    User user = essentials.getUser(p);
                    if (user == null)
                        continue;
                    if (!user.isHidden())
                        user.setHidden(true);

                }

            } catch (Exception e) {

                cancel();
                superVanish.logException(e);

            }

        }

    };

    private BukkitTask forcedInvisibilityTask;

    public EssentialsHook(SuperVanish superVanish) {

        super(superVanish);

    }

    @Override
    public void onPluginEnable(Plugin plugin) {

        essentials = (Essentials) plugin;
        forcedInvisibilityTask = forcedInvisibilityRunnable.runTaskTimer(superVanish, 0, 100);
        forcedInvisibilityRunnable.run();

    }

    @Override
    public void onPluginDisable(Plugin plugin) {

        essentials = null;
        forcedInvisibilityTask.cancel();

    }

    /**
     * Essentials tracks its own vanish list in {@code getVanishedPlayersNew()} and
     * re-hides its members from every new joiner via the deprecated
     * {@code hidePlayer(Player)} API (see EssentialsPlayerListener#onPlayerJoin).
     * That deprecated call parks a {@code null} plugin ref in CraftPlayer's
     * invertedVisibilityEntities map, which Vanish-OG's plugin-scoped
     * {@code showPlayer(Plugin, Player)} cannot clear. So {@code /sv cleanup} must
     * drive Essentials' state back to "not vanished" on every online user — that's
     * the only call that removes the entry from Essentials' list *and* fires the
     * deprecated {@code showPlayer(Player)} that drops the null ref.
     */
    @Override
    public void onCleanup() {

        if (essentials == null)
            return;
        for (Player online : Bukkit.getOnlinePlayers()) {

            try {

                User user = essentials.getUser(online);
                if (user == null)
                    continue;
                if (user.isVanished())
                    user.setVanished(false);
                if (user.isHidden())
                    user.setHidden(false);

            } catch (Exception e) {

                superVanish.logException(e);

            }

        }

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent e) {

        User user = essentials.getUser(e.getPlayer());
        if (user == null)
            return;
        if (superVanish.getVanishStateMgr().isVanished(e.getPlayer().getUniqueId()) && !user.isHidden())
            user.setHidden(true);
        else
            user.setHidden(false);

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanish(PlayerHideEvent e) {

        User user = essentials.getUser(e.getPlayer());
        if (user == null)
            return;
        if (user.isVanished())
            user.setVanished(false);
        preVanishHiddenPlayers.remove(e.getPlayer().getUniqueId());
        user.setHidden(true);

    }

    @EventHandler
    public void onReappear(PostPlayerShowEvent e) {

        User user = essentials.getUser(e.getPlayer());
        if (user == null)
            return;
        user.setHidden(false);

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(final PlayerCommandPreprocessEvent e) {

        if (!CommandAction.VANISH_SELF.checkPermission(e.getPlayer(), superVanish))
            return;
        if (superVanish.getVanishStateMgr().isVanished(e.getPlayer().getUniqueId()))
            return;
        String command = e.getMessage().toLowerCase(Locale.ENGLISH).split(" ")[0].replace("/", "")
                .toLowerCase(Locale.ENGLISH);
        if (command.split(":").length > 1)
            command = command.split(":")[1];
        if (command.equals("supervanish") || command.equals("sv") || command.equals("v") || command.equals("vanish")) {

            final User user = essentials.getUser(e.getPlayer());
            if (user == null || !user.isAfk())
                return;
            user.setHidden(true);
            preVanishHiddenPlayers.add(e.getPlayer().getUniqueId());
            superVanish.getServer().getScheduler().runTaskLater(superVanish, new Runnable() {

                @Override
                public void run() {

                    if (preVanishHiddenPlayers.remove(e.getPlayer().getUniqueId())) {

                        user.setHidden(false);

                    }

                }

            }, 1);

        }

    }

}
