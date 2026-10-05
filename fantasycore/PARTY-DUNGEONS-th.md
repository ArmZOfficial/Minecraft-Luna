# Moonfall — ปาร์ตี้และห้องส่วนตัว (FantasyCore v0.8)

สถานะ 5 ตุลาคม 2026: มี source, JAR และ unit tests 124 รายการผ่านแล้ว
**ยังไม่ได้รัน Minecraft จริง**; `enabled: false` และ `party-enabled: false` เป็นค่าเริ่มต้น
ใช้คู่มือนี้สำหรับรุ่นปัจจุบัน และ [คู่มือ v0.7](DUNGEONS-th.md) สำหรับรายละเอียดโครงแมพ/โหมดเดี่ยวเดิม
[ภาพ 4 ห้องและใบงานตกแต่ง](../output/lobby-concept/dungeons/moonfall/README-th.md) เป็น concept ไม่ใช่ภาพโลกที่ทดสอบแล้ว

## 1. ผู้เล่นใช้อย่างไร

1. หัวหน้าใช้ `/party invite ชื่อเพื่อน`; เพื่อนใช้ `/party accept` ภายใน 60 วินาที
2. ดูทีมด้วย `/party` หรือ `/party list`: เมนู 27 ช่องแสดงหัวหน้า สมาชิก สถานะ รับคำเชิญ ออกจากทีม และเปิดดัน
3. หัวหน้าเปิด `/dungeon` แล้วกดเข้า หรือ `/dungeon join`; ระบบเลือกห้องส่วนตัวที่พร้อมและว่าง
4. ทุกคนต้องออนไลน์ อยู่ Survival ยืนบนพื้นปลอดภัย ไม่มีรถ/บิน/วาร์ป/ต่อสู้ค้าง และมี `fantasy.dungeon`
5. ระบบตรึงสมาชิก จุดกลับ และรางวัลของแต่ละ UUID ก่อนวาร์ป; สมาชิกเปลี่ยนทีมไม่ได้จนรอบปิด
6. เมื่อครบทุกคนในห้องจึงเกิดคลื่นมอนสเตอร์ ห้องรูน → ศิลาจันทร์ → บอส ประตูเปิดเมื่อเคลียร์ครบ
7. ผ่านครบ รับเศษจันทรา ×2 ต่อคนใน `/mail` ตามโควตา แล้วกลับจุดเดิมที่ปลอดภัย

ไม่มีทีม หรือทีมเหลือคนเดียว ใช้โลกฝึกเดี่ยว; ทีม 2–4 คนใช้ห้องปาร์ตี้
เข้ารอบเดี่ยวใหม่ใน v0.8 ใช้ group journal แบบ 1 สมาชิก แต่ยังอ่านประวัติ/จุดกลับ v0.7 ได้
ถ้าห้องเต็มจะแจ้งให้ลองใหม่ ไม่มีคิวอัตโนมัติ และไม่วาร์ปไปใช้ห้องของอีกทีม

| คำสั่ง | ผล |
|---|---|
| `/party invite <ชื่อออนไลน์>` | เชิญคนที่มีสิทธิ์และไม่ได้ลงดัน; สร้างทีมให้ผู้เชิญเมื่อยังไม่มี |
| `/party accept` | รับคำเชิญที่ยังมีชีวิต; รับซ้ำหรือทีมเต็มไม่ได้ |
| `/party kick <ชื่อหรือUUID>` | หัวหน้าเอาสมาชิกออกก่อนเข้าดัน; UUID ใช้กับคนออฟไลน์ได้ |
| `/party leave` | สมาชิกออกก่อนลงดัน; หัวหน้าออกเท่ากับยุบทีม |
| `/party disband` | หัวหน้ายุบทีมก่อนลงดัน |
| `/dungeon leave` | สมาชิกคนใดก็ยกเลิกทั้งรอบ; ทุกคนกลับ ไม่มีรางวัลของรอบที่ยังไม่ผ่านครบ |

คำเชิญที่ยังไม่หมดอายุไม่ถูกหัวหน้าอื่นเขียนทับ; ล็อกรอบแล้วล้างคำเชิญที่ยังไม่ได้รับ
ไม่มี late join, สลับหัวหน้าระหว่างรอบ, kick สมาชิกที่กำลังเล่น หรือรับ loot เพิ่มจากเปลี่ยนทีม
ทีมใน lobby อยู่ใน memory; restart จะล้างทีม ต้องเชิญใหม่ ข้อมูลรอบ/จุดกลับยังอยู่ใน DB

