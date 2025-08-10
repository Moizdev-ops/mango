package me.moiz.mangoparty.gui;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import me.moiz.mangoparty.models.Kit;
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

public class AllowedKitsGui implements Listener {
    private MangoParty plugin;
    private YamlConfiguration allowedKitsConfig;
    
    public AllowedKitsGui(MangoParty plugin) {
        this.plugin = plugin;
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    private void loadConfig() {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        if (!guiDir.exists()) {
            guiDir.mkdirs();
        }
        
        File allowedKitsFile = new File(guiDir, "allowed_kits.yml");
        
        if (!allowedKitsFile.exists()) {
            plugin.saveResource("gui/allowed_kits.yml", false);
        }
        
        allowedKitsConfig = YamlConfiguration.loadConfiguration(allowedKitsFile);
    }
    
    public void openAllowedKitsGui(Player player, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            player.sendMessage("§cArena not found!");
            return;
        }
        
        String title = allowedKitsConfig.getString("title", "§6Allowed Kits").replace("{arena_name}", arenaName);
        int size = allowedKitsConfig.getInt("size", 54);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // Add all available kits with toggle status
        Map<String, Kit> availableKits = plugin.getKitManager().getKits();
        int slot = 0;
        
        for (Kit kit : availableKits.values()) {
            if (slot >= size - 9) break; // Reserve bottom row for controls
            
            ItemStack item = createKitToggleItem(kit, arena);
            gui.setItem(slot, item);
            slot++;
        }
        
        // Add back button
        ConfigurationSection backButtonConfig = allowedKitsConfig.getConfigurationSection("buttons.back");
        if (backButtonConfig != null) {
            ItemStack backButton = new ItemStack(Material.valueOf(backButtonConfig.getString("material")));
            ItemMeta meta = backButton.getItemMeta();
            meta.setDisplayName(backButtonConfig.getString("name"));
            meta.setLore(backButtonConfig.getStringList("lore"));
            
            int customModelData = backButtonConfig.getInt("customModelData");
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            
            backButton.setItemMeta(meta);
            gui.setItem(backButtonConfig.getInt("slot"), backButton);
        }
        
        player.openInventory(gui);
    }
    
    private ItemStack createKitToggleItem(Kit kit, Arena arena) {
        boolean isAllowed = arena.isKitAllowed(kit.getName());
        
        ItemStack item = kit.getIcon() != null ? kit.getIcon().clone() : new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName((isAllowed ? "§a" : "§c") + kit.getDisplayName());
        
        List<String> lore = new ArrayList<>();
        lore.add(isAllowed ? "§aEnabled for this arena" : "§cDisabled for this arena");
        lore.add("§7");
        lore.add("§eClick to " + (isAllowed ? "disable" : "enable") + " this kit");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (!title.startsWith(allowedKitsConfig.getString("title", "§6Allowed Kits").split(" - ")[0])) {
            return;
        }
        
        event.setCancelled(true);
        
        String arenaName = extractArenaNameFromTitle(title);
        if (arenaName == null) return;
        
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Handle back button
        ConfigurationSection backButtonConfig = allowedKitsConfig.getConfigurationSection("buttons.back");
        if (backButtonConfig != null && event.getSlot() == backButtonConfig.getInt("slot")) {
            plugin.getArenaEditorGui().openArenaEditorGui(player, arenaName);
            return;
        }
        
        // Handle kit toggle
        String kitName = extractKitName(clicked);
        if (kitName != null) {
            toggleKitAllowed(arena, kitName);
            plugin.getArenaManager().saveArena(arena);
            
            // Refresh the GUI
            openAllowedKitsGui(player, arenaName);
        }
    }
    
    private String extractArenaNameFromTitle(String title) {
        String prefix = allowedKitsConfig.getString("title", "§6Allowed Kits").split(" - ")[0];
        if (title.startsWith(prefix + " - ")) {
            return title.substring((prefix + " - ").length());
        }
        return null;
    }
    
    private String extractKitName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            // Remove color codes
            displayName = displayName.substring(2);
            
            for (Kit kit : plugin.getKitManager().getKits().values()) {
                if (displayName.equals(kit.getDisplayName())) {
                    return kit.getName();
                }
            }
        }
        return null;
    }
    
    private void toggleKitAllowed(Arena arena, String kitName) {
        if (arena.isKitAllowed(kitName)) {
            arena.removeAllowedKit(kitName);
        } else {
            arena.addAllowedKit(kitName);
        }
    }
}