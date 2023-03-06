package ru.shk.commons.utils.items.bungee;

import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.data.ItemType;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import ru.shk.commons.utils.CustomHead;
import ru.shk.commons.utils.items.ItemStackBuilder;
import ru.shk.commons.utils.items.protocolize.ProtocolizeItemStack;
import ru.shk.commonsbungee.Commons;

@NoArgsConstructor
public class BungeeItemStack extends ProtocolizeItemStack<BungeeItemStack> {

    public BungeeItemStack(@NonNull ItemStack stack) {
        super(stack);
    }

    public BungeeItemStack(@NonNull ItemType item) {
        super(item);
    }

    public BungeeItemStack(@NonNull String type) {
        super(type);
    }

    @Override
    public BungeeItemStack customHead(int id) {
        String texture = Commons.getInstance().getCustomHeadTexture(id);
        if(texture==null) return this;
        base64head(texture);
        customHeadId(id);
        return this;
    }

    @Override
    public BungeeItemStack customHead(String key) {
        CustomHead h = Commons.getInstance().findCustomHead(key);
        if(h==null) return this;
        base64head(h.getTexture());
        customHeadId(h.getId());
        return this;
    }

    public Object stringToComponent(String s){
        return new BaseComponent[]{new TextComponent(s)};
    }

}
