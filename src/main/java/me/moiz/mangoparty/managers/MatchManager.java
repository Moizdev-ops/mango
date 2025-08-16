package me.moiz.mangoparty.managers;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.models.Match;
import me.moiz.mangoparty.models.Party;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all match-related functionality including creation, tracking, and cleanup of matches.
 * Optimized for performance with concurrent collections and efficient arena management.
 */
public class MatchManager {
    private final MangoParty plugin;
    private final Map<String, Match> activeMatches;
    private final Map<UUID, String> playerMatches; // Player UUID -> Match ID
    private final Map<String, BukkitTask> countdownTasks; // Match ID -> Task
    private final Map<String, Arena> arenaCache; // Cache for frequently accessed arenas
    
    /**
     * Constructs a new MatchManager.
     * 
     * @param plugin The MangoParty plugin instance
     */
    public MatchManager(MangoParty plugin) {
        this.plugin = plugin;
        this.activeMatches = new ConcurrentHashMap<>();
        this.playerMatches = new ConcurrentHashMap<>();
        this.countdownTasks = new ConcurrentHashMap<>();
        this.arenaCache = new ConcurrentHashMap<>();
        
        // Schedule periodic cleanup of stale matches
        scheduleMatchCleanup();
    }
    
    /**
     * Clear all entities and drops within an arena's boundaries
     */
    private void clearArenaEntities(Arena arena) {
        if (arena == null || arena.getCorner1() == null || arena.getCorner2() == null) {
            return;
        }
        
        // Get arena boundaries
        Location corner1 = arena.getCorner1();
        Location corner2 = arena.getCorner2();
        double minX = Math.min(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());
        
        // Get all entities in the world
        corner1.getWorld().getEntities().forEach(entity -> {
            // Skip players
            if (entity instanceof Player) {
                return;
            }
            
            // Check if entity is within arena boundaries
            Location loc = entity.getLocation();
            if (loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ) {
                // Remove the entity
                entity.remove();
            }
        });
    }

    /**
     * Gets the match a player is currently in.
     * 
     * @param player The player to check
     * @return The match the player is in, or null if not in a match
     */
    public Match getPlayerMatch(Player player) {
        if (player == null) return null;
        
        String matchId = playerMatches.get(player.getUniqueId());
        return matchId != null ? activeMatches.get(matchId) : null;
    }

    /**
     * Gets all currently active matches.
     * 
     * @return Collection of all active matches
     */
    public Collection<Match> getAllActiveMatches() {
        return Collections.unmodifiableCollection(activeMatches.values());
    }

    /**
     * Eliminates a player from a match and checks if the match is finished.
     * 
     * @param player The player to eliminate
     * @param match The match the player is in
     */
    public void eliminatePlayer(Player player, Match match) {
        if (player == null || match == null) return;
        
        match.eliminatePlayer(player.getUniqueId());
        
        // Check if match is finished
        if (match.isFinished()) {
            endMatch(match);
        }
    }

