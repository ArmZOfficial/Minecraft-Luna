package com.armzofficial.fantasycore.audit;

/** แถว audit ของงานแอดมิน — เขียนใน transaction เดียวกับงานที่ทำ */
public record AuditEntry(String actorUuid, String actorName, String action, String target,
                         String detail, String reason) {
}
