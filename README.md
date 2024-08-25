**shoker's commons** is an API to make my life easier. And maybe even yours ;)

**Mostly used by me for private networks.**

![Latest version](https://img.shields.io/nexus/r/ru.shk/Commons?server=https%3A%2F%2Fnexus.shoker.su&nexusVersion=3&style=for-the-badge&logo=Commons&color=%2313ba59)

Compiled with Java 17

Modules which use NMS support v1.20.1

# Usage

Commons requires to be installed as a plugin. You can download the latest version [here](https://nexus.shoker.su/service/rest/v1/search/assets/download?sort=version&group=ru.shk&q=Commons&repository=maven-releases)

Also don't forget to mention Commons as a dependency in plugin.yml.

```groovy
repositories {
    maven { url = 'https://nexus.shoker.su/repository/maven-releases/' }
}
dependencies {
    compileOnly 'ru.shk:Commons:1.5.8' // LOOK AT THE LATEST VERSION ON TOP ^^^
}
```
All versions can be found [here](https://nexus.shoker.su/#browse/browse:maven-releases:ru%2Fshk%2FCommons)

# Modules:

- **Commons** - spigot library for common methods. Can be found using **Commons.*** or **Commons.getInstance().***
- **ConfigAPI** - simple config-management interface (spigot side only for now). ConfigAPI.getServerName() returns readable name of current server (from 'server-name' in server.properties)
- **GUILib** - spigot library for creating GUIs
- **PacketUtil** - simply send packets with no nms* imports (only implementations needed for me). Supports 1.20.1 version. Explore nms package!
- **GUILibBungee** - bungee library for GUIs (requires Protocolize)
- **PlayerLocationReceiver** - receives player coordinates from Bungee (Commons.getInstance().getPlayerLocationReceiver())

Be careful at importing the correct Commons class:
- **ru.shk.commons.Commons** for Spigot plugins
- **ru.shk.commonsbungee.Commons** for Bungee plugins
- **ru.shk.velocity.commons.Commons** for Velocity plugins

# Commons (spigot-side)

- **getServerVersion()** returns PacketVersion - server version (using server package) used by the plugin for NMS compatibility
- **getCustomHead()** returns a CustomHead defined in a database, requires additional setup
- **secondsToTime()** turns seconds to a String in format "0:00"
- **currentSeconds()** returns current time in seconds
- **registerEvents()** registers a Listener class
- **firework()** spawns and detonates a no-damage firework
- **info()** sends a message to console (auto colorizing)
- **warning()** sends a red message to console (auto colorizing)
- **colorize()** returns a String with translated color codes (&)
- **colorizeWithHex()** returns a String with translated color codes (&) and hex colors (&#000000)
- **PAFManager** (Commons.getInstance().getPafManager()) is a PartyAndFriends receiver for parties
- **WorldEditManager** (Commons.getInstance().getWorldEditManager()) is an early version of worldedit manager to simplify interaction with WorldEdit

### Threads/Scheduling:

- **sync()** executes a Runnable synchronously
- **async()** executes a Runnable in a ThreadPool
- **syncLater()** executes a Runnable synchronously with delay
- **asyncLater()** executes a Runnable asynchronously with delay
- **syncRepeating()** executes a Runnable synchronously with delay and period
- **asyncRepeating()** executes a Runnable asynchronously with delay and period

# ConfigAPI
Examples:
```java
// true means automatically assign file name to 'config.yml'
Config config = new Config(getDataFolder(), true);

// create config from File
Config config = new Config(new File(getDataFolder()+File.separator+"config.yml"));

// create config with custom name, auto adds .yml if it is not provided
Config config = new Config(getDataFolder(), "settings");
```
**Config** is YamlConfiguration so you can use any getters/setters as usual. But there are some extra methods:
```java
// sets the value and automatically saves config to a file
config.setValue("value", true);

// save without try/catch
config.save();
```
```java
// returns Location stored in config as String
Config.decodeLocation(config.getString("location-from-config"));

// returns a String created from Location
Config.encodeLocation(new Location(...));

// automatically gets a string in config and decodes to Location
config.location("location-in-config");
```

```java
// executes an operation if config has a 'config-key' value intended to be Integer
Config.getIfHasInt(config, "config-key", (integer) -> this::keyFound);

// executes an operation if config has a 'config-key' value intended to be Boolean
Config.getIfHasBoolean(config, "config-key", (b) -> this::keyFound);

// executes an operation if config has a 'config-key' value intended to be String
Config.getIfHasString(config, "config-key", (string) -> this::keyFound);

// executes an operation if config has a 'config-key' value intended to be List<String>
Config.getIfHasStringList(config, "config-key", (list) -> this::keyFound);
```

# GUILib is deprecated for all platforms.
## Now you can use modern utils.gui.* classes:

Generic GUI example:
```java
    // for Bukkit
    var gui = new BukkitGUI(plugin, GUIType.CHEST, player, Component.text("Title"))
            .lines(3) // making it a 3-row chest gui
            .item(18, new BukkitItemStack(Material.ARROW).displayName("&c< Back"), e -> back()) // adding an item with click handler
            ;
    // for velocity
    var gui = new VelocityGUI(plugin, GUIType.ANVIL, player, Component.text("Title"))
            .item(0, new VelocityItemStack(ItemType.PLAYER_HEAD).headOwner(player.getUniqueId()).displayName(player.getUsername())) // adding an item without click action
            .universalClick(click -> onClick(click)) // universal item click handler
            ;
    
    // OR if you need a platform-independent code you can use premade static methods:
    GUI.chest(plugin, player, title, lines).item(0, ItemStackBuilder.newEmptyStack().type("obsidian").displayName("Click me!"));
    GUI.anvil(plugin, player, title);
    
    // Of course you can use a regular variable to achieve the same:
    gui.lines();
    gui.item();
    gui.universalClick();
    
    // And for sure you have to open it to a player as well as manage its state
    gui.open();
    gui.update();
    gui.close();
    
```

## Paged GUIs
A new Paged system appears:

```java
    // Let's imagine we have a GUI
    var gui = GUI.chest(plugin, player, Component.text("title"), 3);
    
    // Then we can create Paged and attach it to our GUI
    var paged = new Paged<SomeItem>()
            .lineStartsAt(1)
            .lineEndsAt(2)
            // It's a generator: it converts page number and items on page to a list of items you need to display
            .pageGenerator((pageNumber, itemsOnPage) -> generateItems(pageNumber, itemsOnPage))
            // The page checker checks if there's a page depending on the page number and items per page
            .pageChecker((pageNumber, itemsOnPage) -> true)
            // Item converter generates an ItemStackBuilder for each SomeItem
            .itemConverter(item -> new VelocityItemStack())
            // Called each time any SomeItem is clicked
            .onClickItem(click -> clicked(click))
            // [Optional] Overlays are service buttons like arrows to change the page, they are being placed on bottom of the GUI
            // If you need to show some service items (in addition to page arrows), you can use this provider
            // Keep in mind that 0 and 8 slots are always reserved for arrows
            .overlaysGenerator(overlays -> overlays.item(3, item, onClicked()))
            // [Optional]
            // If you don't want a service line to take place, set it to false. Defaults to true.
            .useServiceLine(false)
    ;
    
```

# Utility classes (spigot-side)

- **BukkitItemStack** - to create ItemStacks simply
- **TextComponentBuilder** - to create md_5's TextComponents simply
- **Coordinates** - xyz holder and can be converted to Location
- **WorldEditManager** - select, copy, fill and work with schematics
- **PacketUtil** - a few useful packet-based methods
- **SB** - scoreboard implementation

# SB - simple scoreboards

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
