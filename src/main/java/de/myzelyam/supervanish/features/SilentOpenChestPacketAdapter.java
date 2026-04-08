/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * license, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.features;

import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.comphenix.protocol.PacketType.Play.Server.*;

/**
 * Adapter that masks a vanished player's silent-chest spectator state from
 * other clients. On Purpur 1.19.4 ProtocolLib's PLAYER_INFO type maps to
 * ClientboundPlayerInfoUpdatePacket which carries a Set<Action> instead of the
 * legacy single action; ViaSuite handles client-side re-encoding for connected
 * versions 1.8-1.21.x.
 */
public class SilentOpenChestPacketAdapter extends PacketAdapter {

    private final SilentOpenChest silentOpenChest;

    private boolean suppressErrors = false;

    public SilentOpenChestPacketAdapter(SilentOpenChest silentOpenChest) {

        super(silentOpenChest.plugin, ListenerPriority.LOW, PLAYER_INFO, ABILITIES, ENTITY_METADATA);
        this.silentOpenChest = silentOpenChest;

    }

    @Override
    public void onPacketSending(PacketEvent event) {

        try {

            Player receiver = event.getPlayer();
            if (receiver == null)
                return;
            if (event.getPacketType() == PLAYER_INFO) {

                // multiple events share same packet object
                event.setPacket(event.getPacket().shallowClone());

                if (!isGameModeUpdate(event))
                    return;

                List<PlayerInfoData> infoDataList = new ArrayList<>(event.getPacket().getPlayerInfoDataLists().read(0));
                boolean modified = false;
                for (PlayerInfoData infoData : ImmutableList.copyOf(infoDataList)) {

                    UUID uuid = extractUUID(infoData);
                    if (uuid == null)
                        continue;
                    if (silentOpenChest.plugin.getVisibilityChanger().getHider().isHidden(uuid, receiver))
                        continue;
                    if (!silentOpenChest.plugin.getVanishStateMgr().isVanished(uuid))
                        continue;
                    if (infoData.getGameMode() != EnumWrappers.NativeGameMode.SPECTATOR)
                        continue;

                    Player vanishedTabPlayer = Bukkit.getPlayer(uuid);
                    if (vanishedTabPlayer == null || !silentOpenChest.hasSilentlyOpenedChest(vanishedTabPlayer))
                        continue;

                    PlayerInfoData newData = new PlayerInfoData(uuid, infoData.getLatency(), infoData.isListed(),
                            EnumWrappers.NativeGameMode.SURVIVAL, infoData.getProfile(), infoData.getDisplayName(),
                            infoData.getRemoteChatSessionData());
                    int index = infoDataList.indexOf(infoData);
                    infoDataList.set(index, newData);
                    modified = true;

                }

                if (modified)
                    event.getPacket().getPlayerInfoDataLists().write(0, infoDataList);

            } else if (event.getPacketType() == GAME_STATE_CHANGE) {

                // Currently unused due to ProtocolLib class loading bug
                if (silentOpenChest.plugin.getVanishStateMgr().isVanished(receiver.getUniqueId())) {

                    try {

                        if (event.getPacket().getIntegers().read(0) != 3)
                            return;

                    } catch (FieldAccessException e) {

                        // TODO
                    }

                    if (!silentOpenChest.hasSilentlyOpenedChest(receiver))
                        return;
                    event.setCancelled(true);

                }

            } else if (event.getPacketType() == ABILITIES) {

                if (silentOpenChest.plugin.getVanishStateMgr().isVanished(receiver.getUniqueId())) {

                    if (!silentOpenChest.hasSilentlyOpenedChest(receiver))
                        return;
                    event.setCancelled(true);

                }

            } else if (event.getPacketType() == ENTITY_METADATA) {

                int entityID = event.getPacket().getIntegers().read(0);
                if (entityID == receiver.getEntityId()) {

                    if (silentOpenChest.plugin.getVanishStateMgr().isVanished(receiver.getUniqueId())) {

                        if (!silentOpenChest.hasSilentlyOpenedChest(receiver))
                            return;
                        event.setCancelled(true);

                    }

                }

            }

        } catch (Exception | NoClassDefFoundError e) {

            if (!suppressErrors) {

                if (e.getMessage() == null || !e.getMessage().endsWith("is not supported for temporary players.")) {

                    silentOpenChest.plugin.logException(e);
                    silentOpenChest.plugin.getLogger()
                            .warning("IMPORTANT: Please make sure that you are using the latest "
                                    + "dev-build of ProtocolLib and that your server is up-to-date! This error likely "
                                    + "happened inside of ProtocolLib code which is out of SuperVanish's control. It's part "
                                    + "of an optional feature module and can be removed safely by disabling "
                                    + "OpenChestsSilently in the config file. Please report this "
                                    + "error if you can reproduce it on an up-to-date server with only latest "
                                    + "ProtocolLib and latest SV installed.");
                    suppressErrors = true;

                }

            }

        }

    }

    private boolean isGameModeUpdate(PacketEvent event) {

        // 1.19.3+: ClientboundPlayerInfoUpdatePacket exposes Set<Action>
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

}
