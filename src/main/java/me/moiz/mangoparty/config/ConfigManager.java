package me.moiz.mangoparty.config;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Kit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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
            splitConfig.set("items.warrior.slot", 10);
            splitConfig.set("items.warrior.material", "DIAMOND_SWORD");
            splitConfig.set("items.warrior.name", "§cWarrior Kit");
            splitConfig.set("items.warrior.lore", new String[]{"§7A balanced melee kit", "§7with sword and armor"});
            splitConfig.set("items.warrior.customModelData", 1001);
            
            splitConfig.set("items.archer.slot", 12);
            splitConfig.set("items.archer.material", "BOW");
            splitConfig.set("items.archer.name", "§aArcher Kit");
            splitConfig.set("items.archer.lore", new String[]{"§7Ranged combat kit", "§7with bow and arrows"});
            splitConfig.set("items.archer.customModelData", 1002);
            
            splitConfig.set("items.mage.slot", 14);
            splitConfig.set("items.mage.material", "BOOK");
            splitConfig.set("items.mage.name", "§9Mage Kit");
            splitConfig.set("items.mage.lore", new String[]{"§7Magical kit with", "§7potions and enchanted items"});
            splitConfig.set("items.mage.customModelData", 1003);
            
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
            ffaConfig.set("items.berserker.slot", 10);
            ffaConfig.set("items.berserker.material", "DIAMOND_AXE");
            ffaConfig.set("items.berserker.name", "§4Berserker Kit");
            ffaConfig.set("items.berserker.lore", new String[]{"§7High damage melee kit", "§7for aggressive players"});
            ffaConfig.set("items.berserker.customModelData", 2001);
            
            ffaConfig.set("items.assassin.slot", 12);
            ffaConfig.set("items.assassin.material", "IRON_SWORD");
            ffaConfig.set("items.assassin.name", "§8Assassin Kit");
            ffaConfig.set("items.assassin.lore", new String[]{"§7Stealth and speed kit", "§7for quick eliminations"});
            ffaConfig.set("items.assassin.customModelData", 2002);
            
            ffaConfig.set("items.tank.slot", 14);
            ffaConfig.set("items.tank.material", "DIAMOND_CHESTPLATE");
            ffaConfig.set("items.tank.name", "§7Tank Kit");
            ffaConfig.set("items.tank.lore", new String[]{"§7Heavy armor kit", "§7for defensive play"});
            ffaConfig.set("items.tank.customModelData", 2003);
            
            try {
                ffaConfig.save(ffaFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create ffa.yml: " + e.getMessage());
            }
        }
        
        // Create queue GUI files
        String[] queueTypes = {"1v1", "2v2", "3v3"};
        for (String queueType : queueTypes) {
            File queueFile = new File(guiDir, queueType + "kits.yml");
            if (!queueFile.exists()) {
                YamlConfiguration queueConfig = new YamlConfiguration();
                queueConfig.set("title", "§6Select Kit - " + queueType.toUpperCase() + " Queue");
                queueConfig.set("size", 27);
                
                // Example kit configurations for queue
                queueConfig.set("items." + queueType + "Warrior.slot", 10);
                queueConfig.set("items." + queueType + "Warrior.material", "DIAMOND_SWORD");
                queueConfig.set("items." + queueType + "Warrior.name", "§c" + queueType.toUpperCase() + " Warrior Kit");
                queueConfig.set("items." + queueType + "Warrior.lore", new String[]{"§7A balanced melee kit", "§7for " + queueType + " matches"});
                queueConfig.set("items." + queueType + "Warrior.customModelData", 3001);
                
                queueConfig.set("items." + queueType + "Archer.slot", 12);
                queueConfig.set("items." + queueType + "Archer.material", "BOW");
                queueConfig.set("items." + queueType + "Archer.name", "§a" + queueType.toUpperCase() + " Archer Kit");
                queueConfig.set("items." + queueType + "Archer.lore", new String[]{"§7Ranged combat kit", "§7for " + queueType + " matches"});
                queueConfig.set("items." + queueType + "Archer.customModelData", 3002);
                
                queueConfig.set("items." + queueType + "Mage.slot", 14);
                queueConfig.set("items." + queueType + "Mage.material", "BOOK");
                queueConfig.set("items." + queueType + "Mage.name", "§9" + queueType.toUpperCase() + " Mage Kit");
                queueConfig.set("items." + queueType + "Mage.lore", new String[]{"§7Magical kit with", "§7potions and enchanted items"});
                queueConfig.set("items." + queueType + "Mage.customModelData", 3003);
                
                try {
                    queueConfig.save(queueFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to create " + queueType + "kits.yml: " + e.getMessage());
                }
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
                return false;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            
            if (itemsSection == null) {
                itemsSection = config.createSection("items");
            }

            // Find next available slot if not specified
            if (slot == null) {
                slot = findNextAvailableSlot(itemsSection);
            }

            // Check if slot is already taken
            if (isSlotTaken(itemsSection, slot)) {
                return false;
            }

            // Add kit to config
            ConfigurationSection kitSection = itemsSection.createSection(kit.getName());
            kitSection.set("slot", slot);
            kitSection.set("material", kit.getIcon().getType().name());
            kitSection.set("name", kit.getDisplayName());
            kitSection.set("lore", Arrays.asList("§7Click to select this kit"));

            config.save(guiFile);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to add kit to GUI config: " + e.getMessage());
            return false;
        }
    }

    public boolean addKitToQueueGuiConfig(Kit kit, String queueType, Integer slot) {
        try {
            File guiFile = new File(plugin.getDataFolder(), "gui/" + queueType + "kits.yml");
            if (!guiFile.exists()) {
                return false;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            
            if (itemsSection == null) {
                itemsSection = config.createSection("items");
            }

            // Find next available slot if not specified
            if (slot == null) {
                slot = findNextAvailableSlot(itemsSection);
            }

            // Check if slot is already taken
            if (isSlotTaken(itemsSection, slot)) {
                return false;
            }

            // Add kit to config
            ConfigurationSection kitSection = itemsSection.createSection(kit.getName());
            kitSection.set("slot", slot);
            kitSection.set("material", kit.getIcon().getType().name());
            kitSection.set("name", kit.getDisplayName());
            kitSection.set("lore", Arrays.asList("§7Click to queue with this kit"));

            config.save(guiFile);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to add kit to queue GUI config: " + e.getMessage());
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
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            
            if (itemsSection == null || !itemsSection.contains(kitName)) {
                return false;
            }

            itemsSection.set(kitName, null);
            config.save(guiFile);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to remove kit from GUI config: " + e.getMessage());
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
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            
            if (itemsSection == null || !itemsSection.contains(kitName)) {
                return false;
            }

            itemsSection.set(kitName, null);
            config.save(guiFile);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to remove kit from queue GUI config: " + e.getMessage());
            return false;
        }
    }

    public Integer getKitSlotInGui(String kitName, String guiType) {
        try {
            String fileName = guiType;
            if (guiType.equals("1v1") || guiType.equals("2v2") || guiType.equals("3v3")) {
                fileName = guiType + "kits";
            }
            
            File guiFile = new File(plugin.getDataFolder(), "gui/" + fileName + ".yml");
            if (!guiFile.exists()) {
                return null;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            
            if (itemsSection == null || !itemsSection.contains(kitName)) {
                return null;
            }

            return itemsSection.getInt(kitName + ".slot", -1);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to get kit slot: " + e.getMessage());
            return null;
        }
    }

    public String getKitAtSlot(String guiType, int slot) {
        try {
            String fileName = guiType;
            if (guiType.equals("1v1") || guiType.equals("2v2") || guiType.equals("3v3")) {
                fileName = guiType + "kits";
            }
            
            File guiFile = new File(plugin.getDataFolder(), "gui/" + fileName + ".yml");
            if (!guiFile.exists()) {
                return null;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            
            if (itemsSection == null) {
                return null;
            }

            for (String kitName : itemsSection.getKeys(false)) {
                if (itemsSection.getInt(kitName + ".slot") == slot) {
                    return kitName;
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
            if (!guiFile.exists()) {
                return false;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            
            if (itemsSection == null || !itemsSection.contains(kitName)) {
                return false;
            }

            itemsSection.set(kitName + ".slot", newSlot);
            config.save(guiFile);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update kit slot: " + e.getMessage());
            return false;
        }
    }

    public boolean updateKitSlotInQueueGui(String kitName, String queueType, int newSlot) {
        try {
            File guiFile = new File(plugin.getDataFolder(), "gui/" + queueType + "kits.yml");
            if (!guiFile.exists()) {
                return false;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            
            if (itemsSection == null || !itemsSection.contains(kitName)) {
                return false;
            }

            itemsSection.set(kitName + ".slot", newSlot);
            config.save(guiFile);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update kit slot in queue GUI: " + e.getMessage());
            return false;
        }
    }

    public void updateKitIconInAllGuis(Kit kit) {
        String[] guiTypes = {"split", "ffa", "1v1kits", "2v2kits", "3v3kits"};
        
        for (String guiType : guiTypes) {
            try {
                File guiFile = new File(plugin.getDataFolder(), "gui/" + guiType + ".yml");
                if (!guiFile.exists()) continue;

                YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
                ConfigurationSection itemsSection = config.getConfigurationSection("items");
                
                if (itemsSection != null && itemsSection.contains(kit.getName())) {
                    itemsSection.set(kit.getName() + ".material", kit.getIcon().getType().name());
                    config.save(guiFile);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to update kit icon in " + guiType + ": " + e.getMessage());
            }
        }
    }

    private int findNextAvailableSlot(ConfigurationSection itemsSection) {
        Set<Integer> usedSlots = new HashSet<>();
        
        for (String key : itemsSection.getKeys(false)) {
            usedSlots.add(itemsSection.getInt(key + ".slot"));
        }
        
        for (int i = 0; i < 54; i++) {
            if (!usedSlots.contains(i)) {
                return i;
            }
        }
        
        return 0; // Fallback
    }

    private boolean isSlotTaken(ConfigurationSection itemsSection, int slot) {
        for (String key : itemsSection.getKeys(false)) {
            if (itemsSection.getInt(key + ".slot") == slot) {
                return true;
            }
        }
        return false;
    }
}
