package me.moiz.mangoparty;

import me.moiz.mangoparty.commands.*;
import me.moiz.mangoparty.config.ConfigManager;
import me.moiz.mangoparty.gui.GuiManager;
import me.moiz.mangoparty.gui.ArenaEditorGui;
import me.moiz.mangoparty.gui.AllowedKitsGui;
import me.moiz.mangoparty.gui.KitEditorGui;
import me.moiz.mangoparty.listeners.*;
import me.moiz.mangoparty.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for MangoParty.
 * Handles initialization, configuration, and provides access to all managers and components.
 */
public final class MangoParty extends JavaPlugin {
    
    // Managers
    private PartyManager partyManager;
    private ArenaManager arenaManager;
    private KitManager kitManager;
    private MatchManager matchManager;
    private ScoreboardManager scoreboardManager;
    private GuiManager guiManager;
    private ConfigManager configManager;
    private PartyDuelManager partyDuelManager;
    private DuelManager duelManager;
    private QueueManager queueManager;
    
    // GUIs
    private ArenaEditorGui arenaEditorGui;
    private KitEditorGui kitEditorGui;
    private AllowedKitsGui allowedKitsGui;
    
    // Listeners
    private PlayerConnectionListener playerConnectionListener;
    private PlayerDeathListener playerDeathListener;
    private PlayerRespawnListener playerRespawnListener;
    private SpectatorListener spectatorListener;
    private KitRulesListener kitRulesListener;
    private ArenaBoundsListener arenaBoundsListener;
    private DuelListener duelListener;
    private MatchCountdownListener matchCountdownListener;
    
    // Server spawn location
    private Location spawnLocation;
    
