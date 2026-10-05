# ภาพอ้างอิงและโมเดล pilot ของ Luma

ภาพอ้างอิงสร้างด้วย ChatGPT image generation โมเดลสร้างและ export ผ่าน Blockbench MCP ที่เชื่อมในเครื่องจริง
**ภาพอ้างอิงละเอียดกว่า geometry ของ pilot** ต้องเก็บ silhouette/UV/รายละเอียดเพิ่มก่อน production
ไฟล์ `.bbmodel` เป็นไฟล์แก้ไขได้ ไม่ใช่ plugin หรือ resource pack ที่ติดตั้งพร้อมเล่น

## NPC v2 ทั้ง 4 ตัว (texture bake ใหม่ 6 ต.ค.2026)

![NPC v2 ทั้ง 4 ตัว](assets/models/npcs-v2-overview.png)

NPC เพิ่มจากชุดนี้: [ไลรา นักปรุงยา](../../server/content/npc-models/LYRA-ALCHEMIST-th.md) — ภาพอ้างอิง/โมเดลลงสี/5ท่า และตรวจ timeline ทุกเฟรม

texture ทุกตัว bake ทีละหน้า 4 texel/หน่วย (64px ต่อบล็อก ไม่ยืด) ด้วย `tools/npc_bake.py` + `tools/texture_bake.py`:
ผ้าทอมีรอยพับบนชายเสื้อยาว ผม/เครามีเส้น ขนสัตว์เป็นปุย ผิว4โทนพร้อมเงาแขนกลม ใบหน้าวาดตามขนาดหัว(ตา ม่านตา ประกายตา ริ้วรอย/แก้มแดง)
ลายปัก/ปกสมุด/เหรียญปั๊ม/ตรากุญแจ/รูนผ้ากันเปื้อน/ลายเสื้อโค้ท/ผ้าห้อยและตราหลังของจอมเวทวาดตามขนาดหน้าจริง; แสงไล่ตามความสูงทั้งตัว
สร้างใหม่ครบวงจรได้ด้วย `python tools/rebuild_npc.py banker|smith|warden|mage`

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

[ไฟล์v2](assets/models/npc_arcane_banker_v2.bbmodel) · [texture512×512](assets/models/npc_arcane_banker_v2.png) · [manifest](assets/models/npc_arcane_banker_v2-manifest.json)
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

[ไฟล์v2](assets/models/npc_rune_smith_v2.bbmodel) · [texture512×512](assets/models/npc_rune_smith_v2.png) · [manifest](assets/models/npc_rune_smith_v2-manifest.json)
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

[ไฟล์v2](assets/models/npc_quest_warden_v2.bbmodel) · [texture512×512](assets/models/npc_quest_warden_v2.png) · [manifest](assets/models/npc_quest_warden_v2-manifest.json)
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

### v2 ลงรายละเอียดเต็ม (6 ต.ค.2026)

![โมเดลผู้ดูแลประตูv2](assets/models/npc_portal_mage_v2-preview.png)
![v2ด้านหน้า](assets/models/npc_portal_mage_v2-front.png)
![v2ร่ายเวท](assets/models/npc_portal_mage_v2-cast.png)

[ไฟล์v2](assets/models/npc_portal_mage_v2.bbmodel) · [texture512×512](assets/models/npc_portal_mage_v2.png) · [manifest](assets/models/npc_portal_mage_v2-manifest.json)
124cubes/18bones/4animations/96keys: idle, greet, cast, open_portal; สูง2.3บล็อก (ยอดฮู้ด)
ฮู้ดแหลมเป็นbone `hood` ลูกของ `hi_head` ขอบทองรอบหน้า อัญมณีหน้าผาก แถบทองหลังฮู้ดถึงยอด; เคราขาวยาว3ชั้น หนวด คิ้วดก ผมขาวข้างขมับ
เสื้อคลุมชั้นนอกน้ำเงินเขียวขอบทองเปิดหน้าเห็นชุดชั้นในงาช้าง ผ้าคลุมไหล่ขอบทอง อัญมณีบนไหล่ แขนเสื้อบานงาช้างพร้อมข้อมือน้ำเงินคาดทอง
เข็มขัดดำหัวทองฝังคริสตัล ผ้าห้อยหน้าลายทอง ชายเสื้อแยกซ้ายขวามีขอบทองและอัญมณีด้านหลัง รองเท้าดำหัวทอง
คทาอยู่ใน `staff_socket` ใต้มือขวา ยืนหน้าแขน(ไม่ทะลุแขนเสื้อ) กำปั้นจับด้าม หัวคทากรงเพชรทองกับคริสตัลลอย(`staff_crystal`)ขยับ/หมุน
ตรวจ17เฟรม: คทาติดมือทุกเฟรม ไม่ชนหัว/ฮู้ด(ตรวจจุดบนชิ้นจริงที่หมุนแล้ว) ทุกท่าสูงสุด2.66บล็อก ต่ำกว่าเพดาน2.8ตามticket; `python tools/verify_portal_mage.py` ผ่าน; ยังไม่ทดสอบในเกม

