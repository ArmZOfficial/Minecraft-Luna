# เซิร์ฟ staging ของ Luma — วิธีตั้ง และ checklist ทดสอบ PoC

เซิร์ฟนี้ใช้ทดสอบ **ลำดับพัฒนาข้อ 1–3** ใน [SERVER-SYSTEMS-PLAN §9](../output/lobby-concept/SERVER-SYSTEMS-PLAN-th.md)
และ **CASUAL-SURVIVAL §12 ข้อ 1–2** (PS tier I/V + บ้าน + RTP) ก่อนเปลี่ยนสถานะ manifest จาก candidate เป็น locked
FantasyCore v0.2 เพิ่มกล่องจดหมาย, รับของรายวัน และการผูก NPC ของ Citizens (หมวด B6, I, J ด้านล่าง)
รุ่นปัจจุบัน v0.8/schema7 เพิ่ม [Moonfall ปาร์ตี้2–4คน/2ห้อง/reconnect](../fantasycore/PARTY-DUNGEONS-th.md) (checklist R/S); ไม่เปิดรับผู้เล่นอัตโนมัติ
staging เปิด whitelist ไว้เสมอ และไม่ใช่เซิร์ฟเปิดให้ผู้เล่นทั่วไป

คลังโมเดล/ไอคอนที่เพิ่ม: [26 แพ็กและแปลนวางในเมือง](content/library/README-th.md), [ชื่อชุดและบาลานซ์ enchant](content/library/BALANCE-th.md)
ไฟล์ที่แก้เป็นสำเนา local; ยังไม่ติดตั้ง JAR provider หรือรับประกัน client 1.16.5 แสดงทุกโมเดลได้

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
- [ ] A2 log มี `ฐานข้อมูลพร้อม (schema v7)`, `ระบบที่ดิน: WorldGuard`, `ลงทะเบียน Vault economy provider`
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
- [ ] H3 อัปจาก v0.1/v0.2/v0.3/v0.4 → v0.5 บนสำเนาฐานข้อมูลเดิม → log ขึ้น `schema v5` ยอดเงิน/บ้าน/NPC/mail/reward/exchange/repair เดิมอยู่ครบ

### K. เควสแลกของ (v0.3)
- [ ] K1 ★ `/exchange` และ `/menu` → กระดานแลกของ เปิด 3 สูตร; คลิก NPC `quest.exchange` ได้เมนูเดียวกัน
- [ ] K2 ★ WHEAT 32 → preview BREAD 8 → ยืนยัน: ตัด 32 พอดี, `/mail` มี 8, กดซ้ำ/รับซ้ำไม่ได้, ไม่ตกพื้น
- [ ] K3 ★ batch 2 ของ garden_lamp ใช้ TORCH 32 + IRON_INGOT 8 → LANTERN 4; ของหลาย stack รวมได้ เหลือแต่ละ slot ตรง; เกราะ/มือรองไม่ถูกใช้
- [ ] K4 ★ ตั้งชื่อ/เพิ่ม lore/enchants/PDC ให้ของชนิดเดียวกับวัตถุดิบ → ไม่ถูกใช้; ของธรรมดาไม่ครบหนึ่งชนิด → ไม่ตัดอะไรเลย
- [ ] K5 ★ กระเป๋าเต็มและของครบ → แลกได้ รางวัลรอ mail; รับ mail เมื่อเต็มไม่ได้ พอมีช่องจึงรับได้; shift/drag/ปุ่มเลข/double-click หยิบไอคอนไม่ได้
- [ ] K6 ★ ใช้ starter_tool ครบ 4 รอบ → รอบที่ 5 ไม่ได้; เพิ่ม recipe version ยังติดโควตา; วันใหม่ไทยเปิด `/exchange` ใหม่ โควตารีเซ็ต; journal ยังอยู่
- [ ] K7 ปิดเมนู/ออกเกม/เปลี่ยนวัตถุดิบ/ถอน `fantasy.exchange` ระหว่าง PREPARED หรือรอ beginConsume → ยกเลิกก่อนตัด ไม่มี mail และคืนโควตา
- [ ] K8 บน staging disposable ใช้ debugger/harness หยุดตามจุด: หลัง PREPARED, หลัง CONSUMING ก่อนตัด, หลังตัดก่อน saveData, หลัง saveData ก่อน DB commit, หลัง commit ก่อนข้อความ แล้ว terminate process → เปิดใหม่และตรวจ state/playerdata/mail ทุกจุด (เก็บ backup และ log)
- [ ] K9 PREPARED ค้าง → CANCELLED; CONSUMING ค้าง → REVIEW บล็อกการแลกทั้งหมดของ UUID; COMMITTED คงเดิมและ mail เดิมไม่เพิ่ม; CLAIMING ของ mail → ใช้หมวด J/review แยกกัน
- [ ] K10 `/fa exchange complete|cancel <op> เหตุผล` แสดง preview → `/fa confirm`; reason/actor/permission/expiry ผิดไม่ apply; confirm ซ้ำไม่สร้างของเพิ่ม; audit ครบ; cancel ไม่คืนวัตถุดิบเอง
- [ ] K11 อัปฐานข้อมูล v0.2 จริงบนสำเนาด้วย Core v0.5 → schema v5 เงิน/บ้าน/NPC/mail/daily เดิมอยู่ครบ; สูตรผิดปิดเฉพาะสูตรนั้น; config เดิมไม่ถูกเขียนทับ
- [ ] K12 ตั้ง `players.disable-saving: true` บน staging แล้ว restart → แลกและรับ mail ไม่ได้; คืน false แล้วทดสอบ disk write failure และดู `Failed to save player data` — ถ้าผล inventory/DB ไม่ตรงต้องแก้ก่อนเปิด public
- [ ] K13 วัด MSPT/เวลาตอบกลับเมื่อหลายคนแลกและรับ mail พร้อมกัน เพราะ saveData เป็น synchronous I/O; บันทึกค่าจริงก่อนตั้งจำนวนผู้เล่นที่รองรับ

