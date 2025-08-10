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

    public void openArenaEditor(Player player, String arenaName) {
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            player.sendMessage(HexUtils.colorify("&cArena not found!"));
            return;
        }

        editingArena.put(player.getUniqueId(), arenaName);

        File configFile = new File(plugin.getDataFolder(), "gui/arena_editor.yml");
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        String title = HexUtils.colorify(config.getString("title", "&6Arena Editor"));
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
        config.set("items.set_center.lore", List.of("&7Click to set arena center", "&7Current: &f{center}"));
        config.set("items.set_center.action", "SET_CENTER");
        
        // Set Spawn 1
        config.set("items.set_spawn1.slot", 11);
        config.set("items.set_spawn1.material", "RED_BED");
        config.set("items.set_spawn1.name", "&cSet Spawn 1");
        config.set("items.set_spawn1.lore", List.of("&7Click to set spawn point 1", "&7Current: &f{spawn1}"));
        config.set("items.set_spawn1.action", "SET_SPAWN1");
        
        // Set Spawn 2
        config.set("items.set_spawn2.slot", 12);
        config.set("items.set_spawn2.material", "BLUE_BED");
        config.set("items.set_spawn2.name", "&9Set Spawn 2");
        config.set("items.set_spawn2.lore", List.of("&7Click to set spawn point 2", "&7Current: &f{spawn2}"));
        config.set("items.set_spawn2.action", "SET_SPAWN2");
        
        // Set Corner 1
        config.set("items.set_corner1.slot", 13);
        config.set("items.set_corner1.material", "STONE");
        config.set("items.set_corner1.name", "&7Set Corner 1");
        config.set("items.set_corner1.lore", List.of("&7Click to set corner 1", "&7Current: &f{corner1}"));
        config.set("items.set_corner1.action", "SET_CORNER1");
        
        // Set Corner 2
        config.set("items.set_corner2.slot", 14);
        config.set("items.set_corner2.material", "COBBLESTONE");
        config.set("items.set_corner2.name", "&7Set Corner 2");
        config.set("items.set_corner2.lore", List.of("&7Click to set corner 2", "&7Current: &f{corner2}"));
        config.set("items.set_corner2.action", "SET_CORNER2");
        
        // Save Arena
        config.set("items.save_arena.slot", 15);
        config.set("items.save_arena.material", "EMERALD");
        config.set("items.save_arena.name", "&aSave Arena");
        config.set("items.save_arena.lore", List.of("&7Click to save arena", "&7and generate schematic"));
        config.set("items.save_arena.action", "SAVE_ARENA");
        
        // Clone Arena
        config.set("items.clone_arena.slot", 16);
        config.set("items.clone_arena.material", "STRUCTURE_BLOCK");
        config.set("items.clone_arena.name", "&bClone Arena");
        config.set("items.clone_arena.lore", List.of("&7Click to clone this arena", "&7at your current location"));
        config.set("items.clone_arena.action", "CLONE_ARENA");
        
        // Manage Allowed Kits
        config.set("items.manage_kits.slot", 19);
        config.set("items.manage_kits.material", "CHEST");
        config.set("items.manage_kits.name", "&6Manage Allowed Kits");
        config.set("items.manage_kits.lore", List.of("&7Left Click: Add kit", "&7Right Click: Remove kit", "&7", "&eAllowed Kits:", "&f{allowed_kits}"));
        config.set("items.manage_kits.action", "MANAGE_KITS");
        
        // Toggle Regeneration
        config.set("items.toggle_regen.slot", 20);
        config.set("items.toggle_regen.material", "GRASS_BLOCK");
        config.set("items.toggle_regen.name", "&2Toggle Block Regeneration");
        config.set("items.toggle_regen.lore", List.of("&7Click to toggle block regeneration", "&7Current: &f{regen_status}"));
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
                meta.setDisplayName(HexUtils.colorify(name));
                
                List<String> lore = section.getStringList("lore");
                List<String> processedLore = new ArrayList<>();
                for (String line : lore) {
                    processedLore.add(HexUtils.colorify(replacePlaceholders(line, arena)));
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
        text = text.replace("{arena}", arena.getName());
        text = text.replace("{center}", formatLocation(arena.getCenter()));
        text = text.replace("{spawn1}", formatLocation(arena.getSpawn1()));
        text = text.replace("{spawn2}", formatLocation(arena.getSpawn2()));
        text = text.replace("{corner1}", formatLocation(arena.getCorner1()));
        text = text.replace("{corner2}", formatLocation(arena.getCorner2()));
        text = text.replace("{regen_status}", arena.isRegenerateBlocks() ? "Enabled" : "Disabled");
        
        // Format allowed kits
        List<String> allowedKits = arena.getAllowedKits();
        if (allowedKits.isEmpty()) {
            text = text.replace("{allowed_kits}", "All kits allowed");
        } else {
            text = text.replace("{allowed_kits}", String.join(", ", allowedKits));
        }
        
        return text;
    }

    private String formatLocation(org.bukkit.Location location) {
        if (location == null) {
            return "Not set";
        }
        return String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (!title.contains("Arena Editor")) return;
        
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

    private String getActionFromItem(ItemStack item) {
        File configFile = new File(plugin.getDataFolder(), "gui/arena_editor.yml");
        if (!configFile.exists()) return null;
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection.getString("material", "").equals(item.getType().name())) {
                    String displayName = HexUtils.colorify(itemSection.getString("name", ""));
                    if (item.getItemMeta().getDisplayName().equals(displayName)) {
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
                player.sendMessage(HexUtils.colorify("&aCenter set to your location!"));
                reopenGui(player, arena.getName());
                break;
                
            case "SET_SPAWN1":
                arena.setSpawn1(player.getLocation());
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorify("&aSpawn 1 set to your location!"));
                reopenGui(player, arena.getName());
                break;
                
            case "SET_SPAWN2":
                arena.setSpawn2(player.getLocation());
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorify("&aSpawn 2 set to your location!"));
                reopenGui(player, arena.getName());
                break;
                
            case "SET_CORNER1":
                arena.setCorner1(player.getLocation());
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorify("&aCorner 1 set to your location!"));
                reopenGui(player, arena.getName());
                break;
                
            case "SET_CORNER2":
                arena.setCorner2(player.getLocation());
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorify("&aCorner 2 set to your location!"));
                reopenGui(player, arena.getName());
                break;
                
            case "SAVE_ARENA":
                if (!arena.isComplete()) {
                    player.sendMessage(HexUtils.colorify("&cArena is not complete! Set all points first."));
                    return;
                }
                arenaManager.saveArenaSchematic(arena);
                player.sendMessage(HexUtils.colorify("&aArena saved and schematic generated!"));
                break;
                
            case "CLONE_ARENA":
                if (!arena.isComplete()) {
                    player.sendMessage(HexUtils.colorify("&cArena is not complete! Cannot clone incomplete arena."));
                    return;
                }
                player.closeInventory();
                String clonedName = arenaManager.cloneArena(arena, player.getLocation());
                if (clonedName != null) {
                    player.sendMessage(HexUtils.colorify("&aArena cloned successfully as: &e" + clonedName));
                } else {
                    player.sendMessage(HexUtils.colorify("&cFailed to clone arena!"));
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
                player.sendMessage(HexUtils.colorify("&aBlock regeneration " + status + "!"));
                reopenGui(player, arena.getName());
                break;
        }
    }

    private void startKitAddition(Player player, Arena arena) {
        waitingForInput.put(player.getUniqueId(), arena.getName());
        inputType.put(player.getUniqueId(), "ADD_KIT");
        inputTimeout.put(player.getUniqueId(), System.currentTimeMillis() + 30000);
        
        player.sendMessage("");
        player.sendMessage(HexUtils.colorify("&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(HexUtils.colorify("&6&lADD KIT TO ARENA"));
        player.sendMessage("");
        player.sendMessage(HexUtils.colorify("&7Arena: &e" + arena.getName()));
        player.sendMessage(HexUtils.colorify("&7Current allowed kits: &f" + 
            (arena.getAllowedKits().isEmpty() ? "All kits allowed" : String.join(", ", arena.getAllowedKits()))));
        player.sendMessage("");
        player.sendMessage(HexUtils.colorify("&eType the name of the kit to add, or 'cancel' to cancel:"));
        player.sendMessage(HexUtils.colorify("&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    private void startKitRemoval(Player player, Arena arena) {
        if (arena.getAllowedKits().isEmpty()) {
            player.sendMessage(HexUtils.colorify("&cNo kits to remove! All kits are currently allowed."));
            reopenGui(player, arena.getName());
            return;
        }
        
        waitingForInput.put(player.getUniqueId(), arena.getName());
        inputType.put(player.getUniqueId(), "REMOVE_KIT");
        inputTimeout.put(player.getUniqueId(), System.currentTimeMillis() + 30000);
        
        player.sendMessage("");
        player.sendMessage(HexUtils.colorify("&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(HexUtils.colorify("&c&lREMOVE KIT FROM ARENA"));
        player.sendMessage("");
        player.sendMessage(HexUtils.colorify("&7Arena: &e" + arena.getName()));
        player.sendMessage(HexUtils.colorify("&7Current allowed kits: &f" + String.join(", ", arena.getAllowedKits())));
        player.sendMessage("");
        player.sendMessage(HexUtils.colorify("&eType the name of the kit to remove, or 'cancel' to cancel:"));
        player.sendMessage(HexUtils.colorify("&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
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
            player.sendMessage(HexUtils.colorify("&cInput timed out!"));
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
            player.sendMessage(HexUtils.colorify("&cCancelled!"));
            Bukkit.getScheduler().runTask(plugin, () -> reopenGui(player, arenaName));
            return;
        }
        
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            player.sendMessage(HexUtils.colorify("&cArena not found!"));
            return;
        }
        
        if (type.equals("ADD_KIT")) {
            Kit kit = kitManager.getKit(message);
            if (kit == null) {
                player.sendMessage(HexUtils.colorify("&cKit not found: " + message));
                Bukkit.getScheduler().runTask(plugin, () -> reopenGui(player, arenaName));
                return;
            }
            
            if (arena.getAllowedKits().contains(kit.getName())) {
                player.sendMessage(HexUtils.colorify("&cKit is already allowed in this arena!"));
            } else {
                arena.addAllowedKit(kit.getName());
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorify("&aKit '" + kit.getName() + "' added to arena!"));
            }
        } else if (type.equals("REMOVE_KIT")) {
            if (!arena.getAllowedKits().contains(message)) {
                player.sendMessage(HexUtils.colorify("&cKit is not in the allowed list!"));
            } else {
                arena.removeAllowedKit(message);
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorify("&aKit '" + message + "' removed from arena!"));
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
