# Minecraft Luna — Luma Fantasy Town

พื้นที่ทำงานสำหรับเซิร์ฟ Minecraft แฟนตาซีและ casual roleplay
ชื่อแบรนด์ที่เสนอคือ **Luma / ลูม่า** ส่วน repository ใช้ชื่อ Minecraft-Luna ตามที่เจ้าของกำหนด

- [เริ่มอ่านแผนทั้งหมด](output/lobby-concept/START-HERE-th.md)
- [เว็บและวิธีเปิดดู](website/README-th.md)
- [ผังแมพและแปลนภายใน](output/lobby-concept/INTERIOR-AND-MAP-PLAN-th.md)
- [ใบงานสร้าง 12 โซน](output/lobby-concept/ZONE-BUILD-TICKETS-th.md)
- [ระบบและปลั๊กอิน](output/lobby-concept/SERVER-SYSTEMS-PLAN-th.md)
- [ระบบตามภาพ: สุ่มวาร์ป/รางวัล/สกิน/วาดรูป/ตั้งบ้าน](output/lobby-concept/CASUAL-SURVIVAL-SYSTEMS-th.md)
- [หินกันบ้าน ProtectionStones 5 ระดับ](output/lobby-concept/PROTECTIONSTONES-TIERS-th.md)
- [โมเดลและภาพอ้างอิง](output/lobby-concept/ASSET-GALLERY-th.md)
- [ปลั๊กอิน FantasyCore v0.3](fantasycore/README-th.md) — เงิน/ธนาคาร/บ้าน/RTP/NPC/กล่องจดหมาย/รางวัลรายวัน/เควสแลกของ
- [ใช้และกู้คืนระบบแลกของ](fantasycore/EXCHANGE-th.md) — สูตร, โควตา, snapshot และรายการรอแอดมินตรวจ
- [เซิร์ฟ staging + checklist ทดสอบ](server/README-th.md) — Paper 26.2 + Java 25

## สถานะงาน

มีเว็บต้นแบบที่ทดลองเมนู ค้นหาโซน คู่มือ ข่าว และศูนย์บัญชีได้ พร้อมภาพและเอกสารออกแบบ
เว็บปัจจุบันใช้ Next.js + React + Motion และ Node API เชื่อม PostgreSQL จริงในเครื่อง
มี NPC pilot 4 ตัวที่มี animation และ item pilot 2 ชิ้น export จาก Blockbench MCP

**ฝั่งเซิร์ฟ (5 ต.ค. 2026):** มีปลั๊กอิน FantasyCore v0.3 — economy/ธนาคาร/ledger/ตายเสียทอง/audit, เมนูไทย, NPC สถานี (+ผูก Citizens),
item template, บ้าน, RTP, claim adapter, กล่องจดหมาย, รับของรายวัน และ `/exchange` (3 สูตร vanilla + preview + journal recovery)
— build กับ Paper 26.2 API ผ่าน และ unit tests 42 รายการผ่าน
พร้อมชุด staging (สคริปต์ดาวน์โหลด Paper + ปลั๊กอินที่ล็อกเวอร์ชันและตรวจ hash, หิน Protect 5 ระดับ, LuckPerms/WorldGuard setup)
**ยังไม่ได้รันบนเซิร์ฟ Minecraft จริง** — ขั้นต่อไปคือทำ checklist ใน [server/README-th.md](server/README-th.md)
ยังไม่มีโลกเมืองที่สร้างเสร็จ, แผนที่สด, payment backend หรือการส่งของจริง

เป้าหมาย client คือ Java 1.16.5 ขึ้นไป (backend Paper 26.2 + ViaVersion/ViaBackwards) ต้องผ่าน staging matrix ก่อนยืนยันการรองรับจริง
ไฟล์โลกและ schematic ที่ดาวน์โหลดมาเดิมไม่ได้รวมใน repository

## เปิดเซิร์ฟ staging (Windows)

```powershell
cd server
.\build-plugin.cmd
.\setup-staging.cmd -AcceptEula   # ใส่ -AcceptEula หลังอ่าน https://aka.ms/MinecraftEULA แล้ว
.\start-staging.cmd
```

ต้องมี Java 25 — รายละเอียดและ checklist อยู่ใน [server/README-th.md](server/README-th.md)

## เปิดเว็บในเครื่อง

```powershell
cd website
npm ci
npm run build
npm start
```

เปิด `http://127.0.0.1:4178/` การตั้ง PostgreSQL และ migration อยู่ใน [คู่มือเว็บ](website/README-th.md)

## GitHub

Remote: https://github.com/ArmZOfficial/Minecraft-Luna.git
remote เชื่อมแล้ว และส่งงานชุดแรกขึ้น main สำเร็จ การแก้ไขใหม่ให้ดู commit ล่าสุดใน GitHub