รายละเอียด flow/ข้อจำกัด storage/การกู้คืน: [EXCHANGE-th.md](../fantasycore/EXCHANGE-th.md)

### L. ItemAdapter + ซ่อม (v0.4)
- [ ] L1 ★ วาง/ผูก NPC `repair.main` ที่โซน 07; `/repair` และปุ่มช่างใน `/menu` เปิดในระยะ 6 บล็อก โลกเดียวกัน; นอกระยะไม่มี remote ใช้ไม่ได้
- [ ] L2 ★ ถือ vanilla IRON_SWORD damage=100/max=250, ทอง≥230 → preview 150/250 → 250/250 ราคา 230; ยืนยันซ่อมใน slot เดิม ทองลด 230 เงินฝาก/เงินแดงคงเดิม ไม่มี mail/ของตกพื้น
- [ ] L3 ★ ของเต็ม/ไม่ใช่อุปกรณ์/unbreakable/amount>1/ค่า damage ผิดช่วง → ไม่จองเงิน; เงินไม่พอ/ราคาผิด config → ไม่เปลี่ยน item
- [ ] L4 ★ vanilla มีชื่อจาก anvil + enchant + repair-cost → ซ่อมแล้วค่าที่ไม่ใช่ DAMAGE เหมือนเดิม; lore/model/attributes/custom data แปลกต้องถูกปฏิเสธ (เทียบ ItemStack serialized/components จริง)
- [ ] L5 ★ `/fa item give <คุณ> starter_runeblade` ให้ทะเบียน DELIVERED แล้วใช้จนเสีย durability → ซ่อมคง template/version/serial/lore/enchant; เปลี่ยน holder/version/material/registry state หรือชนิด PDC ให้เสียรูปบน staging → ปฏิเสธก่อนหักเงิน
- [ ] L6 ★ Core ที่ MAILED ยังมี pending/claiming/review mail → ซ่อมไม่ได้; หลังรับสำเร็จจึงซ่อมได้; สำเนา serial 2 ชิ้นใน storage/เกราะ/มือรองหรือ stack>1 → ปฏิเสธ (ใช้สำเนาทดสอบแยกจาก world public)
- [ ] L7 ★ double click/shift/drag/hotbar/double-collect หยิบไอคอนไม่ได้; preview เกิน 60 วิ/ปิด/เปลี่ยน item/สลับ slot/ย้ายออกระยะ/ตาย/ถอนสิทธิ์ก่อนแก้ของ → cancel คืนค่าจองครั้งเดียว ไม่ย้อนธุรกรรมอื่น
- [ ] L8 vanilla repair ผ่าน anvil, grindstone สองชิ้น, crafting สองอุปกรณ์ถูกบล็อกทั้ง Prepare และ result pickup/shift/number-key; Core marker ใด ๆ ใช้ใน native crafting/anvil/grindstone ไม่ได้; การคราฟต์ปกติและ Mending/enchant table ที่อนุญาตยังทำงาน
- [ ] L9 ตรวจทุก plugin ที่มีคำสั่ง repair/aliases และ LuckPerms ด้วย non-OP ไม่ให้ซ่อมข้ามราคา Core; remote node ไม่ทำราคาเป็นศูนย์; WorldGuard flags/NPC/model click ไม่ข้ามสิทธิ์หรือระยะ
- [ ] L10 หยุด process บน staging disposable หลัง RESERVED, หลัง APPLYING ก่อนแก้ item, หลังแก้ก่อน saveData, หลัง saveData ก่อน complete และหลัง complete → เทียบ DB/ledger/playerdata; RESERVED คืน, APPLYING เป็น REVIEW ไม่คืนเอง, COMMITTED ไม่ซ่อมหรือหักซ้ำ
- [ ] L11 `/fa repair complete|cancel <op> เหตุผล` → อ่าน preview → confirm ผู้สั่งเดิมใน 60 วิ; role/reason/expiry ผิดไม่ apply, confirm ซ้ำไม่ทำซ้ำ; complete ไม่แจก/ซ่อมอีก, cancel คืนจำนวนค่าจอง ไม่แก้ item; audit ครบ
- [ ] L12 `players.disable-saving=true`/bridge อ่านไม่ได้ → repair ไม่คิดเงิน; จำลอง disk failure ตรวจ log และ state; ทดสอบอัป v0.3 DB จริงบนสำเนา → schema v5 และเงิน/บ้าน/NPC/mail/daily/exchange เดิมครบ
- [ ] L13 วัด MSPT และเวลาจริงเมื่อผู้เล่นหลายคนยืนยันซ่อมพร้อมกัน; saveData เป็น synchronous I/O จึงยังรับรองจำนวน concurrent users จาก unit tests ไม่ได้

