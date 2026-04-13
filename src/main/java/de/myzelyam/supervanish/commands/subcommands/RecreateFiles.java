/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.commands.subcommands;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.myzelyam.supervanish.PlaceholderConverter;
import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.commands.CommandAction;
import de.myzelyam.supervanish.commands.SubCommand;
import net.trueog.utilitiesog.UtilitiesOG;

public class RecreateFiles extends SubCommand {

    public RecreateFiles(SuperVanish plugin) {

        super(plugin);

    }

    @Override
    public void execute(Command cmd, CommandSender p, String[] args, String label) {

        if (!canDo(p, CommandAction.RECREATE_FILES, true)) {

            return;

        }

        String files = "the messages file";
        String changes = "all messages";

        if (args.length != 2) {

            if (!plugin.getConfigMgr().isMessagesUpdateRequired()) {

                plugin.sendMessage(p, "NoConfigUpdateAvailable", p);
                return;

            }

            plugin.sendMessage(p, plugin.getMessage("UpdateWarning"), p,
                    PlaceholderConverter.placeholder("changes", changes),
                    PlaceholderConverter.placeholder("files", files));
            return;

        }

        if (StringUtils.equalsIgnoreCase("confirm", args[1]) || StringUtils.equalsIgnoreCase(args[1], "force")) {

            if (!plugin.getConfigMgr().isMessagesUpdateRequired() && !StringUtils.equalsIgnoreCase(args[1], "force")) {

                plugin.sendMessage(p, "NoConfigUpdateAvailable", p);
                return;

            }

            if (StringUtils.equalsIgnoreCase(args[1], "force")) {

                files = "the config and the messages file";
                changes = "all settings and messages";

            }

            // Delete necessary files.
            boolean success = true;
            if (StringUtils.equalsIgnoreCase(args[1], "force")) {

                final File file = new File(plugin.getDataFolder().getPath() + File.separator + "config.yml");
                success = file.delete();
                plugin.getConfigMgr().getSettingsFile().save();
                plugin.getConfigMgr().getSettingsFile().reload();
                plugin.getConfigMgr().setSettings(plugin.getConfigMgr().getSettingsFile().getConfig());

            }

            if (plugin.getConfigMgr().isMessagesUpdateRequired() || StringUtils.equalsIgnoreCase(args[1], "force")) {

                final File file2 = new File(plugin.getDataFolder().getPath() + File.separator + "messages.yml");
                success &= file2.delete();

                plugin.getConfigMgr().getMessagesFile().save();
                plugin.getConfigMgr().getMessagesFile().reload();
                plugin.getConfigMgr().setMessages(plugin.getConfigMgr().getMessagesFile().getConfig());

            }

            if (!success) {

                final String failMsg = "&cERROR: Cannot recreate files because one of them could not be deleted. Are the file permissions valid?";
                if (p instanceof Player player) {

                    UtilitiesOG.trueogMessage(player, failMsg);

                } else {

                    p.sendMessage(failMsg);

                }

                return;

            }

            plugin.sendMessage(p, plugin.getMessage("RecreatedConfig"), p,
                    PlaceholderConverter.placeholder("changes", changes),
                    PlaceholderConverter.placeholder("files", files));

            // Refresh recreation status after regenerating the files.
            plugin.getConfigMgr().checkFilesForLeftOvers();

        } else if (StringUtils.equalsIgnoreCase("dismiss", args[1])) {

            final String currentVersion = plugin.getPluginMeta().getVersion();
            final String dismissId = p instanceof Player ? ((Player) p).getUniqueId().toString() : "CONSOLE";
            final boolean isDismissed = plugin.getVanishStateMgr().isDismissed(dismissId, currentVersion);
            plugin.getVanishStateMgr().setDismissed(dismissId, currentVersion, !isDismissed);
            if (!isDismissed) {

                plugin.sendMessage(p, "DismissedRecreationWarning", p);

            } else {

                plugin.sendMessage(p, "UndismissedRecreationWarning", p);

            }

        }

    }

}