## 2. ความจุและโลก

| Slot | ชื่อโลกคงที่ | สมาชิก/รอบ |
|---|---|---|
| training | `luma_moonfall_training` | 1 |
| party1 | `luma_moonfall_party_1` | 2–4 |
| party2 | `luma_moonfall_party_2` | 2–4 |

สูงสุด 2 ปาร์ตี้พร้อมกัน + 1 เดี่ยว = 9 ผู้เล่นในรอบ ไม่มีการสร้างหรือลบโลกต่อรัน
ทุกห้องสร้างจาก procedural plan เดียวกันขนาด 144×144 ช่วง Y20–88; ไม่คัดลอกเมืองหรือ schematic ที่ดาวน์โหลดมา
มี run ID และ tagged mob แยกต่อ slot; damage/target/progression/รางวัล/cleanup อ้าง session ของโลกนั้น
คลื่นหนึ่งมี Zombie3, คลื่นสอง Husk2, คลื่นบอส Husk1; เมื่อรอบก่อนจบ mobs ถูกล้างก่อนปล่อยห้อง
พร้อมกันสูงสุดตามคลื่นที่มี mobs มากที่สุดคือ 9 combat mobs รวม 3 ห้อง ยังต้องวัด entity/plugin overhead จริง

บิลด์หนึ่งโลกต่อครั้ง ผ่านชื่อที่โค้ดอนุญาตเท่านั้น ไม่รับ path หรือชื่อโลกจากผู้เล่น
ตรวจ ownership marker ของโฟลเดอร์ก่อนโหลด/เขียน; ชื่อหรือโฟลเดอร์ชนโดยไม่มี marker จะหยุด
แมพที่มี version สำเร็จแล้วจะไม่ถูก build ทับ แม้ hash/version ไม่ตรง
โลกฝึกเดิม v0.7 version1 ที่ไม่มี hash ยังอ่านได้เพื่อรักษางานตกแต่ง ไม่แอบ rebuild ให้

**SHA256 template hash คือ hash ของ generator plan ที่เรียงพิกัด/BlockData** ไม่ใช่ checksum ของทุกบล็อกในโลกจริง
party world ต้องมี hash ตรงรุ่นที่สร้าง; hash ไม่ยืนยันว่าทีมงานวางบล็อกผิดหรือแก้โลกหลังสร้าง
ต้องเดินตรวจ/ถ่ายภาพ/ลงชื่อรับงานตาม checklist ทุกโลก และสำรอง world ที่ตกแต่งแล้ว
ไม่มีระบบ restore ทุกบล็อกหลังรอบในรุ่นนี้; block protection ช่วยคงโครงสร้าง แต่ WorldEdit/ปลั๊กอินที่เขียนตรงยังต้องควบคุม

## 3. บาลานซ์ตามจำนวนคน

ใช้จำนวนสมาชิกที่ตรึงตอนเริ่ม N ตั้งแต่ 1–4:
HP = base × (1 + 0.6 × (N−1)), จำกัดไม่เกิน 1000 HP ต่อม็อบ
attack = base × (1 + 0.12 × (N−1)); ค่าโจมตีตารางเป็นค่าดิบก่อนเกราะและกติกา Minecraft
หลุดเกม ตาย หรือออกไม่ทำให้ HP/attack ที่เหลือและสิทธิ์รางวัลปรับตามสมาชิกใหม่

| จำนวนคน | Zombie HP ต่อหนึ่งตัว | Husk HP ต่อหนึ่งตัว | บอส HP | บอสโจมตีดิบ | slam ดิบ |
|---|---:|---:|---:|---:|---:|
| 1 | 30 | 50 | 180 | 6 | 8 |
| 2 | 48 | 80 | 288 | 6.72 | 8.96 |
| 3 | 66 | 110 | 396 | 7.44 | 9.92 |
| 4 | 84 | 140 | 504 | 8.16 | 10.88 |

