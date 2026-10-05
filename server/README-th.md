# เซิร์ฟ staging ของ Luma — วิธีตั้ง และ checklist ทดสอบ PoC

เซิร์ฟนี้ใช้ทดสอบ **ลำดับพัฒนาข้อ 1–3** ใน [SERVER-SYSTEMS-PLAN §9](../output/lobby-concept/SERVER-SYSTEMS-PLAN-th.md)
และ **CASUAL-SURVIVAL §12 ข้อ 1–2** (PS tier I/V + บ้าน + RTP) ก่อนเปลี่ยนสถานะ manifest จาก candidate เป็น locked
FantasyCore v0.2 เพิ่มกล่องจดหมาย, รับของรายวัน และการผูก NPC ของ Citizens (หมวด B6, I, J ด้านล่าง)
staging เปิด whitelist ไว้เสมอ และไม่ใช่เซิร์ฟเปิดให้ผู้เล่นทั่วไป

## สิ่งที่ต้องมี

- Windows 10/11, RAM ว่างอย่างน้อย 6 GB
- **Java 25** — Eclipse Temurin 25: https://adoptium.net/temurin/releases/?version=25 (ติดตั้งแล้ว `java -version` ต้องขึ้น 25)
- Minecraft Java client 26.2 และ 1.16.5 สำหรับทดสอบ

เวอร์ชันทั้งหมดอยู่ใน [manifest/compatibility-manifest.json](manifest/compatibility-manifest.json) — ตรวจกับแหล่งทางการเมื่อ 5 ต.ค. 2026

## ตั้งครั้งแรก (ดับเบิลคลิกหรือรันใน PowerShell จากโฟลเดอร์ `server`)

1. `build-plugin.cmd` — build FantasyCore (ครั้งแรก Gradle ดาวน์โหลดตัวเองและ dependency ราว 2–5 นาที)
2. `setup-staging.cmd` — ดาวน์โหลด Paper 26.2 #129 + ปลั๊กอินตาม manifest, ตรวจ hash, วางไฟล์ตั้งต้นใน `server\runtime\`
3. อ่าน Minecraft EULA https://aka.ms/MinecraftEULA แล้วรัน `setup-staging.cmd -AcceptEula`
4. `start-staging.cmd` (หรือ `start-staging.cmd -MemoryGB 6`) — รอจนขึ้น `Done`
5. ใน console: `whitelist add <ชื่อคุณ>` และ `op <ชื่อคุณ>` (ใช้ OP เฉพาะ staging)
6. วางคำสั่งจาก [setup/luckperms-setup.txt](setup/luckperms-setup.txt) แล้ว [setup/worldguard-setup.txt](setup/worldguard-setup.txt) ลงใน console
7. `/fa doctor` ในเกม — ทุกบรรทัดควรเป็น **[พร้อม]** ยกเว้น Citizens (ไม่บังคับ)
8. commit `server/manifest/plugins.lock.json` ที่สคริปต์สร้าง เพื่อล็อก hash ของ JAR ชุดนี้

รอบถัดไปรัน `setup-staging.cmd` ซ้ำได้ — ไฟล์ที่ hash ตรงจะไม่ดาวน์โหลดใหม่ ไฟล์ตั้งต้นที่แก้แล้วจะไม่ถูกเขียนทับ
และถ้า hash ของ JAR ไม่ตรงกับ `plugins.lock.json` สคริปต์จะหยุดให้ตรวจสอบ

## โครงสร้าง

```
server/
  setup-staging.cmd / start-staging.cmd / build-plugin.cmd   ← ตัวเรียก PowerShell (ไม่ต้องแก้ ExecutionPolicy)
  scripts/*.ps1                                              ← สคริปต์จริง
  manifest/compatibility-manifest.json                       ← backend/ปลั๊กอินที่ล็อก + หลักฐาน
  manifest/plugins.lock.json                                 ← hash จริง (สร้างตอน setup)
  templates/                                                 ← server.properties, ProtectionStones 5 ระดับ
  setup/                                                     ← คำสั่ง LuckPerms/WorldGuard
  runtime/                                                   ← ตัวเซิร์ฟจริง (ไม่อยู่ใน git)
```

