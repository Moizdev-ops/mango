package me.moiz.mangoparty.models;

import org.bukkit.Location;

public class Arena {
    private String name;
    private String world;
    private Location corner1;
    private Location corner2;
    private Location center;
    private Location spawn1;
    private Location spawn2;
    
    public Arena(String name, String world) {
        this.name = name;
        this.world = world;
    }
    
    public String getName() {
        return name;
    }
    
    public String getWorld() {
        return world;
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
    
    public boolean isComplete() {
        return corner1 != null && corner2 != null && center != null && 
               spawn1 != null && spawn2 != null;
    }
}
