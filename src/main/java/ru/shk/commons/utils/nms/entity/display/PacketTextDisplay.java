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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import ru.shk.commons.utils.nms.FieldMappings;
import ru.shk.commons.utils.nms.ReflectionUtil;

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
    public void shadowed(boolean shadowed){
        setFlag(1, shadowed);
    }
    public boolean shadowed(){
        return getFlag(1);
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
    public void background(@Nullable Color color){
        int colorInt = color==null?-1:color.asARGB();
        if(compatibility) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETBACKFROUNDCOLOR.getField(), int.class).invoke(entity, colorInt);
//        else entity.getEntityData().set((EntityDataAccessor<? super Integer>) ReflectionUtil.getStaticField(Display.TextDisplay.class, FieldMappings.TEXTDISPLAY_BACKGROUND_ID.getField()), colorInt);
        else entity.getEntityData().set(Display.TextDisplay.DATA_BACKGROUND_COLOR_ID, color.asARGB());
//        else ReflectionUtil.runMethodWithSingleArgument(Display.TextDisplay.class, entity, FieldMappings.TEXTDISPLAY_SETBACKFROUNDCOLOR.getField(), int.class, colorInt);
        metadata();
    }

    @SneakyThrows@Nullable
    public Color background(){
        int color = (compatibility ? (int)entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETBACKFROUNDCOLOR.getField()).invoke(entity) : ((Display.TextDisplay)entity).getBackgroundColor());
        if(color==-1) return null;
        return Color.fromARGB(color);
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

    public boolean getFlag(int flag) {
        return (flags() & flag) != 0;
    }

    public void setFlag(int flag, boolean set) {
//        Logger.warning("Setting Flag: "+flag+"="+set);
        byte flagBits = flags();
//        Logger.warning("  Current flags: "+flagBits);
        if (set) {
            flagBits = (byte)(flagBits | flag);
        } else {
            flagBits = (byte)(flagBits & (~flag));
        }
//        Logger.warning("  New flags: "+flagBits);
        flags(flagBits);
//        Logger.warning("  Flags set: "+flags());
    }

//    @SneakyThrows
//    private Display.TextDisplay.Align align(){
//        // Display.TextDisplay.getAlign()
//        return (Display.TextDisplay.Align) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETALIGNMENT.getField(), alignClass).invoke(null, flags());
//    }

    @SneakyThrows
    public void flags(byte flags){
        if(compatibility) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETFLAGS.getField(), byte.class).invoke(entity, flags); else ((Display.TextDisplay)entity).setFlags(flags);
        metadata();
    }
    @SneakyThrows
    public byte flags(){
        return compatibility ? (byte) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETFLAGS.getField()).invoke(entity) : ((Display.TextDisplay)entity).getFlags();
    }
    @SneakyThrows
    public int lineWidth(){
        return compatibility ? (int) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_GETLINEWIDTH.getField()).invoke(entity) : ((Display.TextDisplay)entity).getLineWidth();
    }
    @SneakyThrows
    public void lineWidth(int width){
        if(compatibility) entity.getClass().getMethod(FieldMappings.TEXTDISPLAY_SETLINEWIDTH.getField(), int.class).invoke(entity, width);
//        else entity.getEntityData().set((EntityDataAccessor<? super Integer>) ReflectionUtil.getStaticField(Display.TextDisplay.class, FieldMappings.TEXTDISPLAY_LINEWIDTH_ID.getField()), width);
//        ReflectionUtil.runMethodWithSingleArgument(Display.TextDisplay.class, entity, FieldMappings.TEXTDISPLAY_SETLINEWIDTH.getField(), int.class, width);
        else entity.getEntityData().set(Display.TextDisplay.DATA_LINE_WIDTH_ID, width);
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