## 05 — Aether Halo

![ภาพอ้างอิง Halo](assets/references/item-aether-halo.png)
![โมเดล pilot Halo](assets/models/item_aether_halo-preview.png)

ไฟล์ [item_aether_halo.bbmodel](assets/models/item_aether_halo.bbmodel) และ [Java model draft](assets/models/item_aether_halo.json)
12 cubes ไม่มี bone animation ของ NPC ใน Java item JSON
รุ่นตกแต่งบนเว็บไม่มีโบนัส combat; หมุน/เรืองแสงต้องกำหนดวิธีแสดงและ animated texture แยกตาม runtime

### v2 ลงรายละเอียดเต็ม (6 ต.ค.2026)

![Halo v2](assets/models/item_aether_halo_v2-preview.png)
![Halo v2 ทุก display context](assets/models/item_aether_halo_v2-display.png)

[ไฟล์v2](assets/models/item_aether_halo_v2.bbmodel) · [Java JSON](assets/models/item_aether_halo_v2.json) · [texture128×128](assets/models/item_aether_halo_v2.png) · [manifest](assets/models/item_aether_halo_v2-manifest.json)
35 elements: แท่งงาช้าง4ด้านคาดแถบน้ำเงินเขียวทั้งด้านนอก/ใน มุมทองมีขั้นบน อัญมณีบนยอดและด้านนอก2ด้าน แผ่นทองฝังอัญมณีด้านหน้า แผ่นเรียบด้านหลัง
texture bake ทีละหน้า 2 texel/หน่วย ไล่แสงตามความสูงทั้งชิ้น; ตั้งค่าครบ8 context: ลอยเหนือหัว(head) มือซ้าย/ขวาทั้งบุคคลที่1และ3, GUI, พื้น, กรอบไอเทม; texture อ้าง `luma:item/aether_halo_v2`
gate คำนวณขนาดไอคอน GUI จริงหลังแปลง (±7.94 หน่วย) ว่าไม่ล้นช่อง16×16; `python tools/verify_luma_items_v2.py` ผ่าน; ยังไม่โหลดใน resource pack จริง

### v3 แบบมงกุฎ (6 ต.ค.2026, ตามภาพตัวอย่างจากผู้ใช้)

![Halo v3 มงกุฎ](assets/models/item_aether_halo_v3-preview.png)
![Halo v3 ทุก display context](assets/models/item_aether_halo_v3-display.png)

[ไฟล์v3](assets/models/item_aether_halo_v3.bbmodel) · [Java JSON](assets/models/item_aether_halo_v3.json) · [texture256×256](assets/models/item_aether_halo_v3.png) · [manifest](assets/models/item_aether_halo_v3-manifest.json)
92 elements: แถบมงกุฎทอง3ชั้น(ขอบล่างเข้ม/แถบหลักมีเส้นไฮไลต์/ขอบบนอ่อน) ยอดแหลมแบบขั้นต่อกันเป็นฟันเลื่อยรอบวง
ยอดมุมสูงสุด ยอดกลางด้านสูงรอง ยอดเล็กคั่น; ทับทิมใหญ่กลางแถบทุกด้าน ทับทิมเล็กที่ฐานยอดเล็ก อัญมณีฟ้าใกล้ปลายยอดมุม/ยอดกลาง
texture bake ทีละหน้า 2 texel/หน่วย ไล่แสงตามความสูงทั้งชิ้น; สวมแล้วนั่งพอดีบนหัว(head) ไอคอน GUI ±7.84 ไม่ล้นช่อง กรอบไอเทม/มือ/พื้นตั้งค่าแล้ว; texture `luma:item/aether_halo_v3`
v2 (วงแหวนสี่เหลี่ยม) เก็บไว้ เลือกได้ว่าจะ map `luma:aether_halo` กับรุ่นไหนตอนทำ pack; ยังไม่โหลดใน resource pack จริง

