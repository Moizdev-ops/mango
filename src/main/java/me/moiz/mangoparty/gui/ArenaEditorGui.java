package me.moiz.mangoparty.gui;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.managers.ArenaManager;
import me.moiz.mangoparty.managers.KitManager;
import me.moiz.mangoparty.models.Arena;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArenaEditorGui implements Listener {
    private MangoParty plugin;
    private ArenaManager arenaManager;
    private KitManager kitManager;
    private Map<UUID, String> editingArena;
    private Map<UUID, String> waitingForInput;
    private Map<UUID, String> inputType;
    private Map<UUID, Long> inputTimeout;

    public ArenaEditorGui(MangoParty plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
        this.kitManager = plugin.getKitManager();
        this.editingArena = new HashMap<>();
        this.waitingForInput = new HashMap<>();
        this.inputType = new HashMap<>();
        this.inputTimeout = new HashMap<>();
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openArenaListGui(Player player) {
        String title = HexUtils.colorize("&6Arena Manager");
        int size = 54;
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        Map<String, Arena> arenas = arenaManager.getArenas();
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
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(HexUtils.colorize("&e" + arena.getName()));
        
        List<String> lore = new ArrayList<>();
        lore.add(HexUtils.colorize("&7Status: " + (arena.isComplete() ? "&aComplete" : "&cIncomplete")));
        lore.add(HexUtils.colorize("&7Center: &f" + formatLocation(arena.getCenter())));
        lore.add(HexUtils.colorize("&7Spawn 1: &f" + formatLocation(arena.getSpawn1())));
        lore.add(HexUtils.colorize("&7Spawn 2: &f" + formatLocation(arena.getSpawn2())));
        lore.add(HexUtils.colorize("&7Corner 1: &f" + formatLocation(arena.getCorner1())));
        lore.add(HexUtils.colorize("&7Corner 2: &f" + formatLocation(arena.getCorner2())));
        lore.add("");
        lore.add(HexUtils.colorize("&eClick to edit"));
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }

    public void openArenaEditor(Player player, String arenaName) {
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            player.sendMessage(HexUtils.colorize("&cArena not found!"));
            return;
        }

        editingArena.put(player.getUniqueId(), arenaName);

        File configFile = new File(plugin.getDataFolder(), "gui/arena_editor.yml");
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        String title = HexUtils.colorize(config.getString("title", "&6Arena Editor"));
        title = title.replace("{arena}", arena.getName());
        int size = config.getInt("size", 27);
        
        Inventory gui = Bukkit.createInventory(null, size, title);

        // Load items from config
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                ItemStack item = createItemFromConfig(itemSection, arena);
                if (item != null) {
                    int slot = itemSection.getInt("slot", 0);
                    gui.setItem(slot, item);
                }
            }
        }

        player.openInventory(gui);
    }

    private void createDefaultConfig(File configFile) {
        YamlConfiguration config = new YamlConfiguration();
        
        config.set("title", "&6Arena Editor - {arena}");
        config.set("size", 27);
        
        // Set Center
        config.set("items.set_center.slot", 10);
        config.set("items.set_center.material", "COMPASS");
        config.set("items.set_center.name", "&eSet Center");
        config.set("items.set_center.lore", List.of(
            "&7Click to set arena center",
            "&7This is the middle point of the arena",
            "",
            "&eCurrent Location:",
            "&f{center_detailed}",
            "",
            "&7World: &f{center_world}"
        ));
        config.set("items.set_center.action", "SET_CENTER");
        
        // Set Spawn 1
        config.set("items.set_spawn1.slot", 11);
        config.set("items.set_spawn1.material", "RED_BED");
        config.set("items.set_spawn1.name", "&cSet Spawn 1");
        config.set("items.set_spawn1.lore", List.of(
            "&7Click to set spawn point 1",
            "&7This is where team 1 will spawn",
            "",
            "&cCurrent Location:",
            "&f{spawn1_detailed}",
            "",
            "&7World: &f{spawn1_world}",
            "&7Pitch: &f{spawn1_pitch}°",
            "&7Yaw: &f{spawn1_yaw}°"
        ));
        config.set("items.set_spawn1.action", "SET_SPAWN1");
        
        // Set Spawn 2
        config.set("items.set_spawn2.slot", 12);
        config.set("items.set_spawn2.material", "BLUE_BED");
        config.set("items.set_spawn2.name", "&9Set Spawn 2");
        config.set("items.set_spawn2.lore", List.of(
            "&7Click to set spawn point 2",
            "&7This is where team 2 will spawn",
            "",
            "&9Current Location:",
            "&f{spawn2_detailed}",
            "",
            "&7World: &f{spawn2_world}",
            "&7Pitch: &f{spawn2_pitch}°",
            "&7Yaw: &f{spawn2_yaw}°"
        ));
        config.set("items.set_spawn2.action", "SET_SPAWN2");
        
        // Set Corner 1
        config.set("items.set_corner1.slot", 13);
        config.set("items.set_corner1.material", "STONE");
        config.set("items.set_corner1.name", "&7Set Corner 1");
        config.set("items.set_corner1.lore", List.of(
            "&7Click to set corner 1",
            "&7This defines the arena boundary",
            "",
            "&7Current Location:",
            "&f{corner1_detailed}",
            "",
            "&7World: &f{corner1_world}"
        ));
        config.set("items.set_corner1.action", "SET_CORNER1");
        
        // Set Corner 2
        config.set("items.set_corner2.slot", 14);
        config.set("items.set_corner2.material", "COBBLESTONE");
        config.set("items.set_corner2.name", "&7Set Corner 2");
        config.set("items.set_corner2.lore", List.of(
            "&7Click to set corner 2",
            "&7This defines the arena boundary",
            "",
            "&7Current Location:",
            "&f{corner2_detailed}",
            "",
            "&7World: &f{corner2_world}",
            "",
            "&eArena Dimensions:",
            "&f{arena_dimensions}"
        ));
        config.set("items.set_corner2.action", "SET_CORNER2");
        
        // Save Arena
        config.set("items.save_arena.slot", 15);
        config.set("items.save_arena.material", "EMERALD");
        config.set("items.save_arena.name", "&aSave Arena");
        config.set("items.save_arena.lore", List.of(
            "&7Click to save arena",
            "&7and generate schematic",
            "",
            "&aArena Status: &f{arena_status}",
            "&7Completion: &f{completion_percentage}%",
            "",
            "&eRequired Points:",
            "&7• Center: {center_status}",
            "&7• Spawn 1: {spawn1_status}",
            "&7• Spawn 2: {spawn2_status}",
            "&7• Corner 1: {corner1_status}",
            "&7• Corner 2: {corner2_status}"
        ));
        config.set("items.save_arena.action", "SAVE_ARENA");
        
        // Clone Arena
        config.set("items.clone_arena.slot", 16);
        config.set("items.clone_arena.material", "STRUCTURE_BLOCK");
        config.set("items.clone_arena.name", "&bClone Arena");
        config.set("items.clone_arena.lore", List.of(
            "&7Click to clone this arena",
            "&7at your current location",
            "",
            "&bOriginal Arena:",
            "&7Name: &f{arena}",
            "&7Status: &f{arena_status}",
            "",
            "&eYour Location:",
            "&f{player_location}",
            "",
            "&7The cloned arena will be named:",
            "&e{arena}_clone_1"
        ));
        config.set("items.clone_arena.action", "CLONE_ARENA");
        
        // Manage Allowed Kits
        config.set("items.manage_kits.slot", 19);
        config.set("items.manage_kits.material", "CHEST");
        config.set("items.manage_kits.name", "&6Manage Allowed Kits");
        config.set("items.manage_kits.lore", List.of(
            "&7Left Click: Add kit",
            "&7Right Click: Remove kit",
            "",
            "&eKit Restrictions:",
            "&7{kit_restriction_status}",
            "",
            "&6Allowed Kits ({allowed_kit_count}):",
            "{allowed_kits_detailed}",
            "",
            "&7Total Available Kits: &f{total_kits}"
        ));
        config.set("items.manage_kits.action", "MANAGE_KITS");
        
        // Toggle Regeneration
        config.set("items.toggle_regen.slot", 20);
        config.set("items.toggle_regen.material", "GRASS_BLOCK");
        config.set("items.toggle_regen.name", "&2Toggle Block Regeneration");
        config.set("items.toggle_regen.lore", List.of(
            "&7Click to toggle block regeneration",
            "&7This controls if blocks reset after matches",
            "",
            "&2Current Status: &f{regen_status}",
            "&7Performance Impact: &f{regen_performance}",
            "",
            "&eRegeneration Details:",
            "&7• Broken blocks will {regen_behavior}",
            "&7• Placed blocks will {regen_behavior}",
            "&7• Regeneration occurs {regen_timing}"
        ));
        config.set("items.toggle_regen.action", "TOGGLE_REGEN");
        
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create arena_editor.yml: " + e.getMessage());
        }
    }

    private ItemStack createItemFromConfig(ConfigurationSection section, Arena arena) {
        try {
            Material material = Material.valueOf(section.getString("material", "STONE"));
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                String name = section.getString("name", "");
                name = replacePlaceholders(name, arena);
                meta.setDisplayName(HexUtils.colorize(name));
                
                List<String> lore = section.getStringList("lore");
                List<String> processedLore = new ArrayList<>();
                for (String line : lore) {
                    String processedLine = replacePlaceholders(line, arena);
                    processedLore.add(HexUtils.colorize(processedLine));
                }
                meta.setLore(processedLore);
                
                if (section.contains("customModelData")) {
                    meta.setCustomModelData(section.getInt("customModelData"));
                }
                
                item.setItemMeta(meta);
            }
            
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create item from config: " + e.getMessage());
            return null;
        }
    }

    private String replacePlaceholders(String text, Arena arena) {
        // Basic arena info
        text = text.replace("{arena}", arena.getName());
        text = text.replace("{arena_status}", arena.isComplete() ? "Complete" : "Incomplete");
        
        // Simple location formats
        text = text.replace("{center}", formatLocation(arena.getCenter()));
        text = text.replace("{spawn1}", formatLocation(arena.getSpawn1()));
        text = text.replace("{spawn2}", formatLocation(arena.getSpawn2()));
        text = text.replace("{corner1}", formatLocation(arena.getCorner1()));
        text = text.replace("{corner2}", formatLocation(arena.getCorner2()));
        
        // Detailed location formats with coordinates
        text = text.replace("{center_detailed}", formatDetailedLocation(arena.getCenter()));
        text = text.replace("{spawn1_detailed}", formatDetailedLocation(arena.getSpawn1()));
        text = text.replace("{spawn2_detailed}", formatDetailedLocation(arena.getSpawn2()));
        text = text.replace("{corner1_detailed}", formatDetailedLocation(arena.getCorner1()));
        text = text.replace("{corner2_detailed}", formatDetailedLocation(arena.getCorner2()));
        
        // World names
        text = text.replace("{center_world}", getWorldName(arena.getCenter()));
        text = text.replace("{spawn1_world}", getWorldName(arena.getSpawn1()));
        text = text.replace("{spawn2_world}", getWorldName(arena.getSpawn2()));
        text = text.replace("{corner1_world}", getWorldName(arena.getCorner1()));
        text = text.replace("{corner2_world}", getWorldName(arena.getCorner2()));
        
        // Rotation info for spawns
        text = text.replace("{spawn1_pitch}", formatRotation(arena.getSpawn1(), true));
        text = text.replace("{spawn1_yaw}", formatRotation(arena.getSpawn1(), false));
        text = text.replace("{spawn2_pitch}", formatRotation(arena.getSpawn2(), true));
        text = text.replace("{spawn2_yaw}", formatRotation(arena.getSpawn2(), false));
        
        // Arena dimensions
        text = text.replace("{arena_dimensions}", calculateDimensions(arena));
        
        // Completion status for each point
        text = text.replace("{center_status}", arena.getCenter() != null ? "✓ Set" : "✗ Not Set");
        text = text.replace("{spawn1_status}", arena.getSpawn1() != null ? "✓ Set" : "✗ Not Set");
        text = text.replace("{spawn2_status}", arena.getSpawn2() != null ? "✓ Set" : "✗ Not Set");
        text = text.replace("{corner1_status}", arena.getCorner1() != null ? "✓ Set" : "✗ Not Set");
        text = text.replace("{corner2_status}", arena.getCorner2() != null ? "✓ Set" : "✗ Not Set");
        
        // Completion percentage
        text = text.replace("{completion_percentage}", String.valueOf(calculateCompletionPercentage(arena)));
        
        // Regeneration info
        text = text.replace("{regen_status}", arena.isRegenerateBlocks() ? "Enabled" : "Disabled");
        text = text.replace("{regen_performance}", arena.isRegenerateBlocks() ? "Medium" : "Low");
        text = text.replace("{regen_behavior}", arena.isRegenerateBlocks() ? "be restored" : "remain changed");
        text = text.replace("{regen_timing}", arena.isRegenerateBlocks() ? "after each match" : "never");
        
        // Kit management info
        List<String> allowedKits = arena.getAllowedKits();
        text = text.replace("{allowed_kit_count}", String.valueOf(allowedKits.size()));
        text = text.replace("{total_kits}", String.valueOf(kitManager.getKits().size()));
        
        if (allowedKits.isEmpty()) {
            text = text.replace("{allowed_kits}", "All kits allowed");
            text = text.replace("{allowed_kits_detailed}", "&aAll kits are allowed");
            text = text.replace("{kit_restriction_status}", "No restrictions");
        } else {
            text = text.replace("{allowed_kits}", String.join(", ", allowedKits));
            
            StringBuilder detailedKits = new StringBuilder();
            for (int i = 0; i < allowedKits.size(); i++) {
                if (i > 0) detailedKits.append("\n");
                detailedKits.append("&f• ").append(allowedKits.get(i));
            }
            text = text.replace("{allowed_kits_detailed}", detailedKits.toString());
            text = text.replace("{kit_restriction_status}", "Restricted to " + allowedKits.size() + " kits");
        }
        
        return text;
    }

    private String formatLocation(org.bukkit.Location location) {
        if (location == null) {
            return "Not set";
        }
        return String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ());
    }

    private String formatDetailedLocation(org.bukkit.Location location) {
        if (location == null) {
            return "Not set";
        }
        return String.format("X: %.2f, Y: %.2f, Z: %.2f", location.getX(), location.getY(), location.getZ());
    }

    private String getWorldName(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) {
            return "Unknown";
        }
        return location.getWorld().getName();
    }

    private String formatRotation(org.bukkit.Location location, boolean isPitch) {
        if (location == null) {
            return "0.0";
        }
        float value = isPitch ? location.getPitch() : location.getYaw();
        return String.format("%.1f", value);
    }

    private String calculateDimensions(Arena arena) {
        if (arena.getCorner1() == null || arena.getCorner2() == null) {
            return "Unknown (corners not set)";
        }
        
        org.bukkit.Location c1 = arena.getCorner1();
        org.bukkit.Location c2 = arena.getCorner2();
        
        int width = Math.abs((int)(c2.getX() - c1.getX())) + 1;
        int height = Math.abs((int)(c2.getY() - c1.getY())) + 1;
        int length = Math.abs((int)(c2.getZ() - c1.getZ())) + 1;
        
        return String.format("%d x %d x %d blocks", width, height, length);
    }

    private int calculateCompletionPercentage(Arena arena) {
        int totalPoints = 5; // center, spawn1, spawn2, corner1, corner2
        int setPoints = 0;
        
        if (arena.getCenter() != null) setPoints++;
        if (arena.getSpawn1() != null) setPoints++;
        if (arena.getSpawn2() != null) setPoints++;
        if (arena.getCorner1() != null) setPoints++;
        if (arena.getCorner2() != null) setPoints++;
        
        return (setPoints * 100) / totalPoints;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (title.contains("Arena Manager")) {
            event.setCancelled(true);
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String arenaName = extractArenaName(clicked);
            if (arenaName != null) {
                openArenaEditor(player, arenaName);
            }
        } else if (title.contains("Arena Editor")) {
            event.setCancelled(true);
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;
            
            String arenaName = editingArena.get(player.getUniqueId());
            if (arenaName == null) return;
            
            Arena arena = arenaManager.getArena(arenaName);
            if (arena == null) return;
            
            String action = getActionFromItem(clickedItem);
            if (action == null) return;
            
            handleAction(player, arena, action, event.isRightClick());
        }
    }

    private String extractArenaName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            return HexUtils.stripColor(displayName);
        }
        return null;
    }

    private String getActionFromItem(ItemStack item) {
        File configFile = new File(plugin.getDataFolder(), "gui/arena_editor.yml");
        if (!configFile.exists()) return null;
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection.getString("material", "").equals(item.getType().name())) {
                    String displayName = HexUtils.colorize(itemSection.getString("name", ""));
                    if (item.getItemMeta().getDisplayName().contains(HexUtils.stripColor(displayName))) {
                        return itemSection.getString("action");
                    }
                }
            }
        }
        
        return null;
    }

    private void handleAction(Player player, Arena arena, String action, boolean rightClick) {
        switch (action) {
            case "SET_CENTER":
                arena.setCenter(player.getLocation());
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aCenter set to your location!"));
                player.sendMessage(HexUtils.colorize("&7Location: &f" + formatDetailedLocation(player.getLocation())));
                reopenGui(player, arena.getName());
                break;
                
            case "SET_SPAWN1":
                arena.setSpawn1(player.getLocation());
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aSpawn 1 set to your location!"));
                player.sendMessage(HexUtils.colorize("&7Location: &f" + formatDetailedLocation(player.getLocation())));
                reopenGui(player, arena.getName());
                break;
                
            case "SET_SPAWN2":
                arena.setSpawn2(player.getLocation());
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aSpawn 2 set to your location!"));
                player.sendMessage(HexUtils.colorize("&7Location: &f" + formatDetailedLocation(player.getLocation())));
                reopenGui(player, arena.getName());
                break;
                
            case "SET_CORNER1":
                arena.setCorner1(player.getLocation());
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aCorner 1 set to your location!"));
                player.sendMessage(HexUtils.colorize("&7Location: &f" + formatDetailedLocation(player.getLocation())));
                reopenGui(player, arena.getName());
                break;
                
            case "SET_CORNER2":
                arena.setCorner2(player.getLocation());
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aCorner 2 set to your location!"));
                player.sendMessage(HexUtils.colorize("&7Location: &f" + formatDetailedLocation(player.getLocation())));
                if (arena.getCorner1() != null) {
                    player.sendMessage(HexUtils.colorize("&7Arena dimensions: &e" + calculateDimensions(arena)));
                }
                reopenGui(player, arena.getName());
                break;
                
            case "SAVE_ARENA":
                if (!arena.isComplete()) {
                    player.sendMessage(HexUtils.colorize("&cArena is not complete! Set all points first."));
                    player.sendMessage(HexUtils.colorize("&7Completion: &e" + calculateCompletionPercentage(arena) + "%"));
                    return;
                }
                arenaManager.saveArenaSchematic(arena);
                player.sendMessage(HexUtils.colorize("&aArena saved and schematic generated!"));
                player.sendMessage(HexUtils.colorize("&7Dimensions: &e" + calculateDimensions(arena)));
                break;
                
            case "CLONE_ARENA":
                if (!arena.isComplete()) {
                    player.sendMessage(HexUtils.colorize("&cArena is not complete! Cannot clone incomplete arena."));
                    return;
                }
                player.closeInventory();
                String clonedName = arenaManager.cloneArena(arena, player.getLocation());
                if (clonedName != null) {
                    player.sendMessage(HexUtils.colorize("&aArena cloned successfully as: &e" + clonedName));
                    player.sendMessage(HexUtils.colorize("&7Cloned at: &f" + formatDetailedLocation(player.getLocation())));
                } else {
                    player.sendMessage(HexUtils.colorize("&cFailed to clone arena!"));
                }
                break;
                
            case "MANAGE_KITS":
                player.closeInventory();
                if (rightClick) {
                    startKitRemoval(player, arena);
                } else {
                    startKitAddition(player, arena);
                }
                break;
                
            case "TOGGLE_REGEN":
                arena.setRegenerateBlocks(!arena.isRegenerateBlocks());
                arenaManager.saveArena(arena);
                String status = arena.isRegenerateBlocks() ? "enabled" : "disabled";
                player.sendMessage(HexUtils.colorize("&aBlock regeneration " + status + "!"));
                reopenGui(player, arena.getName());
                break;
        }
    }

    private void startKitAddition(Player player, Arena arena) {
        waitingForInput.put(player.getUniqueId(), arena.getName());
        inputType.put(player.getUniqueId(), "ADD_KIT");
        inputTimeout.put(player.getUniqueId(), System.currentTimeMillis() + 30000);
        
        player.sendMessage("");
        player.sendMessage(HexUtils.colorize("&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(HexUtils.colorize("&6&lADD KIT TO ARENA"));
        player.sendMessage("");
        player.sendMessage(HexUtils.colorize("&7Arena: &e" + arena.getName()));
        player.sendMessage(HexUtils.colorize("&7Current allowed kits: &f" + 
            (arena.getAllowedKits().isEmpty() ? "All kits allowed" : String.join(", ", arena.getAllowedKits()))));
        player.sendMessage("");
        player.sendMessage(HexUtils.colorize("&eType the name of the kit to add, or 'cancel' to cancel:"));
        player.sendMessage(HexUtils.colorize("&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    private void startKitRemoval(Player player, Arena arena) {
        if (arena.getAllowedKits().isEmpty()) {
            player.sendMessage(HexUtils.colorize("&cNo kits to remove! All kits are currently allowed."));
            reopenGui(player, arena.getName());
            return;
        }
        
        waitingForInput.put(player.getUniqueId(), arena.getName());
        inputType.put(player.getUniqueId(), "REMOVE_KIT");
        inputTimeout.put(player.getUniqueId(), System.currentTimeMillis() + 30000);
        
        player.sendMessage("");
        player.sendMessage(HexUtils.colorize("&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(HexUtils.colorize("&c&lREMOVE KIT FROM ARENA"));
        player.sendMessage("");
        player.sendMessage(HexUtils.colorize("&7Arena: &e" + arena.getName()));
        player.sendMessage(HexUtils.colorize("&7Current allowed kits: &f" + String.join(", ", arena.getAllowedKits())));
        player.sendMessage("");
        player.sendMessage(HexUtils.colorize("&eType the name of the kit to remove, or 'cancel' to cancel:"));
        player.sendMessage(HexUtils.colorize("&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (!waitingForInput.containsKey(playerId)) return;
        
        event.setCancelled(true);
        
        // Check timeout
        if (System.currentTimeMillis() > inputTimeout.get(playerId)) {
            waitingForInput.remove(playerId);
            inputType.remove(playerId);
            inputTimeout.remove(playerId);
            player.sendMessage(HexUtils.colorize("&cInput timed out!"));
            return;
        }
        
        String message = event.getMessage().trim();
        String arenaName = waitingForInput.get(playerId);
        String type = inputType.get(playerId);
        
        // Clean up
        waitingForInput.remove(playerId);
        inputType.remove(playerId);
        inputTimeout.remove(playerId);
        
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(HexUtils.colorize("&cCancelled!"));
            Bukkit.getScheduler().runTask(plugin, () -> reopenGui(player, arenaName));
            return;
        }
        
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            player.sendMessage(HexUtils.colorize("&cArena not found!"));
            return;
        }
        
        if (type.equals("ADD_KIT")) {
            Kit kit = kitManager.getKit(message);
            if (kit == null) {
                player.sendMessage(HexUtils.colorize("&cKit not found: " + message));
                Bukkit.getScheduler().runTask(plugin, () -> reopenGui(player, arenaName));
                return;
            }
            
            if (arena.getAllowedKits().contains(kit.getName())) {
                player.sendMessage(HexUtils.colorize("&cKit is already allowed in this arena!"));
            } else {
                arena.addAllowedKit(kit.getName());
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aKit '" + kit.getName() + "' added to arena!"));
            }
        } else if (type.equals("REMOVE_KIT")) {
            if (!arena.getAllowedKits().contains(message)) {
                player.sendMessage(HexUtils.colorize("&cKit is not in the allowed list!"));
            } else {
                arena.removeAllowedKit(message);
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aKit '" + message + "' removed from arena!"));
            }
        }
        
        Bukkit.getScheduler().runTask(plugin, () -> reopenGui(player, arenaName));
    }

    private void reopenGui(Player player, String arenaName) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            openArenaEditor(player, arenaName);
        }, 1L);
    }
}