### M. คราฟต์ Core gear (v0.5)

คู่มือสูตร/พิกัด/ทิศ NPC/การ merge config: [CRAFT-th.md](../fantasycore/CRAFT-th.md)

- [ ] M1 ★ `/fa npc spawn craft.main` หรือ bind Citizens ที่เคาน์เตอร์ซ้ายโซน 07; yaw หันเข้าผู้เล่นและ Y พื้นถูกต้อง; `/craft`/NPC/ปุ่ม `/menu` เปิดบริการเดียวกัน
- [ ] M2 ★ client Java 1.16.5 และ 26.2 เห็นทั้งสามสูตร วัตถุดิบ/จำนวนมี/ขาด ราคาและ output; ชื่อ/lore ไทยไม่ล้นจนอ่านไม่ได้; menu pagination ถ้าเพิ่ม >21 สูตร
- [ ] M3 ★ ทอง≥200 + IRON_INGOT12/LAPIS4/STICK2 → ยืนยันดาบ ตัดตรงจำนวน ทองกระเป๋าลด200 เงินฝาก/แดงคงเดิม มี mail หนึ่งรายการและทะเบียน MAILED พร้อม owner/template/version/serial ตรงกัน ไม่มีของตกพื้น
- [ ] M4 ★ กดซ้ำ/shift/drag/hotbar/double collect หยิบไอคอนไม่ได้ ไม่มี batch สำหรับ gear; ยืนยันหนึ่งรายการได้หนึ่ง serial; คราฟต์ใหม่ได้ serial คนละค่า
- [ ] M5 ★ เงินไม่พอ/เกินเพดาน/ขาดวัตถุดิบ → ไม่ตัดของหรือออกทะเบียน; มีเงินใน bank อย่างเดียวใช้ไม่ได้; ราคา0ที่ตั้งทดสอบยังต้องตัดวัตถุดิบตามสูตร
- [ ] M6 ชื่อพิเศษ/enchant/PDC/model/Core marker บนวัตถุดิบไม่ถูกนับ/ตัด; storage ธรรมดาหลาย stack รวมได้; เกราะ/มือรองไม่ถูกใช้
- [ ] M7 ★ ปิดเมนู/ตาย/ย้ายโลกหรือออกระยะ6/เปลี่ยน slot item/ถอนสิทธิ์ก่อนเริ่มตัด → cancel คืนค่าจองครั้งเดียว รักษาธุรกรรมทองที่เกิดระหว่างทาง; remote bypass เฉพาะระยะ ไม่ข้ามเงิน/วัตถุดิบ
- [ ] M8 ★ โควตา3/2/2ต่อวันไทย ข้าม recipe version ของ ID เดิมยังนับ; craft/exchange ID เดียวกัน quota แยก; active/REVIEW บล็อกทั้งสองบริการของ UUID; CANCELLED คืน quota
- [ ] M9 ★ ปิด/kill staging ในช่วง PREPARED/CONSUMING/หลัง saveData/หลัง DB COMMITTED — เปิดใหม่ PREPARED คืนทอง, CONSUMING REVIEW ไม่มี auto-item/refund, COMMITTED ไม่ออกชิ้นซ้ำ; เก็บ playerdata+DB+log ประกอบ
- [ ] M10 `/fa craft review` แสดงค่าจองและ serial/output ที่ตรึงไว้; เปลี่ยน config แล้ว recovery ยังใช้ของเดิม; complete ออกทะเบียน+mailหนึ่งครั้งและคง charge; cancel คืน charge ไม่คืนวัตถุดิบจากการเดา
- [ ] M11 ตรวจ reason≥3, ผู้ยืนยันเดิม, token60วิ, revoke permission และ confirmซ้ำ; `/fa exchange complete` ตัดสิน craft ไม่ได้; audit/ledger ครบ
- [ ] M12 ★ กระเป๋าเต็มผลยังรอ /mail; รับแล้ว identityตรงและใช้/ซ่อมได้ตาม policy; pending/claiming/review mail ห้ามซ่อมสำเนา; mail CLAIMINGค้างใช้ `/fa mail review` แทน resolve craft
- [ ] M13 DB write/mail/registry/audit failure rollback พร้อมกัน; serial conflict ไม่ charge สำเร็จ; refund overflow/ลดmax-transactionระหว่างค้างทำให้ fail closed และตรวจ log แทนลบreceipt
- [ ] M14 templateไม่พบ/versionไม่ตรง/serializedfalse/จำนวนหรือราคาแบบfloat/สูตรdisabled → doctorแจ้งและปิดเฉพาะสูตร; user configเดิมไม่ถูกเขียนทับ; mergeสองtemplateใหม่ใน items.yml ตามคู่มือ
- [ ] M15 `players.disable-saving=true` หรืออ่านbridgeไม่ได้ → ไม่ reserve; diskfailure/restoreผิดชุดต้อง REVIEW และตรวจด้วยคน; สำรองDB/world/playerdataด้วยกันและทดสอบกู้จริง
- [ ] M16 อัปv0.4 DBจริงบนสำเนา→schema5 money/mail/repair/exchangeครบ; Java/Via/NPC/native guard และ MSPTภายใต้ผู้เล่นพร้อมกันผ่านก่อนเปิดบริการ

