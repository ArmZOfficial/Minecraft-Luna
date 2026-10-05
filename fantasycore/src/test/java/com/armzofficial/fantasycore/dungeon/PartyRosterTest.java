package com.armzofficial.fantasycore.dungeon;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static com.armzofficial.fantasycore.dungeon.PartyRoster.Result.*;

class PartyRosterTest {
    AtomicLong now=new AtomicLong(1000);
    PartyRoster roster=new PartyRoster(now::get);
    UUID leader=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID();
    void add(UUID member) { assertEquals(OK,roster.invite(leader,member)); assertEquals(OK,roster.accept(member)); }
    @Test void capacityLeaderAndSingleMembership() {
        add(a); add(b); add(UUID.randomUUID());
        assertEquals(4,roster.party(a).orElseThrow().members().size());
        assertEquals(FULL,roster.invite(leader,UUID.randomUUID()));
        assertEquals(NOT_LEADER,roster.invite(a,UUID.randomUUID()));
        assertEquals(ALREADY_JOINED,roster.invite(UUID.randomUUID(),a));
        assertEquals(INVALID_TARGET,roster.kick(leader,leader));
        assertEquals(NOT_LEADER,roster.kick(a,b));
        assertEquals(OK,roster.kick(leader,b)); assertTrue(roster.party(b).isEmpty());
    }
    @Test void invitationsExpireExactlyAndCannotBeReplaced() {
        assertEquals(OK,roster.invite(leader,a));
        assertEquals(ALREADY_JOINED,roster.invite(b,a));
        assertEquals(leader,roster.invitedBy(a).orElseThrow());
        now.set(61000); assertTrue(roster.invitedBy(a).isEmpty()); assertEquals(NO_INVITE,roster.accept(a));
        assertEquals(OK,roster.invite(b,a)); assertEquals(OK,roster.accept(a));
        assertEquals(b,roster.party(a).orElseThrow().leader());
    }
    @Test void frozenRosterLocksAllChangesAndRejectsStaleSnapshot() {
        add(a); var snapshot=roster.party(leader).orElseThrow();
        assertThrows(UnsupportedOperationException.class,() -> snapshot.members().clear());
        add(b); assertFalse(roster.lock(snapshot));
        var current=roster.party(leader).orElseThrow(); assertTrue(roster.lock(current));
        assertFalse(roster.lock(current)); assertEquals(LOCKED,roster.leave(a));
        assertEquals(LOCKED,roster.kick(leader,a)); assertEquals(LOCKED,roster.disband(leader));
        assertEquals(LOCKED,roster.invite(leader,UUID.randomUUID()));
        roster.unlock(current.id()); assertEquals(OK,roster.leave(a));
    }
    @Test void disbandInvalidatesInvitesAndEveryMember() {
        add(a); roster.invite(leader,b);
        assertEquals(OK,roster.leave(leader)); assertTrue(roster.party(a).isEmpty());
        assertEquals(NO_INVITE,roster.accept(b)); assertEquals(NO_PARTY,roster.leave(a));
        assertEquals(INVALID_TARGET,roster.invite(leader,leader));
    }
    @Test void lockingCancelsUnacceptedInvitesWithoutAddingLateMembers() {
        add(a); roster.invite(leader,b); var snapshot=roster.party(leader).orElseThrow();
        assertTrue(roster.lock(snapshot)); assertEquals(NO_INVITE,roster.accept(b));
        assertEquals(List.of(leader,a),roster.party(leader).orElseThrow().members());
    }
}
