package me.moiz.mangoparty.commands;

import me.moiz.mangoparty.MangoParty;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MangoDuelCallbackCommand implements CommandExecutor, TabCompleter {
    private final MangoParty plugin;
    
    public MangoDuelCallbackCommand(MangoParty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            // This is an internal command, so no usage message needed
            return true;
        }
        
        String action = args[0].toLowerCase();
        String duelIdStr = args[1];
        
        try {
            UUID duelId = UUID.fromString(duelIdStr);
            
            if (action.equals("accept")) {
                plugin.getDuelManager().acceptDuelById(player, duelId);
            } else if (action.equals("decline")) {
                plugin.getDuelManager().declineDuelById(player, duelId);
            }
        } catch (IllegalArgumentException e) {
            // Invalid UUID format, silently ignore
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // This command should not appear in tab completion
        return new ArrayList<>();
    }
}