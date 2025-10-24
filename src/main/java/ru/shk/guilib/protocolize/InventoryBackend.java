package ru.shk.guilib.protocolize;

import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.item.BaseItemStack;
import dev.simplix.protocolize.api.item.component.DataComponent;
import dev.simplix.protocolize.api.item.component.DataComponentType;
import dev.simplix.protocolize.data.inventory.InventoryType;
import dev.simplix.protocolize.data.item.component.HideAdditionalTooltipComponentImpl;
import dev.simplix.protocolize.data.item.component.TooltipDisplayComponentImpl;
import ru.shk.commons.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class InventoryBackend extends Inventory {

    public InventoryBackend(InventoryType type) {
        super(type);
    }

    @Override
    public List<BaseItemStack> itemsIndexed(int protocolVersion) {
        List<BaseItemStack> newList = new ArrayList<>();
        List<BaseItemStack> list = super.itemsIndexed(protocolVersion);

        if (protocolVersion > 768) {
            for (BaseItemStack stack : list) {
                if (stack==null) {
                    newList.add(null);
                    continue;
                }
                List<DataComponent> components = new ArrayList<>(stack.getComponents());
                for (DataComponent component : components) stack = stack.removeComponent(component.getType());
//                for (DataComponent component : components) {
//                    if (component instanceof HideAdditionalTooltipComponentImpl c) {
////                        List<DataComponentType<?>> toHide = new ArrayList<>();
////                        for (DataComponent c1 : components) toHide.add(c1.getType());
//                        stack = stack.removeComponent(c.getType()); //.addComponent(new TooltipDisplayComponentImpl(true, toHide))
//                        Logger.warning("  removed "+c.getType().getName());
//                    } else if (component instanceof TooltipDisplayComponentImpl c) {
////                        List<DataComponentType<?>> toHide = new ArrayList<>();
////                        for (DataComponent c1 : components) toHide.add(c1.getType());
//                        stack = stack.removeComponent(c.getType()); //.addComponent(new TooltipDisplayComponentImpl(true, toHide))
//                        Logger.warning("  removed "+c.getType().getName());
//                    }
//                }
                newList.add(stack);
            }
        }

        return newList;
    }
}