    /**
     * Called when the plugin is enabled.
     * Initializes all components, registers listeners and commands, and loads configuration.
     */
    @Override
    public void onEnable() {
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize components in order of dependency
            initializeManagers();
            initializeGuis();
            initializeListeners();
            registerListeners();
            registerCommands();
            
            // Load spawn location
            loadSpawnLocation();
            
            // Startup message
            getLogger().info("MangoParty v" + getDescription().getVersion() + " has been enabled!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable MangoParty: " + e.getMessage());
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * Initializes all manager classes in the correct dependency order.
     */
    private void initializeManagers() {
        getLogger().info("Initializing managers...");
        configManager = new ConfigManager(this);
        partyManager = new PartyManager();
        arenaManager = new ArenaManager(this);
        kitManager = new KitManager(this);
        matchManager = new MatchManager(this);
        scoreboardManager = new ScoreboardManager(this);
        guiManager = new GuiManager(this);
        partyDuelManager = new PartyDuelManager(this);
        duelManager = new DuelManager(this);
        queueManager = new QueueManager(this);
        getLogger().info("All managers initialized successfully.");
    }
    
    /**
     * Initializes all GUI classes.
     */
    private void initializeGuis() {
        getLogger().info("Initializing GUIs...");
        arenaEditorGui = new ArenaEditorGui(this);
        kitEditorGui = new KitEditorGui(this);
        allowedKitsGui = new AllowedKitsGui(this);
        getLogger().info("All GUIs initialized successfully.");
    }
    
    /**
     * Initializes all event listeners.
     */
    private void initializeListeners() {
        getLogger().info("Initializing event listeners...");
        playerConnectionListener = new PlayerConnectionListener(this);
        playerDeathListener = new PlayerDeathListener(this);
        playerRespawnListener = new PlayerRespawnListener(this);
        spectatorListener = new SpectatorListener(this);
        kitRulesListener = new KitRulesListener(this);
        arenaBoundsListener = new ArenaBoundsListener(this);
        duelListener = new DuelListener(this);
        matchCountdownListener = new MatchCountdownListener(this);
        getLogger().info("All event listeners initialized successfully.");
    }
    
    /**
     * Registers all event listeners with Bukkit's plugin manager.
     */
    private void registerListeners() {
        getLogger().info("Registering event listeners...");
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(playerConnectionListener, this);
        pm.registerEvents(playerDeathListener, this);
        pm.registerEvents(playerRespawnListener, this);
        pm.registerEvents(spectatorListener, this);
        pm.registerEvents(kitRulesListener, this);
        pm.registerEvents(arenaBoundsListener, this);
        pm.registerEvents(duelListener, this);
        pm.registerEvents(matchCountdownListener, this);
        getLogger().info("All event listeners registered successfully.");
    }
    
    /**
     * Called when the plugin is disabled.
     * Performs cleanup operations for all managers.
     */
    @Override
    public void onDisable() {
        try {
            getLogger().info("Disabling MangoParty...");
            // Cleanup all managers that need it
            cleanupManagers();
            getLogger().info("MangoParty has been disabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Error during plugin shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Performs cleanup operations for all managers that require it.
     * This ensures proper shutdown of tasks, connections, and resources.
     */
    private void cleanupManagers() {
        getLogger().info("Cleaning up managers...");
        if (matchManager != null) {
            matchManager.cleanup();
            getLogger().info("MatchManager cleaned up.");
        }
        if (queueManager != null) {
            queueManager.cleanup();
            getLogger().info("QueueManager cleaned up.");
        }
        if (partyDuelManager != null) {
            partyDuelManager.cleanup();
            getLogger().info("PartyDuelManager cleaned up.");
        }
        if (duelManager != null) {
            duelManager.cleanup();
            getLogger().info("DuelManager cleaned up.");
        }
        if (scoreboardManager != null) {
            scoreboardManager.cleanup();
            getLogger().info("ScoreboardManager cleaned up.");
        }
        getLogger().info("All managers cleaned up successfully.");
    }
    
    /**
     * Registers all commands with their respective executors and tab completers.
     */
    private void registerCommands() {
        getLogger().info("Registering commands...");
        
        // Core commands
        registerCommand("party", new PartyCommand(this), new PartyTabCompleter(this));
        registerCommand("spectate", new SpectateCommand(this), new SpectateTabCompleter(this));
        registerCommand("mango", new MangoCommand(this), new MangoTabCompleter(this));
        
        // Queue commands
        QueueTabCompleter queueTabCompleter = new QueueTabCompleter();
        registerCommand("1v1queue", new QueueCommand(this, "1v1"), queueTabCompleter);
        registerCommand("2v2queue", new QueueCommand(this, "2v2"), queueTabCompleter);
        registerCommand("3v3queue", new QueueCommand(this, "3v3"), queueTabCompleter);
        registerCommand("leavequeue", new LeaveQueueCommand(this), queueTabCompleter);
        
        // Duel commands
        DuelCommand duelCommand = new DuelCommand(this);
        registerCommand("duel", duelCommand, duelCommand);
        
        // Duel callback command (internal)
        MangoDuelCallbackCommand callbackCommand = new MangoDuelCallbackCommand(this);
        registerCommand("mangoduelcallback", callbackCommand, callbackCommand);
        
        getLogger().info("All commands registered successfully.");
    }
    
    /**
     * Helper method to register a command with its executor and tab completer.
     * 
     * @param name The name of the command
     * @param executor The command executor
     * @param completer The tab completer
     */
    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor, 
                               org.bukkit.command.TabCompleter completer) {
        var command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(completer);
            getLogger().fine("Registered command: " + name);
        } else {
            getLogger().warning("Failed to register command: " + name + ". Command not found in plugin.yml.");
        }
    }
    
    /**
     * Loads the server spawn location from the configuration file.
     * This location is used for teleporting players when they leave matches or parties.
     */
    private void loadSpawnLocation() {
        if (getConfig().contains("spawn")) {
            String worldName = getConfig().getString("spawn.world");
            double x = getConfig().getDouble("spawn.x");
            double y = getConfig().getDouble("spawn.y");
            double z = getConfig().getDouble("spawn.z");
            float yaw = (float) getConfig().getDouble("spawn.yaw");
            float pitch = (float) getConfig().getDouble("spawn.pitch");
            
            if (Bukkit.getWorld(worldName) != null) {
                spawnLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                getLogger().info("Loaded spawn location: " + worldName + " at " + x + ", " + y + ", " + z);
            } else {
                getLogger().warning("Could not load spawn location: world '" + worldName + "' not found!");
            }
        } else {
            getLogger().warning("No spawn location set in config.yml. Players will not be teleported when leaving matches.");
        }
    }
    
    /**
     * Saves the server spawn location to the configuration file.
     * 
     * @param location The location to save as the spawn point
     */
    public void saveSpawnLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            getLogger().warning("Cannot save null spawn location!");
            return;
        }
        
        getConfig().set("spawn.world", location.getWorld().getName());
        getConfig().set("spawn.x", location.getX());
        getConfig().set("spawn.y", location.getY());
        getConfig().set("spawn.z", location.getZ());
        getConfig().set("spawn.yaw", location.getYaw());
        getConfig().set("spawn.pitch", location.getPitch());
        saveConfig();
        spawnLocation = location;
        
        getLogger().info("Saved spawn location: " + location.getWorld().getName() + 
                         " at " + location.getX() + ", " + location.getY() + ", " + location.getZ());
    }
    
