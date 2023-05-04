package ru.shk.commons.utils.nms.entity.display;

import com.mojang.math.Transformation;
import lombok.SneakyThrows;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.shk.commons.utils.nms.FieldMappings;
import ru.shk.commons.utils.nms.PacketUtil;
import ru.shk.commons.utils.nms.ReflectionUtil;
import ru.shk.commons.utils.nms.entity.PacketEntity;

public class PacketDisplay extends PacketEntity<PacketDisplay> {

    public PacketDisplay(Type type, String entityTypeId, World world, double x, double y, double z) {
        super(type.fullClassName(), entityTypeId, world, x, y, z);
    }
    @SneakyThrows
    public void interpolationDuration(int ticks){
        interpolationDuration(ticks, true);
    }
    @SneakyThrows
    public void interpolationDuration(int ticks, boolean sendMetadata){
//        if(compatibility) ReflectionUtil.runMethod(entity, FieldMappings.DISPLAY_SETINTERPOLATIONDURATION.getField(), ticks); else ((Display)entity).setInterpolationDuration(ticks);
        ((Display)entity).setInterpolationDuration(ticks);
        if(sendMetadata) metadata();
    }
    @SneakyThrows
    public int interpolationDuration(){
        return compatibility ? (int) ReflectionUtil.runMethod(entity.getClass().getSuperclass(), entity, FieldMappings.DISPLAY_GETINTERPOLATIONDURATION.getField()) : ((Display)entity).getInterpolationDuration();
    }
    @SneakyThrows
    public void startInterpolation(int ticks){
        startInterpolation(ticks, true);
    }
    @SneakyThrows
    public void startInterpolation(int ticks, boolean sendMetadata){
//        ReflectionUtil.runMethod(entity, FieldMappings.DISPLAY_SETINTERPOLATIONSTART.getField(), ticks);
        ((Display)entity).setInterpolationDelay(ticks);
        if(sendMetadata) metadata();
    }

    @SneakyThrows
    public int startInterpolation() {
        return (int) ReflectionUtil.runMethod(entity.getClass().getSuperclass(), entity, FieldMappings.DISPLAY_GETINTERPOLATIONSTART.getField());
//        return (int) entity.getClass().getSuperclass().getMethod(FieldMappings.DISPLAY_GETINTERPOLATIONSTART.getField()).invoke(entity);
    }
    @SneakyThrows
    public void transform(org.bukkit.util.Transformation transformation){
        Transformation newTransformation = new Transformation(transformation.getTranslation(), transformation.getLeftRotation(), transformation.getScale(), transformation.getRightRotation());
        if(compatibility) entity.getClass().getMethod(FieldMappings.DISPLAY_SETTRANSFORMATION.getField(), Transformation.class).invoke(entity,
                newTransformation
        ); else ((Display)entity).setTransformation(newTransformation);
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