### N. ItemsCore trial (optional; พิจารณารุ่นเต็มหลังทดสอบ)

ไฟล์ทดลอง: [trial imports](content/itemscore/README-th.md) · [แผน integration](../fantasycore/ITEMSCORE-INTEGRATION-th.md)

- [ ] N1 ทดลองคทา/healer/coat บน demo ทางการก่อน; บันทึกรุ่นที่แสดงและผล GUI/FX/cooldown ไม่สรุป compatibility ของ Core จาก demo
- [ ] N2 บน staging ของเราเมื่อมี JARถูกต้อง: บันทึก version/SHA256/API export/Java/Paper, backup config; doctorตรวจพบ แต่ไม่อ้างว่า craft/repair bridge เปิดแล้ว
- [ ] N3 importทั้ง4ชิ้น → `/itemeditor <name>` เป็น visual method tiles; export/import roundtripยังทำงาน, ไม่มี opaque raw-code block
- [ ] N4 ไม่มี `luma.itemscore.trial` → non-OPใช้ไม่ได้; ให้สิทธิ์เฉพาะบัญชีทดสอบ; userทั่วไปไม่ได้ editor/give/import/admin permissions
- [ ] N5 คทามีFX5ticks/สีteal/เสียงเบา, cooldown8วิ; healerฟื้น4HPไม่เกินmax, cooldown20วิ; ทดสอบถือมือรองและspamไม่หลบcooldown
- [ ] N6 เสื้อคลุมสีteal/armor typeและvanillastats; item unique/stackablefalse; หมดdurability/nativeanvil/cosmetics/liveupdateไม่ล้างproviderIDโดยไม่ทราบผล
- [ ] N7 เข็มทิศเปิด `/menu` เป็นplayercommand; ถอนfantasy.menuแล้วถูกปฏิเสธ; bank/craft/repairยังตรวจstationและเงิน, ไม่มีconsoleหรือOPbypass
- [ ] N8 command collision `/menu`/`craft`/repair/itemeditor/recipe/aliases; provider recipes/lootไม่มีของสอดแทรก, NativeRepairListenerไม่ข้ามpolicy; craft providerที่ไม่ใช่coreถูกปิดก่อนรับวัตถุดิบ
- [ ] N9 เมื่อทดลองVault: provider Coreลงทะเบียนก่อนItemsCore, hookตรวจพบและยอดwalletตรงbankUI; สกุลอื่นไม่เปลี่ยน, fractional/insufficient/concurrentbehaviorทดสอบจริง
- [ ] N10 ติดตั้งpackจริงแล้วทดสอบJava1.16.5/26.2/declinepack/cachehash/fonts/models/animation; ชื่อหน้า1.8–26.2ไม่แทนclientmatrix
- [ ] N11 เก็บMSPT/packet/particleเมื่อหลายผู้เล่นใช้พร้อมกัน; ลดFXก่อนขยายcatalog; ห้ามเปิดhealer/AoEในpublicจนมีregion/combat/statpolicy
- [ ] N12 ก่อนเปิดproviderbridgeต้องผ่านfactoryfreeze/UUIDคู่/registry+mailatomic/recovery/duplicateclaim/livePDC preservation; `/ic give`ยังไม่เป็นpaymentfulfillment

