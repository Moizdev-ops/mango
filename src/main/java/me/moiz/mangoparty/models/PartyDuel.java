package me.moiz.mangoparty.models;

import org.bukkit.entity.Player;

import java.util.UUID;

public class PartyDuel {
    private UUID challengerLeader;
    private UUID challengedLeader;
    private String kitName;
    private long expirationTime;
    
    public PartyDuel(UUID challengerLeader, UUID challengedLeader, String kitName) {
        this.challengerLeader = challengerLeader;
        this.challengedLeader = challengedLeader;
        this.kitName = kitName;
        this.expirationTime = System.currentTimeMillis() + (60 * 1000); // 60 seconds
    }
    
    public UUID getChallengerLeader() {
        return challengerLeader;
    }
    
    public UUID getChallengedLeader() {
        return challengedLeader;
    }
    
    public String getKitName() {
        return kitName;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }
    
    public long getTimeLeft() {
        return Math.max(0, expirationTime - System.currentTimeMillis());
    }
}
