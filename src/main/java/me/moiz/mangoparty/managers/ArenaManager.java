package me.moiz.mangoparty.managers;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {
    private MangoParty plugin;
    private Map<String, Arena> arenas;
    private Set<String> reservedArenas; // Track which arenas are in use
    private File arenasFile;
    private YamlConfiguration arenasConfig;
    
    public ArenaManager(MangoParty plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.reservedArenas = ConcurrentHashMap.newKeySet();
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        loadArenas();
    }
    
    private void loadArenas() {
        if (!arenasFile.exists()) {
            plugin.saveResource("arenas.yml", false);
        }
        
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        
        ConfigurationSection arenasSection = arenasConfig.getConfigurationSection("arenas");
        if (arenasSection != null) {
            for (String arenaName : arenasSection.getKeys(false)) {
                ConfigurationSection arenaSection = arenasSection.getConfigurationSection(arenaName);
                if (arenaSection != null) {
                    Arena arena = loadArenaFromConfig(arenaName, arenaSection);
                    if (arena != null) {
                        arenas.put(arenaName, arena);
                    }
                }
            }
        }
    }
    
    private Arena loadArenaFromConfig(String name, ConfigurationSection section) {
        try {
            String world = section.getString("world");
            Arena arena = new Arena(name, world);
            
            if (section.contains("corner1")) {
                arena.setCorner1(deserializeLocation(section.getConfigurationSection("corner1")));
            }
            if (section.contains("corner2")) {
                arena.setCorner2(deserializeLocation(section.getConfigurationSection("corner2")));
            }
            if (section.contains("center")) {
                arena.setCenter(deserializeLocation(section.getConfigurationSection("center")));
            }
            if (section.contains("spawn1")) {
                arena.setSpawn1(deserializeLocation(section.getConfigurationSection("spawn1")));
            }
            if (section.contains("spawn2")) {
                arena.setSpawn2(deserializeLocation(section.getConfigurationSection("spawn2")));
            }
            if (section.contains("allowedKits")) {
                arena.setAllowedKits(section.getStringList("allowedKits"));
            }
            
            return arena;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load arena: " + name + " - " + e.getMessage());
            return null;
        }
    }
    
    private Location deserializeLocation(ConfigurationSection section) {
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }
    
    private void serializeLocation(ConfigurationSection section, Location location) {
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }
    
    public Arena createArena(String name, String world) {
        Arena arena = new Arena(name, world);
        arenas.put(name, arena);
        saveArena(arena);
        plugin.getLogger().info("Created new arena: " + name);
        return arena;
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
        return null; // No available arenas
    }
    
    public Arena getAvailableArenaForKit(String kitName) {
        for (Arena arena : arenas.values()) {
            if (arena.isComplete() && !reservedArenas.contains(arena.getName()) && arena.isKitAllowed(kitName)) {
                return arena;
            }
        }
        return null; // No available arenas for this kit
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
    
    public void saveArena(Arena arena) {
        ConfigurationSection arenaSection = arenasConfig.createSection("arenas." + arena.getName());
        arenaSection.set("world", arena.getWorld());
        
        if (arena.getCorner1() != null) {
            serializeLocation(arenaSection.createSection("corner1"), arena.getCorner1());
        }
        if (arena.getCorner2() != null) {
            serializeLocation(arenaSection.createSection("corner2"), arena.getCorner2());
        }
        if (arena.getCenter() != null) {
            serializeLocation(arenaSection.createSection("center"), arena.getCenter());
        }
        if (arena.getSpawn1() != null) {
            serializeLocation(arenaSection.createSection("spawn1"), arena.getSpawn1());
        }
        if (arena.getSpawn2() != null) {
            serializeLocation(arenaSection.createSection("spawn2"), arena.getSpawn2());
        }
        if (!arena.getAllowedKits().isEmpty()) {
            arenaSection.set("allowedKits", arena.getAllowedKits());
        }
        
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas.yml: " + e.getMessage());
        }
    }

    public void deleteArena(String name) {
        arenas.remove(name);
        reservedArenas.remove(name);

        // Remove from config
        arenasConfig.set("arenas." + name, null);

        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas.yml after deletion: " + e.getMessage());
        }

        // Delete schematic file if it exists
        File schematicsDir = new File(plugin.getDataFolder(), "schematics");
        File schematicFile = new File(schematicsDir, name + ".schem");
        if (schematicFile.exists()) {
            schematicFile.delete();
        }
        
        plugin.getLogger().info("Deleted arena: " + name);
    }
    
    public Arena cloneArena(Arena originalArena, Location newCenterLocation) {
        if (!originalArena.isComplete()) {
            return null;
        }
        
        // Generate new arena name
        String newArenaName = generateCloneName(originalArena.getName());
        
        // Check if arena already exists
        if (arenas.containsKey(newArenaName)) {
            return null;
        }
        
        try {
            // Paste the schematic at new location
            boolean pasteSuccess = pasteSchematicAtLocation(originalArena, newCenterLocation);
            if (!pasteSuccess) {
                return null;
            }
            
            // Create new arena with calculated positions
            Arena newArena = new Arena(newArenaName, newCenterLocation.getWorld().getName());
            
            // Calculate offsets from original center
            Location originalCenter = originalArena.getCenter();
            Vector spawn1Offset = originalArena.getSpawn1().toVector().subtract(originalCenter.toVector());
            Vector spawn2Offset = originalArena.getSpawn2().toVector().subtract(originalCenter.toVector());
            Vector corner1Offset = originalArena.getCorner1().toVector().subtract(originalCenter.toVector());
            Vector corner2Offset = originalArena.getCorner2().toVector().subtract(originalCenter.toVector());
            
            // Apply offsets to new center
            newArena.setCenter(newCenterLocation.clone());
            newArena.setSpawn1(newCenterLocation.clone().add(spawn1Offset));
            newArena.setSpawn2(newCenterLocation.clone().add(spawn2Offset));
            newArena.setCorner1(newCenterLocation.clone().add(corner1Offset));
            newArena.setCorner2(newCenterLocation.clone().add(corner2Offset));
            
            // Copy allowed kits
            newArena.setAllowedKits(originalArena.getAllowedKits());
            
            // Save arena
            arenas.put(newArenaName, newArena);
            saveArena(newArena);
            
            // Auto-save schematic
            saveSchematic(newArena);
            
            plugin.getLogger().info("Cloned arena: " + originalArena.getName() + " -> " + newArenaName);
            return newArena;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to clone arena " + originalArena.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private String generateCloneName(String originalName) {
        int counter = 1;
        String baseName = originalName;
        
        // Remove existing number suffix if present
        if (originalName.matches(".*_\\d+$")) {
            baseName = originalName.replaceAll("_\\d+$", "");
        }
        
        String newName;
        do {
            newName = baseName + "_" + counter;
            counter++;
        } while (arenas.containsKey(newName));
        
        return newName;
    }
    
    private boolean pasteSchematicAtLocation(Arena originalArena, Location targetLocation) {
        try {
            File schematicsDir = new File(plugin.getDataFolder(), "schematics");
            File schematicFile = new File(schematicsDir, originalArena.getName() + ".schem");
            
            if (!schematicFile.exists()) {
                plugin.getLogger().warning("Schematic file not found: " + schematicFile.getPath());
                return false;
            }
            
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(targetLocation.getWorld());
            
            ClipboardFormat format = ClipboardFormats.findByAlias("schem");
            if (format == null) {
                format = ClipboardFormats.findByAlias("schematic");
            }
            if (format == null) {
                plugin.getLogger().severe("No schematic format found for reading!");
                return false;
            }
            
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                Clipboard clipboard = reader.read();
            
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                    // Calculate paste location to align centers
                    Location originalCenter = originalArena.getCenter();
                    Vector centerOffset = originalCenter.toVector().subtract(originalArena.getCorner1().toVector());
                    
                    BlockVector3 pasteLocation = BlockVector3.at(
                        targetLocation.getBlockX() - centerOffset.getBlockX(),
                        targetLocation.getBlockY() - centerOffset.getBlockY(),
                        targetLocation.getBlockZ() - centerOffset.getBlockZ()
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
            plugin.getLogger().severe("Failed to paste schematic for arena cloning: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean saveSchematic(Arena arena) {
        if (arena.getCorner1() == null || arena.getCorner2() == null) {
            return false;
        }
        
        try {
            File schematicsDir = new File(plugin.getDataFolder(), "schematics");
            if (!schematicsDir.exists()) {
                schematicsDir.mkdirs();
            }
            
            File schematicFile = new File(schematicsDir, arena.getName() + ".schem");
            
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(arena.getCorner1().getWorld());
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
            
            CuboidRegion region = new CuboidRegion(world, min, max);
            
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                // Create clipboard and copy the region
                BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
                clipboard.setOrigin(min); // Set origin to minimum point for consistent pasting
                ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, min);
                Operations.complete(copy);
            
                // Use the built-in schematic format
                ClipboardFormat format = ClipboardFormats.findByAlias("schem");
                if (format == null) {
                    format = ClipboardFormats.findByAlias("schematic");
                }
                if (format == null) {
                    plugin.getLogger().severe("No schematic format found! Make sure WorldEdit is properly installed.");
                    return false;
                }

                try (ClipboardWriter writer = format.getWriter(new FileOutputStream(schematicFile))) {
                    writer.write(clipboard);
                }
            }
            
            plugin.getLogger().info("Saved schematic for arena: " + arena.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save schematic for arena " + arena.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean pasteSchematic(Arena arena) {
        try {
            File schematicsDir = new File(plugin.getDataFolder(), "schematics");
            File schematicFile = new File(schematicsDir, arena.getName() + ".schem");
            
            if (!schematicFile.exists()) {
                plugin.getLogger().warning("Schematic file not found: " + schematicFile.getPath());
                return false;
            }
            
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(arena.getCorner1().getWorld());
            
            ClipboardFormat format = ClipboardFormats.findByAlias("schem");
            if (format == null) {
                format = ClipboardFormats.findByAlias("schematic");
            }
            if (format == null) {
                plugin.getLogger().severe("No schematic format found for reading!");
                return false;
            }
            
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                Clipboard clipboard = reader.read();
            
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                    // Calculate the minimum point where the schematic should be pasted
                    // This should match exactly where it was saved from
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
            e.printStackTrace();
            return false;
        }
    }
    
    public Map<String, Arena> getArenas() {
        return new HashMap<>(arenas);
    }
}
