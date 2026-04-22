/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.listeners;

import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.commands.CommandAction;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class LoginListener implements Listener {

    private final SuperVanish plugin;

    public LoginListener(SuperVanish plugin) {

        this.plugin = plugin;

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent e) {

        try {

            if (e.getResult() != PlayerLoginEvent.Result.ALLOWED)
                return;
            final Player p = e.getPlayer();
            boolean persistedVanished = plugin.getVanishStateMgr().isVanished(p.getUniqueId());
            boolean itemPickUps = plugin.getVanishStateMgr().getItemPickUps(p.getUniqueId(),
                    plugin.getSettings().getBoolean("InvisibilityFeatures.DefaultPickUpItemsOption"));
            plugin.createVanishPlayer(p, itemPickUps);
            if (persistedVanished
                    && plugin.getSettings().getBoolean("VanishStateFeatures.CheckPermissionOnLogin", false)
                    && !CommandAction.VANISH_SELF.checkPermission(p, plugin))
            {

                // Persist the auto-unvanish so reconcileVisibility() and TAB-OG's
                // canSee() do not keep hiding the player based on stale saved state.
                plugin.getVanishStateMgr().setVanishedState(p.getUniqueId(), p.getName(), false, null);
                persistedVanished = false;

            }

            if (persistedVanished) {

                p.setMetadata("vanished", new FixedMetadataValue(plugin, true));

            } else {

                p.removeMetadata("vanished", plugin);

            }

            plugin.reconcileVisibility(p);

        } catch (Exception er) {

            plugin.logException(er);

        }

    }

}
