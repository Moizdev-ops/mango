package me.moiz.mangoparty.models;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class Arena {
    private String name;
    private Location center;
    private Location spawn1;
    private Location spawn2;
    private Location corner1;
    private Location corner2;
    private boolean regenerateBlocks;
    private List<String> allowedKits;

    public Arena(String name) {
        this.name = name;
        this.regenerateBlocks = true;
        this.allowedKits = new ArrayList<>();
    }

    public Arena(String name, Location center, Location spawn1, Location spawn2, Location corner1, Location corner2) {
        this.name = name;
        this.center = center;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.regenerateBlocks = true;
        this.allowedKits = new ArrayList<>();
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getCenter() {
        return center;
    }

    public void setCenter(Location center) {
        this.center = center;
    }

    public Location getSpawn1() {
        return spawn1;
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
    }

    public Location getSpawn2() {
        return spawn2;
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
    }

    public Location getCorner1() {
        return corner1;
    }

    public void setCorner1(Location corner1) {
        this.corner1 = corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    public void setCorner2(Location corner2) {
        this.corner2 = corner2;
    }

    public boolean isRegenerateBlocks() {
        return regenerateBlocks;
    }

    public void setRegenerateBlocks(boolean regenerateBlocks) {
        this.regenerateBlocks = regenerateBlocks;
    }

    public List<String> getAllowedKits() {
        return allowedKits;
    }

    public void setAllowedKits(List<String> allowedKits) {
        this.allowedKits = allowedKits != null ? allowedKits : new ArrayList<>();
    }

    public void addAllowedKit(String kitName) {
        if (!allowedKits.contains(kitName)) {
            allowedKits.add(kitName);
        }
    }

    public void removeAllowedKit(String kitName) {
        allowedKits.remove(kitName);
    }

    public boolean isKitAllowed(String kitName) {
        // If no kits are specified, all kits are allowed
        if (allowedKits.isEmpty()) {
            return true;
        }
        return allowedKits.contains(kitName);
    }

    public boolean isComplete() {
        return center != null && spawn1 != null && spawn2 != null && corner1 != null && corner2 != null;
    }

    public void saveToConfig(ConfigurationSection section) {
        if (center != null) {
            section.set("center.world", center.getWorld().getName());
            section.set("center.x", center.getX());
            section.set("center.y", center.getY());
            section.set("center.z", center.getZ());
            section.set("center.yaw", center.getYaw());
            section.set("center.pitch", center.getPitch());
        }

        if (spawn1 != null) {
            section.set("spawn1.world", spawn1.getWorld().getName());
            section.set("spawn1.x", spawn1.getX());
            section.set("spawn1.y", spawn1.getY());
            section.set("spawn1.z", spawn1.getZ());
            section.set("spawn1.yaw", spawn1.getYaw());
            section.set("spawn1.pitch", spawn1.getPitch());
        }

        if (spawn2 != null) {
            section.set("spawn2.world", spawn2.getWorld().getName());
            section.set("spawn2.x", spawn2.getX());
            section.set("spawn2.y", spawn2.getY());
            section.set("spawn2.z", spawn2.getZ());
            section.set("spawn2.yaw", spawn2.getYaw());
            section.set("spawn2.pitch", spawn2.getPitch());
        }

        if (corner1 != null) {
            section.set("corner1.world", corner1.getWorld().getName());
            section.set("corner1.x", corner1.getX());
            section.set("corner1.y", corner1.getY());
            section.set("corner1.z", corner1.getZ());
        }

        if (corner2 != null) {
            section.set("corner2.world", corner2.getWorld().getName());
            section.set("corner2.x", corner2.getX());
            section.set("corner2.y", corner2.getY());
            section.set("corner2.z", corner2.getZ());
        }

        section.set("regenerateBlocks", regenerateBlocks);
        section.set("allowedKits", allowedKits);
    }
}
