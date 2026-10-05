package com.armzofficial.fantasycore.dungeon;

import java.util.*;

/** Frozen members and fixed 60s grace. Duplicate quit events never extend an existing deadline. */
public final class DungeonPresence {
    public static final long GRACE=60000;
    private final Set<UUID> members;
    private final Map<UUID,Long> away=new HashMap<>();
    public DungeonPresence(Collection<UUID> members) {
        this.members=Set.copyOf(members);
        if(this.members.isEmpty() || this.members.size()>4 || this.members.size()!=members.size()) { throw new IllegalArgumentException("members"); }
    }
    public void disconnected(UUID player,long now) { if(members.contains(player)) { away.putIfAbsent(player,Math.addExact(now,GRACE)); } }
    public boolean waiting() { return !away.isEmpty(); }
    public boolean expired(long now) { return away.values().stream().anyMatch(until -> now>=until); }
    public boolean canReconnect(UUID player,long now) { return away.containsKey(player) && now<away.get(player); }
    public boolean arrived(UUID player,long now) { if(!canReconnect(player,now)) { return false; } away.remove(player); return true; }
    public long secondsLeft(long now) { return away.values().stream().mapToLong(until -> Math.max(0,(until-now+999)/1000)).min().orElse(0); }
}
