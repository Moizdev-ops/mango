package me.moiz.mangoparty.models;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Represents a match between players in the MangoParty plugin.
 * A match can be of different types (FFA, Split, Party vs Party, Queue) and tracks
 * player teams, eliminations, spectators, and match statistics.
 * 
 * This class is optimized for concurrent access with thread-safe collections
 * and efficient data processing methods.
 */
public class Match {
    /**
     * Represents the different states a match can be in during its lifecycle.
     */
    public enum MatchState {
        /** Initial state when match is being set up */
        PREPARING,
        /** Countdown before the match starts */
        COUNTDOWN,
        /** Match is currently in progress */
        ACTIVE,
        /** Match is ending, final calculations being made */
        ENDING,
        /** Match has completed */
        FINISHED
    }
    
    private final String id;                          // Unique identifier for the match
    private final Party party;                        // The party participating in the match
    private volatile Arena arena;                     // The arena where the match takes place
    private final Kit kit;                            // The kit used in the match
    private final String matchType;                   // Type of match (ffa, split, partyvs, queue_*)
    private volatile MatchState state;                // Current state of the match
    private final Map<UUID, Integer> playerTeams;     // Maps player UUIDs to their team numbers (1 or 2)
    private final Set<UUID> eliminatedPlayers;        // Players who have been eliminated
    private final Set<UUID> spectators;               // Players who are spectating
    private final Map<UUID, Integer> kills;           // Tracks kills for each player
    private final Map<UUID, Integer> deaths;          // Tracks deaths for each player
    private final long startTime;                     // When the match started
    private volatile long lastActivityTime;           // Last time there was activity in the match
    private final Map<Integer, List<UUID>> teamCache; // Cache for team player lookups
    
    /**
     * Gets the team cache map that stores player UUIDs by team number.
     *
     * @return The team cache map
     */
    public Map<Integer, List<UUID>> getTeamCache() {
        return teamCache;
    }

