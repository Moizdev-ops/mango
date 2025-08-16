package me.moiz.mangoparty.managers;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages player queues for different match modes and kits.
 * Optimized for performance with concurrent collections and efficient matching algorithms.
 */
public class QueueManager {
    private final MangoParty plugin;
    private final Map<String, Map<String, List<QueueEntry>>> queues; // mode -> kit -> list of players
    private final Map<UUID, QueueEntry> playerQueues; // player UUID -> queue entry
    private final Map<String, Long> lastMatchAttempt; // mode_kit -> timestamp of last match attempt
    private final long MATCH_ATTEMPT_COOLDOWN = 500; // milliseconds between match attempts
    private BukkitTask queueCleanupTask;
    private BukkitTask matchmakingTask;
    
    /**
     * Constructs a new QueueManager.
     * 
     * @param plugin The MangoParty plugin instance
     */
    public QueueManager(MangoParty plugin) {
        this.plugin = plugin;
        this.queues = new ConcurrentHashMap<>();
        this.playerQueues = new ConcurrentHashMap<>();
        this.lastMatchAttempt = new ConcurrentHashMap<>();
        
        // Initialize queue maps for each mode
        queues.put("1v1", new ConcurrentHashMap<>());
        queues.put("2v2", new ConcurrentHashMap<>());
        queues.put("3v3", new ConcurrentHashMap<>());
        
        // Schedule periodic tasks
        scheduleQueueCleanup();
        scheduleMatchmaking();
    }
    
    /**
     * Adds a player to a queue for a specific mode and kit.
     * 
     * @param player The player to add to the queue
     * @param mode The queue mode (1v1, 2v2, 3v3)
     * @param kitName The name of the kit
     */
    public void joinQueue(Player player, String mode, String kitName) {
        if (player == null || mode == null || kitName == null) {
            return;
        }
        
        // Check if player is already in a queue
        if (playerQueues.containsKey(player.getUniqueId())) {
            leaveQueue(player);
        }
        
        // Check if player is in a match
        if (plugin.getMatchManager().isInMatch(player)) {
            player.sendMessage("§cYou cannot join a queue while in a match!");
            return;
        }
        
        // Verify kit exists
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            player.sendMessage("§cKit '" + kitName + "' not found!");
            return;
        }
        
        // Create queue entry
        QueueEntry entry = new QueueEntry(player, mode, kitName);
        
        // Add to queues - use LinkedList for better performance when removing elements
        queues.get(mode).computeIfAbsent(kitName, k -> Collections.synchronizedList(new LinkedList<>())).add(entry);
        playerQueues.put(player.getUniqueId(), entry);
        
        player.sendMessage("§aJoined " + mode + " queue with kit " + kit.getDisplayName());
        
        // Check if we can start a match - use a cooldown to prevent excessive checks
        String queueKey = mode + "_" + kitName;
        long currentTime = System.currentTimeMillis();
        Long lastAttempt = lastMatchAttempt.get(queueKey);
        
