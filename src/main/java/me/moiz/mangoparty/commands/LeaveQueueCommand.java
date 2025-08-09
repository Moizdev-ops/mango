package me.moiz.mangoparty.commands;

import me.moiz.mangoparty.MangoParty;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveQueueCommand implements CommandExecutor {
    private MangoParty plugin;
    
    public LeaveQueueCommand(MangoParty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getQueueManager().leaveQueue(player);
        
        return true;
    }
}