    /**
     * Generates a unique match ID.
     * 
     * @return A unique match ID string
     */
    private String generateMatchId() {
        return "match_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Schedules periodic cleanup of stale matches to prevent memory leaks.
     */
    private void scheduleMatchCleanup() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupStaleMatches();
            }
        }.runTaskTimer(plugin, 20 * 60 * 5, 20 * 60 * 5); // Run every 5 minutes
    }
    
    /**
     * Cleans up matches that have been inactive for too long.
     */
    /**
     * Cleans up matches that have been inactive for too long.
     * Uses the new lastActivityTime tracking in the Match class for more accurate cleanup.
     */
    private void cleanupStaleMatches() {
        final long MAX_MATCH_DURATION = 30 * 60 * 1000; // 30 minutes
        final long MAX_INACTIVE_DURATION = 10 * 60 * 1000; // 10 minutes of inactivity
        
        List<String> matchesToRemove = new ArrayList<>();
        
        for (Map.Entry<String, Match> entry : activeMatches.entrySet()) {
            Match match = entry.getValue();
            
            // End match if it's been running too long or inactive too long
            if ((match.getStartTime() > 0 && System.currentTimeMillis() - match.getStartTime() > MAX_MATCH_DURATION) || 
                match.isInactiveLongerThan(MAX_INACTIVE_DURATION)) {
                matchesToRemove.add(entry.getKey());
            }
        }
        
        for (String matchId : matchesToRemove) {
            Match match = activeMatches.get(matchId);
            if (match != null) {
                plugin.getLogger().info("Cleaning up stale match: " + matchId);
                endMatch(match);
            }
        }
    }
    
    /**
     * Prepares and starts a match for a player with the specified kit and match type.
     * 
     * @param player The player initiating the match
     * @param kit The kit to use for the match
     * @param matchType The type of match (e.g., "ffa", "split")
     */
    public void startMatchPreparation(Player player, Kit kit, String matchType) {
        if (player == null || kit == null || matchType == null) {
            plugin.getLogger().log(Level.WARNING, "Invalid parameters for match preparation");
            return;
        }
        
        try {
            // Get or create party
            Party party = plugin.getPartyManager().getParty(player);
            if (party == null) {
                // Create a temporary party for the single player
                party = plugin.getPartyManager().createParty(player);
                if (party == null) {
                    player.sendMessage("§cFailed to create a temporary party. Please try again.");
                    return;
                }
            }
            
            // Find suitable arena using optimized method
            Arena arena = findSuitableArena(kit.getName());
            if (arena == null) {
                player.sendMessage("§cNo available arenas for this kit. Please try again later.");
                return;
            }
            
            // Start the match
            startMatch(party, arena, kit, matchType);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error starting match preparation", e);
            player.sendMessage("§cAn error occurred while preparing the match. Please try again.");
        }
    }
    
    /**
     * Finds a suitable arena for a kit, with caching for performance.
     * 
     * @param kitName The name of the kit
     * @return A suitable arena, or null if none available
     */
    private Arena findSuitableArena(String kitName) {
        // Check cache first
        String cacheKey = "arena_for_kit_" + kitName;
        Arena cachedArena = arenaCache.get(cacheKey);
        
        if (cachedArena != null && !plugin.getArenaManager().isArenaReserved(cachedArena.getName())) {
            return cachedArena;
        }
        
        // Find an available arena that allows this kit
        Arena arena = plugin.getArenaManager().getAvailableArenaForKit(kitName);
        
        // If no available arena found, create an instance of the original arena
        if (arena == null) {
            // Find a base arena that allows this kit
            Arena baseArena = null;
            for (Arena a : plugin.getArenaManager().getArenas().values()) {
                if (a.isComplete() && a.isKitAllowed(kitName) && !a.isInstance()) {
                    baseArena = a;
                    break;
                }
            }
            
            // If we found a base arena, create an instance
            if (baseArena != null) {
                arena = plugin.getArenaManager().createArenaInstance(baseArena, kitName);
            }
        }
        
        // Update cache if we found an arena
        if (arena != null) {
            arenaCache.put(cacheKey, arena);
        }
        
        return arena;
    }

    /**
     * Starts a match with the specified party, arena, kit, and match type.
     * 
     * @param party The party participating in the match
     * @param arena The arena where the match takes place
     * @param kit The kit to use for the match
     * @param matchType The type of match (e.g., "ffa", "split")
     * @return The created match, or null if the match could not be started
     */
    public Match startMatch(Party party, Arena arena, Kit kit, String matchType) {
        if (party == null || arena == null || kit == null || matchType == null) {
            plugin.getLogger().warning("Cannot start match: Missing required parameters");
            return null;
        }
        
        if (party.isInMatch()) {
            plugin.getLogger().info("Party is already in a match");
            return null; // Party already in match
        }
        
        List<Player> players = party.getOnlineMembers();
        if (players.isEmpty()) {
            plugin.getLogger().info("No online players in party");
            return null;
        }
        
        // Check if kit is allowed in this arena
        if (kit != null && !arena.isKitAllowed(kit.getName())) {
            // Try to find an available arena that allows this kit
            Arena availableArena = plugin.getArenaManager().getAvailableArenaForKit(kit.getName());
            
            // If no available arena found, create an instance of the original arena
            if (availableArena == null) {
                // Find a base arena that allows this kit
                Arena baseArena = null;
                for (Arena a : plugin.getArenaManager().getArenas().values()) {
                    if (a.isComplete() && a.isKitAllowed(kit.getName()) && !a.isInstance()) {
                        baseArena = a;
                        break;
                    }
                }
                
                // If we found a base arena, create an instance
                if (baseArena != null) {
                    availableArena = plugin.getArenaManager().createArenaInstance(baseArena, kit.getName());
                }
            }
            
            // If we found or created an available arena, use it
            if (availableArena != null) {
                arena = availableArena;
            }
        }
        
        // Clear arena entities before reserving
        clearArenaEntities(arena);
        
        // Reserve the arena
        plugin.getArenaManager().reserveArena(arena.getName());
        
        // Create match object
        String matchId = generateMatchId();
        Match match = Match.createWithRandomId(party, arena, kit, matchType);
        
        // Assign teams if split mode
        if ("split".equalsIgnoreCase(matchType)) {
            if (!match.assignTeams()) {
                plugin.getLogger().warning("Failed to assign teams for match: " + matchId);
                return null;
            }
        }
        
        // Store match
        activeMatches.put(matchId, match);
        for (Player player : players) {
            playerMatches.put(player.getUniqueId(), matchId);
        }
        
        // Set gamerule for immediate respawn and keep it true
        arena.getCenter().getWorld().setGameRuleValue("doImmediateRespawn", "true");
        
        // Regenerate arena
        plugin.getArenaManager().pasteSchematic(arena);
        
        // Set party as in match
        party.setInMatch(true);
        match.setState(Match.MatchState.PREPARING);
        match.updateLastActivityTime();
        
        // Heal and feed all players
        for (Player player : players) {
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }
        
        // Teleport players based on match type
        if ("split".equalsIgnoreCase(matchType)) {
            startSplitMatch(players, arena, match);
        } else if ("ffa".equalsIgnoreCase(matchType)) {
            startFFAMatch(players, arena, match);
        }
        
        // Start countdown
        startCountdown(match);
        
        return match;
    }
    
    /**
     * Starts a split match by teleporting players to their team spawns.
     * 
     * @param players The players participating in the match
     * @param arena The arena where the match takes place
     * @param match The match object
     */
    private void startSplitMatch(List<Player> players, Arena arena, Match match) {
        if (players == null || arena == null || match == null) return;
        
        // Teleport teams to their spawns based on match team assignments
        for (Player player : players) {
            if (player == null || !player.isOnline()) continue;
            
            int team = match.getPlayerTeam(player.getUniqueId());
            if (team == 1 && arena.getSpawn1() != null) {
                player.teleport(arena.getSpawn1());
            } else if (team == 2 && arena.getSpawn2() != null) {
                player.teleport(arena.getSpawn2());
            } else {
                // Fallback to center if team is invalid or spawn is null
                player.teleport(arena.getCenter());
            }
            // Freeze player during countdown
            player.setWalkSpeed(0.0f);
        }
        
        // Start 5 second countdown
        new BukkitRunnable() {
            int countdown = 5;
            
            @Override
            public void run() {
                if (countdown > 0) {
                    for (Player player : players) {
                        if (player.isOnline()) {
                            player.sendTitle("§6§lMatch starts in", "§e" + countdown + " seconds", 0, 20, 0);
                        }
                    }
                    countdown--;
                } else {
                    // Unfreeze players and remove invincibility
                    for (Player player : players) {
                        if (player.isOnline()) {
                            player.setWalkSpeed(0.2f); // Default walk speed
                            player.setInvulnerable(false); // Remove invincibility when match starts
                            player.sendTitle("§c§lFIGHT!", "", 0, 20, 0);
                        }
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second
    }
    
    /**
     * Starts a FFA match by teleporting all players to the arena center.
     * 
     * @param players The players participating in the match
     * @param arena The arena where the match takes place
     * @param match The match object for activity tracking
     */
    private void startFFAMatch(List<Player> players, Arena arena, Match match) {
        if (players == null || arena == null || match == null) return;
        
        // Teleport all players to center
        for (Player player : players) {
            if (player == null || !player.isOnline()) continue;
            
            player.teleport(arena.getCenter());
            // Freeze player during countdown
            player.setWalkSpeed(0.0f);
        }
        
        // Update match activity time
        match.updateLastActivityTime();
        
        // Start 5 second countdown
        new BukkitRunnable() {
            int countdown = 5;
            
            @Override
            public void run() {
                if (countdown > 0) {
                    for (Player player : players) {
                        if (player.isOnline()) {
                            player.sendTitle("§6§lMatch starts in", "§e" + countdown + " seconds", 0, 20, 0);
                        }
                    }
                    countdown--;
                } else {
                    // Unfreeze players and remove invincibility
                    for (Player player : players) {
                        if (player.isOnline()) {
                            player.setWalkSpeed(0.2f); // Default walk speed
                            player.setInvulnerable(false); // Remove invincibility when match starts
                            player.sendTitle("§c§lFIGHT!", "", 0, 20, 0);
                        }
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second
    }

    /**
     * Starts a party vs party match with the specified match object and parties.
     * 
     * @param match The match object
     * @param party1 The first party (team 1)
     * @param party2 The second party (team 2)
     * @return true if the match was started successfully, false otherwise
     */
    public boolean startPartyVsPartyMatch(Match match, Party party1, Party party2) {
        if (match == null || party1 == null || party2 == null) {
            plugin.getLogger().warning("Cannot start party vs party match: Missing required parameters");
            return false;
        }
        
        // Assign teams for party vs party
        if (!match.assignPartyVsPartyTeams(party1, party2)) {
            plugin.getLogger().warning("Failed to assign teams for party vs party match");
            return false;
        }
        
        List<Player> allPlayers = match.getAllPlayers();
        if (allPlayers.isEmpty()) {
            plugin.getLogger().warning("No players found for party vs party match");
            return false;
        }
        
        // Regenerate arena
        plugin.getArenaManager().pasteSchematic(match.getArena());
        
        Arena arena = match.getArena();
        Kit kit = match.getKit();
        
        // Check if kit is allowed in this arena
        if (kit != null && !arena.isKitAllowed(kit.getName())) {
            // Try to find an available arena that allows this kit
            Arena availableArena = plugin.getArenaManager().getAvailableArenaForKit(kit.getName());
            
            // If no available arena found, create an instance of the original arena
            if (availableArena == null) {
                // Find a base arena that allows this kit
                Arena baseArena = null;
                for (Arena a : plugin.getArenaManager().getArenas().values()) {
                    if (a.isComplete() && a.isKitAllowed(kit.getName()) && !a.isInstance()) {
                        baseArena = a;
                        break;
                    }
                }
                
                // If we found a base arena, create an instance
                if (baseArena != null) {
                    availableArena = plugin.getArenaManager().createArenaInstance(baseArena, kit.getName());
                }
            }
            
            // If we found or created an available arena, use it
            if (availableArena != null) {
                arena = availableArena;
                match.setArena(arena);
            }
        }
        
        // Reserve the arena
        plugin.getArenaManager().reserveArena(arena.getName());
        
        // Store match
        activeMatches.put(match.getId(), match);
        for (Player player : allPlayers) {
            playerMatches.put(player.getUniqueId(), match.getId());
        }
        
        // Set gamerule for immediate respawn
        arena.getCenter().getWorld().setGameRuleValue("doImmediateRespawn", "true");
        
        // Regenerate arena
        plugin.getArenaManager().pasteSchematic(arena);
        
        match.setState(Match.MatchState.PREPARING);
        match.updateLastActivityTime();
        
        // Heal and feed all players
        for (Player player : allPlayers) {
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }
        
        // Teleport teams to their spawns
        for (Player player : allPlayers) {
            int team = match.getPlayerTeam(player.getUniqueId());
            if (team == 1) {
                player.teleport(arena.getSpawn1());
            } else if (team == 2) {
                player.teleport(arena.getSpawn2());
            }
        }
        
        // Start countdown
        startCountdown(match);
        
        return true;
    }

    /**
     * Starts a queue match with the specified parameters.
     * 
     * @param matchParty The temporary party for the match
     * @param arena The arena where the match takes place
     * @param kit The kit to use for the match
     * @param mode The queue mode (e.g., "1v1", "2v2")
     * @param players The players participating in the match
     * @param match The match object created by QueueManager
     * @return true if the match was successfully started, false otherwise
     */
    public boolean startQueueMatch(Party matchParty, Arena arena, Kit kit, String mode, List<Player> players, Match match) {
        if (matchParty == null || arena == null || kit == null || mode == null || players == null || players.isEmpty() || match == null) {
            plugin.getLogger().warning("Cannot start queue match: Missing required parameters");
            return false;
        }
        
        // Check if kit is allowed in this arena
        if (kit != null && !arena.isKitAllowed(kit.getName())) {
            // Try to find an available arena that allows this kit
            Arena availableArena = plugin.getArenaManager().getAvailableArenaForKit(kit.getName());
            
            // If no available arena found, create an instance of the original arena
            if (availableArena == null) {
                // Find a base arena that allows this kit
                Arena baseArena = null;
                for (Arena a : plugin.getArenaManager().getArenas().values()) {
                    if (a.isComplete() && a.isKitAllowed(kit.getName()) && !a.isInstance()) {
                        baseArena = a;
                        break;
                    }
                }
                
                // If we found a base arena, create an instance
                if (baseArena != null) {
                    availableArena = plugin.getArenaManager().createArenaInstance(baseArena, kit.getName());
                }
            }
            
            // If we found or created an available arena, use it
            if (availableArena != null) {
                arena = availableArena;
                match.setArena(arena);
            }
        }
        
        // Reserve the arena
        plugin.getArenaManager().reserveArena(arena.getName());
        
        // Use match object from QueueManager
        
        // Assign teams based on mode
        if (!assignQueueTeams(match, players, mode)) {
            plugin.getLogger().warning("Failed to assign teams for queue match: " + match.getId());
            return false;
        }
        
        // Store match
        activeMatches.put(match.getId(), match);
        for (Player player : players) {
            playerMatches.put(player.getUniqueId(), match.getId());
        }
        
        // Set gamerule for immediate respawn
        arena.getCenter().getWorld().setGameRuleValue("doImmediateRespawn", "true");
        
        // Regenerate arena
        plugin.getArenaManager().pasteSchematic(arena);
        
        match.setState(Match.MatchState.PREPARING);
        
        // Heal and feed all players
        for (Player player : players) {
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }
        
        // Teleport players based on teams
        for (Player player : players) {
            int team = match.getPlayerTeam(player.getUniqueId());
            if (team == 1) {
                player.teleport(arena.getSpawn1());
            } else if (team == 2) {
                player.teleport(arena.getSpawn2());
            }
        }
        
        // Start countdown
        startCountdown(match);
        
        return true;
    }

    /**
     * Assigns players to teams for a queue match based on the mode.
     * 
     * @param match The match object
     * @param players The players to assign to teams
     * @param mode The queue mode (e.g., "1v1", "2v2")
     * @return true if teams were successfully assigned, false otherwise
     */
    private boolean assignQueueTeams(Match match, List<Player> players, String mode) {
        if (match == null || players == null || players.isEmpty() || mode == null) {
            return false;
        }
        
        // Clear existing team assignments and cache
        match.getPlayerTeams().clear();
        match.invalidateTeamCache();
        
        Collections.shuffle(players); // Randomize teams
        
        int playersPerTeam = getPlayersPerTeam(mode);
        List<UUID> team1Players = new ArrayList<>();
        List<UUID> team2Players = new ArrayList<>();
        
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i) == null || !players.get(i).isOnline()) continue;
            
            UUID playerId = players.get(i).getUniqueId();
            int team = (i < playersPerTeam) ? 1 : 2;
            match.getPlayerTeams().put(playerId, team);
            
            // Add to the appropriate team list
            if (team == 1) {
                team1Players.add(playerId);
            } else {
                team2Players.add(playerId);
            }
        }
        
        // Update team cache
        if (!team1Players.isEmpty()) {
            match.getTeamCache().put(1, team1Players);
        }
        
        if (!team2Players.isEmpty()) {
            match.getTeamCache().put(2, team2Players);
        }
        
        return !team1Players.isEmpty() && !team2Players.isEmpty();
    }

    private int getPlayersPerTeam(String mode) {
        switch (mode) {
            case "1v1": return 1;
            case "2v2": return 2;
            case "3v3": return 3;
            default: return 1;
        }
    }
    
    /**
     * Starts the countdown for a match, giving players their kits and preparing them for the fight.
     * 
     * @param match The match to start the countdown for
     */
    private void startCountdown(Match match) {
        if (match == null) return;
        
        List<Player> players = match.getAllPlayers();
        if (players.isEmpty()) {
            plugin.getLogger().warning("No players found for match countdown: " + match.getId());
            endMatch(match);
            return;
        }
        
        match.setState(Match.MatchState.COUNTDOWN);
        match.updateLastActivityTime();
        
        // Give kits BEFORE countdown starts
        for (Player player : players) {
            player.setGameMode(GameMode.ADVENTURE);
            player.setWalkSpeed(0f);
            player.setFlySpeed(0f);
            
            // Give kit immediately
            plugin.getKitManager().giveKit(player, match.getKit());
        }
        
        // Start scoreboards based on match type
        if (match.getMatchType().startsWith("queue_")) {
            plugin.getScoreboardManager().startQueueMatchScoreboards(match);
        } else if (match.getMatchType().equals("party_duel")) {
            plugin.getScoreboardManager().startPartyDuelScoreboards(match);
        } else {
            plugin.getScoreboardManager().startMatchScoreboards(match);
        }
        
        // Set movement restrictions for all players during countdown
        for (Player player : players) {
            if (player.isOnline()) {
                // Restrict movement but allow inventory interaction
                player.setWalkSpeed(0.0f);
                player.setFlySpeed(0.0f);
            }
        }
        
        BukkitTask countdownTask = new BukkitRunnable() {
            int countdown = 5;
            
            @Override
            public void run() {
                if (countdown > 0) {
                    for (Player player : players) {
                        if (player.isOnline()) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                            player.sendTitle("§e" + countdown, "§6Organize your inventory", 0, 20, 10);
                        }
                    }
                    countdown--;
                } else {
                    // Save inventories for all players
                    for (Player player : players) {
                        if (player.isOnline()) {
                            plugin.getPlayerDeathListener().saveInventory(player);
                            player.setWalkSpeed(0.2f);
                            player.setFlySpeed(0.1f);
                            player.setGameMode(GameMode.SURVIVAL);
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                            player.sendTitle("§c§lFIGHT!", "", 0, 20, 10);
                        }
                    }
                    
                    match.setState(Match.MatchState.ACTIVE);
                    
                    // Update scoreboards
                    plugin.getScoreboardManager().updateMatchScoreboards(match);
                    
                    this.cancel();
                    countdownTasks.remove(match.getId());
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        countdownTasks.put(match.getId(), countdownTask);
    }
    
    /**
     * Ends a match, announcing the winner, cleaning up resources, and teleporting players back to spawn.
     * 
     * @param match The match to end
     */
    public void endMatch(Match match) {
        if (match == null) return;
        
        // Prevent duplicate ending of the same match
        if (match.getState() == Match.MatchState.ENDING || match.getState() == Match.MatchState.FINISHED) {
            return;
        }
        
        match.setState(Match.MatchState.ENDING);
        match.updateLastActivityTime();
        
        List<Player> players = match.getAllPlayers();
        
        // Announce winner
        if ("ffa".equalsIgnoreCase(match.getMatchType())) {
            UUID winner = match.getWinner();
            if (winner != null) {
                Player winnerPlayer = Bukkit.getPlayer(winner);
                String winnerName = winnerPlayer != null ? winnerPlayer.getName() : "Unknown";
                
                for (Player player : players) {
                    player.sendTitle("§6§lMATCH ENDED", "§aWinner: " + winnerName, 10, 60, 10);
                }
            }
        } else if ("split".equalsIgnoreCase(match.getMatchType())) {
            int winningTeam = match.getWinningTeam();
            if (winningTeam > 0) {
                for (Player player : players) {
                    player.sendTitle("§6§lMATCH ENDED", "§aTeam " + winningTeam + " Wins!", 10, 60, 10);
                }
            }
        }
        
        // Regenerate arena first to ensure it's ready for future matches
        plugin.getArenaManager().pasteSchematic(match.getArena());
        
        // Release the arena
        plugin.getArenaManager().releaseArena(match.getArena().getName());
        
        // Teleport all players to spawn after 3 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : players) {
                playerMatches.remove(player.getUniqueId());
                
                // Reset spectator if they were spectating
                if (match.isPlayerSpectator(player.getUniqueId())) {
                    plugin.getSpectatorListener().resetSpectator(player);
                }
                
                // Reset player state
                player.setGameMode(GameMode.SURVIVAL);
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);
                player.getInventory().clear();
                
                // Teleport to spawn
                if (plugin.getSpawnLocation() != null) {
                    player.teleport(plugin.getSpawnLocation());
                }
                
                // Remove scoreboard
                plugin.getScoreboardManager().removeScoreboard(player);
                
                // Clear any remaining titles
                player.sendTitle("", "", 0, 0, 0);
            }
        }, 60L); // 3 seconds delay
        
        // Set party as not in match
        match.getParty().setInMatch(false);
        match.setState(Match.MatchState.FINISHED);
        
        // Clear arena entities at the end of the match
        clearArenaEntities(match.getArena());
        
        // Cancel scoreboard update task
        plugin.getScoreboardManager().cancelTask(match.getId());
        
        // Remove match
        activeMatches.remove(match.getId());
        
        // Cancel countdown task if exists
        BukkitTask task = countdownTasks.remove(match.getId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
    
    public boolean isInMatch(Player player) {
        return playerMatches.containsKey(player.getUniqueId());
    }
    
    /**
     * Cleans up all resources used by the MatchManager when the plugin is disabled.
     * Cancels tasks, releases arenas, and resets player states.
     */
    public void cleanup() {
        plugin.getLogger().info("Cleaning up MatchManager resources...");
        
        // Cancel all countdown tasks
        for (BukkitTask task : countdownTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        countdownTasks.clear();
        
        // Clear arena cache
        arenaCache.clear();
        
        // Release all reserved arenas
        for (Match match : activeMatches.values()) {
            plugin.getArenaManager().releaseArena(match.getArena().getName());
        }
        
        // Reset all players in match
        for (String matchId : activeMatches.keySet()) {
            Match match = activeMatches.get(matchId);
            for (UUID uuid : match.getAllPlayersUUIDs()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    // Reset spectator state
                    plugin.getSpectatorListener().resetSpectator(player);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setWalkSpeed(0.2f);
                    player.setFlySpeed(0.1f);
                }
            }
        }
        activeMatches.clear();
        playerMatches.clear();
    }
}
