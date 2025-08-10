package me.moiz.mangoparty.config;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Kit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConfigManager {
    private MangoParty plugin;
    
    public ConfigManager(MangoParty plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfigs() {
        // Create plugin data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Create default configuration files
        createDefaultConfig();
        createDefaultGuiConfigs();
        createDefaultArenaConfig();
    }
    
    private void createDefaultConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
    }
    
    private void createDefaultGuiConfigs() {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        if (!guiDir.exists()) {
            guiDir.mkdirs();
        }
        
        // Create split.yml
        File splitFile = new File(guiDir, "split.yml");
        if (!splitFile.exists()) {
            YamlConfiguration splitConfig = new YamlConfiguration();
            splitConfig.set("title", "§6Select Kit - Party Split");
            splitConfig.set("size", 27);
            
            // Example kit configurations
            splitConfig.set("kits.warrior.slot", 10);
            splitConfig.set("kits.warrior.name", "§cWarrior Kit");
            splitConfig.set("kits.warrior.kit", "warrior");
            splitConfig.set("kits.warrior.lore", new String[]{"§7A balanced melee kit", "§7with sword and armor"});
            splitConfig.set("kits.warrior.customModelData", 1001);
            
            splitConfig.set("kits.archer.slot", 12);
            splitConfig.set("kits.archer.name", "§aArcher Kit");
            splitConfig.set("kits.archer.kit", "archer");
            splitConfig.set("kits.archer.lore", new String[]{"§7Ranged combat kit", "§7with bow and arrows"});
            splitConfig.set("kits.archer.customModelData", 1002);
            
            splitConfig.set("kits.mage.slot", 14);
            splitConfig.set("kits.mage.name", "§9Mage Kit");
            splitConfig.set("kits.mage.kit", "mage");
            splitConfig.set("kits.mage.lore", new String[]{"§7Magical kit with", "§7potions and enchanted items"});
            splitConfig.set("kits.mage.customModelData", 1003);
            
            try {
                splitConfig.save(splitFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create split.yml: " + e.getMessage());
            }
        }
        
        // Create ffa.yml
        File ffaFile = new File(guiDir, "ffa.yml");
        if (!ffaFile.exists()) {
            YamlConfiguration ffaConfig = new YamlConfiguration();
            ffaConfig.set("title", "§6Select Kit - Party FFA");
            ffaConfig.set("size", 27);
            
            // Example kit configurations for FFA
            ffaConfig.set("kits.berserker.slot", 10);
            ffaConfig.set("kits.berserker.name", "§4Berserker Kit");
            ffaConfig.set("kits.berserker.kit", "berserker");
            ffaConfig.set("kits.berserker.lore", new String[]{"§7High damage melee kit", "§7for aggressive players"});
            ffaConfig.set("kits.berserker.customModelData", 2001);
            
            ffaConfig.set("kits.assassin.slot", 12);
            ffaConfig.set("kits.assassin.name", "§8Assassin Kit");
            ffaConfig.set("kits.assassin.kit", "assassin");
            ffaConfig.set("kits.assassin.lore", new String[]{"§7Stealth and speed kit", "§7for quick eliminations"});
            ffaConfig.set("kits.assassin.customModelData", 2002);
            
            ffaConfig.set("kits.tank.slot", 14);
            ffaConfig.set("kits.tank.name", "§7Tank Kit");
            ffaConfig.set("kits.tank.kit", "tank");
            ffaConfig.set("kits.tank.lore", new String[]{"§7Heavy armor kit", "§7for defensive play"});
            ffaConfig.set("kits.tank.customModelData", 2003);
            
            try {
                ffaConfig.save(ffaFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create ffa.yml: " + e.getMessage());
            }
        }
    }
    
    private void createDefaultArenaConfig() {
        File arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenasFile.exists()) {
            YamlConfiguration arenasConfig = new YamlConfiguration();
            arenasConfig.set("arenas", ""); // Empty arenas section
            
            try {
                arenasConfig.save(arenasFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create arenas.yml: " + e.getMessage());
            }
        }
    }

    public boolean addKitToGuiConfig(Kit kit, String matchType, Integer slot) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile;
        YamlConfiguration guiConfig;

        if (matchType.equalsIgnoreCase("split")) {
            guiFile = new File(guiDir, "split.yml");
        } else if (matchType.equalsIgnoreCase("ffa")) {
            guiFile = new File(guiDir, "ffa.yml");
        } else {
            return false; // Invalid match type
        }

        if (!guiFile.exists()) {
            plugin.getLogger().warning("GUI config file not found: " + guiFile.getName());
            return false;
        }

        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        // Check if kit already exists in this GUI config
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        if (kitsSection != null) {
            for (String key : kitsSection.getKeys(false)) {
                if (kitsSection.getString(key + ".kit", "").equalsIgnoreCase(kit.getName())) {
                    plugin.getLogger().info("Kit " + kit.getName() + " already exists in " + matchType + " GUI.");
                    return false; // Kit already exists
                }
            }
        } else {
            kitsSection = guiConfig.createSection("kits");
        }

        // Determine slot
        if (slot == null) {
            // Find next available slot
            Set<String> existingKitKeys = kitsSection.getKeys(false);
            List<Integer> usedSlots = new ArrayList<>();
            for (String key : existingKitKeys) {
                usedSlots.add(kitsSection.getInt(key + ".slot"));
            }

            for (int i = 0; i < guiConfig.getInt("size", 27); i++) {
                if (!usedSlots.contains(i)) {
                    slot = i;
                    break;
                }
            }
            if (slot == null) {
                plugin.getLogger().warning("No available slot found in " + matchType + " GUI for kit " + kit.getName());
                return false; // No available slot
            }
        } else {
            // Check if provided slot is already taken
            Set<String> existingKitKeys = kitsSection.getKeys(false);
            for (String key : existingKitKeys) {
                if (kitsSection.getInt(key + ".slot") == slot) {
                    plugin.getLogger().warning("Slot " + slot + " is already taken in " + matchType + " GUI.");
                    return false; // Slot already taken
                }
            }
        }

        // Add kit details to config
        String kitConfigKey = kit.getName().toLowerCase().replace(" ", "_"); // Sanitize key
        kitsSection.set(kitConfigKey + ".slot", slot);
        kitsSection.set(kitConfigKey + ".name", kit.getDisplayName());
        kitsSection.set(kitConfigKey + ".kit", kit.getName());
        
        // Default lore if none exists
        List<String> defaultLore = new ArrayList<>();
        defaultLore.add("§7A custom kit created by an admin.");
        kitsSection.set(kitConfigKey + ".lore", defaultLore);

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

    public boolean addKitToQueueGuiConfig(Kit kit, String mode, Integer slot) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, mode + "kits.yml");
        YamlConfiguration guiConfig;

        if (!guiFile.exists()) {
            // Create default queue config
            guiConfig = new YamlConfiguration();
            guiConfig.set("title", "§6" + mode.toUpperCase() + " Kit Selection");
            guiConfig.set("size", 27);
            guiConfig.set("kits", "");
            
            try {
                guiConfig.save(guiFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create " + guiFile.getName() + ": " + e.getMessage());
                return false;
            }
        }

        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        // Check if kit already exists in this GUI config
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        if (kitsSection != null) {
            if (kitsSection.contains(kit.getName())) {
                plugin.getLogger().info("Kit " + kit.getName() + " already exists in " + mode + " queue GUI.");
                return false; // Kit already exists
            }
        } else {
            kitsSection = guiConfig.createSection("kits");
        }

        // Determine slot
        if (slot == null) {
            // Find next available slot
            Set<String> existingKitKeys = kitsSection.getKeys(false);
            List<Integer> usedSlots = new ArrayList<>();
            for (String key : existingKitKeys) {
                usedSlots.add(kitsSection.getInt(key + ".slot"));
            }

            for (int i = 0; i < guiConfig.getInt("size", 27); i++) {
                if (!usedSlots.contains(i)) {
                    slot = i;
                    break;
                }
            }
            if (slot == null) {
                plugin.getLogger().warning("No available slot found in " + mode + " queue GUI for kit " + kit.getName());
                return false; // No available slot
            }
        } else {
            // Check if provided slot is already taken
            Set<String> existingKitKeys = kitsSection.getKeys(false);
            for (String key : existingKitKeys) {
                if (kitsSection.getInt(key + ".slot") == slot) {
                    plugin.getLogger().warning("Slot " + slot + " is already taken in " + mode + " queue GUI.");
                    return false; // Slot already taken
                }
            }
        }

        // Add kit details to config
        String kitConfigKey = kit.getName();
        kitsSection.set(kitConfigKey + ".slot", slot);
        kitsSection.set(kitConfigKey + ".material", kit.getIcon() != null ? kit.getIcon().getType().toString() : "IRON_SWORD");
        kitsSection.set(kitConfigKey + ".name", kit.getDisplayName());
        
        // Default lore with queue placeholder
        List<String> defaultLore = new ArrayList<>();
        defaultLore.add("§7Click to queue with this kit.");
        defaultLore.add("§eQueued: {queued}");
        kitsSection.set(kitConfigKey + ".lore", defaultLore);

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

    public boolean removeKitFromGuiConfig(Kit kit, String matchType) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile;
        YamlConfiguration guiConfig;

        if (matchType.equalsIgnoreCase("split")) {
            guiFile = new File(guiDir, "split.yml");
        } else if (matchType.equalsIgnoreCase("ffa")) {
            guiFile = new File(guiDir, "ffa.yml");
        } else {
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
            for (String key : kitsSection.getKeys(false)) {
                if (kitsSection.getString(key + ".kit", "").equalsIgnoreCase(kit.getName())) {
                    kitsSection.set(key, null);
                    
                    try {
                        guiConfig.save(guiFile);
                        return true;
                    } catch (IOException e) {
                        plugin.getLogger().severe("Failed to save " + guiFile.getName() + ": " + e.getMessage());
                        return false;
                    }
                }
            }
        }
        
        return false; // Kit not found
    }

    public boolean removeKitFromQueueGuiConfig(Kit kit, String mode) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, mode + "kits.yml");
        YamlConfiguration guiConfig;

        if (!guiFile.exists()) {
            plugin.getLogger().warning("Queue GUI config file not found: " + guiFile.getName());
            return false;
        }

        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        // Remove the kit
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        if (kitsSection != null && kitsSection.contains(kit.getName())) {
            kitsSection.set(kit.getName(), null);
            
            try {
                guiConfig.save(guiFile);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save " + guiFile.getName() + ": " + e.getMessage());
                return false;
            }
        }
        
        return false; // Kit not found
    }

    public void updateKitIconInAllGuis(Kit kit) {
        if (kit.getIcon() == null) return;
        
        String materialName = kit.getIcon().getType().toString();
        Integer customModelData = null;
        if (kit.getIcon().hasItemMeta() && kit.getIcon().getItemMeta().hasCustomModelData()) {
            customModelData = kit.getIcon().getItemMeta().getCustomModelData();
        }
        
        // Update split and ffa GUIs
        updateKitIconInGuiConfig(kit, "split", materialName, customModelData);
        updateKitIconInGuiConfig(kit, "ffa", materialName, customModelData);
        
        // Update queue GUIs
        updateKitIconInQueueGuiConfig(kit, "1v1", materialName, customModelData);
        updateKitIconInQueueGuiConfig(kit, "2v2", materialName, customModelData);
        updateKitIconInQueueGuiConfig(kit, "3v3", materialName, customModelData);
    }

    private void updateKitIconInGuiConfig(Kit kit, String matchType, String materialName, Integer customModelData) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, matchType + ".yml");
        
        if (!guiFile.exists()) return;
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null) {
            for (String key : kitsSection.getKeys(false)) {
                if (kitsSection.getString(key + ".kit", "").equalsIgnoreCase(kit.getName())) {
                    // Update material (icon is handled by kit.getIcon() in GUI creation)
                    if (customModelData != null) {
                        kitsSection.set(key + ".customModelData", customModelData);
                    }
                    
                    try {
                        guiConfig.save(guiFile);
                        plugin.getLogger().info("Updated kit icon in " + matchType + " GUI for kit: " + kit.getName());
                    } catch (IOException e) {
                        plugin.getLogger().severe("Failed to update icon in " + guiFile.getName() + ": " + e.getMessage());
                    }
                    break;
                }
            }
        }
    }

    private void updateKitIconInQueueGuiConfig(Kit kit, String mode, String materialName, Integer customModelData) {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File guiFile = new File(guiDir, mode + "kits.yml");
        
        if (!guiFile.exists()) return;
        
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = guiConfig.getConfigurationSection("kits");
        
        if (kitsSection != null && kitsSection.contains(kit.getName())) {
            kitsSection.set(kit.getName() + ".material", materialName);
            if (customModelData != null) {
                kitsSection.set(kit.getName() + ".customModelData", customModelData);
            }
            
            try {
                guiConfig.save(guiFile);
                plugin.getLogger().info("Updated kit icon in " + mode + " queue GUI for kit: " + kit.getName());
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to update icon in " + guiFile.getName() + ": " + e.getMessage());
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
