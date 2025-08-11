package me.moiz.mangoparty.commands;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.managers.DuelManager;
import me.moiz.mangoparty.managers.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DuelCommand implements CommandExecutor, TabCompleter {
    private final MangoParty plugin;
    private final DuelManager duelManager;
    private final KitManager kitManager;
    
    public DuelCommand(MangoParty plugin) {
        this.plugin = plugin;
        this.duelManager = plugin.getDuelManager();
        this.kitManager = plugin.getKitManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 3) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.usage"));
            return true;
        }
        
        // Get target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-not-found"));
            return true;
        }
        
        // Check if player is trying to duel themselves
        if (target.equals(player)) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.self-duel"));
            return true;
        }
        
        // Get kit
        String kitName = args[1];
        if (kitManager.getKit(kitName) == null) {
            player.sendMessage("§cKit not found! Available kits: §f" + 
                              String.join("§7, §f", kitManager.getKits().keySet()));
            return true;
        }
        
        // Get rounds
        int rounds;
        try {
            rounds = Integer.parseInt(args[2]);
            if (rounds < 1 || rounds > 10) {
                player.sendMessage("§cRounds must be between 1 and 10!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number of rounds!");
            return true;
        }
        
        // Send duel request
        duelManager.challengePlayer(player, target, kitName, rounds);
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Player names
            String partialName = args[0].toLowerCase();
            completions = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partialName))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Kit names
            String partialKit = args[1].toLowerCase();
            completions = kitManager.getKits().keySet().stream()
                .filter(kit -> kit.toLowerCase().startsWith(partialKit))
                .collect(Collectors.toList());
        } else if (args.length == 3) {
            // Rounds (1-10)
            String partialRound = args[2].toLowerCase();
            for (int i = 1; i <= 10; i++) {
                String round = String.valueOf(i);
                if (round.startsWith(partialRound)) {
                    completions.add(round);
                }
            }
        }
        
        return completions;
    }
}