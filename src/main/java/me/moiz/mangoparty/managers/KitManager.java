package me.moiz.mangoparty.managers;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.models.KitRules;
import me.moiz.mangoparty.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * Manages the creation, loading, saving, and application of kits.
 * Kits contain inventory contents, armor, and special rules for matches.
 * Optimized for performance with concurrent collections, caching, and asynchronous operations.
 */
public class KitManager {
    private final MangoParty plugin;
    private final Map<String, Kit> kits;
    private final File kitsDir;
    private final ReentrantReadWriteLock kitsLock;
    private BukkitTask autoSaveTask;
    private boolean kitsDirty = false;
    
    // Cache for frequently accessed kits
    private final Map<String, Kit> kitCache;
    
    /**
     * Constructs a new KitManager.
     *
     * @param plugin The main plugin instance
     */
    public KitManager(MangoParty plugin) {
        this.plugin = plugin;
        this.kits = new ConcurrentHashMap<>();
        this.kitsDir = new File(plugin.getDataFolder(), "kits");
        this.kitsLock = new ReentrantReadWriteLock();
        this.kitCache = new ConcurrentHashMap<>();
        
        // Create kits directory if it doesn't exist
        if (!kitsDir.exists()) {
            if (kitsDir.mkdirs()) {
                plugin.getLogger().info("Created kits directory");
            } else {
                plugin.getLogger().warning("Failed to create kits directory");
            }
        }
        
        loadKits();
        scheduleAutoSave();
    }
    
    /**
     * Loads all kits from the kits directory.
     * Optimized with better error handling and logging.
     */
    private void loadKits() {
        plugin.getLogger().info("Loading kits from " + kitsDir.getAbsolutePath());
        File[] kitFiles = kitsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (kitFiles == null || kitFiles.length == 0) {
            plugin.getLogger().info("No kit files found");
            return;
        }
        
        int loadedCount = 0;
        int failedCount = 0;
        
        kitsLock.writeLock().lock();
        try {
            for (File kitFile : kitFiles) {
                String kitName = kitFile.getName().replace(".yml", "");
                Kit kit = loadKitFromFile(kitName, kitFile);
                
                if (kit != null) {
                    kits.put(kitName.toLowerCase(), kit); // Store with lowercase key for case-insensitive lookup
                    loadedCount++;
                } else {
                    failedCount++;
                }
            }
        } finally {
            kitsLock.writeLock().unlock();
        }
        
        plugin.getLogger().info("Loaded " + loadedCount + " kits successfully. Failed to load " + failedCount + " kits.");
    }
    
