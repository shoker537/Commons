package ru.shk.commons.utils;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

@Getter
public class Coordinates {
    private int x;
    private int y;
    private int z;

    public Coordinates(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public Coordinates(Location l) {
        this.x = l.getBlockX();
        this.y = l.getBlockY();
        this.z = l.getBlockZ();
    }

    public Coordinates add(int x, int y, int z){
        this.x+=x;
        this.y+=y;
        this.z+=z;
        return this;
    }
    public Coordinates subtract(int x, int y, int z){
        this.x-=x;
        this.y-=y;
        this.z-=z;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinates that = (Coordinates) o;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    public Location toLocation(World world){
        return new Location(world, x, y, z);
    }
}
