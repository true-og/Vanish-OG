/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility;

import java.util.Locale;
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
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class KeyDBVanishStateMgr extends VanishStateMgr {

    private final SuperVanish plugin;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;
    private final String invisiblePlayersKey;
    private final String playerNamesKey;
    private final String itemPickupsKey;
    private final String dismissedKey;

    public KeyDBVanishStateMgr(SuperVanish plugin, String host, int port, String password, int database,
            String keyPrefix)
    {

        super(plugin);
        this.plugin = plugin;
        final String normalizedKeyPrefix = normalizeKeyPrefix(plugin, keyPrefix);

        RedisURI.Builder uriBuilder = RedisURI.builder().withHost(host).withPort(port).withDatabase(database);

        if (password != null && !password.isEmpty()) {

            uriBuilder.withPassword(password.toCharArray());

        }

        this.redisClient = RedisClient.create(uriBuilder.build());
        this.connection = redisClient.connect();
        this.commands = connection.sync();
        this.invisiblePlayersKey = normalizedKeyPrefix + "invisible_players";
        this.playerNamesKey = normalizedKeyPrefix + "player_names";
        this.itemPickupsKey = normalizedKeyPrefix + "item_pickups";
        this.dismissedKey = normalizedKeyPrefix + "dismissed";

    }

    @Override
    public boolean isVanished(final UUID uuid) {

        return commands.sismember(invisiblePlayersKey, uuid.toString());

    }

    @Override
    public void setVanishedState(final UUID uuid, String name, boolean hide, String causeName) {

        final PlayerVanishStateChangeEvent event = new PlayerVanishStateChangeEvent(uuid, name, hide, causeName);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {

            return;

        }

        if (hide) {

            commands.sadd(invisiblePlayersKey, uuid.toString());
            commands.hset(playerNamesKey, uuid.toString(), name);

        } else {

            commands.srem(invisiblePlayersKey, uuid.toString());
            commands.hdel(playerNamesKey, uuid.toString());

        }

    }

    @Override
    public Set<UUID> getVanishedPlayers() {

        Set<String> uuidStrings = commands.smembers(invisiblePlayersKey);
        Set<UUID> vanishedPlayers = new HashSet<>();
        for (String uuidString : uuidStrings) {

            try {

                vanishedPlayers.add(UUID.fromString(uuidString));

            } catch (IllegalArgumentException e) {

                commands.srem(invisiblePlayersKey, uuidString);
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

        Map<String, String> allNames = commands.hgetall(playerNamesKey);
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

        String value = commands.hget(itemPickupsKey, uuid.toString());
        if (value == null) {

            return defaultValue;

        }

        return Boolean.parseBoolean(value);

    }

    @Override
    public void setItemPickUps(UUID uuid, boolean value) {

        commands.hset(itemPickupsKey, uuid.toString(), String.valueOf(value));

    }

    @Override
    public boolean isDismissed(String id, String version) {

        String value = commands.hget(dismissedKey, id + ":" + version);
        return Boolean.parseBoolean(value);

    }

    @Override
    public void setDismissed(String id, String version, boolean value) {

        commands.hset(dismissedKey, id + ":" + version, String.valueOf(value));

    }

    @Override
    public void close() {

        connection.close();
        redisClient.shutdown();

    }

    private static String normalizeKeyPrefix(SuperVanish plugin, String keyPrefix) {

        final String rawPrefix = keyPrefix == null || keyPrefix.trim().isEmpty()
                ? "vanish-og:" + plugin.getServer().getPort()
                : keyPrefix.trim();
        String normalizedPrefix = rawPrefix.toLowerCase(Locale.ROOT);
        if (!normalizedPrefix.endsWith(":")) {

            normalizedPrefix += ":";

        }

        return normalizedPrefix;

    }

}
