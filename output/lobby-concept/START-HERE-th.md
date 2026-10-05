# เริ่มอ่านที่นี่ — Fantasy MMO RPG Lobby

ฉบับล่าสุด 5 ตุลาคม 2026: **Luma / ลูม่า** เมืองขนาด 400 × 400 บล็อกสำหรับ fantasy และ casual roleplay
มีภาพ 12 โซน แผนภายใน เว็บต้นแบบ โลโก้ และโมเดล pilot ที่ export ผ่าน Blockbench MCP

## เอกสารหลัก

1. [ภาพประกอบ 12 โซน](ZONE-ILLUSTRATIONS-th.md) — ภาพคอนเซปต์เห็นเคาน์เตอร์ NPC และการตกแต่ง
2. [ผังแมพและแปลนภายใน](INTERIOR-AND-MAP-PLAN-th.md) — พิกัดโซน ขนาดอาคาร ตารางเฟอร์นิเจอร์ local และ service action
3. [ระบบและสเปกปลั๊กอิน](SERVER-SYSTEMS-PLAN-th.md) — เทียบคลิป ชุดปลั๊กอิน FantasyCore ฐานข้อมูล ธุรกรรมและแผนทดสอบ
4. [ผลวิจัยจากแหล่งผู้พัฒนา](PLUGIN-RESEARCH-SOURCES-th.md) — ลิงก์ทางการ ตัวเลือก และข้อจำกัด
5. [คู่มือตกแต่งโครงสร้าง](LOBBY-DECORATION-COMPACT-th.md) — รูปทรงคริสตัล หอเวท ถนน และชุดวัสดุ
6. [ใบงานสร้าง 12 โซน](ZONE-BUILD-TICKETS-th.md) — กำกับแต่ละภาพ พิกัดจุดบริการ ทิศ NPC โมเดลและเกณฑ์ตรวจบล็อก
7. [แกลเลอรีภาพและโมเดล](ASSET-GALLERY-th.md) — reference 6 ชุดและไฟล์ NPC/item pilot แก้ไขได้
8. [แผนคลังโมเดลและ modern item](MODEL-AND-CONTENT-PLAN-th.md) — rig, animation, asset budget, legacy/modern pack
9. [ส่งต่อ Antigravity / Blockbench](ANTIGRAVITY-BLOCKBENCH-HANDOFF-th.md) — การเชื่อมที่ทำแล้วและคำสั่ง polish รุ่นถัดไป
10. [TAB / Scoreboard / HUD / MOTD](HUD-TAB-SCOREBOARD-SPEC-th.md) — ล็อกองค์ประกอบตามภาพอ้างอิง พร้อมข้อจำกัดแต่ละ client
11. [FantasyAdminPanel](ADMIN-PANEL-SPEC-th.md) — เมนู คำสั่งสั้น สิทธิ์ และกระบวนการ preview/apply
12. [เว็บและ automation](WEB-AND-AUTOMATION-PLAN-th.md) — BlueMap, ชำระเงิน, ส่งของ, บัญชี และ recovery
13. [แบรนด์ โลโก้และภาพเว็บ](BRAND-GUIDE-th.md) — เขียวหยก/ทอง TAB concept และหน้าหลักเว็บรุ่นใหม่
14. [เปิดเว็บต้นแบบ](../../website/README-th.md) — วิธีรันและเมนูที่ใช้งานได้
15. [แหล่งข้อมูล modern modeling](MODERN-MODELING-SOURCES-th.md) — เอกสารทางการและ compatibility gates
16. [Next.js / React / Node / PostgreSQL](NEXT-POSTGRES-IMPLEMENTATION-th.md) — source เว็บปัจจุบัน animation ฐานข้อมูลและผลทดสอบ
17. [ชุดระบบตามภาพเพิ่มเติม](CASUAL-SURVIVAL-SYSTEMS-th.md) — RTP, daily, หินกันบ้าน, สกินไอเทม, เควสแลกของ, วาดรูป, อัปเกรดและตั้งบ้าน
18. [ProtectionStones 5 ระดับ](PROTECTIONSTONES-TIERS-th.md) — ขนาดจริง 11 × 11 ถึง 81 × 81 ความสูง ราคา สมาชิกและ recovery
19. [FantasyCore v0.8](../../fantasycore/README-th.md) — เงิน/ธนาคาร/ตาย/เมนู/NPC (+Citizens)/บ้าน/RTP/mail/daily/แลกของ/ซ่อม/คราฟต์/depth/ดันฝึก
20. [คู่มือเควสแลกของและ recovery](../../fantasycore/EXCHANGE-th.md) — 3 สูตร, batch, โควตา และการตัดสินรายการค้าง
21. [ItemAdapter และโรงตีเหล็ก](../../fantasycore/REPAIR-th.md) — ตรวจ serial/เจ้าของ, preview ราคา, คืนเงินและ recovery
22. [เซิร์ฟ staging + checklist](../../server/README-th.md) — Paper 26.2, Java 25, ปลั๊กอินที่ล็อกเวอร์ชัน และรายการทดสอบในเกม
23. [คลังโมเดลและไอคอนที่ให้มา](../../server/content/library/README-th.md) — ชื่อไทย 26 แพ็ก/493 รายการ, แปลนวาง และสำเนาแก้ config
24. [บาลานซ์และ Enchant](../../server/content/library/BALANCE-th.md) — ระดับพลัง, ทักษะประจำชุด 17 แบบ, ราคาเควส/สร้างฉบับออกแบบ
25. [PromptPay DEV](../../website/PROMPTPAY-th.md) — สั่งซื้อจำลองกับ PostgreSQL, การป้องกันรับซ้ำ และงานที่ต้องทำก่อนรับเงินจริง