## 06 — Runeblade

![ภาพอ้างอิง Runeblade](assets/references/item-runeblade.png)
![โมเดล pilot Runeblade](assets/models/item_runeblade-preview.png)

ไฟล์ [item_runeblade.bbmodel](assets/models/item_runeblade.bbmodel) และ [Java model draft](assets/models/item_runeblade.json)
6 cubes ต้องเพิ่มรูปทรงใบดาบ/ปลาย/guard และ UV เทียบ reference รุ่นร้านค้าเป็น skin ไม่มีค่าโจมตีเพิ่ม

### v2 ลงรายละเอียดเต็ม (6 ต.ค.2026)

![Runeblade v2](assets/models/item_runeblade_v2-preview.png)
![Runeblade v2 ทุก display context](assets/models/item_runeblade_v2-display.png)

[ไฟล์v2](assets/models/item_runeblade_v2.bbmodel) · [Java JSON](assets/models/item_runeblade_v2.json) · [texture128×128](assets/models/item_runeblade_v2.png) · [manifest](assets/models/item_runeblade_v2-manifest.json)
25 elements: ใบดาบขั้นฐานกว้าง-ลำยาว-ปลายแหลม4ขั้น ร่องรูนฟ้ามีลายทั้งสองหน้า คอใบดาบทองแดง
การ์ดทองฝังอัญมณีหน้า/หลัง ปีกมีปลายยกและขั้นล่าง ด้ามพันหนังลายเฉียงคั่นแหวนทอง ปุ่มท้ายทองฝังอัญมณี
texture bake ทีละหน้า 2 texel/หน่วย ไล่แสงตามความสูงทั้งชิ้น; display ครบ8 context: มือ3rd เอียงขึ้นแบบดาบvanilla ทั้งซ้าย/ขวา, 1st เห็นใบดาบ, GUI/กรอบไอเทมแนวทแยง (±7.86 ในช่อง), พื้นตั้งตรง
texture อ้าง `luma:item/runeblade_v2`; `python tools/verify_luma_items_v2.py` ผ่าน; ยังไม่โหลดใน resource pack จริง

## 08 — Props ประจำจุดบริการ 12 ชิ้น (6 ต.ค.2026)

![props ทั้ง 12 ชิ้น](assets/models/props-overview.png)

