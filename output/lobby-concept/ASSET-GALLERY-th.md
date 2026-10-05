# ภาพอ้างอิงและโมเดล pilot ของ Luma

ภาพอ้างอิงสร้างด้วย ChatGPT image generation โมเดลสร้างและ export ผ่าน Blockbench MCP ที่เชื่อมในเครื่องจริง
**ภาพอ้างอิงละเอียดกว่า geometry ของ pilot** ต้องเก็บ silhouette/UV/รายละเอียดเพิ่มก่อน production
ไฟล์ `.bbmodel` เป็นไฟล์แก้ไขได้ ไม่ใช่ plugin หรือ resource pack ที่ติดตั้งพร้อมเล่น

## 01 — เจ้าหน้าที่ธนาคารเวทมนตร์

![ภาพอ้างอิงนายธนาคาร](assets/references/npc-arcane-banker.png)
![โมเดล pilot ธนาคาร](assets/models/npc_arcane_banker-preview.png)

ไฟล์ [npc_arcane_banker.bbmodel](assets/models/npc_arcane_banker.bbmodel) · 36 cubes · `idle`, `greet`, `count_coins`
สร้างเสื้อคลุมเขียวหยก ขอบทอง สมุดบัญชีและตราคลัง เพิ่ม texture เสื้อ/มือและเหรียญใน v2
วางที่ bank teller ตามใบงานโซน 05 หันเข้าจุดยืนผู้เล่น ใช้ hitbox ของเคาน์เตอร์ที่อ่านทิศได้ชัดเจน

### v2 ลงรายละเอียดเต็ม (6 ต.ค.2026)

![โมเดลธนาคารv2](assets/models/npc_arcane_banker_v2-preview.png)
![v2ด้านหน้า](assets/models/npc_arcane_banker_v2-front.png)
![v2เปิดสมุด](assets/models/npc_arcane_banker_v2-inspect-ledger.png)

[ไฟล์v2](assets/models/npc_arcane_banker_v2.bbmodel) · [texture256×256](assets/models/npc_arcane_banker_v2.png) · [manifest](assets/models/npc_arcane_banker_v2-manifest.json)
182cubes/17bones/4animations/87keys: idle, greet, count_coins, inspect_ledger
แว่นกรอบโปร่งเห็นตา หูยื่นแยกผม เคราแก้ม/กราม/ปลายพร้อมหัวเข็มขัด ปกเสื้องาช้าง สายสร้อยและเหรียญตรา
บ่าทองฝังคริสตัล ข้อมือขอบทอง นิ้วแยก แหวน ชายเสื้อแยกซ้ายขวาพร้อมแถบงาช้าง กุญแจนิรภัยและกระเป๋าเหรียญที่เอว รองเท้ามีหัวเข็มขัด
สมุดบัญชีมีปก/หน้ากระดาษ/สัน/มุมทอง/ตรา/ริบบิ้น และนิ้วโป้งจับปก; เหรียญแปดเหลี่ยมอยู่ในนิ้วขวา หลังเสื้อปักกุญแจห้องนิรภัย
ตรวจ17เฟรมจากtimelineจริง: สมุด/เหรียญติดมือทุกเฟรม (gap 0) และ `python tools/verify_arcane_banker.py` ผ่าน; v1เก็บไว้ไม่แก้
เกินbudgetNPCเมือง(40–70cubes) จึงเหมาะกับnpcธนาคารตัวเดียวที่เคาน์เตอร์ ถ้าวางหลายตัวต้องทำLOD/ตัวเบาแยก; ยังไม่ทดสอบในเกม
⚠️ ชื่อboneซ้าย/ขวาของbanker v2 สลับกับตัวละครจริง: โมเดลหันทิศเหนือ(−Z) มือขวาตัวละครคือ+X แต่bone `left_hand`(ถือสมุด)อยู่+X ภาพไม่ผิด แต่ถ้าโค้ดruntimeอ้างชื่อมือต้องใช้ตามนี้ หรือแก้ชื่อในrevisionถัดไป (smith v2 ใช้ชื่อถูกแล้ว)

## 02 — ช่างตีเหล็กรูน

![ภาพอ้างอิงช่าง](assets/references/npc-rune-smith.png)
![โมเดล pilot ช่าง](assets/models/npc_rune_smith-preview.png)

ไฟล์ [npc_rune_smith.bbmodel](assets/models/npc_rune_smith.bbmodel) · 38 cubes · `idle`, `greet`, `hammer`
เพิ่ม apron folds, ถุงมือ, UV รายละเอียดโลหะและ socket ค้อน ท่าตีต้องตกบนทั่งที่พิกัดเดียวกับ station
วางด้านในโรงตีเหล็ก ห้ามแกว่งค้อนทับช่องคลิกซ่อมของหรือทางเดิน

### v2 ลงรายละเอียดเต็ม (6 ต.ค.2026)

