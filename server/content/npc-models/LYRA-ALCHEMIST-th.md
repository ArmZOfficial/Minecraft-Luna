# ไลรา นักปรุงยา — โมเดลและใบงานร้านยาโซน 06

ผลิตวันที่ 6 ต.ค. 2026 ผ่าน Blockbench MCP ในเครื่อง: **206 cubes / 21 bones / texture 512×512 / 5 animations / 75 keyframes**
มีภาพอ้างอิงจาก ChatGPT แล้วขึ้น geometry, rig และ animation จริง; ไฟล์นี้ยังเป็นงานต้นฉบับสำหรับนำเข้า renderer
**ระบบปรุงยามี source/JAR ใน FantasyCore v0.9 แล้ว; ยังไม่มี ModelEngine pack หรือผลทดสอบ Minecraft จริง** รายละเอียดบริการอยู่ใน [contract ที่ปิดไว้](lyra-model-contract.json)

![ภาพอ้างอิง](../../../output/lobby-concept/assets/references/npc-lyra-alchemist-turnaround.png)
![โมเดลจริงจาก Blockbench](../../../output/lobby-concept/assets/models/npc_lyra_alchemist-preview.png)

## ไฟล์ที่ส่ง

- [ต้นฉบับ .bbmodel](../../../output/lobby-concept/assets/models/npc_lyra_alchemist.bbmodel) — Generic, format 5.0, embedded texture และ keyframes
- [texture PNG](../../../output/lobby-concept/assets/models/npc_lyra_alchemist.png) — ผิว 1,236 หน้า แยก UV ที่ความหนาแน่น 4 texel/หน่วย มีขอบ padding 1px
- [ด้านหน้า](../../../output/lobby-concept/assets/models/npc_lyra_alchemist-front.png) · [ข้างขวาตัวละคร](../../../output/lobby-concept/assets/models/npc_lyra_alchemist-right.png) · [ด้านหลัง](../../../output/lobby-concept/assets/models/npc_lyra_alchemist-back.png)
- [manifest และ SHA-256](../../../output/lobby-concept/assets/models/npc_lyra_alchemist-manifest.json) · [ข้อมูลตรวจ 265 เฟรม](../../../output/lobby-concept/assets/models/npc_lyra_alchemist-pose-checks.json)

## รายละเอียดที่ขึ้นโมเดลแล้ว

| ส่วน | รูปทรง | สีและ UV |
|---|---|---|
| ใบหน้า | จมูกแยก ผมหน้าม้าทแยง หูเอลฟ์หลายขั้น ต่างหู และผมถักแยก bone | ดวงตาม่วง/แสงสะท้อน ปาก แก้ม ผิวและเส้นผมแยกวัสดุ |
| แว่น | กรอบทองโปร่ง 4 ด้าน เลนส์ม่วงฝังด้านใน บานพับและสายหนังรอบหัว | ขอบโลหะมีเงาและไฮไลต์; อยู่บนหน้าผาก ไม่ปิดดวงตา |
| เสื้อ | ปกพับแยก ขอบทอง เสื้อในสีงาช้าง กระดุม สายผ้ากันเปื้อนและเข็มกลัด | ผ้าทอเขียวหม่น ลายพฤกษาทองหน้า/หลัง ผ้ากันเปื้อนม่วง |
| แขนและมือ | ปลอกแขน ขอบข้อมือ ตัวล็อก ถุงมือ นิ้ว 4 นิ้วกับนิ้วโป้ง | ผิวปลายนิ้วและหนังไม่ใช้สีเดียวกันทั้งมือ |
| เข็มขัด | หัวเข็มขัดเป็นกรอบ กระเป๋าสมุนไพร ชั้นขวด 3 ช่อง | ขวดแดง/ฟ้า/ม่วงมีน้ำยา คอขวด จุก และฉลากของตัวเอง |
| ชายเสื้อ/รองเท้า | ชายหน้า/หลังแยก bone มีผ้าซับสีงาช้าง รองเท้ามีพื้น ปลาย สายรัดและหัวเข็มขัด | ลายผ้าต่อในแต่ละหน้า ไม่ยืด swatch ก้อนเดียวทั้งตัว |
| อุปกรณ์ | ขวดถือมือซ้าย ไม้คนถือมือขวา หม้อกลวงมีผนัง ขอบ หูจับ ขาและเตาหิน | น้ำยาม่วง วัสดุแก้วสี ทองเหลือง หิน และเปลวสีฟ้าที่เป็น geometry |

สีแสงใน texture เป็นภาพตกแต่ง ยังไม่ได้ทำ emissive pack หรือไฟที่ส่องโลกจริง
ขวดยาและเตาในโมเดลไม่ใช่ item ที่หยิบได้ และไม่ให้โบนัส/ฮีล/เงินจากการเล่น animation

