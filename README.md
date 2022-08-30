**shoker's commons** is an API to make my life easier. And maybe even yours ;)

**Mostly used by me for private networks.**

![Latest version](https://img.shields.io/nexus/r/ru.shk/Commons?nexusVersion=3&server=https%3A%2F%2Fnexus.shoker.su&style=for-the-badge&label=Commons)

Compiled with Java 17

Modules which use NMS support 1.17.1, 1.18.1 and 1.18.2

# Usage

Commons requires to be installed as a plugin. You can download the latest version [here](https://nexus.shoker.su/service/rest/v1/search/assets/download?sort=version&group=ru.shk&q=Commons&repository=maven-releases)

Also don't forget to mention Commons as a dependency in plugin.yml.

```groovy
repositories {
    maven { url = 'https://nexus.shoker.su/repository/maven-releases/' }
}
dependencies {
    compileOnly 'ru.shk:Commons:1.3.3'
}
```
All versions can be found [here](https://nexus.shoker.su/#browse/browse:maven-releases:ru%2Fshk%2FCommons)

# Modules:

- **Commons** - spigot library for common methods. Can be found using **Commons.*** or **Commons.getInstance().***
- **ConfigAPI** - simple config-management interface (spigot side only for now)
- **GUILib** - spigot library for creating GUIs
- **PacketUtil** - simply send packets with no nms* imports (limited functionality for now). Supports 1.17.1, 1.18.1, 1.18.2, 1.19.1 and 1.19.2 versions. Explore all the methods!
- **GUILibBungee** - bungee library for GUIs (requires Protocolize)

Be careful at importing the correct Commons class:
- **ru.shk.commons.Commons** for Spigot plugins
- **ru.shk.commonsbungee.Commons** for Bungee plugins

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

### Threads/Scheduling:

- **sync()** executes a Runnable synchronously
- **async()** executes a Runnable in a fixed ThreadPool
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

# GUILib (spigot-side)

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
        .withUniversalAction((type, slot, itemStack) -> player.sendMessage("You clicked at "+slot))
        .open(player);
```

Updating items in GUI after its creation:
```java
gui.setItemRaw(2, new ItemStack(Material.PAPER));
```

# GUILib (bungee)

Uses [Protocolize](https://github.com/Exceptionflug/protocolize)
```java
new GUI(plugin, "&cTitle", InventoryType.GENERIC_9X5)
.item(12, new ItemStack(ItemType.ALLIUM), this::action)
.open(player);
```

### Page-generator
If you need to make a GUI with pages, use GUIPageGenerator. 

It can be used as a new instance or you can make your own class and extend it with GUIPageGenerator:
```java
public class FriendsGUI extends GUI {
    public FriendsGUI(Plugin plugin, ProxiedPlayer player){
        super(plugin, "&eYour BROs", InventoryType.GENERIC_9X5);
        new FriendsPageGenerator(player, this);
    }
}

public class FriendsPageGenerator extends GUIPageGenerator {
    private final List<Friend> friends;
    private final ProxiedPlayer player;
    
    public FriendsPageGenerator(ProxiedPlayer player, MyAwesomeGUI gui) {
        super(player,
                gui,
                0, // How many lines to skip (0 if you want the generator to work from the first slot)
                5, // Count of lines for generation items (the entries which the page should show)
                new ItemStackBuilder(ItemType.RED_STAINED_GLASS).displayName("&cNo friends found :(").build(), // The item shown when the page is empty
                22, // The slot for nothing-found item
                new ItemStackBuilder(ItemType.YELLOW_STAINED_GLASS_PANE).build() // The item which covers the last line of the GUI, it is a 'system' line with controls of a page (prev/next page buttons)
                );
        // Receive a list of entries the page should show
        friends = FriendsPlugin.getFriends(player.getUniqueId());
        // Now we need to set a function which checks if next or previous page exists to show buttons or not:
        setPageExistsCheck(page -> {
            if(page<0) return false; // Disabling 'back' on the first page, means the first opened page is the leftmost
            return page*getCountOfGeneratedItems()<friends.size();
        });
        // Now a function which generates a list of entries to show on a specific page
        setPageGenerator(page -> {
            List<Pair<ItemStack, Consumer<InventoryClick>>> items = new ArrayList<>();
            
            friends.stream().skip(page* 27L).limit(27).forEachOrdered(friend -> {
                items.add(Pair.of(new ItemStackBuilder(ItemType.PLAYER_HEAD).headOwner(friend.getName()).build(), null)); // null if no action required or a click consumer
            });
            
            return items;
        });
        generatePage();
    }
    
    @Override
    public void fillBotomPanes(){
        super.fillBottomPanes();
        // Here you can override items on the last (system) line of the GUI (where prev/next buttons appear)
        // Note that prev/next buttons take first and the last slots on this line, so you should not use them in any way
        item(49, new ItemStackBuilder(ItemType.BARRIER).displayName("Close"), click -> {
            getGui().close(player);
        });
    }
}
```

# Utility classes (spigot-side)

- **ItemStackBuilder** - to create ItemStacks simply
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