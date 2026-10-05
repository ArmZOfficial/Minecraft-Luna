# เว็บ Luma — Next.js, React, Node.js และ PostgreSQL

ผู้ใช้เลือก stack นี้ในวันที่ 5 ตุลาคม 2026 จึงย้ายจาก native HTML/JS ไปเป็น React component จริง
ไม่ได้ใช้ React แค่ครอบ iframe หรือหน้า HTML เดิม

## โครงสร้างที่สร้าง

- `website/app` — Next.js App Router และ metadata แยกหน้า
- `website/components/portal.jsx` — UI, state, dialog, search, filters, navigation และ animation
- `website/lib/content.js` — ภาพ/คู่มือ/ข้อมูลประกอบการตรวจแบบ
- `website/public/assets` — โลโก้ เมืองและภาพโซนที่บีบอัด WebP
- `website/server` — Node API กับ PostgreSQL driver, migration และ integration tests
- `website/database/001_initial.sql` — schema บัญชี ข่าว สินค้า orders/payment/outbox/audit
- `website/compose.yaml` — PostgreSQL 17 สำหรับ local development ผูก port กับ loopback เท่านั้น

เวอร์ชันที่ติดตั้งจาก npm registry และล็อกด้วย package-lock.json:
Next.js 16.3.8, React 19.3.0, Motion 14.0.0, pg 8.23.1
Node runtime ที่ตรวจในเครื่องคือ 24.19.0; project กำหนด Node 22 ขึ้นไป
อ้างอิง [Next.js installation](https://nextjs.org/docs/app/getting-started/installation)

## Frontend และ backend

Next.js build เป็น static export ที่ prerender แต่ละหน้า React ทำงานต่อใน browser
Node server เสิร์ฟ export และ `/api` บน origin เดียวกัน จึงใช้ PostgreSQL ได้โดยไม่เปิด database ให้ browser
นี่เป็น Next.js frontend + Node backend แยกขอบเขต ไม่ใช่ Next.js SSR หรือ API Route Handler
เลือกแบบนี้เพื่อแชร์ preview หน้าเว็บได้โดยไม่ผูกการแสดงเว็บเข้ากับ database ที่ยังไม่ตั้งค่าบน cloud

Hosted preview มี frontend ส่วน Node API/PostgreSQL ในเครื่องไม่ถูกอัปโหลดขึ้นเป็นบริการออนไลน์โดยอัตโนมัติ
Frontend รุ่นนี้เผยแพร่แบบ owner-private สำเร็จที่ [Luma Fantasy Town](https://luma-fantasy-town.tunarmzofficial.chatgpt.site)
Production ต้อง deploy Node API ที่เข้าถึง PostgreSQL จริง และกำหนด same-origin reverse proxy หรือ API base ที่เหมาะสม
จะเปลี่ยนเป็น Next.js SSR/Route Handlers บน Node host ได้เมื่อเลือก deployment runtime

## Animation

| ส่วน | พฤติกรรม |
|---|---|
| เปลี่ยนหน้า | opacity + translate Y ประมาณ 380ms |
| ส่วนหน้าแรก | reveal เมื่อเลื่อนเข้ามาในจอ ครั้งเดียว ประมาณ 450ms |
| ภาพเมือง | scale 1 → 1.035 → 1 รอบละ 24 วินาที |
| โลโก้ hero | ลอยขึ้นลง 4px รอบละ 6 วินาที |
| คู่มือ/ข่าว | layout animation เมื่อ filter เปลี่ยน และ exit fade |
| Dialog | transition เนื้อหาประมาณ 180ms โดยคง native focus/ESC |
| แท็บบัญชี | เปลี่ยนแผงประมาณ 150ms |

เคารพ OS reduced motion และ checkbox ลดการเคลื่อนไหวในบัญชี
หยุด ambient movement เมื่อผู้เล่นลดการเคลื่อนไหว ไม่ใช้ animation กับยอดเงินเพื่อบอกสถานะธุรกรรม
อ่าน [Motion accessibility](https://motion.dev/docs/react-use-reduced-motion)

## Database

pool สูงสุด 5 connections ต่อ Node process, connection timeout 2.5 วินาที
ใช้ parameterized queries และไม่คืน connection string/error จาก provider ให้ public API
อ้างอิง [node-postgres pooling](https://node-postgres.com/features/pooling)

เชื่อม PostgreSQL จริงใน Docker แล้ว migration 001 ผ่าน และ container healthy
ข้อมูลข่าวเป็น published/draft แยกกัน สินค้า public ต้อง active และ cosmetic_only
หาก database ยังไม่มีเนื้อหา public หน้าเว็บแสดงข้อมูลตัวอย่างพร้อม label
ไม่มีการ seed ผู้เล่น ยอดเงิน หรือ paid order ปลอม

schema มี unique constraints สำหรับ order idempotency, provider event ID และ delivery order-line
constraints เป็นฐานของ workflow ไม่เท่ากับระบบรับเงิน/ส่งของที่ใช้งานครบแล้ว
งาน payment webhook, game UUID verification และ delivery journal ต้องทำตาม [แผน automation](WEB-AND-AUTOMATION-PLAN-th.md)

## API ที่มีจริง

| Route | การทำงาน |
|---|---|
| GET `/api/health` | ตรวจ SELECT 1 และคืนสถานะ database/game/payment/delivery |
| GET `/api/news` | อ่านเฉพาะ published posts จาก PostgreSQL |
| GET `/api/catalog` | อ่านเฉพาะ active cosmetic products จาก PostgreSQL |
| account/orders/mailbox/admin | ปิดด้วย AUTH_REQUIRED ก่อนมี session จริง |
| auth link/payment/bridge endpoints | ปิดด้วย INTEGRATION_UNCONFIGURED |

รายการปิดเป็นขอบเขตเตรียมเชื่อม ไม่ใช่ระบบ auth หรือรับเงินที่สร้างเสร็จ
static web server ไม่เสิร์ฟ `.env.local`, schema หรือไฟล์ source และจำกัด path ให้อยู่ใน export root

## ผลทดสอบ

- Next.js production build ผ่าน มี home และหน้าแยก 10 หมวดพร้อม 404
- Node integration tests ผ่าน 6 รายการ รวมทดสอบกับ PostgreSQL จริง ไม่ skip
- ทดสอบ draft/public visibility และ provider event ID ซ้ำถูก unique constraint ปฏิเสธ
- PostgreSQL test ใช้ transaction ที่ rollback ทั้งหมด ไม่คงข้อมูล QA ในฐานข้อมูล
- UI ตรวจจริงที่ 1280 × 720 และ 390 × 844: navigation, map, search, dialog, mobile menu และตัวเลือกลดการเคลื่อนไหวผ่าน
- ไม่มี console warning/error ใน flow ที่ตรวจ และ export ไม่มีรหัสผ่านฐานข้อมูลในไฟล์ที่ส่งขึ้นเว็บ
- การทดสอบนี้ยังไม่ครอบคลุม Minecraft bridge, payment provider หรือโหลดผู้ใช้จำนวนมาก

ดู [README ของเว็บ](../../website/README-th.md) สำหรับ npm, Docker, migration และโหมด preview
ค่า secret อยู่ใน `.env.local` ที่ Git ignore ไม่รวมใน source archive หรือ hosted preview
