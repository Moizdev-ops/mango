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
import java.util.Arrays;
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
        
        // Force regenerate kit_editor.yml to include new buttons
        if (kitEditorFile.exists()) {
            kitEditorFile.delete();
        }
        plugin.saveResource("gui/kit_editor.yml", false);
        
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
                if (buttonKey.equals("edit_icon")) {
                    ItemStack item = createIconEditButton(buttonConfig, kit);
                    gui.setItem(buttonConfig.getInt("slot"), item);
                } else if (buttonKey.equals("add_to_gui")) {
                    ItemStack item = createAddToGuiButton(buttonConfig, kit);
                    gui.setItem(buttonConfig.getInt("slot"), item);
                } else if (buttonKey.equals("edit_slots")) {
                    ItemStack item = createEditSlotsButton(buttonConfig, kit);
                    gui.setItem(buttonConfig.getInt("slot"), item);
                } else {
                    ItemStack item = createRuleButton(buttonConfig, buttonKey, kit.getRules());
                    gui.setItem(buttonConfig.getInt("slot"), item);
                }
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

    private ItemStack createIconEditButton(ConfigurationSection config, Kit kit) {
        Material material = Material.valueOf(config.getString("material"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(config.getString("name"));
        
        List<String> lore = new ArrayList<>(config.getStringList("lore"));
        lore.add("§7Current icon: §f" + (kit.getIcon() != null ? kit.getIcon().getType().toString() : "None"));
        meta.setLore(lore);
        
        int customModelData = config.getInt("customModelData");
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAddToGuiButton(ConfigurationSection config, Kit kit) {
        Material material = Material.valueOf(config.getString("material"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(config.getString("name"));
        List<String> lore = new ArrayList<>(config.getStringList("lore"));
        lore.add("§a§lLeft Click: §7Add to GUI");
        lore.add("§c§lRight Click: §7Remove from GUI");
        meta.setLore(lore);
        
        int customModelData = config.getInt("customModelData");
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEditSlotsButton(ConfigurationSection config, Kit kit) {
        Material material = Material.valueOf(config.getString("material"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(config.getString("name"));
        meta.setLore(config.getStringList("lore"));
        
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

    private void openAddToGuiMenu(Player player, Kit kit) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6Add " + kit.getName() + " to GUI");
        
        // Split GUI option
        ItemStack splitItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta splitMeta = splitItem.getItemMeta();
        splitMeta.setDisplayName("§aAdd to Split GUI");
        splitMeta.setLore(Arrays.asList("§7Add this kit to party split matches"));
        splitItem.setItemMeta(splitMeta);
        gui.setItem(10, splitItem);
        
        // FFA GUI option
        ItemStack ffaItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta ffaMeta = ffaItem.getItemMeta();
        ffaMeta.setDisplayName("§cAdd to FFA GUI");
        ffaMeta.setLore(Arrays.asList("§7Add this kit to party FFA matches"));
        ffaItem.setItemMeta(ffaMeta);
        gui.setItem(12, ffaItem);
        
        // 1v1 Queue option
        ItemStack queue1v1Item = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta queue1v1Meta = queue1v1Item.getItemMeta();
        queue1v1Meta.setDisplayName("§6Add to 1v1 Queue");
        queue1v1Meta.setLore(Arrays.asList("§7Add this kit to 1v1 ranked queue"));
        queue1v1Item.setItemMeta(queue1v1Meta);
        gui.setItem(14, queue1v1Item);
        
        // 2v2 Queue option
        ItemStack queue2v2Item = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta queue2v2Meta = queue2v2Item.getItemMeta();
        queue2v2Meta.setDisplayName("§6Add to 2v2 Queue");
        queue2v2Meta.setLore(Arrays.asList("§7Add this kit to 2v2 ranked queue"));
        queue2v2Item.setItemMeta(queue2v2Meta);
        gui.setItem(16, queue2v2Item);
        
        // 3v3 Queue option
        ItemStack queue3v3Item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta queue3v3Meta = queue3v3Item.getItemMeta();
        queue3v3Meta.setDisplayName("§6Add to 3v3 Queue");
        queue3v3Meta.setLore(Arrays.asList("§7Add this kit to 3v3 ranked queue"));
        queue3v3Item.setItemMeta(queue3v3Meta);
        gui.setItem(18, queue3v3Item);
        
        player.openInventory(gui);
    }

    private void openRemoveFromGuiMenu(Player player, Kit kit) {
        Inventory gui = Bukkit.createInventory(null, 27, "§cRemove " + kit.getName() + " from GUI");
        
        // Split GUI option
        ItemStack splitItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta splitMeta = splitItem.getItemMeta();
        splitMeta.setDisplayName("§cRemove from Split GUI");
        splitMeta.setLore(Arrays.asList("§7Remove this kit from party split matches"));
        splitItem.setItemMeta(splitMeta);
        gui.setItem(10, splitItem);
        
        // FFA GUI option
        ItemStack ffaItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta ffaMeta = ffaItem.getItemMeta();
        ffaMeta.setDisplayName("§cRemove from FFA GUI");
        ffaMeta.setLore(Arrays.asList("§7Remove this kit from party FFA matches"));
        ffaItem.setItemMeta(ffaMeta);
        gui.setItem(12, ffaItem);
        
        // 1v1 Queue option
        ItemStack queue1v1Item = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta queue1v1Meta = queue1v1Item.getItemMeta();
        queue1v1Meta.setDisplayName("§cRemove from 1v1 Queue");
        queue1v1Meta.setLore(Arrays.asList("§7Remove this kit from 1v1 ranked queue"));
        queue1v1Item.setItemMeta(queue1v1Meta);
        gui.setItem(14, queue1v1Item);
        
        // 2v2 Queue option
        ItemStack queue2v2Item = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta queue2v2Meta = queue2v2Item.getItemMeta();
        queue2v2Meta.setDisplayName("§cRemove from 2v2 Queue");
        queue2v2Meta.setLore(Arrays.asList("§7Remove this kit from 2v2 ranked queue"));
        queue2v2Item.setItemMeta(queue2v2Meta);
        gui.setItem(16, queue2v2Item);
        
        // 3v3 Queue option
        ItemStack queue3v3Item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta queue3v3Meta = queue3v3Item.getItemMeta();
        queue3v3Meta.setDisplayName("§cRemove from 3v3 Queue");
        queue3v3Meta.setLore(Arrays.asList("§7Remove this kit from 3v3 ranked queue"));
        queue3v3Item.setItemMeta(queue3v3Meta);
        gui.setItem(18, queue3v3Item);
        
        player.openInventory(gui);
    }

    private void openSlotEditorMenu(Player player, Kit kit) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6Edit Slots for " + kit.getName());
        
        // Split GUI slot editor
        ItemStack splitItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta splitMeta = splitItem.getItemMeta();
        splitMeta.setDisplayName("§aEdit Split GUI Slot");
        List<String> splitLore = new ArrayList<>();
        splitLore.add("§7Current slot: §f" + plugin.getConfigManager().getKitSlotInGui(kit, "split"));
        splitLore.add("§7Click to change slot position");
        splitMeta.setLore(splitLore);
        splitItem.setItemMeta(splitMeta);
        gui.setItem(10, splitItem);
        
        // FFA GUI slot editor
        ItemStack ffaItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta ffaMeta = ffaItem.getItemMeta();
        ffaMeta.setDisplayName("§cEdit FFA GUI Slot");
        List<String> ffaLore = new ArrayList<>();
        ffaLore.add("§7Current slot: §f" + plugin.getConfigManager().getKitSlotInGui(kit, "ffa"));
        ffaLore.add("§7Click to change slot position");
        ffaMeta.setLore(ffaLore);
        ffaItem.setItemMeta(ffaMeta);
        gui.setItem(12, ffaItem);
        
        // Queue slots
        String[] queueModes = {"1v1", "2v2", "3v3"};
        Material[] queueMaterials = {Material.GOLDEN_SWORD, Material.GOLDEN_AXE, Material.NETHERITE_SWORD};
        int[] queueSlots = {14, 16, 18};
        
        for (int i = 0; i < queueModes.length; i++) {
            ItemStack queueItem = new ItemStack(queueMaterials[i]);
            ItemMeta queueMeta = queueItem.getItemMeta();
            queueMeta.setDisplayName("§6Edit " + queueModes[i].toUpperCase() + " Queue Slot");
            List<String> queueLore = new ArrayList<>();
            queueLore.add("§7Current slot: §f" + plugin.getConfigManager().getKitSlotInQueueGui(kit, queueModes[i]));
            queueLore.add("§7Click to change slot position");
            queueMeta.setLore(queueLore);
            queueItem.setItemMeta(queueMeta);
            gui.setItem(queueSlots[i], queueItem);
        }
        
        // Add slot selection grid (slots 27-53 for visual slot selection)
        for (int slot = 0; slot < 27; slot++) {
            ItemStack slotItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta slotMeta = slotItem.getItemMeta();
            slotMeta.setDisplayName("§7Slot " + slot);
            slotMeta.setLore(Arrays.asList("§7Click to select this slot"));
            slotItem.setItemMeta(slotMeta);
            gui.setItem(slot + 27, slotItem);
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
        } else if (title.startsWith(kitEditorConfig.getString("title", "§6Kit Rules").split(" - ")[0])) {
            event.setCancelled(true);
            
            String kitName = extractKitNameFromTitle(title);
            if (kitName == null) return;
            
            Kit kit = plugin.getKitManager().getKit(kitName);
            if (kit == null) return;
            
            String ruleKey = identifyRuleButton(event.getSlot());
            if (ruleKey != null) {
                if (ruleKey.equals("edit_icon")) {
                    // Set kit icon to player's main hand item
                    ItemStack heldItem = player.getInventory().getItemInMainHand();
                    if (heldItem != null && heldItem.getType() != Material.AIR) {
                        ItemStack iconItem = heldItem.clone();
                        iconItem.setAmount(1);
                        kit.setIcon(iconItem);
                        plugin.getKitManager().saveKit(kit);
                        
                        // Update icon in ALL GUI configs
                        plugin.getConfigManager().updateKitIconInAllGuis(kit);
                        
                        // Refresh the GUI
                        openKitEditorGui(player, kitName);
                        
                        player.sendMessage("§aKit icon updated to " + iconItem.getType().toString() + " for kit: " + kitName);
                        player.sendMessage("§aIcon updated in all GUIs!");
                    } else {
                        player.sendMessage("§cHold an item in your main hand to set it as the kit icon!");
                    }
                    return;
                }
                if (ruleKey.equals("add_to_gui")) {
                    if (event.getClick().isLeftClick()) {
                        openAddToGuiMenu(player, kit);
                    } else if (event.getClick().isRightClick()) {
                        openRemoveFromGuiMenu(player, kit);
                    }
                    return;
                }
                if (ruleKey.equals("edit_slots")) {
                    openSlotEditorMenu(player, kit);
                    return;
                }
                toggleRule(ruleKey, kit.getRules());
                plugin.getKitManager().saveKit(kit);
                
                // Refresh the GUI
                openKitEditorGui(player, kitName);
                
                player.sendMessage("§aToggled " + ruleKey.replace("_", " ") + " for kit: " + kitName);
            }
        } else if (title.startsWith("§6Add ") && title.endsWith(" to GUI")) {
            event.setCancelled(true);
            
            String kitName = title.replace("§6Add ", "").replace(" to GUI", "");
            Kit kit = plugin.getKitManager().getKit(kitName);
            if (kit == null) return;
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String guiType = "";
            if (clicked.getType() == Material.IRON_SWORD) {
                guiType = "split";
            } else if (clicked.getType() == Material.DIAMOND_SWORD) {
                guiType = "ffa";
            } else if (clicked.getType() == Material.GOLDEN_SWORD) {
                guiType = "1v1";
            } else if (clicked.getType() == Material.GOLDEN_AXE) {
                guiType = "2v2";
            } else if (clicked.getType() == Material.NETHERITE_SWORD) {
                guiType = "3v3";
            }
            
            if (!guiType.isEmpty()) {
                boolean success;
                if (guiType.equals("split") || guiType.equals("ffa")) {
                    success = plugin.getConfigManager().addKitToGuiConfig(kit, guiType, null);
                } else {
                    success = plugin.getConfigManager().addKitToQueueGuiConfig(kit, guiType, null);
                }
                
                if (success) {
                    player.sendMessage("§aKit '" + kitName + "' added to " + guiType.toUpperCase() + " GUI!");
                    plugin.getGuiManager().reloadGuiConfigs();
                } else {
                    player.sendMessage("§cFailed to add kit to " + guiType.toUpperCase() + " GUI. It might already be there.");
                }
                
                player.closeInventory();
            }
        } else if (title.startsWith("§cRemove ") && title.endsWith(" from GUI")) {
            event.setCancelled(true);
            
            String kitName = title.replace("§cRemove ", "").replace(" from GUI", "");
            Kit kit = plugin.getKitManager().getKit(kitName);
            if (kit == null) return;
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String guiType = "";
            if (clicked.getType() == Material.IRON_SWORD) {
                guiType = "split";
            } else if (clicked.getType() == Material.DIAMOND_SWORD) {
                guiType = "ffa";
            } else if (clicked.getType() == Material.GOLDEN_SWORD) {
                guiType = "1v1";
            } else if (clicked.getType() == Material.GOLDEN_AXE) {
                guiType = "2v2";
            } else if (clicked.getType() == Material.NETHERITE_SWORD) {
                guiType = "3v3";
            }
            
            if (!guiType.isEmpty()) {
                boolean success;
                if (guiType.equals("split") || guiType.equals("ffa")) {
                    success = plugin.getConfigManager().removeKitFromGuiConfig(kit, guiType);
                } else {
                    success = plugin.getConfigManager().removeKitFromQueueGuiConfig(kit, guiType);
                }
                
                if (success) {
                    player.sendMessage("§aKit '" + kitName + "' removed from " + guiType.toUpperCase() + " GUI!");
                    plugin.getGuiManager().reloadGuiConfigs();
                } else {
                    player.sendMessage("§cFailed to remove kit from " + guiType.toUpperCase() + " GUI. It might not be there.");
                }
                
                player.closeInventory();
            }
        } else if (title.startsWith("§6Edit Slots for ")) {
            event.setCancelled(true);
            
            String kitName = title.replace("§6Edit Slots for ", "");
            Kit kit = plugin.getKitManager().getKit(kitName);
            if (kit == null) return;
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            // Handle slot selection (slots 27-53)
            if (event.getSlot() >= 27 && event.getSlot() <= 53) {
                int selectedSlot = event.getSlot() - 27;
                // Store the selected slot for the next GUI type click
                // For now, just show a message
                player.sendMessage("§eSelected slot " + selectedSlot + ". Now click a GUI type to update its slot.");
                return;
            }
            
            // Handle GUI type selection for slot editing
            String guiType = "";
            if (clicked.getType() == Material.IRON_SWORD) {
                guiType = "split";
            } else if (clicked.getType() == Material.DIAMOND_SWORD) {
                guiType = "ffa";
            } else if (clicked.getType() == Material.GOLDEN_SWORD) {
                guiType = "1v1";
            } else if (clicked.getType() == Material.GOLDEN_AXE) {
                guiType = "2v2";
            } else if (clicked.getType() == Material.NETHERITE_SWORD) {
                guiType = "3v3";
            }
            
            if (!guiType.isEmpty()) {
                player.sendMessage("§eClick a slot number (bottom row) to set the position for " + guiType.toUpperCase() + " GUI");
                // You could implement a more sophisticated slot selection system here
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
