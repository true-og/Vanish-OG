/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.config;

import de.myzelyam.supervanish.SuperVanish;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Level;

import static de.myzelyam.supervanish.SuperVanish.*;

public class ConfigMgr {

    private final SuperVanish plugin;
    private final FileMgr fileMgr;
    private boolean messagesUpdateRequired;
    private FileConfiguration settings;
    private FileConfiguration messages;
    private ConfigurableFile messagesFile;
    private ConfigurableFile settingsFile;

    public ConfigMgr(SuperVanish plugin) {

        this.plugin = plugin;
        fileMgr = new FileMgr(plugin);

    }

    public void prepareFiles() {

        // messages
        messagesFile = (ConfigurableFile) fileMgr.addFile("messages");
        messages = messagesFile.getConfig();
        // settings
        settingsFile = (ConfigurableFile) fileMgr.addFile("config");
        settings = settingsFile.getConfig();

    }

    public void checkFilesForLeftOvers() {

        try {

            String newestVersion = plugin.getPluginMeta().getVersion();
            String currentMessagesVersion = messages.getString("MessagesVersion");
            messagesUpdateRequired = fileRequiresRecreation(currentMessagesVersion);
            if (newestVersion.equals(currentMessagesVersion))
                messagesUpdateRequired = false;
            if (messagesUpdateRequired) {

                String currentVersion = plugin.getPluginMeta().getVersion();
                boolean isDismissed = plugin.getVanishStateMgr().isDismissed("CONSOLE", currentVersion);
                if (!isDismissed)
                    plugin.log(Level.WARNING,
                            "Messages file needs to be recreated. " + "Use '/sv recreatefiles' to regenerate it.");

            }

        } catch (Exception e) {

            plugin.logException(e);

        }

    }

    private boolean fileRequiresRecreation(String currentVersion) {

        if (currentVersion == null)
            return true;
        for (String ignoredVersion : NON_REQUIRED_MESSAGES_UPDATES) {

            if (currentVersion.equalsIgnoreCase(ignoredVersion))
                return false;

        }

        return true;

    }

    public FileMgr getFileMgr() {

        return fileMgr;

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

}
