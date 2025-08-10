package me.moiz.mangoparty.gui;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.managers.ArenaManager;
import me.moiz.mangoparty.managers.KitManager;
import me.moiz.mangoparty.models.Arena;
import me.moiz.mangoparty.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
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
    private Map<UUID, String> awaitingChatInput;
    private Map<UUID, Long> chatTimeouts;
    private DecimalFormat df = new DecimalFormat("#.##");

    public ArenaEditorGui(MangoParty plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
        this.kitManager = plugin.getKitManager();
        this.editingArena = new HashMap<>();
        this.awaitingChatInput = new HashMap<>();
        this.chatTimeouts = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openArenaEditor(Player player, String arenaName) {
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            player.sendMessage(HexUtils.colorize("&cArena not found!"));
            return;
        }

        editingArena.put(player.getUniqueId(), arenaName);
        
        Inventory gui = Bukkit.createInventory(null, 54, HexUtils.colorize("&6Arena Editor: &e" + arenaName));

        // Set Center (slot 10)
        ItemStack centerItem = new ItemStack(Material.BEACON);
        ItemMeta centerMeta = centerItem.getItemMeta();
        centerMeta.setDisplayName(HexUtils.colorize("&6Set Center"));
        List<String> centerLore = new ArrayList<>();
        if (arena.getCenter() != null) {
            Location center = arena.getCenter();
            centerLore.add(HexUtils.colorize("&aCurrently set:"));
            centerLore.add(HexUtils.colorize("&7X: " + df.format(center.getX()) + ", Y: " + df.format(center.getY()) + ", Z: " + df.format(center.getZ())));
            centerLore.add(HexUtils.colorize("&7World: " + center.getWorld().getName()));
            centerLore.add(HexUtils.colorize("&7Yaw: " + df.format(center.getYaw()) + "°, Pitch: " + df.format(center.getPitch()) + "°"));
        } else {
            centerLore.add(HexUtils.colorize("&cNot set"));
        }
        centerLore.add("");
        centerLore.add(HexUtils.colorize("&eClick to set to your current location"));
        centerMeta.setLore(centerLore);
        centerItem.setItemMeta(centerMeta);
        gui.setItem(10, centerItem);

        // Set Spawn 1 (slot 12)
        ItemStack spawn1Item = new ItemStack(Material.RED_BED);
        ItemMeta spawn1Meta = spawn1Item.getItemMeta();
        spawn1Meta.setDisplayName(HexUtils.colorize("&cSet Spawn 1"));
        List<String> spawn1Lore = new ArrayList<>();
        if (arena.getSpawn1() != null) {
            Location spawn1 = arena.getSpawn1();
            spawn1Lore.add(HexUtils.colorize("&aCurrently set:"));
            spawn1Lore.add(HexUtils.colorize("&7X: " + df.format(spawn1.getX()) + ", Y: " + df.format(spawn1.getY()) + ", Z: " + df.format(spawn1.getZ())));
            spawn1Lore.add(HexUtils.colorize("&7World: " + spawn1.getWorld().getName()));
            spawn1Lore.add(HexUtils.colorize("&7Yaw: " + df.format(spawn1.getYaw()) + "°, Pitch: " + df.format(spawn1.getPitch()) + "°"));
        } else {
            spawn1Lore.add(HexUtils.colorize("&cNot set"));
        }
        spawn1Lore.add("");
        spawn1Lore.add(HexUtils.colorize("&eClick to set to your current location"));
        spawn1Meta.setLore(spawn1Lore);
        spawn1Item.setItemMeta(spawn1Meta);
        gui.setItem(12, spawn1Item);

        // Set Spawn 2 (slot 14)
        ItemStack spawn2Item = new ItemStack(Material.BLUE_BED);
        ItemMeta spawn2Meta = spawn2Item.getItemMeta();
        spawn2Meta.setDisplayName(HexUtils.colorize("&9Set Spawn 2"));
        List<String> spawn2Lore = new ArrayList<>();
        if (arena.getSpawn2() != null) {
            Location spawn2 = arena.getSpawn2();
            spawn2Lore.add(HexUtils.colorize("&aCurrently set:"));
            spawn2Lore.add(HexUtils.colorize("&7X: " + df.format(spawn2.getX()) + ", Y: " + df.format(spawn2.getY()) + ", Z: " + df.format(spawn2.getZ())));
            spawn2Lore.add(HexUtils.colorize("&7World: " + spawn2.getWorld().getName()));
            spawn2Lore.add(HexUtils.colorize("&7Yaw: " + df.format(spawn2.getYaw()) + "°, Pitch: " + df.format(spawn2.getPitch()) + "°"));
        } else {
            spawn2Lore.add(HexUtils.colorize("&cNot set"));
        }
        spawn2Lore.add("");
        spawn2Lore.add(HexUtils.colorize("&eClick to set to your current location"));
        spawn2Meta.setLore(spawn2Lore);
        spawn2Item.setItemMeta(spawn2Meta);
        gui.setItem(14, spawn2Item);

        // Set Corner 1 (slot 28)
        ItemStack corner1Item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta corner1Meta = corner1Item.getItemMeta();
        corner1Meta.setDisplayName(HexUtils.colorize("&aSet Corner 1"));
        List<String> corner1Lore = new ArrayList<>();
        if (arena.getCorner1() != null) {
            Location corner1 = arena.getCorner1();
            corner1Lore.add(HexUtils.colorize("&aCurrently set:"));
            corner1Lore.add(HexUtils.colorize("&7X: " + df.format(corner1.getX()) + ", Y: " + df.format(corner1.getY()) + ", Z: " + df.format(corner1.getZ())));
            corner1Lore.add(HexUtils.colorize("&7World: " + corner1.getWorld().getName()));
        } else {
            corner1Lore.add(HexUtils.colorize("&cNot set"));
        }
        corner1Lore.add("");
        corner1Lore.add(HexUtils.colorize("&eClick to set to your current location"));
        corner1Meta.setLore(corner1Lore);
        corner1Item.setItemMeta(corner1Meta);
        gui.setItem(28, corner1Item);

        // Set Corner 2 (slot 34)
        ItemStack corner2Item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta corner2Meta = corner2Item.getItemMeta();
        corner2Meta.setDisplayName(HexUtils.colorize("&cSet Corner 2"));
        List<String> corner2Lore = new ArrayList<>();
        if (arena.getCorner2() != null) {
            Location corner2 = arena.getCorner2();
            corner2Lore.add(HexUtils.colorize("&aCurrently set:"));
            corner2Lore.add(HexUtils.colorize("&7X: " + df.format(corner2.getX()) + ", Y: " + df.format(corner2.getY()) + ", Z: " + df.format(corner2.getZ())));
            corner2Lore.add(HexUtils.colorize("&7World: " + corner2.getWorld().getName()));
        } else {
            corner2Lore.add(HexUtils.colorize("&cNot set"));
        }
        corner2Lore.add("");
        corner2Lore.add(HexUtils.colorize("&eClick to set to your current location"));
        corner2Meta.setLore(corner2Lore);
        corner2Item.setItemMeta(corner2Meta);
        gui.setItem(34, corner2Item);

        // Arena dimensions info (slot 22)
        ItemStack dimensionsItem = new ItemStack(Material.COMPASS);
        ItemMeta dimensionsMeta = dimensionsItem.getItemMeta();
        dimensionsMeta.setDisplayName(HexUtils.colorize("&6Arena Info"));
        List<String> dimensionsLore = new ArrayList<>();
        
        if (arena.getCorner1() != null && arena.getCorner2() != null) {
            Location c1 = arena.getCorner1();
            Location c2 = arena.getCorner2();
            int width = Math.abs(c1.getBlockX() - c2.getBlockX()) + 1;
            int height = Math.abs(c1.getBlockY() - c2.getBlockY()) + 1;
            int length = Math.abs(c1.getBlockZ() - c2.getBlockZ()) + 1;
            dimensionsLore.add(HexUtils.colorize("&aDimensions: " + width + " x " + height + " x " + length));
        } else {
            dimensionsLore.add(HexUtils.colorize("&cDimensions: Unknown"));
        }
        
        int completionPercentage = arena.getCompletionPercentage();
        dimensionsLore.add(HexUtils.colorize("&7Completion: " + completionPercentage + "%"));
        
        dimensionsMeta.setLore(dimensionsLore);
        dimensionsItem.setItemMeta(dimensionsMeta);
        gui.setItem(22, dimensionsItem);

        // Save Schematic (slot 37)
        ItemStack saveItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta saveMeta = saveItem.getItemMeta();
        saveMeta.setDisplayName(HexUtils.colorize("&aSave Schematic"));
        List<String> saveLore = new ArrayList<>();
        saveLore.add(HexUtils.colorize("&7Saves the arena structure"));
        if (arena.isComplete()) {
            saveLore.add(HexUtils.colorize("&aReady to save"));
        } else {
            saveLore.add(HexUtils.colorize("&cArena must be complete"));
        }
        saveLore.add("");
        saveLore.add(HexUtils.colorize("&eClick to save schematic"));
        saveMeta.setLore(saveLore);
        saveItem.setItemMeta(saveMeta);
        gui.setItem(37, saveItem);

        // Clone Arena (slot 39)
        ItemStack cloneItem = new ItemStack(Material.STRUCTURE_BLOCK);
        ItemMeta cloneMeta = cloneItem.getItemMeta();
        cloneMeta.setDisplayName(HexUtils.colorize("&bClone Arena"));
        List<String> cloneLore = new ArrayList<>();
        cloneLore.add(HexUtils.colorize("&7Creates a copy at your location"));
        if (arena.isComplete()) {
            cloneLore.add(HexUtils.colorize("&aReady to clone"));
        } else {
            cloneLore.add(HexUtils.colorize("&cArena must be complete"));
        }
        cloneLore.add("");
        cloneLore.add(HexUtils.colorize("&eClick to clone arena"));
        cloneMeta.setLore(cloneLore);
        cloneItem.setItemMeta(cloneMeta);
        gui.setItem(39, cloneItem);

        // Manage Allowed Kits (slot 41)
        ItemStack kitsItem = new ItemStack(Material.CHEST);
        ItemMeta kitsMeta = kitsItem.getItemMeta();
        kitsMeta.setDisplayName(HexUtils.colorize("&dManage Allowed Kits"));
        List<String> kitsLore = new ArrayList<>();
        List<String> allowedKits = arena.getAllowedKits();
        int totalKits = kitManager.getAllKits().size();
        
        if (allowedKits.isEmpty()) {
            kitsLore.add(HexUtils.colorize("&aAll kits allowed (" + totalKits + " kits)"));
        } else {
            kitsLore.add(HexUtils.colorize("&7Allowed kits: " + allowedKits.size() + "/" + totalKits));
            for (String kitName : allowedKits) {
                kitsLore.add(HexUtils.colorize("&8• " + kitName));
            }
        }
        kitsLore.add("");
        kitsLore.add(HexUtils.colorize("&eClick to manage kit restrictions"));
        kitsMeta.setLore(kitsLore);
        kitsItem.setItemMeta(kitsMeta);
        gui.setItem(41, kitsItem);

        // Toggle Regeneration (slot 43)
        ItemStack regenItem = new ItemStack(arena.isRegenerateBlocks() ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta regenMeta = regenItem.getItemMeta();
        regenMeta.setDisplayName(HexUtils.colorize("&6Block Regeneration"));
        List<String> regenLore = new ArrayList<>();
        regenLore.add(HexUtils.colorize("&7Status: " + (arena.isRegenerateBlocks() ? "&aEnabled" : "&cDisabled")));
        regenLore.add("");
        regenLore.add(HexUtils.colorize("&eClick to toggle"));
        regenMeta.setLore(regenLore);
        regenItem.setItemMeta(regenMeta);
        gui.setItem(43, regenItem);

        // Back button (slot 49)
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(HexUtils.colorize("&cBack"));
        backItem.setItemMeta(backMeta);
        gui.setItem(49, backItem);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (!event.getView().getTitle().contains("Arena Editor:")) return;
        
        event.setCancelled(true);
        
        String arenaName = editingArena.get(player.getUniqueId());
        if (arenaName == null) return;
        
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) return;
        
        int slot = event.getSlot();
        Location playerLoc = player.getLocation();
        
        switch (slot) {
            case 10: // Set Center
                arena.setCenter(playerLoc);
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aCenter set to your location!"));
                Bukkit.getScheduler().runTaskLater(plugin, () -> openArenaEditor(player, arenaName), 1L);
                break;
                
            case 12: // Set Spawn 1
                arena.setSpawn1(playerLoc);
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aSpawn 1 set to your location!"));
                Bukkit.getScheduler().runTaskLater(plugin, () -> openArenaEditor(player, arenaName), 1L);
                break;
                
            case 14: // Set Spawn 2
                arena.setSpawn2(playerLoc);
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aSpawn 2 set to your location!"));
                Bukkit.getScheduler().runTaskLater(plugin, () -> openArenaEditor(player, arenaName), 1L);
                break;
                
            case 28: // Set Corner 1
                arena.setCorner1(playerLoc);
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aCorner 1 set to your location!"));
                Bukkit.getScheduler().runTaskLater(plugin, () -> openArenaEditor(player, arenaName), 1L);
                break;
                
            case 34: // Set Corner 2
                arena.setCorner2(playerLoc);
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aCorner 2 set to your location!"));
                Bukkit.getScheduler().runTaskLater(plugin, () -> openArenaEditor(player, arenaName), 1L);
                break;
                
            case 37: // Save Schematic
                if (arena.isComplete()) {
                    if (arenaManager.saveSchematic(arena)) {
                        player.sendMessage(HexUtils.colorize("&aSchematic saved successfully!"));
                    } else {
                        player.sendMessage(HexUtils.colorize("&cFailed to save schematic!"));
                    }
                } else {
                    player.sendMessage(HexUtils.colorize("&cArena must be complete to save schematic!"));
                }
                break;
                
            case 39: // Clone Arena
                if (arena.isComplete()) {
                    player.closeInventory();
                    String newArenaName = arenaManager.cloneArena(arena, playerLoc);
                    if (newArenaName != null) {
                        player.sendMessage(HexUtils.colorize("&aArena cloned successfully as: &e" + newArenaName));
                    } else {
                        player.sendMessage(HexUtils.colorize("&cFailed to clone arena!"));
                    }
                } else {
                    player.sendMessage(HexUtils.colorize("&cArena must be complete to clone!"));
                }
                break;
                
            case 41: // Manage Allowed Kits
                player.closeInventory();
                startKitManagement(player, arenaName);
                break;
                
            case 43: // Toggle Regeneration
                arena.setRegenerateBlocks(!arena.isRegenerateBlocks());
                arenaManager.saveArena(arena);
                player.sendMessage(HexUtils.colorize("&aBlock regeneration " + (arena.isRegenerateBlocks() ? "enabled" : "disabled") + "!"));
                Bukkit.getScheduler().runTaskLater(plugin, () -> openArenaEditor(player, arenaName), 1L);
                break;
                
            case 49: // Back
                player.closeInventory();
                plugin.getGuiManager().openArenaListGui(player);
                break;
        }
    }

    private void startKitManagement(Player player, String arenaName) {
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) return;

        awaitingChatInput.put(player.getUniqueId(), "kit_management:" + arenaName);
        chatTimeouts.put(player.getUniqueId(), System.currentTimeMillis() + 30000);

        player.sendMessage("");
        player.sendMessage(HexUtils.colorize("&6&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(HexUtils.colorize("&6&lKIT MANAGEMENT"));
        player.sendMessage("");
        player.sendMessage(HexUtils.colorize("&eType &6'add <kitname>' &eto add a kit restriction"));
        player.sendMessage(HexUtils.colorize("&eType &6'remove <kitname>' &eto remove a kit restriction"));
        player.sendMessage(HexUtils.colorize("&eType &6'cancel' &eto cancel"));
        player.sendMessage("");
        
        List<String> allowedKits = arena.getAllowedKits();
        if (allowedKits.isEmpty()) {
            player.sendMessage(HexUtils.colorize("&7Currently: &aAll kits allowed"));
        } else {
            player.sendMessage(HexUtils.colorize("&7Currently allowed kits:"));
            for (String kit : allowedKits) {
                player.sendMessage(HexUtils.colorize("&8• &a" + kit));
            }
        }
        player.sendMessage("");
        player.sendMessage(HexUtils.colorize("&6&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (!awaitingChatInput.containsKey(playerId)) return;
        
        // Check timeout
        if (System.currentTimeMillis() > chatTimeouts.get(playerId)) {
            awaitingChatInput.remove(playerId);
            chatTimeouts.remove(playerId);
            player.sendMessage(HexUtils.colorize("&cInput timed out."));
            return;
        }
        
        event.setCancelled(true);
        String input = event.getMessage().toLowerCase().trim();
        String context = awaitingChatInput.get(playerId);
        
        awaitingChatInput.remove(playerId);
        chatTimeouts.remove(playerId);
        
        if (input.equals("cancel")) {
            player.sendMessage(HexUtils.colorize("&cCancelled."));
            String arenaName = context.split(":")[1];
            Bukkit.getScheduler().runTask(plugin, () -> openArenaEditor(player, arenaName));
            return;
        }
        
        if (context.startsWith("kit_management:")) {
            String arenaName = context.split(":")[1];
            Arena arena = arenaManager.getArena(arenaName);
            
            if (arena == null) {
                player.sendMessage(HexUtils.colorize("&cArena not found!"));
                return;
            }
            
            String[] parts = input.split(" ", 2);
            if (parts.length != 2) {
                player.sendMessage(HexUtils.colorize("&cInvalid format! Use 'add <kitname>' or 'remove <kitname>'"));
                Bukkit.getScheduler().runTask(plugin, () -> openArenaEditor(player, arenaName));
                return;
            }
            
            String action = parts[0];
            String kitName = parts[1];
            
            if (action.equals("add")) {
                if (kitManager.getKit(kitName) == null) {
                    player.sendMessage(HexUtils.colorize("&cKit '" + kitName + "' does not exist!"));
                } else if (arena.getAllowedKits().contains(kitName)) {
                    player.sendMessage(HexUtils.colorize("&cKit '" + kitName + "' is already allowed!"));
                } else {
                    arena.getAllowedKits().add(kitName);
                    arenaManager.saveArena(arena);
                    player.sendMessage(HexUtils.colorize("&aAdded kit '" + kitName + "' to allowed kits!"));
                }
            } else if (action.equals("remove")) {
                if (arena.getAllowedKits().remove(kitName)) {
                    arenaManager.saveArena(arena);
                    player.sendMessage(HexUtils.colorize("&aRemoved kit '" + kitName + "' from allowed kits!"));
                } else {
                    player.sendMessage(HexUtils.colorize("&cKit '" + kitName + "' was not in the allowed list!"));
                }
            } else {
                player.sendMessage(HexUtils.colorize("&cInvalid action! Use 'add' or 'remove'"));
            }
            
            Bukkit.getScheduler().runTask(plugin, () -> openArenaEditor(player, arenaName));
        }
    }
}
