package ru.shk.commons.utils.nms.entity.display;

import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import ru.shk.commons.utils.nms.FieldMappings;

public class PacketTextDisplay extends PacketDisplay {
//    private static Class<?> alignClass = null;
    public PacketTextDisplay(World world, double x, double y, double z) {
        super(Type.TEXT,"text_display", world, x, y, z);
//        if(alignClass==null) alignClass =
//                Arrays.stream(entity.getClass().getMethods()).filter(method -> Modifier.isStatic(method.getModifiers()) && method.getName().equalsIgnoreCase(FieldMappings.TEXTDISPLAY_GETALIGNMENT.getField())).findAny().get().getReturnType();
    }
    public boolean defaultBackground(){
        return getFlag(4);
    }

    public void defaultBackground(boolean b){
        setFlag(4, b);
    }
    @SneakyThrows
    public void text(Component component){
        entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETTEXT.getField(), net.minecraft.network.chat.Component.class).invoke(
                entity, net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serialize(component))
        );
        metadata();
    }

    @SneakyThrows
    public void background(Color color){
        entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETBACKGROUND.getField(), int.class).invoke(entity, color.asARGB());
        metadata();
    }

    @SneakyThrows
    public Color background(){
        return Color.fromARGB((int)entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETBACKGROUND.getField()).invoke(entity));
    }
    @SneakyThrows
    public Component text(){
        return GsonComponentSerializer.gson().deserialize(net.minecraft.network.chat.Component.Serializer.toJson((net.minecraft.network.chat.Component) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETTEXT.getField()).invoke(entity)));
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
        entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETFLAGS.getField(), byte.class).invoke(entity, flags);
        metadata();
    }
    @SneakyThrows
    private byte flags(){
        return (byte) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETFLAGS.getField()).invoke(entity);
    }
    @SneakyThrows
    public int lineWidth(){
        return (int) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETLINEWIDTH.getField()).invoke(entity);
    }
    @SneakyThrows
    public void lineWidth(int width){
        entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETLINEWIDTH.getField(), int.class).invoke(entity, width);
        metadata();
    }
}