Zombie attack base4 และ Husk base5 ใช้สูตรเดียวกัน
ชุด enchant [v0.6](ENCHANTS-th.md) ยังคงเดิม; ต้องเล่นจริงด้วย gear v1/v2 ก่อนจูนระยะเวลาและความยาก
depth listener ไม่คูณ CUSTOM encounter ซ้ำ รางวัลไม่เพิ่ม combat stat และไม่มีค่าเข้า
BossBar แสดง HP รวมของม็อบที่ยังอยู่ในคลื่น; บอสเป็น Husk native ไม่มี custom boss model ในรุ่นนี้
slam มีวงม่วงรัศมี4 บล็อกเตือน25 ticks จึงทุบ; ตรวจ line of sight และทุกคนในวง
จูนฟอร์มูลาต้องแก้ source/build และผ่าน QA; config เปิดแก้ base HP 20–500 ตามข้อกำหนดเดิม

## 4. หลุดเกมและกรณีจบรอบ

หลัง ACTIVE ถ้าสมาชิกหลุดจะพัก AI, velocity และการทำ damage; ยกเลิก windup slam
บาร์แสดงเวลารอสมาชิกที่ใกล้หมดเวลา ทุกคนกลับทันจึงเปิด AI และหน่วง slam ถัดไป4วินาที
60 วินาทีใช้เวลาจริงต่อสมาชิก เรียก quit ซ้ำไม่ต่อเวลา; กลับที่เวลาครบ60วินาทีถือว่าช้า
หลุดอีกครั้งหลังกลับสำเร็จเริ่มช่วงใหม่ได้ แต่ timeout ทั้งรอบยังจำกัด18นาทีค่าเริ่มต้น
ไม่ได้หยุดนาฬิกาโลก ยา cooldown หรือเวลารอบทั้งหมด; เป็นการพัก AI/ความเสียหายสำหรับ reconnect
โหมดเดี่ยว v0.8 ได้ grace เดียวกัน; เปลี่ยนจาก v0.7 ที่ออกเกมแล้วยกเลิกทันที

กลับเข้าแล้วตรวจสิทธิ์ Survival สภาพผู้เล่น และจุด checkpoint อีกครั้ง
เมื่อวาร์ปถึงพื้นปลอดภัยภายใน deadline สำเร็จจึงนับว่ากลับ; async chunk load มี deadline/nonce กัน callback เก่า
ถ้ามีอีกคนยังออฟไลน์จะพักต่อ ห้ามสมาชิกตีม็อบตอนเพื่อนไม่อยู่
PREPARING ยังวาร์ปไม่ครบแล้วมีคนออก → ยกเลิกทันที ไม่ใช้ grace
ตาย, เปลี่ยน gamemode, ออกด้วยคำสั่ง, timeout, กลับไม่ทัน, encounter หาย/ถูกลบผิดปกติ → ยกเลิกทั้งรอบ
ไม่มี revive หรือ resume combat ข้าม restart ในรุ่นนี้

| จุด | ตำแหน่งเท้าคนกลาง | การกระจายสมาชิก |
|---|---|---|
| entry | (22.5,88,18.5), yaw0 | offset ตาม X |
| hall เมื่อเริ่ม wave | (22.5,78,46.5), yaw0 | offset ตาม X |
| reliquary | (106.5,38,61.5), yaw−90 | offset ตาม Z |
| boss | (116.5,20,100.5), yaw0 | offset ตาม X |

offset = (ลำดับสมาชิก − (N−1)/2) ×2; ทีม4คนใช้ −3/−1/+1/+3
unit test ตรวจพื้น/ช่องหัวของจุดเหล่านี้สำหรับ N1–4; runtime ยังตรวจ landing ก่อนวาร์ปจริง
ห้องใหม่เกิดม็อบเมื่อครบสมาชิกในพื้นที่; ระหว่างต่อสู้เดินออกห้องจะ tether กลับ checkpoint
เดินผ่านประตูไปข้าม encounter ไม่ถือว่าผ่านรอบ

## 5. รางวัล ข้อมูล และการกู้คืน

รางวัลต่อ UUID คือ PRISMARINE_SHARD ×2 พร้อมชื่อ/ลอร์/PDC quest marker ที่ตรึง bytes ก่อนเข้า
ยังไม่มี recipe ใช้เศษจันทรา ไม่ให้ทอง เงินแดง XP mob drop หรือ paid key
quota ใช้ ID `moonfall_training` ต่อ UUID ต่อวันที่ไทย (00:00 Asia/Bangkok) ร่วมทุก slot และประวัติรุ่นเดิม
เวลาที่ใช้ระบุวันคือเมื่อ complete; เริ่มก่อนเที่ยงคืนและจบหลังเที่ยงคืนใช้วันที่จบ
ครบโควตายังช่วยทีมเล่นได้แต่ไม่รับของเพิ่ม; เปลี่ยนหัวหน้า/ทีม/รางวัล version ไม่ล้าง quota