## ข้อเลือกสำคัญ

- Java-first เพื่อความละเอียดและความเสถียร เป้าหมาย client ต่ำสุด 1.16.5
- backend ใช้ Paper รุ่นเดียวที่ยังรองรับและผ่านชุด plugin matrix; client เก่าเข้าโดย ViaVersion/ViaBackwards
- Bedrock เป็นงานขยายภายหลัง ไม่บังคับให้ภาพและระบบ Java ลดคุณภาพในรอบแรก
- ของรุ่นใหม่ใช้เป็นภาพเสริมได้ แต่ป้าย ทางเดิน เมนู และบล็อกหลักมี fallback ที่ client 1.16.5 ใช้ได้
- เงินสองชนิด ธนาคาร รางวัลบอส ซ่อมอาวุธ และตลาดต้องมีเจ้าของข้อมูลชัดเจน
- ระบบที่เห็นในคลิปเป็นชุดหลัก; ประมูล ร้านยา ตีบวก DPS dummy และชุดแฟนตาซีเพิ่มเติมเป็นตัวเลือก
- รูนเป็นพื้นที่อนาคตตามที่ผู้เล่ากล่าว ณ เวลาถ่าย
- แผน Protect ใหม่กำหนด 11/21/31/51/81 บล็อกต่อด้านตาม radius ของ ProtectionStones; ขนาด 10 × 10 เดิมเป็นตัวอย่างจากคลิป
- Core Home เป็นเจ้าของ home/sethome ในแผนชุดใหม่ ไม่เปิดเส้นทาง EssentialsX ที่ข้ามกติกาบ้าน

## สถานะงาน

เว็บต้นแบบทดลองข่าว ค้นหา 12 โซน คู่มือ กิจกรรม อันดับแบบรอข้อมูล และศูนย์บัญชีได้แล้ว
มีโลโก้ต้นฉบับ และ NPC pilot 4 ตัวที่มี idle/greet/ท่างาน พร้อม item pilot 2 ชิ้น
เชื่อม Blockbench MCP ในเครื่องและเพิ่ม config สำหรับ Antigravity แล้ว แต่ Antigravity ต้อง refresh server เพื่อโหลด config ใหม่

ฝั่งเซิร์ฟเริ่มลงมือแล้ว (5 ต.ค. 2026): backend ล็อกเป็น **Paper 26.2 build 129 + Java 25** (26.3 ยังเป็น experimental)
และเขียน FantasyCore ตามลำดับพัฒนาข้อ 1–3 + บ้าน/RTP (v0.1), กล่องจดหมาย/รับของรายวัน/ผูก Citizens (v0.2)
เควสแลกของ vanilla พร้อม journal (v0.3), ItemAdapter/ซ่อม (v0.4) และ [Core Craft](../../fantasycore/CRAFT-th.md) (v0.5), [Native enchant และแม่แบบย้อนหลัง](../../fantasycore/ENCHANTS-th.md) (v0.6)
และ [Moonfall ดันฝึกเดี่ยว](../../fantasycore/DUNGEONS-th.md) (v0.7; สร้างโครงโลกใหม่/3ห้อง/บอส/receipt+mailวันละครั้ง/protect/กลับออก)
ต่อด้วย [ปาร์ตี้2–4คน/2ห้องส่วนตัว/reconnect60s](../../fantasycore/PARTY-DUNGEONS-th.md) (v0.8; roster/stat/rewardตรึง และ quotaร่วมsolo)
— build และ unit tests 124 รายการผ่าน, schema7 แต่ **ยังไม่ได้รันบนเซิร์ฟ Minecraft จริง**
ProtectionStones 2.10.6 ยังไม่ประกาศรองรับ 26.x ต้องยืนยันบน staging ก่อน
ยังไม่ได้แก้โลกเมืองจริง
เว็บยังไม่เชื่อมแผนที่สด ผู้ให้บริการชำระเงิน หรือคิวส่งของเข้าเกม
เว็บย้ายเป็น Next.js + React แล้ว Node API เชื่อม PostgreSQL จริงในเครื่อง สร้าง schema และผ่าน integration tests
GitHub remote ใช้ ArmZOfficial/Minecraft-Luna โดยเผยแพร่ source งานชุดแรกแล้ว
รายการปลั๊กอินเป็นข้อเสนอจากแหล่งทางการ ไม่ได้ยืนยันรายชื่อ plugin ของ SIXPIXEL
ชุดระบบตามภาพและ Protect tiers เพิ่มเป็นสเปกแล้ว ยังไม่ติดตั้งในเกมและยังไม่มี Canvas editor/artist model ที่ผลิตเสร็จ

