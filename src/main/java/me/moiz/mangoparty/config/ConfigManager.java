package me.moiz.mangoparty.config;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.utils.HexUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manages configuration files for the MangoParty plugin.
 * Handles creation, loading, and updating of various configuration files.
 */
public class ConfigManager {
    private final MangoParty plugin;
    private final File dataFolder;
    
    /**
     * Constructs a new ConfigManager.
     * 
     * @param plugin The MangoParty plugin instance
     */
    public ConfigManager(MangoParty plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
    }
    
    /**
     * Loads all configuration files for the plugin.
     * Creates default configurations if they don't exist.
     */
    public void loadConfigs() {
        // Create plugin data folder if it doesn't exist
        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                plugin.getLogger().info("Created plugin data folder");
            } else {
                plugin.getLogger().severe("Failed to create plugin data folder!");
                return;
            }
        }
        
        // Create default configuration files
        createDefaultConfig();
        createDefaultGuiConfigs();
        createDefaultArenaConfig();
        createDefaultScoreboardConfig();
        
        plugin.getLogger().info("All configuration files loaded successfully");
    }
    
    /**
     * Creates the default config.yml file if it doesn't exist.
     */
    private void createDefaultConfig() {
        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            plugin.getLogger().info("Created default config.yml");
        }
    }
    
    /**
     * Creates the default scoreboard.yml file if it doesn't exist.
     */
    private void createDefaultScoreboardConfig() {
        File scoreboardFile = new File(dataFolder, "scoreboard.yml");
        if (!scoreboardFile.exists()) {
            YamlConfiguration scoreboardConfig = new YamlConfiguration();
            
            // Default scoreboard settings
            scoreboardConfig.set("title", "&6&lMango&e&lParty");
            
            // Match scoreboard
            List<String> matchLines = new ArrayList<>();
            matchLines.add("&7&m----------------");
            matchLines.add("&eMatch: &f{match_type}");
            matchLines.add("&eTime: &f{time}");
            matchLines.add("&eKit: &f{kit}");
            matchLines.add("&7&m----------------");
            matchLines.add("&eTeam 1: &f{team1_alive}/{team1_total}");
            matchLines.add("&eTeam 2: &f{team2_alive}/{team2_total}");
            matchLines.add("&7&m----------------");
            scoreboardConfig.set("match", matchLines);
            
            // Duel scoreboard
            List<String> duelLines = new ArrayList<>();
            duelLines.add("&7&m----------------");
            duelLines.add("&eDuel: &f{duel_type}");
            duelLines.add("&eTime: &f{time}");
            duelLines.add("&eKit: &f{kit}");
            duelLines.add("&7&m----------------");
            duelLines.add("&e{player1}: &f{player1_health}❤");
            duelLines.add("&e{player2}: &f{player2_health}❤");
            duelLines.add("&7&m----------------");
            scoreboardConfig.set("duel", duelLines);
            
            try {
                scoreboardConfig.save(scoreboardFile);
                plugin.getLogger().info("Created default scoreboard.yml");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create scoreboard.yml", e);
            }
        }
    }
    
    /**
     * Creates default GUI configuration files if they don't exist.
     * This includes split.yml and ffa.yml for kit selection GUIs.
     */
    private void createDefaultGuiConfigs() {
        File guiDir = new File(dataFolder, "gui");
        if (!guiDir.exists()) {
            if (guiDir.mkdirs()) {
                plugin.getLogger().info("Created GUI configuration directory");
            } else {
                plugin.getLogger().severe("Failed to create GUI configuration directory!");
                return;
            }
        }
        
        // Create split.yml
        createSplitGuiConfig(guiDir);
        
        // Create ffa.yml
        createFfaGuiConfig(guiDir);
        
        // Create queue kit selection GUIs
        createQueueGuiConfig(guiDir, "1v1");
        createQueueGuiConfig(guiDir, "2v2");
        createQueueGuiConfig(guiDir, "3v3");
    }
    
    /**
     * Creates the split.yml configuration file for Party Split match kit selection.
     * 
     * @param guiDir The directory where GUI configuration files are stored
     */
    private void createSplitGuiConfig(File guiDir) {
        File splitFile = new File(guiDir, "split.yml");
        if (!splitFile.exists()) {
            YamlConfiguration splitConfig = new YamlConfiguration();
            splitConfig.set("title", "&6Select Kit - Party Split");
            splitConfig.set("size", 27);
            
            // Example kit configurations
            splitConfig.set("kits.warrior.slot", 10);
            splitConfig.set("kits.warrior.name", "&cWarrior Kit");
            splitConfig.set("kits.warrior.kit", "warrior");
            splitConfig.set("kits.warrior.lore", new String[]{"&7A balanced melee kit", "&7with sword and armor"});
            splitConfig.set("kits.warrior.customModelData", 1001);
            
            splitConfig.set("kits.archer.slot", 12);
            splitConfig.set("kits.archer.name", "&aArcher Kit");
            splitConfig.set("kits.archer.kit", "archer");
            splitConfig.set("kits.archer.lore", new String[]{"&7Ranged combat kit", "&7with bow and arrows"});
            splitConfig.set("kits.archer.customModelData", 1002);
            
            splitConfig.set("kits.mage.slot", 14);
            splitConfig.set("kits.mage.name", "&9Mage Kit");
            splitConfig.set("kits.mage.kit", "mage");
            splitConfig.set("kits.mage.lore", new String[]{"&7Magical kit with", "&7potions and enchanted items"});
            splitConfig.set("kits.mage.customModelData", 1003);
            
            try {
                splitConfig.save(splitFile);
                plugin.getLogger().info("Created default split.yml");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create split.yml", e);
            }
        }
    }
    
    /**
     * Creates the ffa.yml configuration file for Party FFA match kit selection.
     * 
     * @param guiDir The directory where GUI configuration files are stored
     */
    private void createFfaGuiConfig(File guiDir) {
        File ffaFile = new File(guiDir, "ffa.yml");
        if (!ffaFile.exists()) {
            YamlConfiguration ffaConfig = new YamlConfiguration();
            ffaConfig.set("title", "&6Select Kit - Party FFA");
            ffaConfig.set("size", 27);
            
            // Example kit configurations for FFA
            ffaConfig.set("kits.berserker.slot", 10);
            ffaConfig.set("kits.berserker.name", "&4Berserker Kit");
            ffaConfig.set("kits.berserker.kit", "berserker");
            ffaConfig.set("kits.berserker.lore", new String[]{"&7High damage melee kit", "&7for aggressive players"});
            ffaConfig.set("kits.berserker.customModelData", 2001);
            
            ffaConfig.set("kits.assassin.slot", 12);
            ffaConfig.set("kits.assassin.name", "&8Assassin Kit");
            ffaConfig.set("kits.assassin.kit", "assassin");
            ffaConfig.set("kits.assassin.lore", new String[]{"&7Stealth and speed kit", "&7for quick eliminations"});
            ffaConfig.set("kits.assassin.customModelData", 2002);
            
            ffaConfig.set("kits.tank.slot", 14);
            ffaConfig.set("kits.tank.name", "&7Tank Kit");
            ffaConfig.set("kits.tank.kit", "tank");
            ffaConfig.set("kits.tank.lore", new String[]{"&7Heavy armor kit", "&7for defensive play"});
            ffaConfig.set("kits.tank.customModelData", 2003);
            
            try {
                ffaConfig.save(ffaFile);
                plugin.getLogger().info("Created default ffa.yml");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create ffa.yml", e);
            }
        }
    }
    
    /**
     * Creates a queue GUI configuration file for the specified mode.
     * 
     * @param guiDir The directory where GUI configuration files are stored
     * @param mode The queue mode (1v1, 2v2, 3v3)
     */
    private void createQueueGuiConfig(File guiDir, String mode) {
        File queueFile = new File(guiDir, mode + "kits.yml");
        if (!queueFile.exists()) {
            YamlConfiguration queueConfig = new YamlConfiguration();
            queueConfig.set("title", "&6" + mode.toUpperCase() + " Kit Selection");
            queueConfig.set("size", 27);
            queueConfig.createSection("kits");
            
            try {
                queueConfig.save(queueFile);
                plugin.getLogger().info("Created default " + mode + "kits.yml");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create " + mode + "kits.yml", e);
            }
        }
    }
    
    /**
     * Creates the default arenas.yml file if it doesn't exist.
     * This file stores arena configurations.
     */
    private void createDefaultArenaConfig() {
        File arenasFile = new File(dataFolder, "arenas.yml");
        if (!arenasFile.exists()) {
            YamlConfiguration arenasConfig = new YamlConfiguration();
            arenasConfig.createSection("arenas"); // Empty arenas section
            
            try {
                arenasConfig.save(arenasFile);
                plugin.getLogger().info("Created default arenas.yml");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create arenas.yml", e);
            }
        }
    }

    /**
     * Adds a kit to a GUI configuration file.
     * 
     * @param kit The kit to add
     * @param matchType The match type ("split" or "ffa")
     * @param slot The inventory slot to place the kit in, or null for automatic assignment
     * @return true if the kit was successfully added, false otherwise
     */
    public boolean addKitToGuiConfig(Kit kit, String matchType, Integer slot) {
        if (kit == null) {
            plugin.getLogger().warning("Cannot add null kit to GUI config");
            return false;
        }
        
        File guiDir = new File(dataFolder, "gui");
        if (!guiDir.exists() && !guiDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create GUI directory");
            return false;
        }
        
        File guiFile;
        YamlConfiguration guiConfig;

        // Validate match type and get corresponding file
        if (matchType.equalsIgnoreCase("split")) {
            guiFile = new File(guiDir, "split.yml");
        } else if (matchType.equalsIgnoreCase("ffa")) {
            guiFile = new File(guiDir, "ffa.yml");
        } else {
            plugin.getLogger().warning("Invalid match type: " + matchType);
            return false; // Invalid match type
        }

        // Create file if it doesn't exist
        if (!guiFile.exists()) {
            // Create default config
            guiConfig = new YamlConfiguration();
            guiConfig.set("title", "&6Select Kit - " + matchType.toUpperCase());
            guiConfig.set("size", 27);
            guiConfig.createSection("kits");
            try {
                guiConfig.save(guiFile);
                plugin.getLogger().info("Created new GUI config file: " + guiFile.getName());
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create " + guiFile.getName(), e);
                return false;
            }
        }

        // Load configuration
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        if (kitsSection == null) {
            kitsSection = guiConfig.createSection("kits");
        }

        // Determine slot
        if (slot == null) {
            slot = findAvailableSlot(kitsSection, guiConfig.getInt("size", 27));
            if (slot == null) {
                plugin.getLogger().warning("No available slot found in " + matchType + " GUI for kit " + kit.getName());
                return false; // No available slot
            }
        } else if (isSlotTaken(kitsSection, slot)) {
            plugin.getLogger().warning("Slot " + slot + " is already taken in " + matchType + " GUI.");
            return false; // Slot already taken
        }

        // Add kit details to config
        String kitConfigKey = sanitizeConfigKey(kit.getName());
        kitsSection.set(kitConfigKey + ".slot", slot);
        kitsSection.set(kitConfigKey + ".name", "&c&l" + kit.getName());
        kitsSection.set(kitConfigKey + ".kit", kit.getName());
        
        // Set empty lore as requested by user
        kitsSection.set(kitConfigKey + ".lore", new ArrayList<String>());

        // Add customModelData if the kit's icon has it
        if (kit.getIcon() != null && kit.getIcon().hasItemMeta() && kit.getIcon().getItemMeta().hasCustomModelData()) {
            kitsSection.set(kitConfigKey + ".customModelData", kit.getIcon().getItemMeta().getCustomModelData());
        }

        try {
            guiConfig.save(guiFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + guiFile.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to find an available slot in a GUI.
     * 
     * @param kitsSection The configuration section containing kit slots
     * @param guiSize The size of the GUI
     * @return The first available slot, or null if none are available
     */
    private Integer findAvailableSlot(ConfigurationSection kitsSection, int guiSize) {
        Set<String> existingKitKeys = kitsSection.getKeys(false);
        List<Integer> usedSlots = new ArrayList<>();
        
        for (String key : existingKitKeys) {
            usedSlots.add(kitsSection.getInt(key + ".slot"));
        }

        for (int i = 0; i < guiSize; i++) {
            if (!usedSlots.contains(i)) {
                return i;
            }
        }
        
        return null; // No available slot
    }
    
    /**
     * Helper method to check if a slot is already taken in a GUI.
     * 
     * @param kitsSection The configuration section containing kit slots
     * @param slot The slot to check
     * @return true if the slot is taken, false otherwise
     */
    private boolean isSlotTaken(ConfigurationSection kitsSection, int slot) {
        Set<String> existingKitKeys = kitsSection.getKeys(false);
        
        for (String key : existingKitKeys) {
            if (kitsSection.getInt(key + ".slot") == slot) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Helper method to sanitize a string for use as a configuration key.
     * 
     * @param input The input string
     * @return A sanitized version of the input string
     */
    private String sanitizeConfigKey(String input) {
        if (input == null) {
            return "unknown";
        }
        return input.toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
    }
    
    /**
     * Adds a kit to a queue GUI configuration file.
     * 
     * @param kit The kit to add
     * @param mode The queue mode ("1v1", "2v2", or "3v3")
     * @param slot The inventory slot to place the kit in, or null for automatic assignment
     * @return true if the kit was successfully added, false otherwise
     */
    public boolean addKitToQueueGuiConfig(Kit kit, String mode, Integer slot) {
        if (kit == null) {
            plugin.getLogger().warning("Cannot add null kit to queue GUI config");
            return false;
        }
        
        File guiDir = new File(dataFolder, "gui");
        if (!guiDir.exists() && !guiDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create GUI directory");
            return false;
        }
        
        File guiFile = new File(guiDir, mode + "kits.yml");
        YamlConfiguration guiConfig;

        // Create file if it doesn't exist
        if (!guiFile.exists()) {
            // Create default queue config
            guiConfig = new YamlConfiguration();
            guiConfig.set("title", "&6" + mode.toUpperCase() + " Kit Selection");
            guiConfig.set("size", 27);
            guiConfig.createSection("kits");
            
            try {
                guiConfig.save(guiFile);
                plugin.getLogger().info("Created new queue GUI config file: " + guiFile.getName());
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create " + guiFile.getName(), e);
                return false;
            }
        }

        // Load configuration
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        if (kitsSection == null) {
            kitsSection = guiConfig.createSection("kits");
        }

        // Determine slot
        if (slot == null) {
            slot = findAvailableSlot(kitsSection, guiConfig.getInt("size", 27));
            if (slot == null) {
                plugin.getLogger().warning("No available slot found in " + mode + " queue GUI for kit " + kit.getName());
                return false; // No available slot
            }
        } else if (isSlotTaken(kitsSection, slot)) {
            plugin.getLogger().warning("Slot " + slot + " is already taken in " + mode + " queue GUI.");
            return false; // Slot already taken
        }

        // Add kit details to config
        String kitConfigKey = sanitizeConfigKey(kit.getName());
        kitsSection.set(kitConfigKey + ".slot", slot);
        kitsSection.set(kitConfigKey + ".material", kit.getIcon() != null ? kit.getIcon().getType().toString() : "IRON_SWORD");
        kitsSection.set(kitConfigKey + ".name", "&c&l" + kit.getName());
        
        // Set empty lore as requested by user
        kitsSection.set(kitConfigKey + ".lore", new ArrayList<String>());

        // Add customModelData if the kit's icon has it
        if (kit.getIcon() != null && kit.getIcon().hasItemMeta() && kit.getIcon().getItemMeta().hasCustomModelData()) {
            kitsSection.set(kitConfigKey + ".customModelData", kit.getIcon().getItemMeta().getCustomModelData());
        }

        try {
            guiConfig.save(guiFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + guiFile.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes a kit from a GUI configuration file.
     * 
     * @param kit The kit to remove
     * @param matchType The match type ("split" or "ffa")
     * @return true if the kit was successfully removed, false otherwise
     */
    public boolean removeKitFromGuiConfig(Kit kit, String matchType) {
        if (kit == null) {
            plugin.getLogger().warning("Cannot remove null kit from GUI config");
            return false;
        }
        
        File guiDir = new File(dataFolder, "gui");
        File guiFile;
        YamlConfiguration guiConfig;

        // Validate match type and get corresponding file
        if (matchType.equalsIgnoreCase("split")) {
            guiFile = new File(guiDir, "split.yml");
        } else if (matchType.equalsIgnoreCase("ffa")) {
            guiFile = new File(guiDir, "ffa.yml");
        } else {
            plugin.getLogger().warning("Invalid match type: " + matchType);
            return false; // Invalid match type
        }

        if (!guiFile.exists()) {
            plugin.getLogger().warning("GUI config file not found: " + guiFile.getName());
            return false;
        }

        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        // Find and remove the kit
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        if (kitsSection != null) {
            boolean kitFound = false;
            for (String key : kitsSection.getKeys(false)) {
                if (kitsSection.getString(key + ".kit", "").equalsIgnoreCase(kit.getName())) {
                    kitsSection.set(key, null);
                    kitFound = true;
                    break;
                }
            }
            
            if (kitFound) {
                try {
                    guiConfig.save(guiFile);
                    plugin.getLogger().info("Removed kit '" + kit.getName() + "' from " + matchType + " GUI");
                    return true;
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save " + guiFile.getName(), e);
                    return false;
                }
            } else {
                plugin.getLogger().warning("Kit '" + kit.getName() + "' not found in " + matchType + " GUI");
            }
        }
        
        return false; // Kit not found
    }

    /**
     * Removes a kit from a queue GUI configuration file.
     * 
     * @param kit The kit to remove
     * @param mode The queue mode ("1v1", "2v2", or "3v3")
     * @return true if the kit was successfully removed, false otherwise
     */
    public boolean removeKitFromQueueGuiConfig(Kit kit, String mode) {
        if (kit == null) {
            plugin.getLogger().warning("Cannot remove null kit from queue GUI config");
            return false;
        }
        
        File guiDir = new File(dataFolder, "gui");
        File guiFile = new File(guiDir, mode + "kits.yml");
        YamlConfiguration guiConfig;

        if (!guiFile.exists()) {
            plugin.getLogger().warning("Queue GUI config file not found: " + guiFile.getName());
            return false;
        }

        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        // Remove the kit
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        String kitKey = sanitizeConfigKey(kit.getName());
        
        if (kitsSection != null) {
            boolean kitFound = false;
            
            // First try with sanitized key
            if (kitsSection.contains(kitKey)) {
                kitsSection.set(kitKey, null);
                kitFound = true;
            } else {
                // Try to find by kit name in case the key format changed
                for (String key : kitsSection.getKeys(false)) {
                    if (key.equalsIgnoreCase(kit.getName()) || 
                        (kitsSection.contains(key + ".kit") && 
                         kitsSection.getString(key + ".kit").equalsIgnoreCase(kit.getName()))) {
                        kitsSection.set(key, null);
                        kitFound = true;
                        break;
                    }
                }
            }
            
            if (kitFound) {
                try {
                    guiConfig.save(guiFile);
                    plugin.getLogger().info("Removed kit '" + kit.getName() + "' from " + mode + " queue GUI");
                    return true;
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save " + guiFile.getName(), e);
                    return false;
                }
            } else {
                plugin.getLogger().warning("Kit '" + kit.getName() + "' not found in " + mode + " queue GUI");
            }
        }
        
        return false; // Kit not found
    }

    /**
     * Updates the icon for a kit in all GUI configuration files.
     * 
     * @param kit The kit to update
     */
    public void updateKitIconInAllGuis(Kit kit) {
        if (kit == null) {
            plugin.getLogger().warning("Cannot update icon for null kit");
            return;
        }
        
        if (kit.getIcon() == null) {
            plugin.getLogger().warning("Cannot update null icon for kit: " + kit.getName());
            return;
        }
        
        String materialName = kit.getIcon().getType().toString();
        Integer customModelData = null;
        if (kit.getIcon().hasItemMeta() && kit.getIcon().getItemMeta().hasCustomModelData()) {
            customModelData = kit.getIcon().getItemMeta().getCustomModelData();
        }
        
        plugin.getLogger().info("Updating icon for kit '" + kit.getName() + "' in all GUIs");
        
        // Update split and ffa GUIs
        updateKitIconInGuiConfig(kit, "split", materialName, customModelData);
        updateKitIconInGuiConfig(kit, "ffa", materialName, customModelData);
        
        // Update queue GUIs
        updateKitIconInQueueGuiConfig(kit, "1v1", materialName, customModelData);
        updateKitIconInQueueGuiConfig(kit, "2v2", materialName, customModelData);
        updateKitIconInQueueGuiConfig(kit, "3v3", materialName, customModelData);
    }
    
    public boolean updateKitMaterial(Kit kit, String mode, String material) {
        if (mode.equals("1v1") || mode.equals("2v2") || mode.equals("3v3")) {
            return updateKitMaterialInQueueGuiConfig(kit.getName(), mode, material);
        } else {
            return updateKitMaterialInGuiConfig(kit.getName(), mode, material);
        }
    }
    
    public boolean updateKitName(Kit kit, String mode, String displayName) {
        if (mode.equals("1v1") || mode.equals("2v2") || mode.equals("3v3")) {
            return updateKitNameInQueueGuiConfig(kit.getName(), mode, displayName);
        } else {
            return updateKitNameInGuiConfig(kit.getName(), mode, displayName);
        }
    }
    
    public boolean updateKitLore(Kit kit, String mode, String lore) {
        if (mode.equals("1v1") || mode.equals("2v2") || mode.equals("3v3")) {
            return updateKitLoreInQueueGuiConfig(kit.getName(), mode, lore);
        } else {
            return updateKitLoreInGuiConfig(kit.getName(), mode, lore);
        }
    }
    
    public boolean updateKitHideAttributes(Kit kit, String mode, boolean hideAttributes) {
        if (mode.equals("1v1") || mode.equals("2v2") || mode.equals("3v3")) {
            return updateKitHideAttributesInQueueGuiConfig(kit.getName(), mode, hideAttributes);
        } else {
            return updateKitHideAttributesInGuiConfig(kit.getName(), mode, hideAttributes);
        }
    }
    
    private boolean updateKitMaterialInGuiConfig(String kitName, String matchType, String material) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, matchType + ".yml");
        
        if (!guiFile.exists()) return false;
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null) {
            for (String key : kitsSection.getKeys(false)) {
                if (kitsSection.getString(key + ".kit", "").equalsIgnoreCase(kitName)) {
                    // Set the material for the kit in the GUI config
                    kitsSection.set(key + ".material", material);
                    
                    try {
                        guiConfig.save(guiFile);
                        return true;
                    } catch (IOException e) {
                        plugin.getLogger().severe("Failed to update material in " + guiFile.getName() + ": " + e.getMessage());
                        return false;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean updateKitMaterialInQueueGuiConfig(String kitName, String mode, String material) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, mode + "kits.yml");
        
        if (!guiFile.exists()) return false;
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null && kitsSection.contains(kitName)) {
            kitsSection.set(kitName + ".material", material);
            
            try {
                guiConfig.save(guiFile);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to update material in " + guiFile.getName() + ": " + e.getMessage());
                return false;
            }
        }
        return false;
    }
    
    /**
     * Updates the display name for a kit in a GUI configuration file.
     * 
     * @param kitName The name of the kit to update
     * @param matchType The match type ("split" or "ffa")
     * @param displayName The new display name for the kit
     * @return true if the kit was successfully updated, false otherwise
     */
    private boolean updateKitNameInGuiConfig(String kitName, String matchType, String displayName) {
        if (kitName == null || displayName == null || displayName.isEmpty()) {
            plugin.getLogger().warning("Cannot update kit name with null or empty parameters");
            return false;
        }
        
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, matchType + ".yml");
        
        if (!guiFile.exists()) return false;
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null) {
            for (String key : kitsSection.getKeys(false)) {
                if (kitsSection.getString(key + ".kit", "").equalsIgnoreCase(kitName)) {
                    kitsSection.set(key + ".name", displayName);
                    
                    try {
                        guiConfig.save(guiFile);
                        plugin.getLogger().info("Updated display name for kit '" + kitName + "' in " + matchType + " GUI");
                        return true;
                    } catch (IOException e) {
                        plugin.getLogger().severe("Failed to update name in " + guiFile.getName() + ": " + e.getMessage());
                        return false;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean updateKitNameInQueueGuiConfig(String kitName, String mode, String displayName) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, mode + "kits.yml");
        
        if (!guiFile.exists()) return false;
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null && kitsSection.contains(kitName)) {
            kitsSection.set(kitName + ".name", displayName);
            
            try {
                guiConfig.save(guiFile);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to update name in " + guiFile.getName() + ": " + e.getMessage());
                return false;
            }
        }
        return false;
    }
    
    /**
     * Updates the lore for a kit in a GUI configuration file.
     * 
     * @param kitName The name of the kit to update
     * @param matchType The match type ("split" or "ffa")
     * @param lore The new lore for the kit as a string with line breaks
     * @return true if the kit was successfully updated, false otherwise
     */
    private boolean updateKitLoreInGuiConfig(String kitName, String matchType, String lore) {
        if (kitName == null) {
            plugin.getLogger().warning("Cannot update lore for null kit");
            return false;
        }
        
        if (lore == null) {
            lore = ""; // Empty lore is acceptable
        }
        
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, matchType + ".yml");
        
        if (!guiFile.exists()) return false;
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null) {
            for (String key : kitsSection.getKeys(false)) {
                if (kitsSection.getString(key + ".kit", "").equalsIgnoreCase(kitName)) {
                    List<String> loreList = new ArrayList<>();
                    for (String line : lore.split("\\n")) {
                        loreList.add(HexUtils.colorize(line)); // Apply color formatting
                    }
                    kitsSection.set(key + ".lore", loreList);
                    
                    try {
                        guiConfig.save(guiFile);
                        plugin.getLogger().info("Updated lore for kit '" + kitName + "' in " + matchType + " GUI");
                        return true;
                    } catch (IOException e) {
                        plugin.getLogger().severe("Failed to update lore in " + guiFile.getName() + ": " + e.getMessage());
                        return false;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean updateKitLoreInQueueGuiConfig(String kitName, String mode, String lore) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, mode + "kits.yml");
        
        if (!guiFile.exists()) return false;
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null && kitsSection.contains(kitName)) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore.split("\\n")) {
                loreList.add(line);
            }
            // Always add the queue placeholder as the last line
            loreList.add("§eQueued: {queued}");
            kitsSection.set(kitName + ".lore", loreList);
            
            try {
                guiConfig.save(guiFile);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to update lore in " + guiFile.getName() + ": " + e.getMessage());
                return false;
            }
        }
        return false;
    }
    
    private boolean updateKitHideAttributesInGuiConfig(String kitName, String matchType, boolean hideAttributes) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, matchType + ".yml");
        
        if (!guiFile.exists()) return false;
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null) {
            for (String key : kitsSection.getKeys(false)) {
                if (kitsSection.getString(key + ".kit", "").equalsIgnoreCase(kitName)) {
                    kitsSection.set(key + ".hideAttributes", hideAttributes);
                    
                    try {
                        guiConfig.save(guiFile);
                        return true;
                    } catch (IOException e) {
                        plugin.getLogger().severe("Failed to update hideAttributes in " + guiFile.getName() + ": " + e.getMessage());
                        return false;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean updateKitHideAttributesInQueueGuiConfig(String kitName, String mode, boolean hideAttributes) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, mode + "kits.yml");
        
        if (!guiFile.exists()) return false;
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null && kitsSection.contains(kitName)) {
            kitsSection.set(kitName + ".hideAttributes", hideAttributes);
            
            try {
                guiConfig.save(guiFile);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to update hideAttributes in " + guiFile.getName() + ": " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Updates the icon for a kit in a GUI configuration file.
     * 
     * @param kit The kit to update
     * @param matchType The match type ("split" or "ffa")
     * @param materialName The material name for the icon
     * @param customModelData The custom model data for the icon (can be null)
     */
    private void updateKitIconInGuiConfig(Kit kit, String matchType, String materialName, Integer customModelData) {
        if (kit == null || materialName == null) {
            plugin.getLogger().warning("Cannot update kit icon with null kit or material");
            return;
        }
        
        File guiDir = new File(dataFolder, "gui");
        File guiFile = new File(guiDir, matchType + ".yml");
        
        if (!guiFile.exists()) {
            plugin.getLogger().warning("GUI config file not found: " + guiFile.getName());
            return;
        }

        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null) {
            boolean kitFound = false;
            for (String key : kitsSection.getKeys(false)) {
                if (kitsSection.getString(key + ".kit", "").equalsIgnoreCase(kit.getName())) {
                    // Update material and custom model data
                    kitsSection.set(key + ".material", materialName);
                    if (customModelData != null) {
                        kitsSection.set(key + ".customModelData", customModelData);
                    } else {
                        kitsSection.set(key + ".customModelData", null);
                    }
                    kitFound = true;
                    break;
                }
            }
            
            if (kitFound) {
                try {
                    guiConfig.save(guiFile);
                    plugin.getLogger().info("Updated icon for kit '" + kit.getName() + "' in " + matchType + " GUI");
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save " + guiFile.getName(), e);
                }
            } else {
                plugin.getLogger().warning("Kit '" + kit.getName() + "' not found in " + matchType + " GUI for icon update");
            }
        }
    }

    /**
     * Updates the icon for a kit in a queue GUI configuration file.
     * 
     * @param kit The kit to update
     * @param mode The queue mode ("1v1", "2v2", or "3v3")
     * @param materialName The material name for the icon
     * @param customModelData The custom model data for the icon (can be null)
     */
    private void updateKitIconInQueueGuiConfig(Kit kit, String mode, String materialName, Integer customModelData) {
        if (kit == null || materialName == null) {
            plugin.getLogger().warning("Cannot update queue kit icon with null kit or material");
            return;
        }
        
        File guiDir = new File(dataFolder, "gui");
        File guiFile = new File(guiDir, mode + "kits.yml");
        
        if (!guiFile.exists()) {
            plugin.getLogger().warning("Queue GUI config file not found: " + guiFile.getName());
            return;
        }

        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        String kitKey = sanitizeConfigKey(kit.getName());
        boolean kitFound = false;
        
        if (kitsSection != null) {
            // First try with sanitized key
            if (kitsSection.contains(kitKey)) {
                kitsSection.set(kitKey + ".material", materialName);
                if (customModelData != null) {
                    kitsSection.set(kitKey + ".customModelData", customModelData);
                } else {
                    kitsSection.set(kitKey + ".customModelData", null);
                }
                kitFound = true;
            } else {
                // Try to find by kit name in case the key format changed
                for (String key : kitsSection.getKeys(false)) {
                    if (key.equalsIgnoreCase(kit.getName()) || 
                        (kitsSection.contains(key + ".kit") && 
                         kitsSection.getString(key + ".kit").equalsIgnoreCase(kit.getName()))) {
                        kitsSection.set(key + ".material", materialName);
                        if (customModelData != null) {
                            kitsSection.set(key + ".customModelData", customModelData);
                        } else {
                            kitsSection.set(key + ".customModelData", null);
                        }
                        kitFound = true;
                        break;
                    }
                }
            }
            
            if (kitFound) {
                try {
                    guiConfig.save(guiFile);
                    plugin.getLogger().info("Updated icon for kit '" + kit.getName() + "' in " + mode + " queue GUI");
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save " + guiFile.getName(), e);
                }
            } else {
                plugin.getLogger().warning("Kit '" + kit.getName() + "' not found in " + mode + " queue GUI for icon update");
            }
        }
    }

    public String getKitSlotInGui(Kit kit, String matchType) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, matchType + ".yml");
        
        if (!guiFile.exists()) return "Not in GUI";
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null) {
            for (String key : kitsSection.getKeys(false)) {
                if (kitsSection.getString(key + ".kit", "").equalsIgnoreCase(kit.getName())) {
                    return String.valueOf(kitsSection.getInt(key + ".slot"));
                }
            }
        }
        
        return "Not in GUI";
    }

    public String getKitSlotInQueueGui(Kit kit, String mode) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, mode + "kits.yml");
        
        if (!guiFile.exists()) return "Not in GUI";
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null && kitsSection.contains(kit.getName())) {
            return String.valueOf(kitsSection.getInt(kit.getName() + ".slot"));
        }
        
        return "Not in GUI";
    }

    public boolean updateKitSlot(Kit kit, String guiType, int newSlot) {
        if (guiType.equals("split") || guiType.equals("ffa")) {
            return updateKitSlotInGuiConfig(kit, guiType, newSlot);
        } else if (guiType.equals("1v1") || guiType.equals("2v2") || guiType.equals("3v3")) {
            return updateKitSlotInQueueGuiConfig(kit, guiType, newSlot);
        }
        return false;
    }

    private boolean updateKitSlotInGuiConfig(Kit kit, String matchType, int newSlot) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, matchType + ".yml");
        
        if (!guiFile.exists()) return false;
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection == null) return false;
        
        // Check if new slot is already taken
        for (String key : kitsSection.getKeys(false)) {
            if (kitsSection.getInt(key + ".slot") == newSlot && 
                !kitsSection.getString(key + ".kit", "").equalsIgnoreCase(kit.getName())) {
                return false; // Slot taken by another kit
            }
        }
        
        // Find and update the kit's slot
        for (String key : kitsSection.getKeys(false)) {
            if (kitsSection.getString(key + ".kit", "").equalsIgnoreCase(kit.getName())) {
                kitsSection.set(key + ".slot", newSlot);
                
                try {
                    guiConfig.save(guiFile);
                    return true;
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to save " + guiFile.getName() + ": " + e.getMessage());
                    return false;
                }
            }
        }
        
        return false; // Kit not found in GUI
    }

    private boolean updateKitSlotInQueueGuiConfig(Kit kit, String mode, int newSlot) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, mode + "kits.yml");
        
        if (!guiFile.exists()) return false;
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection == null || !kitsSection.contains(kit.getName())) return false;
        
        // Check if new slot is already taken
        for (String key : kitsSection.getKeys(false)) {
            if (kitsSection.getInt(key + ".slot") == newSlot && !key.equals(kit.getName())) {
                return false; // Slot taken by another kit
            }
        }
        
        // Update the slot
        kitsSection.set(kit.getName() + ".slot", newSlot);
        
        try {
            guiConfig.save(guiFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + guiFile.getName() + ": " + e.getMessage());
            return false;
        }
    }
}
