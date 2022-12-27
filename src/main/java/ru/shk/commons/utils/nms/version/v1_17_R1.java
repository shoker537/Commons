package ru.shk.commons.utils.nms.version;

import ru.shk.commons.utils.nms.Version;

public class v1_17_R1 extends Version {
//    @Override
//    public void sendPacket(Player p, Packet<?> packet) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
//        Method getHandle = p.getClass().getMethod("getHandle");
//        Object nmsPlayer = getHandle.invoke(p);
//        Field con_field = nmsPlayer.getClass().getField("b");
//        Object con = con_field.get(nmsPlayer);
//        Method packet_method = con.getClass().getMethod("sendPacket", Packet.class);
//        packet_method.invoke(con, packet);
//    }
//
//    @Override
//    protected Packet<?> createScoreboardTeamPacket(boolean createTeamOrUpdate, boolean collideTeammates, String name, String prefix, String suffix, ChatColor color, List<String> entries) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//        PlayerTeam t = new PlayerTeam(new Scoreboard(), name);
//        if(!collideTeammates) disableTeammatesCollision(t);
//        t.getClass().getMethod("setPefix", Component.class).invoke(t,Component.nullToEmpty(prefix));
//        t.getClass().getMethod("setSuffix", Component.class).invoke(t,Component.nullToEmpty(suffix));
//        if(color!=null) t.setColor(ChatFormatting.valueOf(color.name()));
//        if(entries!=null) ((Collection)t.getClass().getMethod("getPlayerNameSet").invoke(t)).addAll(entries);
//        return PacketPlayOutScoreboardTeam.a(t, createTeamOrUpdate);
//    }
//
//    @Override
//    public Packet<?> createSetBlockPacket(Block block) {
//        return null;
//    }
//
//    @Override
//    protected Packet<?> createRemoveTeamPacket(String team) {
//        return ClientboundSetPlayerTeamPacket.createRemovePacket(new PlayerTeam(new Scoreboard(), team));
//    }
//
//    @Override
//    public String getItemTypeTranslationKey(Material m) {
//        Item nmsItem = CraftMagicNumbers.getItem(m);
//        if (nmsItem == null) return null;
//        return nmsItem.getName();
//    }
//
//    @Override
//    public ItemStack asNMSCopy(org.bukkit.inventory.ItemStack itemStack) {
//        return CraftItemStack.asNMSCopy(itemStack);
//    }
//
//    @Override@SneakyThrows
//    public Object getNMSWorld(World world) {
//        Class<?> wClass = Class.forName("net.minecraft.world.level.World");
//        Class<?> c = Class.forName("org.bukkit.craftbukkit.v1_17_R1.CraftWorld");
//        val craftWorld = c.cast(world);
//        return wClass.cast(craftWorld.getClass().getMethod("getHandle").invoke(craftWorld));
//    }
//
//    @Override
//    protected void explodeFirework(Player p, Location l, org.bukkit.inventory.ItemStack firework) {
//        explodeFirework(p, l, firework, "ad", "Z");
//    }
//
//    @Override@SneakyThrows
//    public net.minecraft.world.level.block.Block getBlock(Material m) {
//        Class<?> c = Class.forName("org.bukkit.craftbukkit.v1_17_R1.util.CraftMagicNumbers");
//        return (net.minecraft.world.level.block.Block) c.getMethod("getBlock", Material.class).invoke(null, m);
//    }
//
//    @Override@SneakyThrows
//    protected void playRiptideAnimation(Player p, int ticks){
//        Class<?> c = Class.forName("org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer");
//        Class<?> c1 = Class.forName("net.minecraft.world.entity.EntityLiving");
//        val craftPlayer = c.cast(p);
//        val worldPlayer = craftPlayer.getClass().getMethod("getHandle").invoke(craftPlayer);
//        val worldEntity = c1.cast(worldPlayer);
//        worldEntity.getClass().getMethod(FieldMappings.ENTITYHUMAN_STARTUSERIPTIDE.getField(), int.class).invoke(worldEntity, ticks);
//    }

}
