package me.moiz.mangoparty.managers;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.models.KitRules;
import me.moiz.mangoparty.utils.HexUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class KitManager {
    private MangoParty plugin;
    private Map<String, Kit> kits;
    private File kitsDir;
    
    public KitManager(MangoParty plugin) {
        this.plugin = plugin;
        this.kits = new HashMap<>();
        this.kitsDir = new File(plugin.getDataFolder(), "kits");
        
        if (!kitsDir.exists()) {
            kitsDir.mkdirs();
        }
        
        loadKits();
    }
    
    private void loadKits() {
        File[] kitFiles = kitsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (kitFiles != null) {
            for (File kitFile : kitFiles) {
                String kitName = kitFile.getName().replace(".yml", "");
                Kit kit = loadKitFromFile(kitName, kitFile);
                if (kit != null) {
                    kits.put(kitName, kit);
                }
            }
        }
    }
    
    private Kit loadKitFromFile(String name, File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            Kit kit = new Kit(name);
            
            kit.setDisplayName(config.getString("displayName", name));
            
            if (config.contains("contents")) {
                ItemStack[] contents = new ItemStack[36];
                for (int i = 0; i < 36; i++) {
                    if (config.contains("contents." + i)) {
                        contents[i] = config.getItemStack("contents." + i);
                    }
                }
                kit.setContents(contents);
            }
            
            if (config.contains("armor")) {
                ItemStack[] armor = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    if (config.contains("armor." + i)) {
                        armor[i] = config.getItemStack("armor." + i);
                    }
                }
                kit.setArmor(armor);
            }
            
            if (config.contains("offhand")) {
                kit.setOffhand(config.getItemStack("offhand"));
            }
            
            if (config.contains("icon")) {
                kit.setIcon(config.getItemStack("icon"));
            }
            
            // Load kit rules
            if (config.contains("rules")) {
                ConfigurationSection rulesSection = config.getConfigurationSection("rules");
                KitRules rules = new KitRules();
                
                rules.setNaturalHealthRegen(rulesSection.getBoolean("natural_health_regen", true));
                rules.setBlockBreak(rulesSection.getBoolean("block_break", false));
                rules.setBlockPlace(rulesSection.getBoolean("block_place", false));
                rules.setDamageMultiplier(rulesSection.getDouble("damage_multiplier", 1.0));
                rules.setInstantTnt(rulesSection.getBoolean("instant_tnt", false));
                
                kit.setRules(rules);
            }
            
            return kit;
        } catch (Exception e) {
            plugin.getLogger().warning(HexUtils.colorize("&c⚠️ Failed to load kit: " + name + " - " + e.getMessage()));
            return null;
        }
    }
    
    public void createKit(String name, Player player) {
        Kit kit = new Kit(name);
        kit.setDisplayName(name);
        kit.setContents(player.getInventory().getContents());
        kit.setArmor(player.getInventory().getArmorContents());
        kit.setOffhand(player.getInventory().getItemInOffHand().clone());
        
        // Use first item in inventory as icon, or default to sword
        ItemStack icon = null;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                icon = item.clone();
                icon.setAmount(1);
                break;
            }
        }
        if (icon == null) {
            icon = new ItemStack(org.bukkit.Material.IRON_SWORD);
        }
        kit.setIcon(icon);
        
        kits.put(name, kit);
        saveKit(kit);
        
        // Automatically add the kit to all GUIs
        plugin.getConfigManager().addKitToGuiConfig(kit, "split", null);
        plugin.getConfigManager().addKitToGuiConfig(kit, "ffa", null);
        plugin.getConfigManager().addKitToQueueGuiConfig(kit, "1v1", null);
        plugin.getConfigManager().addKitToQueueGuiConfig(kit, "2v2", null);
        plugin.getConfigManager().addKitToQueueGuiConfig(kit, "3v3", null);
        
        // Reload GUI configs to reflect changes
        plugin.getGuiManager().reloadGuiConfigs();
        
        plugin.getLogger().info(HexUtils.colorize("&a⚔️ Created new kit: &e" + name + " &7by &e" + player.getName()));
        player.sendMessage(HexUtils.colorize("&aKit '" + name + "' created and added to all GUIs!"));
    }
    
    public void saveKit(Kit kit) {
        File kitFile = new File(kitsDir, kit.getName() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        
        config.set("displayName", kit.getDisplayName());
        
        if (kit.getContents() != null) {
            for (int i = 0; i < kit.getContents().length; i++) {
                if (kit.getContents()[i] != null) {
                    config.set("contents." + i, kit.getContents()[i]);
                }
            }
        }
        
        if (kit.getArmor() != null) {
            for (int i = 0; i < kit.getArmor().length; i++) {
                if (kit.getArmor()[i] != null) {
                    config.set("armor." + i, kit.getArmor()[i]);
                }
            }
        }
        
        if (kit.getOffhand() != null) {
            config.set("offhand", kit.getOffhand());
        }
        
        if (kit.getIcon() != null) {
            config.set("icon", kit.getIcon());
        }
        
        // Save kit rules
        KitRules rules = kit.getRules();
        config.set("rules.natural_health_regen", rules.isNaturalHealthRegen());
        config.set("rules.block_break", rules.isBlockBreak());
        config.set("rules.block_place", rules.isBlockPlace());
        config.set("rules.damage_multiplier", rules.getDamageMultiplier());
        config.set("rules.instant_tnt", rules.isInstantTnt());
        
        try {
            config.save(kitFile);
        } catch (IOException e) {
            plugin.getLogger().severe(HexUtils.colorize("&c❌ Failed to save kit: " + kit.getName() + " - " + e.getMessage()));
        }
    }
    
    public Kit getKit(String name) {
        return kits.get(name);
    }
    
    public Map<String, Kit> getKits() {
        return new HashMap<>(kits);
    }
    
    public void giveKit(Player player, Kit kit) {
        player.getInventory().clear();
        
        if (kit.getContents() != null) {
            player.getInventory().setContents(kit.getContents());
        }
        
        if (kit.getArmor() != null) {
            player.getInventory().setArmorContents(kit.getArmor());
        }
        
        if (kit.getOffhand() != null) {
            player.getInventory().setItemInOffHand(kit.getOffhand());
        }
        
        player.updateInventory();
    }

    public void deleteKit(String name) {
        if (kits.containsKey(name)) {
            kits.remove(name);
            File kitFile = new File(kitsDir, name + ".yml");
            if (kitFile.exists()) {
                if (kitFile.delete()) {
                    plugin.getLogger().info(HexUtils.colorize("&a🗑️ Deleted kit file: &e" + name + ".yml"));
                } else {
                    plugin.getLogger().severe(HexUtils.colorize("&c❌ Failed to delete kit file: &e" + name + ".yml"));
                }
            } else {
                plugin.getLogger().warning(HexUtils.colorize("&c⚠️ Kit file not found for deletion: &e" + name + ".yml"));
            }
            plugin.getLogger().info(HexUtils.colorize("&a🗑️ Kit removed from memory: &e" + name));
        } else {
            plugin.getLogger().warning(HexUtils.colorize("&c⚠️ Kit not found in memory for deletion: &e" + name));
        }
    }
}