## ความเสี่ยงที่รู้ก่อนทดสอบ

- **ProtectionStones 2.10.6** ประกาศบน Modrinth ถึง 1.21.10 เท่านั้น — ถ้า error ตอนเปิดหรือวางหินไม่ได้บน 26.2 ให้จดข้อความ error แล้วเลือก: build tag 2.10.7 จาก source, ประเมิน fork อื่น หรือรอรุ่นใหม่ (ห้ามลด backend เพื่อแก้)
- ระบบบ้านตั้งค่าให้ต้องอยู่ในแปลง PS ถ้า PS ใช้ไม่ได้ ทดสอบบ้านชั่วคราวด้วย `homes.require-claim: false` ใน `plugins/FantasyCore/config.yml` แล้วคืนค่าเป็น true
- ห้ามติดตั้ง economy plugin อื่น (เช่น EssentialsX Economy) — `/fa doctor` จะขึ้น **[ต้องแก้ไข]** ถ้า Vault ใช้ provider อื่น
- ห้ามใช้ `/reload` — รีสตาร์ตทุกครั้งที่เปลี่ยน JAR หรือ config

## Checklist ทดสอบในเกม (PoC)

กรอก ✅/❌ พร้อมหมายเหตุ ทดสอบด้วย client **26.2** ก่อน แล้วซ้ำแถวที่มี ★ ด้วย client **1.16.5** (ไม่ใช้ resource pack)

### A. เปิดเซิร์ฟและเชื่อมต่อ
- [ ] A1 เซิร์ฟเปิดถึง `Done` ไม่มี error สีแดงจาก FantasyCore, WorldGuard, ProtectionStones, VaultUnlocked
- [ ] A2 log มี `ฐานข้อมูลพร้อม (schema v1)`, `ระบบที่ดิน: WorldGuard`, `ลงทะเบียน Vault economy provider`
- [ ] A3 โลก `luma_housing` และ `luma_resource` ถูกสร้าง (`/fa doctor` แสดง border 10,000 และ 6,000)
- [ ] A4 ★ client 26.2 และ 1.16.5 เข้าเซิร์ฟได้ทั้งคู่

### B. เมนูและ NPC
- [ ] B1 ★ `/menu` เปิด 54 ช่อง ข้อความไทยอ่านได้ ปุ่มที่ยังไม่มีระบบแสดง "ยังไม่เชื่อม" และกดแล้วบอกว่ายังไม่เชื่อม
- [ ] B2 ★ shift-click, ลาก, กดเลข 1–9, double-click, Q/ทิ้ง, F สลับมือ ในเมนู — ไม่มีไอเทมเมนูหลุดเข้าตัว
- [ ] B3 `/fa npc spawn bank.main <#DDA54A>นายธนาคาร` วาง NPC ที่ hub → NPC ไม่เดิน ไม่ตาย คลิกขวาแล้วเปิดธนาคาร ไม่เปิดหน้าต่างเทรด
- [ ] B4 ตี/ยิง NPC, ใช้ป้ายชื่อ, เชือก — NPC ไม่เปลี่ยน; รีสตาร์ตแล้ว NPC ยังอยู่และคลิกได้
- [ ] B5 `/fa npc spawn quest.exchange` วางได้ คลิกแล้วบอก "ยังไม่เชื่อมระบบ"
- [ ] B6 (ถ้าติด Citizens) `/npc create นายธนาคาร` → มองไปที่ NPC → `/fa npc bind bank.main` → คลิกขวาแล้วเปิดธนาคาร; `/npc skin <ชื่อ>` เปลี่ยนสกินแล้วยังใช้ได้; รีสตาร์ตแล้วยังใช้ได้; `/fa npc unbind` แล้วคลิกไม่เปิดอะไร