## Rig และท่าขยับ

โมเดลหัน North/−Z เท้า Y=0 ที่ 16 หน่วยต่อบล็อก; ขวามือตัวละครคือ +X ซ้ายคือ −X
`body → upper_arm → forearm → hand` ทั้งสองข้าง; `flask → left_hand`, `stirring_rod → right_hand`
แว่นกับผมถักอยู่ใต้ `hi_head`; ชายเสื้ออยู่ใต้ `waist`; ขาติด `motion_root` จึงไม่ลอยเมื่อหายใจ
`brew_station` ติด root และไม่มี keyframes; `brew_liquid` ขยับได้เล็กน้อยภายในหม้อ
`hitbox` อยู่ root แยก มี cube ที่ซ่อนใน viewport และไม่มี animation

| Clip | ระยะ/รูปแบบ | ท่าที่ทำ | ใช้งานที่เสนอ |
|---|---|---|---|
| idle | 4s loop | หายใจ หันศีรษะเบา ๆ ผมถักแกว่ง | ระหว่างรอผู้เล่น |
| greet | 1.8s once | พยักหน้าและยกขวดยาทักทาย | เปิด preview สนทนา; จำกัดการเรียกซ้ำ |
| stir | 3s once | มือขวาวนไม้คนอยู่ในน้ำยา | แสดงการปรุง; ไม่มีการหักวัตถุดิบจากเฟรม |
| offer_potion | 2.4s once | ยื่นขวดให้ดูโดยถือคอขวด | ดูสินค้าก่อนยืนยัน |
| brew_success | 1.8s once | ยกขวดฉลองและน้ำยาเคลื่อนเล็กน้อย | หลัง Core บันทึก receipt สำเร็จเท่านั้น |

ทุก clip ตั้ง 20 FPS และคืน pose เริ่มต้นเมื่อจบ; idle วนโดยค่าต้น/ปลายตรงกัน
ภาพ [ทักทาย](../../../output/lobby-concept/assets/models/npc_lyra_alchemist-greet.png) · [คนยา](../../../output/lobby-concept/assets/models/npc_lyra_alchemist-stir.png) · [ยื่นยา](../../../output/lobby-concept/assets/models/npc_lyra_alchemist-offer-potion.png) · [สำเร็จ](../../../output/lobby-concept/assets/models/npc_lyra_alchemist-brew-success.png)

## วางในแมพ

ใช้ Shop D ของ [ใบงานโซนตลาด](../../../output/lobby-concept/ZONE-BUILD-TICKETS-th.md): อาคาร 11×13 หน้า South, origin (−75,78)
NPC local u=5.5/v=9.5 → **X−69.5/Z68.5**, เท้าบนพื้นเต็ม Y0+1; เช่นพื้นบล็อก Y80 ใช้เท้า Y81
จุดลูกค้า **X−69.5/Z72.5**, body **yaw 0°** หัน South; ป้าย “ร้านยา · ไลรา นักปรุงยา”
เสนอ station ID `market_alchemist_d` หนึ่งตัวต่อร้าน ไม่ spawn ซ้ำทุก reload

1. กันพื้นที่รอบ anchor 3×3 บล็อก และช่องว่างสูง 3 บล็อกก่อนติดตั้งโมเดล
2. เตาอยู่ทางขวาตัวละคร เป็นส่วนของ source เดียวกัน: เมื่อหมุน authored North ให้หน้า South คาดว่า center เตาอยู่ X−69.8875/Z68.6125
3. ใช้พิกัดนี้เป็นข้อเสนอ; calibrate ด้วย debug arrow และ renderer จริง ห้ามบวก yaw offset 180° ซ้ำหาก provider จัดการให้แล้ว
4. เว้นบริเวณเตาเป็นพื้นเปล่า ไม่ใส่ cauldron block ซ้อน ไม่วาง counter ผ่านเตา/มือ/ขวด; เคาน์เตอร์ลูกค้าอยู่ด้านหน้า เว้นช่องโชว์มือ
5. วางชั้นยาและสมุนไพรหลัง NPC/ชิดผนัง ตู้วัตถุดิบเป็นฉาก; กระบวนการปรุงใช้ journal/ฐานข้อมูลของ Core
6. แยกเครื่องปรุง/ชั้นหนังสือออกจากซอยหลักกว้าง 5 บล็อก และรักษาทางเดินในร้านอย่างน้อย 3 บล็อก
7. บริการ NPC ไม่เดินหรือเดินตามผู้เล่น เพราะเตาติด root; head tracking จำกัดมุมและพักขณะเล่น clip จน blend ผ่าน QA

