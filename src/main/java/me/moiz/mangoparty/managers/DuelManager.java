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
        
        // Create clickable accept/decline buttons with callback UUIDs
        TextComponent message = new TextComponent(plugin.getConfig().getString("messages.player-duel.challenge-received")
                                                .replace("{challenger}", challenger.getName()));
        
        TextComponent kitInfo = new TextComponent(plugin.getConfig().getString("messages.player-duel.kit-info")
                                              .replace("{kit}", kit.getDisplayName())
                                              .replace("{rounds}", String.valueOf(roundsToWin)));
        
        TextComponent acceptButton = new TextComponent("§a[ACCEPT]");
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mangoduelcallback accept " + duelUuid.toString()));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("§aClick to accept the duel").create()));
        
        TextComponent declineButton = new TextComponent("§c[DECLINE]");
        declineButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mangoduelcallback decline " + duelUuid.toString()));
        declineButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("§cClick to decline the duel").create()));
        
        // Send to target player
        target.spigot().sendMessage(message);
        target.spigot().sendMessage(kitInfo);
        target.spigot().sendMessage(acceptButton, new TextComponent(" "), declineButton);
        
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
        arena.getCenter().getWorld().setGameRuleValue("doImmediateRespawn", "true");
        
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
        
        // Set game mode to adventure during countdown
        player1.setGameMode(GameMode.ADVENTURE);
        player2.setGameMode(GameMode.ADVENTURE);
        
        // Allow movement but prevent attacking
        player1.setWalkSpeed(0.2f);
        player2.setWalkSpeed(0.2f);
        
        BukkitTask countdownTask = new BukkitRunnable() {
            int countdown = 5;
            
            @Override
            public void run() {
                if (countdown > 0) {
                    // Display countdown
                    if (player1.isOnline()) {
                        player1.sendTitle("§c" + countdown, "§eOrganize your inventory", 0, 20, 0);
                        player1.playSound(player1.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                    if (player2.isOnline()) {
                        player2.sendTitle("§c" + countdown, "§eOrganize your inventory", 0, 20, 0);
                        player2.playSound(player2.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                    countdown--;
                } else {
                    // Save inventories
                    if (player1.isOnline() && player2.isOnline()) {
                        // Save player inventories
                        duel.setPlayer1Inventory(player1.getInventory().getContents().clone());
                        duel.setPlayer1Armor(player1.getInventory().getArmorContents().clone());
                        duel.setPlayer2Inventory(player2.getInventory().getContents().clone());
                        duel.setPlayer2Armor(player2.getInventory().getArmorContents().clone());
                        
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
        }
        
        if (player2.isOnline()) {
            player2.sendTitle("§aGO!", "§eRound " + duel.getCurrentRound(), 0, 20, 10);
            player2.playSound(player2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            player2.setGameMode(GameMode.SURVIVAL);
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
        if (player.getUniqueId().equals(duel.getChallenger().getUniqueId())) {
            winner = duel.getTarget();
            duel.incrementPlayer2Wins();
        } else {
            winner = duel.getChallenger();
            duel.incrementPlayer1Wins();
        }
        
        // Check if duel is over
        if (duel.getPlayer1Wins() >= duel.getRoundsToWin() || duel.getPlayer2Wins() >= duel.getRoundsToWin()) {
            // Duel is over
            endDuel(duel, winner.getUniqueId());
        } else {
            // Prepare for next round
            prepareNextRound(duel);
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
        
        // Regenerate arena
        plugin.getArenaManager().pasteSchematic(arena);
        
        // Wait 1 second after regeneration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!activeDuels.containsKey(duel.getId())) return; // Duel was ended
            
            // Teleport players back to spawn points
            if (player1.isOnline()) {
                player1.teleport(arena.getSpawn1());
                player1.setHealth(20.0);
                player1.setFoodLevel(20);
                player1.setSaturation(20.0f);
                
                // Restore saved inventory
                player1.getInventory().setContents(duel.getPlayer1Inventory());
                player1.getInventory().setArmorContents(duel.getPlayer1Armor());
                player1.updateInventory();
            }
            
            if (player2.isOnline()) {
                player2.teleport(arena.getSpawn2());
                player2.setHealth(20.0);
                player2.setFoodLevel(20);
                player2.setSaturation(20.0f);
                
                // Restore saved inventory
                player2.getInventory().setContents(duel.getPlayer2Inventory());
                player2.getInventory().setArmorContents(duel.getPlayer2Armor());
                player2.updateInventory();
            }
            
            // Start countdown for next round
            startNextRoundCountdown(duel);
        }, 20L); // 1 second delay
    }
    
    /**
     * Start countdown for the next round
     */
    private void startNextRoundCountdown(Duel duel) {
        Player player1 = duel.getChallenger();
        Player player2 = duel.getTarget();
        
        duel.setState(Duel.DuelState.COUNTDOWN);
        
        // Set game mode to adventure during countdown
        if (player1.isOnline()) {
            player1.setGameMode(GameMode.ADVENTURE);
            player1.setWalkSpeed(0.2f);
        }
        
        if (player2.isOnline()) {
            player2.setGameMode(GameMode.ADVENTURE);
            player2.setWalkSpeed(0.2f);
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
            }
            
            // Remove duel
            activeDuels.remove(duel.getId());
            playerDuels.remove(player1.getUniqueId());
            playerDuels.remove(player2.getUniqueId());
            
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