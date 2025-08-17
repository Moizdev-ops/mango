package me.moiz.mangoparty.managers;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import me.moiz.mangoparty.models.Duel;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.models.Match;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelManager {
    private MangoParty plugin;
    private Map<UUID, Duel> pendingDuels; // Target player UUID -> Duel
    private Map<String, Duel> activeDuels; // Duel ID -> Duel
    private Map<UUID, String> playerDuels; // Player UUID -> Duel ID
    private Map<String, BukkitTask> countdownTasks; // Duel ID -> Task
    private Map<UUID, UUID> duelIdMap; // Duel UUID -> Target UUID (for callback lookup)
    
    public DuelManager(MangoParty plugin) {
        this.plugin = plugin;
        this.pendingDuels = new HashMap<>();
        this.activeDuels = new HashMap<>();
        this.playerDuels = new HashMap<>();
        this.countdownTasks = new HashMap<>();
        this.duelIdMap = new HashMap<>();
    }
    
    /**
     * Challenge a player to a duel
     */
    public void challengePlayer(Player challenger, Player target, String kitName, int roundsToWin) {
        // Check if challenger is already in a match
        if (plugin.getMatchManager().isInMatch(challenger)) {
            challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                 plugin.getConfig().getString("messages.player-duel.already-in-match"));
            return;
        }
        
        // Check if target is already in a match
        if (plugin.getMatchManager().isInMatch(target)) {
            challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                 plugin.getConfig().getString("messages.player-duel.already-in-match"));
            return;
        }
        
        // Check if target is in a party
        if (plugin.getPartyManager().hasParty(target)) {
            challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                 "§cYou cannot duel a player who is in a party!");
            return;
        }
        
        // Check if there's already a pending duel
        if (pendingDuels.containsKey(target.getUniqueId())) {
            challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                 plugin.getConfig().getString("messages.player-duel.already-has-request")
                                 .replace("{target}", target.getName()));
            return;
        }
        
        // Check if kit exists
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                 plugin.getConfig().getString("messages.player-duel.kit-not-found")
                                 .replace("{kit}", kitName));
            return;
        }
        
        // Check if rounds is valid
        if (roundsToWin < 1 || roundsToWin > 10) {
            challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                 plugin.getConfig().getString("messages.player-duel.invalid-rounds"));
            return;
        }
        
        // Create duel request with UUID
        Duel duel = new Duel(challenger, target, kitName, roundsToWin);
        UUID duelUuid = UUID.randomUUID();
        duel.setUuid(duelUuid);
        pendingDuels.put(target.getUniqueId(), duel);
        duelIdMap.put(duelUuid, target.getUniqueId());
        
        // Send messages
        challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                             plugin.getConfig().getString("messages.player-duel.challenge-sent")
                             .replace("{target}", target.getName())
                             .replace("{kit}", kit.getDisplayName())
                             .replace("{rounds}", String.valueOf(roundsToWin)));
        
        // Create stylish duel challenge message with clickable buttons
        // Header
        TextComponent header = new TextComponent("\n§8§l§m-----§r §6§lDUEL CHALLENGE §8§l§m-----§r\n");
        
        // Challenger info
        TextComponent message = new TextComponent("§e" + challenger.getName() + " §7has challenged you to a duel!\n");
        
        // Kit and rounds info
        TextComponent kitInfo = new TextComponent("§7Kit: §b" + kit.getDisplayName() + " §7| Rounds to win: §b" + roundsToWin + "\n\n");
        
        // Buttons with improved styling
        TextComponent acceptButton = new TextComponent("§a§l[ACCEPT]");
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mangoduelcallback accept " + duelUuid.toString()));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("§a§lClick to accept the duel challenge").create()));
        
        TextComponent spacer = new TextComponent("   ");
        
        TextComponent declineButton = new TextComponent("§c§l[DECLINE]");
        declineButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mangoduelcallback decline " + duelUuid.toString()));
        declineButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("§c§lClick to decline the duel challenge").create()));
        
        // Footer
        TextComponent footer = new TextComponent("\n§8§l§m--------------------------§r\n");
        
        // Send to target player
        target.spigot().sendMessage(header);
        target.spigot().sendMessage(message);
        target.spigot().sendMessage(kitInfo);
        target.spigot().sendMessage(acceptButton, spacer, declineButton);
        target.spigot().sendMessage(footer);
        
        // Play sound to notify the player
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        
        // Set expiration timer
        duel.setExpirationTask(new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingDuels.remove(target.getUniqueId()) != null) {
                    if (challenger.isOnline()) {
                        challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                             plugin.getConfig().getString("messages.player-duel.challenge-expired")
                                             .replace("{target}", target.getName()));
                    }
                    if (target.isOnline()) {
                        target.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                       plugin.getConfig().getString("messages.player-duel.challenge-expired-target")
                                       .replace("{challenger}", challenger.getName()));
                    }
                }
            }
        }.runTaskLater(plugin, 1200L)); // 60 seconds
    }
    
    /**
     * Accept a duel request by UUID (for callback)
     */
    public void acceptDuelById(Player accepter, UUID duelUuid) {
        UUID targetUuid = duelIdMap.get(duelUuid);
        if (targetUuid == null || !targetUuid.equals(accepter.getUniqueId())) {
            accepter.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.no-pending-duel"));
            return;
        }
        
        Duel duel = pendingDuels.get(accepter.getUniqueId());
        if (duel == null) {
            accepter.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.no-pending-duel"));
            duelIdMap.remove(duelUuid);
            return;
        }
        
        Player challenger = duel.getChallenger();
        if (challenger == null || !challenger.isOnline()) {
            accepter.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.duel-not-found"));
            pendingDuels.remove(accepter.getUniqueId());
            duelIdMap.remove(duelUuid);
            return;
        }
        
        // Continue with the duel acceptance process
        processDuelAcceptance(duel, accepter, challenger);
        
        // Update scoreboard for both players
        plugin.getScoreboardManager().updateDuelScoreboard(challenger, accepter, duel);
    }
    
    /**
     * Accept a duel request (legacy method for compatibility)
     */
    public void acceptDuel(Player accepter, String challengerName) {
        Duel duel = pendingDuels.get(accepter.getUniqueId());
        if (duel == null) {
            accepter.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.no-pending-duel"));
            return;
        }
        
        Player challenger = duel.getChallenger();
        if (challenger == null || !challenger.isOnline() || !challenger.getName().equalsIgnoreCase(challengerName)) {
            accepter.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.duel-not-found"));
            pendingDuels.remove(accepter.getUniqueId());
            return;
        }
        
        // Continue with the duel acceptance process
        processDuelAcceptance(duel, accepter, challenger);
    }
    
    /**
     * Process duel acceptance (common code for both acceptance methods)
     */
    private void processDuelAcceptance(Duel duel, Player accepter, Player challenger) {
        
        // Check if either player is in a match
        if (plugin.getMatchManager().isInMatch(challenger) || plugin.getMatchManager().isInMatch(accepter)) {
            accepter.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.already-in-match"));
            challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                plugin.getConfig().getString("messages.player-duel.already-in-match"));
            pendingDuels.remove(accepter.getUniqueId());
            return;
        }
        
        // Get kit
        Kit kit = plugin.getKitManager().getKit(duel.getKitName());
        if (kit == null) {
            accepter.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.kit-not-found"));
            challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                plugin.getConfig().getString("messages.player-duel.kit-not-found"));
            pendingDuels.remove(accepter.getUniqueId());
            return;
        }
        
        // Find an available arena that allows this kit
        Arena arena = plugin.getArenaManager().getAvailableArenaForKit(kit.getName());
        
        // If no available arena found, create an instance of the original arena
        if (arena == null) {
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
                arena = plugin.getArenaManager().createArenaInstance(baseArena, kit.getName());
            }
        }
        
        if (arena == null) {
            accepter.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.no-available-arenas"));
            challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                plugin.getConfig().getString("messages.player-duel.no-available-arenas"));
            pendingDuels.remove(accepter.getUniqueId());
            return;
        }
        
        // Reserve arena
        plugin.getArenaManager().reserveArena(arena.getName());
        
        // Generate duel ID
        String duelId = "duel_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        
        // Set up duel
        duel.setId(duelId);
        duel.setArena(arena);
        duel.setState(Duel.DuelState.PREPARING);
        
        // Store duel
        activeDuels.put(duelId, duel);
        playerDuels.put(challenger.getUniqueId(), duelId);
        playerDuels.put(accepter.getUniqueId(), duelId);
        
        // Remove from pending
        pendingDuels.remove(accepter.getUniqueId());
        
        // Cancel expiration task
        if (duel.getExpirationTask() != null) {
            duel.getExpirationTask().cancel();
        }
        
        // Notify players
        accepter.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                           plugin.getConfig().getString("messages.player-duel.challenge-accepted")
                           .replace("{kit}", kit.getDisplayName()));
        challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                            plugin.getConfig().getString("messages.player-duel.challenge-accepted")
                            .replace("{kit}", kit.getDisplayName())
                            .replace("{target}", accepter.getName()));
        
        // Start scoreboards for the duel
        plugin.getScoreboardManager().startDuelScoreboards(duel);
        
        // Start the duel
        startDuel(duel);
    }
    
    /**
     * Decline a duel request by UUID (for callback)
     */
    public void declineDuelById(Player decliner, UUID duelUuid) {
        UUID targetUuid = duelIdMap.get(duelUuid);
        if (targetUuid == null || !targetUuid.equals(decliner.getUniqueId())) {
            decliner.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.no-pending-duel"));
            return;
        }
        
        Duel duel = pendingDuels.get(decliner.getUniqueId());
        if (duel == null) {
            decliner.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.no-pending-duel"));
            duelIdMap.remove(duelUuid);
            return;
        }
        
        Player challenger = duel.getChallenger();
        if (challenger == null) {
            decliner.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.duel-not-found"));
            pendingDuels.remove(decliner.getUniqueId());
            duelIdMap.remove(duelUuid);
            return;
        }
        
        // Process the duel decline
        processDuelDecline(duel, decliner, challenger);
    }
    
    /**
     * Decline a duel request (legacy method for compatibility)
     */
    public void declineDuel(Player decliner, String challengerName) {
        Duel duel = pendingDuels.get(decliner.getUniqueId());
        if (duel == null) {
            decliner.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.no-pending-duel"));
            return;
        }
        
        Player challenger = duel.getChallenger();
        if (challenger == null || !challenger.getName().equalsIgnoreCase(challengerName)) {
            decliner.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.duel-not-found"));
            return;
        }
        
        // Process the duel decline
        processDuelDecline(duel, decliner, challenger);
    }
    
    /**
     * Process duel decline (common code for both decline methods)
     */
    private void processDuelDecline(Duel duel, Player decliner, Player challenger) {
        // Notify players
        if (challenger.isOnline()) {
            challenger.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                plugin.getConfig().getString("messages.player-duel.challenge-declined")
                                .replace("{target}", decliner.getName()));
        }
        decliner.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                           plugin.getConfig().getString("messages.player-duel.challenge-declined")
                           .replace("{target}", challenger.getName()));
        
        // Remove duel and cancel expiration task
        UUID duelUuid = duel.getUuid();
        pendingDuels.remove(decliner.getUniqueId());
        if (duelUuid != null) {
            duelIdMap.remove(duelUuid);
        }
        if (duel.getExpirationTask() != null) {
            duel.getExpirationTask().cancel();
        }
    }
    
    /**
     * Start a duel
     */
    private void startDuel(Duel duel) {
        Player player1 = duel.getChallenger();
        Player player2 = duel.getTarget();
        Arena arena = duel.getArena();
        
        // Set gamerule for immediate respawn
        if (arena.getCenter() != null && arena.getCenter().getWorld() != null) {
            arena.getCenter().getWorld().setGameRuleValue("doImmediateRespawn", "true");
        } else {
            plugin.getLogger().warning("Cannot set gamerule: arena center or world is null for arena " + arena.getName());
        }
        
        // Regenerate arena
        plugin.getArenaManager().pasteSchematic(arena);
        
        // Heal and feed players
        player1.setHealth(20.0);
        player1.setFoodLevel(20);
        player1.setSaturation(20.0f);
        player2.setHealth(20.0);
        player2.setFoodLevel(20);
        player2.setSaturation(20.0f);
        
        // Teleport players to their spawns
        player1.teleport(arena.getSpawn1());
        player2.teleport(arena.getSpawn2());
        
        // Start countdown
        startDuelCountdown(duel);
    }
    
    /**
     * Start the countdown for a duel
     */
    private void startDuelCountdown(Duel duel) {
        Player player1 = duel.getChallenger();
        Player player2 = duel.getTarget();
        Kit kit = plugin.getKitManager().getKit(duel.getKitName());
        
        duel.setState(Duel.DuelState.COUNTDOWN);
        
        // Give kits to players
        plugin.getKitManager().giveKit(player1, kit);
        plugin.getKitManager().giveKit(player2, kit);
        
        // Set game mode to adventure and make players invincible during countdown
        player1.setGameMode(GameMode.ADVENTURE);
        player2.setGameMode(GameMode.ADVENTURE);
        
        // Make players invincible during countdown
        player1.setInvulnerable(true);
        player2.setInvulnerable(true);
        
        // Prevent movement and attacking during countdown
        player1.setWalkSpeed(0.0f);
        player2.setWalkSpeed(0.0f);
        
        BukkitTask countdownTask = new BukkitRunnable() {
            int countdown = 10; // Increased countdown time to give more time to organize inventory
            
            @Override
            public void run() {
                if (countdown > 0) {
                    // Display countdown with colorful emoji numbers
                    String countdownNumber;
                    String countdownColor;
                    
                    // Determine emoji and color based on countdown value
                    if (countdown == 10) {
                        countdownNumber = "❿";
                        countdownColor = "§4"; // Dark Red
                    } else if (countdown == 9) {
                        countdownNumber = "❾";
                        countdownColor = "§c"; // Red
                    } else if (countdown == 8) {
                        countdownNumber = "❽";
                        countdownColor = "§6"; // Gold
                    } else if (countdown == 7) {
                        countdownNumber = "❼";
                        countdownColor = "§e"; // Yellow
                    } else if (countdown == 6) {
                        countdownNumber = "❻";
                        countdownColor = "§2"; // Dark Green
                    } else if (countdown == 5) {
                        countdownNumber = "❺";
                        countdownColor = "§a"; // Green
                    } else if (countdown == 4) {
                        countdownNumber = "❹";
                        countdownColor = "§b"; // Aqua
                    } else if (countdown == 3) {
                        countdownNumber = "❸";
                        countdownColor = "§9"; // Blue
                    } else if (countdown == 2) {
                        countdownNumber = "❷";
                        countdownColor = "§d"; // Light Purple
                    } else if (countdown == 1) {
                        countdownNumber = "❶";
                        countdownColor = "§5"; // Dark Purple
                    } else {
                        countdownNumber = String.valueOf(countdown);
                        countdownColor = "§f"; // White
                    }
                    
                    if (player1.isOnline()) {
                        player1.sendTitle(countdownColor + countdownNumber, "§eOrganize your inventory", 0, 20, 0);
                        player1.playSound(player1.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                    if (player2.isOnline()) {
                        player2.sendTitle(countdownColor + countdownNumber, "§eOrganize your inventory", 0, 20, 0);
                        player2.playSound(player2.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                    countdown--;
                } else {
                    // Save inventories
                    if (player1.isOnline() && player2.isOnline()) {
                        // Create deep copies of inventory contents to preserve organization
                        ItemStack[] player1Contents = new ItemStack[player1.getInventory().getContents().length];
                        ItemStack[] player1Armor = new ItemStack[player1.getInventory().getArmorContents().length];
                        ItemStack player1Offhand = player1.getInventory().getItemInOffHand().clone();
                        
                        ItemStack[] player2Contents = new ItemStack[player2.getInventory().getContents().length];
                        ItemStack[] player2Armor = new ItemStack[player2.getInventory().getArmorContents().length];
                        ItemStack player2Offhand = player2.getInventory().getItemInOffHand().clone();
                        
                        // Deep copy each item to preserve organization
                        for (int i = 0; i < player1.getInventory().getContents().length; i++) {
                            ItemStack item = player1.getInventory().getContents()[i];
                            player1Contents[i] = (item != null) ? item.clone() : null;
                        }
                        
                        for (int i = 0; i < player1.getInventory().getArmorContents().length; i++) {
                            ItemStack item = player1.getInventory().getArmorContents()[i];
                            player1Armor[i] = (item != null) ? item.clone() : null;
                        }
                        
                        for (int i = 0; i < player2.getInventory().getContents().length; i++) {
                            ItemStack item = player2.getInventory().getContents()[i];
                            player2Contents[i] = (item != null) ? item.clone() : null;
                        }
                        
                        for (int i = 0; i < player2.getInventory().getArmorContents().length; i++) {
                            ItemStack item = player2.getInventory().getArmorContents()[i];
                            player2Armor[i] = (item != null) ? item.clone() : null;
                        }
                        
                        // Save the deep copies to the duel object
                        duel.setPlayer1Inventory(player1Contents);
                        duel.setPlayer1Armor(player1Armor);
                        duel.setPlayer1Offhand(player1Offhand);
                        duel.setPlayer2Inventory(player2Contents);
                        duel.setPlayer2Armor(player2Armor);
                        duel.setPlayer2Offhand(player2Offhand);
                        
                        // Notify players that their inventory has been saved
                        player1.sendMessage("§aYour inventory has been saved for all rounds!");
                        player2.sendMessage("§aYour inventory has been saved for all rounds!");
                        
                        // Start the round
                        startRound(duel);
                    } else {
                        // One of the players disconnected
                        endDuel(duel, player1.isOnline() ? player1.getUniqueId() : player2.getUniqueId());
                    }
                    
                    this.cancel();
                    countdownTasks.remove(duel.getId());
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        countdownTasks.put(duel.getId(), countdownTask);
    }
    
    /**
     * Start a round in the duel
     */
    private void startRound(Duel duel) {
        Player player1 = duel.getChallenger();
        Player player2 = duel.getTarget();
        
        duel.setState(Duel.DuelState.ACTIVE);
        duel.incrementCurrentRound();
        
        // Display round start message
        if (player1.isOnline()) {
            player1.sendTitle("§aGO!", "§eRound " + duel.getCurrentRound(), 0, 20, 10);
            player1.playSound(player1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            player1.setGameMode(GameMode.SURVIVAL);
            player1.setWalkSpeed(0.2f); // Reset walk speed to normal (default is 0.2)
            player1.setInvulnerable(false); // Remove invincibility when duel starts
        }
        
        if (player2.isOnline()) {
            player2.sendTitle("§aGO!", "§eRound " + duel.getCurrentRound(), 0, 20, 10);
            player2.playSound(player2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            player2.setGameMode(GameMode.SURVIVAL);
            player2.setWalkSpeed(0.2f); // Reset walk speed to normal (default is 0.2)
            player2.setInvulnerable(false); // Remove invincibility when duel starts
        }
        
        // Clear titles after 1 second
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player1.isOnline()) {
                player1.sendTitle("", "", 0, 0, 0);
            }
            if (player2.isOnline()) {
                player2.sendTitle("", "", 0, 0, 0);
            }
        }, 20L);
    }
    
    /**
     * Handle a player death in a duel
     */
    public void handlePlayerDeath(Player player) {
        String duelId = playerDuels.get(player.getUniqueId());
        if (duelId == null) return;
        
        Duel duel = activeDuels.get(duelId);
        if (duel == null) return;
        
        // Determine winner of the round
        Player winner = null;
        Player loser = player;
        if (player.getUniqueId().equals(duel.getChallenger().getUniqueId())) {
            winner = duel.getTarget();
            duel.incrementPlayer2Wins();
        } else {
            winner = duel.getChallenger();
            duel.incrementPlayer1Wins();
        }
        
        // Make both players invincible and clear their inventories
        if (winner.isOnline()) {
            winner.setInvulnerable(true);
            winner.getInventory().clear();
            winner.getInventory().setArmorContents(null);
            winner.getInventory().setItemInOffHand(null);
            winner.updateInventory();
        }
        
        if (loser.isOnline()) {
            loser.setInvulnerable(true);
            loser.getInventory().clear();
            loser.getInventory().setArmorContents(null);
            loser.getInventory().setItemInOffHand(null);
            loser.updateInventory();
        }
        
        // Store winner UUID for lambda to avoid effectively final issue
        final UUID winnerUUID = winner.getUniqueId();
        
        // Check if duel is over
        if (duel.getPlayer1Wins() >= duel.getRoundsToWin() || duel.getPlayer2Wins() >= duel.getRoundsToWin()) {
            // Duel is over - skip delay on final round
            endDuel(duel, winnerUUID);
        } else {
            // Make players invulnerable during the delay
            Player player1 = duel.getChallenger();
            Player player2 = duel.getTarget();
            
            if (player1 != null && player1.isOnline()) {
                player1.setInvulnerable(true);
                player1.setGameMode(GameMode.ADVENTURE);
            }
            
            if (player2 != null && player2.isOnline()) {
                player2.setInvulnerable(true);
                player2.setGameMode(GameMode.ADVENTURE);
            }
            
            // Wait 2 seconds before proceeding to next round
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Prepare for next round
                prepareNextRound(duel);
            }, 40L); // 40 ticks = 2 seconds
        }
    }
    
    /**
     * Prepare for the next round
     */
    private void prepareNextRound(Duel duel) {
        duel.setState(Duel.DuelState.PREPARING);
        
        Player player1 = duel.getChallenger();
        Player player2 = duel.getTarget();
        Arena arena = duel.getArena();
        
        // Announce round result
        String roundResult = plugin.getConfig().getString("messages.player-duel.round-ended")
                             .replace("{round}", String.valueOf(duel.getCurrentRound()))
                             .replace("{player1}", player1.getName())
                             .replace("{score1}", String.valueOf(duel.getPlayer1Wins()))
                             .replace("{score2}", String.valueOf(duel.getPlayer2Wins()))
                             .replace("{player2}", player2.getName());
        
        if (player1.isOnline()) {
            player1.sendMessage(roundResult);
        }
        if (player2.isOnline()) {
            player2.sendMessage(roundResult);
        }
        
        // Clear all entities and drops in the arena
        clearArenaEntities(arena);
        
        // Regenerate arena
        plugin.getArenaManager().pasteSchematic(arena);
        
        // Wait 1 second after regeneration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!activeDuels.containsKey(duel.getId())) return; // Duel was ended
            
            // Teleport players back to spawn points
            if (player1.isOnline()) {
                // Reset invulnerability
                player1.setInvulnerable(false);
                
                // Clear all potion effects
                player1.getActivePotionEffects().forEach(effect -> player1.removePotionEffect(effect.getType()));
                
                player1.teleport(arena.getSpawn1());
                player1.setHealth(20.0);
                player1.setFoodLevel(20);
                player1.setSaturation(20.0f);
                
                // Clear inventory first to avoid any leftover items
                player1.getInventory().clear();
                player1.getInventory().setArmorContents(null);
                player1.getInventory().setItemInOffHand(null);
                
                // Restore saved inventory from first round
                player1.getInventory().setContents(duel.getPlayer1Inventory());
                player1.getInventory().setArmorContents(duel.getPlayer1Armor());
                player1.getInventory().setItemInOffHand(duel.getPlayer1Offhand());
                player1.updateInventory();
            }
            
            if (player2.isOnline()) {
                // Reset invulnerability
                player2.setInvulnerable(false);
                
                // Clear all potion effects
                player2.getActivePotionEffects().forEach(effect -> player2.removePotionEffect(effect.getType()));
                
                player2.teleport(arena.getSpawn2());
                player2.setHealth(20.0);
                player2.setFoodLevel(20);
                player2.setSaturation(20.0f);
                
                // Clear inventory first to avoid any leftover items
                player2.getInventory().clear();
                player2.getInventory().setArmorContents(null);
                player2.getInventory().setItemInOffHand(null);
                
                // Restore saved inventory from first round
                player2.getInventory().setContents(duel.getPlayer2Inventory());
                player2.getInventory().setArmorContents(duel.getPlayer2Armor());
                player2.getInventory().setItemInOffHand(duel.getPlayer2Offhand());
                player2.updateInventory();
            }
            
            // Start countdown for next round
            startNextRoundCountdown(duel);
        }, 20L); // 1 second delay
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
     * Start countdown for the next round
     */
    private void startNextRoundCountdown(Duel duel) {
        Player player1 = duel.getChallenger();
        Player player2 = duel.getTarget();
        
        duel.setState(Duel.DuelState.COUNTDOWN);
        
        // Set game mode to adventure and make players invincible during countdown
        if (player1.isOnline()) {
            player1.setGameMode(GameMode.ADVENTURE);
            player1.setWalkSpeed(0.0f); // Prevent movement during countdown
            player1.setInvulnerable(true); // Make player invincible during countdown
        }
        
        if (player2.isOnline()) {
            player2.setGameMode(GameMode.ADVENTURE);
            player2.setWalkSpeed(0.0f); // Prevent movement during countdown
            player2.setInvulnerable(true); // Make player invincible during countdown
        }
        
        BukkitTask countdownTask = new BukkitRunnable() {
            int countdown = 5;
            
            @Override
            public void run() {
                if (countdown > 0) {
                    // Display countdown
                    if (player1.isOnline()) {
                        player1.sendTitle("§c" + countdown, "§eGet ready", 0, 20, 0);
                        player1.playSound(player1.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                    if (player2.isOnline()) {
                        player2.sendTitle("§c" + countdown, "§eGet ready", 0, 20, 0);
                        player2.playSound(player2.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                    countdown--;
                } else {
                    // Start the round
                    startRound(duel);
                    
                    this.cancel();
                    countdownTasks.remove(duel.getId());
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        countdownTasks.put(duel.getId(), countdownTask);
    }
    
    /**
     * End a duel
     */
    public void endDuel(Duel duel, UUID winnerUuid) {
        duel.setState(Duel.DuelState.ENDING);
        
        Player player1 = duel.getChallenger();
        Player player2 = duel.getTarget();
        Player winner = Bukkit.getPlayer(winnerUuid);
        Player loser = winner != null && winner.equals(player1) ? player2 : player1;
        
        // Reset invulnerability for both players
        if (player1.isOnline()) {
            player1.setInvulnerable(false);
        }
        
        if (player2.isOnline()) {
            player2.setInvulnerable(false);
        }
        
        // Announce winner
        String winMessage = plugin.getConfig().getString("messages.player-duel.duel-victory")
                           .replace("{winner}", winner != null ? winner.getName() : "Unknown")
                           .replace("{loser}", loser != null ? loser.getName() : "Unknown")
                           .replace("{winner_score}", String.valueOf(winner != null && winner.equals(player1) ? duel.getPlayer1Wins() : duel.getPlayer2Wins()))
                           .replace("{loser_score}", String.valueOf(winner != null && winner.equals(player1) ? duel.getPlayer2Wins() : duel.getPlayer1Wins()));
        
        if (player1.isOnline()) {
            player1.sendMessage(winMessage);
            player1.sendTitle(winner != null && winner.equals(player1) ? "§6§lVICTORY!" : "§c§lDEFEAT!", winMessage, 10, 60, 10);
        }
        
        if (player2.isOnline()) {
            player2.sendMessage(winMessage);
            player2.sendTitle(winner != null && winner.equals(player2) ? "§6§lVICTORY!" : "§c§lDEFEAT!", winMessage, 10, 60, 10);
        }
        
        // Release the arena
        plugin.getArenaManager().releaseArena(duel.getArena().getName());
        
        // Teleport players to spawn after 3 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Reset player states
            if (player1.isOnline()) {
                player1.setGameMode(GameMode.SURVIVAL);
                player1.getInventory().clear();
                player1.getInventory().setArmorContents(null);
                
                // Teleport to spawn
                if (plugin.getSpawnLocation() != null) {
                    player1.teleport(plugin.getSpawnLocation());
                }
                
                // Clear any remaining titles
                player1.sendTitle("", "", 0, 0, 0);
                
                // Remove scoreboard
                plugin.getScoreboardManager().removeScoreboard(player1);
            }
            
            if (player2.isOnline()) {
                player2.setGameMode(GameMode.SURVIVAL);
                player2.getInventory().clear();
                player2.getInventory().setArmorContents(null);
                
                // Teleport to spawn
                if (plugin.getSpawnLocation() != null) {
                    player2.teleport(plugin.getSpawnLocation());
                }
                
                // Clear any remaining titles
                player2.sendTitle("", "", 0, 0, 0);
                
                // Remove scoreboard
                plugin.getScoreboardManager().removeScoreboard(player2);
            }
            
            // Remove duel
            activeDuels.remove(duel.getId());
            playerDuels.remove(player1.getUniqueId());
            playerDuels.remove(player2.getUniqueId());
            
            // Cancel scoreboard update task
            plugin.getScoreboardManager().cancelTask(duel.getId());
            
            // Cancel countdown task if exists
            BukkitTask task = countdownTasks.remove(duel.getId());
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
            
            // Regenerate arena
            plugin.getArenaManager().pasteSchematic(duel.getArena());
        }, 60L); // 3 seconds delay
    }
    
    /**
     * Get a player's active duel
     */
    public Duel getPlayerDuel(Player player) {
        String duelId = playerDuels.get(player.getUniqueId());
        return duelId != null ? activeDuels.get(duelId) : null;
    }
    
    /**
     * Check if a player is in a duel
     */
    public boolean isInDuel(Player player) {
        return playerDuels.containsKey(player.getUniqueId());
    }
    
    /**
     * Handle player disconnect during a duel
     */
    public void handlePlayerDisconnect(Player player) {
        // Check if player has a pending duel request
        if (pendingDuels.containsKey(player.getUniqueId())) {
            Duel duel = pendingDuels.get(player.getUniqueId());
            Player challenger = duel.getChallenger();
            
            if (challenger != null && challenger.isOnline()) {
                challenger.sendMessage("§c" + player.getName() + " disconnected. Duel request cancelled.");
            }
            
            // Cancel expiration task
            if (duel.getExpirationTask() != null) {
                duel.getExpirationTask().cancel();
            }
            
            pendingDuels.remove(player.getUniqueId());
        }
        
        // Check if player is in an active duel
        String duelId = playerDuels.get(player.getUniqueId());
        if (duelId != null) {
            Duel duel = activeDuels.get(duelId);
            if (duel != null) {
                // Determine the winner (the player who didn't disconnect)
                UUID winnerUuid;
                if (player.getUniqueId().equals(duel.getChallenger().getUniqueId())) {
                    winnerUuid = duel.getTarget().getUniqueId();
                } else {
                    winnerUuid = duel.getChallenger().getUniqueId();
                }
                
                // End the duel
                endDuel(duel, winnerUuid);
            }
        }
    }
    
    /**
     * Clean up all duels
     */
    public void cleanup() {
        // Cancel all pending duel expiration tasks
        for (Duel duel : pendingDuels.values()) {
            if (duel.getExpirationTask() != null) {
                duel.getExpirationTask().cancel();
            }
        }
        pendingDuels.clear();
        
        // Cancel all countdown tasks
        for (BukkitTask task : countdownTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        countdownTasks.clear();
        
        // Release all reserved arenas
        for (Duel duel : activeDuels.values()) {
            plugin.getArenaManager().releaseArena(duel.getArena().getName());
            
            // Reset players if online
            Player player1 = duel.getChallenger();
            Player player2 = duel.getTarget();
            
            if (player1 != null && player1.isOnline()) {
                player1.setGameMode(GameMode.SURVIVAL);
                if (plugin.getSpawnLocation() != null) {
                    player1.teleport(plugin.getSpawnLocation());
                }
            }
            
            if (player2 != null && player2.isOnline()) {
                player2.setGameMode(GameMode.SURVIVAL);
                if (plugin.getSpawnLocation() != null) {
                    player2.teleport(plugin.getSpawnLocation());
                }
            }
        }
        
        activeDuels.clear();
        playerDuels.clear();
    }
}