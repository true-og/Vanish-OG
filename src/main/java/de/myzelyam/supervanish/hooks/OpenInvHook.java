package de.myzelyam.supervanish.hooks;

import de.myzelyam.api.vanish.PlayerShowEvent;
import de.myzelyam.api.vanish.PostPlayerHideEvent;
import de.myzelyam.supervanish.SuperVanish;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OpenInvHook extends PluginHook {

    private boolean errorLogged = false;

    private final Set<UUID> alreadyHiddenBeforeVanishing = new HashSet<>();

    public OpenInvHook(SuperVanish superVanish) {

        super(superVanish);

    }

    @EventHandler
    public void onVanish(PostPlayerHideEvent e) {

        try {

            Player p = e.getPlayer();

            if (getSilentContainerStatus(p)) {

                alreadyHiddenBeforeVanishing.add(p.getUniqueId());

            } else {

                if (!p.hasPermission("sv.silentchest"))
                    return;
                setSilentContainerStatus(p, true);

            }

        } catch (Exception | NoSuchMethodError | NoClassDefFoundError er) {

            if (!errorLogged) {

                superVanish.logException(er);
                errorLogged = true;

            }

        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReappear(PlayerShowEvent e) {

        try {

            Player p = e.getPlayer();

            if (!alreadyHiddenBeforeVanishing.remove(p.getUniqueId())) {

                setSilentContainerStatus(p, false);

            }

        } catch (Exception | NoSuchMethodError | NoClassDefFoundError er) {

            if (!errorLogged) {

                superVanish.logException(er);
                errorLogged = true;

            }

        }

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent e) {

        try {

            Player p = e.getPlayer();

            if (superVanish.getVanishStateMgr().isVanished(p.getUniqueId())) {

                if (getSilentContainerStatus(p)) {

                    alreadyHiddenBeforeVanishing.add(p.getUniqueId());

                } else {

                    if (!p.hasPermission("sv.silentchest"))
                        return;
                    setSilentContainerStatus(p, true);

                }

            }

        } catch (Exception | NoSuchMethodError | NoClassDefFoundError er) {

            if (!errorLogged) {

                superVanish.logException(er);
                errorLogged = true;

            }

        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {

        try {

            Player p = e.getPlayer();

            if (superVanish.getVanishStateMgr().isVanished(p.getUniqueId())) {

                if (!alreadyHiddenBeforeVanishing.remove(p.getUniqueId())) {

                    setSilentContainerStatus(p, false);

                }

            }

        } catch (Exception | NoSuchMethodError | NoClassDefFoundError er) {

            if (!errorLogged) {

                superVanish.logException(er);
                errorLogged = true;

            }

        }

    }

    public boolean openPlayerInventory(Player vanished, Player target) {

        try {

            Method getSpecialInventoryMethod = plugin.getClass().getMethod("getSpecialInventory", Player.class,
                    boolean.class);
            Inventory specialInventory = (Inventory) getSpecialInventoryMethod.invoke(plugin, target, true);
            Method openInventoryMethod = plugin.getClass().getMethod("openInventory", Player.class, Inventory.class);
            openInventoryMethod.invoke(plugin, vanished, specialInventory);

        } catch (Exception | NoSuchMethodError | NoClassDefFoundError e) {

            if (!errorLogged) {

                superVanish.logException(e);
                errorLogged = true;

            }

            return false;

        }

        return true;

    }

    private boolean getSilentContainerStatus(Player player) throws ReflectiveOperationException {

        Method method = plugin.getClass().getMethod("getSilentContainerStatus", Player.class);
        return (Boolean) method.invoke(plugin, player);

    }

    private void setSilentContainerStatus(Player player, boolean status) throws ReflectiveOperationException {

        Method method = plugin.getClass().getMethod("setSilentContainerStatus", Player.class, boolean.class);
        method.invoke(plugin, player, status);

    }

}
