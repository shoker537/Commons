package ru.shk.commons.utils.nms.entity.display;

import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.world.entity.Display;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.Range;
import ru.shk.commons.utils.nms.FieldMappings;

@SuppressWarnings("unused")
public class PacketTextDisplay extends PacketDisplay {
//    private static Class<?> alignClass = null;
    public PacketTextDisplay(World world, double x, double y, double z) {
        super(Type.TEXT,"text_display", world, x, y, z);
//        if(alignClass==null) alignClass =
//                Arrays.stream(entity.getClass().getMethods()).filter(method -> Modifier.isStatic(method.getModifiers()) && method.getName().equalsIgnoreCase(FieldMappings.TEXTDISPLAY_GETALIGNMENT.getField())).findAny().get().getReturnType();
    }
    public PacketTextDisplay(Location l){
        this(l.getWorld(), l.getX(), l.getY(), l.getZ());
    }
    public boolean defaultBackground(){
        return getFlag(4);
    }

    public void defaultBackground(boolean b){
        setFlag(4, b);
    }
    @SneakyThrows
    public void text(Component component){
        net.minecraft.network.chat.Component c = net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serialize(component));
        if(compatibility) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETTEXT.getField(), net.minecraft.network.chat.Component.class).invoke(
                entity, c
        ); else ((Display.TextDisplay)entity).setText(c);
        metadata();
    }

    @SneakyThrows
    public void background(Color color){
        if(compatibility) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETBACKFROUNDCOLOR.getField(), int.class).invoke(entity, color.asARGB()); else ((Display.TextDisplay)entity).setBackgroundColor(color.asARGB());
        metadata();
    }

    @SneakyThrows
    public Color background(){
        return Color.fromARGB(compatibility ? (int)entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETBACKFROUNDCOLOR.getField()).invoke(entity) : ((Display.TextDisplay)entity).getBackgroundColor());
    }
    @SneakyThrows
    public Component text(){
        return GsonComponentSerializer.gson().deserialize(net.minecraft.network.chat.Component.Serializer.toJson(compatibility ? (net.minecraft.network.chat.Component) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETTEXT.getField()).invoke(entity) : ((Display.TextDisplay)entity).textRenderState().text()));
    }
//    @SneakyThrows
//    public void alignment(TextDisplay.TextAlignment alignment){
//        // Display.TextDisplay.Align.valueOf()
//        Enum<?> v = (Enum<?>) alignClass.getMethod("valueOf", String.class).invoke(null, alignment.name());
//
//    }
    @SneakyThrows
    public TextDisplay.TextAlignment alignment(){
        // CraftTextDisplay
        // Display.TextDisplay.getAlign()
//        return TextDisplay.TextAlignment.valueOf(align().name());
        boolean f8 = getFlag(8);
        boolean f16 = getFlag(16);
        if(f8 && !f16) return TextDisplay.TextAlignment.LEFT;
        if(!f8 && f16) return TextDisplay.TextAlignment.RIGHT;
        return TextDisplay.TextAlignment.CENTER;
    }

    public void alignment(TextDisplay.TextAlignment alignment) {
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

    private boolean getFlag(int flag) {
        return (flags() & flag) != 0;
    }

    private void setFlag(int flag, boolean set) {
        byte flagBits = flags();

        if (set) {
            flagBits |= flag;
        } else {
            flagBits &= ~flag;
        }

        flags(flagBits);
    }

//    @SneakyThrows
//    private Display.TextDisplay.Align align(){
//        // Display.TextDisplay.getAlign()
//        return (Display.TextDisplay.Align) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETALIGNMENT.getField(), alignClass).invoke(null, flags());
//    }

    @SneakyThrows
    private void flags(byte flags){
        if(compatibility) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETFLAGS.getField(), byte.class).invoke(entity, flags); else ((Display.TextDisplay)entity).setFlags(flags);
        metadata();
    }
    @SneakyThrows
    private byte flags(){
        return compatibility ? (byte) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETFLAGS.getField()).invoke(entity) : ((Display.TextDisplay)entity).getFlags();
    }
    @SneakyThrows
    public int lineWidth(){
        return compatibility ? (int) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETLINEWIDTH.getField()).invoke(entity) : ((Display.TextDisplay)entity).getLineWidth();
    }
    @SneakyThrows
    public void lineWidth(int width){
        if(compatibility) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETLINEWIDTH.getField(), int.class).invoke(entity, width); else ((Display.TextDisplay)entity).setLineWidth(width);
        metadata();
    }
    @SneakyThrows
    public void textOpacity(@Range(from = 0, to = 252) int opacity){
        if(compatibility) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETTEXTOPACITY.getField(), byte.class).invoke(entity, intOpacityToByte(opacity)); else ((Display.TextDisplay)entity).setTextOpacity(intOpacityToByte(opacity));
        metadata();
    }
    @SneakyThrows
    public void textOpacityRaw(byte opacity){
        if(compatibility) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETTEXTOPACITY.getField(), byte.class).invoke(entity, opacity); else ((Display.TextDisplay)entity).setTextOpacity(opacity);
        metadata();
    }

    private static byte intOpacityToByte(int opacity){
        opacity+=4;
        if(opacity>=128) return (byte) -(256-opacity);
        return (byte) (opacity);
    }
}
