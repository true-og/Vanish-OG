/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.features;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedRemoteChatSessionData;
import com.google.common.collect.ImmutableList;
import de.myzelyam.api.vanish.PlayerShowEvent;
import de.myzelyam.api.vanish.PostPlayerHideEvent;
import de.myzelyam.supervanish.SuperVanish;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.comphenix.protocol.PacketType.Play.Server.PLAYER_INFO;

/**
 * Marks visible vanished players as spectators in player info updates. On
 * Purpur 1.19.4 this operates on the server-native
 * ClientboundPlayerInfoUpdatePacket format and ViaSuite handles translation for
 * connected 1.8-1.21.x clients.
 */
public class VanishIndication extends Feature {

    private boolean suppressErrors = false;

    public VanishIndication(SuperVanish plugin) {

        super(plugin);

    }

    @Override
    public boolean isActive() {

        return plugin.getSettings().getBoolean("IndicationFeatures.MarkVanishedPlayersAsSpectators");

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanish(PostPlayerHideEvent e) {

        Player p = e.getPlayer();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

            if (!plugin.getVisibilityChanger().getHider().isHidden(p, onlinePlayer) && p != onlinePlayer) {

                sendPlayerInfoChangeGameModePacket(onlinePlayer, p, true);

            }

        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReappear(PlayerShowEvent e) {

        final Player p = e.getPlayer();
        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {

            if (!plugin.getVisibilityChanger().getHider().isHidden(p, onlinePlayer) && p != onlinePlayer) {

                delay(() -> sendPlayerInfoChangeGameModePacket(onlinePlayer, p, false));

            }

        }

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {

        final Player p = e.getPlayer();
        delay(() -> {

            // tell others that p is a spectator
            if (plugin.getVanishStateMgr().isVanished(p.getUniqueId()))
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

                    if (!plugin.getVisibilityChanger().getHider().isHidden(p, onlinePlayer) && p != onlinePlayer) {

                        sendPlayerInfoChangeGameModePacket(onlinePlayer, p, true);

                    }

                }
            // tell p that others are spectators
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

                if (!plugin.getVanishStateMgr().isVanished(onlinePlayer.getUniqueId()))
                    continue;
                if (!plugin.getVisibilityChanger().getHider().isHidden(onlinePlayer, p) && p != onlinePlayer) {

                    sendPlayerInfoChangeGameModePacket(p, onlinePlayer, true);

                }

            }

        });

    }

    @Override
    public void onEnable() {

        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO)
                {

                    @Override
                    public void onPacketSending(PacketEvent event) {

                        try {

                            // multiple events share same packet object
                            event.setPacket(event.getPacket().shallowClone());
                            List<PlayerInfoData> infoDataList = new ArrayList<>(
                                    event.getPacket().getPlayerInfoDataLists().read(0));
                            Player receiver = event.getPlayer();
                            for (PlayerInfoData infoData : ImmutableList.copyOf(infoDataList)) {

                                try {

                                    if (!isGameModeUpdate(event))
                                        break;

                                    if (extractUUID(infoData) == null)
                                        continue;
                                    if (!VanishIndication.this.plugin.getVisibilityChanger().getHider()
                                            .isHidden(extractUUID(infoData), receiver)
                                            && VanishIndication.this.plugin.getVanishStateMgr()
                                                    .isVanished(extractUUID(infoData)))
                {

                                        if (!receiver.getUniqueId().equals(extractUUID(infoData)))
                                            if (infoData.getGameMode() != EnumWrappers.NativeGameMode.SPECTATOR) {

                                                PlayerInfoData newData = createGameModeUpdate(infoData,
                                                        EnumWrappers.NativeGameMode.SPECTATOR);
                                                int index = infoDataList.indexOf(infoData);
                                                infoDataList.set(index, newData);

                                            }

                                    }

                                } catch (UnsupportedOperationException ignored) {

                                }

                            }

                            event.getPacket().getPlayerInfoDataLists().write(0, infoDataList);

                        } catch (Exception | NoClassDefFoundError e) {

                            if (!suppressErrors) {

                                VanishIndication.this.plugin.logException(e);
                                plugin.getLogger().warning("IMPORTANT: Please make sure that you are using the latest "
                                        + "dev-build of ProtocolLib and that your server is up-to-date! This error likely "
                                        + "happened inside of ProtocolLib code which is out of SuperVanish's control. It's part "
                                        + "of an optional feature module and can be removed safely by disabling "
                                        + "MarkVanishedPlayersAsSpectators in the config file. Please report this "
                                        + "error if you can reproduce it on an up-to-date server with only latest "
                                        + "ProtocolLib and latest SV installed.");
                                suppressErrors = true;

                            }

                        }

                    }

                });

    }

    private void sendPlayerInfoChangeGameModePacket(Player p, Player change, boolean spectator) {

        PacketContainer packet = new PacketContainer(PLAYER_INFO);
        if (plugin.getVersionUtil().isOneDotXOrHigher(19)) {

            packet.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE));

        } else {

            packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE);

        }

        List<PlayerInfoData> data = new ArrayList<>();
        int ping = ThreadLocalRandom.current().nextInt(20) + 15;
        EnumWrappers.NativeGameMode gameMode = spectator ? EnumWrappers.NativeGameMode.SPECTATOR
                : EnumWrappers.NativeGameMode.fromBukkit(change.getGameMode());
        WrappedChatComponent displayName = WrappedChatComponent
                .fromText(LegacyComponentSerializer.legacySection().serialize(change.playerListName()));
        WrappedGameProfile profile = WrappedGameProfile.fromPlayer(change);
        if (plugin.getVersionUtil().isOneDotXOrHigher(19)) {

            data.add(new PlayerInfoData(change.getUniqueId(), ping, true, gameMode, profile, displayName,
                    (WrappedRemoteChatSessionData) null));

        } else {

            data.add(new PlayerInfoData(profile, ping, gameMode, displayName));

        }

        packet.getPlayerInfoDataLists().write(0, data);
        try {

            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet);

        } catch (Exception e) {

            throw new RuntimeException("Cannot send packet", e);

        }

    }

    private boolean isGameModeUpdate(PacketEvent event) {

        try {

            Set<EnumWrappers.PlayerInfoAction> actions = event.getPacket().getPlayerInfoActions().read(0);
            if (actions != null)
                return actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE);

        } catch (FieldAccessException | IllegalArgumentException ignored) {

            // legacy single-action packet on pre-1.19.3
        }

        try {

            EnumWrappers.PlayerInfoAction action = event.getPacket().getPlayerInfoAction().read(0);
            return action == EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE;

        } catch (FieldAccessException | IllegalArgumentException ignored) {

            return false;

        }

    }

    private UUID extractUUID(PlayerInfoData infoData) {

        UUID uuid = infoData.getProfileId();
        if (uuid != null)
            return uuid;
        if (infoData.getProfile() != null)
            return infoData.getProfile().getUUID();
        return null;

    }

    private PlayerInfoData createGameModeUpdate(PlayerInfoData infoData, EnumWrappers.NativeGameMode gameMode) {

        UUID uuid = extractUUID(infoData);
        if (plugin.getVersionUtil().isOneDotXOrHigher(19)) {

            return new PlayerInfoData(uuid, infoData.getLatency(), infoData.isListed(), gameMode, infoData.getProfile(),
                    infoData.getDisplayName(), infoData.getRemoteChatSessionData());

        }

        return new PlayerInfoData(infoData.getProfile(), infoData.getLatency(), gameMode, infoData.getDisplayName());

    }

}
