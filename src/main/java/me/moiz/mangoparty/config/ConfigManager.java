package me.moiz.mangoparty.config;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Kit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public boolean addKitToGuiConfig(Kit kit, String guiType, Integer slot) {
        try {
            File guiFile = new File(plugin.getDataFolder(), "gui/" + guiType + ".yml");
            if (!guiFile.exists()) {
                plugin.getLogger().warning("GUI file not found: " + guiFile.getPath());
                return false;
            }
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            
            // Check if kit already exists
            ConfigurationSection kitsSection = config.getConfigurationSection("kits");
            if (kitsSection != null && kitsSection.contains(kit.getName())) {
                return false; // Kit already exists
            }
            
            // Create kits section if it doesn't exist
            if (kitsSection == null) {
                kitsSection = config.createSection("kits");
            }
            
            // Find available slot if not specified
            if (slot == null) {
                slot = findAvailableSlot(config);
            }
            
            // Add kit configuration
            ConfigurationSection kitSection = kitsSection.createSection(kit.getName());
            kitSection.set("slot", slot);
            kitSection.set("material", kit.getIcon().getType().name());
            kitSection.set("name", "§e" + kit.getName());
            kitSection.set("lore", java.util.Arrays.asList("§7Click to select this kit"));
            
            if (kit.getIcon().hasItemMeta() && kit.getIcon().getItemMeta().hasCustomModelData()) {
                kitSection.set("customModelData", kit.getIcon().getItemMeta().getCustomModelData());
            }
            
            config.save(guiFile);
            return true;
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save GUI config: " + e.getMessage());
            return false;
        }
    }
    
    public boolean addKitToQueueGuiConfig(Kit kit, String queueType, Integer slot) {
        try {
            File guiFile = new File(plugin.getDataFolder(), "gui/" + queueType + "kits.yml");
            if (!guiFile.exists()) {
                plugin.getLogger().warning("Queue GUI file not found: " + guiFile.getPath());
                return false;
            }
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            
            // Check if kit already exists
            ConfigurationSection kitsSection = config.getConfigurationSection("kits");
            if (kitsSection != null && kitsSection.contains(kit.getName())) {
                return false; // Kit already exists
            }
            
            // Create kits section if it doesn't exist
            if (kitsSection == null) {
                kitsSection = config.createSection("kits");
            }
            
            // Find available slot if not specified
            if (slot == null) {
                slot = findAvailableSlot(config);
            }
            
            // Add kit configuration
            ConfigurationSection kitSection = kitsSection.createSection(kit.getName());
            kitSection.set("slot", slot);
            kitSection.set("material", kit.getIcon().getType().name());
            kitSection.set("name", "§e" + kit.getName());
            kitSection.set("lore", java.util.Arrays.asList("§7Click to queue with this kit"));
            
            if (kit.getIcon().hasItemMeta() && kit.getIcon().getItemMeta().hasCustomModelData()) {
                kitSection.set("customModelData", kit.getIcon().getItemMeta().getCustomModelData());
            }
            
            config.save(guiFile);
            return true;
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save queue GUI config: " + e.getMessage());
            return false;
        }
    }
    
    public boolean removeKitFromGuiConfig(String kitName, String guiType) {
        try {
            File guiFile = new File(plugin.getDataFolder(), "gui/" + guiType + ".yml");
            if (!guiFile.exists()) {
                return false;
            }
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection kitsSection = config.getConfigurationSection("kits");
            
            if (kitsSection == null || !kitsSection.contains(kitName)) {
                return false; // Kit doesn't exist
            }
            
            kitsSection.set(kitName, null);
            config.save(guiFile);
            return true;
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save GUI config: " + e.getMessage());
            return false;
        }
    }
    
    public boolean removeKitFromQueueGuiConfig(String kitName, String queueType) {
        try {
            File guiFile = new File(plugin.getDataFolder(), "gui/" + queueType + "kits.yml");
            if (!guiFile.exists()) {
                return false;
            }
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection kitsSection = config.getConfigurationSection("kits");
            
            if (kitsSection == null || !kitsSection.contains(kitName)) {
                return false; // Kit doesn't exist
            }
            
            kitsSection.set(kitName, null);
            config.save(guiFile);
            return true;
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save queue GUI config: " + e.getMessage());
            return false;
        }
    }
    
    public void updateKitIconInAllGuis(Kit kit) {
        String[] guiTypes = {"split", "ffa"};
        String[] queueTypes = {"1v1", "2v2", "3v3"};
        
        // Update regular GUIs
        for (String guiType : guiTypes) {
            updateKitIconInGui(kit, guiType);
        }
        
        // Update queue GUIs
        for (String queueType : queueTypes) {
            updateKitIconInQueueGui(kit, queueType);
        }
    }
    
    private void updateKitIconInGui(Kit kit, String guiType) {
        try {
            File guiFile = new File(plugin.getDataFolder(), "gui/" + guiType + ".yml");
            if (!guiFile.exists()) return;
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection kitsSection = config.getConfigurationSection("kits");
            
            if (kitsSection != null && kitsSection.contains(kit.getName())) {
                ConfigurationSection kitSection = kitsSection.getConfigurationSection(kit.getName());
                kitSection.set("material", kit.getIcon().getType().name());
                
                if (kit.getIcon().hasItemMeta() && kit.getIcon().getItemMeta().hasCustomModelData()) {
                    kitSection.set("customModelData", kit.getIcon().getItemMeta().getCustomModelData());
                } else {
                    kitSection.set("customModelData", null);
                }
                
                config.save(guiFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update kit icon in GUI: " + e.getMessage());
        }
    }
    
    private void updateKitIconInQueueGui(Kit kit, String queueType) {
        try {
            File guiFile = new File(plugin.getDataFolder(), "gui/" + queueType + "kits.yml");
            if (!guiFile.exists()) return;
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection kitsSection = config.getConfigurationSection("kits");
            
            if (kitsSection != null && kitsSection.contains(kit.getName())) {
                ConfigurationSection kitSection = kitsSection.getConfigurationSection(kit.getName());
                kitSection.set("material", kit.getIcon().getType().name());
                
                if (kit.getIcon().hasItemMeta() && kit.getIcon().getItemMeta().hasCustomModelData()) {
                    kitSection.set("customModelData", kit.getIcon().getItemMeta().getCustomModelData());
                } else {
                    kitSection.set("customModelData", null);
                }
                
                config.save(guiFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update kit icon in queue GUI: " + e.getMessage());
        }
    }
    
    public Integer getKitSlotInGui(String kitName, String guiType) {
        try {
            String fileName = guiType.equals("1v1") || guiType.equals("2v2") || guiType.equals("3v3") 
                ? guiType + "kits.yml" 
                : guiType + ".yml";
                
            File guiFile = new File(plugin.getDataFolder(), "gui/" + fileName);
            if (!guiFile.exists()) return null;
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection kitsSection = config.getConfigurationSection("kits");
            
            if (kitsSection != null && kitsSection.contains(kitName)) {
                return kitsSection.getConfigurationSection(kitName).getInt("slot");
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to get kit slot: " + e.getMessage());
            return null;
        }
    }
    
    public String getKitAtSlot(String guiType, int slot) {
        try {
            String fileName = guiType.equals("1v1") || guiType.equals("2v2") || guiType.equals("3v3") 
                ? guiType + "kits.yml" 
                : guiType + ".yml";
                
            File guiFile = new File(plugin.getDataFolder(), "gui/" + fileName);
            if (!guiFile.exists()) return null;
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection kitsSection = config.getConfigurationSection("kits");
            
            if (kitsSection != null) {
                for (String kitName : kitsSection.getKeys(false)) {
                    ConfigurationSection kitSection = kitsSection.getConfigurationSection(kitName);
                    if (kitSection.getInt("slot") == slot) {
                        return kitName;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to get kit at slot: " + e.getMessage());
            return null;
        }
    }
    
    public boolean updateKitSlotInGui(String kitName, String guiType, int newSlot) {
        try {
            File guiFile = new File(plugin.getDataFolder(), "gui/" + guiType + ".yml");
            if (!guiFile.exists()) return false;
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection kitsSection = config.getConfigurationSection("kits");
            
            if (kitsSection != null && kitsSection.contains(kitName)) {
                kitsSection.getConfigurationSection(kitName).set("slot", newSlot);
                config.save(guiFile);
                return true;
            }
            
            return false;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update kit slot: " + e.getMessage());
            return false;
        }
    }
    
    public boolean updateKitSlotInQueueGui(String kitName, String queueType, int newSlot) {
        try {
            File guiFile = new File(plugin.getDataFolder(), "gui/" + queueType + "kits.yml");
            if (!guiFile.exists()) return false;
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection kitsSection = config.getConfigurationSection("kits");
            
            if (kitsSection != null && kitsSection.contains(kitName)) {
                kitsSection.getConfigurationSection(kitName).set("slot", newSlot);
                config.save(guiFile);
                return true;
            }
            
            return false;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update kit slot in queue GUI: " + e.getMessage());
            return false;
        }
    }
    
    private int findAvailableSlot(YamlConfiguration config) {
        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        if (kitsSection == null) return 0;
        
        // Get all used slots
        Set<Integer> usedSlots = new HashSet<>();
        for (String kitName : kitsSection.getKeys(false)) {
            ConfigurationSection kitSection = kitsSection.getConfigurationSection(kitName);
            usedSlots.add(kitSection.getInt("slot"));
        }
        
        // Find first available slot
        for (int i = 0; i < 54; i++) {
            if (!usedSlots.contains(i)) {
                return i;
            }
        }
        
        return 0; // Fallback
    }
}
