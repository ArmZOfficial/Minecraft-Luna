# มอนสเตอร์ตามความลึก — Core v0.6

เพิ่มความท้าทายให้รองรับ [Enchant ชุดเริ่มต้น](ENCHANTS-th.md) ที่เด่นขึ้น โดยใช้ Y ตอนมอนสเตอร์เกิดใน `luma_resource`
สถานะ: โค้ด listener/BossBar และกฎ config ทำแล้ว; **ยังไม่ผ่านการเล่นจริงใน Minecraft**
ดันเจี้ยนเป็นอีกระบบ: อ่าน [ซากวิหารจันทราและแปลนดัน](../output/lobby-concept/dungeons/moonfall/README-th.md)

## 1. ความยากเริ่มต้น

| ชั้น / สีแถบ | Y ตอนเกิด | HP multiplier | ดาเมจตี/ยิง | ตัวอย่างฐาน 20 HP |
|---|---|---:|---:|---:|
| 1 ผิวดิน / เขียว | ≥64 | ×1.0 | ×1.0 | 20 HP |
| 2 ถ้ำรูน / เหลือง | 32–63 | ×1.5 | ×1.2 | 30 HP |
| 3 ถ้ำลึก / แดง | 16–31 | ×2.2 | ×1.5 | 44 HP |
| 4 ห้วงจันทรา / ม่วง | ≤15 | ×3.2 | ×1.8 | 64 HP |

HP มีเพดาน 200; ไม่สร้างมอนสเตอร์เกินจำนวนเดิม และไม่เพิ่มเงิน/XP/loot ตามเลือด
ตัวเลข HP คำนวณจาก max-health จริงของตัวที่เกิด ไม่ตั้งทุกชนิดเป็น 20 HP
ส่วนดาเมจที่ Core เพิ่มจำกัดถึง raw 12 HP ก่อน armor/absorption/resistance; การโจมตี vanilla ที่เกิน 12 อยู่ก่อนจะไม่ถูกลดลง
ตัวอย่าง raw 4 → 7.2 ที่ชั้น 4, raw 10 → 12, raw 15 → 15 ไม่ใช่การรับรองว่าดาเมจสุดท้ายจะเท่านี้

เลือกเฉพาะ Zombie, Husk, Drowned, Skeleton, Stray, Spider, Cave Spider, Creeper และ Enderman
ต้องเกิดด้วย `SpawnReason.NATURAL`; mob ที่มีชื่อ/PDC/scoreboard tag/NPC metadata อยู่ก่อน หรือมี health attribute modifiers จะถูกข้าม
boss/สัตว์/ชาวบ้าน, spawner/egg/command/raid/reinforcement/CUSTOM spawn ไม่เข้า scaling รุ่นนี้
ถ้า NPC/provider เพิ่ม PDC/scoreboard marker ภายหลัง Core ถอดระดับและคืนเฉพาะ health base ที่ยังตรง snapshot เพื่อไม่คูณซ้อน; nametagอย่างเดียวไม่ถอดระดับที่ติดไว้แล้ว
ดาเมจเพิ่มเฉพาะการตีตรงและ projectile จากมอนสเตอร์ที่มี marker Core ไปยังผู้เล่น
Creeper เพิ่ม HP แต่คงแรง/รัศมีระเบิด; poison, potion, fire และความเสียหายสิ่งแวดล้อมไม่ถูกคูณ
Armor/พฤติกรรม AI, ความเร็ว, knockback และตาราง loot ไม่ถูกเพิ่มโดยระบบนี้

## 2. แถบเลือดที่มองเห็น

เมื่อเล็งมอนสเตอร์ Core ภายใน 8 บล็อก จะแสดง BossBar หนึ่งแถบต่อผู้เล่น เช่น:

```text
✦ ถ้ำลึก · ชั้น 3 | Zombie · 36/44 HP
```

ชื่อชนิดมอนสเตอร์ใช้ translation key ของ vanilla; ภาษาแสดงตาม client
ตรวจด้วย ray trace ที่คำนึงถึงบล็อกและ entity ด้านหน้า ไม่เห็นเลือดทะลุกำแพงหรือผ่านผู้เล่นที่บัง
อัปเดตทุก 10 ticks (ประมาณ 0.5 วินาทีเมื่อ 20 TPS); เลิกเล็ง/ออกโลก/ตาย/เป็น spectator/ออกเกมแล้วซ่อน
ไม่ตั้ง nameplate บนมอนสเตอร์ทุกตัว ไม่สร้าง armor stand/hologram เพิ่ม ไม่ใช้ packet plugin หรือ resource pack
จึงลดสิ่งที่ต้อง render แต่ **ยังต้องวัด MSPT และความลื่นจริง** ก่อนรับรองกับจำนวนผู้เล่นเป้าหมาย
BossBar ไม่มี flags เปลี่ยนฟ้า/หมอก/เพลง และไม่ทำงานกับบอส Mythic เพื่อเลี่ยงแถบซ้อน

## 3. Config และการรักษาสถานะ

