package me.moiz.mangoparty.models;

/**
 * Represents the gameplay rules associated with a kit.
 * These rules define various mechanics such as health regeneration,
 * block interactions, damage multipliers, and special effects.
 */
public class KitRules {
    private boolean naturalHealthRegen;
    private boolean blockBreak;
    private boolean blockPlace;
    private double damageMultiplier;
    private boolean instantTnt;
    
    /**
     * Creates a new KitRules instance with default values.
     * By default:
     * - Natural health regeneration is enabled
     * - Block breaking is disabled
     * - Block placing is disabled
     * - Damage multiplier is set to 1.0 (normal damage)
     * - Instant TNT explosion is disabled
     */
    public KitRules() {
        this.naturalHealthRegen = true;
        this.blockBreak = false;
        this.blockPlace = false;
        this.damageMultiplier = 1.0;
        this.instantTnt = false;
    }
    
    /**
     * Checks if natural health regeneration is enabled for this kit.
     *
     * @return true if natural health regeneration is enabled, false otherwise
     */
    public boolean isNaturalHealthRegen() {
        return naturalHealthRegen;
    }
    
    /**
     * Sets whether natural health regeneration is enabled for this kit.
     *
     * @param naturalHealthRegen true to enable natural health regeneration, false to disable it
     */
    public void setNaturalHealthRegen(boolean naturalHealthRegen) {
        this.naturalHealthRegen = naturalHealthRegen;
    }
    
    /**
     * Checks if block breaking is allowed for this kit.
     *
     * @return true if block breaking is allowed, false otherwise
     */
    public boolean isBlockBreak() {
        return blockBreak;
    }
    
    /**
     * Sets whether block breaking is allowed for this kit.
     *
     * @param blockBreak true to allow block breaking, false to disallow it
     */
    public void setBlockBreak(boolean blockBreak) {
        this.blockBreak = blockBreak;
    }
    
    /**
     * Checks if block placing is allowed for this kit.
     *
     * @return true if block placing is allowed, false otherwise
     */
    public boolean isBlockPlace() {
        return blockPlace;
    }
    
    /**
     * Sets whether block placing is allowed for this kit.
     *
     * @param blockPlace true to allow block placing, false to disallow it
     */
    public void setBlockPlace(boolean blockPlace) {
        this.blockPlace = blockPlace;
    }
    
    /**
     * Gets the damage multiplier for this kit.
     * A value of 1.0 means normal damage, values greater than 1.0 increase damage,
     * and values less than 1.0 decrease damage.
     *
     * @return the damage multiplier
     */
    public double getDamageMultiplier() {
        return damageMultiplier;
    }
    
    /**
     * Sets the damage multiplier for this kit.
     * A value of 1.0 means normal damage, values greater than 1.0 increase damage,
     * and values less than 1.0 decrease damage.
     *
     * @param damageMultiplier the new damage multiplier (should be positive)
     */
    public void setDamageMultiplier(double damageMultiplier) {
        this.damageMultiplier = Math.max(0.0, damageMultiplier);
    }
    
    /**
     * Checks if TNT should explode instantly for this kit.
     *
     * @return true if TNT explodes instantly, false otherwise
     */
    public boolean isInstantTnt() {
        return instantTnt;
    }
    
    /**
     * Sets whether TNT should explode instantly for this kit.
     *
     * @param instantTnt true to make TNT explode instantly, false for normal TNT behavior
     */
    public void setInstantTnt(boolean instantTnt) {
        this.instantTnt = instantTnt;
    }
}
