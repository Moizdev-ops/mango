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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
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
    private double defaultXOffset = 500.0; // Default X-axis offset for arena instances
    private double defaultZOffset = 500.0; // Default Z-axis offset for arena instances
    private ExecutorService asyncExecutor = Executors.newFixedThreadPool(3); // Thread pool for async operations
    
    public ArenaManager(MangoParty plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.reservedArenas = ConcurrentHashMap.newKeySet();
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        loadArenas();
    }
    
    private void loadArenas() {
        plugin.getLogger().info("Attempting to load arenas...");
        if (!arenasFile.exists()) {
            plugin.getLogger().info("arenas.yml not found, saving default resource.");
            plugin.saveResource("arenas.yml", false);
        }
        
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        plugin.getLogger().info("Loaded arenas.yml configuration.");
        
        ConfigurationSection arenasSection = arenasConfig.getConfigurationSection("arenas");
        if (arenasSection != null) {
            plugin.getLogger().info("Found 'arenas' section in config. Processing arenas...");
            for (String arenaName : arenasSection.getKeys(false)) {
                plugin.getLogger().info("Processing arena: " + arenaName);
                ConfigurationSection arenaSection = arenasSection.getConfigurationSection(arenaName);
                if (arenaSection != null) {
                    Arena arena = loadArenaFromConfig(arenaName, arenaSection);
                    if (arena != null) {
                        arenas.put(arenaName, arena);
                        plugin.getLogger().info("Successfully loaded and added arena: " + arenaName);
                    } else {
                        plugin.getLogger().warning("Failed to load arena: " + arenaName + ". Skipping.");
                    }
                } else {
                    plugin.getLogger().warning("Arena section for " + arenaName + " is null. Skipping.");
                }
            }
        } else {
            plugin.getLogger().info("No 'arenas' section found in arenas.yml.");
        }
        plugin.getLogger().info("Finished loading arenas. Total arenas loaded: " + arenas.size());
    }
    
    private Arena loadArenaFromConfig(String name, ConfigurationSection section) {
        plugin.getLogger().info("Loading arena from config: " + name);
        try {
            String world = section.getString("world");
            Arena arena = new Arena(name, world);
            plugin.getLogger().info("Arena " + name + " world: " + world);
            
            if (section.contains("corner1")) {
                arena.setCorner1(deserializeLocation(section.getConfigurationSection("corner1")));
                plugin.getLogger().info("Arena " + name + " corner1 loaded.");
            }
            if (section.contains("corner2")) {
                arena.setCorner2(deserializeLocation(section.getConfigurationSection("corner2")));
                plugin.getLogger().info("Arena " + name + " corner2 loaded.");
            }
            if (section.contains("center")) {
                arena.setCenter(deserializeLocation(section.getConfigurationSection("center")));
                plugin.getLogger().info("Arena " + name + " center loaded.");
            }
            if (section.contains("spawn1")) {
                arena.setSpawn1(deserializeLocation(section.getConfigurationSection("spawn1")));
                plugin.getLogger().info("Arena " + name + " spawn1 loaded.");
            }
            if (section.contains("spawn2")) {
                arena.setSpawn2(deserializeLocation(section.getConfigurationSection("spawn2")));
                plugin.getLogger().info("Arena " + name + " spawn2 loaded.");
            }
            
            // Load allowed kits
            if (section.contains("allowed_kits")) {
                List<String> allowedKits = section.getStringList("allowed_kits");
                arena.setAllowedKits(allowedKits);
                plugin.getLogger().info("Arena " + name + " allowed kits: " + String.join(", ", allowedKits));
            }
            
            // Load instance information
            if (section.contains("is_instance")) {
                arena.setInstance(section.getBoolean("is_instance"));
                plugin.getLogger().info("Arena " + name + " is_instance: " + arena.isInstance());
            }
            if (section.contains("original_arena")) {
                arena.setOriginalArena(section.getString("original_arena"));
                plugin.getLogger().info("Arena " + name + " original_arena: " + arena.getOriginalArena());
            }
            if (section.contains("instance_number")) {
                arena.setInstanceNumber(section.getInt("instance_number"));
                plugin.getLogger().info("Arena " + name + " instance_number: " + arena.getInstanceNumber());
            }
            if (section.contains("x_offset")) {
                arena.setXOffset(section.getDouble("x_offset"));
                plugin.getLogger().info("Arena " + name + " x_offset: " + arena.getXOffset());
            }
            if (section.contains("z_offset")) {
                arena.setZOffset(section.getDouble("z_offset"));
                plugin.getLogger().info("Arena " + name + " z_offset: " + arena.getZOffset());
            }
            
            plugin.getLogger().info("Successfully loaded arena: " + name + ". Is complete: " + arena.isComplete());
            return arena;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load arena: " + name + " - " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed debugging
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
        plugin.getLogger().info("Searching for an available arena...");
        for (Arena arena : arenas.values()) {
            plugin.getLogger().info("Checking arena: " + arena.getName() + ", isComplete: " + arena.isComplete() + ", isReserved: " + reservedArenas.contains(arena.getName()));
            if (arena.isComplete() && !reservedArenas.contains(arena.getName())) {
                plugin.getLogger().info("Found available arena: " + arena.getName());
                return arena;
            }
        }
        plugin.getLogger().info("No available arenas found.");
        return null; // No available arenas
    }
    
    public Arena getAvailableArenaForKit(String kitName) {
        plugin.getLogger().info("Searching for available arena for kit: " + kitName);
        // First, try to find an existing available arena that allows this kit
        for (Arena arena : arenas.values()) {
            plugin.getLogger().info("Checking arena: " + arena.getName() + ", isComplete: " + arena.isComplete() + ", isReserved: " + reservedArenas.contains(arena.getName()) + ", isKitAllowed: " + arena.isKitAllowed(kitName) + ", isInstance: " + arena.isInstance());
            if (arena.isComplete() && !reservedArenas.contains(arena.getName()) &&
                arena.isKitAllowed(kitName) && !arena.isInstance()) { // Prefer non-instance arenas first
                plugin.getLogger().info("Found available non-instance arena for kit " + kitName + ": " + arena.getName());
                return arena;
            }
        }

        plugin.getLogger().info("No non-instance arena found for kit " + kitName + ". Checking for available instances...");
        // If no non-instance arena is available, check for available instances
        for (Arena arena : arenas.values()) {
            if (arena.isComplete() && !reservedArenas.contains(arena.getName()) &&
                arena.isKitAllowed(kitName) && arena.isInstance()) {
                plugin.getLogger().info("Found available instance arena for kit " + kitName + ": " + arena.getName());
                return arena;
            }
        }

        plugin.getLogger().info("No available arena (instance or non-instance) found for kit " + kitName + ". Attempting to create a new instance.");
        // If no available arena (instance or non-instance) is found, create a new instance
        Arena baseArena = null;
        for (Arena a : arenas.values()) {
            // Find a complete, non-instance base arena that allows this kit
            if (a.isComplete() && a.isKitAllowed(kitName) && !a.isInstance()) {
                baseArena = a;
                plugin.getLogger().info("Found base arena for instance creation: " + a.getName());
                break;
            }
        }

        if (baseArena != null) {
            plugin.getLogger().info("Creating new arena instance from " + baseArena.getName() + " for kit: " + kitName);
            return createArenaInstance(baseArena, kitName);
        }

        plugin.getLogger().warning("No base arena found to create an instance for kit: " + kitName + ". Cannot provide an arena.");
        return null; // No available arenas and no base arena to create an instance
    }

    private Location offsetLocation(Location original, double xOffset, double zOffset) {
        if (original == null) return null;
        return new Location(original.getWorld(), original.getX() + xOffset, original.getY(), original.getZ() + zOffset, original.getYaw(), original.getPitch());
    }

    private void pasteArenaSchematic(Arena baseArena, Arena instanceArena) throws IOException {
        plugin.getLogger().info("Pasting schematic for instance " + instanceArena.getName() + " from base " + baseArena.getName());
        File schematicFile = new File(plugin.getDataFolder(), "schematics/" + baseArena.getName() + ".schem");
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Schematic file not found for base arena: " + baseArena.getName() + ". Path: " + schematicFile.getAbsolutePath());
            return;
        }

        Clipboard clipboard = null;
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            clipboard = reader.read();
        }

        if (clipboard == null) {
            plugin.getLogger().warning("Failed to read schematic from file: " + schematicFile.getName());
            return;
        }

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(Bukkit.getWorld(instanceArena.getWorld())))) {
            BlockVector3 pasteLocation = BlockVector3.at(instanceArena.getCorner1().getX(), instanceArena.getCorner1().getY(), instanceArena.getCorner1().getZ());
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(pasteLocation)
                    .ignoreAirBlocks(false) // Copy air blocks too
                    .build();
            Operations.complete(operation);
            plugin.getLogger().info("Schematic pasted successfully for " + instanceArena.getName() + " at " + pasteLocation.toString());
        } catch (Exception e) {
            plugin.getLogger().severe("Error pasting schematic for arena instance " + instanceArena.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public Arena createArenaInstance(Arena originalArena, String kitName) {
        plugin.getLogger().info("Attempting to create arena instance for original arena: " + originalArena.getName() + " for kit: " + kitName);
        if (originalArena == null || !originalArena.isComplete()) {
            plugin.getLogger().warning("Original arena is null or incomplete. Cannot create instance.");
            return null;
        }
        
        // Find the next available instance number
        int instanceNumber = 1;
        String baseName = originalArena.getName();
        while (arenas.containsKey(baseName + "_instance" + instanceNumber)) {
            instanceNumber++;
        }
        plugin.getLogger().info("Next instance number for " + baseName + ": " + instanceNumber);
        
        String instanceName = baseName + "_instance" + instanceNumber;
        Arena instance = new Arena(instanceName, originalArena.getWorld());
        plugin.getLogger().info("Created new Arena object for instance: " + instanceName);
        
        // Copy settings from original arena
        instance.setInstance(true);
        instance.setOriginalArena(originalArena.getName());
        instance.setInstanceNumber(instanceNumber);
        instance.setXOffset(originalArena.getXOffset() > 0 ? originalArena.getXOffset() : defaultXOffset);
        instance.setZOffset(originalArena.getZOffset() > 0 ? originalArena.getZOffset() : defaultZOffset);
        plugin.getLogger().info("Instance settings copied. XOffset: " + instance.getXOffset() + ", ZOffset: " + instance.getZOffset());
        
        // Calculate new center position based on offset
        double newX = originalArena.getCenter().getX() + (instanceNumber * 500.0);
        double newZ = originalArena.getCenter().getZ();
        Location newCenter = new Location(
            originalArena.getCenter().getWorld(),
            newX,
            originalArena.getCenter().getY(),
            newZ
        );
        instance.setCenter(newCenter);
        plugin.getLogger().info("New center calculated: " + newCenter.toString());
        
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
            plugin.getLogger().info("New spawn1 calculated: " + newSpawn1.toString());
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
            plugin.getLogger().info("New spawn2 calculated: " + newSpawn2.toString());
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
            plugin.getLogger().info("New corner1 calculated: " + newCorner1.toString());
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
            plugin.getLogger().info("New corner2 calculated: " + newCorner2.toString());
        }
        
        // Copy allowed kits
        instance.setAllowedKits(new ArrayList<>(originalArena.getAllowedKits()));
        plugin.getLogger().info("Allowed kits copied to instance: " + String.join(", ", instance.getAllowedKits()));
        
        // Save the instance
        arenas.put(instanceName, instance);
        saveArena(instance);
        plugin.getLogger().info("Instance " + instanceName + " saved and added to arenas map.");
        
        // Paste the schematic at the new location
        boolean pasteSuccess = pasteSchematicForInstance(originalArena, instance);
        plugin.getLogger().info("Schematic paste for " + instanceName + " successful: " + pasteSuccess);
        
        return instance;
    }
    
    private boolean pasteSchematicForInstance(Arena originalArena, Arena instance) {
        plugin.getLogger().info("Attempting to paste schematic for instance: " + instance.getName() + " from original arena: " + originalArena.getName());
        try {
            String schematicName = originalArena.getName();
            
            // Try to get clipboard from cache first
            Clipboard clipboard = schematicCache.get(schematicName);
            
            // If not in cache, load it from disk
            if (clipboard == null) {
                File schematicsDir = new File(plugin.getDataFolder(), "schematics");
                File schematicFile = new File(schematicsDir, schematicName + ".schem");
                plugin.getLogger().info("Schematic file path: " + schematicFile.getAbsolutePath());
                
                if (!schematicFile.exists()) {
                    plugin.getLogger().warning("Schematic file not found for original arena: " + originalArena.getName() + ". Path: " + schematicFile.getAbsolutePath());
                    return false;
                }
                
                ClipboardFormat format = ClipboardFormats.findByAlias("schem");
                if (format == null) {
                    format = ClipboardFormats.findByAlias("schematic");
                }
                if (format == null) {
                    plugin.getLogger().severe("No schematic format found for reading!");
                    return false;
                }
                plugin.getLogger().info("Using clipboard format: " + format.getName());
                
                try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                    clipboard = reader.read();
                    plugin.getLogger().info("Clipboard read successfully. Clipboard dimensions: " + clipboard.getDimensions().toString());
                    // Cache the clipboard for future use
                    schematicCache.put(schematicName, clipboard);
                }
            }
            
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(instance.getCorner1().getWorld());
            plugin.getLogger().info("Adapted Bukkit world to WorldEdit world: " + world.getName());
            
            // Create a final copy of the clipboard for use in the lambda
            final Clipboard finalClipboard = clipboard;
            
            // Use async operation for better performance
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                    // Calculate the minimum point where the schematic should be pasted
                    BlockVector3 pasteLocation = BlockVector3.at(
                        Math.min(instance.getCorner1().getBlockX(), instance.getCorner2().getBlockX()),
                        Math.min(instance.getCorner1().getBlockY(), instance.getCorner2().getBlockY()),
                        Math.min(instance.getCorner1().getBlockZ(), instance.getCorner2().getBlockZ())
                    );
                    
                    // Set fast mode to true for better performance
                    editSession.setFastMode(true);
                    
                    Operation operation = new ClipboardHolder(finalClipboard)
                        .createPaste(editSession)
                        .to(pasteLocation)
                        .ignoreAirBlocks(true) // Ignore air blocks for better performance
                        .build();
                    
                    Operations.complete(operation);
                    plugin.getLogger().info("Schematic pasted successfully for instance " + instance.getName());
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to paste schematic for arena instance " + instance.getName() + ": " + e.getMessage());
                }
            });
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to paste schematic for arena instance " + instance.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Reserves an arena so it cannot be used for other matches.
     * 
     * @param arenaName The name of the arena to reserve
     * @return True if the arena was successfully reserved, false otherwise
     */
    public boolean reserveArena(String arenaName) {
        if (arenaName == null) return false;
        
        // Check if already reserved
        if (reservedArenas.contains(arenaName)) {
            return false;
        }
        
        reservedArenas.add(arenaName);
        plugin.getLogger().fine("Reserved arena: " + arenaName);
        return true;
    }
    
    public void releaseArena(String arenaName) {
        reservedArenas.remove(arenaName);
    }
    
    /**
     * Checks if an arena is currently reserved.
     * 
     * @param arenaName The name of the arena to check
     * @return True if the arena is reserved, false otherwise
     */
    public boolean isArenaReserved(String arenaName) {
        return arenaName != null && reservedArenas.contains(arenaName);
    }
    
    public void saveArena(Arena arena) {
        plugin.getLogger().info("Saving arena: " + arena.getName());
        ConfigurationSection arenaSection = arenasConfig.createSection("arenas." + arena.getName());
        arenaSection.set("world", arena.getWorld());
        plugin.getLogger().info("Arena " + arena.getName() + " world set to: " + arena.getWorld());
        
        if (arena.getCorner1() != null) {
            serializeLocation(arenaSection.createSection("corner1"), arena.getCorner1());
            plugin.getLogger().info("Arena " + arena.getName() + " corner1 saved.");
        }
        if (arena.getCorner2() != null) {
            serializeLocation(arenaSection.createSection("corner2"), arena.getCorner2());
            plugin.getLogger().info("Arena " + arena.getName() + " corner2 saved.");
        }
        if (arena.getCenter() != null) {
            serializeLocation(arenaSection.createSection("center"), arena.getCenter());
            plugin.getLogger().info("Arena " + arena.getName() + " center saved.");
        }
        if (arena.getSpawn1() != null) {
            serializeLocation(arenaSection.createSection("spawn1"), arena.getSpawn1());
            plugin.getLogger().info("Arena " + arena.getName() + " spawn1 saved.");
        }
        if (arena.getSpawn2() != null) {
            serializeLocation(arenaSection.createSection("spawn2"), arena.getSpawn2());
            plugin.getLogger().info("Arena " + arena.getName() + " spawn2 saved.");
        }
        
        arenaSection.set("allowed_kits", arena.getAllowedKits());
        plugin.getLogger().info("Arena " + arena.getName() + " allowed kits saved: " + String.join(", ", arena.getAllowedKits()));
        
        arenaSection.set("is_instance", arena.isInstance());
        arenaSection.set("original_arena", arena.getOriginalArena());
        arenaSection.set("instance_number", arena.getInstanceNumber());
        arenaSection.set("x_offset", arena.getXOffset());
        arenaSection.set("z_offset", arena.getZOffset());
        plugin.getLogger().info("Arena " + arena.getName() + " instance info saved: is_instance=" + arena.isInstance() + ", original_arena=" + arena.getOriginalArena() + ", instance_number=" + arena.getInstanceNumber() + ", x_offset=" + arena.getXOffset() + ", z_offset=" + arena.getZOffset());
        
        try {
            arenasConfig.save(arenasFile);
            plugin.getLogger().info("Arena " + arena.getName() + " successfully saved to file.");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save arena " + arena.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteArena(String name) {
        plugin.getLogger().info("Attempting to delete arena: " + name);
        if (arenas.containsKey(name)) {
            arenas.remove(name);
            plugin.getLogger().info("Arena " + name + " removed from in-memory map.");
        } else {
            plugin.getLogger().warning("Arena " + name + " not found in in-memory map for deletion.");
        }
        
        if (arenasConfig.contains("arenas." + name)) {
            arenasConfig.set("arenas." + name, null);
            plugin.getLogger().info("Arena " + name + " marked for deletion in config.");
        } else {
            plugin.getLogger().warning("Arena " + name + " not found in config for deletion.");
        }

        try {
            arenasConfig.save(arenasFile);
            plugin.getLogger().info("Arena " + name + " successfully deleted from file.");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not delete arena " + name + ": " + e.getMessage());
            e.printStackTrace();
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
    
    // Cache for schematics to avoid loading from disk each time
    private Map<String, Clipboard> schematicCache = new ConcurrentHashMap<>();
    
    /**
     * Pastes a schematic for an arena.
     * Optimized with caching and asynchronous loading.
     * 
     * @param arena The arena to paste the schematic for
     * @return True if the paste operation was initiated successfully, false otherwise
     */
    public boolean pasteSchematic(Arena arena) {
        if (arena == null || !arena.isComplete()) {
            plugin.getLogger().warning("Cannot paste schematic for incomplete arena.");
            return false;
        }
        
        try {
            String schematicName;
            
            // For instances, use the original arena's schematic
            if (arena.isInstance() && arena.getOriginalArena() != null) {
                schematicName = arena.getOriginalArena();
            } else {
                schematicName = arena.getName();
            }
            
            // Try to get clipboard from cache first
            Clipboard clipboard = schematicCache.get(schematicName);
            
            // If not in cache, load it from disk
            if (clipboard == null) {
                File schematicsDir = new File(plugin.getDataFolder(), "schematics");
                File schematicFile = new File(schematicsDir, schematicName + ".schem");
                
                if (!schematicFile.exists()) {
                    plugin.getLogger().warning("Schematic file not found: " + schematicFile.getPath());
                    return false;
                }
                
                clipboard = loadSchematicFromFile(schematicFile);
                if (clipboard == null) return false;
                
                // Cache for future use
                schematicCache.put(schematicName, clipboard);
            }
            
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(arena.getCorner1().getWorld());
            
            // Create a final copy of the clipboard for use in the lambda
            final Clipboard finalClipboard = clipboard;
            
            // Use our thread pool for better resource management
            asyncExecutor.submit(() -> {
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                    // Calculate the minimum point where the schematic should be pasted
                    BlockVector3 pasteLocation = BlockVector3.at(
                        Math.min(arena.getCorner1().getBlockX(), arena.getCorner2().getBlockX()),
                        Math.min(arena.getCorner1().getBlockY(), arena.getCorner2().getBlockY()),
                        Math.min(arena.getCorner1().getBlockZ(), arena.getCorner2().getBlockZ())
                    );
                    
                    // Set fast mode to true for better performance
                    editSession.setFastMode(true);
                    
                    Operation operation = new ClipboardHolder(finalClipboard)
                        .createPaste(editSession)
                        .to(pasteLocation)
                        .ignoreAirBlocks(true) // Ignore air blocks for better performance
                        .build();
                    
                    Operations.complete(operation);
                    plugin.getLogger().fine("Schematic pasted successfully for " + arena.getName());
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to paste schematic for arena " + arena.getName(), e);
                }
            });
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to paste schematic for arena " + arena.getName(), e);
            return false;
        }
    }
    
    /**
     * Loads a schematic from a file.
     * 
     * @param schematicFile The file to load the schematic from
     * @return The loaded clipboard, or null if loading failed
     */
    private Clipboard loadSchematicFromFile(File schematicFile) {
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) {
                plugin.getLogger().warning("Unknown schematic format for file: " + schematicFile.getName());
                return null;
            }
            
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                return reader.read();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading schematic from file: " + schematicFile.getName(), e);
            return null;
        }
    }
    
    public Map<String, Arena> getArenas() {
        return new HashMap<>(arenas);
    }
}
