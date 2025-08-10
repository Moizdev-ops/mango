package me.moiz.mangoparty.managers;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {
    private MangoParty plugin;
    private Map<String, Arena> arenas;
    private Set<String> reservedArenas;
    private File arenasFile;
    private YamlConfiguration arenasConfig;

    public ArenaManager(MangoParty plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.reservedArenas = ConcurrentHashMap.newKeySet();
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        loadArenas();
    }

    public void loadArenas() {
        if (!arenasFile.exists()) {
            try {
                arenasFile.createNewFile();
                arenasConfig = new YamlConfiguration();
                arenasConfig.set("arenas", "");
                arenasConfig.save(arenasFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create arenas.yml: " + e.getMessage());
                return;
            }
        }

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

        plugin.getLogger().info("Loaded " + arenas.size() + " arenas.");
    }

    private Arena loadArenaFromConfig(String name, ConfigurationSection section) {
        try {
            Arena arena = new Arena(name);

            // Load center
            if (section.contains("center")) {
                ConfigurationSection centerSection = section.getConfigurationSection("center");
                World world = Bukkit.getWorld(centerSection.getString("world"));
                if (world != null) {
                    Location center = new Location(world,
                            centerSection.getDouble("x"),
                            centerSection.getDouble("y"),
                            centerSection.getDouble("z"),
                            (float) centerSection.getDouble("yaw"),
                            (float) centerSection.getDouble("pitch"));
                    arena.setCenter(center);
                }
            }

            // Load spawn1
            if (section.contains("spawn1")) {
                ConfigurationSection spawn1Section = section.getConfigurationSection("spawn1");
                World world = Bukkit.getWorld(spawn1Section.getString("world"));
                if (world != null) {
                    Location spawn1 = new Location(world,
                            spawn1Section.getDouble("x"),
                            spawn1Section.getDouble("y"),
                            spawn1Section.getDouble("z"),
                            (float) spawn1Section.getDouble("yaw"),
                            (float) spawn1Section.getDouble("pitch"));
                    arena.setSpawn1(spawn1);
                }
            }

            // Load spawn2
            if (section.contains("spawn2")) {
                ConfigurationSection spawn2Section = section.getConfigurationSection("spawn2");
                World world = Bukkit.getWorld(spawn2Section.getString("world"));
                if (world != null) {
                    Location spawn2 = new Location(world,
                            spawn2Section.getDouble("x"),
                            spawn2Section.getDouble("y"),
                            spawn2Section.getDouble("z"),
                            (float) spawn2Section.getDouble("yaw"),
                            (float) spawn2Section.getDouble("pitch"));
                    arena.setSpawn2(spawn2);
                }
            }

            // Load corner1
            if (section.contains("corner1")) {
                ConfigurationSection corner1Section = section.getConfigurationSection("corner1");
                World world = Bukkit.getWorld(corner1Section.getString("world"));
                if (world != null) {
                    Location corner1 = new Location(world,
                            corner1Section.getDouble("x"),
                            corner1Section.getDouble("y"),
                            corner1Section.getDouble("z"));
                    arena.setCorner1(corner1);
                }
            }

            // Load corner2
            if (section.contains("corner2")) {
                ConfigurationSection corner2Section = section.getConfigurationSection("corner2");
                World world = Bukkit.getWorld(corner2Section.getString("world"));
                if (world != null) {
                    Location corner2 = new Location(world,
                            corner2Section.getDouble("x"),
                            corner2Section.getDouble("y"),
                            corner2Section.getDouble("z"));
                    arena.setCorner2(corner2);
                }
            }

            // Load settings
            arena.setRegenerateBlocks(section.getBoolean("regenerateBlocks", true));
            
            // Load allowed kits
            List<String> allowedKits = section.getStringList("allowedKits");
            arena.setAllowedKits(allowedKits);

            return arena;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load arena " + name + ": " + e.getMessage());
            return null;
        }
    }

    public Arena createArena(String name, String worldName) {
        Arena arena = new Arena(name);
        arenas.put(name, arena);
        saveArena(arena);
        plugin.getLogger().info("Created new arena: " + name);
        return arena;
    }

    public void saveArena(Arena arena) {
        if (arenasConfig == null) {
            arenasConfig = new YamlConfiguration();
        }

        ConfigurationSection arenasSection = arenasConfig.getConfigurationSection("arenas");
        if (arenasSection == null) {
            arenasSection = arenasConfig.createSection("arenas");
        }

        ConfigurationSection arenaSection = arenasSection.createSection(arena.getName());
        arena.saveToConfig(arenaSection);

        try {
            arenasConfig.save(arenasFile);
            arenas.put(arena.getName(), arena);
            plugin.getLogger().info("Saved arena: " + arena.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save arena " + arena.getName() + ": " + e.getMessage());
        }
    }

    public void deleteArena(String name) {
        arenas.remove(name);
        reservedArenas.remove(name);
        
        if (arenasConfig != null) {
            ConfigurationSection arenasSection = arenasConfig.getConfigurationSection("arenas");
            if (arenasSection != null) {
                arenasSection.set(name, null);
                try {
                    arenasConfig.save(arenasFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save arenas.yml after deleting arena: " + e.getMessage());
                }
            }
        }

        // Delete schematic file
        File schematicFile = new File(plugin.getDataFolder(), "schematics/" + name + ".schem");
        if (schematicFile.exists()) {
            schematicFile.delete();
        }
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public Arena getAvailableArena() {
        for (Arena arena : arenas.values()) {
            if (arena.isComplete() && !reservedArenas.contains(arena.getName())) {
                return arena;
            }
        }
        return null;
    }

    public void reserveArena(String arenaName) {
        reservedArenas.add(arenaName);
    }

    public void releaseArena(String arenaName) {
        reservedArenas.remove(arenaName);
    }

    public boolean isArenaReserved(String arenaName) {
        return reservedArenas.contains(arenaName);
    }

    public List<Arena> getAllArenas() {
        return new ArrayList<>(arenas.values());
    }

    public List<String> getArenaNames() {
        return new ArrayList<>(arenas.keySet());
    }

    public Map<String, Arena> getArenas() {
        return new HashMap<>(arenas);
    }

    public boolean arenaExists(String name) {
        return arenas.containsKey(name);
    }

    public boolean saveSchematic(Arena arena) {
        try {
            saveArenaSchematic(arena);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save schematic: " + e.getMessage());
            return false;
        }
    }

    public void saveArenaSchematic(Arena arena) {
        if (!arena.isComplete()) {
            plugin.getLogger().warning("Cannot save schematic for incomplete arena: " + arena.getName());
            return;
        }

        try {
            // Create schematics directory
            File schematicsDir = new File(plugin.getDataFolder(), "schematics");
            if (!schematicsDir.exists()) {
                schematicsDir.mkdirs();
            }

            // Get WorldEdit world
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(arena.getCorner1().getWorld());
            
            // Create selection region
            BlockVector3 min = BlockVector3.at(
                Math.min(arena.getCorner1().getBlockX(), arena.getCorner2().getBlockX()),
                Math.min(arena.getCorner1().getBlockY(), arena.getCorner2().getBlockY()),
                Math.min(arena.getCorner1().getBlockZ(), arena.getCorner2().getBlockZ())
            );
            
            BlockVector3 max = BlockVector3.at(
                Math.max(arena.getCorner1().getBlockX(), arena.getCorner2().getBlockX()),
                Math.max(arena.getCorner1().getBlockY(), arena.getCorner2().getBlockY()),
                Math.max(arena.getCorner1().getBlockZ(), arena.getCorner2().getBlockZ())
            );

            // Create edit session and copy region
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                com.sk89q.worldedit.regions.CuboidRegion region = 
                    new com.sk89q.worldedit.regions.CuboidRegion(world, min, max);
                
                com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard clipboard = 
                    new com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard(region);
                
                com.sk89q.worldedit.function.operation.ForwardExtentCopy copy = 
                    new com.sk89q.worldedit.function.operation.ForwardExtentCopy(
                        editSession, region, clipboard, region.getMinimumPoint());
                
                Operations.complete(copy);

                // Save to file
                File schematicFile = new File(schematicsDir, arena.getName() + ".schem");
                ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
                
                try (ClipboardWriter writer = format.getWriter(new FileOutputStream(schematicFile))) {
                    writer.write(clipboard);
                }
                
                plugin.getLogger().info("Saved schematic for arena: " + arena.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save schematic for arena " + arena.getName() + ": " + e.getMessage());
        }
    }

    public boolean pasteSchematic(Arena arena) {
        try {
            File schematicFile = new File(plugin.getDataFolder(), "schematics/" + arena.getName() + ".schem");
            if (!schematicFile.exists()) {
                plugin.getLogger().warning("Schematic file not found for arena: " + arena.getName());
                return false;
            }

            // Paste schematic using WorldEdit
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                Clipboard clipboard = reader.read();
                
                com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(arena.getCorner1().getWorld());
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                    BlockVector3 pasteLocation = BlockVector3.at(
                        Math.min(arena.getCorner1().getBlockX(), arena.getCorner2().getBlockX()),
                        Math.min(arena.getCorner1().getBlockY(), arena.getCorner2().getBlockY()),
                        Math.min(arena.getCorner1().getBlockZ(), arena.getCorner2().getBlockZ())
                    );
                    
                    Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(pasteLocation)
                        .ignoreAirBlocks(false)
                        .build();
                    
                    Operations.complete(operation);
                }
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to paste schematic for arena " + arena.getName() + ": " + e.getMessage());
            return false;
        }
    }

    public String cloneArena(Arena originalArena, Location pasteLocation) {
        if (!originalArena.isComplete()) {
            return null;
        }

        try {
            // Generate new arena name
            String newArenaName = generateCloneName(originalArena.getName());
            
            // Load and paste schematic
            File schematicFile = new File(plugin.getDataFolder(), "schematics/" + originalArena.getName() + ".schem");
            if (!schematicFile.exists()) {
                plugin.getLogger().warning("Schematic file not found for arena: " + originalArena.getName());
                return null;
            }

            // Paste schematic using FAWE
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                Clipboard clipboard = reader.read();
                
                com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(pasteLocation.getWorld());
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                    Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(pasteLocation.getBlockX(), pasteLocation.getBlockY(), pasteLocation.getBlockZ()))
                        .build();
                    
                    Operations.complete(operation);
                }
            }

            // Calculate new positions based on offsets from original center
            Location originalCenter = originalArena.getCenter();
            Vector spawn1Offset = originalArena.getSpawn1().toVector().subtract(originalCenter.toVector());
            Vector spawn2Offset = originalArena.getSpawn2().toVector().subtract(originalCenter.toVector());
            Vector corner1Offset = originalArena.getCorner1().toVector().subtract(originalCenter.toVector());
            Vector corner2Offset = originalArena.getCorner2().toVector().subtract(originalCenter.toVector());

            // Create new arena with calculated positions
            Arena newArena = new Arena(newArenaName);
            newArena.setCenter(pasteLocation.clone());
            newArena.setSpawn1(pasteLocation.clone().add(spawn1Offset));
            newArena.setSpawn2(pasteLocation.clone().add(spawn2Offset));
            newArena.setCorner1(pasteLocation.clone().add(corner1Offset));
            newArena.setCorner2(pasteLocation.clone().add(corner2Offset));
            
            // Copy settings
            newArena.setRegenerateBlocks(originalArena.isRegenerateBlocks());
            newArena.setAllowedKits(new ArrayList<>(originalArena.getAllowedKits()));

            // Save new arena
            saveArena(newArena);
            saveArenaSchematic(newArena);

            plugin.getLogger().info("Successfully cloned arena " + originalArena.getName() + " to " + newArenaName);
            return newArenaName;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to clone arena " + originalArena.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private String generateCloneName(String originalName) {
        String baseName = originalName;
        int counter = 1;
        
        // Check if original name already has a number suffix
        if (originalName.matches(".*_\\d+$")) {
            String[] parts = originalName.split("_");
            baseName = String.join("_", java.util.Arrays.copyOf(parts, parts.length - 1));
        }
        
        String newName;
        do {
            newName = baseName + "_" + counter;
            counter++;
        } while (arenaExists(newName));
        
        return newName;
    }

    public List<Arena> getArenasForKit(String kitName) {
        List<Arena> availableArenas = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.isKitAllowed(kitName)) {
                availableArenas.add(arena);
            }
        }
        return availableArenas;
    }
}
