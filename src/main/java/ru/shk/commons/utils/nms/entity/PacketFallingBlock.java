package ru.shk.commons.utils.nms.entity;

import lombok.Getter;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;
import ru.shk.commons.utils.nms.PacketUtil;

public class PacketFallingBlock extends PacketEntity<PacketFallingBlock> {
    private static final String entityClass = "net.minecraft.world.entity.item.EntityFallingBlock";
    private static final String entityTypeId = "L";
    @Getter private final BlockData blockData;
    @Getter private final Location startPosition;
    public PacketFallingBlock(BlockData block, World world, double x, double y, double z) {
        this(block, new Location(world, x,y,z));
    }

    public PacketFallingBlock(BlockData block, Location l) {
        super(entityClass, entityTypeId, l.getWorld());
        this.startPosition = l;
        this.blockData = block;
        createEntity(l);
    }
    @Override
    public void createEntity(World world) {}

    private void createEntity(Location location){
        entity = new FallingBlockEntity((Level) PacketUtil.getNMSWorld(location.getWorld()), location.getX(), location.getY(), location.getZ(), ((CraftBlockData)blockData).getState());
    }

}
