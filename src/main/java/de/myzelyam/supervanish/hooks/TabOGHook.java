/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.hooks;

import de.myzelyam.api.vanish.PostPlayerHideEvent;
import de.myzelyam.api.vanish.PostPlayerShowEvent;
import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.visibility.VanishStateMgr;

import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.integration.VanishIntegration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.UUID;

public class TabOGHook extends PluginHook {

    private VanishOGIntegration integration;

    public TabOGHook(SuperVanish superVanish) {

        super(superVanish);

    }

    @Override
    public void onPluginEnable(Plugin plugin) {

        // Do not double-register if an earlier lifecycle already registered one.
        unregisterIfPresent();
        integration = new VanishOGIntegration(superVanish);
        integration.register();

    }

    @Override
    public void onPluginDisable(Plugin plugin) {

        unregisterIfPresent();

    }

    // Called from SuperVanish.onDisable() so the integration is removed from
    // TAB-OG's static HANDLERS list before the plugin's state is torn down.
    // Without this, TAB-OG keeps a stale handler whose VanishStateMgr has a
    // closed Redis connection, which then throws on every canSee() call and
    // corrupts packet writes during subsequent logins.
    public void onSuperVanishDisable() {

        unregisterIfPresent();

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanish(PostPlayerHideEvent event) {

        notifyTabFeatures(event.getPlayer().getUniqueId());

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReappear(PostPlayerShowEvent event) {

        notifyTabFeatures(event.getPlayer().getUniqueId());

    }

    private void unregisterIfPresent() {

        if (integration != null) {

            try {

                integration.unregister();

            } catch (Throwable ignored) {

                // TAB-OG may already be unloaded; removing a missing handler is harmless.
            }

            integration = null;

        }

    }

    private void notifyTabFeatures(UUID playerUuid) {

        // Fire once immediately for metadata-based TAB-OG features and once after
        // BackendTabPlayer's vanish cache window so cache-backed features refresh too.
        Bukkit.getScheduler().runTask(superVanish, () -> notifyTabFeaturesNow(playerUuid));
        Bukkit.getScheduler().runTaskLater(superVanish, () -> notifyTabFeaturesNow(playerUuid), 20L);

    }

    private void notifyTabFeaturesNow(UUID playerUuid) {

        if (integration == null) {

            return;

        }

        try {

            Class<?> tabClass = Class.forName("me.neznamy.tab.shared.TAB");
            Object tab = tabClass.getMethod("getInstance").invoke(null);
            if (tab == null) {

                return;

            }

            Object tabPlayer = tabClass.getMethod("getPlayer", UUID.class).invoke(tab, playerUuid);
            if (tabPlayer == null) {

                return;

            }

            Object featureManager = tabClass.getMethod("getFeatureManager").invoke(tab);
            Class<?> tabPlayerClass = Class.forName("me.neznamy.tab.shared.platform.TabPlayer");
            Method vanishChangeMethod = featureManager.getClass().getMethod("onVanishStatusChange", tabPlayerClass);
            vanishChangeMethod.invoke(featureManager, tabPlayer);

        } catch (ReflectiveOperationException ignored) {

            // TAB-OG implementation internals are optional and may differ across versions.

        } catch (Throwable t) {

            superVanish.logException(t);

        }

    }

    private static final class VanishOGIntegration extends VanishIntegration {

        private final SuperVanish plugin;

        private VanishOGIntegration(SuperVanish plugin) {

            super("Vanish-OG");
            this.plugin = plugin;

        }

        // TAB-OG invokes this on its async CPU-manager thread during join
        // processing, so every field access must tolerate partial-enable and
        // partial-disable states. Any exception thrown here propagates through
        // TAB-OG's feature pipeline and breaks player login packet flow.
        @Override
        public boolean canSee(@NotNull TabPlayer viewer, @NotNull TabPlayer target) {

            try {

                final VanishStateMgr stateMgr = plugin.getVanishStateMgr();
                if (stateMgr == null)
                    return true;
                if (!stateMgr.isVanished(target.getUniqueId()))
                    return true;
                final Player bukkitViewer = Bukkit.getPlayer(viewer.getUniqueId());
                if (bukkitViewer == null)
                    return true;
                // Direct permission check only: plugin.hasPermissionToSee() reaches
                // LayeredPermissionChecker, which iterates the non-thread-safe
                // vanishPlayers HashSet on SuperVanish. That's safe on the Bukkit
                // main thread but not here, so we stick to Player.hasPermission.
                return bukkitViewer.hasPermission("sv.see");

            } catch (Throwable t) {

                plugin.logException(t);
                return true;

            }

        }

        @Override
        public boolean isVanished(@NotNull TabPlayer player) {

            try {

                final VanishStateMgr stateMgr = plugin.getVanishStateMgr();
                if (stateMgr == null)
                    return false;
                return stateMgr.isVanished(player.getUniqueId());

            } catch (Throwable t) {

                plugin.logException(t);
                return false;

            }

        }

    }

}
