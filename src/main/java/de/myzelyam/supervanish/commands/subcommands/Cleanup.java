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

import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.commands.CommandAction;
import de.myzelyam.supervanish.commands.SubCommand;
import de.myzelyam.supervanish.visibility.KeyDBVanishStateMgr;
import de.myzelyam.supervanish.visibility.VanishStateMgr;
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

        VanishStateMgr mgr = plugin.getVanishStateMgr();
        if (mgr instanceof KeyDBVanishStateMgr keyDbMgr) {

            keyDbMgr.clearAllVanishState();

        }

        if (plugin.getVisibilityChanger() != null) {

            plugin.forceShowAllPlayers();
            plugin.getVisibilityChanger().getHider().clearHiddenState();

        }

        for (Player online : Bukkit.getOnlinePlayers()) {

            online.removeMetadata("vanished", plugin);

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
