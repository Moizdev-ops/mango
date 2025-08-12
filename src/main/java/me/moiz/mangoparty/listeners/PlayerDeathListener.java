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
import org.bukkit.event.entity.EntityDamageEvent;
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
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Only handle players in duels or matches
        boolean isInDuel = plugin.getDuelManager().isInDuel(player);
        boolean isInMatch = plugin.getMatchManager().getPlayerMatch(player) != null;
        
        if (!isInDuel && !isInMatch) {
            return; // Let vanilla death handling work for players not in duels/matches
        }
        
        // Check if player is invincible
        if (invincibilityTimers.containsKey(player.getUniqueId()) && 
            System.currentTimeMillis() < invincibilityTimers.get(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        
        // Check if player would die from this damage
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            // Cancel the damage event to prevent actual death
            event.setCancelled(true);
            
            // Check if player is in a duel
            if (isInDuel) {
                handleDuelPlayerDeath(player);
            }
            // Check if player is in a party match
            else if (isInMatch) {
                Match match = plugin.getMatchManager().getPlayerMatch(player);
                handlePartyPlayerDeath(player, match);
            }
        }
    }
    
    /**
     * Handle player deaths in duels without triggering the vanilla death event
     */
    private void handleDuelPlayerDeath(Player player) {
        // Store death location with exact yaw/pitch preserved
        final Location deathLocation = player.getLocation().clone();
        // Slightly raise Y coordinate to avoid spawning inside blocks
        deathLocation.setY(deathLocation.getY() + 0.1);
        
        // Play death animation without actually killing the player
        player.playEffect(org.bukkit.EntityEffect.DEATH);
        
        // Set health to full
        player.setHealth(20.0);
        
        // Clear inventory immediately
        if (!savedInventories.containsKey(player.getUniqueId())) {
            saveInventory(player);
        }
        player.getInventory().clear();
        
        // Make player invincible for 2 seconds
        player.setInvulnerable(true);
        invincibilityTimers.put(player.getUniqueId(), System.currentTimeMillis() + 2000);
        
        // Teleport player back to death location and handle death with 2 second delay
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.teleport(deathLocation);
                    // Handle the death in DuelManager
                    plugin.getDuelManager().handlePlayerDeath(player);
                }
            }
        }.runTaskLater(plugin, 40L); // Run after 2 seconds (40 ticks)
        
        // Remove invincibility after 2 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                player.setInvulnerable(false);
            }
        }.runTaskLater(plugin, 40L); // 2 seconds = 40 ticks
    }
    
    /**
     * Handle player deaths in party matches without triggering the vanilla death event
     */
    private void handlePartyPlayerDeath(Player player, Match match) {
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
        
        // Save inventory and clear it immediately
        if (!savedInventories.containsKey(player.getUniqueId())) {
            saveInventory(player);
        }
        player.getInventory().clear();
        
        // Make player a spectator after 2 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // Keep player at death location
                    player.teleport(deathLocation);
                    // Make player a spectator
                    makeSpectator(player);
                }
            }
        }.runTaskLater(plugin, 40L); // Run after 2 seconds (40 ticks)
        
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
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Prevent drops and exp loss for all plugin-managed players
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Check if player is in a duel
        if (plugin.getDuelManager().isInDuel(player)) {
            // Cancel the death event to prevent respawn
            player.setHealth(20.0);
            
            // Handle the death manually
            handleDuelPlayerDeath(player);
        }
        // Check if player is in a party match
        else if (plugin.getMatchManager().getPlayerMatch(player) != null) {
            Match match = plugin.getMatchManager().getPlayerMatch(player);
            
            // Cancel the death event to prevent respawn
            player.setHealth(20.0);
            
            // Handle the death manually
            handlePartyPlayerDeath(player, match);
        }
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
