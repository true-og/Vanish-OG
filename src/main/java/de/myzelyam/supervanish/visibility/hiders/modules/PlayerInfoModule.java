/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * license, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility.hiders.modules;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.visibility.hiders.PlayerHider;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filters vanished players out of the server-bound PLAYER_INFO packet. On
 * Purpur 1.19.4 the server emits ClientboundPlayerInfoUpdatePacket which
 * ProtocolLib 5.0 maps to PacketType.Play.Server.PLAYER_INFO. ViaSuite handles
 * re-encoding for clients on 1.8-1.21.x, so this listener only needs to operate
 * on the server-native packet.
 */
public class PlayerInfoModule extends PacketAdapter {

    private final PlayerHider hider;
    private final SuperVanish plugin;

    private boolean errorLogged = false;

    public PlayerInfoModule(SuperVanish plugin, PlayerHider hider) {

        super(plugin, ListenerPriority.HIGH, PacketType.Play.Server.PLAYER_INFO);
        this.plugin = plugin;
        this.hider = hider;

    }

    public static void register(SuperVanish plugin, PlayerHider hider) {

        ProtocolLibrary.getProtocolManager().addPacketListener(new PlayerInfoModule(plugin, hider));

    }

    @Override
    public void onPacketSending(PacketEvent event) {

        try {

            // If nobody is vanished, do not touch login/player-info packets at all.
            if (plugin.getVanishStateMgr().getOnlineVanishedPlayers().isEmpty())
                return;

            // multiple events share same packet object
            event.setPacket(event.getPacket().shallowClone());
            List<PlayerInfoData> rawList = event.getPacket().getPlayerInfoDataLists().read(0);
            if (rawList == null)
                return;

            Player receiver = event.getPlayer();
            if (receiver == null)
                return;

            List<PlayerInfoData> infoDataList = new ArrayList<>(rawList);
            boolean modified = false;
            for (PlayerInfoData infoData : new ArrayList<>(infoDataList)) {

                if (infoData == null)
                    continue;

                UUID uuid = extractUUID(infoData);
                if (uuid == null)
                    continue;
                if (hider.isHidden(uuid, receiver)) {

                    infoDataList.remove(infoData);
                    modified = true;

                }

            }

            if (!modified) {

                // Untouched packets must not be rewritten: writing back into
                // ClientboundPlayerInfoUpdatePacket on 1.19.3+ also rewrites its
                // final EnumSet<Action> field, which ProtocolLib cannot mutate.
                return;

            }

            if (infoDataList.isEmpty()) {

                event.setCancelled(true);
                return;

            }

            event.getPacket().getPlayerInfoDataLists().write(0, infoDataList);

        } catch (Exception | NoClassDefFoundError e) {

            if (e.getMessage() == null || !e.getMessage().endsWith("is not supported for temporary players.")) {

                if (errorLogged)
                    return;
                plugin.logException(e);
                plugin.getLogger()
                        .warning("IMPORTANT: Please make sure that you are using the latest "
                                + "dev-build of ProtocolLib and that your server is up-to-date! This error likely "
                                + "happened inside of ProtocolLib code which is out of Vanish-OG's control. It's part "
                                + "of an optional invisibility module and can be removed safely by disabling "
                                + "ModifyTablistPackets in the config. Please report this "
                                + "error if you can reproduce it on an up-to-date server with only latest "
                                + "ProtocolLib and latest Vanish-OG installed.");
                errorLogged = true;

            }

        }

    }

    private UUID extractUUID(PlayerInfoData infoData) {

        // On 1.19.3+ the per-entry profile is null for non-ADD_PLAYER actions, so
        // prefer
        // profileId which the wrapper exposes for both legacy and update packets.
        UUID uuid = infoData.getProfileId();
        if (uuid != null)
            return uuid;
        if (infoData.getProfile() != null)
            return infoData.getProfile().getUUID();
        return null;

    }

}
