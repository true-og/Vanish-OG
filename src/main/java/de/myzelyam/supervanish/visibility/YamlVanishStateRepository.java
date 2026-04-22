/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import de.myzelyam.supervanish.SuperVanish;

public class YamlVanishStateRepository implements VanishStateRepository {

    private static final String VANISHED_PATH = "vanished";
    private static final String ITEM_PICKUPS_PATH = "item-pickups";
    private static final String DISMISSED_PATH = "dismissed";

    private final SuperVanish plugin;
    private final File file;

    public YamlVanishStateRepository(SuperVanish plugin) {

        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");

    }

    @Override
    public PersistedVanishState load() {

        Set<UUID> vanishedPlayers = new HashSet<>();
        Map<UUID, String> vanishedNames = new HashMap<>();
        Map<UUID, Boolean> itemPickUps = new HashMap<>();
        Map<String, Boolean> dismissed = new HashMap<>();

        try {

            ensureFileExists();
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            final ConfigurationSection vanishedSection = config.getConfigurationSection(VANISHED_PATH);
            if (vanishedSection != null) {

                for (String rawUuid : vanishedSection.getKeys(false)) {

                    try {

                        UUID uuid = UUID.fromString(rawUuid);
                        vanishedPlayers.add(uuid);
                        vanishedNames.put(uuid, vanishedSection.getString(rawUuid + ".name"));

                    } catch (IllegalArgumentException e) {

                        plugin.log(Level.WARNING, "Skipping malformed vanished UUID in data.yml: " + rawUuid);

                    }

                }

            }

            final ConfigurationSection itemPickupsSection = config.getConfigurationSection(ITEM_PICKUPS_PATH);
            if (itemPickupsSection != null) {

                for (String rawUuid : itemPickupsSection.getKeys(false)) {

                    try {

                        itemPickUps.put(UUID.fromString(rawUuid), itemPickupsSection.getBoolean(rawUuid));

                    } catch (IllegalArgumentException e) {

                        plugin.log(Level.WARNING, "Skipping malformed item-pickups UUID in data.yml: " + rawUuid);

                    }

                }

            }

            final ConfigurationSection dismissedSection = config.getConfigurationSection(DISMISSED_PATH);
            if (dismissedSection != null) {

                for (String key : dismissedSection.getKeys(false)) {

                    dismissed.put(key, dismissedSection.getBoolean(key));

                }

            }

        } catch (IOException e) {

            plugin.log(Level.WARNING, "Failed to load vanish data from " + file.getName() + ".", e);

        }

        return new PersistedVanishState(vanishedPlayers, vanishedNames, itemPickUps, dismissed);

    }

    @Override
    public void save(PersistedVanishState state) {

        final YamlConfiguration config = new YamlConfiguration();

        ConfigurationSection vanishedSection = config.createSection(VANISHED_PATH);
        for (UUID uuid : state.vanishedPlayers()) {

            vanishedSection.set(uuid.toString() + ".name", state.vanishedNames().get(uuid));

        }

        ConfigurationSection itemPickupsSection = config.createSection(ITEM_PICKUPS_PATH);
        for (Map.Entry<UUID, Boolean> entry : state.itemPickUps().entrySet()) {

            itemPickupsSection.set(entry.getKey().toString(), entry.getValue());

        }

        ConfigurationSection dismissedSection = config.createSection(DISMISSED_PATH);
        for (Map.Entry<String, Boolean> entry : state.dismissed().entrySet()) {

            dismissedSection.set(entry.getKey(), entry.getValue());

        }

        try {

            ensureFileExists();
            config.save(file);

        } catch (IOException e) {

            plugin.log(Level.WARNING, "Failed to save vanish data to " + file.getName() + ".", e);

        }

    }

    private void ensureFileExists() throws IOException {

        if (!plugin.getDataFolder().exists()) {

            plugin.getDataFolder().mkdirs();

        }

        if (!file.exists()) {

            file.createNewFile();

        }

    }

}
