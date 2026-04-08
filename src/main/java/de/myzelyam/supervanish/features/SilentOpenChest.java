/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.features;

import static org.bukkit.Material.BLACK_SHULKER_BOX;
import static org.bukkit.Material.BLUE_SHULKER_BOX;
import static org.bukkit.Material.BROWN_SHULKER_BOX;
import static org.bukkit.Material.CHEST;
import static org.bukkit.Material.CYAN_SHULKER_BOX;
import static org.bukkit.Material.ENDER_CHEST;
import static org.bukkit.Material.GRAY_SHULKER_BOX;
import static org.bukkit.Material.GREEN_SHULKER_BOX;
import static org.bukkit.Material.ITEM_FRAME;
import static org.bukkit.Material.LIGHT_BLUE_SHULKER_BOX;
import static org.bukkit.Material.LIGHT_GRAY_SHULKER_BOX;
import static org.bukkit.Material.LIME_SHULKER_BOX;
import static org.bukkit.Material.MAGENTA_SHULKER_BOX;
import static org.bukkit.Material.ORANGE_SHULKER_BOX;
import static org.bukkit.Material.PINK_SHULKER_BOX;
import static org.bukkit.Material.PURPLE_SHULKER_BOX;
import static org.bukkit.Material.RED_SHULKER_BOX;
import static org.bukkit.Material.SHULKER_BOX;
import static org.bukkit.Material.TRAPPED_CHEST;
import static org.bukkit.Material.WHITE_SHULKER_BOX;
import static org.bukkit.Material.YELLOW_SHULKER_BOX;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.comphenix.protocol.ProtocolLibrary;

import de.myzelyam.api.vanish.PlayerShowEvent;
import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.hooks.OpenInvHook;

public class SilentOpenChest extends Feature {

    private final Map<Player, StateInfo> playerStateInfoMap = new HashMap<>();

    private final Collection<Material> additionalChestMaterials;

    public SilentOpenChest(SuperVanish plugin) {

        super(plugin);
        additionalChestMaterials = new ArrayList<>();
        if (plugin.getVersionUtil().isOneDotXOrHigher(11)) {

            try {

                additionalChestMaterials.addAll(Arrays.asList(BLACK_SHULKER_BOX, BLUE_SHULKER_BOX, BROWN_SHULKER_BOX,
                        CYAN_SHULKER_BOX, GRAY_SHULKER_BOX, GREEN_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX, LIME_SHULKER_BOX,
                        MAGENTA_SHULKER_BOX, ORANGE_SHULKER_BOX, PINK_SHULKER_BOX, PURPLE_SHULKER_BOX, RED_SHULKER_BOX,
                        WHITE_SHULKER_BOX, YELLOW_SHULKER_BOX));
                try {

                    additionalChestMaterials.add(LIGHT_GRAY_SHULKER_BOX);

                } catch (NoSuchFieldError e) {

                    // Old name.
                    additionalChestMaterials.add(Material.valueOf("SILVER_SHULKER_BOX"));

                }

                try {

                    additionalChestMaterials.add(SHULKER_BOX);

                } catch (NoSuchFieldError ignored) {

                    // no standard shulker box in old versions
                }

                if (plugin.getVersionUtil().isOneDotXOrHigher(14)) {

                    additionalChestMaterials.add(Material.valueOf("BARREL"));

                }

            } catch (NoSuchFieldError | IllegalArgumentException ignored) {

                // no shulker box support in very old versions
            }

        }

    }