### O. Enchant จริงและการอัปเกรดแม่แบบ (v0.6)

คู่มือ [ENCHANTS-th.md](../fantasycore/ENCHANTS-th.md); ทุกช่องยังรอ Minecraft runtime proof

- [ ] O1 clean install: doctor ไม่มี template/recipe error, `/fa item list` current v2 ทั้ง 3 ชิ้น; preview แสดง enchant คนละบรรทัดและระดับตรงค่าจริง
- [ ] O2 คราฟต์/รับ mail แต่ละชิ้น: ตรวจ native Enchantment key/level, material, PDC/version/serial ใน ItemStack จริง ไม่ตัดสินจาก lore หรือ glint
- [ ] O3 ทดลองเทียบ iron ธรรมดากับ v2: ดาบ/ขุด/เกราะ/ความทนทาน, PvP หากเปิด; ไม่มี Mending/unbreakable/custom attributes/ทักษะที่ไม่ได้ตั้ง
- [ ] O4 สำเนาเซิร์ฟ v0.5: เก็บ item v1 ในมือและ pending mail ก่อนอัป, archive config จริงแล้วอัป v2 → รับ/ซ่อมทั้งสองรุ่นคงชื่อ/lore/enchant/version/serial/owner
- [ ] O5 ซ่อม v1 ต้องไม่เติม enchant ของ v2; ซ่อม v2 เปลี่ยน DAMAGE เท่านั้น; restart แล้ว registry/เงิน/mail ถูกต้อง
- [ ] O6 ลบ revision/แก้ lore/material ของ archive บนสำเนาทดสอบ → ปฏิเสธของเก่าก่อนจองเงิน; คืน snapshot จริงแล้วซ่อมได้
- [ ] O7 ใส่ level เกินเพดาน/ทศนิยม/key ไม่รู้จัก/alias ซ้ำ/Sharpness บน pickaxe → แม่แบบนั้นปิดและ doctor แจ้ง; archive ที่ถูกต้องกับแม่แบบอื่นยังใช้ได้
- [ ] O8 YAML syntax ผิด → Core เปิดไม่ได้; คืน config backup แล้วเปิดได้; current/output version ไม่ตรง → สูตรปิดก่อนตัดของ
- [ ] O9 ไม่แก้ config เดิมตอนเปลี่ยน JAR → v1 ยังเหมือนเดิม; เพิ่ม recipe version ระหว่างวัน → quota ของ ID เดิมไม่เพิ่มใหม่
- [ ] O10 pending/REVIEW ของ craft และ mail ก่อน restart ใช้ frozen bytes/serial เดิม; เงินไม่หักซ้ำและไม่ออกผลรุ่นใหม่แทนรายการเก่า
- [ ] O11 native anvil/grindstone/crafting และปลั๊กอินแต่ง enchant อื่นไม่ทำลาย identity; ทดสอบ downgrade/rollback ด้วย backup ชุดเดียวกัน
- [ ] O12 Java 1.16.5 และรุ่นใหม่: enchant/tooltip/preview/ประกาย/ซ่อมตรงกัน, provider conflicts และ MSPT ผ่านก่อนเปิดสาธารณะ

### P. มอนสเตอร์ตามความลึก/Target HP (v0.6)

คู่มือ [MONSTERS-th.md](../fantasycore/MONSTERS-th.md); checklist ยังรอเล่นจริง

