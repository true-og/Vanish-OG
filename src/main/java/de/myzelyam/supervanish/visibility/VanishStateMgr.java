/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility;

import de.myzelyam.supervanish.SuperVanishPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;

public class VanishStateMgr {

    protected final SuperVanishPlugin plugin;
    private final VanishStateRepository repository;
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> vanishedNames = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> itemPickUps = new ConcurrentHashMap<>();
    private final Map<String, Boolean> dismissed = new ConcurrentHashMap<>();

    public VanishStateMgr(SuperVanishPlugin plugin, VanishStateRepository repository) {

        this.plugin = plugin;
        this.repository = repository;
        load();

    }

    public boolean isVanished(final UUID uuid) {

        return vanishedPlayers.contains(uuid);

    }

    public final boolean isVanished(final Player player) {

        return isVanished(player.getUniqueId());

    }

    public synchronized void setVanishedState(final UUID uuid, String name, boolean hide, String causeName) {

        final PlayerVanishStateChangeEvent event = new PlayerVanishStateChangeEvent(uuid, name, hide, causeName);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {

            return;

        }

        if (hide) {

            vanishedPlayers.add(uuid);
            vanishedNames.put(uuid, name);

        } else {

            vanishedPlayers.remove(uuid);
            vanishedNames.remove(uuid);

        }

        save();

    }

    public final void setVanishedState(final UUID uuid, String name, boolean hide) {

        setVanishedState(uuid, name, hide, null);

    }

    public Set<UUID> getVanishedPlayers() {

        return new HashSet<>(vanishedPlayers);

    }

    public Collection<UUID> getOnlineVanishedPlayers() {

        Set<UUID> onlineVanishedPlayers = new HashSet<>();
        for (UUID uuid : vanishedPlayers) {

            if (Bukkit.getPlayer(uuid) != null) {

                onlineVanishedPlayers.add(uuid);

            }

        }

        return onlineVanishedPlayers;

    }

    public UUID getVanishedUUIDFromName(String name) {

        for (Map.Entry<UUID, String> entry : vanishedNames.entrySet()) {

            if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(name)) {

                return entry.getKey();

            }

        }

        return null;

    }

    public boolean getItemPickUps(UUID uuid, boolean defaultValue) {

        return itemPickUps.getOrDefault(uuid, defaultValue);

    }

    public synchronized void setItemPickUps(UUID uuid, boolean value) {

        itemPickUps.put(uuid, value);
        save();

    }

    public boolean isDismissed(String id, String version) {

        return dismissed.getOrDefault(createDismissedKey(id, version), false);

    }

    public synchronized void setDismissed(String id, String version, boolean value) {

        final String key = createDismissedKey(id, version);
        if (value) {

            dismissed.put(key, true);

        } else {

            dismissed.remove(key);

        }

        save();

    }

    public synchronized void clearAllVanishState() {

        vanishedPlayers.clear();
        vanishedNames.clear();
        save();
        plugin.log(Level.INFO, "Cleared all persisted vanish state.");

    }

    private void load() {

        apply(repository.load());

    }

    private synchronized void save() {

        repository.save(snapshot());

    }

    private void apply(PersistedVanishState state) {

        vanishedPlayers.clear();
        vanishedPlayers.addAll(state.vanishedPlayers());
        vanishedNames.clear();
        vanishedNames.putAll(state.vanishedNames());
        itemPickUps.clear();
        itemPickUps.putAll(state.itemPickUps());
        dismissed.clear();
        dismissed.putAll(state.dismissed());

    }

    private PersistedVanishState snapshot() {

        return new PersistedVanishState(new HashSet<>(vanishedPlayers), new HashMap<>(vanishedNames),
                new HashMap<>(itemPickUps), new HashMap<>(dismissed));

    }

    private String createDismissedKey(String id, String version) {

        return escapePathSegment(id) + "__" + escapePathSegment(version);

    }

    private String escapePathSegment(String value) {

        return value.replace("%", "%25").replace(".", "%2E");

    }

    public void close() {

        repository.close();

    }

}
