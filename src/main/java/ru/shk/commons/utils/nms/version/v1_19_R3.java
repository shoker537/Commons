package ru.shk.commons.utils.nms.version;

import lombok.SneakyThrows;
import net.minecraft.network.protocol.Packet;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.bukkit.entity.Player;
import ru.shk.commons.utils.nms.FieldMappings;
import ru.shk.commons.utils.nms.Version;

import java.lang.reflect.Method;

public class v1_19_R3 extends Version {
    @Override@SneakyThrows
    protected void entityMetadata(Player p, Object e) {
        Class<?> packet = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata");
        Object dataWatcher = e.getClass().getMethod(FieldMappings.ENTITY_GETDATAWATCHER.getField()).invoke(e);
        int id = (int) e.getClass().getMethod(FieldMappings.ENTITY_GETID.getField()).invoke(e);
        Method m = dataWatcher.getClass().getDeclaredMethod("packAll");
        m.setAccessible(true);
        Packet<?> pk = (Packet<?>) ConstructorUtils.invokeConstructor(packet, new Object[]{id, m.invoke(dataWatcher)});
        sendPacket(p, pk);
    }
}