        if (lastAttempt == null || currentTime - lastAttempt > MATCH_ATTEMPT_COOLDOWN) {
            lastMatchAttempt.put(queueKey, currentTime);
            checkForMatch(mode, kitName);
        }
    }
    
    /**
     * Removes a player from their current queue.
     * 
     * @param player The player to remove from the queue
     */
    public void leaveQueue(Player player) {
        if (player == null) return;
        
        QueueEntry entry = playerQueues.remove(player.getUniqueId());
        if (entry != null) {
            Map<String, List<QueueEntry>> modeQueues = queues.get(entry.getMode());
            if (modeQueues != null) {
                List<QueueEntry> queueList = modeQueues.get(entry.getKitName());
                if (queueList != null) {
                    queueList.remove(entry);
                    if (queueList.isEmpty()) {
                        modeQueues.remove(entry.getKitName());
                    }
                }
            }
            player.sendMessage("§cLeft " + entry.getMode() + " queue");
        } else {
            player.sendMessage("§cYou are not in any queue!");
        }
    }
    
    /**
     * Removes a player from their current queue by UUID.
     * Used when a player disconnects or is otherwise unavailable.
     * 
     * @param playerId The UUID of the player to remove
     */
    public void removePlayer(UUID playerId) {
        if (playerId == null) return;
        
        QueueEntry entry = playerQueues.remove(playerId);
        if (entry != null) {
            Map<String, List<QueueEntry>> modeQueues = queues.get(entry.getMode());
            if (modeQueues != null) {
                List<QueueEntry> queueList = modeQueues.get(entry.getKitName());
                if (queueList != null) {
                    queueList.remove(entry);
                    if (queueList.isEmpty()) {
                        modeQueues.remove(entry.getKitName());
                    }
                }
            }
        }
    }
    
    /**
     * Checks if there are enough players in a queue to start a match.
     * 
     * @param mode The queue mode
     * @param kitName The kit name
     */
    private void checkForMatch(String mode, String kitName) {
        Map<String, List<QueueEntry>> modeQueues = queues.get(mode);
        if (modeQueues == null) return;
        
        List<QueueEntry> queueList = modeQueues.get(kitName);
        if (queueList == null || queueList.isEmpty()) return;
        
        int requiredPlayers = getRequiredPlayers(mode);
        
        // Synchronize on the queue list to prevent concurrent modification
        synchronized (queueList) {
            if (queueList.size() >= requiredPlayers) {
                // Get players for the match - first validate all players are still online
                List<QueueEntry> matchEntries = new ArrayList<>();
                Iterator<QueueEntry> iterator = queueList.iterator();
                int count = 0;
                
                while (iterator.hasNext() && count < requiredPlayers) {
                    QueueEntry entry = iterator.next();
                    Player player = entry.getPlayer();
                    
                    // Skip offline players or players in matches
                    if (!player.isOnline() || plugin.getMatchManager().isInMatch(player)) {
                        iterator.remove();
                        playerQueues.remove(player.getUniqueId());
                        continue;
                    }
                    
                    matchEntries.add(entry);
                    count++;
                }
                
                // If we don't have enough valid players, return
                if (matchEntries.size() < requiredPlayers) {
                    return;
                }
                
                // Remove players from queue
                for (QueueEntry entry : matchEntries) {
                    queueList.remove(entry);
                    playerQueues.remove(entry.getPlayer().getUniqueId());
                }
                
                // Clean up empty queue
                if (queueList.isEmpty()) {
                    modeQueues.remove(kitName);
                }
                
                // Start the match asynchronously to avoid blocking the main thread
                Bukkit.getScheduler().runTask(plugin, () -> startQueueMatch(mode, kitName, matchEntries));
            }
        }
    }
    
    /**
     * Starts a match with players from the queue.
     * 
     * @param mode The queue mode
     * @param kitName The kit name
     * @param entries The queue entries for players in the match
     */
    private void startQueueMatch(String mode, String kitName, List<QueueEntry> entries) {
        if (entries == null || entries.isEmpty()) return;
        
        // Verify all players are still online
        Iterator<QueueEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            QueueEntry entry = iterator.next();
            if (!entry.getPlayer().isOnline()) {
                iterator.remove();
            }
        }
        
        // If we don't have enough players after verification, return them to queue
        int requiredPlayers = getRequiredPlayers(mode);
        if (entries.size() < requiredPlayers) {
            for (QueueEntry entry : entries) {
                if (entry.getPlayer().isOnline()) {
                    queues.get(mode).computeIfAbsent(kitName, k -> Collections.synchronizedList(new LinkedList<>())).add(entry);
                    playerQueues.put(entry.getPlayer().getUniqueId(), entry);
                    entry.getPlayer().sendMessage("§cNot enough players for match! Returned to queue.");
                }
            }
            return;
        }
        
        // Try to get an arena that supports this kit
        Arena arena = plugin.getArenaManager().getAvailableArenaForKit(kitName);
        if (arena == null) {
            // Fall back to any available arena if no kit-specific arena is available
            arena = plugin.getArenaManager().getAvailableArena();
        }
        
        if (arena == null) {
            // Return players to queue if no arena available
            for (QueueEntry entry : entries) {
                queues.get(mode).computeIfAbsent(kitName, k -> Collections.synchronizedList(new LinkedList<>())).add(entry);
                playerQueues.put(entry.getPlayer().getUniqueId(), entry);
                entry.getPlayer().sendMessage("§cNo available arenas! Returned to queue.");
            }
            return;
        }
        
        // Get kit
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            for (QueueEntry entry : entries) {
                entry.getPlayer().sendMessage("§cKit not found! Removed from queue.");
            }
            return;
        }
        
        // Create temporary party for the match
        List<Player> players = new ArrayList<>();
        for (QueueEntry entry : entries) {
            players.add(entry.getPlayer());
        }
        
        // Create a temporary party for match management
        Party tempParty = new Party(players.get(0).getUniqueId());
        for (Player player : players) {
            tempParty.addMember(player.getUniqueId());
        }
        
        try {
            // Reserve the arena
            if (!plugin.getArenaManager().reserveArena(arena.getName())) {
                // Arena was reserved by another match, return players to queue
                for (QueueEntry entry : entries) {
                    queues.get(mode).computeIfAbsent(kitName, k -> Collections.synchronizedList(new LinkedList<>())).add(entry);
                    playerQueues.put(entry.getPlayer().getUniqueId(), entry);
                    entry.getPlayer().sendMessage("§cArena was taken! Returned to queue.");
                }
                return;
            }
            
            // Create match object
            String matchId = "queue_" + mode + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            Match match = new Match(matchId, tempParty, arena, kit, "queue_" + mode);
            
            // Start the match
            boolean matchStarted = plugin.getMatchManager().startQueueMatch(tempParty, arena, kit, mode, players, match);
            
            // Check if match was started successfully
            if (!matchStarted) {
                plugin.getLogger().warning("Failed to start queue match for mode: " + mode);
                
                // Return players to queue
                for (Player player : players) {
                    player.sendMessage("§cFailed to start match. Returned to queue.");
                    joinQueue(player, mode, kitName);
                }
                return;
            }
            
            // Start scoreboards
            plugin.getScoreboardManager().startQueueMatchScoreboards(match);
            
            // Notify players
            for (Player player : players) {
                player.sendMessage("§aMatch found! Starting " + mode + " with kit: " + kit.getDisplayName());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error starting queue match", e);
            
            // Return players to queue on error
            for (QueueEntry entry : entries) {
                if (entry.getPlayer().isOnline()) {
                    queues.get(mode).computeIfAbsent(kitName, k -> Collections.synchronizedList(new LinkedList<>())).add(entry);
                    playerQueues.put(entry.getPlayer().getUniqueId(), entry);
                    entry.getPlayer().sendMessage("§cError starting match! Returned to queue.");
                }
            }
        }
    }
    
    /**
     * Gets the number of players required for a specific mode.
     * 
     * @param mode The queue mode
     * @return The number of players required
     */
    private int getRequiredPlayers(String mode) {
        if (mode == null) return 2;
        
        switch (mode) {
            case "1v1": return 2;
            case "2v2": return 4;
            case "3v3": return 6;
            default: return 2;
        }
    }
    
    /**
     * Gets the number of players in a specific queue.
     * 
     * @param mode The queue mode
     * @param kitName The kit name
     * @return The number of players in the queue
     */
    public int getQueueCount(String mode, String kitName) {
        if (mode == null || kitName == null) return 0;
        
        Map<String, List<QueueEntry>> modeQueues = queues.get(mode);
        if (modeQueues == null) return 0;
        
        List<QueueEntry> queueList = modeQueues.get(kitName);
        return queueList != null ? queueList.size() : 0;
    }
    
    /**
     * Checks if a player is in any queue.
     * 
     * @param player The player to check
     * @return True if the player is in a queue, false otherwise
     */
    public boolean isInQueue(Player player) {
        return player != null && playerQueues.containsKey(player.getUniqueId());
    }
    
    /**
     * Gets the queue entry for a player.
     * 
     * @param player The player to get the queue entry for
     * @return The queue entry, or null if the player is not in a queue
     */
    public QueueEntry getPlayerQueue(Player player) {
        return player != null ? playerQueues.get(player.getUniqueId()) : null;
    }
    
    /**
     * Schedules a task to clean up stale queue entries.
     * Removes offline players from queues.
     */
    private void scheduleQueueCleanup() {
        queueCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Remove offline players from queues
            Set<UUID> toRemove = new HashSet<>();
            
            for (Map.Entry<UUID, QueueEntry> entry : playerQueues.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) {
                    toRemove.add(entry.getKey());
                }
            }
            
            for (UUID uuid : toRemove) {
                removePlayer(uuid);
            }
            
            if (!toRemove.isEmpty()) {
                plugin.getLogger().fine("Removed " + toRemove.size() + " offline players from queues");
            }
        }, 20 * 30, 20 * 30); // Run every 30 seconds
    }
    
    /**
     * Schedules a task to periodically check for matches.
     * This ensures matches are created even if players join at different times.
     */
    private void scheduleMatchmaking() {
        matchmakingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Check all queues for potential matches
            for (String mode : queues.keySet()) {
                Map<String, List<QueueEntry>> modeQueues = queues.get(mode);
                if (modeQueues != null) {
                    for (String kitName : new HashSet<>(modeQueues.keySet())) {
                        checkForMatch(mode, kitName);
                    }
                }
            }
        }, 20 * 5, 20 * 5); // Run every 5 seconds
    }
    
    /**
     * Cleans up all queues and cancels scheduled tasks.
     * Called when the plugin is disabled.
     */
    public void cleanup() {
        // Cancel scheduled tasks
        if (queueCleanupTask != null) {
            queueCleanupTask.cancel();
            queueCleanupTask = null;
        }
        
        if (matchmakingTask != null) {
            matchmakingTask.cancel();
            matchmakingTask = null;
        }
        
        // Clear all queues
        queues.clear();
        playerQueues.clear();
        lastMatchAttempt.clear();
    }
}