| ชิ้น | จุดใช้ | elements | รายละเอียด |
|---|---|---:|---|
| [prop_bank_ledger](assets/models/prop_bank_ledger.json) | ธนาคาร | 16 | ปกน้ำเงินเขียว หน้ากระดาษ สันหนังคาดทอง มุมทอง ตราอัญมณี ตะขอ ริบบิ้น |
| [prop_vault_key](assets/models/prop_vault_key.json) | ธนาคาร | 16 | ห่วงกุญแจทองฝังอัญมณี ปลอกทองแดง ฟันกุญแจ3ขั้น เส้นรูนฟ้าบนก้าน |
| [prop_coin_stack](assets/models/prop_coin_stack.json) | ธนาคาร | 36 | เหรียญกลมแบบพิกเซล3กองสูงไม่เท่ากัน เหรียญพิงฝังอัญมณี |
| [prop_forge_hammer](assets/models/prop_forge_hammer.json) | โรงตีเหล็ก | 11 | หัวเหล็กคาดทอง หน้าตีเข้ม รูนฟ้า ด้ามไม้พันหนัง |
| [prop_tongs](assets/models/prop_tongs.json) | โรงตีเหล็ก | 8 | ขาคีมไขว้ หมุดทอง ด้ามพันหนัง คีบแท่งรูน |
| [prop_quest_scroll](assets/models/prop_quest_scroll.json) | บอร์ดเควส | 11 | ม้วนแปดเหลี่ยม ฝาทอง ริบบิ้น ตราครั่งแดง |
| [prop_map_table](assets/models/prop_map_table.json) | บอร์ดเควส | 15 | โต๊ะไม้ แผนที่เอียง มีเส้นทาง/หมุด เข็มทิศ หมึกกับขนนก |
| [prop_mailbox](assets/models/prop_mailbox.json) | ไปรษณีย์ | 15 | ตู้น้ำเงินเขียวหลังคาขั้น กรอบประตูทอง จดหมายในช่อง ธงแดง |
| [prop_shop_sign](assets/models/prop_shop_sign.json) | ร้านค้า | 11 | เสา แขนเหล็ก โซ่ ป้ายกรอบทองลายเหรียญ |
| [prop_fish_crate](assets/models/prop_fish_crate.json) | ท่าเรือ | 26 | ลังไม้แผ่นเว้นช่อง น้ำแข็ง ปลา3ตัวมีหาง/ตา/ครีบ |
| [prop_portal_focus](assets/models/prop_portal_focus.json) | ประตูวาร์ป | 20 | ฐานหิน ถ้วยทอง กรง4แขน คริสตัลซ้อนชั้น วงโคจรทอง |
| [prop_rune_plinth](assets/models/prop_rune_plinth.json) | ประตูวาร์ป | 12 | แท่นหินแกะรูนเรืองแสงทุกด้าน ขอบทอง อัญมณีบนยอด |

texture แบบ bake ทีละหน้า 2 texel/หน่วย (32px ต่อบล็อก ไม่ยืด) ไล่เฉดอุ่น/เย็น ลายไม้/อิฐหิน/หนังเย็บ/เพชรเจียระไน/เหรียญปั๊ม/แผนที่/ป้าย/รูนวาดตามขนาดหน้า อ้าง `luma:item/prop_<ชื่อ>`; GUI/กรอบไอเทมคำนวณขนาดจากรูปทรงจริงให้พอดีช่อง (ทุกชิ้น ≤±7.4) วางบนหัวแล้วนั่งบนหัวพอดี
ไม่มีภาพ reference เฉพาะ props จึงใช้โทนสีชุด NPC v2; `python tools/build_luma_props.py [ชื่อ...]` สร้างใหม่, `python tools/verify_luma_items_v2.py` ตรวจรวมกับไอเทม; ยังไม่โหลดใน resource pack จริง

## 09 — พ่อค้าตลาด (NPC บริการตัวที่ 5, 6 ต.ค.2026)

![พ่อค้าตลาด](assets/models/npc_market_merchant-preview.png)
![ด้านหน้า](assets/models/npc_market_merchant-front.png)
![ด้านหลัง](assets/models/npc_market_merchant-back.png)
![ยกหมวกทักทาย](assets/models/npc_market_merchant-greet-hat-tip.png)
![ชั่งของ](assets/models/npc_market_merchant-weigh-goods.png)

