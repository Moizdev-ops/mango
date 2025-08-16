package me.moiz.mangoparty.models;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a kit that can be applied to players in the game.
 * A kit contains inventory contents, armor, offhand item, and an icon for GUI display.
 * Each kit also has associated rules that define gameplay mechanics when using this kit.
 */
public class Kit {
    private final String name;
    private String displayName;
    private ItemStack[] contents;
    private ItemStack[] armor;
    private ItemStack offhand;
    private ItemStack icon;
    private KitRules rules;
    
    /**
     * Creates a new kit with the specified name.
     *
     * @param name The unique identifier for this kit
     */
    public Kit(String name) {
        this.name = name != null ? name : "unknown";
        this.displayName = this.name;
        this.rules = new KitRules();
    }
    
    /**
     * Gets the unique identifier name of this kit.
     *
     * @return The kit's name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the display name of this kit shown to players.
     *
     * @return The kit's display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Sets the display name of this kit shown to players.
     *
     * @param displayName The new display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName != null ? displayName : this.name;
    }
    
    /**
     * Gets the inventory contents of this kit.
     *
     * @return The kit's inventory contents
     */
    public ItemStack[] getContents() {
        return contents;
    }
    
    /**
     * Sets the inventory contents of this kit.
     *
     * @param contents The new inventory contents
     */
    public void setContents(ItemStack[] contents) {
        this.contents = contents;
    }
    
    /**
     * Gets the armor contents of this kit.
     *
     * @return The kit's armor contents
     */
    public ItemStack[] getArmor() {
        return armor;
    }
    
    /**
     * Sets the armor contents of this kit.
     *
     * @param armor The new armor contents
     */
    public void setArmor(ItemStack[] armor) {
        this.armor = armor;
    }
    
    /**
     * Gets the icon representing this kit in GUIs.
     *
     * @return The kit's icon
     */
    public ItemStack getIcon() {
        return icon;
    }
    
    /**
     * Sets the icon representing this kit in GUIs.
     *
     * @param icon The new icon
     */
    public void setIcon(ItemStack icon) {
        this.icon = icon;
    }
    
    /**
     * Gets the rules associated with this kit.
     *
     * @return The kit's rules
     */
    public KitRules getRules() {
        return rules;
    }
    
    /**
     * Sets the rules associated with this kit.
     *
     * @param rules The new rules
     */
    public void setRules(KitRules rules) {
        this.rules = rules != null ? rules : new KitRules();
    }
    
    /**
     * Gets the offhand item of this kit.
     *
     * @return The kit's offhand item
     */
    public ItemStack getOffhand() {
        return offhand;
    }
    
    /**
     * Sets the offhand item of this kit.
     *
     * @param offhand The new offhand item
     */
    public void setOffhand(ItemStack offhand) {
        this.offhand = offhand;
    }
}