- [ ] P1 natural spawn ที่ Y64/63/32/31/16/15/1 ได้ tier/HP/damage ถูก; worldบ้าน/hubไม่ถูกปรับ
- [ ] P2 เดินข้าม Y/โหลดchunk/restart ไม่ฮีล/คูณHPซ้ำ; ตัวเกิดก่อนอัปคงเดิม; health/marker snapshotอยู่ครบ
- [ ] P3 spawner/command/egg/raid/CUSTOM/Mythic/NPC/สัตว์/leader healthmodifiersไม่ถูกปรับ; ไม่มีdepthซ้อนบอสดัน
- [ ] P4 melee/arrow ไปPlayerคูณครั้งเดียวก่อนarmor และcapเฉพาะส่วนเพิ่ม; WorldGuard cancelยังไม่ทำdamage
- [ ] P5 Creeperแรงระเบิด/poison/fire/XP/loot/spawncountคงเดิม; เพื่อน/PvPไม่มีmultiplierของmonster
- [ ] P6 เล็ง≤8บล็อกแสดงชื่อชั้น/HPจริง; กำแพง/ผู้เล่นบัง, เลิกเล็ง/ตาย/spectator/quit/เปลี่ยนโลกแล้วซ่อน
- [ ] P7 BossBarไม่ซ้อนท้องฟ้า/เพลง/ชื่อNPC, client1.16.5และรุ่นใหม่อ่านตรง; วัดMSPTเมื่อหลายคนเล็งพร้อมกัน
- [ ] P8 ปิดenabled/restartแล้ว loaded+later-loaded Core mobsคืนoriginalhealthเป็นสัดส่วนและลบownmarker; ค่าproviderที่แก้ต่างออกไปไม่ถูกเขียนทับ
- [ ] P9 scale/Y/order/ID/NaN/booleanผิด → ปิดระบบมอนสเตอร์/doctorแจ้ง, เงินและบริการCoreอื่นไม่ถูกปิด; แก้configแล้วเปิดใหม่
- [ ] P10 ทดสอบชุดเริ่มต้นแรงขึ้นกับมอนสเตอร์4ชั้นจริง จูนkilltime/ความตาย/รายได้Fortuneและค่าซ่อมก่อนเปิด

### Q. ภาพ/แปลนดันเจี้ยน (ยังไม่ใช่ runtime)

- [ ] อ่าน [ใบงาน4ภาพ](../output/lobby-concept/dungeons/moonfall/README-th.md), สร้างtemplate/walkthrough/protect/NPCyawก่อนเปิดปุ่มเข้าดัน
- [ ] dungeon party/instance/checkpoint/boss model+animation/skill/รางวัลatomic/cleanup/compatibilityต้องผ่านใบงานก่อนประกาศเล่นได้

### R. Moonfall ดันฝึกเดี่ยว (v0.7 — ยังรอรัน Minecraft จริง)

อ่าน [DUNGEONS-th.md](../fantasycore/DUNGEONS-th.md); tests105รายการไม่ได้แทนรายการด้านล่าง ห้ามติ๊กผ่านจาก compile/ภาพ