    /**
     * Schedules an auto-save task to periodically save modified kits.
     */
    private void scheduleAutoSave() {
        autoSaveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (kitsDirty) {
                saveAllKits();
                kitsDirty = false;
            }
        }, 20 * 60 * 5, 20 * 60 * 5); // Run every 5 minutes
    }
    
    /**
     * Saves all kits to their respective files.
     */
    private void saveAllKits() {
        kitsLock.readLock().lock();
        try {
            plugin.getLogger().info("Auto-saving all kits...");
            int savedCount = 0;
            
            for (Kit kit : kits.values()) {
                if (saveKit(kit)) {
                    savedCount++;
                }
            }
            
            plugin.getLogger().info("Auto-saved " + savedCount + " kits");
        } finally {
            kitsLock.readLock().unlock();
        }
    }
    
    /**
     * Loads a kit from a file.
     *
     * @param name The name of the kit
     * @param file The file to load the kit from
     * @return The loaded kit, or null if loading failed
     */
    private Kit loadKitFromFile(String name, File file) {
        if (file == null || !file.exists()) {
            plugin.getLogger().warning("Kit file does not exist: " + (file != null ? file.getPath() : "null"));
            return null;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            Kit kit = new Kit(name);
            
            // Load display name (default to kit name if not specified)
            kit.setDisplayName(config.getString("displayName", name));
            
            // Load inventory contents
            if (config.contains("contents")) {
                ItemStack[] contents = new ItemStack[36]; // Player inventory size
                for (int i = 0; i < contents.length; i++) {
                    if (config.contains("contents." + i)) {
                        contents[i] = config.getItemStack("contents." + i);
                    }
                }
                kit.setContents(contents);
            }
            
            // Load armor contents
            if (config.contains("armor")) {
                ItemStack[] armor = new ItemStack[4]; // Armor size (helmet, chestplate, leggings, boots)
                for (int i = 0; i < armor.length; i++) {
                    if (config.contains("armor." + i)) {
                        armor[i] = config.getItemStack("armor." + i);
                    }
                }
                kit.setArmor(armor);
            }
            
            // Load offhand item
            if (config.contains("offhand")) {
                kit.setOffhand(config.getItemStack("offhand"));
            }
            
            // Load kit icon
            if (config.contains("icon")) {
                kit.setIcon(config.getItemStack("icon"));
            } else {
                // Default icon if none specified
                kit.setIcon(new ItemStack(Material.IRON_SWORD));
            }
            
            // Load kit rules
            loadKitRules(kit, config);
            
            plugin.getLogger().info("Successfully loaded kit: " + name);
            return kit;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load kit: " + name, e);
            return null;
        }
    }
    
    /**
     * Loads kit rules from configuration.
     *
     * @param kit The kit to set rules for
     * @param config The configuration to load rules from
     */
    private void loadKitRules(Kit kit, YamlConfiguration config) {
        if (!config.contains("rules")) {
            // Use default rules if none specified
            return;
        }
        
        ConfigurationSection rulesSection = config.getConfigurationSection("rules");
        if (rulesSection == null) {
            plugin.getLogger().warning("Invalid rules section for kit: " + kit.getName());
            return;
        }
        
        KitRules rules = new KitRules();
        
        rules.setNaturalHealthRegen(rulesSection.getBoolean("natural_health_regen", true));
        rules.setBlockBreak(rulesSection.getBoolean("block_break", false));
        rules.setBlockPlace(rulesSection.getBoolean("block_place", false));
        rules.setDamageMultiplier(rulesSection.getDouble("damage_multiplier", 1.0));
        rules.setInstantTnt(rulesSection.getBoolean("instant_tnt", false));
        
        kit.setRules(rules);
    }
    
    /**
     * Creates a new kit from a player's current inventory.
     * Optimized with thread safety and better error handling.
     *
     * @param name The name of the kit
     * @param player The player whose inventory will be used for the kit
     * @return true if the kit was created successfully, false otherwise
     */
    public boolean createKit(String name, Player player) {
        if (name == null || name.isEmpty()) {
            plugin.getLogger().warning("Cannot create kit with null or empty name");
            return false;
        }
        
        if (player == null) {
            plugin.getLogger().warning("Cannot create kit from null player");
            return false;
        }
        
        String lowerName = name.toLowerCase();
        
        // Check if kit already exists
        kitsLock.readLock().lock();
        try {
            if (kits.containsKey(lowerName)) {
                plugin.getLogger().warning("Kit with name '" + name + "' already exists");
                player.sendMessage(HexUtils.colorize("&cA kit with that name already exists!"));
                return false;
            }
        } finally {
            kitsLock.readLock().unlock();
        }
        
        // Create new kit
        Kit kit = new Kit(name);
        kit.setDisplayName(name);
        
        try {
            kit.setContents(player.getInventory().getContents().clone()); // Clone to avoid reference issues
            kit.setArmor(player.getInventory().getArmorContents().clone());
            kit.setOffhand(player.getInventory().getItemInOffHand().clone());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error cloning inventory for kit: " + name, e);
            player.sendMessage(HexUtils.colorize("&cError creating kit. Please try again."));
            return false;
        }
        
        // Find a suitable icon from the player's inventory
        ItemStack icon = findSuitableIcon(player);
        kit.setIcon(icon);
        
        // Save kit to memory and file
        kitsLock.writeLock().lock();
        try {
            kits.put(lowerName, kit);
            kitCache.put(lowerName, kit); // Add to cache
            kitsDirty = true; // Mark for auto-save
        } finally {
            kitsLock.writeLock().unlock();
        }
        
        // Save immediately and add to GUIs
        if (saveKit(kit)) {
            // Add kit to all GUIs asynchronously to avoid lag
            Bukkit.getScheduler().runTask(plugin, () -> {
                addKitToAllGuis(kit);
                
                // Notify player and log
                plugin.getLogger().info("Created new kit: " + name + " by " + player.getName());
                player.sendMessage(HexUtils.colorize("&aKit '" + name + "' created and added to all GUIs!"));
            });
            return true;
        } else {
            player.sendMessage(HexUtils.colorize("&cFailed to save kit. Check console for details."));
            return false;
        }
    }
    
    /**
     * Finds a suitable icon for a kit from a player's inventory.
     * Prefers weapons, then tools, then any non-null item.
     *
     * @param player The player whose inventory to search
     * @return A suitable ItemStack to use as an icon
     */
    private ItemStack findSuitableIcon(Player player) {
        ItemStack icon = null;
        
        // First try to find a weapon or tool as they make good icons
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String type = item.getType().name();
                if (type.contains("SWORD") || type.contains("AXE") || type.contains("BOW")) {
                    icon = item.clone();
                    icon.setAmount(1);
                    break;
                }
            }
        }
        
        // If no weapon found, use the first non-null item
        if (icon == null) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null) {
                    icon = item.clone();
                    icon.setAmount(1);
                    break;
                }
            }
        }
        
        // If inventory is empty, default to iron sword
        if (icon == null) {
            icon = new ItemStack(Material.IRON_SWORD);
        }
        
        return icon;
    }
    
    /**
     * Adds a kit to all GUI configurations.
     *
     * @param kit The kit to add
     */
    private void addKitToAllGuis(Kit kit) {
        // Add to match type GUIs
        plugin.getConfigManager().addKitToGuiConfig(kit, "split", null);
        plugin.getConfigManager().addKitToGuiConfig(kit, "ffa", null);
        
        // Add to queue GUIs
        plugin.getConfigManager().addKitToQueueGuiConfig(kit, "1v1", null);
        plugin.getConfigManager().addKitToQueueGuiConfig(kit, "2v2", null);
        plugin.getConfigManager().addKitToQueueGuiConfig(kit, "3v3", null);
        
        // Reload GUI configs to reflect changes
        plugin.getGuiManager().reloadGuiConfigs();
    }
    
    /**
     * Saves a kit to its configuration file.
     *
     * @param kit The kit to save
     * @return true if the kit was saved successfully, false otherwise
     */
    public boolean saveKit(Kit kit) {
        if (kit == null) {
            plugin.getLogger().warning("Cannot save null kit");
            return false;
        }
        
        File kitFile = new File(kitsDir, kit.getName() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        
        // Save basic kit information
        config.set("displayName", kit.getDisplayName());
        
        // Save inventory contents
        if (kit.getContents() != null) {
            for (int i = 0; i < kit.getContents().length; i++) {
                if (kit.getContents()[i] != null) {
                    config.set("contents." + i, kit.getContents()[i]);
                }
            }
        }
        
        // Save armor contents
        if (kit.getArmor() != null) {
            for (int i = 0; i < kit.getArmor().length; i++) {
                if (kit.getArmor()[i] != null) {
                    config.set("armor." + i, kit.getArmor()[i]);
                }
            }
        }
        
        // Save offhand item
        if (kit.getOffhand() != null) {
            config.set("offhand", kit.getOffhand());
        }
        
        // Save icon
        if (kit.getIcon() != null) {
            config.set("icon", kit.getIcon());
        }
        
        // Save kit rules
        saveKitRules(kit, config);
        
        try {
            config.save(kitFile);
            plugin.getLogger().info("Saved kit: " + kit.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kit: " + kit.getName(), e);
            return false;
        }
    }
    
    /**
     * Saves kit rules to a configuration.
     *
     * @param kit The kit containing the rules
     * @param config The configuration to save to
     */
    private void saveKitRules(Kit kit, YamlConfiguration config) {
        KitRules rules = kit.getRules();
        if (rules == null) {
            return;
        }
        
        config.set("rules.natural_health_regen", rules.isNaturalHealthRegen());
        config.set("rules.block_break", rules.isBlockBreak());
        config.set("rules.block_place", rules.isBlockPlace());
        config.set("rules.damage_multiplier", rules.getDamageMultiplier());
        config.set("rules.instant_tnt", rules.isInstantTnt());
    }
    
    /**
     * Gets a kit by name (case-insensitive).
     * Optimized with caching for frequently accessed kits.
     *
     * @param name The name of the kit
     * @return The kit, or null if not found
     */
    public Kit getKit(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        String lowerName = name.toLowerCase();
        
        // Check cache first
        Kit cachedKit = kitCache.get(lowerName);
        if (cachedKit != null) {
            return cachedKit;
        }
        
        // If not in cache, check main collection
        kitsLock.readLock().lock();
        try {
            Kit kit = kits.get(lowerName);
            if (kit != null) {
                // Add to cache for future lookups
                kitCache.put(lowerName, kit);
            }
            return kit;
        } finally {
            kitsLock.readLock().unlock();
        }
    }
    
    /**
     * Gets all loaded kits.
     * Thread-safe with read lock.
     *
     * @return A copy of the kits map
     */
    public Map<String, Kit> getKits() {
        kitsLock.readLock().lock();
        try {
            return new HashMap<>(kits);
        } finally {
            kitsLock.readLock().unlock();
        }
    }
    
    /**
     * Gives a kit to a player.
     *
     * @param player The player to give the kit to
     * @param kit The kit to give
     */
    public void giveKit(Player player, Kit kit) {
        if (player == null || kit == null) {
            plugin.getLogger().warning("Cannot give kit: player or kit is null");
            return;
        }
        
        // Clear inventory before applying kit
        player.getInventory().clear();
        
        // Apply inventory contents
        if (kit.getContents() != null) {
            player.getInventory().setContents(kit.getContents());
        }
        
        // Apply armor
        if (kit.getArmor() != null) {
            player.getInventory().setArmorContents(kit.getArmor());
        }
        
        // Apply offhand item
        if (kit.getOffhand() != null) {
            player.getInventory().setItemInOffHand(kit.getOffhand());
        }
        
        // Update inventory to reflect changes
        player.updateInventory();
        plugin.getLogger().info("Gave kit '" + kit.getName() + "' to player " + player.getName());
    }

    /**
     * Deletes a kit by name.
     * Optimized with thread safety and better error handling.
     *
     * @param name The name of the kit to delete
     * @return true if the kit was deleted, false if it wasn't found or couldn't be deleted
     */
    public boolean deleteKit(String name) {
        if (name == null || name.isEmpty()) {
            plugin.getLogger().warning("Cannot delete kit with null or empty name");
            return false;
        }
        
        String lowerName = name.toLowerCase();
        Kit kit;
        
        // Check if kit exists and remove from memory
        kitsLock.writeLock().lock();
        try {
            if (!kits.containsKey(lowerName)) {
                plugin.getLogger().warning("Kit not found for deletion: " + name);
                return false;
            }
            
            // Remove from memory and cache
            kit = kits.remove(lowerName);
            kitCache.remove(lowerName);
            kitsDirty = true; // Mark for auto-save
        } finally {
            kitsLock.writeLock().unlock();
        }
        
        // Remove from file system asynchronously
        final Kit finalKit = kit;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File kitFile = new File(kitsDir, name + ".yml");
            boolean fileDeleted = true;
            
            if (kitFile.exists()) {
                fileDeleted = kitFile.delete();
                if (fileDeleted) {
                    plugin.getLogger().info("Deleted kit file: " + name + ".yml");
                } else {
                    plugin.getLogger().severe("Failed to delete kit file: " + name + ".yml");
                }
            } else {
                plugin.getLogger().warning("Kit file not found for deletion: " + name + ".yml");
            }
            
            // Remove from GUIs on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    plugin.getConfigManager().removeKitFromGuiConfig(finalKit, "split");
                    plugin.getConfigManager().removeKitFromGuiConfig(finalKit, "ffa");
                    plugin.getConfigManager().removeKitFromQueueGuiConfig(finalKit, "1v1");
                    plugin.getConfigManager().removeKitFromQueueGuiConfig(finalKit, "2v2");
                    plugin.getConfigManager().removeKitFromQueueGuiConfig(finalKit, "3v3");
                    
                    // Reload GUI configs to reflect changes
                    plugin.getGuiManager().reloadGuiConfigs();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error removing kit from GUIs: " + name, e);
                }
            });
        });
        
        plugin.getLogger().info("Kit removed from memory: " + name);
        return true;
    }
    
    /**
     * Cleans up resources when the plugin is disabled.
     */
    public void cleanup() {
        // Cancel auto-save task
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        
        // Save any pending changes
        if (kitsDirty) {
            saveAllKits();
        }
        
        // Clear caches
        kitCache.clear();
    }
}
