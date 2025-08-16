package me.moiz.mangoparty.models;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Represents a party of players in the MangoParty plugin.
 * A party consists of a leader and members, with functionality for invites,
 * match participation, and public/private status.
 * 
 * This class is thread-safe and optimized for concurrent access patterns
 * commonly found in multiplayer environments.
 */
public class Party {
    private volatile UUID leader;         // The UUID of the party leader (volatile for thread safety)
    private final Set<UUID> members;      // Thread-safe set of UUIDs of all party members
    private final Map<UUID, Long> invites; // Thread-safe map of UUID -> expiration time for invites
    private volatile boolean inMatch;     // Whether the party is currently in a match
    private volatile boolean isPublic;    // Whether the party is public (can be joined without invite)
    private final long creationTime;      // When this party was created (for cleanup/metrics)
    
    /**
     * Creates a new party with the specified leader.
     *
     * @param leader The UUID of the player who will be the party leader
     * @throws IllegalArgumentException if leader is null
     */
    public Party(UUID leader) {
        if (leader == null) {
            throw new IllegalArgumentException("Party leader cannot be null");
        }
        this.leader = leader;
        this.members = new CopyOnWriteArraySet<>(); // Thread-safe set implementation
        this.invites = new ConcurrentHashMap<>();  // Thread-safe map implementation
        this.members.add(this.leader);
        this.inMatch = false;
        this.isPublic = false;
        this.creationTime = System.currentTimeMillis();
    }
    
    /**
     * Creates a temporary party for a single player.
     * Useful for queue matches or other temporary groupings.
     *
     * @param leader The UUID of the player who will be the sole member
     * @return A new temporary party
     */
    public static Party createTemporaryParty(UUID leader) {
        if (leader == null) {
            throw new IllegalArgumentException("Party leader cannot be null");
        }
        return new Party(leader);
    }
    
    /**
     * Gets the UUID of the party leader.
     *
     * @return The UUID of the party leader
     */
    public UUID getLeader() {
        return leader;
    }
    
    /**
     * Sets a new party leader.
     * If the new leader is not already a member, they will not be added to the party.
     *
     * @param leader The UUID of the new party leader
     */
    public void setLeader(UUID leader) {
        if (leader != null) {
            this.leader = leader;
        }
    }
    
    /**
     * Gets a copy of the set of member UUIDs in this party.
     * Returns an unmodifiable set to prevent external modification.
     *
     * @return An unmodifiable set containing the UUIDs of all party members
     */
    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }
    
    /**
     * Adds a player to the party and removes any pending invite for them.
     *
     * @param member The UUID of the player to add to the party
     */
    public void addMember(UUID member) {
        if (member != null) {
            members.add(member);
            invites.remove(member);
        }
    }
    
    /**
     * Removes a player from the party.
     * If the player is the leader, the leader is not changed.
     *
     * @param member The UUID of the player to remove from the party
     */
    public void removeMember(UUID member) {
        if (member != null) {
            members.remove(member);
        }
    }
    
    /**
     * Checks if a player is a member of this party.
     *
     * @param player The UUID of the player to check
     * @return true if the player is a member, false otherwise
     */
    public boolean isMember(UUID player) {
        return player != null && members.contains(player);
    }
    
    /**
     * Checks if a player is the leader of this party.
     *
     * @param player The UUID of the player to check
     * @return true if the player is the leader, false otherwise
     */
    public boolean isLeader(UUID player) {
        return player != null && leader != null && leader.equals(player);
    }
    
    /**
     * Adds an invite for a player to join the party.
     *
     * @param player The UUID of the player to invite
     * @param expirationTime The time (in milliseconds) when the invite expires
     */
    public void addInvite(UUID player, long expirationTime) {
        if (player != null) {
            invites.put(player, expirationTime);
        }
    }
    
    /**
     * Removes an invite for a player.
     *
     * @param player The UUID of the player whose invite should be removed
     */
    public void removeInvite(UUID player) {
        if (player != null) {
            invites.remove(player);
        }
    }
    
    /**
     * Checks if a player has a valid (non-expired) invite to this party.
     * If the invite has expired, it is automatically removed.
     *
     * @param player The UUID of the player to check
     * @return true if the player has a valid invite, false otherwise
     */
    public boolean hasInvite(UUID player) {
        if (player == null) return false;
        
        Long expiration = invites.get(player);
        if (expiration == null) return false;
        
        if (System.currentTimeMillis() > expiration) {
            invites.remove(player);
            return false;
        }
        return true;
    }
    
    /**
     * Removes all expired invites from the party.
     * This should be called periodically to clean up old invites.
     * 
     * @return The number of expired invites that were removed
     */
    public int cleanExpiredInvites() {
        long currentTime = System.currentTimeMillis();
        int initialSize = invites.size();
        invites.entrySet().removeIf(entry -> entry.getValue() < currentTime);
        return initialSize - invites.size();
    }
    
    /**
     * Gets the number of members in the party.
     *
     * @return The number of members in the party
     */
    public int getSize() {
        return members.size();
    }
    
    /**
     * Gets a list of all online members in the party.
     * This checks if each member is currently online on the server.
     * Optimized using Java streams for better performance.
     *
     * @return A list of online Player objects who are party members
     */
    public List<Player> getOnlineMembers() {
        return members.stream()
                .filter(Objects::nonNull)
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline())
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the creation time of this party.
     *
     * @return The system time in milliseconds when this party was created
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Checks if this party has been inactive for longer than the specified duration.
     *
     * @param maxAgeMillis The maximum age in milliseconds
     * @return true if the party is older than the specified age, false otherwise
     */
    public boolean isOlderThan(long maxAgeMillis) {
        return System.currentTimeMillis() - creationTime > maxAgeMillis;
    }
    
    /**
     * Checks if the party is currently in a match.
     *
     * @return true if the party is in a match, false otherwise
     */
    public boolean isInMatch() {
        return inMatch;
    }
    
    /**
     * Sets whether the party is currently in a match.
     *
     * @param inMatch true if the party is entering a match, false if leaving a match
     */
    public void setInMatch(boolean inMatch) {
        this.inMatch = inMatch;
    }
    
    /**
     * Disbands the party by clearing all members and invites.
     * After calling this method, the party will be empty except for the leader.
     */
    public void disbandParty() {
        members.clear();
        invites.clear();
        // Optionally add the leader back as the sole member
        if (leader != null) {
            members.add(leader);
        }
    }

    /**
     * Checks if the party is public (can be joined without an invite).
     *
     * @return true if the party is public, false if it's private
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * Sets whether the party is public or private.
     *
     * @param aPublic true to make the party public, false to make it private
     */
    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }
}
