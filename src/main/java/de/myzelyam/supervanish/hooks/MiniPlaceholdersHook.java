/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.hooks;

import de.myzelyam.supervanish.SuperVanish;

import io.github.miniplaceholders.api.Expansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.UUID;

public class MiniPlaceholdersHook extends PluginHook {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final String yes, no, prefix, suffix;
    private Expansion expansion;

    public MiniPlaceholdersHook(SuperVanish superVanish) {

        super(superVanish);
        yes = superVanish.getMessage("PlaceholderIsVanishedYes");
        no = superVanish.getMessage("PlaceholderIsVanishedNo");
        prefix = superVanish.getMessage("PlaceholderVanishPrefix");
        suffix = superVanish.getMessage("PlaceholderVanishSuffix");

    }

    @Override
    public void onPluginEnable(Plugin plugin) {

        expansion = Expansion.builder("supervanish")
                .audiencePlaceholder("isvanished", (audience, queue, ctx) -> textTag(isVanishedText(audience)))
                .audiencePlaceholder("vanishprefix",
                        (audience, queue, ctx) -> componentTag(isVanished(audience) ? prefix : ""))
                .audiencePlaceholder("vanishsuffix",
                        (audience, queue, ctx) -> componentTag(isVanished(audience) ? suffix : ""))
                .audiencePlaceholder("vanishedplayers", (audience, queue, ctx) -> textTag(getVanishedPlayers(audience)))
                .audiencePlaceholder("playercount",
                        (audience, queue, ctx) -> textTag(String.valueOf(getPlayerCount(audience))))
                .build();
        expansion.register();

    }

    @Override
    public void onPluginDisable(Plugin plugin) {

        if (expansion != null && expansion.registered()) {

            expansion.unregister();

        }

        expansion = null;

    }

    private String isVanishedText(Object audience) {

        return isVanished(audience) ? yes : no;

    }

    private boolean isVanished(Object audience) {

        if (!(audience instanceof Player))
            return false;
        return superVanish.getVanishStateMgr().isVanished(((Player) audience).getUniqueId());

    }

    private String getVanishedPlayers(Object audience) {

        Player viewer = audience instanceof Player ? (Player) audience : null;
        Collection<UUID> onlineVanishedPlayers = superVanish.getVanishStateMgr().getOnlineVanishedPlayers();
        StringBuilder playerListMessage = new StringBuilder();
        for (UUID uuid : onlineVanishedPlayers) {

            Player onlineVanished = Bukkit.getPlayer(uuid);
            if (onlineVanished == null)
                continue;
            if (viewer != null
                    && superVanish.getSettings()
                            .getBoolean("IndicationFeatures.LayeredPermissions.HideInvisibleInCommands", false)
                    && !superVanish.hasPermissionToSee(viewer, onlineVanished))
            {

                continue;

            }

            if (playerListMessage.length() > 0) {

                playerListMessage.append(", ");

            }

            playerListMessage.append(onlineVanished.getName());

        }

        return playerListMessage.toString();

    }

    private int getPlayerCount(Object audience) {

        Player viewer = audience instanceof Player ? (Player) audience : null;
        int playerCount = Bukkit.getOnlinePlayers().size();
        for (UUID uuid : superVanish.getVanishStateMgr().getOnlineVanishedPlayers()) {

            Player onlineVanished = Bukkit.getPlayer(uuid);
            if (onlineVanished == null)
                continue;
            if (viewer == null || !superVanish.canSee(viewer, onlineVanished)) {

                playerCount--;

            }

        }

        return playerCount;

    }

    private Tag textTag(String value) {

        return Tag.selfClosingInserting(Component.text(value));

    }

    private Tag componentTag(String value) {

        return Tag.selfClosingInserting(LEGACY_SERIALIZER.deserialize(value));

    }

}
