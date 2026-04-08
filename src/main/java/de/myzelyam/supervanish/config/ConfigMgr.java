/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.config;

import de.myzelyam.supervanish.SuperVanish;
import net.trueog.utilitiesog.UtilitiesOG;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Collections;
import java.util.logging.Level;

import static de.myzelyam.supervanish.SuperVanish.*;

public class ConfigMgr {

    private final SuperVanish plugin;
    private final FileMgr fileMgr;
    private boolean settingsUpdateRequired;
    private boolean messagesUpdateRequired;
    private FileConfiguration settings;
    private FileConfiguration messages;
    private FileConfiguration playerData;
    private ConfigurableFile messagesFile;
    private ConfigurableFile settingsFile;
    private StorageFile playerDataFile;

    public ConfigMgr(SuperVanish plugin) {

        this.plugin = plugin;
        fileMgr = new FileMgr(plugin);

    }

    public void prepareFiles() {

        // messages
        messagesFile = (ConfigurableFile) fileMgr.addFile("messages", FileMgr.FileType.CONFIG);
        messages = messagesFile.getConfig();
        // settings
        settingsFile = (ConfigurableFile) fileMgr.addFile("config", FileMgr.FileType.CONFIG);
        settings = settingsFile.getConfig();
        // data
        playerDataFile = (StorageFile) fileMgr.addFile("data", FileMgr.FileType.STORAGE);
        playerData = playerDataFile.getConfig();
        playerData.addDefault("InvisiblePlayers", Collections.emptyList());
        playerData.options().copyDefaults(true);
        playerData.options().setHeader(List.of("SuperVanish v" + plugin.getPluginMeta().getVersion() + " - Data file"));
        playerDataFile.save();

        checkFilesForLeftOvers();

    }

    public void checkFilesForLeftOvers() {

        try {

            String currentSettingsVersion = settings.getString("ConfigVersion");
            String newestVersion = plugin.getPluginMeta().getVersion();
            String currentMessagesVersion = messages.getString("MessagesVersion");
            messagesUpdateRequired = fileRequiresRecreation(currentMessagesVersion, false);
            settingsUpdateRequired = fileRequiresRecreation(currentSettingsVersion, true);
            if (newestVersion.equals(currentSettingsVersion))
                settingsUpdateRequired = false;
            if (newestVersion.equals(currentMessagesVersion))
                messagesUpdateRequired = false;
            if (settingsUpdateRequired || messagesUpdateRequired) {

                String currentVersion = plugin.getPluginMeta().getVersion();
                boolean isDismissed = playerData
                        .getBoolean("PlayerData.CONSOLE.dismissed." + currentVersion.replace(".", "_"), false);
                if (!isDismissed)
                    plugin.log(Level.WARNING, "At least one config file needs to be recreated. "
                            + "Use '/sv recreatefiles' to regenerate it.");

            }

            if (currentSettingsVersion.startsWith("1.5.") || currentSettingsVersion.startsWith("1.4.")) {

                UtilitiesOG.logToConsole("[SuperVanish]",
                        "<yellow>You have a very old config file. Your settings will not work until you "
                                + "regenerate your SV files using /sv recreatefiles.");

            }

        } catch (Exception e) {

            plugin.logException(e);

        }

    }

    private boolean fileRequiresRecreation(String currentVersion, boolean isSettingsFile) {

        if (currentVersion == null)
            return true;
        for (String ignoredVersion : isSettingsFile ? NON_REQUIRED_SETTINGS_UPDATES : NON_REQUIRED_MESSAGES_UPDATES) {

            if (currentVersion.equalsIgnoreCase(ignoredVersion))
                return false;

        }

        return true;

    }

    public FileMgr getFileMgr() {

        return fileMgr;

    }

    public boolean isSettingsUpdateRequired() {

        return settingsUpdateRequired;

    }

    public boolean isMessagesUpdateRequired() {

        return messagesUpdateRequired;

    }

    public FileConfiguration getSettings() {

        return settings;

    }

    public FileConfiguration getMessages() {

        return messages;

    }

    public FileConfiguration getPlayerData() {

        return playerData;

    }

    public void setSettings(FileConfiguration settings) {

        this.settings = settings;

    }

    public void setMessages(FileConfiguration messages) {

        this.messages = messages;

    }

    public ConfigurableFile getMessagesFile() {

        return messagesFile;

    }

    public ConfigurableFile getSettingsFile() {

        return settingsFile;

    }

    public StorageFile getPlayerDataFile() {

        return playerDataFile;

    }

}
