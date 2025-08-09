package me.moiz.mangoparty.models;

import java.util.UUID;

public class QueueEntry {
    private UUID playerId;
    private String mode;
    private String kitName;
    private long joinTime;
    
    public QueueEntry(UUID playerId, String mode, String kitName) {
        this.playerId = playerId;
        this.mode = mode;
        this.kitName = kitName;
        this.joinTime = System.currentTimeMillis();
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getMode() {
        return mode;
    }
    
    public String getKitName() {
        return kitName;
    }
    
    public long getJoinTime() {
        return joinTime;
    }
    
    public long getWaitTime() {
        return System.currentTimeMillis() - joinTime;
    }
}