[ไฟล์](assets/models/npc_market_merchant.bbmodel) · [texture512×512](assets/models/npc_market_merchant.png) · [manifest](assets/models/npc_market_merchant-manifest.json)
179cubes/20bones/5animations/156keys: idle, greet (ยกหมวก), show_wares (ยื่นมือเสนอสินค้า), weigh_goods (ยกตาชั่งมาหมุนขวางตัว คานโยกแล้วนิ่ง), sale_success (กระโดดดีใจ กางแขน หมวกเด้ง); สูงพร้อมหมวก 2.2บล็อก
พ่อค้าเร่: เสื้อกั๊กแดงไวน์ลายดามัสก์ ขอบทอง กระดุมทอง เสื้อเชิ้ตพับแขนพร้อมสายรัดแขน ปลอกแขนหนังหมุดทอง ผ้าพันคอเขียวหยกชายพู่ทอง
หมวกสักหลาดปีกกว้าง แถบแดงไวน์ หัวเข็มขัดทอง ขนนกเขียวหยกปัดไปด้านหลัง; หนวดงอนปลาย เคราแพะ คิ้วหนา ตาสีอำพัน
ด้านหลังเป็นเป้โครงไม้ ฝาเป้ปักตราตาชั่งในวงทอง พรมม้วนลายแถบทอง/เขียวหยก ตะเกียงแก้วเรืองแสง กระทะทองแดง กระบอกม้วนกระดาษ
เอวมีถุงเงิน (สะโพกขวา) และขวดยา3ขวด (สะโพกซ้าย); มือซ้ายถือตาชั่งแบบแขวนที่มีกระดูกแยก ทำให้จานแกว่งสวนคานได้
ตรวจ26เฟรม: ตาชั่งติดกำปั้นซ้ายทุกเฟรม (gap 0) จานไม่เอียงเกินกำหนด มือขวาแตะหมวกตอนทักทายจริง; ท่ายกหมวกหามุมแขนด้วยการค้นในBlockbenchให้แขนไม่บังหน้า
`python tools/verify_market_merchant.py` ผ่าน, สร้างใหม่ทั้งชุดได้ด้วย `python tools/rebuild_npc.py merchant`
ใช้ได้กับร้าน A–D โซน06 (หันหน้าตาม station yaw); ยังไม่มีภาพอ้างอิงจาก ChatGPT และยังไม่ทดสอบในเกม; เกินbudgetNPCเมืองจึงเหมาะร้านละหนึ่งตัว

## 10 — ผู้นำทางลานคริสตัล (NPC บริการตัวที่ 7, 6 ต.ค.2026)

![ผู้นำทาง](assets/models/npc_crystal_guide-preview.png)
![ด้านหน้า](assets/models/npc_crystal_guide-front.png)
![ด้านหลัง](assets/models/npc_crystal_guide-back.png)
![โบกมือทักทาย](assets/models/npc_crystal_guide-greet.png)
![ชี้ทาง](assets/models/npc_crystal_guide-point-direction.png)
![กางแผนที่](assets/models/npc_crystal_guide-show-map.png)
![ต้อนรับ](assets/models/npc_crystal_guide-welcome.png)

[ไฟล์](assets/models/npc_crystal_guide.bbmodel) · [texture512×512](assets/models/npc_crystal_guide.png) · [manifest](assets/models/npc_crystal_guide-manifest.json)
149cubes/18bones/5animations/110keys: idle, greet (โบกแผนที่เหนือหัว), point_direction (ชี้ทางด้วยแผนที่ หันหน้าตาม), show_map (ยกแผนที่ขึ้นหน้าอกแล้วกางออก ก้มมอง), welcome (กางแขนต้อนรับ ไม้เท้ายกออกข้าง คริสตัลหมุน); ยืนสูงพร้อมไม้เท้า 2.14บล็อก สูงสุดทุกท่า 2.44บล็อก (ตอนโบกแผนที่) กว้าง 2.02
ใช้กับ spawn_guide โซน01 (navigation.main) แทน quest_warden variant; สีชุดอิงธงลานคริสตัล: เสื้อคลุมน้ำเงินหลวงคาดผ้าทาบอกลายกากบาทขาว กลางกากบาทเป็นคริสตัลฟ้า
ชุด: เสื้อตัวในสีงาช้าง ผ้าคลุมไหล่ขอบทอง เข็มกลัดคริสตัลคู่ที่คอ ผ้าคลุมหลังลายเข็มทิศ (แฉกงาช้าง วงทอง คริสตัลกลาง) ฮู้ดพับที่ต้นคอมีพู่ทอง
อุปกรณ์: มือขวาถือไม้เท้าครอบทองเหลืองใส่คริสตัลฟ้าเรืองแสง (มีเศษคริสตัลม่วง), มือซ้ายถือแผนที่พับวาดชายฝั่ง ป่า ภูเขา เส้นทางจุดแดง และจุดคริสตัล; เอวมีเข็มทิศห้อย ถุงใส่แผนที่ และกระบอกม้วนกระดาษ
หน้าตา: ผมน้ำตาลแดงยุ่ง ๆ มีปอยชี้ คิ้ว ตาฟ้า กระบนจมูก หูมีคริสตัลเม็ดเล็ก
ตรวจ269เฟรม (ทุก tick ที่20FPS ทั้ง5ท่า): ไม้เท้าและแผนที่ติดกำปั้นทุกเฟรม (gap 0); ทดสอบ27จุดต่อชิ้นในกรอบของทุกชิ้น ไม้เท้าไม่ทะลุหัว/ตัว และแผนที่ไม่ทะลุตัว/แขนเลยสักจุด; ไม่จมพื้น สูงไม่เกิน40px
ปัญหาที่เจอแล้วแก้: ไม้เท้าทะลุผ้าคลุมไหล่/ปลอกแขน → เลื่อนมาข้างหน้า0.85และทำกำปั้นลึกขึ้น; แผนที่นอนแบนและพับเข้าแขน → เปลี่ยนจุดจับเป็นมุมแผนที่ บานพับไปไว้ขอบไกล แล้วค้นมุมแขนใน Blockbench ให้แผนที่ตั้งตรงหันหาตาและไม่ชนทุกเฟรม; ท่าต้อนรับเดิมไม้เท้าบังหน้า → เปลี่ยนเป็นยกออกข้าง
`python tools/verify_crystal_guide.py` ผ่าน, สร้างใหม่ทั้งชุดได้ด้วย `python tools/rebuild_npc.py guide`; ยังไม่มีภาพอ้างอิงจาก ChatGPT และยังไม่ทดสอบในเกม; เกินbudgetNPCเมืองแต่มีตัวเดียวที่จุดเกิด

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