- [ ] R1 สำเนาDBv0.6จริง→schema6: accounts/homes/items/mail/repair/craft/NPC/dailyเดิมครบ; rollbackด้วยbackupชุดเดียวกัน
- [ ] R2 clean install enabledfalseไม่สร้างโลก/ไม่รับjoin; configเดิมไม่ถูกเขียนทับ, YAML/HP/timeoutผิด doctorแจ้งและดันปิด
- [ ] R3 buildต้องmanage+reason+preview/confirm/audit, tokenหมดอายุ/ต่างผู้สั่ง/ยืนยันซ้ำไม่ทำงาน; existingunowned/symlink worldหยุดโดยไม่แตะบล็อก
- [ ] R4 newworldbuildจบ/บันทึกจริงและstatusถูก; restartกลางbuildไม่ready, resumeownedincompleteได้; buildซ้ำreadyไม่ทับงานตกแต่ง
- [ ] R5 visitแบบยังปิดไม่มีmob/reward, NPC/serviceนอกworldเปิดเมนูได้; playerสองคน/visitแข่งjoin/กดjoinซ้ำมีเพียงหนึ่งรอบ
- [ ] R6 เดินlanding/bันได4ชุด/ประตู/roomseamsจริง ช่องหัวไม่ชน, พื้นรองรับ ไม่มีvoid/บล็อกconnection/lightค้าง; ตรวจterrainทีละchunk
- [ ] R7 spawnCUSTOMไม่ถูกcancelด้วยgamerule/WorldGuard/provider; Zombie3/Husk2/Huskboss1 HP/อาวุธ/เกราะ/hungerจริงตรงguide, ไม่ซ้อนdepth
- [ ] R8 gateเปิดหลังstagecommit, ยิงจากก่อนห้อง/ย้อนห้อง/ข้ามgate/บิน/เปลี่ยนmode/ขี่/ม็อบถูกremoveไม่ทำให้ชนะฟรีหรือวงค้าง
- [ ] R9 HPbarตรงเลือดจริงหลังdamage/ตาย/regen, bossมีวงเตือน25ticksก่อนrawdamage8และarmorลดได้; wall/outsidecircleไม่โดน
- [ ] R10 ตายรักษาinventory/XPและไม่หักwallet, durability/food/arrow/potionที่ใช้ลดตามปกติ; ไม่dropของ/XP/mobgearหรือคืนของเพิ่ม
- [ ] R11 blockbreak/place/bucket/piston/fire/explosion/hanging/container/pickup/drop/PSregion/portalถูกป้องกัน; staff/providerที่ข้ามeventทำบนสำเนาเท่านั้น
- [ ] R12 pearl/chorus/home/RTP/ปลั๊กอินTPเข้าออกระหว่างPREPARING/ACTIVEถูกcancel; leaveไม่ติดcombatlockแต่ยกเลิกรอบ
- [ ] R13 quit/death/leave/adminabort/timeoutระหว่างจอง/TP/activate/wave/advance/boss/slamหยุดรอบและเคลียร์entity/bar/tasks/ticketsครบ
- [ ] R14 asyncchunkloadเกินtimeout/callbackช้าไม่เข้าแมพหรือวาร์ปซ้ำ, ชนกับleave/recoverไม่มีผู้เล่นสองคนในlaneหรือstateค้าง
- [ ] R15 จุดเดิม/โลกไม่พร้อมใช้safehub; TPcancel/unsafehubคงreturnflagและleaveซ้ำทำงาน; อย่าทดลองด้วยผู้เล่นจริง
- [ ] R16 restartช่วงACTIVE/ก่อนcompleteยกเลิกไม่เดารางวัล; completeแล้วก่อนreturn/reconnectยังมีmailเพียงรายการเดียวและพาคนกลับได้
- [ ] R17 ผ่านแล้วได้PDCเศษจันทราx2ในmailวันละครั้งไทย; เล่นซ้ำไม่แจก, วันใหม่ได้, inventoryเต็มเก็บmail; ไม่มีทอง/เงินแดง/XP/paidสิทธิ์
- [ ] R18 fault injectionDB/mail/auditบนสำเนา: receipt+mail rollbackหรือcommitครบ, ไม่successปลอม; pendingreturn/reviewตรวจได้และของเดิมไม่หาย
- [ ] R19 ★ Java26.2/1.16.5: stairs/roof/particles/HPbar/menu/nativegear/questshardเทียบกัน, packfailยังหลบskillได้; หันmodel/NPCตามสัญญาจริง
- [ ] R20 วัดMSPTตอนbuild/chunkload/combat/skillและlogoutหลายคน, ถ้าไม่ผ่านลดbudget/แก้ก่อนเปิด; Party/ModelEngineยังไม่ใช้ผลนี้แทนmatrix

คู่มือราคา/provider/ผัง NPC/recovery: [REPAIR-th.md](../fantasycore/REPAIR-th.md)

เมื่อผ่านครบ: เปลี่ยน `status` ใน manifest เป็น locked, commit `plugins.lock.json` และบันทึกผลในเอกสารสถานะ
แถวที่ไม่ผ่านให้จดข้อความ error/ภาพหน้าจอ แล้วแก้ก่อนต่อ provider adapter, custom recipe/coupon และ upgrade

### S. Moonfall ปาร์ตี้และห้องส่วนตัว (v0.8 — ยังรอ Minecraft จริง)

คู่มือปัจจุบัน: [Party Dungeons](../fantasycore/PARTY-DUNGEONS-th.md); source/tests ผ่านไม่ถือว่า checklist นี้ผ่าน
ใช้ staging ที่สำรองจาก production, 2–4 account ต่อทีม และ client1.16.5/รุ่นกลาง/26.2
ค่าเริ่มต้น enabled/party-enabled false; เช็ก SHA/JAR/config/schema7 ก่อนเปิดเพื่อ QA

