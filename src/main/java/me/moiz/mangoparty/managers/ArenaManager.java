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
    private Set<String> reservedArenas; // Track which arenas are in use
    private File arenasFile;
    private YamlConfiguration arenasConfig;
    private double defaultXOffset = 200.0; // Default X-axis offset for arena instances
    private double defaultZOffset = 0.0; // Default Z-axis offset for arena instances
    
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
            
            // Load allowed kits
            if (section.contains("allowed_kits")) {
                List<String> allowedKits = section.getStringList("allowed_kits");
                arena.setAllowedKits(allowedKits);
            }
            
            // Load instance information
            if (section.contains("is_instance")) {
                arena.setInstance(section.getBoolean("is_instance"));
            }
            if (section.contains("original_arena")) {
                arena.setOriginalArena(section.getString("original_arena"));
            }
            if (section.contains("instance_number")) {
                arena.setInstanceNumber(section.getInt("instance_number"));
            }
            if (section.contains("x_offset")) {
                arena.setXOffset(section.getDouble("x_offset"));
            }
            if (section.contains("z_offset")) {
                arena.setZOffset(section.getDouble("z_offset"));
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
            if (arena.isComplete() && !reservedArenas.contains(arena.getName()) && 
                arena.isKitAllowed(kitName)) {
                return arena;
            }
        }
        return null; // No available arenas for this kit
    }
    
    public Arena createArenaInstance(Arena originalArena, String kitName) {
        if (originalArena == null || !originalArena.isComplete()) {
            return null;
        }
        
        // Find the next available instance number
        int instanceNumber = 1;
        String baseName = originalArena.getName();
        while (arenas.containsKey(baseName + "_instance" + instanceNumber)) {
            instanceNumber++;
        }
        
        String instanceName = baseName + "_instance" + instanceNumber;
        Arena instance = new Arena(instanceName, originalArena.getWorld());
        
        // Copy settings from original arena
        instance.setInstance(true);
        instance.setOriginalArena(originalArena.getName());
        instance.setInstanceNumber(instanceNumber);
        instance.setXOffset(originalArena.getXOffset() > 0 ? originalArena.getXOffset() : defaultXOffset);
        instance.setZOffset(originalArena.getZOffset() > 0 ? originalArena.getZOffset() : defaultZOffset);
        
        // Calculate new center position based on offset
        double newX = originalArena.getCenter().getX() + (instanceNumber * instance.getXOffset());
        double newZ = originalArena.getCenter().getZ() + (instanceNumber * instance.getZOffset());
        Location newCenter = new Location(
            originalArena.getCenter().getWorld(),
            newX,
            originalArena.getCenter().getY(),
            newZ
        );
        instance.setCenter(newCenter);
        
        // Calculate new spawn and corner positions based on offsets from center
        if (originalArena.getSpawn1() != null && originalArena.getCenter() != null) {
            Location spawn1Offset = originalArena.getSpawn1Offset();
            Location newSpawn1 = new Location(
                newCenter.getWorld(),
                newCenter.getX() + spawn1Offset.getX(),
                newCenter.getY() + spawn1Offset.getY(),
                newCenter.getZ() + spawn1Offset.getZ(),
                originalArena.getSpawn1().getYaw(),
                originalArena.getSpawn1().getPitch()
            );
            instance.setSpawn1(newSpawn1);
        }
        
        if (originalArena.getSpawn2() != null && originalArena.getCenter() != null) {
            Location spawn2Offset = originalArena.getSpawn2Offset();
            Location newSpawn2 = new Location(
                newCenter.getWorld(),
                newCenter.getX() + spawn2Offset.getX(),
                newCenter.getY() + spawn2Offset.getY(),
                newCenter.getZ() + spawn2Offset.getZ(),
                originalArena.getSpawn2().getYaw(),
                originalArena.getSpawn2().getPitch()
            );
            instance.setSpawn2(newSpawn2);
        }
        
        if (originalArena.getCorner1() != null && originalArena.getCenter() != null) {
            Location corner1Offset = originalArena.getCorner1Offset();
            Location newCorner1 = new Location(
                newCenter.getWorld(),
                newCenter.getX() + corner1Offset.getX(),
                newCenter.getY() + corner1Offset.getY(),
                newCenter.getZ() + corner1Offset.getZ()
            );
            instance.setCorner1(newCorner1);
        }
        
        if (originalArena.getCorner2() != null && originalArena.getCenter() != null) {
            Location corner2Offset = originalArena.getCorner2Offset();
            Location newCorner2 = new Location(
                newCenter.getWorld(),
                newCenter.getX() + corner2Offset.getX(),
                newCenter.getY() + corner2Offset.getY(),
                newCenter.getZ() + corner2Offset.getZ()
            );
            instance.setCorner2(newCorner2);
        }
        
        // Copy allowed kits
        instance.setAllowedKits(new ArrayList<>(originalArena.getAllowedKits()));
        
        // Save the instance
        arenas.put(instanceName, instance);
        saveArena(instance);
        
        // Paste the schematic at the new location
        pasteSchematicForInstance(originalArena, instance);
        
        return instance;
    }
    
    private boolean pasteSchematicForInstance(Arena originalArena, Arena instance) {
        try {
            File schematicsDir = new File(plugin.getDataFolder(), "schematics");
            File schematicFile = new File(schematicsDir, originalArena.getName() + ".schem");
            
            if (!schematicFile.exists()) {
                plugin.getLogger().warning("Schematic file not found for original arena: " + originalArena.getName());
                return false;
            }
            
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(instance.getCorner1().getWorld());
            
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
                    BlockVector3 pasteLocation = BlockVector3.at(
                        Math.min(instance.getCorner1().getBlockX(), instance.getCorner2().getBlockX()),
                        Math.min(instance.getCorner1().getBlockY(), instance.getCorner2().getBlockY()),
                        Math.min(instance.getCorner1().getBlockZ(), instance.getCorner2().getBlockZ())
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
            plugin.getLogger().severe("Failed to paste schematic for arena instance " + instance.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
        
        // Save allowed kits
        arenaSection.set("allowed_kits", arena.getAllowedKits());
        
        // Save instance information
        arenaSection.set("is_instance", arena.isInstance());
        if (arena.getOriginalArena() != null) {
            arenaSection.set("original_arena", arena.getOriginalArena());
        }
        arenaSection.set("instance_number", arena.getInstanceNumber());
        arenaSection.set("x_offset", arena.getXOffset());
        arenaSection.set("z_offset", arena.getZOffset());
        
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
