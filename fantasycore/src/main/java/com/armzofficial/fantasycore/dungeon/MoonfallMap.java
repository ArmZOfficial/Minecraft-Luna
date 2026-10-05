package com.armzofficial.fantasycore.dungeon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic, testable block plan. Y denotes feet, floors are Y-1. No server access here. */
public final class MoonfallMap {
    public static final int VERSION = 1;
    public static final String WORLD = "luma_moonfall_training";
    public record Pos(int x, int y, int z) {}
    public record Room(int minX, int maxX, int minZ, int maxZ, int feetY) {
        public boolean inside(double x, double y, double z) {
            return x > minX + 1 && x < maxX && z > minZ + 1 && z < maxZ && Math.abs(y - feetY) < 3;
        }
    }
    public static final Room ENTRY = new Room(8,36,8,32,88);
    public static final Room HALL = new Room(10,34,43,69,78);
    public static final Room BRIDGE = new Room(55,82,49,73,58);
    public static final Room RELIQUARY = new Room(103,129,48,74,38);
    public static final Room BOSS = new Room(93,139,95,141,20);
    public static final List<Room> ENCOUNTERS = List.of(HALL, RELIQUARY, BOSS);
    private final Map<Pos,String> blocks = new LinkedHashMap<>();
    private MoonfallMap() {}
    private static final String FINGERPRINT=calculateFingerprint();
    public static String fingerprint() { return FINGERPRINT; }
    private static String calculateFingerprint() {
        try {
            var digest=java.security.MessageDigest.getInstance("SHA-256");
            plan().entrySet().stream().sorted(java.util.Comparator.comparingInt((Map.Entry<Pos,String> e) -> e.getKey().x())
                    .thenComparingInt(e -> e.getKey().y()).thenComparingInt(e -> e.getKey().z())).forEach(e ->
                    digest.update((e.getKey().x()+","+e.getKey().y()+","+e.getKey().z()+"="+e.getValue()+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public static Map<Pos,String> plan() {
        MoonfallMap m = new MoonfallMap();
        for (Room room : List.of(ENTRY,HALL,BRIDGE,RELIQUARY)) { m.room(room); }
        m.roundBoss();
        m.stairs(20,24,33,42,88,false);
        m.stairs(59,63,35,54,78,true);
        m.stairs(59,63,83,102,58,true);
        m.stairs(114,118,75,92,38,false);
        m.box(114,118,17,19,93,94,"stone_bricks");
        m.box(113,113,20,24,93,94,"stone_bricks"); m.box(119,119,20,24,93,94,"stone_bricks");
        m.box(113,119,27,27,93,94,"stone_bricks");
        // Openings are carved last, without touching the supporting floor.
        m.box(20,24,88,92,32,32,"air"); m.box(20,24,78,82,43,43,"air");
        m.box(34,34,78,82,59,63,"air"); m.box(55,55,58,62,59,63,"air");
        m.box(82,82,58,62,59,63,"air"); m.box(103,103,38,42,59,63,"air");
        m.box(114,118,38,42,74,74,"air"); m.box(114,118,20,24,95,96,"air");
        for (int gate : List.of(0,1)) { gates(gate,true).forEach(m.blocks::put); }
        return Collections.unmodifiableMap(m.blocks);
    }

    public static Map<Pos,String> gates(int stage, boolean closed) {
        Map<Pos,String> result = new LinkedHashMap<>();
        if (stage == 0) {
            for (int y=78;y<=81;y++) { for (int z=59;z<=63;z++) { result.put(new Pos(35,y,z),closed?"minecraft:chiseled_stone_bricks":"minecraft:air"); } }
        } else if (stage == 1) {
            for (int x=114;x<=118;x++) { for (int y=38;y<=41;y++) { result.put(new Pos(x,y,75),closed?"minecraft:chiseled_stone_bricks":"minecraft:air"); } }
        }
        return result;
    }

    private void room(Room r) {
        box(r.minX,r.maxX,r.feetY-4,r.feetY-1,r.minZ,r.maxZ,"stone_bricks");
        box(r.minX,r.maxX,r.feetY,r.feetY+6,r.minZ,r.minZ,"stone_bricks");
        box(r.minX,r.maxX,r.feetY,r.feetY+6,r.maxZ,r.maxZ,"stone_bricks");
        box(r.minX,r.minX,r.feetY,r.feetY+6,r.minZ,r.maxZ,"stone_bricks");
        box(r.maxX,r.maxX,r.feetY,r.feetY+6,r.minZ,r.maxZ,"stone_bricks");
        box(r.minX,r.maxX,r.feetY+7,r.feetY+7,r.minZ,r.maxZ,"stone_bricks");
        for (int x=r.minX+3;x<r.maxX;x+=6) {
            for (int z=r.minZ+3;z<r.maxZ;z+=6) {
                put(x,r.feetY-1,z,"sea_lantern"); put(x,r.feetY+7,z,"sea_lantern");
            }
        }
        // Edge pillars frame the room; central paths/combat floors remain empty.
        for (int x : List.of(r.minX+1,r.maxX-1)) {
            for (int z : List.of(r.minZ+2,r.maxZ-2)) {
                box(x,x,r.feetY,r.feetY+6,z,z,"polished_andesite"); put(x,r.feetY+4,z,"sea_lantern");
            }
        }
        int cx=(r.minX+r.maxX)/2, cz=(r.minZ+r.maxZ)/2;
        for (int dx=-2;dx<=2;dx++) { put(cx+dx,r.feetY-1,cz,"cyan_concrete"); }
        put(cx,r.feetY-1,cz,"gold_block");
    }

    private void roundBoss() {
        for (int x=93;x<=139;x++) {
            for (int z=95;z<=141;z++) {
                double radius=Math.hypot(x-116,z-118);
                if (radius>23.4) { continue; }
                box(x,x,16,19,z,z,radius>20.5?"polished_andesite":"stone_bricks");
                put(x,28,z,"stone_bricks");
                if (radius>22.3) { box(x,x,20,27,z,z,"stone_bricks"); }
                if (radius>18.5 && radius<19.5) { put(x,19,z,"cyan_concrete"); }
                if ((x+z)%7==0 && radius>20.5) { put(x,27,z,"sea_lantern"); }
            }
        }
        for (int x=114;x<=118;x++) { for (int z=116;z<=120;z++) { put(x,19,z,"purpur_block"); } }
        put(116,19,118,"sea_lantern");
    }

    /** across is the five-wide corridor; along descends one full block per row. */
    private void stairs(int acrossMin,int acrossMax,int start,int end,int feet,boolean east) {
        for (int row=start;row<=end;row++) {
            int floor=feet-1-(row-start);
            for (int a=acrossMin;a<=acrossMax;a++) {
                int x=east?row:a,z=east?a:row;
                box(x,x,floor-3,floor-1,z,z,"stone_bricks");
                put(x,floor,z,"stone_brick_stairs[facing="+(east?"west":"north")+",half=bottom,shape=straight,waterlogged=false]");
                put(x,floor+8,z,"stone_bricks");
            }
            for (int edge : List.of(acrossMin-1,acrossMax+1)) {
                int x=east?row:edge,z=east?edge:row;
                box(x,x,floor-3,floor+7,z,z,"stone_bricks");
                if ((row-start)%4==0) { put(x,floor+3,z,"sea_lantern"); }
            }
        }
    }

    private void box(int x1,int x2,int y1,int y2,int z1,int z2,String block) {
        for(int x=x1;x<=x2;x++) { for(int y=y1;y<=y2;y++) { for(int z=z1;z<=z2;z++) { put(x,y,z,block); } } }
    }
    private void put(int x,int y,int z,String block) { blocks.put(new Pos(x,y,z),"minecraft:"+block); }
}
