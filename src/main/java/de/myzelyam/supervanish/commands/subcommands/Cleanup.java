/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.commands.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.commands.CommandAction;
import de.myzelyam.supervanish.commands.SubCommand;
import net.trueog.utilitiesog.UtilitiesOG;

public class Cleanup extends SubCommand {

    public Cleanup(SuperVanish plugin) {

        super(plugin);

    }

    @Override
    public void execute(Command cmd, CommandSender p, String[] args, String label) {

        if (!canDo(p, CommandAction.CLEANUP, true)) {

            return;

        }

        plugin.getVanishStateMgr().clearAllVanishState();

        // Must run before forceShowAllPlayers: Essentials' setVanished(false)
        // internally calls the deprecated showPlayer(Player) for every online
        // user, and we want our aggressive pass to finish after that so nothing
        // sneaks back in.
        if (plugin.getPluginHookMgr() != null) {

            plugin.getPluginHookMgr().onCleanup();

        }

        if (plugin.getVisibilityChanger() != null) {

            // Aggressive mode also drops the null-plugin-ref hide that Essentials
            // and other deprecated-API users leave in CraftPlayer's
            // invertedVisibilityEntities. Without it, CraftPlayer#canSee still
            // returns false because the set for the target UUID is non-empty.
            plugin.forceShowAllPlayers(true);
            plugin.getVisibilityChanger().getHider().clearHiddenState();

        }

        for (Player online : Bukkit.getOnlinePlayers()) {

            for (Plugin other : Bukkit.getPluginManager().getPlugins()) {

                // Each plugin owns its own "vanished" metadata entry; leaving any
                // of them populated keeps TAB-OG and other consumers treating the
                // player as vanished even though Vanish-OG has been wiped.
                online.removeMetadata("vanished", other);

            }

        }

        plugin.reconcileAllVisibilities();

        final String msg = "&aVanish state cleared. All players should now be visible and mutually damageable again.";
        if (p instanceof Player player) {

            UtilitiesOG.trueogMessage(player, msg);

        } else {

            p.sendMessage(msg);

        }

    }

}