    @Override
    public void onDisable() {

        for (Player p : playerStateInfoMap.keySet()) {

            final StateInfo stateInfo = playerStateInfoMap.remove(p);
            if (stateInfo == null) {

                continue;

            }

            restoreState(stateInfo, p);

        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpectatorClick(InventoryClickEvent e) {

        if (!(e.getWhoClicked() instanceof Player)) {

            return;

        }

        final Player p = (Player) e.getWhoClicked();
        if (!plugin.getVanishStateMgr().isVanished(p.getUniqueId())) {

            return;

        }

        if (!playerStateInfoMap.containsKey(p)) {

            return;

        }

        if (p.getGameMode() == GameMode.SPECTATOR) {

            e.setCancelled(false);

        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {

        final Player p = e.getPlayer();
        final StateInfo stateInfo = playerStateInfoMap.remove(p);
        if (stateInfo == null) {

            return;

        }

        restoreState(stateInfo, p);

        playerStateInfoMap.remove(p);

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent e) {

        final Player p = e.getPlayer();
        if (playerStateInfoMap.containsKey(p) && e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {

            e.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReappear(PlayerShowEvent e) {

        final Player p = e.getPlayer();
        final StateInfo stateInfo = playerStateInfoMap.remove(p);
        if (stateInfo == null) {

            return;

        }

        p.closeInventory();

        restoreState(stateInfo, p);

        playerStateInfoMap.remove(p);

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {

        final Player p = e.getPlayer();
        if (!playerStateInfoMap.containsKey(p)) {

            return;

        }

        final Location loc = e.getTo() != null ? e.getTo() : e.getFrom();
        if (playerStateInfoMap.get(p).openLoc.distance(loc) <= .5) {

            return;

        }

        p.closeInventory();

        restoreState(playerStateInfoMap.get(p), p);

        playerStateInfoMap.remove(p);

    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onGameModeChange(PlayerGameModeChangeEvent e) {

        final Player p = e.getPlayer();
        final boolean condition = playerStateInfoMap.containsKey(p) && e.getNewGameMode() != GameMode.SPECTATOR
                && e.isCancelled();
        // Don't let low-priority event listeners cancel the gamemode change.
        if (condition) {

            e.setCancelled(false);

        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChestInteract(PlayerInteractEvent e) {

        final Player p = e.getPlayer();
        if (!plugin.getVanishStateMgr().isVanished(p.getUniqueId()) || !p.hasPermission("sv.silentchest")) {

            return;

        }

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {

            return;

        }

        if (p.getGameMode() == GameMode.SPECTATOR) {

            return;

        }

        final ItemStack heldItem = p.getInventory().getItemInMainHand();
        if (p.isSneaking() && heldItem != null && (heldItem.getType().isBlock() || heldItem.getType() == ITEM_FRAME)
                && heldItem.getType() != Material.AIR)
        {

            return;

        }

        final Block block = e.getClickedBlock();
        if (block == null) {

            return;

        }

        if (block.getType() == ENDER_CHEST) {

            e.setCancelled(true);
            p.openInventory(p.getEnderChest());
            return;

        }

        if (!(block.getType() == CHEST || block.getType() == TRAPPED_CHEST
                || plugin.getVersionUtil().isOneDotXOrHigher(11) && additionalChestMaterials.contains(block.getType())))
        {

            return;

        }

        final StateInfo stateInfo = StateInfo.extract(p);

        p.setVelocity(new Vector(0, 0, 0));

        playerStateInfoMap.put(p, stateInfo);

        p.setGameMode(GameMode.SPECTATOR);

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChestClose(InventoryCloseEvent e) {

        if (!(e.getPlayer() instanceof Player)) {

            return;

        }

        final Player p = (Player) e.getPlayer();
        if (!playerStateInfoMap.containsKey(p)) {

            return;

        }

        if (!isSilentOpenInventory(e.getView())) {

            return;

        }

        new BukkitRunnable() {

            @Override
            public void run() {

                final StateInfo stateInfo = playerStateInfoMap.get(p);
                if (stateInfo == null) {

                    return;

                }

                restoreState(stateInfo, p);

                playerStateInfoMap.remove(p);

            }

        }.runTaskLater(plugin, 1);

    }

    private void restoreState(StateInfo stateInfo, Player p) {

        p.setGameMode(stateInfo.gameMode);
        p.teleport(p.getLocation().add(0, 0.2, 0));
        new BukkitRunnable() {

            @Override
            public void run() {

                p.setAllowFlight(stateInfo.canFly);
                p.setFlying(stateInfo.isFlying);

            }

        }.runTaskLater(plugin, 1);

    }

    @Override
    public boolean isActive() {

        return plugin.getSettings().getBoolean("InvisibilityFeatures.OpenChestsSilently")
                && !(plugin.getPluginHookMgr() != null && plugin.getPluginHookMgr().isHookActive(OpenInvHook.class));

    }

    private boolean isShulkerBox(Inventory inv) {

        try {

            return inv.getType() == InventoryType.SHULKER_BOX;

        } catch (NoSuchFieldError e) {

            return false;

        }

    }

    private boolean isShulkerBox(InventoryView inv) {

        return isShulkerBox(inv.getTopInventory());

    }

    private boolean isBarrel(InventoryView inv) {

        try {

            return inv.getType() == InventoryType.BARREL;

        } catch (NoSuchFieldError e) {

            return false;

        }

    }

    private boolean isSilentOpenInventory(InventoryView inv) {

        return inv.getType() == InventoryType.CHEST || inv.getType() == InventoryType.ENDER_CHEST || isShulkerBox(inv)
                || isBarrel(inv);

    }

    public boolean hasSilentlyOpenedChest(Player p) {

        return playerStateInfoMap.containsKey(p);

    }

    @Override
    public void onEnable() {

        // ProtocolLib 5.0 maps PLAYER_INFO to ClientboundPlayerInfoUpdatePacket
        final SilentOpenChestPacketAdapter packetAdapter = new SilentOpenChestPacketAdapter(this);
        ProtocolLibrary.getProtocolManager().addPacketListener(packetAdapter);

    }

    private record StateInfo(boolean canFly, boolean isFlying, GameMode gameMode, Location openLoc) {

        static StateInfo extract(Player p) {

            return new StateInfo(p.getAllowFlight(), p.isFlying(), p.getGameMode(), p.getLocation());

        }

    }

}
