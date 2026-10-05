package com.armzofficial.fantasycore.dungeon;

import java.util.*;
import java.util.function.LongSupplier;

/** Main-thread lobby roster. Run snapshots are immutable; invitations are not authorization to join a run. */
public final class PartyRoster {
    public static final int MAX=4;
    public record Snapshot(UUID id,UUID leader,List<UUID> members) {
        public Snapshot { members=List.copyOf(members); }
    }
    private record Invite(UUID party,UUID leader,long expires) {}
    public enum Result { OK, NOT_LEADER, ALREADY_JOINED, FULL, LOCKED, NO_INVITE, NO_PARTY, INVALID_TARGET }
    private static final class Party {
        final UUID id=UUID.randomUUID(), leader;
        final LinkedHashSet<UUID> members=new LinkedHashSet<>(); boolean locked;
        Party(UUID leader) { this.leader=leader; members.add(leader); }
        Snapshot snapshot() { return new Snapshot(id,leader,List.copyOf(members)); }
    }
    private final Map<UUID,Party> byMember=new HashMap<>();
    private final Map<UUID,Invite> invites=new HashMap<>();
    private final LongSupplier clock;
    public PartyRoster(LongSupplier clock) { this.clock=clock; }
    public Optional<Snapshot> party(UUID player) { return Optional.ofNullable(byMember.get(player)).map(Party::snapshot); }
    public boolean locked(UUID player) { var party=byMember.get(player); return party!=null && party.locked; }
    public Result invite(UUID actor,UUID target) {
        if(actor==null || target==null || actor.equals(target)) { return Result.INVALID_TARGET; }
        var party=byMember.get(actor);
        if(party!=null && !party.leader.equals(actor)) { return Result.NOT_LEADER; }
        if(party!=null && party.locked) { return Result.LOCKED; }
        if(byMember.containsKey(target)) { return Result.ALREADY_JOINED; }
        if(party==null) { party=new Party(actor); byMember.put(actor,party); }
        if(party.members.size()>=MAX) { return Result.FULL; }
        invites.values().removeIf(i -> i.expires<=clock.getAsLong());
        // Keep the first live invitation; another leader cannot silently replace it.
        if(invites.containsKey(target)) { return Result.ALREADY_JOINED; }
        invites.put(target,new Invite(party.id,actor,clock.getAsLong()+60000)); return Result.OK;
    }
    public Optional<UUID> invitedBy(UUID target) {
        var invite=invites.get(target);
        return invite!=null && invite.expires>clock.getAsLong()?Optional.of(invite.leader):Optional.empty();
    }
    public Result accept(UUID target) {
        if(byMember.containsKey(target)) { return Result.ALREADY_JOINED; }
        var invite=invites.remove(target);
        if(invite==null || invite.expires<=clock.getAsLong()) { return Result.NO_INVITE; }
        var party=byMember.get(invite.leader);
        if(party==null || !party.id.equals(invite.party)) { return Result.NO_INVITE; }
        if(party.locked) { return Result.LOCKED; }
        if(party.members.size()>=MAX) { return Result.FULL; }
        party.members.add(target); byMember.put(target,party); return Result.OK;
    }
    public Result leave(UUID target) {
        var party=byMember.get(target); if(party==null) { return Result.NO_PARTY; }
        if(party.locked) { return Result.LOCKED; }
        if(party.leader.equals(target)) { return disband(target); }
        party.members.remove(target); byMember.remove(target); return Result.OK;
    }
    public Result kick(UUID actor,UUID target) {
        var party=byMember.get(actor); if(party==null) { return Result.NO_PARTY; }
        if(!party.leader.equals(actor)) { return Result.NOT_LEADER; }
        if(party.locked) { return Result.LOCKED; }
        if(actor.equals(target) || !party.members.contains(target)) { return Result.INVALID_TARGET; }
        party.members.remove(target); byMember.remove(target); return Result.OK;
    }
    public Result disband(UUID actor) {
        var party=byMember.get(actor); if(party==null) { return Result.NO_PARTY; }
        if(!party.leader.equals(actor)) { return Result.NOT_LEADER; }
        if(party.locked) { return Result.LOCKED; }
        party.members.forEach(byMember::remove); invites.values().removeIf(i -> i.party.equals(party.id)); return Result.OK;
    }
    public boolean lock(Snapshot snapshot) {
        var party=byMember.get(snapshot.leader);
        if(party==null || party.locked || !party.snapshot().equals(snapshot)) { return false; }
        party.locked=true; invites.values().removeIf(i -> i.party.equals(party.id)); return true;
    }
    public void unlock(UUID partyId) { byMember.values().stream().filter(p -> p.id.equals(partyId)).findFirst().ifPresent(p -> p.locked=false); }
}
