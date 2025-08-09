package me.moiz.mangoparty.gui;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.models.KitRules;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KitEditorGui implements Listener {
    private MangoParty plugin;
    private YamlConfiguration kitListConfig;
    private YamlConfiguration kitEditorConfig;
    
    public KitEditorGui(MangoParty plugin) {
        this.plugin = plugin;
        loadConfigs();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    private void loadConfigs() {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        if (!guiDir.exists()) {
            guiDir.mkdirs();
        }
        
        File kitListFile = new File(guiDir, "kit_list.yml");
        File kitEditorFile = new File(guiDir, "kit_editor.yml");
        
        if (!kitListFile.exists()) {
            plugin.saveResource("gui/kit_list.yml", false);
        }
        if (!kitEditorFile.exists()) {
            plugin.saveResource("gui/kit_editor.yml", false);
        }
        
        kitListConfig = YamlConfiguration.loadConfiguration(kitListFile);
        kitEditorConfig = YamlConfiguration.loadConfiguration(kitEditorFile);
    }
    
    public void openKitListGui(Player player) {
        String title = kitListConfig.getString("title", "§6Kit Manager");
        int size = kitListConfig.getInt("size", 54);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        Map<String, Kit> kits = plugin.getKitManager().getKits();
        int slot = 0;
        
        for (Kit kit : kits.values()) {
            if (slot >= size) break;
            
            ItemStack item = createKitItem(kit);
            gui.setItem(slot, item);
            slot++;
        }
        
        player.openInventory(gui);
    }
    
    private ItemStack createKitItem(Kit kit) {
        ConfigurationSection kitConfig = kitListConfig.getConfigurationSection("kits." + kit.getName());
        ConfigurationSection defaultConfig = kitListConfig.getConfigurationSection("default");
        
        String materialName = kitConfig != null ? kitConfig.getString("material") : defaultConfig.getString("material");
        Material material = Material.valueOf(materialName);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String name = kitConfig != null ? kitConfig.getString("name") : defaultConfig.getString("name");
        name = name.replace("{kit_name}", kit.getName());
        meta.setDisplayName(name);
        
        List<String> lore = kitConfig != null ? kitConfig.getStringList("lore") : defaultConfig.getStringList("lore");
        List<String> processedLore = new ArrayList<>();
        for (String line : lore) {
            line = line.replace("{kit_name}", kit.getName());
            line = line.replace("{kit_display_name}", kit.getDisplayName());
            processedLore.add(line);
        }
        meta.setLore(processedLore);
        
        int customModelData = kitConfig != null ? kitConfig.getInt("customModelData") : defaultConfig.getInt("customModelData");
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    public void openKitEditorGui(Player player, String kitName) {
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            player.sendMessage("§cKit not found!");
            return;
        }
        
        String title = kitEditorConfig.getString("title", "§6Kit Rules").replace("{kit_name}", kitName);
        int size = kitEditorConfig.getInt("size", 27);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection buttons = kitEditorConfig.getConfigurationSection("buttons");
        if (buttons != null) {
            for (String buttonKey : buttons.getKeys(false)) {
                ConfigurationSection buttonConfig = buttons.getConfigurationSection(buttonKey);
                ItemStack item = createRuleButton(buttonConfig, buttonKey, kit.getRules());
                gui.setItem(buttonConfig.getInt("slot"), item);
            }
        }
        
        player.openInventory(gui);
    }
    
    private ItemStack createRuleButton(ConfigurationSection config, String ruleKey, KitRules rules) {
        boolean enabled = getRuleValue(ruleKey, rules);
        
        String materialKey = enabled ? "material_enabled" : "material_disabled";
        String nameKey = enabled ? "name_enabled" : "name_disabled";
        String loreKey = enabled ? "lore_enabled" : "lore_disabled";
        
        Material material = Material.valueOf(config.getString(materialKey));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(config.getString(nameKey));
        meta.setLore(config.getStringList(loreKey));
        
        int customModelData = config.getInt("customModelData");
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private boolean getRuleValue(String ruleKey, KitRules rules) {
        switch (ruleKey) {
            case "natural_health_regen":
                return rules.isNaturalHealthRegen();
            case "block_break":
                return rules.isBlockBreak();
            case "block_place":
                return rules.isBlockPlace();
            case "damage_multiplier":
                return rules.getDamageMultiplier() > 1.0;
            case "instant_tnt":
                return rules.isInstantTnt();
            default:
                return false;
        }
    }
    
    private void toggleRule(String ruleKey, KitRules rules) {
        switch (ruleKey) {
            case "natural_health_regen":
                rules.setNaturalHealthRegen(!rules.isNaturalHealthRegen());
                break;
            case "block_break":
                rules.setBlockBreak(!rules.isBlockBreak());
                break;
            case "block_place":
                rules.setBlockPlace(!rules.isBlockPlace());
                break;
            case "damage_multiplier":
                rules.setDamageMultiplier(rules.getDamageMultiplier() > 1.0 ? 1.0 : 1.33);
                break;
            case "instant_tnt":
                rules.setInstantTnt(!rules.isInstantTnt());
                break;
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (title.equals(kitListConfig.getString("title", "§6Kit Manager"))) {
            event.setCancelled(true);
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String kitName = extractKitName(clicked);
            if (kitName != null) {
                openKitEditorGui(player, kitName);
            }
        } else if (title.startsWith(kitEditorConfig.getString("title", "§6Kit Rules").split(" - ")[0])) {
            event.setCancelled(true);
            
            String kitName = extractKitNameFromTitle(title);
            if (kitName == null) return;
            
            Kit kit = plugin.getKitManager().getKit(kitName);
            if (kit == null) return;
            
            String ruleKey = identifyRuleButton(event.getSlot());
            if (ruleKey != null) {
                toggleRule(ruleKey, kit.getRules());
                plugin.getKitManager().saveKit(kit);
                
                // Refresh the GUI
                openKitEditorGui(player, kitName);
                
                player.sendMessage("§aToggled " + ruleKey.replace("_", " ") + " for kit: " + kitName);
            }
        }
    }
    
    private String extractKitName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            for (Kit kit : plugin.getKitManager().getKits().values()) {
                if (displayName.contains(kit.getName())) {
                    return kit.getName();
                }
            }
        }
        return null;
    }
    
    private String extractKitNameFromTitle(String title) {
        String prefix = kitEditorConfig.getString("title", "§6Kit Rules").split(" - ")[0];
        if (title.startsWith(prefix + " - ")) {
            return title.substring((prefix + " - ").length());
        }
        return null;
    }
    
    private String identifyRuleButton(int slot) {
        ConfigurationSection buttons = kitEditorConfig.getConfigurationSection("buttons");
        if (buttons != null) {
            for (String buttonKey : buttons.getKeys(false)) {
                if (buttons.getConfigurationSection(buttonKey).getInt("slot") == slot) {
                    return buttonKey;
                }
            }
        }
        return null;
    }
}
