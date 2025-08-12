package me.moiz.mangoparty.models;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Duel {
    private String id;
    private UUID uuid; // Unique identifier for callback handling
    private Player challenger;
    private Player target;
    private String kitName;
    private int roundsToWin;
    private int player1Wins;
    private int player2Wins;
    private int currentRound;
    private Arena arena;
    private DuelState state;
    private long startTime;
    private BukkitTask expirationTask;
    private boolean isPartySplitMatch;
    private Map<UUID, Integer> playerTeams; // Player UUID -> Team (1 or 2)
    
    // Saved inventories
    private ItemStack[] player1Inventory;
    private ItemStack[] player1Armor;
    private ItemStack player1Offhand;
    private ItemStack[] player2Inventory;
    private ItemStack[] player2Armor;
    private ItemStack player2Offhand;
    
    public enum DuelState {
        PENDING,
        PREPARING,
        COUNTDOWN,
        ACTIVE,
        ENDING,
        FINISHED
    }
    
    public Duel(Player challenger, Player target, String kitName, int roundsToWin) {
        this.challenger = challenger;
        this.target = target;
        this.kitName = kitName;
        this.roundsToWin = roundsToWin;
        this.player1Wins = 0;
        this.player2Wins = 0;
        this.currentRound = 0;
        this.state = DuelState.PENDING;
        this.startTime = System.currentTimeMillis();
        this.isPartySplitMatch = false;
        this.playerTeams = new HashMap<>();
        
        // Default team assignments for regular duels
        if (challenger != null) {
            playerTeams.put(challenger.getUniqueId(), 1);
        }
        if (target != null) {
            playerTeams.put(target.getUniqueId(), 2);
        }
    }
    
    /**
     * Check if the duel request has expired (60 seconds)
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > 60000;
    }
    
    /**
     * Get the duration of the duel in milliseconds
     */
    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Increment player 1 wins
     */
    public void incrementPlayer1Wins() {
        player1Wins++;
    }
    
    /**
     * Increment player 2 wins
     */
    public void incrementPlayer2Wins() {
        player2Wins++;
    }
    
    /**
     * Increment current round
     */
    public void incrementCurrentRound() {
        currentRound++;
    }
    
    // Getters and setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    public Player getChallenger() {
        return challenger;
    }
    
    public Player getTarget() {
        return target;
    }
    
    public String getKitName() {
        return kitName;
    }
    
    public int getRoundsToWin() {
        return roundsToWin;
    }
    
    public int getPlayer1Wins() {
        return player1Wins;
    }
    
    public int getPlayer2Wins() {
        return player2Wins;
    }
    
    public int getCurrentRound() {
        return currentRound;
    }
    
    public Arena getArena() {
        return arena;
    }
    
    public void setArena(Arena arena) {
        this.arena = arena;
    }
    
    public DuelState getState() {
        return state;
    }
    
    public void setState(DuelState state) {
        this.state = state;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public BukkitTask getExpirationTask() {
        return expirationTask;
    }
    
    public void setExpirationTask(BukkitTask expirationTask) {
        this.expirationTask = expirationTask;
    }
    
    public ItemStack[] getPlayer1Inventory() {
        return player1Inventory;
    }
    
    public void setPlayer1Inventory(ItemStack[] player1Inventory) {
        this.player1Inventory = player1Inventory;
    }
    
    public ItemStack[] getPlayer1Armor() {
        return player1Armor;
    }
    
    public void setPlayer1Armor(ItemStack[] player1Armor) {
        this.player1Armor = player1Armor;
    }
    
    public ItemStack[] getPlayer2Inventory() {
        return player2Inventory;
    }
    
    public void setPlayer2Inventory(ItemStack[] player2Inventory) {
        this.player2Inventory = player2Inventory;
    }
    
    public ItemStack[] getPlayer2Armor() {
        return player2Armor;
    }
    
    public void setPlayer2Armor(ItemStack[] player2Armor) {
        this.player2Armor = player2Armor;
    }
    
    public ItemStack getPlayer1Offhand() {
        return player1Offhand;
    }
    
    public void setPlayer1Offhand(ItemStack player1Offhand) {
        this.player1Offhand = player1Offhand;
    }
    
    public boolean isPartySplitMatch() {
        return isPartySplitMatch;
    }
    
    public void setPartySplitMatch(boolean partySplitMatch) {
        isPartySplitMatch = partySplitMatch;
    }
    
    public Map<UUID, Integer> getPlayerTeams() {
        return playerTeams;
    }
    
    public void setPlayerTeam(UUID playerId, int team) {
        playerTeams.put(playerId, team);
    }
    
    public int getPlayerTeam(UUID playerId) {
        return playerTeams.getOrDefault(playerId, 0);
    }
    
    public boolean arePlayersOnSameTeam(Player player1, Player player2) {
        int team1 = getPlayerTeam(player1.getUniqueId());
        int team2 = getPlayerTeam(player2.getUniqueId());
        return team1 > 0 && team2 > 0 && team1 == team2;
    }
    
    public boolean arePlayersOnSameTeam(UUID player1Id, UUID player2Id) {
        int team1 = getPlayerTeam(player1Id);
        int team2 = getPlayerTeam(player2Id);
        return team1 > 0 && team2 > 0 && team1 == team2;
    }
    
    public ItemStack getPlayer2Offhand() {
        return player2Offhand;
    }
    
    public void setPlayer2Offhand(ItemStack player2Offhand) {
        this.player2Offhand = player2Offhand;
    }
}