/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import de.myzelyam.supervanish.utils.Validation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.trueog.utilitiesog.UtilitiesOG;

public class PlaceholderConverter {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    public PlaceholderConverter(SuperVanish plugin) {

    }

    public String replacePlaceholders(String msg, Object... additionalPlayerInfo) {

        return LEGACY_SERIALIZER.serialize(expandPlaceholders(msg, additionalPlayerInfo));

    }

    public Component expandPlaceholders(String msg, Object... additionalPlayerInfo) {

        return expandPlaceholders(null, msg, additionalPlayerInfo);

    }

    public Component expandPlaceholders(CommandSender recipient, String msg, Object... additionalPlayerInfo) {

        Validation.checkNotNull("Failed to replace variables (Illegal arguments)", msg, additionalPlayerInfo);
        final PlaceholderContext context = parseContext(additionalPlayerInfo);
        final String preparedMessage = applyInlinePlaceholders(msg.replace("\\n", "\n"), context);

        if (recipient instanceof Player player) {

            if (context.primaryPlayer != null) {

                return UtilitiesOG.trueogExpand(preparedMessage, player, context.primaryPlayer);

            }

            return UtilitiesOG.trueogExpand(preparedMessage, player);

        }

        if (context.primaryPlayer != null) {

            return UtilitiesOG.trueogExpand(preparedMessage, context.primaryPlayer);

        }

        return UtilitiesOG.trueogExpand(preparedMessage);

    }

    public static MessagePlaceholder placeholder(String name, Object value) {

        return new MessagePlaceholder(name, value == null ? "" : String.valueOf(value));

    }

    private PlaceholderContext parseContext(Object... additionalPlayerInfo) {

        final PlaceholderContext context = new PlaceholderContext();
        if (additionalPlayerInfo == null) {

            return context;

        }

        for (Object entry : additionalPlayerInfo) {

            if (entry instanceof MessagePlaceholder messagePlaceholder) {

                context.inlinePlaceholders.add(messagePlaceholder);
                continue;

            }

            if (context.primaryObject == null) {

                context.primaryObject = entry;
                if (entry instanceof Player player) {

                    context.primaryPlayer = player;

                }

                continue;

            }

            if (context.otherName == null && (entry instanceof Player || entry instanceof String)) {

                if (entry instanceof Player player) {

                    context.otherName = player.getName();

                } else {

                    context.otherName = String.valueOf(entry);

                }

            }

        }

        return context;

    }

    private String applyInlinePlaceholders(String message, PlaceholderContext context) {

        message = replaceTag(message, "sv_player_name", getPrimaryName(context.primaryObject));
        message = replaceTag(message, "sv_player_display_name", getPrimaryDisplayName(context.primaryObject));
        message = replaceTag(message, "sv_player_tab_name", getPrimaryTabName(context.primaryObject));
        message = replaceTag(message, "sv_player_prefix", getPrimaryPrefix(context.primaryObject));
        message = replaceTag(message, "sv_player_suffix", getPrimarySuffix(context.primaryObject));
        message = replaceTag(message, "sv_player_group", getPrimaryGroup(context.primaryObject));
        message = replaceTag(message, "sv_player_nick", getPrimaryNick(context.primaryObject));
        message = replaceTag(message, "sv_other_name", context.otherName != null ? context.otherName : "UNKNOWN");

        for (MessagePlaceholder inlinePlaceholder : context.inlinePlaceholders) {

            message = replaceTag(message, inlinePlaceholder.name(), inlinePlaceholder.value());

        }

        return message;

    }

    private String getPrimaryName(Object primaryObject) {

        if (primaryObject instanceof OfflinePlayer offlinePlayer) {

            return offlinePlayer.getName();

        }

        if (primaryObject instanceof CommandSender) {

            return "Console";

        }

        return "";

    }

    private String getPrimaryDisplayName(Object primaryObject) {

        if (primaryObject instanceof Player player) {

            return LEGACY_SERIALIZER.serialize(player.displayName());

        }

        return getPrimaryName(primaryObject);

    }

    private String getPrimaryTabName(Object primaryObject) {

        if (primaryObject instanceof Player player) {

            return LEGACY_SERIALIZER.serialize(player.playerListName());

        }

        return getPrimaryName(primaryObject);

    }

    private String getPrimaryNick(Object primaryObject) {

        if (!(primaryObject instanceof Player player) || Bukkit.getPluginManager().getPlugin("Essentials") == null) {

            return getPrimaryName(primaryObject);

        }

        final Essentials essentials = (Essentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
        final User user = essentials.getUser(player);
        if (user == null || user.getNickname() == null) {

            return player.getName();

        }

        return user.getNickname();

    }

    private String getPrimaryGroup(Object primaryObject) {

        if (!(primaryObject instanceof OfflinePlayer offlinePlayer)) {

            return "";

        }

        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {

            return "";

        }

        try {

            final LuckPerms luckPerms = LuckPermsProvider.get();
            final net.luckperms.api.model.user.User user = luckPerms.getUserManager()
                    .getUser(offlinePlayer.getUniqueId());
            return user == null ? "" : user.getPrimaryGroup();

        } catch (IllegalStateException ignored) {

            return "";

        }

    }

    private String getPrimaryPrefix(Object primaryObject) {

        final CachedMetaData metaData = getPrimaryMetaData(primaryObject);
        return metaData == null || metaData.getPrefix() == null ? "" : metaData.getPrefix();

    }

    private String getPrimarySuffix(Object primaryObject) {

        final CachedMetaData metaData = getPrimaryMetaData(primaryObject);
        return metaData == null || metaData.getSuffix() == null ? "" : metaData.getSuffix();

    }

    private CachedMetaData getPrimaryMetaData(Object primaryObject) {

        if (!(primaryObject instanceof OfflinePlayer offlinePlayer)
                || Bukkit.getPluginManager().getPlugin("LuckPerms") == null)
        {

            return null;

        }

        try {

            final LuckPerms luckPerms = LuckPermsProvider.get();
            final net.luckperms.api.model.user.User user = luckPerms.getUserManager()
                    .getUser(offlinePlayer.getUniqueId());
            return user == null ? null : user.getCachedData().getMetaData();

        } catch (IllegalStateException ignored) {

            return null;

        }

    }

    private String replaceTag(String message, String tagName, String value) {

        return message.replace("<" + tagName + ">", value == null ? "" : value);

    }

    public record MessagePlaceholder(String name, String value) {
    }

    private static final class PlaceholderContext {

        private Object primaryObject;
        private Player primaryPlayer;
        private String otherName;
        private final List<MessagePlaceholder> inlinePlaceholders = new ArrayList<>();

    }

}
