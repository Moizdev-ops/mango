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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages scoreboards for all match types and players
 */

public class ScoreboardManager {
    private final MangoParty plugin;
    private final Map<UUID, FastBoard> scoreboards;
    private final Map<String, BukkitTask> updateTasks; // Match/Duel ID -> Task
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
     * 
     * @param match The match to create scoreboards for
     */
    public void startMatchScoreboards(Match match) {
        if (match == null) return;
        
        String matchType = match.getMatchType().toLowerCase();
        final String configSection = getConfigSectionForMatchType(matchType);
        
        // Get title from config
        String title = getScoreboardTitle(configSection);
        
        // Create scoreboards for all players
        createScoreboardsForPlayers(match.getAllPlayers(), title);
        
        // Start update task
        startUpdateTask(match.getId(), () -> updateMatchScoreboards(match, configSection));
    }
    
    /**
     * Get the appropriate config section for a match type
     * 
     * @param matchType The match type
     * @return The config section name
     */
    private String getConfigSectionForMatchType(String matchType) {
        if ("ffa".equals(matchType)) {
            return "party_ffa";
        } else if ("split".equals(matchType)) {
            return "party_split";
        } else {
            // Default to party_ffa if unknown type
            return "party_ffa";
        }
    }
    
    /**
     * Get the scoreboard title from config
     * 
     * @param configSection The config section to get the title from
     * @return The colorized title
     */
    private String getScoreboardTitle(String configSection) {
        String title = scoreboardConfig.getString(configSection + ".title", 
                                              scoreboardConfig.getString("global.title", "&#FFD700&lMangoParty"));
        return HexUtils.colorize(title);
    }
    
