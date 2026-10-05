"use client";
import { useEffect, useRef, useState } from "react";

const labels = {
  pending: "รอผลจำลอง",
  paid: "ชำระสำเร็จแบบจำลอง",
  failed: "ชำระไม่สำเร็จ",
  expired: "หมดเวลารายการ",
  cancelled: "ยกเลิกรายการ",
};
const money = (n) =>
  new Intl.NumberFormat("th-TH", { style: "currency", currency: "THB" }).format(
    n / 100,
  );
const errors = {
  ORDER_TERMINAL: "รายการจบแล้ว กรุณาสร้างรายการใหม่",
  ORDER_NOT_FOUND: "ไม่พบรายการของเซสชันนี้",
  DEV_SESSION_EXPIRED: "เซสชันทดสอบหมดอายุ กรุณาเริ่มใหม่",
  DEV_DISABLED: "โหมดทดสอบยังไม่เปิด",
  DEV_DATABASE_UNAVAILABLE: "ฐานข้อมูลทดสอบยังไม่พร้อม",
  TOO_MANY_PENDING_ORDERS: "มีรายการรอผลมากเกินไป กรุณายกเลิกรายการเดิมก่อน",
};
export default function DevCheckout({ product }) {
  const [enabled, setEnabled] = useState(false),
    [order, setOrder] = useState(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const key = useRef(null),
    sessionReady = useRef(false);
  async function request(route, input, signal) {
    const res = await fetch(
      `/api/dev/${route}`,
      input
        ? {
            method: "POST",
            headers: { "Content-Type": "application/json", "X-Luma-Dev": "1" },
            body: JSON.stringify(input),
            signal,
          }
        : { signal },
    );
    const data = await res.json();
    if (!res.ok)
      throw Object.assign(
        new Error(errors[data.error] || "ระบบทดสอบยังไม่พร้อม ลองอีกครั้ง"),
        { code: data.error },
      );
    return data;
  }
  useEffect(() => {
    const controller = new AbortController();
    request("status", null, controller.signal)
      .then(async (s) => {
        const last = localStorage.getItem(`luma-dev-order:${product.id}`);
        if (s.enabled && last) {
          try {
            const found = await request(
              `orders/${last}`,
              null,
              controller.signal,
            );
            setOrder(found);
            sessionReady.current = true;
          } catch (e) {
            if (e.name !== "AbortError")
              localStorage.removeItem(`luma-dev-order:${product.id}`);
          }
        }
        setEnabled(s.enabled);
      })
      .catch(() => {});
    return () => controller.abort();
  }, [product.id]);
  useEffect(() => {
    if (order?.status !== "pending") return;
    const controller = new AbortController();
    const timer = setInterval(
      () =>
        request(`orders/${order.id}`, null, controller.signal)
          .then((next) =>
            setOrder((previous) =>
              previous &&
              previous.status !== "pending" &&
              next.status === "pending"
                ? previous
                : next,
            ),
          )
          .catch((e) => {
            if (e.name !== "AbortError") setError(e.message);
          }),
      2500,
    );
    return () => {
      clearInterval(timer);
      controller.abort();
    };
  }, [order?.id, order?.status]);
  async function start() {
    setBusy(true);
    setError("");
    try {
      if (!sessionReady.current) {
        await request("session", {});
        sessionReady.current = true;
      }
      key.current ||= crypto.randomUUID();
      const result = await request("checkout", {
        productId: product.id,
        idempotencyKey: key.current,
      });
      setOrder(result);
      localStorage.setItem(`luma-dev-order:${product.id}`, result.id);
    } catch (e) {
      if (["DEV_SESSION_EXPIRED", "DEV_SESSION_REQUIRED"].includes(e.code))
        sessionReady.current = false;
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function outcome(value) {
    setBusy(true);
    setError("");
    try {
      setOrder(await request(`orders/${order.id}/outcome`, { outcome: value }));
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  if (!enabled)
    return (
      <>
        <p className="fine">ยังไม่เปิดรับชำระเงินจริงหรือส่งของเข้าเกม</p>
        <button className="button" disabled>
          ยังไม่เปิดรับชำระ
        </button>
      </>
    );
  return (
    <section className="dev-checkout" aria-label="PromptPay โหมดทดสอบ">
      <div className="dev-heading">
        <span className="tag">DEV / MOCK</span>
        <strong>PromptPay Sandbox</strong>
      </div>
      <p>ทดสอบขั้นตอนซื้อ ไม่มีการรับเงินจริง และไม่มีของส่งเข้าเกม</p>
      {!order ? (
        <button className="button" disabled={busy} onClick={start}>
          {busy ? "กำลังสร้างรายการ…" : "ทดลองซื้อด้วย PromptPay"}
        </button>
      ) : (
        <>
          <div className={`dev-payment-display ${order.status}`}>
            <span aria-hidden="true">DEV</span>
            <strong>{labels[order.status]}</strong>
            <b>{money(order.amountMinor)}</b>
            <small>ภาพจำลอง • สแกนชำระไม่ได้</small>
          </div>
          <dl className="dev-order-details">
            <div>
              <dt>สินค้า</dt>
              <dd>{order.product.name}</dd>
            </div>
            <div>
              <dt>เลขรายการ</dt>
              <dd>
                <code>{order.id}</code>
              </dd>
            </div>
            <div>
              <dt>หมดเวลา</dt>
              <dd>{new Date(order.expiresAt).toLocaleString("th-TH")}</dd>
            </div>
          </dl>
          {order.status === "pending" ? (
            <div className="dev-actions">
              <button
                className="button"
                disabled={busy}
                onClick={() => outcome("paid")}
              >
                จำลองชำระสำเร็จ
              </button>
              <button
                className="button secondary"
                disabled={busy}
                onClick={() => outcome("failed")}
              >
                จำลองล้มเหลว
              </button>
              <button
                className="text-button"
                disabled={busy}
                onClick={() => outcome("cancelled")}
              >
                ยกเลิก
              </button>
            </div>
          ) : (
            <button
              className="button secondary"
              onClick={() => {
                key.current = null;
                setOrder(null);
                localStorage.removeItem(`luma-dev-order:${product.id}`);
              }}
            >
              ทดสอบรายการใหม่
            </button>
          )}
          {order.receipt && (
            <div className="dev-receipt" role="status">
              <strong>กล่องจดหมายจำลองได้รับสิทธิ์แล้ว 1 รายการ</strong>
              <p>
                {order.receipt.product.name} • การส่งซ้ำใช้เลขรายการเดิม
                จึงไม่เพิ่มสิทธิ์ซ้ำ
              </p>
              <small>ยังไม่ได้ส่งของเข้าเซิร์ฟ Minecraft</small>
            </div>
          )}
        </>
      )}
      {error && (
        <p className="dev-error" role="alert">
          {error}
        </p>
      )}
    </section>
  );
}
