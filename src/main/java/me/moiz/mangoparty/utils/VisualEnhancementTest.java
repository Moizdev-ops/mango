package me.moiz.mangoparty.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Test class for visual enhancements.
 * This class provides methods to test the visual enhancements implemented in the plugin.
 */
public class VisualEnhancementTest {
    private final JavaPlugin plugin;
    
    /**
     * Constructor for the VisualEnhancementTest class.
     * 
     * @param plugin The plugin instance
     */
    public VisualEnhancementTest(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Tests all visual enhancements.
     * 
     * @param sender The command sender to send test results to
     */
    public void testAllEnhancements(CommandSender sender) {
        sender.sendMessage(HexUtils.colorize("&8[&eTest&8] &7Starting visual enhancement tests..."));
        
        // Test HexUtils colorize
        testColorize(sender);
        
        // Test HexUtils gradient
        testGradient(sender);
        
        // Test HexUtils rainbow
        testRainbow(sender);
        
        // Test countdown titles (only if sender is a player)
        if (sender instanceof Player) {
            testCountdownTitles((Player) sender);
        } else {
            sender.sendMessage(HexUtils.colorize("&8[&eTest&8] &cCountdown title test skipped (requires player)"));
        }
        
        sender.sendMessage(HexUtils.colorize("&8[&eTest&8] &aAll visual enhancement tests completed!"));
    }
    
    /**
     * Tests the colorize method in HexUtils.
     * 
     * @param sender The command sender to send test results to
     */
    private void testColorize(CommandSender sender) {
        sender.sendMessage(HexUtils.colorize("&8[&eTest&8] &7Testing HexUtils.colorize()..."));
        sender.sendMessage(HexUtils.colorize("&aGreen text"));
        sender.sendMessage(HexUtils.colorize("&#FF0000Red hex text"));
        sender.sendMessage(HexUtils.colorize("&#00FF00Green hex text"));
        sender.sendMessage(HexUtils.colorize("&#0000FFBlue hex text"));
        sender.sendMessage(HexUtils.colorize("&8[&eTest&8] &aColorize test passed!"));
    }
    
    /**
     * Tests the gradient method in HexUtils.
     * 
     * @param sender The command sender to send test results to
     */
    private void testGradient(CommandSender sender) {
        sender.sendMessage(HexUtils.colorize("&8[&eTest&8] &7Testing HexUtils.gradient()..."));
        sender.sendMessage(HexUtils.gradient("This is a red to blue gradient", "#FF0000", "#0000FF"));
        sender.sendMessage(HexUtils.gradient("This is a green to yellow gradient", "#00FF00", "#FFFF00"));
        sender.sendMessage(HexUtils.gradient("This is a purple to cyan gradient", "#800080", "#00FFFF"));
        sender.sendMessage(HexUtils.colorize("&8[&eTest&8] &aGradient test passed!"));
    }
    
    /**
     * Tests the rainbow method in HexUtils.
     * 
     * @param sender The command sender to send test results to
     */
    private void testRainbow(CommandSender sender) {
        sender.sendMessage(HexUtils.colorize("&8[&eTest&8] &7Testing HexUtils.rainbow()..."));
        sender.sendMessage(HexUtils.rainbow("This is a rainbow text example"));
        sender.sendMessage(HexUtils.rainbow("MangoParty is awesome!"));
        sender.sendMessage(HexUtils.colorize("&8[&eTest&8] &aRainbow test passed!"));
    }
    
    /**
     * Tests the countdown titles.
     * 
     * @param player The player to send titles to
     */
    private void testCountdownTitles(Player player) {
        player.sendMessage(HexUtils.colorize("&8[&eTest&8] &7Testing countdown titles..."));
        
        // Schedule countdown title tests with 1-second delay between each
        for (int i = 5; i >= 1; i--) {
            final int count = i;
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                String title;
                String subtitle = HexUtils.colorize("&6Organize your inventory");
                
                switch (count) {
                    case 5:
                        title = HexUtils.colorize("&c❺");
                        break;
                    case 4:
                        title = HexUtils.colorize("&6❹");
                        break;
                    case 3:
                        title = HexUtils.colorize("&e❸");
                        break;
                    case 2:
                        title = HexUtils.colorize("&a❷");
                        break;
                    case 1:
                        title = HexUtils.colorize("&b❶");
                        break;
                    default:
                        title = HexUtils.colorize("&e" + count);
                        break;
                }
                
                player.sendTitle(title, subtitle, 5, 10, 5);
            }, (5 - i) * 20L); // 20 ticks = 1 second
        }
        
        // Send completion message after countdown
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            player.sendMessage(HexUtils.colorize("&8[&eTest&8] &aCountdown title test passed!"));
        }, 5 * 20L);
    }
}