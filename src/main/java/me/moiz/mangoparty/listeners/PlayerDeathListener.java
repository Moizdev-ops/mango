package me.moiz.mangoparty.listeners;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Duel;
import me.moiz.mangoparty.models.Match;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDeathListener implements Listener {
    private MangoParty plugin;
    private Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private Map<UUID, ItemStack> savedOffhand = new HashMap<>();
    private Map<UUID, Long> invincibilityTimers = new HashMap<>();
    
    public PlayerDeathListener(MangoParty plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle fatal damage for players in duels or matches
     * For players not in duels or matches, vanilla death mechanics are preserved
     * IMPORTANT: This listener ONLY affects players in duels or matches
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        // Only handle player damage
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Check if player is in a duel
        boolean isInDuel = plugin.getDuelManager().isInDuel(player);
        // Check if player is in a match
        boolean isInMatch = plugin.getMatchManager().isInMatch(player);
        
        // IMPORTANT: Only handle damage for players in duels or matches
        // This ensures vanilla mechanics work for all other players
        if (!isInDuel && !isInMatch) {
            return; // Let vanilla handle damage for non-match players
        }
        
        // Debug information - only log if debug is enabled
        boolean debug = plugin.getConfig().getBoolean("debug", false);
        if (debug) {
            plugin.getLogger().info("Custom death handling for player in " + 
                                  (isInDuel ? "duel" : "match") + ": " + player.getName());
        }
        
        // Check if player is invincible (for match/duel players only)
        if (invincibilityTimers.containsKey(player.getUniqueId()) && 
            System.currentTimeMillis() < invincibilityTimers.get(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        
        // Only handle fatal damage
        if ((player.getHealth() - event.getFinalDamage()) <= 0) {
            // Check for totem of undying BEFORE handling death
            boolean hasTotem = false;
            
            // Check main hand
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && mainHand.getType() == Material.TOTEM_OF_UNDYING) {
                hasTotem = true;
            }
            
            // Check off hand
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (offHand != null && offHand.getType() == Material.TOTEM_OF_UNDYING) {
                hasTotem = true;
            }
            
            // If player has totem, let vanilla handle it - don't cancel or handle manually
            if (hasTotem) {
                if (debug) {
                    plugin.getLogger().info("Player has totem, letting vanilla handle it: " + player.getName());
                }
                return; // Let totem activate naturally
            }
            
            // Cancel the damage event to prevent vanilla death
            event.setCancelled(true);
            
            // Handle death based on game mode
            if (isInDuel) {
                handleDuelPlayerDeath(player);
            } else if (isInMatch) {
                Match match = plugin.getMatchManager().getPlayerMatch(player);
                handlePartyPlayerDeath(player, match);
            }
        }
    }
    
    /**
     * Handle player deaths in duels - optimized for next round handling
     */
    private void handleDuelPlayerDeath(Player player) {
        // Play death animation
        player.playEffect(org.bukkit.EntityEffect.DEATH);
        
        // Set health to full immediately
        player.setHealth(20.0);
        
        // Clear inventory and make invincible
        player.getInventory().clear();
        player.setInvulnerable(true);
        
        // Handle death directly in DuelManager - no delays needed
        plugin.getDuelManager().handlePlayerDeath(player);
    }
    
    /**
     * Handle player deaths in party matches - optimized for spectator mode
     */
    private void handlePartyPlayerDeath(Player player, Match match) {
        // Handle killer if exists
        Player killer = player.getKiller();
        if (killer != null && plugin.getMatchManager().isInMatch(killer)) {
            match.addKill(killer.getUniqueId());
        }
        
        // Eliminate player from match and add to spectators
        plugin.getMatchManager().eliminatePlayer(player, match);
        
        // Send elimination message
        player.sendTitle("§c§lELIMINATED", "§7You are now spectating", 10, 40, 10);
        
        // Clear inventory and make spectator immediately
        player.getInventory().clear();
        makeSpectator(player);
        
        // Announce elimination to all match players
        for (Player matchPlayer : match.getAllPlayers()) {
            if (!matchPlayer.equals(player)) {
                String killerName = killer != null ? killer.getName() : "unknown causes";
                matchPlayer.sendMessage("§c" + player.getName() + " §7was eliminated by §c" + killerName);
            }
        }
        
        // Update scoreboard
        plugin.getScoreboardManager().updateMatchScoreboards(match);
    }
    
    /**
     * Handle totem of undying activation for players in duels/matches
     * This ensures proper handling after totem saves the player
     * For players not in duels or matches, vanilla resurrection mechanics are preserved
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Check if player is in a duel or match
        boolean isInDuel = plugin.getDuelManager().isInDuel(player);
        boolean isInMatch = plugin.getMatchManager().isInMatch(player);
        
        // IMPORTANT: Only handle resurrection for players in duels or matches
        // This ensures vanilla mechanics work for all other players
        if (!isInDuel && !isInMatch) {
            return; // Let vanilla handle resurrection for non-match players
        }
        
        // Allow totem to activate
        event.setCancelled(false);
        
        // After totem activates, handle the post-resurrection effects
        if (isInDuel) {
            // For duels, just ensure they're properly positioned
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20);
        } else if (isInMatch) {
            // For party matches, ensure they're still in the match
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20);
        }
    }

    /**
     * Make a player a spectator in plugin's custom spectator mode
     */
    private void makeSpectator(Player player) {
        // Set to survival mode but make invisible and allow flight
        player.setGameMode(GameMode.SURVIVAL);
        
        // Clear inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.updateInventory();
        
        // Make invisible to other players
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.equals(player)) {
                online.hidePlayer(plugin, player);
            }
        }
        
        // Allow flight
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Make invulnerable
        player.setInvulnerable(true);
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
