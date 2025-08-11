package me.moiz.mangoparty.managers;

import fr.mrmicky.fastboard.FastBoard;
import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Duel;
import me.moiz.mangoparty.models.Match;
import me.moiz.mangoparty.models.Party;
import me.moiz.mangoparty.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {
    private MangoParty plugin;
    private Map<UUID, FastBoard> scoreboards;
    private Map<String, BukkitTask> updateTasks; // Match/Duel ID -> Task
    private FileConfiguration scoreboardConfig;
    private int updateInterval;
    
    public ScoreboardManager(MangoParty plugin) {
        this.plugin = plugin;
        this.scoreboards = new ConcurrentHashMap<>();
        this.updateTasks = new ConcurrentHashMap<>();
        loadScoreboardConfig();
    }
    
    /**
     * Load the scoreboard configuration from scoreboard.yml
     */
    public void loadScoreboardConfig() {
        File configFile = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!configFile.exists()) {
            plugin.saveResource("scoreboard.yml", false);
        }
        
        scoreboardConfig = YamlConfiguration.loadConfiguration(configFile);
        updateInterval = scoreboardConfig.getInt("global.update_interval", 20);
    }
    
    /**
     * Start scoreboards for a match based on match type
     */
    public void startMatchScoreboards(Match match) {
        String matchType = match.getMatchType().toLowerCase();
        String configSection;
        
        // Determine which scoreboard config to use based on match type
        if (matchType.equals("ffa")) {
            configSection = "party_ffa";
        } else if (matchType.equals("split")) {
            configSection = "party_split";
        } else {
            // Default to party_ffa if unknown type
            configSection = "party_ffa";
        }
        
        // Get title from config
        String title = scoreboardConfig.getString(configSection + ".title", 
                                              scoreboardConfig.getString("global.title", "&#FFD700&lMangoParty"));
        title = HexUtils.colorize(title);
        
        // Create scoreboards for all players
        for (Player player : match.getAllPlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(title);
            scoreboards.put(player.getUniqueId(), board);
        }
        
        // Start update task
        String matchId = match.getId();
        cancelTask(matchId);
        
        updateTasks.put(matchId, new BukkitRunnable() {
            @Override
            public void run() {
                updateMatchScoreboards(match, configSection);
            }
        }.runTaskTimer(plugin, 0L, updateInterval));
    }
    
    /**
     * Start scoreboards for a duel
     */
    public void startDuelScoreboards(Duel duel) {
        // Get title from config
        String title = scoreboardConfig.getString("duel.title", 
                                              scoreboardConfig.getString("global.title", "&#FFD700&lMangoParty"));
        title = HexUtils.colorize(title);
        
        // Create scoreboards for both players
        Player player1 = duel.getChallenger();
        Player player2 = duel.getTarget();
        
        if (player1 != null && player1.isOnline()) {
            FastBoard board = new FastBoard(player1);
            board.updateTitle(title);
            scoreboards.put(player1.getUniqueId(), board);
        }
        
        if (player2 != null && player2.isOnline()) {
            FastBoard board = new FastBoard(player2);
            board.updateTitle(title);
            scoreboards.put(player2.getUniqueId(), board);
        }
        
        // Start update task
        String duelId = duel.getId();
        cancelTask(duelId);
        
        updateTasks.put(duelId, new BukkitRunnable() {
            @Override
            public void run() {
                updateDuelScoreboards(duel);
            }
        }.runTaskTimer(plugin, 0L, updateInterval));
    }
    
    /**
     * Start scoreboards for a queue match
     */
    public void startQueueMatchScoreboards(Match match) {
        String matchType = match.getMatchType().toLowerCase();
        String configSection = "queue_" + matchType;
        
        // Fallback if config section doesn't exist
        if (!scoreboardConfig.contains(configSection)) {
            configSection = "queue_1v1"; // Default
        }
        
        // Get title from config
        String title = scoreboardConfig.getString(configSection + ".title", 
                                              scoreboardConfig.getString("global.title", "&#FFD700&lMangoParty"));
        title = HexUtils.colorize(title);
        
        // Create scoreboards for all players
        for (Player player : match.getAllPlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(title);
            scoreboards.put(player.getUniqueId(), board);
        }
        
        // Start update task
        String matchId = match.getId();
        cancelTask(matchId);
        
        updateTasks.put(matchId, new BukkitRunnable() {
            @Override
            public void run() {
                updateMatchScoreboards(match, configSection);
            }
        }.runTaskTimer(plugin, 0L, updateInterval));
    }
    
    /**
     * Start scoreboards for a party duel
     */
    public void startPartyDuelScoreboards(Match match) {
        // Get title from config
        String title = scoreboardConfig.getString("party_duel.title", 
                                              scoreboardConfig.getString("global.title", "&#FFD700&lMangoParty"));
        title = HexUtils.colorize(title);
        
        // Create scoreboards for all players
        for (Player player : match.getAllPlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(title);
            scoreboards.put(player.getUniqueId(), board);
        }
        
        // Start update task
        String matchId = match.getId();
        cancelTask(matchId);
        
        updateTasks.put(matchId, new BukkitRunnable() {
            @Override
            public void run() {
                updateMatchScoreboards(match, "party_duel");
            }
        }.runTaskTimer(plugin, 0L, updateInterval));
    }
    
    /**
     * Cancel an existing update task
     */
    public void cancelTask(String id) {
        BukkitTask task = updateTasks.remove(id);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
    
    /**
     * Remove a player's scoreboard
     */
    public void removeScoreboard(Player player) {
        FastBoard board = scoreboards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }
    
    /**
     * Update match scoreboards with the specified configuration section
     */
    private void updateMatchScoreboards(Match match, String configSection) {
        List<String> lines = scoreboardConfig.getStringList(configSection + ".lines");
        if (lines.isEmpty()) {
            // Fallback to global lines if section-specific lines are not defined
            lines = scoreboardConfig.getStringList("global.lines");
        }
        
        for (Player player : match.getAllPlayers()) {
            FastBoard board = scoreboards.get(player.getUniqueId());
            if (board == null) continue;
            
            List<String> playerLines = new ArrayList<>();
            
            for (String line : lines) {
                String processedLine = processPlaceholders(line, match, player);
                processedLine = HexUtils.colorize(processedLine);
                
                // Skip empty lines and limit to 15 lines max
                if (!processedLine.trim().isEmpty() && playerLines.size() < 15) {
                    playerLines.add(processedLine);
                }
            }
            
            board.updateLines(playerLines);
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public void updateMatchScoreboards(Match match) {
        updateMatchScoreboards(match, "party_ffa");
    }
    
    /**
     * Update duel scoreboards
     */
    private void updateDuelScoreboards(Duel duel) {
        List<String> lines = scoreboardConfig.getStringList("duel.lines");
        if (lines.isEmpty()) {
            // Fallback to global lines if duel-specific lines are not defined
            lines = scoreboardConfig.getStringList("global.lines");
        }
        
        Player player1 = duel.getChallenger();
        Player player2 = duel.getTarget();
        
        // Update player1's scoreboard
        if (player1 != null && player1.isOnline()) {
            updateDuelPlayerScoreboard(player1, player2, duel, lines);
        }
        
        // Update player2's scoreboard
        if (player2 != null && player2.isOnline()) {
            updateDuelPlayerScoreboard(player2, player1, duel, lines);
        }
    }
    
    /**
     * Public method to update duel scoreboard for specific players
     */
    public void updateDuelScoreboard(Player player1, Player player2, Duel duel) {
        List<String> lines = scoreboardConfig.getStringList("duel.lines");
        if (lines.isEmpty()) {
            // Fallback to global lines if duel-specific lines are not defined
            lines = scoreboardConfig.getStringList("global.lines");
        }
        
        // Update player1's scoreboard
        if (player1 != null && player1.isOnline()) {
            updateDuelPlayerScoreboard(player1, player2, duel, lines);
        }
        
        // Update player2's scoreboard
        if (player2 != null && player2.isOnline()) {
            updateDuelPlayerScoreboard(player2, player1, duel, lines);
        }
    }
    
    /**
     * Update scoreboard for a specific player in a duel
     */
    private void updateDuelPlayerScoreboard(Player player, Player opponent, Duel duel, List<String> lines) {
        FastBoard board = scoreboards.get(player.getUniqueId());
        if (board == null) return;
        
        List<String> playerLines = new ArrayList<>();
        
        for (String line : lines) {
            String processedLine = line;
            
            // Replace basic placeholders
            processedLine = processedLine.replace("{kit}", duel.getKitName());
            processedLine = processedLine.replace("{rounds_to_win}", String.valueOf(duel.getRoundsToWin()));
            processedLine = processedLine.replace("{current_round}", String.valueOf(duel.getCurrentRound()));
            
            // Player specific placeholders
            processedLine = processedLine.replace("{player}", player.getName());
            processedLine = processedLine.replace("{opponent}", opponent.getName());
            
            // Wins
            int yourWins = player == duel.getChallenger() ? duel.getPlayer1Wins() : duel.getPlayer2Wins();
            int opponentWins = player == duel.getChallenger() ? duel.getPlayer2Wins() : duel.getPlayer1Wins();
            processedLine = processedLine.replace("{your_wins}", String.valueOf(yourWins));
            processedLine = processedLine.replace("{opponent_wins}", String.valueOf(opponentWins));
            
            // Add new placeholders for player_score and opponent_score (same as your_wins and opponent_wins)
            processedLine = processedLine.replace("{player_score}", String.valueOf(yourWins));
            processedLine = processedLine.replace("{opponent_score}", String.valueOf(opponentWins));
            
            // Time
            long duration = duel.getDuration();
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;
            String timeString = String.format("%02d:%02d", minutes, seconds);
            processedLine = processedLine.replace("{time}", timeString);
            
            processedLine = HexUtils.colorize(processedLine);
            
            // Skip empty lines and limit to 15 lines max
            if (!processedLine.trim().isEmpty() && playerLines.size() < 15) {
                playerLines.add(processedLine);
            }
        }
        
        board.updateLines(playerLines);
    }
    
    private String processPlaceholders(String line, Match match, Player player) {
        // Basic match info
        line = line.replace("{arena}", match.getArena().getName());
        line = line.replace("{kit}", match.getKit().getDisplayName());
        line = line.replace("{match_type}", match.getMatchType().toUpperCase());
        line = line.replace("{state}", match.getState().toString());
        
        // Player status
        String status = match.isPlayerAlive(player.getUniqueId()) ? "§aAlive" : "§cSpectating";
        line = line.replace("{status}", status);
        
        // Player counts
        line = line.replace("{players_alive}", String.valueOf(match.getAlivePlayersCount()));
        line = line.replace("{players_total}", String.valueOf(match.getAllPlayersUUIDs().size()));
        line = line.replace("{spectators}", String.valueOf(match.getSpectatorsCount()));
        
        // Player stats
        line = line.replace("{kills}", String.valueOf(match.getPlayerKills(player.getUniqueId())));
        line = line.replace("{deaths}", String.valueOf(match.getPlayerDeaths(player.getUniqueId())));
        
        // Team info (for split matches)
        if ("split".equalsIgnoreCase(match.getMatchType())) {
            int playerTeam = match.getPlayerTeam(player.getUniqueId());
            int opponentTeam = playerTeam == 1 ? 2 : 1;
            
            line = line.replace("{your_team_alive}", String.valueOf(match.getTeamAliveCount(playerTeam)));
            line = line.replace("{your_team_total}", String.valueOf(match.getTeamTotalCount(playerTeam)));
            line = line.replace("{opponent_team_alive}", String.valueOf(match.getTeamAliveCount(opponentTeam)));
            line = line.replace("{opponent_team_total}", String.valueOf(match.getTeamTotalCount(opponentTeam)));
            
            // Individual team counts
            line = line.replace("{team1_alive}", String.valueOf(match.getTeamAliveCount(1)));
            line = line.replace("{team2_alive}", String.valueOf(match.getTeamAliveCount(2)));
        } else {
            // For FFA, team info doesn't apply
            line = line.replace("{your_team_alive}", "N/A");
            line = line.replace("{your_team_total}", "N/A");
            line = line.replace("{opponent_team_alive}", "N/A");
            line = line.replace("{opponent_team_total}", "N/A");
            line = line.replace("{team1_alive}", "N/A");
            line = line.replace("{team2_alive}", "N/A");
        }
        
        // Time
        long duration = match.getMatchDuration();
        long minutes = duration / 60000;
        long seconds = (duration % 60000) / 1000;
        String timeString = String.format("%02d:%02d", minutes, seconds);
        line = line.replace("{time}", timeString);
        
        // Player name
        line = line.replace("{player}", player.getName());
        
        return line;
    }
    
    public void removeScoreboard(Player player) {
        FastBoard board = scoreboards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }
    
    public void cleanup() {
        // Cancel all update tasks
        for (BukkitTask task : updateTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        updateTasks.clear();
        
        // Remove all scoreboards
        for (FastBoard board : scoreboards.values()) {
            board.delete();
        }
        scoreboards.clear();
    }
}