    /**
     * Gets the unique identifier for this match.
     *
     * @return The match ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Creates a new match with the specified parameters.
     *
     * @param id        The unique identifier for this match
     * @param party     The party participating in the match
     * @param arena     The arena where the match takes place
     * @param kit       The kit used in the match
     * @param matchType The type of match (ffa, split, partyvs, queue_*)
     * @throws IllegalArgumentException if id is null
     */
    public Match(String id, Party party, Arena arena, Kit kit, String matchType) {
        if (id == null) {
            throw new IllegalArgumentException("Match ID cannot be null");
        }
        
        this.id = id;
        this.party = party;
        this.arena = arena;
        this.kit = kit;
        this.matchType = matchType != null ? matchType : "unknown";
        this.state = MatchState.PREPARING;
        
        // Use thread-safe collections for concurrent access
        this.playerTeams = new ConcurrentHashMap<>();
        this.eliminatedPlayers = new CopyOnWriteArraySet<>();
        this.spectators = new CopyOnWriteArraySet<>();
        this.kills = new ConcurrentHashMap<>();
        this.deaths = new ConcurrentHashMap<>();
        
        this.startTime = System.currentTimeMillis();
        this.lastActivityTime = this.startTime;
        this.teamCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Creates a new match with a randomly generated UUID as the identifier.
     *
     * @param party     The party participating in the match
     * @param arena     The arena where the match takes place
     * @param kit       The kit used in the match
     * @param matchType The type of match (ffa, split, partyvs, queue_*)
     * @return A new Match instance
     */
    public static Match createWithRandomId(Party party, Arena arena, Kit kit, String matchType) {
        return new Match(UUID.randomUUID().toString(), party, arena, kit, matchType);
    }
    
    /**
     * Assigns players to teams by alternating between team 1 and 2.
     * Players are shuffled to ensure random team assignment.
     * 
     * @return true if teams were successfully assigned, false otherwise
     */
    public boolean assignTeams() {
        if (party == null) return false;
        
        List<Player> players = party.getOnlineMembers();
        if (players.isEmpty()) return false;
        
        // Clear existing team assignments and cache
        playerTeams.clear();
        teamCache.clear();
        
        // Shuffle players for random team assignment
        Collections.shuffle(players);
        
        // Create temporary lists for each team
        List<UUID> team1Players = new ArrayList<>();
        List<UUID> team2Players = new ArrayList<>();
        
        for (int i = 0; i < players.size(); i++) {
            UUID playerId = players.get(i).getUniqueId();
            int team = (i % 2) + 1; // Alternate between team 1 and 2
            playerTeams.put(playerId, team);
            
            // Add to the appropriate team list
            if (team == 1) {
                team1Players.add(playerId);
            } else {
                team2Players.add(playerId);
            }
        }
        
        // Update team cache
        teamCache.put(1, team1Players);
        teamCache.put(2, team2Players);
        
        // Update last activity time
        this.lastActivityTime = System.currentTimeMillis();
        
        return true;
    }
    
    /**
     * Assigns players to teams based on their party membership.
     * All members of party1 are assigned to team 1, and all members of party2 are assigned to team 2.
     *
     * @param party1 The first party, whose members will be assigned to team 1
     * @param party2 The second party, whose members will be assigned to team 2
     * @return true if teams were successfully assigned, false otherwise
     */
    public boolean assignPartyVsPartyTeams(Party party1, Party party2) {
        if (party1 == null || party2 == null) return false;
        
        // Clear existing team assignments and cache
        playerTeams.clear();
        teamCache.clear();
        
        // Create temporary lists for each team
        List<UUID> team1Players = new ArrayList<>();
        List<UUID> team2Players = new ArrayList<>();
        
        // Assign party1 to team 1
        for (Player player : party1.getOnlineMembers()) {
            if (player != null) {
                UUID playerId = player.getUniqueId();
                playerTeams.put(playerId, 1);
                team1Players.add(playerId);
            }
        }
        
        // Assign party2 to team 2
        for (Player player : party2.getOnlineMembers()) {
            if (player != null) {
                UUID playerId = player.getUniqueId();
                playerTeams.put(playerId, 2);
                team2Players.add(playerId);
            }
        }
        
        // Update team cache
        teamCache.put(1, team1Players);
        teamCache.put(2, team2Players);
        
        // Update last activity time
        this.lastActivityTime = System.currentTimeMillis();
        
        return !team1Players.isEmpty() && !team2Players.isEmpty();
    }
    
    /**
     * Eliminates a player from the match, adds them to spectators, and increments their death count.
     *
     * @param playerId The UUID of the player to eliminate
     * @return true if the player was eliminated, false if already eliminated or null
     */
    public boolean eliminatePlayer(UUID playerId) {
        if (playerId == null || eliminatedPlayers.contains(playerId)) return false;
        
        eliminatedPlayers.add(playerId);
        spectators.add(playerId);
        deaths.put(playerId, deaths.getOrDefault(playerId, 0) + 1);
        
        // Update last activity time
        this.lastActivityTime = System.currentTimeMillis();
        
        return true;
    }
    
    /**
     * Increments the kill count for a player.
     *
     * @param playerId The UUID of the player who got a kill
     */
    public void addKill(UUID playerId) {
        if (playerId == null) return;
        
        kills.put(playerId, kills.getOrDefault(playerId, 0) + 1);
    }
    
    /**
     * Checks if a player has been eliminated from the match.
     *
     * @param playerId The UUID of the player to check
     * @return true if the player is eliminated, false otherwise
     */
    public boolean isPlayerEliminated(UUID playerId) {
        return playerId != null && eliminatedPlayers.contains(playerId);
    }
    
    /**
     * Checks if a player is still alive in the match.
     *
     * @param playerId The UUID of the player to check
     * @return true if the player is alive, false if eliminated or null
     */
    public boolean isPlayerAlive(UUID playerId) {
        return playerId != null && !isPlayerEliminated(playerId);
    }
    
    /**
     * Adds a player to the spectator list.
     *
     * @param playerId The UUID of the player to add as a spectator
     */
    public void addSpectator(UUID playerId) {
        if (playerId != null) {
            spectators.add(playerId);
        }
    }
    
    /**
     * Removes a player from the spectator list.
     *
     * @param playerId The UUID of the player to remove from spectators
     */
    public void removeSpectator(UUID playerId) {
        if (playerId != null) {
            spectators.remove(playerId);
        }
    }
    
    /**
     * Checks if a player is currently spectating the match.
     *
     * @param playerId The UUID of the player to check
     * @return true if the player is a spectator, false otherwise
     */
    public boolean isPlayerSpectator(UUID playerId) {
        return playerId != null && spectators.contains(playerId);
    }
    
    /**
     * Checks if the match is finished based on the match type and remaining players.
     * - For FFA matches, the match is finished when only 0 or 1 player remains.
     * - For team-based matches, the match is finished when all players of one team are eliminated.
     *
     * @return true if the match is finished, false otherwise
     */
    public boolean isFinished() {
        if ("ffa".equalsIgnoreCase(matchType)) {
            // FFA ends when only 1 player remains
            return getAlivePlayers().size() <= 1;
        } else if ("split".equalsIgnoreCase(matchType) || 
                  (matchType != null && matchType.startsWith("queue_")) || 
                  "partyvs".equalsIgnoreCase(matchType)) {
            // Team matches end when all players of one team are eliminated
            Set<Integer> aliveTeams = new HashSet<>();
            for (Player player : getAllPlayers()) {
                if (player != null && !isPlayerEliminated(player.getUniqueId())) {
                    Integer team = playerTeams.get(player.getUniqueId());
                    if (team != null) {
                        aliveTeams.add(team);
                    }
                }
            }
            return aliveTeams.size() <= 1;
        }
        return false;
    }
    
    /**
     * Gets the UUID of the winner for FFA matches.
     * Only applicable for FFA matches where there's a single winner.
     *
     * @return The UUID of the winner, or null if there is no winner or not an FFA match
     */
    public UUID getWinner() {
        if ("ffa".equalsIgnoreCase(matchType)) {
            List<Player> alivePlayers = getAlivePlayers();
            return alivePlayers.isEmpty() ? null : alivePlayers.get(0).getUniqueId();
        }
        return null;
    }
    
    /**
     * Gets the winning team number for team-based matches.
     * Only applicable for team-based matches where one team wins.
     *
     * @return The team number of the winning team (1 or 2), or 0 if there is no winner yet
     */
    public int getWinningTeam() {
        if ("split".equalsIgnoreCase(matchType) || 
            (matchType != null && matchType.startsWith("queue_")) || 
            "partyvs".equalsIgnoreCase(matchType)) {
            
            Set<Integer> aliveTeams = new HashSet<>();
            for (Player player : getAllPlayers()) {
                if (player != null && !isPlayerEliminated(player.getUniqueId())) {
                    Integer team = playerTeams.get(player.getUniqueId());
                    if (team != null) {
                        aliveTeams.add(team);
                    }
                }
            }
            return aliveTeams.size() == 1 ? aliveTeams.iterator().next() : 0;
        }
        return 0;
    }
    
    /**
     * Gets a list of all players who are still alive in the match.
     * Optimized using Java streams for better performance.
     *
     * @return A list of alive players
     */
    public List<Player> getAlivePlayers() {
        return getAllPlayers().stream()
                .filter(player -> player != null && !isPlayerEliminated(player.getUniqueId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the number of players who are still alive in the match.
     *
     * @return The count of alive players
     */
    public int getAlivePlayersCount() {
        return getAlivePlayers().size();
    }
    
    /**
     * Gets the number of spectators in the match.
     *
     * @return The count of spectators
     */
    public int getSpectatorsCount() {
        return spectators.size();
    }
    
    /**
     * Gets the number of kills for a specific player.
     *
     * @param playerId The UUID of the player
     * @return The number of kills for the player, or 0 if none
     */
    public int getPlayerKills(UUID playerId) {
        return playerId != null ? kills.getOrDefault(playerId, 0) : 0;
    }
    
    /**
     * Gets the number of deaths for a specific player.
     *
     * @param playerId The UUID of the player
     * @return The number of deaths for the player, or 0 if none
     */
    public int getPlayerDeaths(UUID playerId) {
        return playerId != null ? deaths.getOrDefault(playerId, 0) : 0;
    }
    
    /**
     * Gets the duration of the match in milliseconds.
     *
     * @return The match duration in milliseconds
     */
    public long getMatchDuration() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Gets a list of all players involved in the match, including party members
     * and any additional players from team assignments (for queue matches).
     * Optimized to avoid duplicate lookups and unnecessary contains() checks.
     *
     * @return A list of all players in the match
     */
    public List<Player> getAllPlayers() {
        // Use a Set to automatically handle duplicates
        Set<UUID> playerIds = new HashSet<>();
        List<Player> allPlayers = new ArrayList<>();
        
        // Add party members
        if (party != null) {
            for (Player player : party.getOnlineMembers()) {
                if (player != null) {
                    UUID playerId = player.getUniqueId();
                    if (playerIds.add(playerId)) { // Returns true if this set did not already contain the specified element
                        allPlayers.add(player);
                    }
                }
            }
        }
        
        // Add any additional players from team assignments (for queue matches)
        for (UUID uuid : playerTeams.keySet()) {
            if (uuid != null && playerIds.add(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    allPlayers.add(player);
                }
            }
        }
        
        return allPlayers;
    }
    
    /**
     * Gets a set of UUIDs for all players involved in the match.
     *
     * @return A set of UUIDs for all players in the match
     */
    public Set<UUID> getAllPlayersUUIDs() {
        Set<UUID> allUUIDs = new HashSet<>();
        
        // Add party members
        if (party != null) {
            for (Player player : party.getOnlineMembers()) {
                if (player != null) {
                    allUUIDs.add(player.getUniqueId());
                }
            }
        }
        
        // Add any additional players from team assignments
        allUUIDs.addAll(playerTeams.keySet());
        // Remove null entries if any exist
        allUUIDs.remove(null);
        
        return allUUIDs;
    }
    
    /**
     * Gets the team number for a specific player.
     *
     * @param playerId The UUID of the player
     * @return The team number (1 or 2), or 0 if the player is not on a team
     */
    public int getPlayerTeam(UUID playerId) {
        return playerId != null ? playerTeams.getOrDefault(playerId, 0) : 0;
    }
    
    /**
     * Checks if two players are on the same team.
     *
     * @param player1Id The UUID of the first player
     * @param player2Id The UUID of the second player
     * @return true if both players are on the same team (and not team 0), false otherwise
     */
    public boolean arePlayersOnSameTeam(UUID player1Id, UUID player2Id) {
        if (player1Id == null || player2Id == null) return false;
        
        int team1 = getPlayerTeam(player1Id);
        int team2 = getPlayerTeam(player2Id);
        return team1 == team2 && team1 != 0;
    }
    
    /**
     * Gets a list of all players on a specific team.
     * Optimized with caching for better performance.
     *
     * @param team The team number (1 or 2)
     * @return A list of players on the specified team
     */
    public List<Player> getTeamPlayers(int team) {
        if (team <= 0) return Collections.emptyList();
        
        // Use cached team player UUIDs if available
        List<UUID> teamPlayerIds = teamCache.get(team);
        
        // If cache is not available, rebuild it
        if (teamPlayerIds == null) {
            teamPlayerIds = new ArrayList<>();
            for (Map.Entry<UUID, Integer> entry : playerTeams.entrySet()) {
                if (entry.getKey() != null && entry.getValue() == team) {
                    teamPlayerIds.add(entry.getKey());
                }
            }
            teamCache.put(team, teamPlayerIds);
        }
        
        // Convert UUIDs to Player objects
        return teamPlayerIds.stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline())
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the number of alive players on a specific team.
     * Optimized with caching and streams for better performance.
     *
     * @param team The team number (1 or 2)
     * @return The count of alive players on the specified team
     */
    public int getTeamAliveCount(int team) {
        if (team <= 0) return 0;
        
        // Use cached team player UUIDs if available
        List<UUID> teamPlayerIds = teamCache.get(team);
        
        // If cache is not available, rebuild it
        if (teamPlayerIds == null) {
            teamPlayerIds = new ArrayList<>();
            for (Map.Entry<UUID, Integer> entry : playerTeams.entrySet()) {
                if (entry.getKey() != null && entry.getValue() == team) {
                    teamPlayerIds.add(entry.getKey());
                }
            }
            teamCache.put(team, teamPlayerIds);
        }
        
        // Count alive players
        return (int) teamPlayerIds.stream()
                .filter(uuid -> !isPlayerEliminated(uuid))
                .count();
    }
    
    /**
     * Gets the total number of players on a specific team, including eliminated players.
     * Optimized with caching for better performance.
     *
     * @param team The team number (1 or 2)
     * @return The total count of players on the specified team
     */
    public int getTeamTotalCount(int team) {
        if (team <= 0) return 0;
        
        // Use cached team player UUIDs if available
        List<UUID> teamPlayerIds = teamCache.get(team);
        
        // If cache is not available, rebuild it
        if (teamPlayerIds == null) {
            teamPlayerIds = new ArrayList<>();
            for (Map.Entry<UUID, Integer> entry : playerTeams.entrySet()) {
                if (entry.getKey() != null && entry.getValue() == team) {
                    teamPlayerIds.add(entry.getKey());
                }
            }
            teamCache.put(team, teamPlayerIds);
            return teamPlayerIds.size();
        }
        
        return teamPlayerIds.size();
    }
    
    /**
     * Invalidates the team cache, forcing it to be rebuilt on the next team-related query.
     * Call this method after making changes to team assignments.
     */
    public void invalidateTeamCache() {
        teamCache.clear();
    }
    
    /**
     * Updates the last activity time to the current time.
     * This should be called whenever there is significant activity in the match.
     */
    public void updateLastActivityTime() {
        this.lastActivityTime = System.currentTimeMillis();
    }
    
    /**
     * Gets the time of the last activity in the match.
     *
     * @return The last activity time in milliseconds
     */
    public long getLastActivityTime() {
        return lastActivityTime;
    }
    
    /**
     * Checks if the match has been inactive for longer than the specified duration.
     *
     * @param maxInactiveTimeMillis The maximum inactive time in milliseconds
     * @return true if the match has been inactive for longer than the specified time, false otherwise
     */
    public boolean isInactiveLongerThan(long maxInactiveTimeMillis) {
        return System.currentTimeMillis() - lastActivityTime > maxInactiveTimeMillis;
    }
    
    // Getters and setters

    /**
     * Gets the party participating in the match.
     * @return The party
     */
    public Party getParty() { return party; }
    
    /**
     * Gets the arena where the match takes place.
     * @return The arena
     */
    public Arena getArena() { return arena; }
    
    /**
     * Sets the arena where the match takes place.
     * @param arena The new arena
     */
    public void setArena(Arena arena) { this.arena = arena; }
    
    /**
     * Gets the kit used in the match.
     * @return The kit
     */
    public Kit getKit() { return kit; }
    
    /**
     * Gets the type of match.
     * @return The match type (ffa, split, partyvs, queue_*)
     */
    public String getMatchType() { return matchType; }
    
    /**
     * Gets the current state of the match.
     * @return The match state
     */
    public MatchState getState() { return state; }
    
    /**
     * Sets the current state of the match.
     * @param state The new match state
     */
    public void setState(MatchState state) { this.state = state != null ? state : MatchState.PREPARING; }
    
    /**
     * Gets the map of player UUIDs to team numbers.
     * @return The player teams map
     */
    public Map<UUID, Integer> getPlayerTeams() { return playerTeams; }
    
    /**
     * Gets the set of eliminated player UUIDs.
     * @return The eliminated players set
     */
    public Set<UUID> getEliminatedPlayers() { return eliminatedPlayers; }
    
    /**
     * Gets the set of spectator UUIDs.
     * @return The spectators set
     */
    public Set<UUID> getSpectators() { return spectators; }
    
    /**
     * Gets the time when the match started.
     * @return The start time in milliseconds
     */
    public long getStartTime() { return startTime; }
}
