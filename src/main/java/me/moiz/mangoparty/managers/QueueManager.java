package me.moiz.mangoparty.managers;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.models.Match;
import me.moiz.mangoparty.models.Party;
import me.moiz.mangoparty.models.QueueEntry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueueManager {
    private MangoParty plugin;
    private Map<UUID, QueueEntry> queuedPlayers; // Player UUID -> Queue Entry
    private Map<String, Map<String, List<UUID>>> queues; // Mode -> Kit -> List of Player UUIDs
    
    public QueueManager(MangoParty plugin) {
        this.plugin = plugin;
        this.queuedPlayers = new ConcurrentHashMap<>();
        this.queues = new ConcurrentHashMap<>();
        
        // Initialize queues for each mode
        queues.put("1v1", new ConcurrentHashMap<>());
        queues.put("2v2", new ConcurrentHashMap<>());
        queues.put("3v3", new ConcurrentHashMap<>());
    }
    
    public boolean joinQueue(Player player, String mode, String kitName) {
        // Check if player is already in a queue
        if (queuedPlayers.containsKey(player.getUniqueId())) {
            leaveQueue(player);
        }
        
        // Check if player is in a party (for team modes)
        if (("2v2".equals(mode) || "3v3".equals(mode)) && !plugin.getPartyManager().hasParty(player)) {
            player.sendMessage("§cYou need to be in a party to join " + mode + " queue!");
            return false;
        }
        
        // Check party size for team modes
        if ("2v2".equals(mode)) {
            Party party = plugin.getPartyManager().getParty(player);
            if (party != null && party.getSize() != 2) {
                player.sendMessage("§cYour party must have exactly 2 members for 2v2 queue!");
                return false;
            }
        } else if ("3v3".equals(mode)) {
            Party party = plugin.getPartyManager().getParty(player);
            if (party != null && party.getSize() != 3) {
                player.sendMessage("§cYour party must have exactly 3 members for 3v3 queue!");
                return false;
            }
        }
        
        // Check if player is already in a match
        if (plugin.getMatchManager().isInMatch(player)) {
            player.sendMessage("§cYou are already in a match!");
            return false;
        }
        
        // Verify kit exists
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            player.sendMessage("§cKit not found!");
            return false;
        }
        
        // Add to queue
        QueueEntry entry = new QueueEntry(player.getUniqueId(), mode, kitName);
        queuedPlayers.put(player.getUniqueId(), entry);
        
        queues.get(mode).computeIfAbsent(kitName, k -> new ArrayList<>()).add(player.getUniqueId());
        
        player.sendMessage("§aJoined " + mode + " queue with kit " + kit.getDisplayName() + "!");
        
        // Check if we can start a match
        checkForMatch(mode, kitName);
        
        return true;
    }
    
    public boolean leaveQueue(Player player) {
        QueueEntry entry = queuedPlayers.remove(player.getUniqueId());
        if (entry == null) {
            player.sendMessage("§cYou are not in any queue!");
            return false;
        }
        
        // Remove from specific queue
        List<UUID> queue = queues.get(entry.getMode()).get(entry.getKitName());
        if (queue != null) {
            queue.remove(player.getUniqueId());
            if (queue.isEmpty()) {
                queues.get(entry.getMode()).remove(entry.getKitName());
            }
        }
        
        player.sendMessage("§cLeft " + entry.getMode() + " queue.");
        return true;
    }
    
    public void removePlayer(UUID playerId) {
        QueueEntry entry = queuedPlayers.remove(playerId);
        if (entry != null) {
            List<UUID> queue = queues.get(entry.getMode()).get(entry.getKitName());
            if (queue != null) {
                queue.remove(playerId);
                if (queue.isEmpty()) {
                    queues.get(entry.getMode()).remove(entry.getKitName());
                }
            }
        }
    }
    
    public int getQueueCount(String mode, String kitName) {
        List<UUID> queue = queues.get(mode).get(kitName);
        return queue != null ? queue.size() : 0;
    }
    
    public boolean isInQueue(Player player) {
        return queuedPlayers.containsKey(player.getUniqueId());
    }
    
    public QueueEntry getQueueEntry(Player player) {
        return queuedPlayers.get(player.getUniqueId());
    }
    
    private void checkForMatch(String mode, String kitName) {
        List<UUID> queue = queues.get(mode).get(kitName);
        if (queue == null) return;
        
        int requiredPlayers = getRequiredPlayers(mode);
        if (queue.size() < requiredPlayers) return;
        
        // Get players for the match
        List<UUID> matchPlayers = new ArrayList<>();
        for (int i = 0; i < requiredPlayers && i < queue.size(); i++) {
            matchPlayers.add(queue.get(i));
        }
        
        // Remove players from queue
        for (UUID playerId : matchPlayers) {
            queue.remove(playerId);
            queuedPlayers.remove(playerId);
        }
        
        if (queue.isEmpty()) {
            queues.get(mode).remove(kitName);
        }
        
        // Start the match
        startQueueMatch(mode, kitName, matchPlayers);
    }
    
    private int getRequiredPlayers(String mode) {
        switch (mode) {
            case "1v1": return 2;
            case "2v2": return 4;
            case "3v3": return 6;
            default: return 2;
        }
    }
    
    private void startQueueMatch(String mode, String kitName, List<UUID> playerIds) {
        // Get online players
        List<Player> players = new ArrayList<>();
        for (UUID playerId : playerIds) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        
        if (players.size() < getRequiredPlayers(mode)) {
            // Not enough players online, re-queue the online ones
            for (Player player : players) {
                joinQueue(player, mode, kitName);
            }
            return;
        }
        
        // Get arena and kit
        Arena arena = plugin.getArenaManager().getAvailableArena();
        Kit kit = plugin.getKitManager().getKit(kitName);
        
        if (arena == null) {
            // No available arena, re-queue players
            for (Player player : players) {
                player.sendMessage("§cNo available arenas! You have been re-queued.");
                joinQueue(player, mode, kitName);
            }
            return;
        }
        
        if (kit == null) {
            for (Player player : players) {
                player.sendMessage("§cKit no longer available!");
            }
            return;
        }
        
        // Create a temporary party for the match
        Party matchParty = new Party(players.get(0).getUniqueId());
        for (int i = 1; i < players.size(); i++) {
            matchParty.addMember(players.get(i).getUniqueId());
        }
        
        // Start the match
        plugin.getMatchManager().startQueueMatch(matchParty, arena, kit, mode, players);
        
        // Notify players
        for (Player player : players) {
            player.sendMessage("§aMatch found! Starting " + mode + " with kit " + kit.getDisplayName());
        }
    }
    
    public void cleanup() {
        queuedPlayers.clear();
        for (Map<String, List<UUID>> modeQueues : queues.values()) {
            modeQueues.clear();
        }
    }
}
