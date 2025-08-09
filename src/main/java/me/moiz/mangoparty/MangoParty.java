package me.moiz.mangoparty;

import me.moiz.mangoparty.commands.MangoCommand;
import me.moiz.mangoparty.commands.PartyCommand;
import me.moiz.mangoparty.commands.SpectateCommand;
import me.moiz.mangoparty.commands.MangoTabCompleter;
import me.moiz.mangoparty.commands.PartyTabCompleter;
import me.moiz.mangoparty.commands.SpectateTabCompleter;
import me.moiz.mangoparty.config.ConfigManager;
import me.moiz.mangoparty.gui.GuiManager;
import me.moiz.mangoparty.gui.ArenaEditorGui;
import me.moiz.mangoparty.gui.KitEditorGui;
import me.moiz.mangoparty.listeners.PlayerDeathListener;
import me.moiz.mangoparty.listeners.PlayerRespawnListener;
import me.moiz.mangoparty.listeners.KitRulesListener;
import me.moiz.mangoparty.listeners.SpectatorListener;
import me.moiz.mangoparty.listeners.PlayerConnectionListener;
import me.moiz.mangoparty.listeners.ArenaBoundsListener;
import me.moiz.mangoparty.managers.ArenaManager;
import me.moiz.mangoparty.managers.KitManager;
import me.moiz.mangoparty.managers.MatchManager;
import me.moiz.mangoparty.managers.PartyManager;
import me.moiz.mangoparty.managers.ScoreboardManager;
import me.moiz.mangoparty.managers.QueueManager;
import me.moiz.mangoparty.models.Arena;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.commands.QueueCommand;
import me.moiz.mangoparty.commands.LeaveQueueCommand;
import me.moiz.mangoparty.commands.QueueTabCompleter;
import me.moiz.mangoparty.managers.PartyDuelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class MangoParty extends JavaPlugin {
    
    private static MangoParty instance;
    private PartyManager partyManager;
    private ArenaManager arenaManager;
    private KitManager kitManager;
    private MatchManager matchManager;
    private GuiManager guiManager;
    private ConfigManager configManager;
    private ScoreboardManager scoreboardManager;
    private QueueManager queueManager;
    private ArenaEditorGui arenaEditorGui;
    private KitEditorGui kitEditorGui;
    private SpectatorListener spectatorListener;
    private ArenaBoundsListener arenaBoundsListener;
    private Location spawnLocation;
    private PartyDuelManager partyDuelManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Cool startup banner
        printStartupBanner();
        
        logInfo("⚡ Initializing MangoParty systems...");
        
        // Initialize configuration
        logInfo("📁 Loading configuration files...");
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        logSuccess("Configuration loaded successfully!");
        
        // Load spawn location
        loadSpawnLocation();
        
        // Initialize managers
        logInfo("🎮 Initializing core managers...");
        partyManager = new PartyManager();
        arenaManager = new ArenaManager(this);
        kitManager = new KitManager(this);
        matchManager = new MatchManager(this);
        guiManager = new GuiManager(this);
        logSuccess("Core managers initialized!");
        
        // Initialize scoreboard manager
        logInfo("📊 Setting up scoreboard system...");
        scoreboardManager = new ScoreboardManager(this);
        logSuccess("Scoreboard system ready!");

        // Initialize queue manager
        logInfo("🎯 Setting up queue system...");
        queueManager = new QueueManager(this);
        logSuccess("Queue system ready!");

        // Initialize party duel manager
        logInfo("⚔️ Setting up party duel system...");
        partyDuelManager = new PartyDuelManager(this);
        logSuccess("Party duel system ready!");
        
        // Initialize GUI managers
        logInfo("🖥️ Loading GUI systems...");
        arenaEditorGui = new ArenaEditorGui(this);
        kitEditorGui = new KitEditorGui(this);
        logSuccess("GUI systems loaded!");
        
        // Initialize listeners
        logInfo("👂 Registering event listeners...");
        spectatorListener = new SpectatorListener(this);
        arenaBoundsListener = new ArenaBoundsListener(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new KitRulesListener(this), this);
        getServer().getPluginManager().registerEvents(spectatorListener, this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(arenaBoundsListener, this);
        logSuccess("Event listeners registered!");
        
        // Register commands with tab completers
        logInfo("⌨️ Registering commands...");
        getCommand("party").setExecutor(new PartyCommand(this));
        getCommand("party").setTabCompleter(new PartyTabCompleter(this));

        getCommand("mango").setExecutor(new MangoCommand(this));
        getCommand("mango").setTabCompleter(new MangoTabCompleter(this));

        getCommand("spectate").setExecutor(new SpectateCommand(this));
        getCommand("spectate").setTabCompleter(new SpectateTabCompleter(this));

        getCommand("1v1queue").setExecutor(new QueueCommand(this, "1v1"));
        getCommand("1v1queue").setTabCompleter(new QueueTabCompleter(this));

        getCommand("2v2queue").setExecutor(new QueueCommand(this, "2v2"));
        getCommand("2v2queue").setTabCompleter(new QueueTabCompleter(this));

        getCommand("3v3queue").setExecutor(new QueueCommand(this, "3v3"));
        getCommand("3v3queue").setTabCompleter(new QueueTabCompleter(this));

        getCommand("leavequeue").setExecutor(new LeaveQueueCommand(this));
        logSuccess("Commands registered!");
        
        // Display loaded content
        displayLoadedContent();
        
        // Final startup message
        getLogger().info("");
        logSuccess("🎉 MangoParty has been successfully enabled!");
        logInfo("⚡ Ready for epic party battles!");
        getLogger().info("");
    }
    
    private void printStartupBanner() {
        getLogger().info("");
        getLogger().info("╔══════════════════════════════════════╗");
        getLogger().info("║              MANGO PARTY              ║");
        getLogger().info("║                                      ║");
        getLogger().info("║        🥭 Epic Party Battles 🥭        ║");
        getLogger().info("║                                      ║");
        getLogger().info("║           Version: 1.0.0             ║");
        getLogger().info("║           Author: Moiz               ║");
        getLogger().info("╚══════════════════════════════════════╝");
        getLogger().info("");
    }
    
    private void logInfo(String message) {
        getLogger().info(stripColorCodes(message));
    }
    
    private void logSuccess(String message) {
        getLogger().info(stripColorCodes("✓ " + message));
    }

    private String stripColorCodes(String message) {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    private void displayLoadedContent() {
        getLogger().info("");
        getLogger().info("📋 LOADED CONTENT SUMMARY");
        getLogger().info("═══════════════════════════════════");
        
        // Display loaded arenas
        Map<String, Arena> arenas = arenaManager.getArenas();
        getLogger().info("🏟️ Arenas: " + arenas.size() + " loaded");
        if (!arenas.isEmpty()) {
            for (Arena arena : arenas.values()) {
                String status = arena.isComplete() ? "✓ Complete" : "✗ Incomplete";
                getLogger().info("  • " + arena.getName() + " - " + status);
            }
        } else {
            getLogger().info("  • No arenas found! Use /mango arena create <name> to create one.");
        }
        
        getLogger().info("");
        
        // Display loaded kits
        Map<String, Kit> kits = kitManager.getKits();
        getLogger().info("⚔️ Kits: " + kits.size() + " loaded");
        if (!kits.isEmpty()) {
            for (Kit kit : kits.values()) {
                String rules = getKitRulesSummary(kit);
                getLogger().info("  • " + kit.getName() + " - " + rules);
            }
        } else {
            getLogger().info("  • No kits found! Use /mango create kit <name> to create one.");
        }
        
        getLogger().info("");
        
        // Display spawn status
        if (spawnLocation != null) {
            getLogger().info("🏠 Spawn: ✓ Set (" + spawnLocation.getWorld().getName() + 
                       " " + (int)spawnLocation.getX() + ", " + (int)spawnLocation.getY() + 
                       ", " + (int)spawnLocation.getZ() + ")");
        } else {
            getLogger().info("🏠 Spawn: ✗ Not set - Use /mango setspawn");
        }
        
        getLogger().info("");
        getLogger().info("═══════════════════════════════════");
        logSuccess("🚀 All systems operational!");
    }
    
    private String getKitRulesSummary(Kit kit) {
        int activeRules = 0;
        if (!kit.getRules().isNaturalHealthRegen()) activeRules++;
        if (kit.getRules().isBlockBreak()) activeRules++;
        if (kit.getRules().isBlockPlace()) activeRules++;
        if (kit.getRules().getDamageMultiplier() > 1.0) activeRules++;
        if (kit.getRules().isInstantTnt()) activeRules++;
        
        if (activeRules == 0) {
            return "Default rules";
        } else {
            return activeRules + " custom rule" + (activeRules == 1 ? "" : "s");
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("");
        getLogger().info(ChatColor.GOLD + "🛑 Shutting down MangoParty...");
        
        // Clean up any ongoing matches
        if (matchManager != null) {
            getLogger().info(ChatColor.YELLOW + "⏹️ Cleaning up active matches...");
            matchManager.cleanup();
        }
        
        // Clean up scoreboards
        if (scoreboardManager != null) {
            getLogger().info(ChatColor.YELLOW + "📊 Cleaning up scoreboards...");
            scoreboardManager.cleanup();
        }

        // Clean up queues
        if (queueManager != null) {
            getLogger().info("🎯 Cleaning up queues...");
            queueManager.cleanup();
        }

        // Clean up party duels
        if (partyDuelManager != null) {
            getLogger().info("⚔️ Cleaning up party duels...");
            partyDuelManager.cleanup();
        }
        
        logSuccess("All systems shut down cleanly!");
        getLogger().info(ChatColor.GOLD + "🥭 Thanks for using MangoParty! 🥭");
        getLogger().info("");
    }
    
    public static MangoParty getInstance() {
        return instance;
    }
    
    public PartyManager getPartyManager() {
        return partyManager;
    }
    
    public ArenaManager getArenaManager() {
        return arenaManager;
    }
    
    public KitManager getKitManager() {
        return kitManager;
    }
    
    public MatchManager getMatchManager() {
        return matchManager;
    }
    
    public GuiManager getGuiManager() {
        return guiManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }
    
    public ArenaEditorGui getArenaEditorGui() {
        return arenaEditorGui;
    }
    
    public KitEditorGui getKitEditorGui() {
        return kitEditorGui;
    }
    
    public SpectatorListener getSpectatorListener() {
        return spectatorListener;
    }
    
    public ArenaBoundsListener getArenaBoundsListener() {
        return arenaBoundsListener;
    }
    
    public Location getSpawnLocation() {
        return spawnLocation;
    }
    
    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;
        // Save to config
        getConfig().set("spawn.world", location.getWorld().getName());
        getConfig().set("spawn.x", location.getX());
        getConfig().set("spawn.y", location.getY());
        getConfig().set("spawn.z", location.getZ());
        getConfig().set("spawn.yaw", location.getYaw());
        getConfig().set("spawn.pitch", location.getPitch());
        saveConfig();
        
        getLogger().info(ChatColor.GREEN + "🏠 Spawn location updated to: " + location.getWorld().getName() + 
                        " " + (int)location.getX() + ", " + (int)location.getY() + ", " + (int)location.getZ());
    }
    
    private void loadSpawnLocation() {
        if (getConfig().contains("spawn")) {
            String worldName = getConfig().getString("spawn.world");
            if (worldName != null && Bukkit.getWorld(worldName) != null) {
                spawnLocation = new Location(
                    Bukkit.getWorld(worldName),
                    getConfig().getDouble("spawn.x"),
                    getConfig().getDouble("spawn.y"),
                    getConfig().getDouble("spawn.z"),
                    (float) getConfig().getDouble("spawn.yaw"),
                    (float) getConfig().getDouble("spawn.pitch")
                );
            }
        }
    }

    public PartyDuelManager getPartyDuelManager() {
        return partyDuelManager;
    }
}
