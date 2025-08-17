package me.moiz.mangoparty.commands;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.utils.HexUtils;
import me.moiz.mangoparty.utils.VisualEnhancementTest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command to test visual enhancements.
 */
public class VisualTestCommand implements CommandExecutor {
    private final MangoParty plugin;
    private final VisualEnhancementTest tester;
    
    /**
     * Constructor for the VisualTestCommand class.
     * 
     * @param plugin The plugin instance
     */
    public VisualTestCommand(MangoParty plugin) {
        this.plugin = plugin;
        this.tester = new VisualEnhancementTest(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mangoparty.admin")) {
            sender.sendMessage(HexUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }
        
        tester.testAllEnhancements(sender);
        return true;
    }
}