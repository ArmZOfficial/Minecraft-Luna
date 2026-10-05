import Link from "next/link";
export default function NotFound() {
  return (
    <div className="shell">
      <h1>เส้นทางนี้ยังไม่มีในเมือง</h1>
      <p>เลือกเมนูด้านบน หรือกลับไปเริ่มที่ลานคริสตัล</p>
      <div className="actions">
        <Link className="button" href="/">
          กลับหน้าหลัก
        </Link>
      </div>
    </div>
  );
}
