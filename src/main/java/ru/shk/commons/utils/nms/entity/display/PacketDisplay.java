package ru.shk.commons.utils.nms.entity.display;

import com.mojang.math.Transformation;
import lombok.SneakyThrows;
import net.minecraft.network.syncher.SynchedEntityData;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.shk.commons.utils.nms.FieldMappings;
import ru.shk.commons.utils.nms.PacketUtil;
import ru.shk.commons.utils.nms.entity.PacketEntity;

public class PacketDisplay extends PacketEntity<PacketDisplay> {

    public PacketDisplay(Type type, String entityTypeId, World world, double x, double y, double z) {
        super(type.fullClassName(), entityTypeId, world, x, y, z);
    }
    @SneakyThrows
    public void interpolationDuration(int ticks){
        entity.getClass().getMethod(FieldMappings.DISPLAY_SETINTERPOLATIONDURATION.getField(), int.class).invoke(entity, ticks);
        metadata();
    }
    @SneakyThrows
    public int interpolationDuration(){
        return (int) entity.getClass().getMethod(FieldMappings.DISPLAY_GETINTERPOLATIONDURATION.getField()).invoke(entity);
    }
    @SneakyThrows
    public void transform(org.bukkit.util.Transformation transformation){
        entity.getClass().getMethod(FieldMappings.DISPLAY_SETTRANSFORMATION.getField(), Transformation.class).invoke(entity,
                new Transformation(transformation.getTranslation(), transformation.getLeftRotation(), transformation.getScale(), transformation.getRightRotation())
        );
        metadata();
    }
    @SneakyThrows
    public org.bukkit.util.Transformation transformation(){
        Transformation t = (Transformation) entity.getClass().getMethod(FieldMappings.DISPLAY_CREATETRANSFORMATION.getField(), SynchedEntityData.class).invoke(null, getEntityData());
        return new org.bukkit.util.Transformation(t.getTranslation(), t.getLeftRotation(), t.getScale(), t.getRightRotation());
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

    @Override
    protected void sendSpawnPacket(Player p) {
        PacketUtil.spawnEntity(p, entity);
    }
}
