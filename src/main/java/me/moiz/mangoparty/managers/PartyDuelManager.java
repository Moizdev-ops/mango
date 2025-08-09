package me.moiz.mangoparty.managers;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.*;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyDuelManager {
    private MangoParty plugin;
    private Map<UUID, PartyDuel> pendingDuels; // Challenged player UUID -> Duel
    
    public PartyDuelManager(MangoParty plugin) {
        this.plugin = plugin;
        this.pendingDuels = new HashMap<>();
    }
    
    public void challengeParty(Player challenger, Player challengedLeader, String kitName) {
        Party challengerParty = plugin.getPartyManager().getParty(challenger);
        Party challengedParty = plugin.getPartyManager().getParty(challengedLeader);
        
        if (challengerParty == null || !challengerParty.isLeader(challenger.getUniqueId())) {
            challenger.sendMessage("§cYou must be a party leader to challenge other parties!");
            return;
        }
        
        if (challengedParty == null || !challengedParty.isLeader(challengedLeader.getUniqueId())) {
            challenger.sendMessage("§cTarget player is not a party leader!");
            return;
        }
        
        if (challengerParty.isInMatch() || challengedParty.isInMatch()) {
            challenger.sendMessage("§cOne of the parties is already in a match!");
            return;
        }
        
        // Check if there's already a pending duel
        if (pendingDuels.containsKey(challengedLeader.getUniqueId())) {
            challenger.sendMessage("§cThat party already has a pending duel request!");
            return;
        }
        
        // Create duel request
        PartyDuel duel = new PartyDuel(challenger, challengedLeader, kitName);
        pendingDuels.put(challengedLeader.getUniqueId(), duel);
        
        // Send messages
        challenger.sendMessage("§aDuel request sent to " + challengedLeader.getName() + "'s party!");
        
        // Create clickable accept/decline buttons
        TextComponent message = new TextComponent("§e" + challenger.getName() + "'s party has challenged you to a duel!");
        
        TextComponent acceptButton = new TextComponent("§a[ACCEPT]");
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept " + challenger.getName()));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("§aClick to accept the duel").create()));
        
        TextComponent declineButton = new TextComponent("§c[DECLINE]");
        declineButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party decline " + challenger.getName()));
        declineButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("§cClick to decline the duel").create()));
        
        // Send to challenged party
        for (Player member : challengedParty.getOnlineMembers()) {
            member.sendMessage(message);
            member.spigot().sendMessage(acceptButton, new TextComponent(" "), declineButton);
        }
        
        // Set expiration timer
        duel.setExpirationTask(new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingDuels.remove(challengedLeader.getUniqueId()) != null) {
                    challenger.sendMessage("§cYour duel request to " + challengedLeader.getName() + "'s party has expired.");
                    challengedLeader.sendMessage("§cThe duel request from " + challenger.getName() + "'s party has expired.");
                }
            }
        }.runTaskLater(plugin, 1200L)); // 60 seconds
    }
    
    public void acceptDuel(Player accepter, String challengerName) {
        PartyDuel duel = pendingDuels.get(accepter.getUniqueId());
        if (duel == null) {
            accepter.sendMessage("§cYou don't have any pending duel requests!");
            return;
        }
        
        Player challenger = duel.getChallenger();
        if (challenger == null || !challenger.isOnline() || !challenger.getName().equalsIgnoreCase(challengerName)) {
            accepter.sendMessage("§cDuel request not found or challenger is offline!");
            pendingDuels.remove(accepter.getUniqueId());
            return;
        }
        
        Party challengerParty = plugin.getPartyManager().getParty(challenger);
        Party challengedParty = plugin.getPartyManager().getParty(accepter);
        
        if (challengerParty == null || challengedParty == null) {
            accepter.sendMessage("§cOne of the parties no longer exists!");
            pendingDuels.remove(accepter.getUniqueId());
            return;
        }
        
        if (challengerParty.isInMatch() || challengedParty.isInMatch()) {
            accepter.sendMessage("§cOne of the parties is already in a match!");
            pendingDuels.remove(accepter.getUniqueId());
            return;
        }
        
        // Get arena and kit
        Arena arena = plugin.getArenaManager().getAvailableArena();
        if (arena == null) {
            accepter.sendMessage("§cNo available arenas for the duel!");
            challenger.sendMessage("§cNo available arenas for the duel!");
            pendingDuels.remove(accepter.getUniqueId());
            return;
        }
        
        Kit kit = plugin.getKitManager().getKit(duel.getKitName());
        if (kit == null) {
            accepter.sendMessage("§cKit not found!");
            challenger.sendMessage("§cKit not found!");
            pendingDuels.remove(accepter.getUniqueId());
            return;
        }
        
        // Reserve arena
        plugin.getArenaManager().reserveArena(arena.getName());
        
        // Create match
        String matchId = "partyduel_" + System.currentTimeMillis();
        Match match = new Match(matchId, challengerParty, arena, kit, "partyvs");
        
        // Assign teams
        match.assignPartyVsPartyTeams(challengerParty, challengedParty);
        
        // Set parties as in match
        challengerParty.setInMatch(true);
        challengedParty.setInMatch(true);
        
        // Start the match
        plugin.getMatchManager().startPartyVsPartyMatch(match, challengerParty, challengedParty);
        
        // Notify players
        for (Player member : challengerParty.getOnlineMembers()) {
            member.sendMessage("§aDuel accepted! Starting match with kit: " + kit.getDisplayName());
        }
        for (Player member : challengedParty.getOnlineMembers()) {
            member.sendMessage("§aDuel accepted! Starting match with kit: " + kit.getDisplayName());
        }
        
        // Remove duel and cancel expiration task
        pendingDuels.remove(accepter.getUniqueId());
        if (duel.getExpirationTask() != null) {
            duel.getExpirationTask().cancel();
        }
    }
    
    public void declineDuel(Player decliner, String challengerName) {
        PartyDuel duel = pendingDuels.get(decliner.getUniqueId());
        if (duel == null) {
            decliner.sendMessage("§cYou don't have any pending duel requests!");
            return;
        }
        
        Player challenger = duel.getChallenger();
        if (challenger == null || !challenger.getName().equalsIgnoreCase(challengerName)) {
            decliner.sendMessage("§cDuel request not found!");
            return;
        }
        
        // Notify players
        if (challenger.isOnline()) {
            challenger.sendMessage("§c" + decliner.getName() + "'s party has declined your duel request.");
        }
        decliner.sendMessage("§cYou have declined the duel request from " + challenger.getName() + "'s party.");
        
        // Remove duel and cancel expiration task
        pendingDuels.remove(decliner.getUniqueId());
        if (duel.getExpirationTask() != null) {
            duel.getExpirationTask().cancel();
        }
    }
    
    public boolean hasPendingDuel(Player player) {
        return pendingDuels.containsKey(player.getUniqueId());
    }
    
    public void cleanup() {
        for (PartyDuel duel : pendingDuels.values()) {
            if (duel.getExpirationTask() != null) {
                duel.getExpirationTask().cancel();
            }
        }
        pendingDuels.clear();
    }
}
