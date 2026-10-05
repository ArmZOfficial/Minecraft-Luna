package com.armzofficial.fantasycore.claim;

/**
 * ข้อมูลแปลงตามขอบเขตจริงของ region: กว้าง = maxX - minX + 1 (PROTECTIONSTONES-TIERS §1)
 */
public record ClaimInfo(String regionId, boolean owner, boolean member, int sizeX, int sizeZ,
                        int minY, int maxY, int priority) {

    public boolean trusted() {
        return owner || member;
    }
}
