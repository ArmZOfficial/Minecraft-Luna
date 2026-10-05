package com.armzofficial.fantasycore.dungeon;

import java.util.Arrays;
import java.util.Optional;

/** Fixed names only: player-supplied strings never become filesystem/world names. */
public enum InstanceSlot {
    TRAINING("training",MoonfallMap.WORLD,false),
    PARTY1("party1","luma_moonfall_party_1",true),
    PARTY2("party2","luma_moonfall_party_2",true);
    private final String key, world;
    private final boolean party;
    InstanceSlot(String key,String world,boolean party) { this.key=key; this.world=world; this.party=party; }
    public String key() { return key; }
    public String world() { return world; }
    public boolean party() { return party; }
    public static Optional<InstanceSlot> parse(String key) { return Arrays.stream(values()).filter(s -> s.key.equals(key)).findFirst(); }
}
