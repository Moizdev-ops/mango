package me.moiz.mangoparty.listeners;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.managers.DuelManager;
import me.moiz.mangoparty.models.Duel;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelListener implements Listener {
    private final MangoParty plugin;
    private final DuelManager duelManager;
    
    // Map to store player death locations with exact yaw/pitch preserved
    // This map is accessed by PlayerRespawnListener to set exact respawn locations
    private final Map<UUID, Location> deathLocations = new HashMap<>();
    
    public DuelListener(MangoParty plugin) {
        this.plugin = plugin;
        this.duelManager = plugin.getDuelManager();
    }
    
    /**
     * Get the stored death location for a player
     * @param playerId UUID of the player
     * @return Location where the player died, or null if not found
     */
    public Location getDeathLocation(UUID playerId) {
        return deathLocations.get(playerId);
    }
    
    /**
     * Remove a player's death location from storage
     * @param playerId UUID of the player
     * @return The removed location, or null if not found
     */
    public Location removeDeathLocation(UUID playerId) {
        return deathLocations.remove(playerId);
    }
    
    /**
     * Handle fatal damage to players in duels
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Check if player is in a duel and would die from this damage
        if (duelManager.isInDuel(player) && player.getHealth() - event.getFinalDamage() <= 0) {
            // Cancel the damage event to prevent actual death
            event.setCancelled(true);
            
            // Store death location with exact yaw/pitch preserved
            final Location deathLocation = player.getLocation().clone();
            // Slightly raise Y coordinate to avoid spawning inside blocks
            deathLocation.setY(deathLocation.getY() + 0.1);
            
            // Store location for respawn
            deathLocations.put(player.getUniqueId(), deathLocation);
            
            // Set player health to 0 to trigger death animation
            player.setHealth(0);
            
            // Schedule a task to teleport player back to death location and restore health
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // Teleport player back to death location
                        player.teleport(deathLocation);
                        // Set health to full
                        player.setHealth(20.0);
                        // Handle the death in DuelManager
                        duelManager.handlePlayerDeath(player);
                    }
                }
            }.runTaskLater(plugin, 1L); // Run 1 tick later
        }
    }
    
    // Removed handleCustomDuelDeath method as we're now using vanilla death handling
    
    /**
     * Handle player death in a duel (backup handler)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Check if player is in a duel
        if (duelManager.isInDuel(player)) {
            // Prevent drops and exp loss
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
            
            // Get the stored death location
            Location deathLoc = deathLocations.get(player.getUniqueId());
            if (deathLoc == null) {
                // If no stored location, use current location
                deathLoc = player.getLocation().clone();
                // Slightly raise Y coordinate to avoid spawning inside blocks
                deathLoc.setY(deathLoc.getY() + 0.1);
                deathLocations.put(player.getUniqueId(), deathLoc);
            }
            
            // We'll let the respawn event handle teleportation back to death location
            // The damage event handler should have already called duelManager.handlePlayerDeath
            // This is just a backup in case something went wrong
            final Location finalDeathLoc = deathLoc.clone();
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // Ensure player is teleported back to death location
                        player.teleport(finalDeathLoc);
                        // Set health to full
                        player.setHealth(20.0);
                    }
                }
            }.runTaskLater(plugin, 1L); // Run 1 tick later
        }
    }
    
    /**
     * Handle player disconnect during a duel
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in a duel
        if (duelManager.isInDuel(player)) {
            duelManager.handlePlayerDisconnect(player);
        }
    }
    
    /**
     * Prevent damage during countdown or between players not in the same duel
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if both entities are players
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player damaged = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();
        
        // Check if damaged player is in a duel
        Duel damagedDuel = duelManager.getPlayerDuel(damaged);
        if (damagedDuel == null) {
            return;
        }
        
        // Check if damager is in the same duel
        Duel damagerDuel = duelManager.getPlayerDuel(damager);
        if (damagerDuel == null || !damagerDuel.getId().equals(damagedDuel.getId())) {
            event.setCancelled(true);
            // Send message when players are in different duels
            damager.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                             plugin.getConfig().getString("messages.player-duel.cannot-damage-other-duel"));
            return;
        }
        
        // Prevent damage during countdown or preparation
        if (damagedDuel.getState() == Duel.DuelState.COUNTDOWN || 
            damagedDuel.getState() == Duel.DuelState.PREPARING) {
            event.setCancelled(true);
        }
    }
}