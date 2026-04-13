/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import de.myzelyam.supervanish.SuperVanish;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class KeyDBVanishStateMgr extends VanishStateMgr {

    private static final String INVISIBLE_PLAYERS_KEY = "vanish:invisible_players";
    private static final String PLAYER_NAMES_KEY = "vanish:player_names";
    private static final String ITEM_PICKUPS_KEY = "vanish:item_pickups";
    private static final String DISMISSED_KEY = "vanish:dismissed";

    private final SuperVanish plugin;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;

    public KeyDBVanishStateMgr(SuperVanish plugin, String uri) {

        super(plugin);
        this.plugin = plugin;
        this.redisClient = RedisClient.create(uri);
        this.connection = redisClient.connect();
        this.commands = connection.sync();

    }

    @Override
    public boolean isVanished(final UUID uuid) {

        return commands.sismember(INVISIBLE_PLAYERS_KEY, uuid.toString());

    }

    @Override
    public void setVanishedState(final UUID uuid, String name, boolean hide, String causeName) {

        final PlayerVanishStateChangeEvent event = new PlayerVanishStateChangeEvent(uuid, name, hide, causeName);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {

            return;

        }

        if (hide) {

            commands.sadd(INVISIBLE_PLAYERS_KEY, uuid.toString());
            commands.hset(PLAYER_NAMES_KEY, uuid.toString(), name);

        } else {

            commands.srem(INVISIBLE_PLAYERS_KEY, uuid.toString());
            commands.hdel(PLAYER_NAMES_KEY, uuid.toString());

        }

    }

    @Override
    public Set<UUID> getVanishedPlayers() {

        Set<String> uuidStrings = commands.smembers(INVISIBLE_PLAYERS_KEY);
        Set<UUID> vanishedPlayers = new HashSet<>();
        for (String uuidString : uuidStrings) {

            try {

                vanishedPlayers.add(UUID.fromString(uuidString));

            } catch (IllegalArgumentException e) {

                commands.srem(INVISIBLE_PLAYERS_KEY, uuidString);
                plugin.log(Level.WARNING, "KeyDB contained an invalid player uuid, deleting it.");

            }

        }

        return vanishedPlayers;

    }

    @Override
    public Collection<UUID> getOnlineVanishedPlayers() {

        Set<UUID> onlineVanishedPlayers = new HashSet<>();
        getVanishedPlayers().stream().filter(uuid -> Bukkit.getPlayer(uuid) != null)
                .forEach(onlineVanishedPlayers::add);

        return onlineVanishedPlayers;

    }

    @Override
    public UUID getVanishedUUIDFromName(String name) {

        Map<String, String> allNames = commands.hgetall(PLAYER_NAMES_KEY);
        for (Map.Entry<String, String> entry : allNames.entrySet()) {

            if (entry.getValue().equalsIgnoreCase(name)) {

                try {

                    return UUID.fromString(entry.getKey());

                } catch (IllegalArgumentException e) {

                    return null;

                }

            }

        }

        return null;

    }

    @Override
    public boolean getItemPickUps(UUID uuid, boolean defaultValue) {

        String value = commands.hget(ITEM_PICKUPS_KEY, uuid.toString());
        if (value == null) {

            return defaultValue;

        }

        return Boolean.parseBoolean(value);

    }

    @Override
    public void setItemPickUps(UUID uuid, boolean value) {

        commands.hset(ITEM_PICKUPS_KEY, uuid.toString(), String.valueOf(value));

    }

    @Override
    public boolean isDismissed(String id, String version) {

        String value = commands.hget(DISMISSED_KEY, id + ":" + version);
        return Boolean.parseBoolean(value);

    }

    @Override
    public void setDismissed(String id, String version, boolean value) {

        commands.hset(DISMISSED_KEY, id + ":" + version, String.valueOf(value));

    }

    @Override
    public void close() {

        connection.close();
        redisClient.shutdown();

    }

}
