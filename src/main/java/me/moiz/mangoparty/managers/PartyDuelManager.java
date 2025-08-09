package me.moiz.mangoparty.managers;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.models.Match;
import me.moiz.mangoparty.models.Party;
import me.moiz.mangoparty.models.PartyDuel;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PartyDuelManager {
    private MangoParty plugin;
    private Map<UUID, PartyDuel> pendingDuels; // Challenged party leader -> duel
    
    public PartyDuelManager(MangoParty plugin) {
        this.plugin = plugin;
        this.pendingDuels = new HashMap<>();
    }
    
    public boolean challengeParty(Player challenger, Player challengedLeader, String kitName) {
        Party challengerParty = plugin.getPartyManager().getParty(challenger);
        Party challengedParty = plugin.getPartyManager().getParty(challengedLeader);
        
        if (challengerParty == null || !challengerParty.isLeader(challenger.getUniqueId())) {
            challenger.sendMessage("§cYou must be a party leader to challenge other parties!");
            return false;
        }
        
        if (challengedParty == null || !challengedParty.isLeader(challengedLeader.getUniqueId())) {
            challenger.sendMessage("§cThat player is not a party leader!");
            return false;
        }
        
        if (challengerParty.equals(challengedParty)) {
            challenger.sendMessage("§cYou cannot challenge your own party!");
            return false;
        }
        
        if (challengerParty.isInMatch() || challengedParty.isInMatch()) {
            challenger.sendMessage("§cOne of the parties is already in a match!");
            return false;
        }
        
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            challenger.sendMessage("§cKit not found!");
            return false;
        }
        
        // Check if there's already a pending duel
        if (pendingDuels.containsKey(challengedLeader.getUniqueId())) {
            challenger.sendMessage("§cThat party already has a pending duel request!");
            return false;
        }
        
        // Create duel request
        PartyDuel duel = new PartyDuel(challenger.getUniqueId(), challengedLeader.getUniqueId(), kitName);
        pendingDuels.put(challengedLeader.getUniqueId(), duel);
        
        // Send challenge message to challenged party leader
        TextComponent message = new TextComponent("§6" + challenger.getName() + "'s party §7has challenged your party to a duel with kit §e" + kit.getDisplayName() + "§7! ");
        TextComponent acceptButton = new TextComponent("§a[ACCEPT]");
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party acceptduel"));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to accept the duel").create()));
        
        TextComponent declineButton = new TextComponent(" §c[DECLINE]");
        declineButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party declineduel"));
        declineButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to decline the duel").create()));
        
        message.addExtra(acceptButton);
        message.addExtra(declineButton);
        challengedLeader.spigot().sendMessage(message);
        
        // Notify challenger
        challenger.sendMessage("§aDuel request sent to " + challengedLeader.getName() + "'s party!");
        
        return true;
    }
    
    public boolean acceptDuel(Player player) {
        PartyDuel duel = pendingDuels.get(player.getUniqueId());
        if (duel == null) {
            player.sendMessage("§cYou don't have any pending duel requests!");
            return false;
        }
        
        if (duel.isExpired()) {
            pendingDuels.remove(player.getUniqueId());
            player.sendMessage("§cThe duel request has expired!");
            return false;
        }
        
        Player challenger = Bukkit.getPlayer(duel.getChallengerLeader());
        if (challenger == null || !challenger.isOnline()) {
            pendingDuels.remove(player.getUniqueId());
            player.sendMessage("§cThe challenger is no longer online!");
            return false;
        }
        
        Party challengerParty = plugin.getPartyManager().getParty(challenger);
        Party challengedParty = plugin.getPartyManager().getParty(player);
        
        if (challengerParty == null || challengedParty == null) {
            pendingDuels.remove(player.getUniqueId());
            player.sendMessage("§cOne of the parties no longer exists!");
            return false;
        }
        
        if (challengerParty.isInMatch() || challengedParty.isInMatch()) {
            pendingDuels.remove(player.getUniqueId());
            player.sendMessage("§cOne of the parties is already in a match!");
            return false;
        }
        
        // Get kit and arena
        Kit kit = plugin.getKitManager().getKit(duel.getKitName());
        Arena arena = plugin.getArenaManager().getAvailableArena();
        
        if (kit == null) {
            pendingDuels.remove(player.getUniqueId());
            player.sendMessage("§cThe selected kit is no longer available!");
            return false;
        }
        
        if (arena == null) {
            pendingDuels.remove(player.getUniqueId());
            player.sendMessage("§cNo available arenas for the duel!");
            return false;
        }
        
        // Start party vs party match
        startPartyVsPartyMatch(challengerParty, challengedParty, arena, kit);
        
        // Remove duel request
        pendingDuels.remove(player.getUniqueId());
        
        return true;
    }
    
    public boolean declineDuel(Player player) {
        PartyDuel duel = pendingDuels.remove(player.getUniqueId());
        if (duel == null) {
            player.sendMessage("§cYou don't have any pending duel requests!");
            return false;
        }
        
        Player challenger = Bukkit.getPlayer(duel.getChallengerLeader());
        if (challenger != null && challenger.isOnline()) {
            challenger.sendMessage("§c" + player.getName() + "'s party declined your duel request.");
        }
        
        player.sendMessage("§cDuel request declined.");
        return true;
    }
    
    private void startPartyVsPartyMatch(Party party1, Party party2, Arena arena, Kit kit) {
        // Create a combined "super party" for the match system
        Party combinedParty = new Party(party1.getLeader());
        
        // Add all members from both parties
        for (UUID member : party1.getMembers()) {
            if (!member.equals(party1.getLeader())) {
                combinedParty.addMember(member);
            }
        }
        for (UUID member : party2.getMembers()) {
            combinedParty.addMember(member);
        }
        
        // Reserve the arena
        plugin.getArenaManager().reserveArena(arena.getName());
        
        // Create match object with special party vs party type
        String matchId = "pvp_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        Match match = new Match(matchId, combinedParty, arena, kit, "partyvs");
        
        // Manually assign teams based on original parties
        int teamNum = 1;
        for (UUID member : party1.getMembers()) {
            match.getPlayerTeams().put(member, teamNum);
        }
        teamNum = 2;
        for (UUID member : party2.getMembers()) {
            match.getPlayerTeams().put(member, teamNum);
        }
        
        // Set both parties as in match
        party1.setInMatch(true);
        party2.setInMatch(true);
        
        // Start the match using existing match manager
        plugin.getMatchManager().startPartyVsPartyMatch(match, party1, party2);
        
        // Notify all players
        for (Player player : party1.getOnlineMembers()) {
            player.sendMessage("§aParty vs Party duel started! Kit: " + kit.getDisplayName());
        }
        for (Player player : party2.getOnlineMembers()) {
            player.sendMessage("§aParty vs Party duel started! Kit: " + kit.getDisplayName());
        }
    }
    
    public void cleanExpiredDuels() {
        Iterator<Map.Entry<UUID, PartyDuel>> iterator = pendingDuels.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PartyDuel> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                
                Player challenged = Bukkit.getPlayer(entry.getKey());
                if (challenged != null && challenged.isOnline()) {
                    challenged.sendMessage("§cA duel request has expired.");
                }
            }
        }
    }
    
    public void cleanup() {
        pendingDuels.clear();
    }
}
