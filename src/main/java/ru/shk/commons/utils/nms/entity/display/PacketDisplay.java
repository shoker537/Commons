package ru.shk.commons.utils.nms.entity.display;

import com.mojang.math.Transformation;
import lombok.SneakyThrows;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.shk.commons.utils.nms.PacketUtil;
import ru.shk.commons.utils.nms.entity.PacketEntity;

@SuppressWarnings("unused")
public class PacketDisplay extends PacketEntity<PacketDisplay> {

    public PacketDisplay(Type type, String entityTypeId, World world, double x, double y, double z) {
        super(type.fullClassName(), entityTypeId, world, x, y, z);
    }

    public PacketDisplay(Type type, String entityTypeId, Location l){
        this(type, entityTypeId, l.getWorld(), l.getX(), l.getY(), l.getZ());
    }

    @SneakyThrows
    public synchronized void interpolationDuration(int ticks){
        interpolationDuration(ticks, true);
    }
    @SneakyThrows
    public synchronized void interpolationDuration(int ticks, boolean sendMetadata){
        ((Display)entity).setInterpolationDuration(ticks);
        if(sendMetadata) metadata();
    }
    @SneakyThrows
    public int interpolationDuration(){
        return ((Display)entity).getInterpolationDuration();
    }
    @SneakyThrows
    public synchronized void startInterpolation(int ticks){
        startInterpolation(ticks, true);
    }
    @SneakyThrows
    public synchronized void startInterpolation(int ticks, boolean sendMetadata){
        ((Display)entity).setInterpolationDelay(ticks);
        if(sendMetadata) metadata();
    }

    @SneakyThrows
    public int startInterpolation() {
        return ((Display)entity).getInterpolationDelay();
    }
    @SneakyThrows
    public synchronized void transform(org.bukkit.util.Transformation transformation){
        Transformation newTransformation = new Transformation(transformation.getTranslation(), transformation.getLeftRotation(), transformation.getScale(), transformation.getRightRotation());
        ((Display)entity).setTransformation(newTransformation);
        metadata();
    }
    @SneakyThrows
    public org.bukkit.util.Transformation transformation(){
        Transformation t = Display.createTransformation((SynchedEntityData) getEntityData());
        return new org.bukkit.util.Transformation(t.getTranslation(), t.getLeftRotation(), t.getScale(), t.getRightRotation());
    }

    @SneakyThrows
    public synchronized void followLookType(FollowLookType type){
        ((Display) entity).setBillboardConstraints(Display.BillboardConstraints.valueOf(type.name()));
    }

    protected enum Type {
        TEXT("TextDisplay"), ITEM("ItemDisplay"), BLOCK("BlockDisplay");
        private final String clazz;

        Type(String clazz) {
            this.clazz = clazz;
        }

        public String fullClassName(){
            return "net.minecraft.world.entity.Display$"+clazz;
        }
    }
    public enum FollowLookType {

        /**
         * No rotation (default).
         */
        FIXED,
        /**
         * Can pivot around vertical axis.
         */
        VERTICAL,
        /**
         * Can pivot around horizontal axis.
         */
        HORIZONTAL,
        /**
         * Can pivot around center point.
         */
        CENTER;
    }
    @Override
    protected void sendSpawnPacket(Player p) {
        PacketUtil.spawnEntity(p, entity);
    }
}