    /**
     * Sets the server spawn location.
     * Alias for saveSpawnLocation for API consistency.
     * 
     * @param location The location to set as the spawn point
     */
    public void setSpawnLocation(Location location) {
        saveSpawnLocation(location);
    }
    
    // Getters
    /**
     * @return The party manager instance
     */
    public PartyManager getPartyManager() { return partyManager; }
    
    /**
     * @return The arena manager instance
     */
    public ArenaManager getArenaManager() { return arenaManager; }
    
    /**
     * @return The kit manager instance
     */
    public KitManager getKitManager() { return kitManager; }
    
    /**
     * @return The match manager instance
     */
    public MatchManager getMatchManager() { return matchManager; }
    
    /**
     * @return The scoreboard manager instance
     */
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    
    /**
     * @return The GUI manager instance
     */
    public GuiManager getGuiManager() { return guiManager; }
    
    /**
     * @return The config manager instance
     */
    public ConfigManager getConfigManager() { return configManager; }
    
    /**
     * @return The party duel manager instance
     */
    public PartyDuelManager getPartyDuelManager() { return partyDuelManager; }
    
    /**
     * @return The duel manager instance
     */
    public DuelManager getDuelManager() { return duelManager; }
    
    /**
     * @return The queue manager instance
     */
    public QueueManager getQueueManager() { return queueManager; }
    
    /**
     * @return The arena editor GUI instance
     */
    public ArenaEditorGui getArenaEditorGui() { return arenaEditorGui; }
    
    /**
     * @return The kit editor GUI instance
     */
    public KitEditorGui getKitEditorGui() { return kitEditorGui; }
    
    /**
     * @return The allowed kits GUI instance
     */
    public AllowedKitsGui getAllowedKitsGui() { return allowedKitsGui; }
    
    /**
     * @return The spectator listener instance
     */
    public SpectatorListener getSpectatorListener() { return spectatorListener; }
    
    /**
     * @return The player death listener instance
     */
    public PlayerDeathListener getPlayerDeathListener() { return playerDeathListener; }
    
    /**
     * @return The duel listener instance
     */
    public DuelListener getDuelListener() { return duelListener; }
    
    /**
     * @return The match countdown listener instance
     */
    public MatchCountdownListener getMatchCountdownListener() { return matchCountdownListener; }
    
    /**
     * @return The server spawn location
     */
    public Location getSpawnLocation() { return spawnLocation; }
}
