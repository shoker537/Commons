package ru.shk.commons.utils.nms.dfu;

import ca.spottedleaf.moonrise.paper.PaperHooks;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

public class DFU {
    private static final PaperHooks paperHooks = new PaperHooks();
    public static String updateItem(String nbtString, int versionFrom) throws CommandSyntaxException {
        DataFixer fixer = DataFixers.getDataFixer();
        CompoundTag tag = TagParser.parseCompoundFully(nbtString);
        CompoundTag updated = paperHooks.convertNBT(References.ITEM_STACK, fixer, tag, versionFrom, SharedConstants.getCurrentVersion().dataVersion().version());
        return updated.toString();
    }
    public static String updateEntity(String nbtString, int versionFrom) throws CommandSyntaxException {
        DataFixer fixer = DataFixers.getDataFixer();
        CompoundTag tag = TagParser.parseCompoundFully(nbtString);
        CompoundTag updated = paperHooks.convertNBT(References.ENTITY, fixer, tag, versionFrom, SharedConstants.getCurrentVersion().dataVersion().version());
        return updated.toString();
    }
}