### C. เงิน ธนาคาร ตาย (ข้อ 3 ของลำดับพัฒนา)
- [ ] C1 `/fa eco give <คุณ> gold 1000 ทดสอบ` → ได้รหัส → `/fa confirm <รหัส>` → `/balance` เห็น 1,000; ใช้รหัสเดิมซ้ำแล้วถูกปฏิเสธ
- [ ] C2 preview แล้วให้ยอดเปลี่ยน (ฝากเงิน) ก่อน confirm → ระบบบอก "ยอดเปลี่ยนระหว่าง preview" และไม่ปรับ
- [ ] C3 ★ ที่ NPC ธนาคาร: ฝาก 100 / ถอน 100 / ฝากทั้งหมด / ถอนทั้งหมด ยอดรวมทอง+ฝากคงที่
- [ ] C4 คลิกปุ่มฝากรัว ๆ 10 ครั้ง — ยอดย้ายตามจำนวนครั้งที่ระบบตอบ ไม่ติดลบ ไม่มีเงินเกิดใหม่
- [ ] C5 เดินห่าง NPC ธนาคาร > 6 บล็อกแล้ว `/bank deposit 100` → ถูกปฏิเสธ; `lp user <คุณ> parent add rank_bank_remote` แล้วทำได้
- [ ] C6 ใน `luma_housing` พกทอง 1,000 แล้วตาย → เสีย 300, เงินฝาก/เงินแดงเท่าเดิม, ของไม่ดรอป; ตายที่ hub ไม่เสียเงิน
- [ ] C7 `/bank history` และ `/fa bank <คุณ>` แสดงทุกรายการพร้อมรหัสอ้างอิง; `/fa audit <คุณ>` เห็นการปรับยอดพร้อมเหตุผล

### D. ไอเทมแม่แบบ
- [ ] D1 `/fa item give <คุณ> starter_runeblade` ได้ดาบชื่อไทย มี #serial; `/fa item inspect` ขึ้น DELIVERED
- [ ] D2 ไอเทมธรรมดาที่เปลี่ยนชื่อใน anvil ให้เหมือนกัน → `/fa item inspect` บอกว่าไม่ใช่ของ Core
- [ ] D3 กระเป๋าเต็มแล้วสั่ง give → ข้อความ "เข้ากล่องจดหมายแทน", ไม่มีของตกพื้น, `/mail` รับได้เมื่อเคลียร์ช่อง และ `/fa item inspect` ขึ้น MAILED

### E. หิน Protect (CASUAL §12 ข้อ 1)
- [ ] E1 `ps give luma_land_1 <คุณ>` และ `luma_land_5` (จาก console/OP) — ชื่อ/lore ไทย
- [ ] E2 วางหิน I ที่ X100/Z100 ใน `luma_housing` → `/land` ขึ้น 11 × 11 และ `/ps info` ขอบ X95..105, Z95..105
- [ ] E3 วางหิน V → 81 × 81; วางหินใน hub/resource ไม่เกิดแปลง
- [ ] E4 ผู้เล่นอื่นทุบ/เปิดหีบในแปลงไม่ได้; `/ps add <เพื่อน>` แล้วเพื่อนทำได้
- [ ] E5 ผู้เล่นทั่วไปใช้ `/ps get` และ `/ps home` ไม่ได้

### F. บ้าน (CASUAL §12 ข้อ 2)
- [ ] F1 ★ ยืนในแปลงตัวเอง `/sethome` สำเร็จ; นอกแปลง/ใน hub ถูกปฏิเสธพร้อมเหตุผลไทย
- [ ] F2 `/sethome home` ซ้ำ → ขอ `confirm` และแสดงพิกัดเก่า → ใหม่
- [ ] F3 ตั้งบ้านที่ 2 เกินโควตา 1 ถูกปฏิเสธ; `lp user <คุณ> parent add housing_workshop` แล้วได้ 3 หลัง
- [ ] F4 ★ `/home` นับ 3 วิ; ขยับ/โดนตีระหว่างนับ → ยกเลิก; ตีม็อบแล้ว `/home` ทันที → รอ 15 วิ
- [ ] F5 เพื่อนตั้งบ้านในแปลงคุณ แล้วคุณ `/ps remove <เพื่อน>` → เพื่อน `/home` ไปไม่ได้
- [ ] F6 วางบล็อกทับจุดบ้าน → `/home` ปฏิเสธว่าไม่ปลอดภัย ไม่วาร์ปเข้ากำแพง

