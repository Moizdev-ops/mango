package me.moiz.mangoparty.listeners;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Match;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDeathListener implements Listener {
    private MangoParty plugin;
    private Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private Map<UUID, ItemStack> savedOffhand = new HashMap<>();
    
    public PlayerDeathListener(MangoParty plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        Match match = plugin.getMatchManager().getPlayerMatch(player);
        
        // Check if player is in a match and would die from this damage
        if (match != null && player.getHealth() - event.getFinalDamage() <= 0) {
            // Cancel the damage event to prevent death
            event.setCancelled(true);
            
            // Handle the death manually
            handlePlayerDeath(player, match);
        }
    }
    
    /**
     * Custom method to handle player deaths in matches without triggering the vanilla death event
     */
    private void handlePlayerDeath(Player player, Match match) {
        // Store death location
        final Location deathLocation = player.getLocation().clone();
        
        // Handle killer if exists
        Player killer = player.getKiller();
        if (killer != null && plugin.getMatchManager().isInMatch(killer)) {
            match.addKill(killer.getUniqueId());
        }
        
        // Eliminate player from match
        plugin.getMatchManager().eliminatePlayer(player, match);
        
        // Send elimination message
        player.sendTitle("§c§lELIMINATED", "§7You are now spectating", 10, 40, 10);
        
        // Save inventories before clearing them
        if (!savedInventories.containsKey(player.getUniqueId())) {
            saveInventory(player);
        }
        if (killer != null && !savedInventories.containsKey(killer.getUniqueId())) {
            saveInventory(killer);
        }
        
        // Make player invincible for 2 seconds
        player.setInvulnerable(true);
        
        // Clear inventories of both players
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.updateInventory();
        
        if (killer != null) {
            killer.getInventory().clear();
            killer.getInventory().setArmorContents(null);
            killer.getInventory().setItemInOffHand(null);
            killer.updateInventory();
        }
        
        // Ensure player stays at death location
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // Keep player at death location
                    player.teleport(deathLocation);
                }
            }
        }.runTaskLater(plugin, 1L); // Run 1 tick later
        
        // Announce elimination to all match players
        for (Player matchPlayer : match.getAllPlayers()) {
            if (!matchPlayer.equals(player)) {
                String killerName = killer != null ? killer.getName() : "unknown causes";
                matchPlayer.sendMessage("§c" + player.getName() + " §7was eliminated by §c" + killerName);
            }
        }
        
        // Start new round after 2 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (match.isFinished()) {
                    return;
                }
                
                // Remove invincibility
                player.setInvulnerable(false);
                
                // Restore saved inventories for both players
                restoreInventory(player);
                if (killer != null) {
                    restoreInventory(killer);
                }
        
                // Show countdown message
                for (Player matchPlayer : match.getAllPlayers()) {
                    matchPlayer.sendTitle("§6§lNEW ROUND", "§eOrganize your inventory", 10, 100, 10);
                }
            }
        }.runTaskLater(plugin, 40L); // 2 seconds = 40 ticks
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Match match = plugin.getMatchManager().getPlayerMatch(player);
        
        if (match == null) {
            return; // Player not in a match
        }
        
        // Override death behavior for match players
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Cancel the death event to prevent respawn
        player.setHealth(20.0);
        
        // Handle the death manually
        handlePlayerDeath(player, match);
        
        // Update scoreboard
        plugin.getScoreboardManager().updateMatchScoreboards(match);
    }

    public void saveInventory(Player player) {
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());
        savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents().clone());
        savedOffhand.put(player.getUniqueId(), player.getInventory().getItemInOffHand().clone());
    }

    private void restoreInventory(Player player) {
        UUID playerId = player.getUniqueId();
        if (savedInventories.containsKey(playerId)) {
            player.getInventory().setContents(savedInventories.get(playerId));
            player.getInventory().setArmorContents(savedArmor.get(playerId));
            player.getInventory().setItemInOffHand(savedOffhand.get(playerId));
            player.updateInventory();
        }
    }
}
