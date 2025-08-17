package me.moiz.mangoparty.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling hex color codes in messages.
 * Provides methods to colorize strings with both hex and legacy color codes.
 */
public class HexUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final boolean SUPPORTS_HEX;
    
    /**
     * Static initialization to check version support only once.
     * Determines if the server supports hex colors based on version or method availability.
     */
    static {
        boolean supported = false;
        try {
            // Check if the server version supports hex colors (1.16+)
            String version = Bukkit.getVersion();
            if (version.matches(".*1\\.(1[6-9]|2[0-9]).*")) {
                supported = true;
            } else {
                // Fallback to checking if ChatColor.of method exists
                try {
                    ChatColor.class.getMethod("of", String.class);
                    supported = true;
                } catch (NoSuchMethodException ex) {
                    supported = false;
                }
            }
        } catch (Exception e) {
            supported = false;
        }
        SUPPORTS_HEX = supported;
    }
    
    /**
     * Converts hex color codes in a string to Minecraft color codes.
     * 
     * @param message The message to colorize
     * @return The colorized message, or null if the input was null
     */
    public static String colorize(String message) {
        if (message == null) return null;
        if (message.isEmpty()) return message;
        
        String result = message;
        
        if (SUPPORTS_HEX) {
            // Process hex colors
            Matcher matcher = HEX_PATTERN.matcher(result);
            StringBuffer buffer = new StringBuffer(result.length() + 4);
            
            while (matcher.find()) {
                String hexCode = matcher.group(1);
                try {
                    String replacement = ChatColor.of("#" + hexCode).toString();
                    matcher.appendReplacement(buffer, replacement);
                } catch (Exception e) {
                    // If there's an error with this hex code, just keep the original text
                    matcher.appendReplacement(buffer, "&#" + hexCode);
                }
            }
            
            matcher.appendTail(buffer);
            result = buffer.toString();
        } else {
            // Fallback to legacy colors for older versions
            result = convertHexToLegacy(result);
        }
        
        // Process legacy color codes
        result = ChatColor.translateAlternateColorCodes('&', result);
        
        return result;
    }
    
    /**
     * Converts hex color codes to legacy color codes for older Minecraft versions.
     * 
     * @param message The message containing hex color codes
     * @return The message with hex colors converted to legacy colors
     */
    private static String convertHexToLegacy(String message) {
        if (message == null || message.isEmpty()) return message;
        
        // Map of common hex colors to their legacy equivalents
        String[][] colorMappings = {
            {"&#FF0000", "&c"}, // Red
            {"&#00FF00", "&a"}, // Green
            {"&#0000FF", "&9"}, // Blue
            {"&#FFFF00", "&e"}, // Yellow
            {"&#FF00FF", "&d"}, // Magenta
            {"&#00FFFF", "&b"}, // Cyan
            {"&#FFFFFF", "&f"}, // White
            {"&#000000", "&0"}, // Black
            {"&#808080", "&8"}, // Gray
            {"&#C0C0C0", "&7"}, // Light Gray
            {"&#800000", "&4"}, // Dark Red
            {"&#008000", "&2"}, // Dark Green
            {"&#000080", "&1"}, // Dark Blue
            {"&#808000", "&6"}, // Gold
            {"&#800080", "&5"}, // Dark Purple
            {"&#008080", "&3"}  // Dark Aqua
        };
        
        String result = message;
        
        // Apply each color mapping
        for (String[] mapping : colorMappings) {
            result = result.replace(mapping[0], mapping[1]);
        }
        
        // Remove any remaining hex patterns
        result = HEX_PATTERN.matcher(result).replaceAll("&f");
        
        return result;
    }
    
    /**
     * Strips all color codes (both hex and legacy) from a message.
     * 
     * @param message The message to strip colors from
     * @return The message without color codes, or null if the input was null
     */
    public static String stripColor(String message) {
        if (message == null) return null;
        if (message.isEmpty()) return message;
        
        // Remove hex colors
        String result = HEX_PATTERN.matcher(message).replaceAll("");
        
        // Remove legacy colors
        result = ChatColor.stripColor(result);
        
        return result;
    }
    
    /**
     * Checks if the server supports hex colors.
     * 
     * @return true if the server supports hex colors, false otherwise
     */
    public static boolean supportsHex() {
        return SUPPORTS_HEX;
    }
    
    /**
     * Creates a gradient between two colors and applies it to a string.
     * Each character in the text will have a color that is part of the gradient.
     * 
     * @param text The text to apply the gradient to
     * @param startHex The starting hex color (format: "#RRGGBB")
     * @param endHex The ending hex color (format: "#RRGGBB")
     * @return The text with gradient colors applied
     */
    public static String gradient(String text, String startHex, String endHex) {
        if (text == null || text.isEmpty()) return text;
        if (!SUPPORTS_HEX) {
            // Fallback to legacy colors if hex is not supported
            return colorize(text);
        }
        
        // Remove any existing color codes
        String stripped = stripColor(text);
        
        // Parse hex colors
        java.awt.Color startColor = java.awt.Color.decode(startHex);
        java.awt.Color endColor = java.awt.Color.decode(endHex);
        
        StringBuilder result = new StringBuilder();
        int length = stripped.length();
        
        for (int i = 0; i < length; i++) {
            // Calculate the color for this position in the gradient
            float ratio = (float) i / (length - 1);
            int red = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
            int green = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
            int blue = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);
            
            // Format the color and append the character
            String hex = String.format("#%02x%02x%02x", red, green, blue);
            result.append(ChatColor.of(hex)).append(stripped.charAt(i));
        }
        
        return result.toString();
    }
    
    /**
     * Creates a rainbow effect on the given text.
     * Each character will have a color from the rainbow spectrum.
     * 
     * @param text The text to apply the rainbow effect to
     * @return The text with rainbow colors applied
     */
    public static String rainbow(String text) {
        if (text == null || text.isEmpty()) return text;
        if (!SUPPORTS_HEX) {
            // Fallback to legacy colors if hex is not supported
            return colorize("&c" + text);
        }
        
        // Remove any existing color codes
        String stripped = stripColor(text);
        
        StringBuilder result = new StringBuilder();
        int length = stripped.length();
        
        // Rainbow colors
        String[] colors = {
            "#FF0000", // Red
            "#FF7F00", // Orange
            "#FFFF00", // Yellow
            "#00FF00", // Green
            "#0000FF", // Blue
            "#4B0082", // Indigo
            "#9400D3"  // Violet
        };
        
        for (int i = 0; i < length; i++) {
            // Calculate which color to use
            int colorIndex = i % colors.length;
            
            // Apply the color and append the character
            result.append(ChatColor.of(colors[colorIndex])).append(stripped.charAt(i));
        }
        
        return result.toString();
    }
}