### G. สุ่มวาร์ป
- [ ] G1 ★ `/rtp housing` ลงพื้นปลอดภัย 500–4,000 บล็อกจาก 0,0 ไม่ลงน้ำ/ลาวา; ทำซ้ำ 10 ครั้ง
- [ ] G2 ภายใน 120 วิ `/rtp` ซ้ำ → บอกเวลารอ; รีสตาร์ตเซิร์ฟแล้ว cooldown ยังอยู่
- [ ] G3 ขยับระหว่างนับ → ยกเลิกและไม่เสีย cooldown
- [ ] G4 ไม่ลงในหรือใกล้แปลงที่มีคนเคลม (เว้น 8 บล็อก)
- [ ] G5 วัด MSPT ด้วย `/mspt` (หรือ `/spark tps`) ระหว่างให้ 3 คน `/rtp` พร้อมกัน — จดค่า

### I. รับของรายวัน (CASUAL §4)
- [ ] I1 เข้าเกมแล้วเห็นข้อความ "วันนี้ยังไม่ได้รับของรายวัน"; ★ `/rewards` เปิดปฏิทิน 7 ช่อง ครั้งที่ 1 ขึ้น "รับได้วันนี้"
- [ ] I2 กดรับ → ได้ 100 ทอง; กดซ้ำ/พิมพ์ `/rewards claim` ซ้ำ → "วันนี้รับไปแล้ว" พร้อมเวลาถึง 00:00 เวลาไทย
- [ ] I3 ให้ 2 หน้าต่าง/คำสั่งกดพร้อมกัน → ได้รางวัลครั้งเดียว (`/fa bank` เห็น ledger รายการเดียว)
- [ ] I4 ทดสอบข้ามวัน: เปลี่ยนเวลาเครื่องเซิร์ฟ staging หรือรอหลัง 00:00 → ครั้งที่ 2 ได้ 150 ทอง + ขนมปัง 8; ถ้ากระเป๋าเต็มขนมปังไป `/mail`
- [ ] I5 รีสตาร์ตเซิร์ฟ → สถานะรับแล้วของวันนี้ยังอยู่

### J. กล่องจดหมาย
- [ ] J1 `/fa mail give <คุณ> ทดสอบกล่อง` (ถือของในมือ) → `/mail` เห็นของ คลิกรับได้ครั้งเดียว
- [ ] J2 ★ กระเป๋าเต็ม → กดรับ → "กระเป๋าเต็ม" ของยังอยู่ในกล่อง; `/mail all` รับจนเต็มแล้วบอกจำนวนที่เหลือ
- [ ] J3 ในเมนูกล่อง shift-click/ลาก/กดเลข — หยิบไอคอนออกมาไม่ได้
- [ ] J4 `/fa doctor` แถวกล่องจดหมายเป็น [พร้อม] (ไม่มีรายการค้างตรวจ)

### H. ความทนทาน
- [ ] H1 ปิดเซิร์ฟด้วย `stop` ระหว่างมีคนเปิดเมนูธนาคาร → เปิดใหม่ ยอดตรงกับ ledger
- [ ] H2 ลบ/ย้าย VaultUnlocked ออกชั่วคราว → FantasyCore ยังเปิดได้ (แจ้งเตือนเรื่อง Vault) ระบบเงินในเกมยังใช้ได้
- [ ] H3 อัปจาก v0.1 → v0.2 บนฐานข้อมูลเดิม → log ขึ้น `schema v2` ยอดเงิน/บ้าน/NPC เดิมอยู่ครบ

เมื่อผ่านครบ: เปลี่ยน `status` ใน manifest เป็น locked, commit `plugins.lock.json` และบันทึกผลในเอกสารสถานะ
แถวที่ไม่ผ่านให้จดข้อความ error/ภาพหน้าจอ แล้วแก้ก่อนไปลำดับข้อ 4 ส่วนที่เหลือ (item adapter + recipe + repair) และเควสแลกของ (CASUAL §7)
