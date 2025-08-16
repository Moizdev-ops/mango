package me.moiz.mangoparty.listeners;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.managers.DuelManager;
import me.moiz.mangoparty.models.Duel;
import me.moiz.mangoparty.models.Match;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.EntityEffect;
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
     * Store death location when player takes fatal damage in duel
     * This is used by PlayerRespawnListener for exact respawn positioning
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Only store death location for duels - actual death handling is done by PlayerDeathListener
        if (duelManager.isInDuel(player) && player.getHealth() - event.getFinalDamage() <= 0) {
            // Store death location with exact yaw/pitch preserved
            final Location deathLocation = player.getLocation().clone();
            // Slightly raise Y coordinate to avoid spawning inside blocks
            deathLocation.setY(deathLocation.getY() + 0.1);
            deathLocations.put(player.getUniqueId(), deathLocation);
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
     * Also handles party split matches where players can hit opponents on the other team
     * IMPORTANT: Only affects players in duels or matches, preserves vanilla mechanics for others
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if event is already cancelled
        if (event.isCancelled()) {
            return;
        }
        
        // Only handle player vs player damage
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player damaged = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();
        
        // Debug information - only log if debug is enabled
        boolean debug = plugin.getConfig().getBoolean("debug", false);
        if (debug) {
            plugin.getLogger().info("Damage event: " + damager.getName() + " -> " + damaged.getName());
        }
        
        // Check if players are in duels or matches
        Duel damagedDuel = duelManager.getPlayerDuel(damaged);
        Duel damagerDuel = duelManager.getPlayerDuel(damager);
        Match damagedMatch = plugin.getMatchManager().getPlayerMatch(damaged);
        Match damagerMatch = plugin.getMatchManager().getPlayerMatch(damager);
        
        // CRITICAL: If neither player is in a duel or match, let vanilla handle it
        if (damagedDuel == null && damagerDuel == null && damagedMatch == null && damagerMatch == null) {
            if (debug) {
                plugin.getLogger().info("Neither player in duel/match, letting vanilla handle damage");
            }
            return; // Let vanilla handle damage for non-duel, non-match players
        }
        
        // Handle mixed scenarios (one player in match/duel, other not)
        if ((damagedDuel == null && damagerDuel != null) || (damagedDuel != null && damagerDuel == null)) {
            // One player in duel, other not - prevent damage
            event.setCancelled(true);
            if (damagerDuel != null) {
                damager.sendMessage("§cYou cannot attack players outside your duel!");
            }
            return;
        }
        
        if ((damagedMatch == null && damagerMatch != null) || (damagedMatch != null && damagerMatch == null)) {
            // One player in match, other not - prevent damage
            event.setCancelled(true);
            if (damagerMatch != null) {
                damager.sendMessage("§cYou cannot attack players outside your match!");
            }
            return;
        }
        
        // Handle party match damage
        if (damagedMatch != null && damagerMatch != null) {
            if (debug) {
                plugin.getLogger().info("Party match damage check: " + damagedMatch.getId() + " vs " + damagerMatch.getId());
            }
            
            // If players are in different matches, cancel damage
            if (!damagedMatch.getId().equals(damagerMatch.getId())) {
                event.setCancelled(true);
                damager.sendMessage("§cYou cannot attack players in different matches!");
                return;
            }
            
            // If match is in preparing or countdown state, cancel damage
            if (damagedMatch.getState() == Match.MatchState.PREPARING || 
                damagedMatch.getState() == Match.MatchState.COUNTDOWN) {
                event.setCancelled(true);
                damager.sendMessage("§cYou cannot attack during the countdown!");
                return;
            }
            
            // If match is a split match, check if players are on the same team
            if (damagedMatch.getMatchType().equals("split")) {
                boolean sameTeam = damagedMatch.arePlayersOnSameTeam(damaged.getUniqueId(), damager.getUniqueId());
                if (debug) {
                    plugin.getLogger().info("Split match team check: " + sameTeam);
                }
                
                if (sameTeam) {
                    event.setCancelled(true);
                    damager.sendMessage("§cYou cannot attack players on your team!");
                    return;
                }
            }
            
            // If damaged player is eliminated (spectator), cancel damage
            if (damagedMatch.isPlayerEliminated(damaged.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            
            // Allow damage between players in the same match
            if (debug) {
                plugin.getLogger().info("Allowing damage in party match");
            }
            event.setCancelled(false); // Explicitly allow damage
            return;
        }
        
        // Handle duel damage
        if (damagedDuel != null && damagerDuel != null) {
        
            if (debug) {
                plugin.getLogger().info("Duel damage check: " + damagedDuel.getId() + " vs " + damagerDuel.getId());
            }
            
            // Check if both players are in the same duel match
            if (!damagerDuel.getId().equals(damagedDuel.getId())) {
                event.setCancelled(true);
                // Send message when players are in different duels
                damager.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                 plugin.getConfig().getString("messages.player-duel.cannot-damage-other-duel"));
                return;
            }
            
            // Check if players are on the same team in a party split match
            if (damagedDuel.isPartySplitMatch()) {
                boolean sameTeam = damagedDuel.arePlayersOnSameTeam(damaged.getUniqueId(), damager.getUniqueId());
                if (debug) {
                    plugin.getLogger().info("Duel split match team check: " + sameTeam);
                }
                
                if (sameTeam) {
                    event.setCancelled(true);
                    damager.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                     plugin.getConfig().getString("messages.player-duel.cannot-damage-teammate"));
                    return;
                }
            }
            
            // Explicitly allow damage between players in the same duel
            if (debug) {
                plugin.getLogger().info("Allowing damage in duel");
            }
            event.setCancelled(false);
            
            // Prevent damage during countdown or preparation
            if (damagedDuel.getState() == Duel.DuelState.COUNTDOWN || 
                damagedDuel.getState() == Duel.DuelState.PREPARING) {
                event.setCancelled(true);
            }
        }
    }
}