- [ ] S1 อัปสำเนา DBv0.7จริง→schema7 เงิน/ledger/items/homes/NPC/mail/daily/legacyreward/จุดกลับเดิมครบ; ก่อนอัป backup DB+world/playerdata/config/JAR; ทดลองกู้ backupชุดเดียวกลับv0.7
- [ ] S2 configเดิมไม่มี party-enabled → partyปิด; messagesใหม่ mergeครบ doctorไม่มี missing; เปิดเฉพาะstaging+restartได้; productionยังfalse
- [ ] S3 buildparty1/2 ต้องสิทธิ์ เหตุผล preview/confirm/audit; slotอื่นหรือ pathปฏิเสธ; buildพร้อมกันปฏิเสธ; โฟลเดอร์ไม่มีmarkerและโลกสร้างเสร็จไม่เขียนทับ
- [ ] S4 เดินตรวจทุกslot พื้น/หัว/บันได/ประตู/border/void/มุมอับ; จุดเกิดครบ4คนและspawnม็อบถูก; นำงานตกแต่งที่reviewแล้วลงทั้ง3โลกและbackup ไม่ถือhashgeneratorเป็นchecksumโลกจริง
- [ ] S5 invite/accept60s/team4เต็ม/คนอยู่ทีมแล้ว/สิทธิ์/หัวหน้า/kickofflineUUID/disbandทำงาน; เปิดmenuแล้วshift/drag/hotbar/doubleclickเอาiconออกไม่ได้
- [ ] S6 leaderกดเข้า สมาชิกทั้งหมดonline/Survival/safefloor/noรถบิน/combat/วาร์ปค้าง; ใครไม่พร้อมหรือworld/DBไม่พร้อมไม่เริ่มครึ่งทีม; pendingreturnกันเริ่มใหม่
- [ ] S7 สองปาร์ตี้พร้อมกันคนละโลก + solo; mob/bar/damage/target/ประตู/lootไม่ข้ามทีม; ทีมที่3ไม่มีที่ปฏิเสธ ไม่เข้าโลกทีมอื่น
- [ ] S8 roster/reward/HP/attackตรึง ตรวจ HPบอส180/288/396/504 และattackจริงหลังเกราะ; ชั้น1/2/3หนักขึ้น ระยะเวลาgearv1/v2สมเหตุผล ไม่แก้balanceจากlore
- [ ] S9 หลุดหนึ่งคนกลางwave/slam → AI/velocity/damageพัก วงหาย เพื่อนตีไม่ได้; reconnectก่อน60sกลับcheckpointปลอดภัย แสดงbar ครบแล้วresume/slamหน่วง4s
- [ ] S10 สองคนหลุดคนละเวลา→deadlineแรกชนะ; quitซ้ำไม่เพิ่มเวลา; กลับที่60s/ช้า/สิทธิ์หาย/ตาย/TPถูกยกเลิก→abort; soloมีgrace; timeout18นาทียังนับช่วงพัก
- [ ] S11 PREPARINGหลุด/TPtimeout/ขยับจากจุดก่อนเข้า→ยกเลิก คืนคนที่เข้าไปแล้ว; loadingcallbackเก่าไม่วาร์ปหลังabort; ไม่นับว่ากลับจนteleportสำเร็จ
- [ ] S12 ตาย/leave/gamemode/disbandตอนล็อก/encounterหาย/คิลจากปลั๊กอินตอนพัก→abortตามกติกา; noPVP/place/break/bucket/explosion/drop/container/portalทุกslot รวมdisabled; ไม่ลบของที่พก
- [ ] S13 ผ่านครบ reward2shardsต่อUUIDในmailวันละครั้งไทย; เปลี่ยนทีม/หัวหน้า/slot/soloไม่ได้เพิ่มquota; เพื่อนquotaหมดไม่บล็อกรางวัลคนอื่น; สิ้นวันใช้dateเมื่อcomplete
- [ ] S14 จำลอง DBfailureช่วงจอง/activate/advance/finalmail/finishaudit→ไม่มีrewardบางคนหรือซ้ำ; crashก่อน/หลังcommit/ก่อนreturn→abort/recovery/mailตรงjournal; CLAIMINGยังREVIEW ไม่สุ่มคืนเอง
- [ ] S15 offlineตอนจบ/login/restart→คืนจุดของคนนั้น/unsafehubfallback; คนเก่าloginในslotที่ทีมใหม่ใช้→กลับออกโดยไม่join/abortทีมใหม่; กลับfailedเก็บneeds_returnและleaveลองใหม่ได้
- [ ] S16 cleanupม็อบ/projectile/bar/chunkticket/telegraphครบเฉพาะslot ปลดrosterหลังห้องว่าง; slot reuseไม่เอาcallback/runเก่ามาprogress; /fa dungeon abort previewระบุทุกรอบชัดเจน
- [ ] S17 วัดTPS/MSPT/chunkload/RAM/entityระหว่าง3รอบและbuild ไม่เดาตัวเลขจากunit tests; ViaBossBar/particle/หัวข้อไทย/เกราะ/telegraph/landingทุกclientตรงกัน; packfailยังอ่านและหลบได้
- [ ] S18 เจ้าของลงชื่อผลR/Sและgearplaytestก่อนproductionเปิด; custombossยังHuskfallback ไม่อ้างanimationพร้อม; ปิดparty/ทุกดัน+restartแล้วprotect/recoveryยังอยู่

ผลรอบพัฒนา 5 ต.ค.2026: compileไม่มีwarning + unit tests124ผ่าน (0fail/error/skip); ยังไม่มีผล S ในเกม
