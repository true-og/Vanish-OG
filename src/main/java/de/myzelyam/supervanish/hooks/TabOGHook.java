/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.hooks;

import de.myzelyam.supervanish.SuperVanish;

import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.integration.VanishIntegration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class TabOGHook extends PluginHook {

    private VanishIntegration integration;

    public TabOGHook(SuperVanish superVanish) {

        super(superVanish);

    }

    @Override
    public void onPluginEnable(Plugin plugin) {

        integration = new VanishOGIntegration(superVanish);
        integration.register();

    }

    @Override
    public void onPluginDisable(Plugin plugin) {

        if (integration != null) {

            integration.unregister();
            integration = null;

        }

    }

    private static final class VanishOGIntegration extends VanishIntegration {

        private final SuperVanish plugin;

        private VanishOGIntegration(SuperVanish plugin) {

            super("Vanish-OG");
            this.plugin = plugin;

        }

        @Override
        public boolean canSee(@NotNull TabPlayer viewer, @NotNull TabPlayer target) {

            Player bukkitViewer = Bukkit.getPlayer(viewer.getUniqueId());
            Player bukkitTarget = Bukkit.getPlayer(target.getUniqueId());
            if (bukkitViewer == null || bukkitTarget == null)
                return true;
            if (!plugin.getVanishStateMgr().isVanished(bukkitTarget.getUniqueId()))
                return true;
            return plugin.hasPermissionToSee(bukkitViewer, bukkitTarget);

        }

        @Override
        public boolean isVanished(@NotNull TabPlayer player) {

            return plugin.getVanishStateMgr().isVanished(player.getUniqueId());

        }

    }

}
