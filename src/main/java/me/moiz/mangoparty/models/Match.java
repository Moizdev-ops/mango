package me.moiz.mangoparty.models;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Match {
    public enum MatchState {
        PREPARING,
        COUNTDOWN,
        ACTIVE,
        ENDING,
        FINISHED
    }
    
    private String id;
    private Party party;
    private Arena arena;
    private Kit kit;
    private String matchType;
    private MatchState state;
    private long startTime;
    private Map<UUID, Integer> playerTeams; // Player UUID -> Team number (1 or 2)
    private Set<UUID> alivePlayers;
    private Set<UUID> spectators;
    private Map<UUID, Integer> kills;
    private Map<UUID, Integer> deaths;
    
    public Match(String id, Party party, Arena arena, Kit kit, String matchType) {
        this.id = id;
        this.party = party;
        this.arena = arena;
        this.kit = kit;
        this.matchType = matchType;
        this.state = MatchState.PREPARING;
        this.startTime = System.currentTimeMillis();
        this.playerTeams = new ConcurrentHashMap<>();
        this.alivePlayers = ConcurrentHashMap.newKeySet();
        this.spectators = ConcurrentHashMap.newKeySet();
        this.kills = new ConcurrentHashMap<>();
        this.deaths = new ConcurrentHashMap<>();
        
        // Initialize all party members as alive
        for (UUID member : party.getMembers()) {
            alivePlayers.add(member);
            kills.put(member, 0);
            deaths.put(member, 0);
        }
    }
    
    public void assignTeams() {
        if (!"split".equalsIgnoreCase(matchType)) {
            return;
        }
        
        List<UUID> members = new ArrayList<>(party.getMembers());
        Collections.shuffle(members);
        
        for (int i = 0; i < members.size(); i++) {
            int team = (i % 2) + 1; // Team 1 or 2
            playerTeams.put(members.get(i), team);
        }
    }
    
    public void eliminatePlayer(UUID player) {
        alivePlayers.remove(player);
        spectators.add(player);
        
        // Increment death count
        deaths.put(player, deaths.getOrDefault(player, 0) + 1);
    }
    
    public void addKill(UUID player) {
        kills.put(player, kills.getOrDefault(player, 0) + 1);
    }
    
    public boolean isFinished() {
        if ("ffa".equalsIgnoreCase(matchType)) {
            return alivePlayers.size() <= 1;
        } else if ("split".equalsIgnoreCase(matchType)) {
            // Check if all players from one team are eliminated
            Set<Integer> aliveTeams = new HashSet<>();
            for (UUID alive : alivePlayers) {
                Integer team = playerTeams.get(alive);
                if (team != null) {
                    aliveTeams.add(team);
                }
            }
            return aliveTeams.size() <= 1;
        }
        return false;
    }
    
    public UUID getWinner() {
        if ("ffa".equalsIgnoreCase(matchType) && alivePlayers.size() == 1) {
            return alivePlayers.iterator().next();
        }
        return null;
    }
    
    public int getWinningTeam() {
        if ("split".equalsIgnoreCase(matchType)) {
            Set<Integer> aliveTeams = new HashSet<>();
            for (UUID alive : alivePlayers) {
                Integer team = playerTeams.get(alive);
                if (team != null) {
                    aliveTeams.add(team);
                }
            }
            if (aliveTeams.size() == 1) {
                return aliveTeams.iterator().next();
            }
        }
        return 0;
    }
    
    public int getPlayerTeam(UUID player) {
        return playerTeams.getOrDefault(player, 0);
    }
    
    public List<Player> getAllPlayers() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : party.getMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }
    
    public Set<UUID> getAllPlayersUUIDs() {
        return new HashSet<>(party.getMembers());
    }
    
    public int getAlivePlayersCount() {
        return alivePlayers.size();
    }
    
    public int getSpectatorsCount() {
        return spectators.size();
    }
    
    public int getTeamAliveCount(int team) {
        if (!"split".equalsIgnoreCase(matchType)) {
            return 0;
        }
        
        int count = 0;
        for (UUID alive : alivePlayers) {
            if (playerTeams.getOrDefault(alive, 0) == team) {
                count++;
            }
        }
        return count;
    }
    
    public int getTeamTotalCount(int team) {
        if (!"split".equalsIgnoreCase(matchType)) {
            return 0;
        }
        
        int count = 0;
        for (Integer playerTeam : playerTeams.values()) {
            if (playerTeam == team) {
                count++;
            }
        }
        return count;
    }
    
    public boolean isPlayerAlive(UUID player) {
        return alivePlayers.contains(player);
    }
    
    public boolean isPlayerSpectator(UUID player) {
        return spectators.contains(player);
    }
    
    public int getPlayerKills(UUID player) {
        return kills.getOrDefault(player, 0);
    }
    
    public int getPlayerDeaths(UUID player) {
        return deaths.getOrDefault(player, 0);
    }
    
    public long getMatchDuration() {
        return System.currentTimeMillis() - startTime;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public Party getParty() {
        return party;
    }
    
    public Arena getArena() {
        return arena;
    }
    
    public Kit getKit() {
        return kit;
    }
    
    public String getMatchType() {
        return matchType;
    }
    
    public MatchState getState() {
        return state;
    }
    
    public void setState(MatchState state) {
        this.state = state;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public Map<UUID, Integer> getPlayerTeams() {
        return playerTeams;
    }
}
