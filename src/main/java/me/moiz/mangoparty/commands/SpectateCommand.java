package me.moiz.mangoparty.commands;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Match;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpectateCommand implements CommandExecutor {
    private MangoParty plugin;
    
    public SpectateCommand(MangoParty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage("§cUsage: /spectate <player>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§cPlayer not found or not online!");
            return true;
        }
        
        Match targetMatch = plugin.getMatchManager().getPlayerMatch(target);
        if (targetMatch == null) {
            player.sendMessage("§cThat player is not currently in a match!");
            return true;
        }

        // Set player to spectator mode and teleport
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // If player is already spectating, remove them from their current match's spectator list
        Match currentMatch = plugin.getMatchManager().getPlayerMatch(player);
        if (currentMatch != null && currentMatch.isPlayerSpectator(player.getUniqueId())) {
            currentMatch.removeSpectator(player);
        }

        // Add player to the target match's spectator list
        targetMatch.addSpectator(player);

        player.teleport(target.getLocation());
        player.sendMessage("§aNow spectating " + target.getName() + " in match " + targetMatch.getMatchId() + "!");
        
        return true;
    }
}