![โมเดลช่างv2](assets/models/npc_rune_smith_v2-preview.png)
![v2ด้านหน้า](assets/models/npc_rune_smith_v2-front.png)
![v2ง้างค้อน](assets/models/npc_rune_smith_v2-hammer-windup.png)

[ไฟล์v2](assets/models/npc_rune_smith_v2.bbmodel) · [texture256×256](assets/models/npc_rune_smith_v2.png) · [manifest](assets/models/npc_rune_smith_v2-manifest.json)
171cubes/16bones/4animations/103keys: idle, greet, hammer, craft_success; สูง2.2บล็อก
แว่นช่างกรอบทองเลนส์ฟ้าพร้อมสายรัดและหัวเข็มขัดหลังหัว เคราเต็ม หนวด คิ้ว หูยื่น ผมแยกหน้า/หลังหู
ผ้ากันเปื้อนหนังเขียวขอบทอง ลายรูนฟ้า ชายเว้าตามreference สายสะพายไขว้Xด้านหลังพร้อมหัวเข็มขัด เข็มขัดใหญ่ กระเป๋าฝังคริสตัล
แขนเปลือย ปลอกแขนหนังขอบทองฝังอัญมณี กำปั้นแยกนิ้ว เกราะไหล่งาช้างเฉพาะไหล่ซ้าย; ค้อนมือขวา(ด้ามพันหนัง หัวเหล็กคาดทอง) คีมแขวนสะโพกซ้าย
ท่า `hammer` ตี2ครั้ง กระทบที่tick 11 และ 25 (ใช้syncเสียงทั่งฝั่งserver); `craft_success` ชูค้อนหมุน
ตรวจ19เฟรม: ค้อนติดกำปั้นทุกเฟรม (gap 0) แขนที่ยกไม่ทะลุหัว; `python tools/verify_rune_smith.py` ผ่าน; v1เก็บไว้ไม่แก้
เกินbudgetNPCเมืองเช่นเดียวกับbanker v2 เหมาะกับช่างประจำโรงตีเหล็กตัวเดียว; ยังไม่ทดสอบในเกม

## 03 — ผู้ดูแลเควส

![ภาพอ้างอิงผู้ดูแลเควส](assets/references/npc-quest-warden.png)
![โมเดล pilot ผู้ดูแลเควส](assets/models/npc_quest_warden-preview.png)

ไฟล์ [npc_quest_warden.bbmodel](assets/models/npc_quest_warden.bbmodel) · 30 cubes · `idle`, `greet`, `offer_scroll`
เพิ่มกระเป๋า หนังสือ ม้วนกระดาษและลายผ้า ท่ายื่นม้วนกระดาษต้องเคลื่อนจากมือเดิม ไม่มี prop ลอย
วางหน้าบอร์ดเควส ด้านหลังมีพื้นที่อ่านบอร์ด และแบ่งช่องคุย NPC กับช่องเลือกเควส

### v2 ลงรายละเอียดเต็ม (6 ต.ค.2026)

![โมเดลผู้ดูแลเควสv2](assets/models/npc_quest_warden_v2-preview.png)
![v2ด้านหน้า](assets/models/npc_quest_warden_v2-front.png)
![v2ยื่นม้วนเควส](assets/models/npc_quest_warden_v2-offer-scroll.png)

[ไฟล์v2](assets/models/npc_quest_warden_v2.bbmodel) · [texture256×256](assets/models/npc_quest_warden_v2.png) · [manifest](assets/models/npc_quest_warden_v2-manifest.json)
123cubes/17bones/4animations/74keys: idle, greet, offer_scroll, point_direction; สูง2.1บล็อก
เอลฟ์หูแหลมทะลุผมที่แยกหน้า/หลังหู ผมงาช้างไล่ชั้นหน้าม้า ปอยข้างแก้ม มงกุฎผมไม่เท่ากัน ตาฟ้าโตพร้อมไฮไลต์และแก้มชมพู
เสื้อโค้ทยาวสีน้ำเงินเขียว ขนสัตว์งาช้างที่ปก/ไหล่/ชายเสื้อ เข็มกลัดทองฝังคริสตัล เสื้อชั้นในเข้ม ลายทองที่ชายหน้า/หลัง ตราทองกลางหลัง
ชายเสื้อแยกซ้ายขวาแกว่งได้ ข้อมือขนสัตว์มีตะขอทองฝังอัญมณี เข็มขัดหัวทอง กระเป๋าสะโพกซ้าย เข็มทิศห้อยสะโพกขวา บูทมีอัญมณีหน้าแข้ง
ม้วนเควสปิดผนึก(ฝาทอง ริบบิ้น ตราอัญมณี)อยู่มือซ้ายตามticket ท่า `offer_scroll` หมุนม้วนเป็นแนวนอนยื่นให้ผู้เล่น; `point_direction` ชี้ทางพร้อมหันหน้า
ตรวจ17เฟรม: ม้วน/เข็มทิศติดตัวทุกเฟรม (gap 0) วัดตำแหน่งมือจริงไม่ทะลุลำตัว; `python tools/verify_quest_warden.py` ผ่าน; v1เก็บไว้ไม่แก้; ยังไม่ทดสอบในเกม

