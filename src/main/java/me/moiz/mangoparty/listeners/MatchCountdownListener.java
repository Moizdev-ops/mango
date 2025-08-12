package me.moiz.mangoparty.listeners;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class MatchCountdownListener implements Listener {
    private MangoParty plugin;
    
    public MatchCountdownListener(MangoParty plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getPlayerMatch(player);
        
        // Prevent dropping items if player is in a match that's in preparation state
        if (match != null && match.getState() == Match.MatchState.PREPARING) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot drop items during the countdown!");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only cancel significant movement (not just looking around)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getPlayerMatch(player);
        
        // Prevent movement if player is in a match that's in preparation state
        if (match != null && match.getState() == Match.MatchState.PREPARING) {
            // Allow movement if player has normal walk speed (countdown ended)
            if (player.getWalkSpeed() == 0.0f) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getPlayerMatch(player);
        
        // Check if player is in a match that's in preparation state
        if (match != null && match.getState() == Match.MatchState.PREPARING) {
            // Check if player is trying to use an ender pearl
            ItemStack item = event.getItem();
            if (item != null && item.getType().name().contains("ENDER_PEARL")) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot use ender pearls during the countdown!");
                return;
            }
            
            // Allow all other interactions during countdown (including crossbow loading)
            // We want to allow inventory organization and crossbow loading during the countdown
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        Match match = plugin.getMatchManager().getPlayerMatch(player);
        
        // Check if player is in a match that's in preparation state
        if (match != null && match.getState() == Match.MatchState.PREPARING || 
            match != null && match.getState() == Match.MatchState.COUNTDOWN) {
            
            // Prevent fire damage during countdown
            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE || 
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || 
                event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                
                // Cancel the damage event
                event.setCancelled(true);
                
                // Extinguish the player
                player.setFireTicks(0);
            }
        }
    }
}