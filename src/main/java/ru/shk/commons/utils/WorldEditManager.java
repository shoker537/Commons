package ru.shk.commons.utils;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.factory.parser.mask.NegateMaskParser;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.io.*;

public class WorldEditManager {
    private final WorldEdit worldEdit;

    public WorldEditManager(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    public void clear(Location loc1, Location loc2) throws MaxChangedBlocksException {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ()), BlockVector3.at(loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ()));
        clear(loc1.getWorld(), region);
    }

    public void clear(World world, Region region) throws MaxChangedBlocksException {
        try (EditSession setSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            ParserContext con = new ParserContext();
            con.setExtent(setSession);
            setSession.setBlocks(region, BlockTypes.AIR.getDefaultState());
        }
    }

    public void saveSchematic(Location loc1, Location loc2, File file) throws IOException {
        CuboidRegion region = createRegion(loc1, loc2);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        saveSchematic(clipboard, file);
    }

    public CuboidRegion createRegion(Location loc1, Location loc2){
        return new CuboidRegion(BlockVector3.at(loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ()), BlockVector3.at(loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ()));
    }

    public void saveSchematic(Clipboard clipboard, File file) throws IOException {
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
            writer.write(clipboard);
        }
    }

    public void pasteSchematic(File file, Location location) throws WorldEditException, IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();
            pasteSchematic(clipboard, location);
        }
    }

    public void pasteSchematicIgnoringAir(File file, Location location) throws WorldEditException, IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();
            pasteSchematic(clipboard, location, true);
        }
    }

    public void pasteSchematic(Clipboard clipboard, Location location) throws WorldEditException, IOException {
        clipboard.setOrigin(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(location.getWorld());
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        try (EditSession editSession = worldEdit.newEditSession(world)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(x, y, z))
                    .copyEntities(true)
                    .copyBiomes(true)
                    .build();
            Operations.complete(operation);
        }
    }

    public void pasteSchematic(Clipboard clipboard, Location location, boolean ignoreAir) throws WorldEditException, IOException {
        clipboard.setOrigin(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(location.getWorld());
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        try (EditSession editSession = worldEdit.newEditSession(world)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(x, y, z))
                    .copyEntities(true)
                    .copyBiomes(true)
                    .ignoreAirBlocks(ignoreAir)
                    .build();
            Operations.complete(operation);
        }
    }
    @Nullable
    public Region getPlayerSelection(Player p){
        try {
            LocalSession s = worldEdit.getSessionManager().get(BukkitAdapter.adapt(p));
            return s.getRegionSelector(s.getSelectionWorld()).getRegion();
        } catch (IncompleteRegionException ignored) {}
        return null;
    }

}
