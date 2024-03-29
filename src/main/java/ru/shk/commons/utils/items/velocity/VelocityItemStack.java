package ru.shk.commons.utils.items.velocity;

import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.data.ItemType;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import ru.shk.commons.utils.CustomHead;
import ru.shk.commons.utils.items.protocolize.ProtocolizeItemStack;
import ru.shk.velocity.commons.Commons;

@NoArgsConstructor
public class VelocityItemStack extends ProtocolizeItemStack<VelocityItemStack> {

    public VelocityItemStack(@NonNull ItemStack stack) {
        super(stack);
    }

    public VelocityItemStack(@NonNull ItemType item) {
        super(item);
    }

    public VelocityItemStack(@NonNull String type) {
        super(type);
    }

    @Override
    public VelocityItemStack customHead(int id) {
        String texture = Commons.getInstance().getCustomHeadTexture(id);
        if(texture==null) return this;
        base64head(texture);
        customHeadId(id);
        return this;
    }

    @Override
    public VelocityItemStack customHead(String key) {
        CustomHead head = Commons.getInstance().findCustomHead(key);
        if(head==null) return this;
        base64head(head.getTexture());
        customHeadId(head.getId());
        return this;
    }

    @Override
    public Object stringToComponent(String s) {
        return Commons.colorize(s);
    }

}