    /**
     * Create scoreboards for a list of players
     * 
     * @param players The players to create scoreboards for
     * @param title The title for the scoreboards
     */
    private void createScoreboardsForPlayers(Iterable<Player> players, String title) {
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                FastBoard board = new FastBoard(player);
                board.updateTitle(title);
                scoreboards.put(player.getUniqueId(), board);
            }
        }
    }
    
    /**
     * Start an update task for a match or duel
     * 
     * @param id The match or duel ID
     * @param updateTask The task to run
     */
    private void startUpdateTask(String id, Runnable updateTask) {
        cancelTask(id);
        
        updateTasks.put(id, new BukkitRunnable() {
            @Override
            public void run() {
                updateTask.run();
            }
        }.runTaskTimer(plugin, 0L, updateInterval));
    }
    
    /**
     * Start scoreboards for a duel
     * 
     * @param duel The duel to create scoreboards for
     */
    public void startDuelScoreboards(Duel duel) {
        if (duel == null) return;
        
        // Get title from config
        String title = getScoreboardTitle("duel");
        
        // Create scoreboards for both players
        Player player1 = duel.getChallenger();
        Player player2 = duel.getTarget();
        
        if (player1 != null && player1.isOnline()) {
            createScoreboardForPlayer(player1, title);
        }
        
        if (player2 != null && player2.isOnline()) {
            createScoreboardForPlayer(player2, title);
        }
        
        // Start update task
        startUpdateTask(duel.getId(), () -> updateDuelScoreboards(duel));
    }
    
    /**
     * Create a scoreboard for a single player
     * 
     * @param player The player to create a scoreboard for
     * @param title The title for the scoreboard
     */
    private void createScoreboardForPlayer(Player player, String title) {
        if (player != null && player.isOnline()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(title);
            scoreboards.put(player.getUniqueId(), board);
        }
    }
    
    /**
     * Start scoreboards for a queue match
     * 
     * @param match The queue match to create scoreboards for
     */
    public void startQueueMatchScoreboards(Match match) {
        if (match == null) return;
        
        String matchType = match.getMatchType().toLowerCase();
        final String configSection = getQueueConfigSection(matchType);
        
        // Get title from config
        String title = getScoreboardTitle(configSection);
        
        // Create scoreboards for all players
        createScoreboardsForPlayers(match.getAllPlayers(), title);
        
        // Start update task
        startUpdateTask(match.getId(), () -> updateMatchScoreboards(match, configSection));
    }
    
    /**
     * Get the appropriate config section for a queue match type
     * 
     * @param matchType The match type
     * @return The config section name
     */
    private String getQueueConfigSection(String matchType) {
        String section = "queue_" + matchType;
        return scoreboardConfig.contains(section) ? section : "queue_1v1";
    }
    
    /**
     * Start scoreboards for a party duel
     * 
     * @param match The party duel match to create scoreboards for
     */
    public void startPartyDuelScoreboards(Match match) {
        if (match == null) return;
        
        // Get title from config
        String title = getScoreboardTitle("party_duel");
        
        // Create scoreboards for all players
        createScoreboardsForPlayers(match.getAllPlayers(), title);
        
        // Start update task
        startUpdateTask(match.getId(), () -> updateMatchScoreboards(match, "party_duel"));
    }
    
    /**
     * Cancel an existing update task
     * 
     * @param id The ID of the task to cancel
     */
    public void cancelTask(String id) {
        if (id == null) return;
        
        BukkitTask task = updateTasks.remove(id);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
    
    /**
     * Remove a player's scoreboard
     * 
     * @param player The player whose scoreboard to remove
     */
    public void removePlayerScoreboard(Player player) {
        if (player == null) return;
        
        FastBoard board = scoreboards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }
    
    /**
     * Update match scoreboards with the specified configuration section
     * 
     * @param match The match to update scoreboards for
     * @param configSection The configuration section to get scoreboard lines from
     */
    private void updateMatchScoreboards(Match match, String configSection) {
        if (match == null) return;
        
        List<String> lines = scoreboardConfig.getStringList(configSection + ".lines");
        if (lines.isEmpty()) {
            // Fallback to global lines if section-specific lines are not defined
            lines = scoreboardConfig.getStringList("global.lines");
        }
        
        for (Player player : match.getAllPlayers()) {
            updatePlayerMatchScoreboard(player, match, lines);
        }
    }
    
    /**
     * Updates the scoreboard for a specific player in a match
     * 
     * @param player The player to update the scoreboard for
     * @param match The match the player is in
     * @param lines The scoreboard lines from configuration
     */
    private void updatePlayerMatchScoreboard(Player player, Match match, List<String> lines) {
        if (player == null) return;
        
        FastBoard board = scoreboards.get(player.getUniqueId());
        if (board == null) return;
        
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
    
    /**
     * Legacy method for backward compatibility
     */
    public void updateMatchScoreboards(Match match) {
        updateMatchScoreboards(match, "party_ffa");
    }
    
    /**
     * Update duel scoreboards
     * 
     * @param duel The duel to update scoreboards for
     */
    private void updateDuelScoreboards(Duel duel) {
        if (duel == null) return;
        
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
     * 
     * @param player1 The first player
     * @param player2 The second player
     * @param duel The duel the players are in
     */
    public void updateDuelScoreboard(Player player1, Player player2, Duel duel) {
        if (duel == null) return;
        
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
     * 
     * @param player The player to update the scoreboard for
     * @param opponent The opponent player
     * @param duel The duel the players are in
     * @param lines The scoreboard lines from configuration
     */
    private void updateDuelPlayerScoreboard(Player player, Player opponent, Duel duel, List<String> lines) {
        if (player == null || duel == null) return;
        
        FastBoard board = scoreboards.get(player.getUniqueId());
        if (board == null) return;
        
        List<String> playerLines = new ArrayList<>();
        
        for (String line : lines) {
            String processedLine = processDuelPlaceholders(line, player, opponent, duel);
            
            // Skip empty lines and limit to 15 lines max
            if (!processedLine.trim().isEmpty() && playerLines.size() < 15) {
                playerLines.add(processedLine);
            }
        }
        
        board.updateLines(playerLines);
    }
    
    /**
     * Process placeholders for duel scoreboards
     * 
     * @param line The line to process
     * @param player The player
     * @param opponent The opponent player
     * @param duel The duel
     * @return The processed line with placeholders replaced
     */
    private String processDuelPlaceholders(String line, Player player, Player opponent, Duel duel) {
        String processedLine = line;
        
        // Replace basic placeholders
        processedLine = processedLine.replace("{kit}", duel.getKitName());
        processedLine = processedLine.replace("{rounds_to_win}", String.valueOf(duel.getRoundsToWin()));
        processedLine = processedLine.replace("{current_round}", String.valueOf(duel.getCurrentRound()));
        processedLine = processedLine.replace("{total_rounds}", String.valueOf(duel.getRoundsToWin() * 2 - 1));
        
        // Player specific placeholders
        processedLine = processedLine.replace("{player}", player.getName());
        processedLine = processedLine.replace("{opponent}", opponent != null ? opponent.getName() : "Unknown");
        
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
        
        return HexUtils.colorize(processedLine);
    }
    
    /**
     * Process placeholders for match scoreboards
     * 
     * @param line The line to process
     * @param match The match
     * @param player The player
     * @return The processed line with placeholders replaced
     */
    private String processPlaceholders(String line, Match match, Player player) {
        if (line == null || match == null || player == null) {
            return line;
        }
        
        String processedLine = line;
        
        // Basic match info
        processedLine = processedLine.replace("{arena}", match.getArena().getName());
        processedLine = processedLine.replace("{kit}", match.getKit().getDisplayName());
        processedLine = processedLine.replace("{match_type}", match.getMatchType().toUpperCase());
        processedLine = processedLine.replace("{state}", match.getState().toString());
        
        // Player status
        String status = match.isPlayerAlive(player.getUniqueId()) ? "§aAlive" : "§cSpectating";
        processedLine = processedLine.replace("{status}", status);
        
        // Player counts
        processedLine = processedLine.replace("{players_alive}", String.valueOf(match.getAlivePlayersCount()));
        processedLine = processedLine.replace("{players_total}", String.valueOf(match.getAllPlayersUUIDs().size()));
        processedLine = processedLine.replace("{spectators}", String.valueOf(match.getSpectatorsCount()));
        
        // Player stats
        processedLine = processedLine.replace("{kills}", String.valueOf(match.getPlayerKills(player.getUniqueId())));
        processedLine = processedLine.replace("{deaths}", String.valueOf(match.getPlayerDeaths(player.getUniqueId())));
        
        // Process team-specific placeholders
        processedLine = processTeamPlaceholders(processedLine, match, player);
        
        // Time
        processedLine = processedLine.replace("{time}", formatMatchDuration(match.getMatchDuration()));
        
        // Player name
        processedLine = processedLine.replace("{player}", player.getName());
        
        return processedLine;
    }
    
    /**
     * Process team-specific placeholders for match scoreboards
     * 
     * @param line The line to process
     * @param match The match
     * @param player The player
     * @return The processed line with team placeholders replaced
     */
    private String processTeamPlaceholders(String line, Match match, Player player) {
        String processedLine = line;
        
        if ("split".equalsIgnoreCase(match.getMatchType())) {
            int playerTeam = match.getPlayerTeam(player.getUniqueId());
            int opponentTeam = playerTeam == 1 ? 2 : 1;
            
            processedLine = processedLine.replace("{your_team_alive}", String.valueOf(match.getTeamAliveCount(playerTeam)));
            processedLine = processedLine.replace("{your_team_total}", String.valueOf(match.getTeamTotalCount(playerTeam)));
            processedLine = processedLine.replace("{opponent_team_alive}", String.valueOf(match.getTeamAliveCount(opponentTeam)));
            processedLine = processedLine.replace("{opponent_team_total}", String.valueOf(match.getTeamTotalCount(opponentTeam)));
            
            // Individual team counts
            processedLine = processedLine.replace("{team1_alive}", String.valueOf(match.getTeamAliveCount(1)));
            processedLine = processedLine.replace("{team2_alive}", String.valueOf(match.getTeamAliveCount(2)));
        } else {
            // For FFA, team info doesn't apply
            processedLine = processedLine.replace("{your_team_alive}", "N/A");
            processedLine = processedLine.replace("{your_team_total}", "N/A");
            processedLine = processedLine.replace("{opponent_team_alive}", "N/A");
            processedLine = processedLine.replace("{opponent_team_total}", "N/A");
            processedLine = processedLine.replace("{team1_alive}", "N/A");
            processedLine = processedLine.replace("{team2_alive}", "N/A");
        }
        
        return processedLine;
    }
    
    /**
     * Format match duration into a readable time string
     * 
     * @param duration The match duration in milliseconds
     * @return Formatted time string (MM:SS)
     */
    private String formatMatchDuration(long duration) {
        long minutes = duration / 60000;
        long seconds = (duration % 60000) / 1000;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * Removes a player's scoreboard
     * 
     * @param player The player whose scoreboard should be removed
     */
    public void removeScoreboard(Player player) {
        if (player == null) return;
        
        FastBoard board = scoreboards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }
    
    /**
     * Cleans up all scoreboards and tasks when the plugin is disabled
     * Should be called during plugin shutdown
     */
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
        
        plugin.getLogger().info("Cleaned up all scoreboards and tasks");
    }
}
