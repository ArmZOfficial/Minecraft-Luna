# Luma — Next.js / React / Node.js / PostgreSQL

เว็บต้นแบบแนว Minecraft modern สำหรับ Luma ใช้เขียวเข้ม–ทอง ภาพเมืองเต็มสีและโลโก้ original
มีหน้าแรก ข่าว แผนที่ คู่มือ กิจกรรม ร้านค้า ศูนย์บัญชี อันดับ สถานะ และร่างเงื่อนไข/ความเป็นส่วนตัว
ทดลองค้นหาทั้งเว็บ (`Ctrl+K`), ค้นหา 12 โซน, เลื่อน/ขยายภาพ, กรองคู่มือ/ข่าว, เปิดรายละเอียด และสลับแท็บบัญชีได้
มีธีมกลางวัน/กลางคืนและตัวเลือกลดการเคลื่อนไหว
ใช้ Next.js App Router, React component และ Motion animation พร้อม Node API ที่เชื่อม PostgreSQL
มี production build และ migration ที่ทดสอบกับ PostgreSQL ใน Docker จริง

**สถานะจริง:** Node API อ่านข่าว/สินค้าและตรวจ health ได้ PostgreSQL มี schema บัญชี/orders/payment/outbox/audit
ยังไม่มี authentication, payment-provider integration หรือ Minecraft delivery bridge ที่เปิดใช้งานจริง
ปุ่มรับเงินจริงปิดไว้ มี [PromptPay โหมดจำลอง](PROMPTPAY-th.md) สำหรับ local development พร้อม order/receipt ที่แยกตารางจากเงินจริง
ร้าน draft เพิ่ม 22 ชุดและใช้ไอคอนจากคลังที่ผู้ใช้ให้; อ่าน [คู่มือคลังโมเดลและแปลนวาง](../server/content/library/README-th.md) กับ [บาลานซ์](../server/content/library/BALANCE-th.md)
ไฟล์ภาพจากแพ็กต้อง regenerate ในเครื่องก่อน build ตามคู่มือ เพราะไม่ได้แจกไฟล์ผลิตภัณฑ์ต้นฉบับบน Git
ภาพแผนที่เป็น concept art กับหมุดตัวอย่าง ไม่ใช่ BlueMap สด

แผนเชื่อมระบบจริงอยู่ใน [WEB-AND-AUTOMATION-PLAN-th.md](../output/lobby-concept/WEB-AND-AUTOMATION-PLAN-th.md)
ขั้นตอนรันในเครื่อง (Node 22 ขึ้นไป; Docker Desktop สำหรับฐานข้อมูล):

```powershell
cd website
npm ci
node server/setup-local.mjs
docker compose --env-file .env.local up -d --wait
npm run db:migrate
npm test
npm run build
npm start
```

เปิด `http://127.0.0.1:4178/` ได้ทั้งหน้า React และ Node API บน origin เดียวกัน
`setup-local.mjs` สร้างรหัส database แบบสุ่มใน `.env.local` และไม่ทับไฟล์ที่มีอยู่
ฐานข้อมูล local ที่เตรียมในงานนี้รันชื่อ `luma-portal-database-1` และเก็บข้อมูลใน Docker volume
ใช้ `docker compose --env-file .env.local stop` เมื่อต้องการหยุด โดยไม่ลบข้อมูล

พัฒนา frontend ด้วย `npm run dev` ที่พอร์ต 4179 โดยให้ Node API ที่ 4178 รันอยู่
production build เป็น Next.js static export ส่วน Node backend แยกขอบเขตและใช้ PostgreSQL
Hosted preview แชร์ frontend; backend และ PostgreSQL ในเครื่องไม่ได้กลายเป็นบริการ cloud อัตโนมัติ
แผนและรายละเอียดอยู่ใน [NEXT-POSTGRES-IMPLEMENTATION-th.md](../output/lobby-concept/NEXT-POSTGRES-IMPLEMENTATION-th.md)
ต้นแบบ HTML รุ่นก่อนเก็บไว้ใน `dist/` เพื่ออ้างอิง ส่วน source ปัจจุบันอยู่ใน `app/`, `components/`, `server/`

ชื่อ Luma เป็นข้อเสนอสำหรับงานนี้ ยังไม่ตรวจ trademark/domain availability

ภาพเว็บแปลงเป็น WebP รวมประมาณ 7.4 MB จากต้นฉบับ PNG ประมาณ 50 MB และโหลดภาพส่วนล่างแบบ lazy
เก็บภาพ PNG ต้นฉบับใน `output/lobby-concept` ครบ
หากต้อง regenerate WebP ใช้ `tools/prepare_web_assets.mjs` กับ Node และ package `sharp`
frontend ที่ export ไม่ต้องใช้ sharp ระหว่างรัน ส่วน backend ต้องใช้ Node

Source repository: https://github.com/ArmZOfficial/Minecraft-Luna
