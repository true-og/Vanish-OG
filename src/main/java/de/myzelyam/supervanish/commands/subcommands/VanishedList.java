/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.commands.subcommands;

import com.google.common.collect.ImmutableList;

import de.myzelyam.supervanish.PlaceholderConverter;
import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.commands.CommandAction;
import de.myzelyam.supervanish.commands.SubCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

public class VanishedList extends SubCommand {

    public VanishedList(SuperVanish plugin) {

        super(plugin);

    }

    @Override
    public void execute(Command cmd, final CommandSender sender, final String[] args, String label) {

        if (!canDo(sender, CommandAction.VANISHED_LIST, true)) {

            return;

        }

        String listMessage = plugin.getMessage("ListMessagePrefix");
        final StringBuilder stringBuilder = new StringBuilder();
        final List<UUID> allInvisiblePlayerUUIDs = ImmutableList.copyOf(getAllVanishedPlayers());
        if (allInvisiblePlayerUUIDs.isEmpty()) {

            stringBuilder.append("none");

        }

        for (int i = 0; i < allInvisiblePlayerUUIDs.size(); i++) {

            final UUID playerUUID = allInvisiblePlayerUUIDs.get(i);
            String name = Bukkit.getOfflinePlayer(playerUUID).getName();
            if (Bukkit.getPlayer(playerUUID) == null) {

                name = LegacyComponentSerializer.legacySection()
                        .serialize(MiniMessage.miniMessage().deserialize(name + "<red>[offline]<white>"));

            }

            stringBuilder.append(name);
            if (i != allInvisiblePlayerUUIDs.size() - 1) {

                stringBuilder.append(LegacyComponentSerializer.legacySection()
                        .serialize(MiniMessage.miniMessage().deserialize("<green>, <white>")));

            }

        }

        plugin.sendMessage(sender, listMessage, sender,
                PlaceholderConverter.placeholder("vanished_list", stringBuilder.toString()));

    }

}
