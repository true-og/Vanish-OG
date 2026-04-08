/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;

import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import de.myzelyam.supervanish.SuperVanish;

public class FileVanishStateMgr extends VanishStateMgr {

    private final SuperVanish plugin;

    public FileVanishStateMgr(SuperVanish plugin) {

        super(plugin);
        this.plugin = plugin;

    }

    @Override
    public boolean isVanished(final UUID uuid) {

        return getVanishedPlayersOnFile().contains(uuid);

    }

    @Override
    public void setVanishedState(final UUID uuid, String name, boolean hide, String causeName) {

        final PlayerVanishStateChangeEvent event = new PlayerVanishStateChangeEvent(uuid, name, hide, causeName);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {

            return;

        }

        final Set<UUID> vanishedPlayers = getVanishedPlayersOnFile();
        if (hide) {

            vanishedPlayers.add(uuid);

        } else {

            vanishedPlayers.remove(uuid);

        }

        setVanishedPlayersOnFile(vanishedPlayers);
        if (hide) {

            plugin.getPlayerData().set("PlayerData." + uuid + ".information.name", name);
            plugin.getConfigMgr().getPlayerDataFile().save();

        }

    }

    @Override
    public Set<UUID> getVanishedPlayers() {

        return getVanishedPlayersOnFile();

    }

    @Override
    public Collection<UUID> getOnlineVanishedPlayers() {

        final Set<UUID> onlineVanishedPlayers = new HashSet<>();
        getVanishedPlayers().stream().filter(vanishedUUID -> Bukkit.getPlayer(vanishedUUID) != null)
                .forEach(onlineVanishedPlayers::add);

        return onlineVanishedPlayers;

    }

    public UUID getVanishedUUIDFromNameOnFile(String name) {

        return getVanishedPlayersOnFile().stream()
                .filter(uuid -> StringUtils.equalsIgnoreCase(
                        plugin.getPlayerData().getString("PlayerData." + uuid + ".information.name"), name))
                .findFirst().orElse(null);

    }

    private Set<UUID> getVanishedPlayersOnFile() {

        final List<String> vanishedPlayerUUIDStrings = plugin.getPlayerData().getStringList("InvisiblePlayers");
        final Set<UUID> vanishedPlayerUUIDs = new HashSet<>();
        new ArrayList<>(vanishedPlayerUUIDStrings).forEach(uuidString -> {

            try {

                vanishedPlayerUUIDs.add(UUID.fromString(uuidString));

            } catch (IllegalArgumentException e) {

                vanishedPlayerUUIDStrings.remove(uuidString);
                plugin.log(Level.WARNING, "The data.yml file contains an invalid player uuid," + " deleting it.");
                plugin.getPlayerData().set("InvisiblePlayers", vanishedPlayerUUIDStrings);
                plugin.getConfigMgr().getPlayerDataFile().save();

            }

        });

        return vanishedPlayerUUIDs;

    }

    private void setVanishedPlayersOnFile(Set<UUID> vanishedPlayers) {

        final List<String> vanishedPlayerUUIDStrings = new ArrayList<>();
        vanishedPlayers.forEach(uuid -> vanishedPlayerUUIDStrings.add(uuid.toString()));
        plugin.getPlayerData().set("InvisiblePlayers", vanishedPlayerUUIDStrings);
        plugin.getConfigMgr().getPlayerDataFile().save();

    }

}