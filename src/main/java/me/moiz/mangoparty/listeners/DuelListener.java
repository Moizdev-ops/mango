package me.moiz.mangoparty.listeners;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.managers.DuelManager;
import me.moiz.mangoparty.models.Duel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DuelListener implements Listener {
    private final MangoParty plugin;
    private final DuelManager duelManager;
    
    public DuelListener(MangoParty plugin) {
        this.plugin = plugin;
        this.duelManager = plugin.getDuelManager();
    }
    
    /**
     * Handle player death in a duel
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
            
            // Handle duel death
            duelManager.handlePlayerDeath(player);
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