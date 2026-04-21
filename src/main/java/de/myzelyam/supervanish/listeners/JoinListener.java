/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.listeners;

import de.myzelyam.supervanish.SuperVanish;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.scheduler.BukkitRunnable;

public class JoinListener implements EventExecutor, Listener {

    private final SuperVanish plugin;

    public JoinListener(SuperVanish plugin) {

        this.plugin = plugin;

    }

    @Override
    public void execute(Listener l, Event event) {

        try {

            if (event instanceof PlayerJoinEvent) {

                PlayerJoinEvent e = (PlayerJoinEvent) event;
                final Player p = e.getPlayer();
                // vanished:
                if (plugin.getVanishStateMgr().isVanished(p.getUniqueId())) {

                    // reminding message
                    if (plugin.getSettings().getBoolean("MessageOptions.RemindVanishedOnJoin")) {

                        plugin.sendMessage(p, "RemindingMessage", p);

                    }

                    // re-add action bar
                    if (plugin.getActionBarMgr() != null
                            && plugin.getSettings().getBoolean("MessageOptions.DisplayActionBar"))
                    {

                        plugin.getActionBarMgr().addActionBar(p);

                    }

                    // sleep state
                    p.setSleepingIgnored(true);
                    // adjust fly
                    if (plugin.getSettings().getBoolean("InvisibilityFeatures.Fly.Enable")) {

                        p.setAllowFlight(true);

                    }

                    // metadata
                    p.setMetadata("vanished", new FixedMetadataValue(plugin, true));

                } else {

                    // not vanished:
                    // metadata
                    p.removeMetadata("vanished", plugin);

                }

                plugin.reconcileVisibility(p);

                // not necessarily vanished:
                // recreate files msg
                if ((p.hasPermission("sv.recreatecfg") || p.hasPermission("sv.recreatefiles"))
                        && plugin.getConfigMgr().isMessagesUpdateRequired())
                {

                    String currentVersion = plugin.getPluginMeta().getVersion();
                    boolean isDismissed = plugin.getVanishStateMgr().isDismissed(p.getUniqueId().toString(),
                            currentVersion);
                    if (!isDismissed)
                        new BukkitRunnable() {

                            @Override
                            public void run() {

                                plugin.sendMessage(p, "RecreationRequiredMsg", p);

                            }

                        }.runTaskLater(plugin, 1);

                }

            }

        } catch (Exception er) {

            plugin.logException(er);

        }

    }

}
