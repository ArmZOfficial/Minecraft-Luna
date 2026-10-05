package com.armzofficial.fantasycore.economy;

import java.util.Objects;
import java.util.UUID;

/**
 * ข้อมูลประกอบทุกธุรกรรม
 *
 * @param opId   operation ID ที่ไม่ซ้ำ; ส่ง ID เดิมซ้ำจะได้ DUPLICATE และไม่ทำซ้ำ
 * @param kind   ชนิดงาน เช่น bank.deposit, death.loss, admin.adjust, vault.withdraw
 * @param actor  ผู้สั่ง (UUID ผู้เล่น, "console", "vault", "system")
 * @param reason เหตุผลที่อ่านได้ เก็บใน ledger
 * @param ref    ค่าอ้างอิงเพิ่มเติม (nullable)
 */
public record OpMeta(String opId, String kind, String actor, String reason, String ref) {

    public OpMeta {
        Objects.requireNonNull(opId, "opId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(reason, "reason");
        if (opId.isBlank() || opId.length() > 128) {
            throw new IllegalArgumentException("opId ต้องยาว 1–128 ตัวอักษร");
        }
    }

    public static OpMeta of(String kind, String actor, String reason) {
        return new OpMeta(newOpId(), kind, actor, reason, null);
    }

    public static String newOpId() {
        return UUID.randomUUID().toString();
    }
}