hitbox ที่เสนอ 0.5875×0.5875 บล็อก สูง 2.075 บล็อก eye height 1.775 บล็อก **ไม่ครอบเตา**
WorldGuard/region ของร้านต้องคุ้มครองตำแหน่งเตาและปิด PvP/ความเสียหาย; ยังไม่ยืนยัน click/collision ของ renderer
ขอบเขตท่าที่วัดจริงและค่ามากสุดเป็นบล็อกอยู่ใน manifest ต้องทดสอบกับโต๊ะ ประตู หลังคา และ label อีกครั้ง

## เชื่อมกับระบบเซิร์ฟ

FantasyCore v0.9 มี `alchemy.main` และ `/alchemy` แล้ว: สามสูตร native potion → preview → ตรวจวัตถุดิบ/เงิน → shared CRAFT journal → ผลเข้า `/mail`
ดู [คู่มือร้านยา/สูตร/ราคา/วาง NPC/อัปเกรด](../../../fantasycore/ALCHEMY-th.md); `market.main` ยังเป็น action planned และไม่ใช่ร้านยา
ผูก Core fallback หรือ Citizens กับ `alchemy.main` หนึ่งตัวต่อสถานี; ไม่มี animation event bridge จึงยังไม่เล่นท่าตามธุรกรรมในเกม
ราคากับ item ID ต้องใช้ catalog/adapter เดิม ไม่สร้าง stat เพิ่มจากสีขวดหรือจากฝั่ง renderer
กดซ้ำ ปิด GUI กระเป๋าเต็ม หลุดเกม และ restart ต้องตรวจตาม checklist T; สถานะบริการเป็นคนละส่วนกับการเล่น clip
renderer โหลดไม่ได้หรือผู้เล่นปฏิเสธ pack ให้ใช้ NPC/ป้าย vanilla และเปิดบริการที่ผ่าน QA แล้วต่อได้
contract ชุดนี้ยัง `enabled:false` และไม่ได้เปลี่ยนค่าใน FantasyCore หรือ spawn NPC ลงโลกจริง

## ตรวจแล้วและงานก่อนเปิดใช้

ตรวจ native timeline **265 เฟรม** ที่ระยะ 0.05s รวมต้น/ปลายของทุก clip:
คอขวดติดมือซ้าย ด้ามไม้ติดมือขวา ผมถักต่อศีรษะ ปลายไม้อยู่ในขอบเขตน้ำยา เท้าอยู่พื้น และกรอบเตาไม่เคลื่อน
ใช้ world AABB เป็นตัวตรวจระยะและดูภาพทุกมุมประกอบ; ไม่ใช่หลักฐาน collision แบบ oriented หรือคำรับรอง FPS ในเกม
offline gate ตรวจ UUID/parent/source geometry/มุม cube/UV ไม่ซ้อน/atlas ทุก pixel/loop/75 keys/265 frames และ hash
Auto UV ปิดสำหรับไฟล์นี้เพื่อรักษางานวาด หากแก้สัดส่วนให้ rebake UV และตรวจอีกครั้ง

```powershell
python tools/build_alchemist.py --dry-run
python tools/verify_alchemist.py
python tools/verify_luma_artifacts.py
```

builder จริงต้องใช้ empty project ชื่อ `npc_lyra_alchemist` และ UUID ที่อ่านจาก MCP; ปฏิเสธ output ที่มีอยู่แล้ว
source revision ต่อไปใช้ชื่อใหม่ ไม่ทับไฟล์เผยแพร่; preview ต้องเลือก UUID และชื่อถูกต้องทุกครั้ง

- [ ] importer ของ ModelEngine JAR ที่เลือกอ่าน Generic `.bbmodel` format 5.0 และ texture ถูกต้อง
- [ ] pack legacy 1.16.5/รุ่นกลาง/modern ที่ประกาศรองรับผ่านจริงพร้อม pack hash; decline pack มี fallback
- [ ] body South, จ้องผู้เล่น, หม้อไม่ฝังพื้น, มือไม่ชน counter/ขวดไม่ทะลุข้อมือใน renderer จริง
- [ ] click bounds, region เตา, animation cooldown/priority/cancel และ dedup หลัง unload/reload/restart
- [ ] profile client frame pacing และ server MSPT; เริ่มเพียงหนึ่งตัวใน Shop D ก่อนเพิ่มสำเนา
- [ ] ทดสอบร้านยา source v0.9 ตาม checklist T และทำ animation event bridge หลัง receipt; ตลาด/การซื้อด้วยเงินจริงยังเป็นงานแยก

หลัก Generic/North/16units/มุม cube/parenting อ้างอิง [ModelEngine Creating a Model](https://wiki.mythiccraft.io/modelengine/Modeling/Creating-a-Model)
และข้อกำหนด loop/timeline อ้างอิง [Animating a Model](https://wiki.mythiccraft.io/modelengine/Modeling/Animating-a-Model); exact runtime ยังต้องทดสอบตาม checklist
