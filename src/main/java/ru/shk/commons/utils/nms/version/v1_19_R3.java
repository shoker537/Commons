package ru.shk.commons.utils.nms.version;

import lombok.SneakyThrows;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.bukkit.entity.Player;
import ru.shk.commons.utils.nms.FieldMappings;
import ru.shk.commons.utils.nms.ReflectionUtil;
import ru.shk.commons.utils.nms.Version;

import java.util.List;

public class v1_19_R3 extends Version {
    @Override@SneakyThrows
    protected void entityMetadata(Player p, Object e, boolean full) {
        Entity entity = (Entity) e;
        if(!full) {
            sendPacket(p, new ClientboundSetEntityDataPacket(entityId(entity), (List<SynchedEntityData.DataValue<?>>)ReflectionUtil.runMethod(entityData(entity), "packDirty")));
            return;
        }
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(entityId(entity), (List<SynchedEntityData.DataValue<?>>)ReflectionUtil.runMethod(entityData(entity), "packAll"));
        sendPacket(p, packet);
    }

    private SynchedEntityData entityData(Entity e){
        return (SynchedEntityData) ReflectionUtil.runMethod(e, FieldMappings.ENTITY_GETDATAWATCHER.getField());
    }


}
