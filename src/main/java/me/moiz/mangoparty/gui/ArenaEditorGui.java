package me.moiz.mangoparty.gui;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArenaEditorGui implements Listener {
    private MangoParty plugin;
    private YamlConfiguration arenaListConfig;
    private YamlConfiguration arenaEditorConfig;
    private Map<UUID, String> pendingLocationSets;
    private Map<UUID, String> pendingArenas;
    
    public ArenaEditorGui(MangoParty plugin) {
        this.plugin = plugin;
        this.pendingLocationSets = new HashMap<>();
        this.pendingArenas = new HashMap<>();
        loadConfigs();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    private void loadConfigs() {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        if (!guiDir.exists()) {
            guiDir.mkdirs();
        }
        
        File arenaListFile = new File(guiDir, "arena_list.yml");
        File arenaEditorFile = new File(guiDir, "arena_editor.yml");
        
        if (!arenaListFile.exists()) {
            plugin.saveResource("gui/arena_list.yml", false);
        }
        if (!arenaEditorFile.exists()) {
            plugin.saveResource("gui/arena_editor.yml", false);
        }
        
        arenaListConfig = YamlConfiguration.loadConfiguration(arenaListFile);
        arenaEditorConfig = YamlConfiguration.loadConfiguration(arenaEditorFile);
    }
    
    public void openArenaListGui(Player player) {
        String title = arenaListConfig.getString("title", "§6Arena Manager");
        int size = arenaListConfig.getInt("size", 54);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        Map<String, Arena> arenas = plugin.getArenaManager().getArenas();
        int slot = 0;
        
        for (Arena arena : arenas.values()) {
            if (slot >= size) break;
            
            ItemStack item = createArenaItem(arena);
            gui.setItem(slot, item);
            slot++;
        }
        
        player.openInventory(gui);
    }
    
    private ItemStack createArenaItem(Arena arena) {
        ConfigurationSection arenaConfig = arenaListConfig.getConfigurationSection("arenas." + arena.getName());
        ConfigurationSection defaultConfig = arenaListConfig.getConfigurationSection("default");
        
        String materialName = arenaConfig != null ? arenaConfig.getString("material") : defaultConfig.getString("material");
        Material material = Material.valueOf(materialName);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String name = arenaConfig != null ? arenaConfig.getString("name") : defaultConfig.getString("name");
        name = name.replace("{arena_name}", arena.getName());
        meta.setDisplayName(name);
        
        List<String> lore = arenaConfig != null ? arenaConfig.getStringList("lore") : defaultConfig.getStringList("lore");
        List<String> processedLore = new ArrayList<>();
        for (String line : lore) {
            line = line.replace("{arena_name}", arena.getName());
            line = line.replace("{status}", arena.isComplete() ? "§aComplete" : "§cIncomplete");
            processedLore.add(line);
        }
        meta.setLore(processedLore);
        
        int customModelData = arenaConfig != null ? arenaConfig.getInt("customModelData") : defaultConfig.getInt("customModelData");
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    public void openArenaEditorGui(Player player, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            player.sendMessage("§cArena not found!");
            return;
        }
        
        String title = arenaEditorConfig.getString("title", "§6Arena Editor").replace("{arena_name}", arenaName);
        int size = arenaEditorConfig.getInt("size", 27);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection buttons = arenaEditorConfig.getConfigurationSection("buttons");
        if (buttons != null) {
            for (String buttonKey : buttons.getKeys(false)) {
                ConfigurationSection buttonConfig = buttons.getConfigurationSection(buttonKey);
                ItemStack item = createEditorButton(buttonConfig, buttonKey, arena);
                gui.setItem(buttonConfig.getInt("slot"), item);
            }
        }
        
        player.openInventory(gui);
    }
    
    private ItemStack createEditorButton(ConfigurationSection config, String buttonKey, Arena arena) {
        Material material = Material.valueOf(config.getString("material"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String name = config.getString("name");
        meta.setDisplayName(name);
        
        List<String> lore = new ArrayList<>(config.getStringList("lore"));
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            line = processStatusPlaceholder(line, buttonKey, arena);
            lore.set(i, line);
        }
        meta.setLore(lore);
        
        int customModelData = config.getInt("customModelData");
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private String processStatusPlaceholder(String line, String buttonKey, Arena arena) {
        switch (buttonKey) {
            case "spawn1":
                if (line.contains("{spawn1_status}")) {
                    return line.replace("{spawn1_status}", arena.getSpawn1() != null ? "§aSet" : "§cNot Set");
                } else if (line.contains("{spawn1_coords}") && arena.getSpawn1() != null) {
                    return line.replace("{spawn1_coords}", String.format("§7X: %.1f, Y: %.1f, Z: %.1f", 
                            arena.getSpawn1().getX(), arena.getSpawn1().getY(), arena.getSpawn1().getZ()));
                }
                break;
            case "spawn2":
                if (line.contains("{spawn2_status}")) {
                    return line.replace("{spawn2_status}", arena.getSpawn2() != null ? "§aSet" : "§cNot Set");
                } else if (line.contains("{spawn2_coords}") && arena.getSpawn2() != null) {
                    return line.replace("{spawn2_coords}", String.format("§7X: %.1f, Y: %.1f, Z: %.1f", 
                            arena.getSpawn2().getX(), arena.getSpawn2().getY(), arena.getSpawn2().getZ()));
                }
                break;
            case "center":
                if (line.contains("{center_status}")) {
                    return line.replace("{center_status}", arena.getCenter() != null ? "§aSet" : "§cNot Set");
                } else if (line.contains("{center_coords}") && arena.getCenter() != null) {
                    return line.replace("{center_coords}", String.format("§7X: %.1f, Y: %.1f, Z: %.1f", 
                            arena.getCenter().getX(), arena.getCenter().getY(), arena.getCenter().getZ()));
                }
                break;
            case "corner1":
                if (line.contains("{corner1_status}")) {
                    return line.replace("{corner1_status}", arena.getCorner1() != null ? "§aSet" : "§cNot Set");
                } else if (line.contains("{corner1_coords}") && arena.getCorner1() != null) {
                    return line.replace("{corner1_coords}", String.format("§7X: %.1f, Y: %.1f, Z: %.1f", 
                            arena.getCorner1().getX(), arena.getCorner1().getY(), arena.getCorner1().getZ()));
                }
                break;
            case "corner2":
                if (line.contains("{corner2_status}")) {
                    return line.replace("{corner2_status}", arena.getCorner2() != null ? "§aSet" : "§cNot Set");
                } else if (line.contains("{corner2_coords}") && arena.getCorner2() != null) {
                    return line.replace("{corner2_coords}", String.format("§7X: %.1f, Y: %.1f, Z: %.1f", 
                            arena.getCorner2().getX(), arena.getCorner2().getY(), arena.getCorner2().getZ()));
                }
                break;
        }
        return line;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (title.equals(arenaListConfig.getString("title", "§6Arena Manager"))) {
            event.setCancelled(true);
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String arenaName = extractArenaName(clicked);
            if (arenaName != null) {
                openArenaEditorGui(player, arenaName);
            }
        } else if (title.startsWith(arenaEditorConfig.getString("title", "§6Arena Editor").split(" - ")[0])) {
            event.setCancelled(true);
            
            String arenaName = extractArenaNameFromTitle(title);
            if (arenaName == null) return;
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String buttonType = identifyButton(event.getSlot());
            if (buttonType != null) {
                handleEditorButtonClick(player, arenaName, buttonType);
            }
        }
    }
    
    private String extractArenaName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            // Extract arena name from item display name or lore
            String displayName = item.getItemMeta().getDisplayName();
            // Simple extraction - you might want to improve this
            for (Arena arena : plugin.getArenaManager().getArenas().values()) {
                if (displayName.contains(arena.getName())) {
                    return arena.getName();
                }
            }
        }
        return null;
    }
    
    private String extractArenaNameFromTitle(String title) {
        String prefix = arenaEditorConfig.getString("title", "§6Arena Editor").split(" - ")[0];
        if (title.startsWith(prefix + " - ")) {
            return title.substring((prefix + " - ").length());
        }
        return null;
    }
    
    private String identifyButton(int slot) {
        ConfigurationSection buttons = arenaEditorConfig.getConfigurationSection("buttons");
        if (buttons != null) {
            for (String buttonKey : buttons.getKeys(false)) {
                if (buttons.getConfigurationSection(buttonKey).getInt("slot") == slot) {
                    return buttonKey;
                }
            }
        }
        return null;
    }
    
    private void handleEditorButtonClick(Player player, String arenaName, String buttonType) {
        if ("generate_schematic".equals(buttonType)) {
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            if (arena == null) {
                player.sendMessage("§cArena not found!");
                return;
            }
            
            if (arena.getCorner1() == null || arena.getCorner2() == null) {
                player.sendMessage("§cBoth corners must be set before generating schematic!");
                return;
            }
            
            boolean success = plugin.getArenaManager().saveSchematic(arena);
            if (success) {
                player.sendMessage("§aSchematic generated successfully for arena: " + arenaName);
            } else {
                player.sendMessage("§cFailed to generate schematic for arena: " + arenaName);
            }
            player.closeInventory();
        } else if ("allowed_kits".equals(buttonType)) {
            // Open the allowed kits GUI
            plugin.getAllowedKitsGui().openAllowedKitsGui(player, arenaName);
        } else {
            // Set up location setting
            pendingLocationSets.put(player.getUniqueId(), buttonType);
            pendingArenas.put(player.getUniqueId(), arenaName);
            
            player.closeInventory();
            player.sendMessage("§aShift + Left Click to set " + buttonType + " for arena: " + arenaName);
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (!pendingLocationSets.containsKey(player.getUniqueId())) return;
        
        if (event.getAction() == Action.LEFT_CLICK_AIR && player.isSneaking()) {
            event.setCancelled(true);
            
            String buttonType = pendingLocationSets.remove(player.getUniqueId());
            String arenaName = pendingArenas.remove(player.getUniqueId());
            
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            if (arena == null) {
                player.sendMessage("§cArena not found!");
                return;
            }
            
            Location location = player.getLocation();
            String coordsInfo = String.format("§7X: %.1f, Y: %.1f, Z: %.1f", location.getX(), location.getY(), location.getZ());
            
            switch (buttonType) {
                case "spawn1":
                    arena.setSpawn1(location);
                    player.sendMessage("§aSpawn 1 set for arena: " + arenaName);
                    player.sendMessage(coordsInfo);
                    break;
                case "spawn2":
                    arena.setSpawn2(location);
                    player.sendMessage("§aSpawn 2 set for arena: " + arenaName);
                    player.sendMessage(coordsInfo);
                    break;
                case "center":
                    arena.setCenter(location);
                    player.sendMessage("§aCenter set for arena: " + arenaName);
                    player.sendMessage(coordsInfo);
                    break;
                case "corner1":
                    arena.setCorner1(location);
                    player.sendMessage("§aCorner 1 set for arena: " + arenaName);
                    player.sendMessage(coordsInfo);
                    break;
                case "corner2":
                    arena.setCorner2(location);
                    player.sendMessage("§aCorner 2 set for arena: " + arenaName);
                    player.sendMessage(coordsInfo);
                    break;
            }
            
            plugin.getArenaManager().saveArena(arena);
            
            // Reopen the arena editor GUI
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                openArenaEditorGui(player, arenaName);
            }, 1L);
        }
    }
}
