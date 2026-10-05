package com.armzofficial.fantasycore.economy;

/**
 * ผลของธุรกรรม
 *
 * @param amount จำนวนที่ย้ายจริง (เป็นบวกเสมอ; 0 เมื่อไม่ได้ทำ)
 * @param after  ยอดหลังทำ (หรือยอดปัจจุบันเมื่อไม่สำเร็จ)
 */
public record TxResult(Status status, long amount, Balances after, String opId) {

    public enum Status {
        /** commit แล้ว */
        OK,
        /** ไม่มีอะไรต้องทำ เช่น ฝาก "ทั้งหมด" ตอนไม่มีเงิน หรือ death loss = 0 */
        NOTHING,
        /** op ID นี้เคย commit แล้ว ไม่ทำซ้ำ */
        DUPLICATE,
        INSUFFICIENT_FUNDS,
        INVALID_AMOUNT,
        LIMIT_EXCEEDED,
        /** ยอดเปลี่ยนตั้งแต่ตอน preview — ต้อง preview ใหม่ */
        BALANCE_CHANGED
    }

    public boolean ok() {
        return status == Status.OK;
    }
}
