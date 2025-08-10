package me.moiz.mangoparty.managers;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {
    private MangoParty plugin;
    private Map<String, Arena> arenas;
    private Set<String> arenasInUse;
    private File arenasFile;
    private YamlConfiguration arenasConfig;

    public ArenaManager(MangoParty plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.arenasInUse = new HashSet<>();
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        
        if (!arenasFile.exists()) {
            try {
                arenasFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create arenas.yml file!");
            }
        }
        
        loadArenas();
    }

    private void loadArenas() {
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        
        ConfigurationSection arenasSection = arenasConfig.getConfigurationSection("arenas");
        if (arenasSection != null) {
            for (String arenaName : arenasSection.getKeys(false)) {
                ConfigurationSection arenaSection = arenasSection.getConfigurationSection(arenaName);
                Arena arena = loadArenaFromConfig(arenaName, arenaSection);
                if (arena != null) {
                    arenas.put(arenaName, arena);
                }
            }
        }
        
        plugin.getLogger().info("§a⚔️ Loaded " + arenas.size() + " arenas");
    }

    private Arena loadArenaFromConfig(String name, ConfigurationSection section) {
        try {
            Arena arena = new Arena(name);
            
            // Load locations
            if (section.contains("center")) {
                arena.setCenter(loadLocationFromConfig(section.getConfigurationSection("center")));
            }
            if (section.contains("spawn1")) {
                arena.setSpawn1(loadLocationFromConfig(section.getConfigurationSection("spawn1")));
            }
            if (section.contains("spawn2")) {
                arena.setSpawn2(loadLocationFromConfig(section.getConfigurationSection("spawn2")));
            }
            if (section.contains("corner1")) {
                arena.setCorner1(loadLocationFromConfig(section.getConfigurationSection("corner1")));
            }
            if (section.contains("corner2")) {
                arena.setCorner2(loadLocationFromConfig(section.getConfigurationSection("corner2")));
            }
            
            // Load settings
            arena.setRegenerateBlocks(section.getBoolean("regenerateBlocks", true));
            
            // Load allowed kits
            List<String> allowedKits = section.getStringList("allowedKits");
            arena.setAllowedKits(allowedKits);
            
            return arena;
        } catch (Exception e) {
            plugin.getLogger().warning("§c⚠️ Failed to load arena: " + name + " - " + e.getMessage());
            return null;
        }
    }

    private Location loadLocationFromConfig(ConfigurationSection section) {
        if (section == null) return null;
        
        String worldName = section.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("§c⚠️ World not found: " + worldName);
            return null;
        }
        
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Arena createArena(String name, String worldName) {
        Arena arena = new Arena(name);
        arenas.put(name, arena);
        saveArena(arena);
        return arena;
    }

    public void saveArena(Arena arena) {
        ConfigurationSection arenaSection = arenasConfig.createSection("arenas." + arena.getName());
        arena.saveToConfig(arenaSection);
        
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("§c❌ Failed to save arena: " + arena.getName() + " - " + e.getMessage());
        }
    }

    public boolean saveSchematic(Arena arena) {
        if (!arena.isComplete()) {
            return false;
        }
        
        try {
            // Create schematics directory
            File schematicsDir = new File(plugin.getDataFolder(), "schematics");
            if (!schematicsDir.exists()) {
                schematicsDir.mkdirs();
            }
            
            // Save schematic file (placeholder implementation)
            File schematicFile = new File(schematicsDir, arena.getName() + ".schematic");
            if (!schematicFile.exists()) {
                schematicFile.createNewFile();
            }
            
            plugin.getLogger().info("§a⚔️ Saved schematic for arena: " + arena.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("§c❌ Failed to save schematic for arena: " + arena.getName() + " - " + e.getMessage());
            return false;
        }
    }

    public void saveArenaSchematic(Arena arena) {
        saveSchematic(arena);
    }

    public String cloneArena(Arena originalArena, Location newLocation) {
        if (!originalArena.isComplete()) {
            return null;
        }
        
        // Generate new arena name
        String baseName = originalArena.getName();
        String newName = baseName + "_clone_1";
        int counter = 1;
        while (arenas.containsKey(newName)) {
            counter++;
            newName = baseName + "_clone_" + counter;
        }
        
        // Create new arena
        Arena clonedArena = new Arena(newName);
        
        // Calculate offsets from original center to new location
        Location originalCenter = originalArena.getCenter();
        double offsetX = newLocation.getX() - originalCenter.getX();
        double offsetY = newLocation.getY() - originalCenter.getY();
        double offsetZ = newLocation.getZ() - originalCenter.getZ();
        
        // Apply offsets to all locations
        clonedArena.setCenter(applyOffset(originalArena.getCenter(), offsetX, offsetY, offsetZ));
        clonedArena.setSpawn1(applyOffset(originalArena.getSpawn1(), offsetX, offsetY, offsetZ));
        clonedArena.setSpawn2(applyOffset(originalArena.getSpawn2(), offsetX, offsetY, offsetZ));
        clonedArena.setCorner1(applyOffset(originalArena.getCorner1(), offsetX, offsetY, offsetZ));
        clonedArena.setCorner2(applyOffset(originalArena.getCorner2(), offsetX, offsetY, offsetZ));
        
        // Copy settings
        clonedArena.setRegenerateBlocks(originalArena.isRegenerateBlocks());
        clonedArena.setAllowedKits(new ArrayList<>(originalArena.getAllowedKits()));
        
        // Save the cloned arena
        arenas.put(newName, clonedArena);
        saveArena(clonedArena);
        
        // Auto-save schematic
        saveSchematic(clonedArena);
        
        return newName;
    }

    private Location applyOffset(Location original, double offsetX, double offsetY, double offsetZ) {
        if (original == null) return null;
        
        return new Location(
            original.getWorld(),
            original.getX() + offsetX,
            original.getY() + offsetY,
            original.getZ() + offsetZ,
            original.getYaw(),
            original.getPitch()
        );
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public Map<String, Arena> getArenas() {
        return new HashMap<>(arenas);
    }

    public List<Arena> getAllArenas() {
        return new ArrayList<>(arenas.values());
    }

    public boolean arenaExists(String name) {
        return arenas.containsKey(name);
    }

    public Arena getAvailableArena() {
        for (Arena arena : arenas.values()) {
            if (arena.isComplete() && !arenasInUse.contains(arena.getName())) {
                return arena;
            }
        }
        return null;
    }

    public void setArenaInUse(String arenaName, boolean inUse) {
        if (inUse) {
            arenasInUse.add(arenaName);
        } else {
            arenasInUse.remove(arenaName);
        }
    }

    public void deleteArena(String arenaName) {
        arenas.remove(arenaName);
        arenasInUse.remove(arenaName);
        
        // Remove from config
        arenasConfig.set("arenas." + arenaName, null);
        
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("§c❌ Failed to delete arena from config: " + arenaName);
        }
        
        // Delete schematic file
        File schematicFile = new File(plugin.getDataFolder(), "schematics/" + arenaName + ".schematic");
        if (schematicFile.exists()) {
            schematicFile.delete();
        }
        
        plugin.getLogger().info("§c⚔️ Deleted arena: " + arenaName);
    }
}