schema7 เพิ่ม:
- `dungeon_group_runs`: run/leader/party/instance/state/stage/member_count/template_hash
- `dungeon_group_members`: UUID/join snapshot/จุดกลับ/needs_return/reward bytes/version แยกคน
- `dungeon_group_rewards`: receipt/mail ID/วันที่ แยกสมาชิก

ตารางเดิม schema6 และข้อมูล economy/ledger/items/homes/NPC/mail ไม่ถูกลบหรือย้าย
transaction begin จอง slot+ทุกสมาชิก+audit ก่อนวาร์ป; สมาชิกที่ needs_return ค้างเริ่มรอบใหม่ไม่ได้
stage update ตรวจ leader/state/ลำดับ; final complete ตรวจ stage3 และสมาชิกครบตาม snapshot
mail+receipt ของสมาชิกที่ยังมีโควตา+COMPLETED+audit อยู่ transaction เดียวกัน
สมาชิกที่ได้ quota ไปแล้วไม่ขัดขวางรางวัลเพื่อน; complete ซ้ำได้ผล already ไม่เพิ่ม mail
หากสร้าง mail/receipt/audit คนใดไม่สำเร็จ rollback ทั้งชุดและรอบหยุด ไม่ส่งของบางคนแล้วเดาเติมเอง
leader เป็นผู้ advance/complete; สมาชิกที่อยู่ใน snapshot มีสิทธิ์ abort/return ของตัวเอง คนภายนอกไม่มี

restart/recovery ยกเลิก PREPARING/ACTIVE ทั้งเก่าและใหม่ เก็บจุดกลับ และคง mail ที่ commit สำเร็จ
ออฟไลน์ขณะจบรอบกลับตอน login; จุดเดิมอันตรายหรือ world ไม่พร้อมใช้ safe hub
ล้าง needs_return เมื่อพ้นโลกดันสำเร็จแล้ว ถ้ายังกลับไม่ได้ยังคง journal และใช้ /dungeon leave ลองใหม่
login ของสมาชิกเก่าที่ห้องถูกทีมใหม่ใช้อยู่ต้องนำสมาชิกเก่าออกโดยไม่เข้า roster ใหม่; มี gate ตรวจ recovery ก่อน visitor guard
**stage3 กับ complete เป็นคนละ transaction**: crash คั่นกลางจะ abort ไม่เดาว่าควรแจก loot
เมื่อยืนยัน kill ครบแล้ว complete commit ก่อน quit/abort ที่เข้าคิวภายหลัง mail จะอยู่ตามผลที่ commit
ไม่อ้าง atomicity ระหว่าง world/playerdata กับ SQLite; Mail CLAIMING/REVIEW ยังใช้ [ขั้นตอนเดิม](README-th.md)

## 6. วิธีติดตั้งบน staging และ rollback

1. หยุดเซิร์ฟและสำรอง JAR/config/Core DB (+wal/shmถ้ายังมี)/playerdata/โลก/WorldGuard เป็นชุดเดียว ตั้งชื่อก่อน-v0.8
2. สำเนาชุดนั้นไป staging; ไม่เปลี่ยน EULA โดยอัตโนมัติ และไม่ใช้ /reload
3. Build v0.8: JAR `build/libs/FantasyCore-0.8.0.jar`; คัดลอกแทน Core เก่าเพียงตัวเดียว ตรวจ log schema v7 และ /fa doctor
4. resource เดิมไม่ถูกเขียนทับ: merge `party-enabled: false` และข้อความใหม่จาก `src/main/resources/messages_th.yml` ให้ครบ ดู missing keys ใน doctor
5. สร้าง/ตรวจโลกที่ยังไม่มีด้วยคำสั่งตารางด้านล่าง รอสร้างเสร็จทีละโลก ห้ามแก้/วาง NPC บนจุดเกิดและช่องทางเดิน
6. staging เท่านั้น: หลังตรวจแมพ เปิด `enabled: true` + `party-enabled: true` แล้ว restart เพื่อเล่น checklist R/S กับ client1.16.5/รุ่นกลาง/26.2
7. production คง false จนเจ้าของรับผลทดสอบจริงและ backup พร้อม

