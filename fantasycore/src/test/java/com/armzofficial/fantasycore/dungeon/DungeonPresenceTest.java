package com.armzofficial.fantasycore.dungeon;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DungeonPresenceTest {
    UUID a=UUID.randomUUID(),b=UUID.randomUUID();
    @Test void fixedDeadlineDuplicateQuitAndStrictBoundary() {
        var p=new DungeonPresence(List.of(a,b)); p.disconnected(a,1000); p.disconnected(a,30000);
        assertTrue(p.waiting()); assertEquals(60,p.secondsLeft(1000)); assertEquals(1,p.secondsLeft(60999));
        assertTrue(p.canReconnect(a,60999)); assertFalse(p.expired(60999));
        assertFalse(p.canReconnect(a,61000)); assertTrue(p.expired(61000));
        assertFalse(p.arrived(a,61000)); assertTrue(p.waiting());
    }
    @Test void everyoneMustReturnAndFirstMissingDeadlineWins() {
        var p=new DungeonPresence(List.of(a,b)); p.disconnected(a,0); p.disconnected(b,20000);
        assertEquals(40,p.secondsLeft(20000)); assertFalse(p.arrived(UUID.randomUUID(),30000));
        assertTrue(p.arrived(a,59999)); assertTrue(p.waiting()); assertFalse(p.expired(60000));
        assertEquals(20,p.secondsLeft(60000)); assertTrue(p.arrived(b,79999)); assertFalse(p.waiting());
        p.disconnected(a,90000); assertEquals(60,p.secondsLeft(90000));
    }
    @Test void outsidersAndInvalidMembershipDoNotChangeAValidRun() {
        var p=new DungeonPresence(List.of(a)); p.disconnected(b,0); assertFalse(p.waiting());
        assertThrows(IllegalArgumentException.class,() -> new DungeonPresence(List.of()));
        assertThrows(IllegalArgumentException.class,() -> new DungeonPresence(List.of(a,a)));
        assertThrows(IllegalArgumentException.class,() -> new DungeonPresence(java.util.stream.IntStream.range(0,5).mapToObj(i -> UUID.randomUUID()).toList()));
    }
}
