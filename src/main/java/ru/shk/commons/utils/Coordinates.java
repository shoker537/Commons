package ru.shk.commons.utils;

import lombok.Getter;

import java.util.Objects;

@Getter
public class Coordinates {
    private String world;
    private int x;
    private int y;
    private int z;

    public Coordinates(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public Coordinates(String world, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }
    public Coordinates(org.bukkit.Location l) {
        this.x = l.getBlockX();
        this.y = l.getBlockY();
        this.z = l.getBlockZ();
        this.world = l.getWorld().getName();
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
    public String toString() {
        return "{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
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

    public org.bukkit.Location toLocation(org.bukkit.World world){
        return new org.bukkit.Location(world, x, y, z);
    }
}