## 04 — ผู้ดูแลประตูเวทมนตร์

![ภาพอ้างอิงผู้ดูแลประตู](assets/references/npc-portal-mage.png)
![โมเดล pilot ผู้ดูแลประตู](assets/models/npc_portal_mage-preview.png)

ไฟล์ [npc_portal_mage.bbmodel](assets/models/npc_portal_mage.bbmodel) · 36 cubes · `idle`, `greet`, `cast`
เพิ่มขอบ hood, rune stitching และคทาคริสตัล ท่าร่ายเป็นภาพประกอบ ไม่ใช้ frame animation ตัดสินการวาร์ป
วางข้างจุดเลือกปลายทาง หันเข้าพื้นที่อ่านระดับและเงื่อนไข ห้ามบังหน้าประตูหรือ warp pad

## 05 — Aether Halo

![ภาพอ้างอิง Halo](assets/references/item-aether-halo.png)
![โมเดล pilot Halo](assets/models/item_aether_halo-preview.png)

ไฟล์ [item_aether_halo.bbmodel](assets/models/item_aether_halo.bbmodel) และ [Java model draft](assets/models/item_aether_halo.json)
12 cubes ไม่มี bone animation ของ NPC ใน Java item JSON
รุ่นตกแต่งบนเว็บไม่มีโบนัส combat; หมุน/เรืองแสงต้องกำหนดวิธีแสดงและ animated texture แยกตาม runtime

## 06 — Runeblade

![ภาพอ้างอิง Runeblade](assets/references/item-runeblade.png)
![โมเดล pilot Runeblade](assets/models/item_runeblade-preview.png)

ไฟล์ [item_runeblade.bbmodel](assets/models/item_runeblade.bbmodel) และ [Java model draft](assets/models/item_runeblade.json)
6 cubes ต้องเพิ่มรูปทรงใบดาบ/ปลาย/guard และ UV เทียบ reference รุ่นร้านค้าเป็น skin ไม่มีค่าโจมตีเพิ่ม

## การส่งต่อและตรวจงาน

- [คำสั่งส่งต่อ Antigravity/Blockbench](ANTIGRAVITY-BLOCKBENCH-HANDOFF-th.md) — ลำดับ polish, output v2 และเงื่อนไขตรวจ
- [แผน model pipeline และคลัง 140 asset/variant](MODEL-AND-CONTENT-PLAN-th.md) — จำนวนนี้เป็นแผน ไม่ใช่โมเดลที่สร้างครบแล้ว
- [manifest](assets/models/manifest.json) — ทุก pilot มี `runtime_tested: false`
- [พรอมป์ต์ภาพอ้างอิง](assets/references/PROMPTS.md)

ต้อง compile pack ให้ namespace ถูกต้อง, ตรวจ display transform/UV และทดสอบ ModelEngine จริง
Java JSON draft ยังเป็น profile export ของ Blockbench รุ่นปัจจุบัน ไม่รับรองโหลดตรงบน 1.16.5
SQL/ระบบบริการอยู่ที่ FantasyCore ไม่ผูกเงินหรือผลธุรกรรมกับ animation frame

## 07 — ผู้พิทักษ์จันทร์แตก (บอสลงสีละเอียด)

![ภาพอ้างอิงบอส4มุม](assets/references/boss-moonfall-guardian-turnaround.png)
![โมเดลบอสจริงจากBlockbench](assets/models/boss_moonfall_guardian_v1-preview.png)

[ไฟล์ .bbmodel](assets/models/boss_moonfall_guardian_v1.bbmodel) · [texture128×128](assets/models/boss_moonfall_guardian_v1.png) · [คู่มือและanimation](../../server/content/dungeons/moonfall/README-th.md)
146cubes/19bones/8animations/150keys: idle, walk, spawn, attack, slam, enrage, hurt, death
มีหินแตกร้าว ขอบทองแดงหลายเฉด หมุด นิ้ว/ข้อต่อ รูนฟ้า แกนม่วงซ้อนชั้น และตราจันทร์ด้านหลัง
ภาพviewport29เฟรม/UUID/UV/loop/hashตรวจผ่าน; ยังไม่มีModelEnginepackหรือผลMinecraftจริง CoreยังHuskfallback
บอสชิ้นนี้สร้างเพิ่ม ไม่ทับNPC/itempilot6ตัวเดิม; มาตรฐานสีและรายละเอียดสำหรับrevisionNPCv2อยู่ใน [แผนโมเดล](MODEL-AND-CONTENT-PLAN-th.md)