ไฟล์ [monsters.yml](src/main/resources/monsters.yml) ถูกสร้างใน `plugins/FantasyCore` เมื่อยังไม่มี
`enabled: true` เปิดการติดระดับแก่ natural spawn ใหม่; `worlds` default มีเฉพาะโลกทรัพยากร ไม่รวม hub/บ้าน/ดัน/โลกบอส
`target-health-bar: false` ปิดเฉพาะแถบเลือดได้โดยไม่เปลี่ยน scaling
ใช้ `/fa doctor` ดูสถานะ โลก จำนวนชั้นและข้อผิดพลาด; แก้ไฟล์ตอนหยุดเซิร์ฟ แล้ว restart

กฎ validation: 1–8 ชั้น, ID ไม่ซ้ำ, Y เรียงสูงไปต่ำ, HP/damage ห้ามลดเมื่อยิ่งลึก
HP scale 1–4, damage scale 1–2, HP cap 20–1000, raw-damage-cap 1–40; ทุกค่าต้อง finite ไม่รับข้อความแทนตัวเลข
Y threshold ต้องจำนวนเต็ม −64..320; สี GREEN/YELLOW/RED/PURPLE; config ผิดปิดเฉพาะระบบมอนสเตอร์และรายงาน doctor

ติด PDC profile version/ชื่อชั้น/ลำดับ/สี/damage multiplier/cap และ original/scaled max-health base ไว้ที่ entity
health attribute และ marker ถูกบันทึกพร้อมโลกเมื่อเซิร์ฟ save ตามปกติ ไม่เพิ่มตาราง SQLite ในรุ่นนี้
ระดับตรึงตอนเกิด: ลากมอนสเตอร์ขึ้นผิวดินยังเป็นระดับเดิม; ไม่ฮีลและไม่คูณ HP ใหม่เมื่อเดินข้าม Y หรือ restart
เปลี่ยน multiplier ใน config มีผลกับตัวเกิดใหม่; ตัวเก่ายังใช้ damage snapshot ที่บันทึกไว้ ไม่แอบปรับให้แรงขึ้น
ตัวที่เกิดก่อน listener เปิดจะไม่ถูกไล่ปรับย้อนหลัง; รอ natural despawn/เกิดใหม่

ปิด `enabled` หรือ config ใช้ไม่ได้: หยุด scaling และแถบ, คืน original health base ของตัว Core ใน chunks ที่โหลดแล้ว/โหลดภายหลัง
คงสัดส่วนเลือด ไม่เติมจนเต็ม; ลบเฉพาะ marker ของ Core
ถ้าปลั๊กอินอื่นเปลี่ยน health base ไปจาก scaled snapshot จะรักษาค่าใหม่ไว้ และถอด marker ของ Coreเพื่อไม่ซ้อนพลังต่อ
หากถอน JAR ทั้งตัวจะไม่มี listener คืน attribute: ให้ปิดระบบด้วย config และโหลด chunks ให้ครบ หรือกู้ backup โลกชุดก่อนเปลี่ยน
unloaded mobs ที่ไม่เคยโหลดในรอบปิดยังเก็บ HP เดิมจนเข้า chunk; ไม่สแกน/โหลดโลกทั้งหมดเพื่อรีเซ็ต

## 4. Java เก่าและดันเจี้ยน

เป้าหมายยังเป็น Java 1.16.5+ ผ่าน Via; native entity/enchants/BossBar ไม่ได้ยืนยัน compatibility จนผ่าน staging
client <1.17 มีข้อจำกัด Y 0–255: RTP เดิมอยู่ Y16–200; สำหรับถ้ำชั้น 4 ให้ทำเส้นทางที่เท้าผู้เล่น Y≥1 และพื้น Y≥0
Y ติดลบยังใช้ tier สุดท้ายบน backend แต่ห้ามใช้เป็นเส้นทางหลักของ client เก่า; ต้องตรวจ height guard/โลกจริงก่อนเปิด
ดันแรกวางเท้าผู้เล่นทุกห้อง Y20–88 เพื่อหลีกเลี่ยงปัญหานี้

ดันเจี้ยนจะใช้ความยากตาม room/encounter และขนาดปาร์ตี้ ไม่เดาจาก Y ของห้องเพียงอย่างเดียว
Mythic/CUSTOM spawn ยังถูกข้ามโดยระบบนี้; dungeon provider ต้องเป็นเจ้าของ HP/skill/loot ของ encounter เพียงระบบเดียว
ปุ่มเข้า, instance, checkpoint, boss model/animation และรางวัลดันยังเป็นงานตามแผน ไม่เปิดจาก listener นี้

## 5. ตรวจรับ

Unit tests ตรวจขอบ Y, multiplier/cap, จำนวน/order/ID ของชั้น, finite numbers และ config ที่ผิดชนิด
ยังไม่ได้ทดสอบ event spawn จริง, armor/projectile, health persistence, reset/BossBar/WorldGuard/providers หรือ MSPT ในเกม
ทำ checklist **P** ใน [server/README-th.md](../server/README-th.md) รวมคู่มือ [Enchant](ENCHANTS-th.md) ก่อน deploy

API ที่ใช้อ้างอิง [CreatureSpawnEvent](https://jd.papermc.io/paper/26.2/org/bukkit/event/entity/CreatureSpawnEvent.html),
[EntityDamageByEntityEvent](https://jd.papermc.io/paper/26.2/org/bukkit/event/entity/EntityDamageByEntityEvent.html),
[MAX_HEALTH attribute](https://jd.papermc.io/paper/26.2/org/bukkit/attribute/Attribute.html)
