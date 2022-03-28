**shoker's commons** is an API to make my life easier. And maybe even yours ;)

Modules which use NMS support 1.17.1 and 1.18.2.

#Importing

```groovy
repositories {
    maven { url = 'https://nexus.shoker.su/repository/maven-releases/' }
}
dependencies {
    compileOnly 'ru.shk:Commons:1.3.3'
}
```
Check out the latest version [here](https://nexus.shoker.su/#browse/browse:maven-releases:ru%2Fshk%2FCommons)

# Modules:

- **Commons** - spigot library for common methods
- **CommonsBungee** - bungee library for common methods
- **ConfigAPI** - simple config-management interface (spigot side only for now)
- **GUILib** - spigot library for creating GUIs
- **GUILibBungee** - bungee library for GUIs (requires Protocolize)

#Commons (spigot-side)

- **getServerVersion** returns PacketVersion - server version (using server package) used by the plugin for NMS compatibility
- **getCustomHead** returns a CustomHead defined in a database
- **secondsToTime** turns seconds to a String in format "0:00"
- **currentSeconds** returns current time in seconds
- **registerEvents** registers a Listener class
- **firework** spawns and detonates a no-damage firework
- **info** sends a message to console (auto colorizing)
- **warning** sends a red message to console (auto colorizing)
- **colorize** returns a String with translated color codes

### Threads/Scheduling:

- **sync** executes a Runnable synchronously
- **async** executes a Runnable in async ThreadPool
- **syncLater** executes a Runnable synchronously with delay
- **asyncLater** executes a Runnable asynchronously with delay
- **syncRepeating** executes a Runnable synchronously with delay and period
- **asyncRepeating** executes a Runnable asynchronously with delay and period

#GUILib (spigot-side)

GUI examples:
```java
GUI gui = new GUI(plugin, 27, "&cSelect a player");

gui.addItem(14, new ItemStackBuilder(Material.PLAYER_HEAD).skullOwner(Bukkit.getOfflinePlayer("shoker137")), this::clicked);

gui.addItem(20, new ItemStack(Material.REDSTONE));
gui.addSlotAction(20, this::slotAction);

gui.open(player);
```

```java
new GUI(plugin, 27, "&cSelect a player")
        .addItem(14, new ItemStackBuilder(Material.PLAYER_HEAD).skullOwner(Bukkit.getOfflinePlayer("shoker137")))
        .withUniversalAction((type, slot, itemStack) -> player.sendMessage("You clicked at "+slot));
        .open(player);
```

Updating items in GUI after its creation:
```java
gui.setItemRaw(2, new ItemStack(Material.PAPER));
```

#GUILib (bungee)

Uses [Protocolize](https://github.com/Exceptionflug/protocolize)
```java
new GUI("&cHello", InventoryType.GENERIC_9X5)
.item(12, new ItemStack(ItemType.ALLIUM), this::action)
.open(player);
```

#Utility classes (spigot-side)

- **ItemStackBuilder** - to create ItemStacks simply
- **TextComponentBuilder** - to create md_5's TextComponents simply
- **Coordinates** - xyz holder and can be converted to Location
- **WorldEditManager** - select, copy, fill and work with schematics
- **PacketUtil** - a few useful packet-based methods
- **SB** - scoreboard implementation

#SB - simple scoreboards

Example:

```java
SB sb = new SB("&bMyCoolScoreboard", "lobby", 16);
sb.addLine("&aStatic never-changing line");
sb.addBlank();
sb.addDynamicLine("line-identifier", "&bLeft part: ", "&cRight part");
sb.addDynamicLine("status", "&bStatus: ", "&fWaiting");
sb.addLine("&f      example.com      ");
```
Or using builder:
```java
SB sb = new SB("&bMyCoolScoreboard", "lobby", 16)
    .line("&aStatic never-changing line")
    .blankLine()
    .dynamicLine("line-identifier", "&bLeft part: ", "&cRight part")
    .dynamicLine("status", "&bStatus: ", "&fWaiting")
    .line("&f      example.com      ");
```
Showing:
```java
player.setScoreboard(sb.getBoard());
```
Updating lines personally (dynamic lines only; using packets):
```java
SB.updateScoreboardPersonally(player, "status", "Status: ", "YourPersonalStatus");
```