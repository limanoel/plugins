package dev.limanoel.justshards;


import org.bukkit.Location;

public class RegionSelect {

    private final Location pos1;
    private final Location pos2;

    public RegionSelect(Location pos1, Location pos2) {

        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public boolean contains(Location loc) {

        if (!loc.getWorld().equals(pos1.getWorld())) {
            return false;
        }

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());

        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());

        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        return loc.getX() >= minX && loc.getX() <= maxX
                && loc.getY() >= minY && loc.getY() <= maxY
                && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
}

