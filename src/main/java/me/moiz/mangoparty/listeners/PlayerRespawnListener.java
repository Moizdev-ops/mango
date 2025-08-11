package me.moiz.mangoparty.listeners;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Duel;
import me.moiz.mangoparty.models.Match;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class PlayerRespawnListener implements Listener {
    private MangoParty plugin;
    
    public PlayerRespawnListener(MangoParty plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player respawn with HIGHEST priority to override any other plugins
     * This ensures players in duels respawn exactly where they died with the same facing direction
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in a duel
        if (plugin.getDuelManager().isInDuel(player)) {
            // Get the stored death location from DuelListener
            DuelListener duelListener = plugin.getDuelListener();
            Location deathLoc = duelListener.getDeathLocation(player.getUniqueId());
            
            if (deathLoc != null) {
                // Set respawn location to exact death location with preserved yaw/pitch
                event.setRespawnLocation(deathLoc);
                // Remove the stored location to prevent memory leaks
                duelListener.removeDeathLocation(player.getUniqueId());
            } else {
                // Fallback if no stored location (shouldn't happen)
                event.setRespawnLocation(player.getLocation());
            }
            return;
        }
        
        // Handle regular matches
        Match match = plugin.getMatchManager().getPlayerMatch(player);
        
        if (match == null) {
            return; // Player not in a match
        }
        
        // For match players, we don't cancel the event but we set a custom respawn location
        // If player is eliminated, they should respawn at their death location
        if (match.isPlayerSpectator(player.getUniqueId())) {
            // Set respawn location to their current location (death location)
            // This prevents them from being teleported to world spawn
            event.setRespawnLocation(player.getLocation());
        }
    }
}