| คำสั่งแอดมิน | ผล |
|---|---|
| `/fa dungeon status` | สถานะรวมและทุก slot |
| `/fa dungeon build <เหตุผล>` | preview โลกเดี่ยว → /fa confirm token |
| `/fa dungeon buildparty 1 <เหตุผล>` | preview party1 → confirm; เปลี่ยนเป็น2เพื่อ party2 |
| `/fa dungeon visit [training\|party1\|party2]` | staff ตรวจห้องว่าง ไม่มี encounter/รางวัล; ออกด้วย /dungeon leave |
| `/fa dungeon abort <เหตุผล>` | preview ระบุ **ALL ACTIVE INSTANCES** → confirm ยกเลิกทุกรอบ |

`fantasy.party` เป็นสิทธิ์ผู้เล่นผ่าน fantasy.player; staff ต้อง fantasyadmin.view + fantasyadmin.dungeon.manage
งาน build/abort บันทึก audit ก่อนเริ่ม มี token60วินาที ตรวจสิทธิ์ซ้ำตอนยืนยัน
ownership ไม่แทน backup; ไม่ลบ world เมื่อ build error ไม่เขียนทับโลกที่สำเร็จแล้ว
NPC ประตูอยู่ hub ใช้ action `dungeon.main`; ปุ่ม party ในเมนูดันจัดทีมได้ ไม่สร้าง service NPC ภายในโลกต่อสู้

ถ้าต้องปิดเฉพาะ party ตั้ง party-enabled false และ restart; solo ใช้ต่อได้
ถ้าปิดทุกดันตั้ง enabled false และ restart; protect/return ของ owned worlds ยังทำงาน
ย้อน v0.7 ต้องหยุดและกู้ JAR+DB schema6+config+world/playerdata จาก backupชุดเดียว ไม่ย้อนเฉพาะ JARและไม่dropตาราง v7
ข้อมูลรอบ/lootหลัง backup จะหายในการ restore จึงต้องบันทึกและแจ้งเจ้าของก่อนทำจริง
ไม่มี script ลด schema7 หรือโอน group history กลับตาราง solo; migration นี้เป็นการเพิ่มตารางและอ่าน legacy เท่านั้น

## 7. หลักฐานและงานต่อไป

124 JUnit tests ผ่าน: เพิ่ม19กรณีจาก v0.7 คือ PartyRoster5, Presence3, GroupStore9, map/scaling2
ทดสอบสองslot/สมาชิกซ้อน, starts/complete12threads, frozenreward, rollbackเมื่อสมาชิกคนที่สอง/finishauditล้ม,
quotaเก่า↔ใหม่, restart/reopen, migrateสำเนาschema6 และ immutable roster/deadline/headroom
compile ผ่าน Paper API ที่ตรึงไว้, YAML9ไฟล์/MiniMessage377ข้อความตรวจได้
JARที่buildตรวจรอบนี้: `FantasyCore-0.8.0.jar`, SHA256 `5EF2B8BE2749C842D05F2F64D56DEE66B5ABC68F99F41BEF86AD71B0C5A25D08`
ตรวจลิงก์Markdown347จุดและpilotmodelเดิม6ชุดผ่าน; JARเป็นผลbuildในเครื่องและCI ไม่commit binary ลงrepo
ยังไม่มี Bukkit runtime ใน tests; [checklist S](../server/README-th.md) ต้องทดสอบทุกข้อด้วย Minecraft จริง
ภาพ/world decoration, ModelEngine/MythicMobs adapter, gear playtest และ loot recipe ยังรอทำ/ทดสอบ
[โมเดลบอสจันทราลงสีและ8animations](../server/content/dungeons/moonfall/README-th.md) exportผ่านBlockbenchแล้ว แต่Coreยังใช้Husk ไม่ได้เรียกmodelในเกม

งานลำดับถัดไปหลัง QA: polish แมพทั้ง3โลก → reference turnaroundบอสจากภาพ04 → Blockbenchที่เชื่อมจริง
→ bbmodel/texture/animation/hitbox/telegraph export → provider adapter → clientmatrix → recipeเศษจันทราและlootcosmetic
ยังไม่เปิดขาย combat reward ของดันผ่านเว็บ; payment dev/website bridge เป็นงานคนละ phase ตาม [แผนเว็บ](../output/lobby-concept/WEB-AND-AUTOMATION-PLAN-th.md)
