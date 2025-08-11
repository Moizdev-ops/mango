package me.moiz.mangoparty.commands;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.managers.DuelManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DuelDeclineCommand implements CommandExecutor, TabCompleter {
    private final MangoParty plugin;
    private final DuelManager duelManager;
    
    public DuelDeclineCommand(MangoParty plugin) {
        this.plugin = plugin;
        this.duelManager = plugin.getDuelManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 1) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-duel.decline-usage"));
            return true;
        }
        
        // Get challenger name
        String challengerName = args[0];
        
        // Decline duel
        duelManager.declineDuel(player, challengerName);
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || args.length != 1) {
            return new ArrayList<>();
        }
        
        // Only show online players
        String partialName = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(partialName))
            .collect(Collectors.toList());
    }
}