package ru.shk.commons.utils.nms.entity.display;

import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import ru.shk.commons.utils.nms.PacketUtil;

@SuppressWarnings("unused")
public class PacketTextDisplay extends PacketDisplay {
    public PacketTextDisplay(World world, double x, double y, double z) {
        super(Type.TEXT,"text_display", world, x, y, z);
    }
    public PacketTextDisplay(Location l){
        this(l.getWorld(), l.getX(), l.getY(), l.getZ());
    }
    public boolean defaultBackground(){
        return getFlag(4);
    }
    @Override
    public void createEntity(World world) {
        entity = new Display.TextDisplay(EntityType.TEXT_DISPLAY, (Level) PacketUtil.getNMSWorld(world));
    }
    public synchronized void defaultBackground(boolean b){
        setFlag(4, b);
    }
    public synchronized void shadowed(boolean shadowed){
        setFlag(1, shadowed);
    }
    public boolean shadowed(){
        return getFlag(1);
    }
    @SneakyThrows
    public synchronized void text(Component component){
        net.minecraft.network.chat.Component c = net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serialize(component));
        ((Display.TextDisplay)entity).setText(c);
        metadata();
    }

    @SneakyThrows
    public synchronized void background(@Nullable Color color){
        int colorInt = color==null?-1:color.asARGB();
        entity.getEntityData().set(Display.TextDisplay.DATA_BACKGROUND_COLOR_ID, color.asARGB());
        metadata();
    }

    @SneakyThrows@Nullable
    public Color background(){
        int color = (((Display.TextDisplay)entity).getBackgroundColor());
        if(color==-1) return null;
        return Color.fromARGB(color);
    }
    @SneakyThrows
    public Component text(){
        return GsonComponentSerializer.gson().deserialize(net.minecraft.network.chat.Component.Serializer.toJson(((Display.TextDisplay)entity).textRenderState().text()));
    }
    @SneakyThrows
    public TextDisplay.TextAlignment alignment(){
        boolean f8 = getFlag(8);
        boolean f16 = getFlag(16);
        if(f8 && !f16) return TextDisplay.TextAlignment.LEFT;
        if(!f8 && f16) return TextDisplay.TextAlignment.RIGHT;
        return TextDisplay.TextAlignment.CENTER;
    }

    public synchronized void alignment(TextDisplay.TextAlignment alignment) {
        Preconditions.checkArgument(alignment != null, "Alignment cannot be null");

        switch (alignment) {
            case LEFT:
                this.setFlag(8, true);
                this.setFlag(16, false);
                break;
            case RIGHT:
                this.setFlag(8, false);
                this.setFlag(16, true);
                break;
            case CENTER:
                this.setFlag(8, false); // LEFT
                this.setFlag(16, false); // RIGHT
                break;
            default:
                throw new IllegalArgumentException("Unknown alignment " + alignment);
        }
        metadata();
    }

    public boolean getFlag(int flag) {
        return (flags() & flag) != 0;
    }

    public synchronized void setFlag(int flag, boolean set) {
        byte flagBits = flags();
        if (set) {
            flagBits = (byte)(flagBits | flag);
        } else {
            flagBits = (byte)(flagBits & (~flag));
        }
        flags(flagBits);
    }

    @SneakyThrows
    public synchronized void flags(byte flags){
        ((Display.TextDisplay)entity).setFlags(flags);
        metadata();
    }
    @SneakyThrows
    public byte flags(){
        return ((Display.TextDisplay)entity).getFlags();
    }
    @SneakyThrows
    public int lineWidth(){
        return ((Display.TextDisplay)entity).getLineWidth();
    }
    @SneakyThrows
    public synchronized void lineWidth(int width){
        entity.getEntityData().set(Display.TextDisplay.DATA_LINE_WIDTH_ID, width);
        metadata();
    }
    @SneakyThrows
    public synchronized void textOpacity(@Range(from = 0, to = 252) int opacity){
        ((Display.TextDisplay)entity).setTextOpacity(intOpacityToByte(opacity));
        metadata();
    }
    @SneakyThrows
    public synchronized void textOpacityRaw(byte opacity){
        ((Display.TextDisplay)entity).setTextOpacity(opacity);
        metadata();
    }

    private static byte intOpacityToByte(int opacity){
        opacity+=4;
        if(opacity>=128) return (byte) -(256-opacity);
        return (byte) (opacity);
    }
}
