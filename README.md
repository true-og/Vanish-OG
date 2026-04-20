# Vanish-OG

A fork of [SuperVanish](https://www.spigotmc.org/resources/supervanish-be-invisible.1331/) for [TrueOG Network](https://trueog.net). Lets staff become completely invisible so they can quietly observe and moderate.

Built for **Purpur 1.19.4**. Clients from **Minecraft 1.8 through 1.21.11** can connect through ViaSuite (ViaVersion / ViaBackwards / ViaRewind) and vanish works correctly across the board.

### What's different from SuperVanish?

- **Tab list hiding works on 1.19+** Upstream switched it off after a Mojang change broke it. Vanish-OG handles the new format so vanished staff stay hidden from the tab list.
- **Spectator indication works on 1.19.4 again.** `MarkVanishedPlayersAsSpectators` now uses the native 1.19.4 player info update packet, so staff who can see vanished players still get the dark-gray/head-only tab list cue across Via-served 1.8-1.21.11 clients.
- **Silent chest opening works on 1.19.4 again.** Peeking into containers looks completely invisible to other players if vanished.
- **All connected clients see the same thing.** Whether someone joins from 1.8 or the latest 1.21.11 release through ViaSuite, vanish behaves identically.
- **[Chat-OG](https://github.com/true-og/Chat-OG) owns announcement visibility.** Vanish-OG no longer rewrites join/quit, death, or advancement messages and no longer emits fake join/quit broadcasts on vanish toggles. Chat-OG is responsible for hiding vanished-player activity from Discord-facing announcements.
- **OpenInv support.** If installed, Vanish-OG hands off chest viewing to OpenInv so staff never enter spectator mode and can't get stuck there after a crash.
- **MiniPlaceholders support** (via Utilities-OG) for use in chat, tab, and hologram plugins.
- **Safer chest peeking.** Staff no longer occasionally fall through the floor when finishing a peek. Pair with [WGamemode](https://github.com/true-og/WGamemode) to auto-restore gamemodes after crashes if OpenInv is not installed.
- **KeyDB-backed storage.** Vanish state and player preferences are stored in KeyDB (Redis-compatible) via the Lettuce client instead of flat YAML files. Requires a `KeyDB.URI` in `config.yml`.
- **Early vanish metadata for TAB-OG compatibility.** The `"vanished"` player metadata is set during `PlayerLoginEvent` (before any `PlayerJoinEvent` handler fires), so plugins like TAB-OG that process joins at lower event priorities can detect vanish state immediately without a race condition.
- **Removed old clutter.** Deprecated functions are removed and the code is updated for java 17 conventions.

### Chat-OG handles some events that used to be handled by SuperVanish

If your stack includes [Chat-OG](https://github.com/true-og/Chat-OG), think of the split like this:

- Vanish-OG manages vanish state, player/entity visibility, tab list hiding, spectator indication, and silent chest behavior.
- Chat-OG manages whether vanished-player join/quit, kick, advancement, death, and Discord bridge announcements are surfaced.

This avoids two plugins trying to suppress or fake the same announcement events.

### API

Vanish-OG ships the same Bukkit-facing API package as SuperVanish: `de.myzelyam.api.vanish`.

### Main entry point

Use [`VanishAPI`](src/main/java/de/myzelyam/api/vanish/VanishAPI.java) for all direct integrations.

```java
import de.myzelyam.api.vanish.VanishAPI;
import net.trueog.utilitiesog.UtilitiesOG;
```

### Commands

Main command: `/vanish` (also `/v`, `/sv`, `/supervanish`).

### State queries

- `VanishAPI.getInvisiblePlayers()`
  Returns the UUIDs of all online vanished players.
- `VanishAPI.getAllInvisiblePlayers()`
  Returns the UUIDs of all vanished players, including offline players stored in KeyDB.
- `VanishAPI.isVanished(UUID uuid)`
  Checks if a player is vanished by querying KeyDB. Returns `boolean`.

Java:

```java
if (VanishAPI.isVanished(player.getUniqueId())) {
    UtilitiesOG.trueogMessage(player, "You are vanished.");
}
```

Kotlin:

```kotlin
if (VanishAPI.isVanished(player.uniqueId)) {
    UtilitiesOG.trueogMessage(player, "You are vanished.")
}
```

### Visibility control

- `VanishAPI.hidePlayer(Player player)`
  Vanishes the player through the normal SuperVanish pipeline and fires the related events. Main thread only.
- `VanishAPI.showPlayer(Player player)`
  Reveals the player through the normal SuperVanish pipeline and fires the related events. Main thread only.
- `VanishAPI.canSee(Player viewer, Player viewed)`
  Returns whether `viewer` should currently be able to see `viewed`. Main thread only.

Java:

```java
if (!VanishAPI.canSee(viewer, target)) {
    UtilitiesOG.trueogMessage(viewer, "<red>That player is hidden from you.");
}

VanishAPI.hidePlayer(target);
VanishAPI.showPlayer(target);
```

Kotlin:

```kotlin
if (!VanishAPI.canSee(viewer, target)) {
    UtilitiesOG.trueogMessage(viewer, "<red>That player is hidden from you.")
}

VanishAPI.hidePlayer(target)
VanishAPI.showPlayer(target)
```

### Configuration access

- `VanishAPI.getConfiguration()`
  Returns `config.yml`. Main thread only; treat the returned `FileConfiguration` as Bukkit-owned mutable state.
- `VanishAPI.getMessages()`
  Returns `messages.yml`. Main thread only; treat the returned `FileConfiguration` as Bukkit-owned mutable state.
- `VanishAPI.reloadConfig()`
  Reloads the plugin. Main thread only.
- `VanishAPI.getPlugin()`
  Returns the active `SuperVanish` plugin instance. Fetching the reference is cheap, but any interaction with the plugin or Bukkit APIs should stay on the main thread.

### Events

The API exposes the following Bukkit events in `de.myzelyam.api.vanish`:

All of these are normal Bukkit events and should be handled on the main server thread. Do not assume they fire asynchronously.

- [`PlayerHideEvent`](src/main/java/de/myzelyam/api/vanish/PlayerHideEvent.java)
  Fired before a player is hidden. Cancellable. Includes `isSilent()` / `setSilent(boolean)`.
- [`PostPlayerHideEvent`](src/main/java/de/myzelyam/api/vanish/PostPlayerHideEvent.java)
  Fired after a player has been hidden. Not cancellable. Includes `isSilent()`.
- [`PlayerShowEvent`](src/main/java/de/myzelyam/api/vanish/PlayerShowEvent.java)
  Fired before a player is shown. Cancellable. Includes `isSilent()` / `setSilent(boolean)`.
- [`PostPlayerShowEvent`](src/main/java/de/myzelyam/api/vanish/PostPlayerShowEvent.java)
  Fired after a player has been shown. Not cancellable. Includes `isSilent()`.
- [`PlayerVanishStateChangeEvent`](src/main/java/de/myzelyam/api/vanish/PlayerVanishStateChangeEvent.java)
  Fired when persisted vanish state changes for a UUID. Cancellable. Exposes `isVanishing()`, `getUUID()`, `getName()`, and `getCause()`.

### Example listener

Java:

```java
@EventHandler
public void onPlayerHide(PlayerHideEvent event) {
    if (!event.getPlayer().hasPermission("myplugin.allowvanish")) {
        UtilitiesOG.trueogMessage(event.getPlayer(), "<red>You are not allowed to vanish.");
        event.setCancelled(true);
    }
}
```

Kotlin:

```kotlin
@EventHandler
fun onPlayerHide(event: PlayerHideEvent) {
    if (!event.player.hasPermission("myplugin.allowvanish")) {
        UtilitiesOG.trueogMessage(event.player, "<red>You are not allowed to vanish.")
        event.isCancelled = true
    }
}
```

## Reporting issues

Open an issue on the [Vanish-OG GitHub repository](https://github.com/true-og/Vanish-OG/issues).