ตัวเลขค่าธรรมเนียม/ดาเมจ/เวลาในคลิปเป็นตัวอย่างและคำกล่าวของผู้เล่า สูตรและตารางรางวัลต้นฉบับยังไม่มีครบ จึงยังอ้างว่าเหมือนทุกค่าไม่ได้

## ภาพที่เก็บไว้

- [ภาพเมือง Fantasy ขนาดกลาง](lobby-compact-fantasy.png)
- [ภาพเมืองขนาดกลางก่อนปรับ Fantasy](lobby-compact.png)
- [ภาพเมืองขนาดใหญ่เดิม](lobby-overview.png) และ [ผังใหญ่เดิม](lobby-zones.png)
- ภาพรายโซนทั้งหมดอยู่ในโฟลเดอร์ zones พร้อม [พรอมป์ต์ภาพ](zones/PROMPTS.md)

26. [มอนสเตอร์ยิ่งลึกยิ่งโหด](../../fantasycore/MONSTERS-th.md) — Core v0.6 / Target HP BossBar / checklist P
27. [ซากวิหารจันทรา](dungeons/moonfall/README-th.md) — 4 ภาพ AI, แปลน 144×144, NPC/ห้อง/บอส/model/reward/instance specification (ยังเป็นแผน)
28. [Moonfall ดันฝึกเดี่ยว v0.7](../../fantasycore/DUNGEONS-th.md) — โค้ดสร้างโครงแมพ/เข้ารอบ/บอส/รางวัล/protect/กลับออกและ checklist R; ปาร์ตี้ทำต่อในv0.8; โมเดลยังเป็นแผน
29. [Moonfall ปาร์ตี้ v0.8](../../fantasycore/PARTY-DUNGEONS-th.md) — 2instance/roster/scale/reconnect/schema7/recovery/checklist S

30. [บอสจันทราลงสีละเอียด/8animations](../../server/content/dungeons/moonfall/README-th.md) — nativeBlockbench146cubes/19bones/150keys, 4มุมและ29poseframes, sourceพร้อม; ModelEngineadapter/เกมจริงยังรอ
31. [นายธนาคารv2ลงรายละเอียดเต็ม](ASSET-GALLERY-th.md) — 182cubes/17bones/4animations, สมุด/เหรียญติดมือทุกเฟรม, offlinegateผ่าน; เกมจริงยังรอ
32. [ช่างตีเหล็กv2ลงรายละเอียดเต็ม](ASSET-GALLERY-th.md) — 171cubes/16bones/4animations, ตีค้อนtick11/25, offlinegateผ่าน; เกมจริงยังรอ
33. [ผู้ดูแลเควสv2ลงรายละเอียดเต็ม](ASSET-GALLERY-th.md) — 123cubes/17bones/4animations, ม้วนเควสติดมือทุกเฟรม, offlinegateผ่าน; เกมจริงยังรอ
34. [ผู้ดูแลประตูv2ลงรายละเอียดเต็ม](ASSET-GALLERY-th.md) — 124cubes/18bones/4animations, คทาไม่ชนหัว, ทุกท่า≤2.66บล็อก, offlinegateผ่าน; เกมจริงยังรอ
35. [ไอเทมv2 รูนดาบ+Halo](ASSET-GALLERY-th.md) — Java JSON `luma:` texture, display8context, ไอคอนGUIไม่ล้นช่อง, offlinegateผ่าน; packจริงยังรอ
36. [Halo v3 แบบมงกุฎ](ASSET-GALLERY-th.md) — มงกุฎทอง ยอดฟันเลื่อย ทับทิม/อัญมณีฟ้า 92 elements, สวมพอดีหัว; packจริงยังรอ
37. [Props ประจำจุดบริการ 12 ชิ้น](ASSET-GALLERY-th.md) — ธนาคาร/ตีเหล็ก/เควส/ไปรษณีย์/ร้าน/ท่าเรือ/วาร์ป, display8context, gateผ่าน; packจริงยังรอ
