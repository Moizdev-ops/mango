package me.moiz.mangoparty.commands;

import me.moiz.mangoparty.MangoParty;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class QueueTabCompleter implements TabCompleter {
    private MangoParty plugin;
    
    public QueueTabCompleter(MangoParty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // No tab completion needed for queue commands
        return new ArrayList<>();
    }
}