[ไฟล์ .bbmodel](assets/models/boss_moonfall_guardian_v1.bbmodel) · [texture1024×1024](assets/models/boss_moonfall_guardian_v1.png) · [คู่มือและanimation](../../server/content/dungeons/moonfall/README-th.md)
146cubes/19bones/8animations/150keys: idle, walk, spawn, attack, slam, enrage, hurt, death
มีหินแตกร้าว ขอบทองแดงหลายเฉด หมุด นิ้ว/ข้อต่อ รูนฟ้า แกนม่วงซ้อนชั้น และตราจันทร์ด้านหลัง
texture bake ใหม่ 6 ต.ค. (4 texel/หน่วย, atlas 1024): หินก้อนใหญ่สลักมีรอยร้าว เกราะทองแดงเงามันเส้นเดียว รูนฟ้าเรืองแสง คริสตัลม่วงเจียระไน แผ่นหลังรูปจันทร์เสี้ยว แสงไล่ตามความสูงทั้งตัว
ภาพviewport29เฟรม/UUID/UV/loop/hashตรวจผ่าน; ยังไม่มีModelEnginepackหรือผลMinecraftจริง CoreยังHuskfallback
บอสชิ้นนี้สร้างเพิ่ม ไม่ทับNPC/itempilot6ตัวเดิม; มาตรฐานสีและรายละเอียดสำหรับrevisionNPCv2อยู่ใน [แผนโมเดล](MODEL-AND-CONTENT-PLAN-th.md)

## 08 — ไลรา นักปรุงยา (6 ต.ค.2026)

![ภาพอ้างอิงไลรา](assets/references/npc-lyra-alchemist-turnaround.png)
![โมเดลไลราจาก Blockbench](assets/models/npc_lyra_alchemist-preview.png)

[ต้นฉบับ](assets/models/npc_lyra_alchemist.bbmodel) · [texture512](assets/models/npc_lyra_alchemist.png) · [คู่มือร้านยา/ทุกท่า](../../server/content/npc-models/LYRA-ALCHEMIST-th.md)
206cubes/21bones/5animations/75keys: idle, greet, stir, offer_potion, brew_success
มีเสื้อเขียวลายพฤกษาทอง ผ้ากันเปื้อนม่วง ผมถัก แว่นกรอบซ้อน นิ้วจับคอขวด ขวด3สี และหม้อกลวงพร้อมเตา
UVแยก1,236หน้าไม่ซ้อน ตรวจnativeครบ265เฟรมที่20FPS: gripติดมือ ปลายไม้อยู่ในน้ำยา เตานิ่ง เท้าอยู่พื้น
วางShopDหน้าSouthตามyaw0; sourceและmanifestผ่าน; ร้านยามี [Core v0.9](../../fantasycore/ALCHEMY-th.md) แล้ว แต่ยังรอเกมจริง/renderer/pack และ animation event bridge
