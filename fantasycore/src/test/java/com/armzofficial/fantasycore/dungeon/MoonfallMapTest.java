package com.armzofficial.fantasycore.dungeon;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MoonfallMapTest {
    final Map<MoonfallMap.Pos,String> blocks=MoonfallMap.plan();
    String block(int x,int y,int z) { return blocks.getOrDefault(new MoonfallMap.Pos(x,y,z),"minecraft:air"); }
    @Test void allBlocksStayInsideLegacyHeightAnd144FootprintAndPlanIsStable() {
        assertTrue(blocks.size()>10000 && blocks.size()<100000); assertEquals(blocks,MoonfallMap.plan());
        for(var pos:blocks.keySet()) { assertTrue(pos.x()>=0 && pos.x()<144 && pos.z()>=0 && pos.z()<144 && pos.y()>=1 && pos.y()<128,"bounds: "+pos); }
        assertThrows(UnsupportedOperationException.class,() -> blocks.clear());
    }
    @Test void spawnPointsAndClearBossFloorHaveHeadroomAndSupport() {
        for(int[] point:List.of(new int[]{22,88,18},new int[]{22,78,46},new int[]{18,78,59},new int[]{26,78,59},new int[]{22,78,63},
                new int[]{106,38,61},new int[]{117,38,56},new int[]{121,38,66},new int[]{116,20,100},new int[]{116,20,118})) { standing(point[0],point[1],point[2]); }
        for(int x=96;x<=136;x++) { for(int z=98;z<=138;z++) { if(Math.hypot(x-116,z-118)<=20) { standing(x,20,z); } } }
        for(int z=18;z<=32;z++) { standing(22,88,z); }
        for(int z=43;z<=61;z++) { standing(22,78,z); }
        for(int x=22;x<=34;x++) { standing(x,78,61); }
        for(int x=55;x<=82;x++) { standing(x,58,61); }
        for(int x=103;x<=116;x++) { standing(x,38,61); }
        for(int z=61;z<=74;z++) { standing(116,38,z); }
        for(int z=93;z<=118;z++) { standing(116,20,z); }
    }
    void standing(int x,int feet,int z) {
        assertNotEquals("minecraft:air",block(x,feet-1,z),"floor "+x+","+z);
        for(int y=feet;y<=feet+2;y++) { assertEquals("minecraft:air",block(x,y,z),"headroom "+x+","+y+","+z); }
    }
    @Test void fourStairRunsDescendOnePerRowAndFaceUpstream() {
        stairs(false,22,33,42,88,"north"); stairs(true,61,35,54,78,"west");
        stairs(true,61,83,102,58,"west"); stairs(false,116,75,92,38,"north");
    }
    void stairs(boolean east,int across,int start,int end,int feet,String facing) {
        var open=new HashMap<>(blocks); MoonfallMap.gates(0,false).forEach(open::put); MoonfallMap.gates(1,false).forEach(open::put);
        for(int width=across-2;width<=across+2;width++) {
          for(int row=start;row<=end;row++) {
            int x=east?row:width,z=east?width:row, floor=feet-1-(row-start);
            assertTrue(block(x,floor,z).contains("stone_brick_stairs[facing="+facing));
            for(int y=floor+1;y<=floor+4;y++) { assertTrue(open.getOrDefault(new MoonfallMap.Pos(x,y,z),"minecraft:air").endsWith("air"),"stair headroom "+x+","+y+","+z); }
            assertNotEquals("minecraft:air",block(x,floor-1,z));
          }
          standing(east?start-1:width,feet,east?width:start-1);
          standing(east?end+1:width,feet-(end-start+1),east?width:end+1);
        }
    }
    @Test void gatesOnlyChangeTheirOwnAirspaceAndDoNotRemoveStairFloor() {
        for(int stage:List.of(0,1)) {
            var closed=MoonfallMap.gates(stage,true); var opened=MoonfallMap.gates(stage,false);
            assertEquals(closed.keySet(),opened.keySet()); assertEquals(20,closed.size());
            for(var pos:closed.keySet()) { assertTrue(blocks.containsKey(pos)); assertTrue(opened.get(pos).endsWith("air")); }
        }
        assertTrue(block(35,77,61).contains("stairs")); assertTrue(block(116,37,75).contains("stairs"));
        assertTrue(MoonfallMap.gates(2,false).isEmpty());
    }
    @Test void shippedRulesAreDisabledAndInvalidTypesValuesAndReversedDepthFailClosed() throws Exception {
        var yaml=new YamlConfiguration(); yaml.loadFromString(Files.readString(Path.of("src/main/resources/dungeons.yml")));
        var rules=DungeonRules.load(yaml); assertFalse(rules.enabled()); assertEquals(1080,rules.timeoutSeconds()); assertEquals(180,rules.bossHealth());
        yaml.set("enabled","true"); assertThrows(IllegalArgumentException.class,() -> DungeonRules.load(yaml)); yaml.set("enabled",true);
        yaml.set("boss-health",Double.NaN); assertThrows(IllegalArgumentException.class,() -> DungeonRules.load(yaml));
        yaml.set("boss-health",20); assertThrows(IllegalArgumentException.class,() -> DungeonRules.load(yaml)); yaml.set("boss-health",180);
        yaml.set("timeout-seconds",18.5); assertThrows(IllegalArgumentException.class,() -> DungeonRules.load(yaml));
        yaml.set("timeout-seconds",3000); assertThrows(IllegalArgumentException.class,() -> DungeonRules.load(yaml));
    }
}
