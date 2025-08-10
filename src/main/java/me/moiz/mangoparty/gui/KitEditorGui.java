package me.moiz.mangoparty.gui;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Kit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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

public class KitEditorGui implements Listener {
    private MangoParty plugin;
    private YamlConfiguration kitListConfig;
    private YamlConfiguration kitEditorConfig;
    private Map<UUID, String> pendingGuiActions; // player -> "add" or "remove"
    private Map<UUID, String> pendingKitNames; // player -> kit name
    private Map<UUID, String> pendingSlotEdits; // player -> gui type for slot editing
    
    public KitEditorGui(MangoParty plugin) {
        this.plugin = plugin;
        this.pendingGuiActions = new HashMap<>();
        this.pendingKitNames = new HashMap<>();
        this.pendingSlotEdits = new HashMap<>();
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
    
    public void reloadConfigs() {
        loadConfigs();
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
        
        String title = kitEditorConfig.getString("title", "§6Kit Editor").replace("{kit_name}", kitName);
        int size = kitEditorConfig.getInt("size", 27);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection buttons = kitEditorConfig.getConfigurationSection("buttons");
        if (buttons != null) {
            for (String buttonKey : buttons.getKeys(false)) {
                ConfigurationSection buttonConfig = buttons.getConfigurationSection(buttonKey);
                ItemStack item = createEditorButton(buttonConfig, buttonKey, kit);
                gui.setItem(buttonConfig.getInt("slot"), item);
            }
        }
        
        player.openInventory(gui);
    }
    
    private ItemStack createEditorButton(ConfigurationSection config, String buttonKey, Kit kit) {
        Material material = Material.valueOf(config.getString("material"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String name = config.getString("name");
        meta.setDisplayName(name);
        
        List<String> lore = new ArrayList<>(config.getStringList("lore"));
        
        // Add current slot information for slot editor
        if ("edit_slots".equals(buttonKey)) {
            lore.add("");
            lore.add("§7Current Slots:");
            
            // Get current slots from each GUI config
            Map<String, Integer> currentSlots = getCurrentSlots(kit.getName());
            for (Map.Entry<String, Integer> entry : currentSlots.entrySet()) {
                String guiType = entry.getKey();
                Integer slot = entry.getValue();
                if (slot != null) {
                    lore.add("§e" + guiType.toUpperCase() + ": §a" + slot);
                } else {
                    lore.add("§e" + guiType.toUpperCase() + ": §cNot set");
                }
            }
        }
        
        meta.setLore(lore);
        
        int customModelData = config.getInt("customModelData");
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private Map<String, Integer> getCurrentSlots(String kitName) {
        Map<String, Integer> slots = new HashMap<>();
        String[] guiTypes = {"split", "ffa", "1v1", "2v2", "3v3"};
        
        for (String guiType : guiTypes) {
            Integer slot = plugin.getConfigManager().getKitSlotInGui(kitName, guiType);
            slots.put(guiType, slot);
        }
        
        return slots;
    }
    
    public void openSlotEditorGui(Player player, String kitName) {
        String title = "§6Edit GUI Slots - " + kitName;
        int size = 27;
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // Create buttons for each GUI type
        String[] guiTypes = {"split", "ffa", "1v1", "2v2", "3v3"};
        Material[] materials = {Material.DIAMOND_SWORD, Material.IRON_SWORD, Material.WOODEN_SWORD, Material.STONE_SWORD, Material.GOLDEN_SWORD};
        int[] slots = {10, 11, 12, 14, 15};
        
        for (int i = 0; i < guiTypes.length; i++) {
            ItemStack item = new ItemStack(materials[i]);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + guiTypes[i].toUpperCase() + " GUI");
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to edit slot position");
            
            Integer currentSlot = plugin.getConfigManager().getKitSlotInGui(kitName, guiTypes[i]);
            if (currentSlot != null) {
                lore.add("§7Current slot: §a" + currentSlot);
            } else {
                lore.add("§7Current slot: §cNot set");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(slots[i], item);
        }
        
        player.openInventory(gui);
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
        } else if (title.startsWith(kitEditorConfig.getString("title", "§6Kit Editor").split(" - ")[0])) {
            event.setCancelled(true);
            
            String kitName = extractKitNameFromTitle(title);
            if (kitName == null) return;
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String buttonType = identifyButton(event.getSlot());
            if (buttonType != null) {
                handleEditorButtonClick(player, kitName, buttonType, event.getClick());
            }
        } else if (title.startsWith("§6Edit GUI Slots - ")) {
            event.setCancelled(true);
            
            String kitName = title.substring("§6Edit GUI Slots - ".length());
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String guiType = identifyGuiTypeFromSlotEditor(event.getSlot());
            if (guiType != null) {
                handleSlotEditorClick(player, kitName, guiType);
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
        String prefix = kitEditorConfig.getString("title", "§6Kit Editor").split(" - ")[0];
        if (title.startsWith(prefix + " - ")) {
            return title.substring((prefix + " - ").length());
        }
        return null;
    }
    
    private String identifyButton(int slot) {
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
    
    private String identifyGuiTypeFromSlotEditor(int slot) {
        switch (slot) {
            case 10: return "split";
            case 11: return "ffa";
            case 12: return "1v1";
            case 14: return "2v2";
            case 15: return "3v3";
            default: return null;
        }
    }
    
    private void handleEditorButtonClick(Player player, String kitName, String buttonType, ClickType clickType) {
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            player.sendMessage("§cKit not found!");
            return;
        }
        
        if ("change_icon".equals(buttonType)) {
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (heldItem == null || heldItem.getType() == Material.AIR) {
                player.sendMessage("§cPlease hold an item to set as the kit icon!");
                return;
            }
            
            // Update kit icon
            kit.setIcon(heldItem.clone());
            plugin.getKitManager().saveKit(kit);
            
            // Update all GUI configs with new icon
            plugin.getConfigManager().updateKitIconInAllGuis(kit);
            plugin.getGuiManager().reloadGuiConfigs();
            
            player.sendMessage("§aKit icon updated for '" + kitName + "'!");
            
            // Reopen GUI after 1 tick
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                openKitEditorGui(player, kitName);
            }, 1L);
            
        } else if ("manage_gui_placement".equals(buttonType)) {
            if (clickType == ClickType.LEFT) {
                // Add to GUI
                pendingGuiActions.put(player.getUniqueId(), "add");
                pendingKitNames.put(player.getUniqueId(), kitName);
                
                player.closeInventory();
                player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                player.sendMessage("§6ADD TO GUI");
                player.sendMessage("§7Kit: §e" + kitName);
                player.sendMessage("");
                player.sendMessage("§7Please type the GUI type:");
                player.sendMessage("§a• split §7- Split GUI");
                player.sendMessage("§a• ffa §7- FFA GUI");
                player.sendMessage("§a• 1v1 §7- 1v1 Queue GUI");
                player.sendMessage("§a• 2v2 §7- 2v2 Queue GUI");
                player.sendMessage("§a• 3v3 §7- 3v3 Queue GUI");
                player.sendMessage("§7Or type 'cancel' to cancel");
                player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                
            } else if (clickType == ClickType.RIGHT) {
                // Remove from GUI
                pendingGuiActions.put(player.getUniqueId(), "remove");
                pendingKitNames.put(player.getUniqueId(), kitName);
                
                player.closeInventory();
                player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                player.sendMessage("§6REMOVE FROM GUI");
                player.sendMessage("§7Kit: §e" + kitName);
                player.sendMessage("");
                player.sendMessage("§7Please type the GUI type:");
                player.sendMessage("§c• split §7- Split GUI");
                player.sendMessage("§c• ffa §7- FFA GUI");
                player.sendMessage("§c• 1v1 §7- 1v1 Queue GUI");
                player.sendMessage("§c• 2v2 §7- 2v2 Queue GUI");
                player.sendMessage("§c• 3v3 §7- 3v3 Queue GUI");
                player.sendMessage("§7Or type 'cancel' to cancel");
                player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            }
            
            // Schedule expiration
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (pendingGuiActions.containsKey(player.getUniqueId())) {
                        pendingGuiActions.remove(player.getUniqueId());
                        pendingKitNames.remove(player.getUniqueId());
                        if (player.isOnline()) {
                            player.sendMessage("§cGUI action expired.");
                        }
                    }
                }
            }.runTaskLater(plugin, 600L); // 30 seconds
            
        } else if ("edit_slots".equals(buttonType)) {
            openSlotEditorGui(player, kitName);
        }
    }
    
    private void handleSlotEditorClick(Player player, String kitName, String guiType) {
        pendingSlotEdits.put(player.getUniqueId(), guiType);
        pendingKitNames.put(player.getUniqueId(), kitName);
        
        player.closeInventory();
        
        Integer currentSlot = plugin.getConfigManager().getKitSlotInGui(kitName, guiType);
        String currentSlotText = currentSlot != null ? String.valueOf(currentSlot) : "Not set";
        
        player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§6SLOT EDITOR");
        player.sendMessage("§7Kit: §e" + kitName);
        player.sendMessage("§7GUI: §e" + guiType.toUpperCase());
        player.sendMessage("§7Current slot: §a" + currentSlotText);
        player.sendMessage("");
        player.sendMessage("§7Please type the new slot number (0-26):");
        player.sendMessage("§7Or type 'cancel' to cancel");
        player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Schedule expiration
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingSlotEdits.containsKey(player.getUniqueId())) {
                    pendingSlotEdits.remove(player.getUniqueId());
                    pendingKitNames.remove(player.getUniqueId());
                    if (player.isOnline()) {
                        player.sendMessage("§cSlot editing expired.");
                    }
                }
            }
        }.runTaskLater(plugin, 600L); // 30 seconds
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Handle GUI placement actions
        if (pendingGuiActions.containsKey(playerId)) {
            event.setCancelled(true);
            
            String action = pendingGuiActions.remove(playerId);
            String kitName = pendingKitNames.remove(playerId);
            String input = event.getMessage().trim().toLowerCase();
            
            if (input.equals("cancel")) {
                player.sendMessage("§cGUI action cancelled.");
                // Reopen GUI after 1 tick
                Bukkit.getScheduler().runTask(plugin, () -> {
                    openKitEditorGui(player, kitName);
                });
                return;
            }
            
            if (!input.equals("split") && !input.equals("ffa") && !input.equals("1v1") && !input.equals("2v2") && !input.equals("3v3")) {
                player.sendMessage("§cInvalid GUI type! Please use: split, ffa, 1v1, 2v2, or 3v3");
                // Reopen GUI after 1 tick
                Bukkit.getScheduler().runTask(plugin, () -> {
                    openKitEditorGui(player, kitName);
                });
                return;
            }
            
            Kit kit = plugin.getKitManager().getKit(kitName);
            if (kit == null) {
                player.sendMessage("§cKit not found!");
                return;
            }
            
            if ("add".equals(action)) {
                boolean success;
                if (input.equals("1v1") || input.equals("2v2") || input.equals("3v3")) {
                    success = plugin.getConfigManager().addKitToQueueGuiConfig(kit, input, null);
                } else {
                    success = plugin.getConfigManager().addKitToGuiConfig(kit, input, null);
                }
                
                if (success) {
                    player.sendMessage("§aKit '" + kitName + "' added to " + input.toUpperCase() + " GUI!");
                    plugin.getGuiManager().reloadGuiConfigs();
                } else {
                    player.sendMessage("§cFailed to add kit to GUI. It might already be there.");
                }
            } else if ("remove".equals(action)) {
                boolean success;
                if (input.equals("1v1") || input.equals("2v2") || input.equals("3v3")) {
                    success = plugin.getConfigManager().removeKitFromQueueGuiConfig(kitName, input);
                } else {
                    success = plugin.getConfigManager().removeKitFromGuiConfig(kitName, input);
                }
                
                if (success) {
                    player.sendMessage("§aKit '" + kitName + "' removed from " + input.toUpperCase() + " GUI!");
                    plugin.getGuiManager().reloadGuiConfigs();
                } else {
                    player.sendMessage("§cFailed to remove kit from GUI. It might not be there.");
                }
            }
            
            // Reopen GUI after 1 tick
            Bukkit.getScheduler().runTask(plugin, () -> {
                openKitEditorGui(player, kitName);
            });
            return;
        }
        
        // Handle slot editing
        if (pendingSlotEdits.containsKey(playerId)) {
            event.setCancelled(true);
            
            String guiType = pendingSlotEdits.remove(playerId);
            String kitName = pendingKitNames.remove(playerId);
            String input = event.getMessage().trim();
            
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage("§cSlot editing cancelled.");
                // Reopen slot editor after 1 tick
                Bukkit.getScheduler().runTask(plugin, () -> {
                    openSlotEditorGui(player, kitName);
                });
                return;
            }
            
            try {
                int slot = Integer.parseInt(input);
                if (slot < 0 || slot > 26) {
                    player.sendMessage("§cSlot must be between 0 and 26!");
                    // Reopen slot editor after 1 tick
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        openSlotEditorGui(player, kitName);
                    });
                    return;
                }
                
                // Check if slot is already taken by another kit
                String existingKit = plugin.getConfigManager().getKitAtSlot(guiType, slot);
                if (existingKit != null && !existingKit.equals(kitName)) {
                    player.sendMessage("§cSlot " + slot + " is already taken by kit '" + existingKit + "'!");
                    // Reopen slot editor after 1 tick
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        openSlotEditorGui(player, kitName);
                    });
                    return;
                }
                
                // Update slot
                boolean success;
                if (guiType.equals("1v1") || guiType.equals("2v2") || guiType.equals("3v3")) {
                    success = plugin.getConfigManager().updateKitSlotInQueueGui(kitName, guiType, slot);
                } else {
                    success = plugin.getConfigManager().updateKitSlotInGui(kitName, guiType, slot);
                }
                
                if (success) {
                    player.sendMessage("§aSlot updated! Kit '" + kitName + "' is now at slot " + slot + " in " + guiType.toUpperCase() + " GUI.");
                    plugin.getGuiManager().reloadGuiConfigs();
                } else {
                    player.sendMessage("§cFailed to update slot!");
                }
                
                // Reopen slot editor after 1 tick
                Bukkit.getScheduler().runTask(plugin, () -> {
                    openSlotEditorGui(player, kitName);
                });
                
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number! Please enter a valid slot number (0-26).");
                // Reopen slot editor after 1 tick
                Bukkit.getScheduler().runTask(plugin, () -> {
                    openSlotEditorGui(player, kitName);
                });
            }
        }
    }
}
