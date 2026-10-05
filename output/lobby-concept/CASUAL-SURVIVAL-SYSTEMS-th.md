# Luma — ชุดระบบ Survival และ casual roleplay ตามภาพที่ผู้ใช้ส่ง

ฉบับออกแบบ 5 ตุลาคม 2026 เพิ่มจากภาพรายการระบบ: สุ่มพื้นที่โลก, รับของรายวัน, หินกันบ้าน, สกินไอเทม,
เควสแลกของ, วาดรูป, อัปเกรดและตั้งบ้าน
ภาพเป็น brief ของผู้ใช้ ไม่ได้ยืนยันชื่อปลั๊กอินหรือ config ของเซิร์ฟต้นฉบับ
เอกสารนี้เพิ่มสเปกจริงสำหรับทีมทำเซิร์ฟ ยังไม่มี JAR, world หรือผลทดสอบ Minecraft runtime ของระบบชุดนี้

## 1. ระบบทั้งหมดเชื่อมกันอย่างไร

| ระบบตามภาพ | ชื่อบนเมนู | เจ้าของระบบที่เสนอ | จุดบริการ |
|---|---|---|---|
| สุ่มพื้นที่โลก | สำรวจโลก / สุ่มวาร์ป | FantasyCore Travel | โซน 09 portal keeper |
| รับของรายวัน | รางวัลของฉัน | FantasyCore Reward + mailbox | โซน 02 welcome rewards |
| หินกันบ้าน | ที่ดินและบ้าน | ProtectionStones + WorldGuard + Core ClaimAdapter | โซน 09 และเมนู `/land` |
| สกินไอเทม | ห้องแต่งไอเทม | Core Cosmetic + item/pack adapter ที่เลือก | โซน 02 wardrobe |
| เควสแลกของ | กระดานแลกของ | Core QuestExchange | โซน 03 NPC เควสเดิม |
| วาดรูป | สตูดิโอภาพพิกเซล | Core Canvas + Bukkit map rendering | แผงศิลปินโซน 06 |
| อัปเกรด | ขยายบ้าน / อัปเกรดอุปกรณ์ | ClaimAdapter / Core Upgrade + item adapter | โซน 09 / โซน 07 |
| เซ็ตบ้าน | จุดกลับบ้าน | Core Home + Travel validator | `/sethome`, `/home`, `/land home` |

ใช้ FantasyCore Economy เป็นแหล่งยอดเงินเดียว ไม่มี plugin ของแต่ละระบบสร้าง wallet ของตัวเอง
เพื่อให้ home ตรวจ world/claim/combat/revision ชุดเดียวกัน เลือก Core Home เป็นเจ้าของ `/home` และ `/sethome` ในแผนใหม่
EssentialsX เหลือ utility ที่เปิดโดยตั้งใจ เช่น spawn/ข้อความ; ไม่เปิดคำสั่ง home/repair/economy ของ EssentialsX ซ้อนให้ข้าม Core
ต้องตรวจ command conflict และ permission ของคำสั่ง namespace จริงก่อน release; มี fallback `fantasy:home` สำหรับทดสอบ

## 2. เมนูหลักที่ผู้เล่นเข้าใจทันที

`/menu` ใช้ inventory GUI 54 ช่อง ข้อความไทย สีหยก/ทอง ตัวเลขยอดเงิน server-side
หน้าหลักมี 7 ปุ่ม: สำรวจโลก, รางวัล, ที่ดินและบ้าน, แต่งไอเทม, เควสแลกของ, วาดรูป, อัปเกรด
ใต้แต่ละปุ่มมีคำอธิบาย 1 บรรทัดและสถานะ เช่น “พร้อมรับวันนี้”, “1/2 แปลง”, “มี 3 สกิน”
บ้านแสดงในปุ่มที่ดิน ไม่เพิ่มหน้าซ้อน 5 ชั้น; มี back/close อยู่ตำแหน่งเดิมทุกหน้า

| คำสั่ง Core ที่เสนอ | หน้าที่ | คำอธิบายเวลาทำไม่สำเร็จ |
|---|---|---|
| `/rtp` | เลือกโลกและสุ่มจุดปลอดภัย | บอก world/cooldown/ไม่มีจุดปลอดภัย |
| `/rewards` | รางวัลวันนี้และรางวัลสะสม | บอกเวลารอบใหม่/เงื่อนไขที่ขาด |
| `/land` | หิน แปลง สมาชิก ขยายบ้าน | บอกพื้นที่ติดใคร/จำนวนแปลงเต็ม |
| `/skins` | preview/equip/reset สกินไอเทม | บอกชนิดอุปกรณ์และสกินที่ยังไม่มี |
| `/exchange` | รายการเควสส่งของ | บอกวัสดุที่ยอมรับและจำนวนที่ขาด |
| `/paint` | เปิดงานวาดของตนเอง | บอก quota/สิทธิ์ภาพ/ขั้นตอนเผยแพร่ |
| `/sethome <ชื่อ>` | บันทึกจุดกลับบ้าน | บอกสาเหตุที่ world/พื้นที่นี้ตั้งไม่ได้ |
| `/home [ชื่อ]` | เลือกหรือกลับบ้าน | บอก combat/จุดเดิมไม่ปลอดภัย |
| `/upgrade` | เลือกอุปกรณ์เพื่อ preview | บอกว่าเป็น equipment upgrade; land upgrade อยู่ `/land` |

คำสั่งในตารางเป็นสเปกใหม่ ยังไม่ได้ implement ไม่ใช้คำสั่งสมมตินี้เป็นผลรับรองว่า plugin ทำงานแล้ว
กล่องจำนวนเงิน/วัสดุตรวจที่ server อีกครั้งก่อน commit ไม่เชื่อค่าจาก GUI หรือ web
มี pending state ป้องกัน double-click; close/logout ไม่ยกเลิก operation ที่ commit แล้วจนทำให้รับซ้ำ

## 3. สุ่มพื้นที่โลก — Random teleport

เปิดสำหรับ logical world `housing` และ `resource` เท่านั้น; lobby/dungeon/boss/arena ไม่มี RTP
Housing เป็นโลกบ้านถาวร; Resource มี world epoch ใช้แยกจุดที่หมดอายุหลัง reset
ชื่อโลก/seed/border จริงต้องบันทึกใน deployment manifest ไม่ใช้ชื่อในเอกสารเป็น world ที่โหลดแล้ว

ค่าทดลองที่เสนอ: อุ่นเครื่อง 3 วินาที, cooldown 120 วินาที, combat lock 15 วินาที, ค่าบริการ 0 ทองใน launch
Housing สุ่มใน annulus ระยะ 500–4,000 บล็อกจากจุดอ้างอิงโลกบ้าน; staging border เริ่มเสนอรัศมี 5,000
Resource ใช้ search profile แยก ไม่ย้ายคนไป Housing แบบเงียบ ๆ หากหาไม่ได้
ตรวจ border ทั้งพื้นที่ยืน ไม่ใช่แค่ center; reserve regions และพื้นที่ claim ทุก owner รวมของตัวเองไม่ใช้เป็นจุด RTP

ขั้นตอน:

1. ผู้เล่นเลือก world และเห็น cooldown/ราคา จากนั้น server ตรวจ alive/world allowed/combat/restrictions
2. สุ่ม candidate แบบ area-uniform ตามวงแหวน ตรวจ block footprint 2 × 2 และช่องศีรษะ 3 บล็อก
3. หลีกเลี่ยง lava, fire, cactus, powder snow, magma, void, liquids และพื้นอันตรายตาม backend material registry
4. load/generate chunk ผ่าน API ที่รุ่นรองรับและมีงบ queue; ไม่มี loop generate chunk จำนวนมากบน main thread
5. เมื่อจะย้าย ตรวจผู้เล่นยัง online, region ยังอนุญาต, floor/headroom ยังปลอดภัย และ epoch เดิมอีกครั้ง
6. teleport สำเร็จแล้วจึงบันทึก cooldown/receipt; event ที่ plugin อื่น cancel ไม่แสดง “สำเร็จ” และไม่คิดเงิน
7. จำกัด 24 candidates/คำขอ, timeout รวม 10 วินาที, งาน chunk พร้อมกันไม่เกิน 2 ต่อโลกในค่าเริ่มต้น
8. หากหาไม่ได้คืนสถานะเป็น failed พร้อมข้อความไทย ไม่มี fallback ไป X0/Y0 และไม่มี free reroll fee exploit

ไม่ใช้ client coordinate หรือยอมให้ผู้เล่นส่งชื่อโลก arbitrary; ID ต้องอยู่ใน allowlist
สำหรับอนาคตที่คิดค่าบริการ ใช้ reserve → teleport → settle/refund operation journal ตาม Core
ไม่จำเป็นต้องซื้อ BetterRTP ใน launch ถ้า Core Travel ทำงานแค่นี้; หากเปลี่ยนไปใช้ provider ภายนอก ให้ adapter เดียวเป็นผู้ควบคุม cooldown/price

## 4. รับของรายวันและรางวัลสะสม

ใช้ Asia/Bangkok และ game UUID วันใหม่ที่ 00:00; หน้ารับแสดงเวลารอบใหม่ชัดเจน
รอบ 7 ครั้งที่เสนอเป็นการสะสมจำนวนวันที่รับ ไม่บังคับต่อเนื่อง และไม่ลงโทษผู้เล่น casual ที่ขาดวัน
รับได้ไม่เกิน 1 ครั้งต่อวัน; ไม่ให้ไล่รับย้อนหลังหลายวันที่ offline

| ครั้งในรอบ | รางวัลทดลอง |
|---|---|
| 1 | 100 gold |
| 2 | 150 gold + อาหาร 8 ชิ้น |
| 3 | 200 gold + ใบซ่อม 1 ใบ (credit สูงสุด 100 gold) |
| 4 | 250 gold |
| 5 | 300 gold + วัตถุดิบคราฟต์พื้นฐาน 4 หน่วย |
| 6 | 350 gold |
| 7 | 500 gold + 1 red + คูปองสีวาดภาพ 1 ใบ |

รายการไอเทมต้อง resolve template ที่มีจริงก่อน publish; หากไม่มีใบซ่อมหรือคูปอง ห้ามส่งไอเทม lore เปล่าแทนสิทธิ์ที่ใช้ไม่ได้
ใบซ่อมเป็น credit ใช้ได้ครั้งเดียว ตัดราคาได้ไม่เกินค่างาน ไม่มีเงินทอน ไม่เพิ่ม durability เกินสูงสุด
คูปองสีปลด palette variant ไม่มี stat; คนไม่มีคูปองยังวาดได้ด้วย palette เริ่มต้น
ทั้งหมดเป็นค่า balance ที่เสนอ ไม่ใช่ตารางต้นฉบับ และไม่เชื่อมกับ paid crate

เก็บ unique `(player_uuid, reward_program, Bangkok_day)` และ delivery receipt แยกจากจำนวนครั้งในรอบ
สอง instance/GUI พร้อมกันรับได้ครั้งเดียว; inventory เต็มส่ง mailbox ไม่โยน item ลงพื้นและส่ง mail พร้อมกัน
starter/housing stone แยก program รับครั้งเดียว ไม่ reset พร้อม daily
online/AFK ใช้กติกาเดิมใน SERVER-SYSTEMS-PLAN-th.md ไม่ให้ daily GUI เปลี่ยนค่าเวลาแอบแฝง

## 5. หินกันบ้านและการขยายที่ดิน

ตารางขนาดและการป้องกันฉบับเต็มอยู่ที่ [ProtectionStones tiers](PROTECTIONSTONES-TIERS-th.md)
ระดับ I–V: 11 × 11, 21 × 21, 31 × 31, 51 × 51, 81 × 81; สูงเต็ม world
นี่เป็นสเปก Luma ที่เลือกใหม่ตามการนับ radius ของ plugin ไม่ใช่ claim ว่า 10 × 10 ในคลิปมี radius=10
บ้านตัวอย่างที่ lobby ใช้ 11 × 11 ในมุมสอน 13 × 13; โลกบ้านจริงแยกจาก lobby
เมนูรวมซื้อหิน ดูขอบ สมาชิก ตั้ง home และ upgrade ใช้ permission/owner/operation registry เดียว

## 6. สกินไอเทม — เปลี่ยนภาพโดยคงความสามารถ

เลือก item/pack provider เพียงตัวเดียวตาม MODEL-AND-CONTENT-PLAN-th.md; Core เก็บสิทธิ์และ visual selection
launch เสนอ 12 สกิน: ดาบ 4, ธนู 2, เบ็ด 2, เครื่องมือ 4; ใช้ silhouette/rig ร่วมและ texture variants เมื่อเหมาะสม
เริ่มเปิดจริงหลังมี asset ที่ทดสอบแล้ว 4 ชิ้น ไม่ใส่ครบ 12 ใน catalog ขณะไฟล์ยังไม่มี
ธีมคอลเลกชัน: Jade Warden, Sunforge, Moonwater, Forest Wanderer; ผูก product ID แยกจากอาวุธ RPG ที่มี stat

ขั้นตอนผู้เล่น: ใส่อุปกรณ์ลงช่องอ่านอย่างปลอดภัย → เลือกสกินที่เป็นเจ้าของ → ดู preview → ยืนยัน/คืนภาพเดิม
preview เป็นตัวอย่างที่ไม่ใช้โจมตีจริง; เวลา preview ไม่ย้ายอุปกรณ์จริงออกจาก inventory ให้ dupe ได้
รักษา item template ID, serial, durability, enchant, stat, owner/bind, sockets และ upgrade level
visual key ต้องเป็นค่า allowlisted ตามชนิดอุปกรณ์; เปลี่ยน lore/client packet ไม่ปลด entitlement
สกินผูก player UUID ใน launch หากโอน item เจ้าของใหม่ไม่รับ entitlement อัตโนมัติ; คง policy reset visual ตาม adapter
ของผูกเจ้าของแสดงเหตุผลก่อน trade; serial duplication ต้องเข้าสู่ review ไม่ auto delete ของโดยเดา

modern pack และ legacy 1.16.5 pack ใช้สิทธิ์ชุดเดียว แต่ mapping ต่างกันตาม version; client ไม่รับ pack มี vanilla fallback
ตรวจถือ/สวม/ตี/drop/pickup/ตาย/ซ่อม/upgrade/relog ไม่ทำสกินหรือข้อมูล gameplay หาย
หาก provider ใช้ armor stand/display entity เสริม ให้เป็น cosmetic ไม่เป็นตำแหน่ง hitbox จริงของ item

## 7. เควสแลกของ — ส่งวัตถุดิบ รับของที่เห็นล่วงหน้า

เปิด 6 recipes ทดลองก่อนเพิ่ม 12 รายการ; แยก exchange รับของทันทีจากเนื้อเรื่องและ player market orders
ตัวอย่าง 6 recipe IDs: food_bundle, repair_credit, paint_palette, garden_lamp, starter_tool, wardrobe_token
อัตราตัวอย่าง: ข้าวสาลี 32 → ชุดอาหาร 1; แท่งเหล็ก 16 → ใบซ่อม credit 100; หมึก 8 + กระดาษ 16 → คูปองสี 1
recipes ของโมเดล/สกิน publish หลัง output entitlement/item template มีจริง; วัสดุและจำนวนทั้งหมดต้อง balance จากรายได้จริง
ไม่รับของที่เพียงชื่อเหมือน: ตรวจ template/material, serialized item policy, enchant/bind และ provider metadata ตามสูตร
หากรับ vanilla material ให้บอกว่าเป็น vanilla; ไม่กิน named/custom item ที่ใช้ material เดียวกันโดยไม่แจ้ง

GUI แสดง input ที่จะตัด exact slots/amount, output, quota วันนี้, และเหตุผลที่ยังทำไม่ได้
การตัด inventory กับ grant/mailbox ไม่มี SQL atomic ครอบโลกเกม ต้องใช้ operation journal และ item snapshot/recovery
ขั้นตอน slot reservation/revalidation ใช้ server thread ผู้เล่นย้าย/drag/swap item ระหว่าง preview ต้องตรวจใหม่
ส่งซ้ำเร็ว ใช้ shift-click หรือเปิดสองเมนูไม่กิน/จ่ายเกิน; batch สูงสุด 16 ครั้ง/operation และไม่เกิน quota
inventory เต็มส่ง output ที่ commit แล้วเข้า mailbox; failed input validation ไม่จ่ายบางส่วน
reconnect/คำสั่ง/BetonQuest conversation ต้องเรียก Core action ID เดียว ไม่แจกอีกชุดผ่าน console reward

## 8. วาดรูป — งานพิกเซลที่นำไปติดบ้านได้

ตีความ “วาดรูป” เป็นสร้าง pixel art บน filled map สำหรับแต่งบ้าน ไม่เป็นการแก้แผนที่โลก
launch เลือกเว็บ pixel editor ที่เชื่อมบัญชีเกมภายหลัง สำหรับวาดละเอียด และ in-game GUI สำหรับเลือก/พิมพ์/จัดการงาน
เวอร์ชันในเกมอย่างเดียวเป็น palette+stamp editor แบบจำกัด ไม่อ้างว่า inventory GUI วาด freehand ลื่นเหมือนเว็บ
ระบบ Canvas และ editor ยังไม่ได้พัฒนาในเว็บปัจจุบัน

ภาพ 128 × 128 pixels; palette เริ่มต้น 16 สีที่ map renderer ของ legacy/new clients ใช้ได้
เครื่องมือเว็บ: pencil 1/2/4px, eraser, fill, undo/redo สูงสุด 50 ขั้น, grid, zoom, save draft, preview แล้ว submit
ผู้เล่นเก็บ draft ไม่เกิน 5 งาน งานพิมพ์ active ไม่เกิน 10 designs และไม่เกิน 20 copies ต่อคนในค่าเริ่มต้น
ใช้ buffer 16,384 palette indices พร้อม artwork UUID/version/hash ไม่เก็บ base64 image หลาย MB ใน lore
launch ปิด arbitrary upload URL/ไฟล์รูปจากภายนอกและ gallery public; รับเฉพาะ pixel data ที่ editor สร้างและ server validate
หากเพิ่ม import ภายหลังต้องออกแบบ size limits, decoder, moderation และ fetch isolation ก่อนเปิด

workflow: draft → submitted → approved/rejected → printable; เผยแพร่ให้ผู้อื่นดูต้อง moderation แยก
ค่าพิมพ์ทดลอง 100 gold + กระดาษ 8 ต่อ copy หลังผ่านอนุมัติ ไม่ตัดค่า approval ที่ถูกปฏิเสธ
หากได้รับ coupon ให้ลดค่าบริการตาม policy และใช้ coupon receipt ครั้งเดียว
งาน reject บอกเหตุผลที่อ่านได้ แก้แล้วส่งเป็น version ใหม่; การถอนอนุมัติซ่อนภาพ public และใช้ placeholder ตาม policy ไม่แก้ map อื่นในโลก
map ID และ artwork ID เป็นคนละค่า; renderer ผูก dataset/hash ของงานเจ้าของที่ตรวจแล้ว ไม่รับ raw map ID จาก web ไปแก้ภาพได้เลย

ใช้ [Bukkit MapRenderer](https://jd.papermc.io/paper/1.21.11/org/bukkit/map/MapRenderer.html) และ
[MapCanvas](https://jd.papermc.io/paper/1.21.11/org/bukkit/map/MapCanvas.html) เป็น API reference สำหรับออกแบบ adapter
ต้อง compile กับ Paper build ที่เลือกจริงและทดสอบ palette/packet/metadata บน 1.16.5 ไม่ถือว่า Javadoc รุ่นนี้พิสูจน์ compatibility แล้ว
cache raster ที่ validate แล้ว, dirty rectangles/bounded update, ไม่ทำ SQL/PNG decode ใน render callback ทุก tick
เฉพาะผู้อยู่ใกล้ภาพหรือกำลังถือ map ได้ update ไม่ broadcast ทุกภาพทุก tick ให้ทุกคน
ภาพติด item frame ต้องผ่าน claim ownership; map copy/ขาย/ลบ/restart มี revision และ receipt กัน grant ซ้ำ

## 9. ตั้งบ้านและอัปเกรด

Core Home เก็บ world UUID, epoch, X/Y/Z/yaw/pitch, claimID/revision และ owner
เริ่ม 1 personal home; ปลด 3 หลัง housing_workshop และ 5 หลัง housing_community ไม่เกิน 5 ใน launch
claim home เป็นจุดของแปลง ใช้สมาชิก/เจ้าของตรวจสิทธิ์แปลงใหม่ทุกครั้ง แยกจาก quota personal homes
personal home ตั้งได้ในแปลงที่เป็นเจ้าของหรือ trusted member เท่านั้น; ไม่ตั้งใน resource/reset world/dungeon/boss/arena
เมื่อลบสมาชิก ถอนแปลง โลกถูกย้าย หรือเปลี่ยน revision สำคัญ ให้ validate ก่อนวาร์ป ไม่อาศัยข้อมูลครั้งที่บันทึกเพียงครั้งเดียว

ชื่อบ้าน 1–16 ตัวอักษรตาม whitelist ที่เลือก ใช้แสดงชื่อไทยแยกจาก key; ไม่มี path/SQL/command injection จากชื่อ
ตั้งทับชื่อเดิมมี preview พิกัดก่อน/หลัง; delete home ไม่ลบบ้านหรือ region
วาร์ปอุ่นเครื่อง 3 วินาที ยกเลิกเมื่อเคลื่อนที่/ได้รับดาเมจ/combat lock และใช้ landing validator ร่วมกับ RTP
จุดอับถูกบล็อก/พื้นหายให้บอกว่าต้องตั้งใหม่ อาจใช้ safe spawn ตามความยินยอมผู้เล่น ไม่พยายามสุ่มเข้าแปลงผู้อื่น

อัปเกรดมีสองหมวดแยกชัด:

- **ที่ดิน:** เพิ่ม tier ตาม PROTECTIONSTONES-TIERS-th.md คงบ้าน/สมาชิก/จุดวาร์ป ไม่ย้ายทั้งโครงสร้างด้วย WorldEdit
- **อุปกรณ์:** ผ่าน Core Upgrade เดิม แสดงสูตร/ราคา/ผลลัพธ์ อาจเริ่มแบบ deterministic +1..+3 ไม่มีแตกหายหรือสุ่มจนกติกาพร้อม

เก็บ equipment template/version, level, serial และ metadata; ต้องกำหนด stat cap/damage formula จริงก่อนเปิด
สกินอาวุธไม่เพิ่มอัตรา upgrade และรับเงินจริงไม่ทำให้ home/claim bypass ข้อจำกัดที่ตั้งไว้

## 10. ตกแต่งแมพและ NPC เพิ่มโดยคงสเกลเดิม

ใช้ service station เดิมเพื่อลด NPC และไม่เพิ่มอาคารใหญ่:

| Station | ใช้บริการใหม่ | การตกแต่ง/asset |
|---|---|---|
| portal_keeper | travel.rtp / land.main / home.main | ป้ายโลกบ้านและทรัพยากร คนละสี; ตาราง Protect 5 ระดับ |
| welcome_rewards | reward.daily / reward.progress | ปฏิทิน 7 ช่อง ตู้จดหมายข้างเคาน์เตอร์; offer_parcel เมื่อ commit |
| welcome_wardrobe | cosmetic.item_skin | แท่นอาวุธ preview 2 ชิ้น ไม่วาง model ทุก skin พร้อมกัน |
| quest_gold / quest_red | quest.exchange | บอร์ดวัสดุ input→output และความถี่รับ ไม่ให้ผู้เล่นหยิบของโชว์ |
| forge_upgrade | equipment.upgrade | แท่นเดิม แสง/ค้อนหลัง success; ไม่มีเสียงรัวทุก hover |
| market_artist (ใหม่) | canvas.main | แผง 5 × 5 ใช้แผงเดิมโซน 06; easel, palette, map frame |

artist anchor เสนอ X=-91.5, Z=57.5, Y=Y0+1 (81 เมื่อพื้น Y0=80), target X=-91.5/Z54.5, yaw=180
body หัน North เข้าหาลูกค้า; head clamp ±35° ไม่หมุนทั้งตัวตามคนหลังโต๊ะ ตรวจ collider กับทางเดินอย่างน้อย 3 บล็อก
เป็นตำแหน่งเสนอในแผงตลาดเดิม ต้องตรวจ terrain/world จริงก่อนสร้าง ไม่ใช้ยืนยันว่า NPC ถูก spawn แล้ว
model ใหม่ที่ต้องทำ: artist variant, paintbrush prop, easel, protection-stone inventory variants 5 สี
easel/ป้าย/แผงใช้ vanilla blocks ได้ก่อน; NPC animation idle/greet/paint one-shot ไม่วาดทั้งวันทุก instance
ทำ reference ด้วย ChatGPT → ส่ง brief ผ่าน Antigravity/Blockbench ตาม handoff เดิม → polish → import/test ก่อนเปิด catalog
ยังไม่มีภาพหรือ bbmodel ของ artist/หิน 5 สีที่สร้างในงานเพิ่มเอกสารรอบนี้

## 11. ข้อมูลและ AdminPanel

สเปก storage Core ที่เสนอ ต้องออก migration หลังเลือก runtime ไม่ใช่ตารางที่มีอยู่แล้วใน PostgreSQL เว็บ:

| กลุ่มข้อมูล | Keys สำคัญ |
|---|---|
| claim registry | worldUUID+regionID, ownerUUID, tier, bounds, stoneSerial, revision |
| housing unlocks | playerUUID+permitID; idempotent earned event |
| home registry | playerUUID+homeKey, worldUUID, epoch, claimID, yaw/pitch |
| reward claims | playerUUID+program+day, cycleIndex, operationID, receipt |
| skin entitlements | playerUUID+skinID, sourceReceipt; itemSerial+selectedVisual |
| exchange operations | operationID, playerUUID, recipeVersion, inputSnapshot, outputReceipt |
| artwork | artworkUUID+version, ownerUUID, paletteData/hash, moderation status |
| canvas copies | copySerial, artworkVersion, mapID, playerUUID, operationID |

AdminPanel เพิ่มหมวด บ้าน/Protect, Rewards, สกิน, Exchange, Canvas และ RTP profiles
หมวดที่มี adapter ยังไม่พร้อมแสดง “ยังไม่เชื่อม” ไม่สร้างปุ่ม success ปลอม
permission แยก `.claims.inspect/.adjust/.unclaim`, `.rewards.edit`, `.skins.grant`, `.exchange.edit`, `.canvas.moderate`, `.travel.edit`
moderator อนุมัติภาพได้เมื่อได้รับ node แต่ปรับเงิน/ที่ดินไม่ได้; builder แก้ station staging ไม่ปลด reward/skin
edit/publish แยกกัน; confirm ราคา/พื้นที่/target พร้อม revision; ทุก write audit reason และ operation ID
freeze เฉพาะ module เมื่อ recovery ไม่แน่นอน และทำให้ผู้เล่นอ่านเหตุผลได้

เว็บเพิ่มใน phase integration: คู่มือ 7 ระบบ, land tiers public, บ้านของฉัน private, reward receipt, wardrobe,
pixel editor และ staff moderation; สั่งทำงาน typed API/allowlist ไม่เปิด RCON/console จากเว็บ
public BlueMap ไม่มีพิกัดบ้านหรือภาพส่วนตัวจนเจ้าของ opt-in และผ่าน policy
ไม่แก้ wallet หรือ WorldGuard จาก Node SQL โดยตรง ต้องส่ง operation ให้ Core authority

## 12. ลำดับส่งงานและเกณฑ์เปิด

1. ล็อก backend/dependencies และ PoC PS tier I/V พร้อม client 1.16.5 และรุ่น backend
2. ทำ Claim + Home + RTP ก่อน เพื่อให้เริ่มบ้านได้จริงและไม่ตก void/เคลมทับ
3. ทำ Reward + Exchange + mailbox พร้อม crash recovery และ economy balance
4. ทำสกิน 4 ชิ้นก่อนขยาย 12; test repair/upgrade/trade/pack fallback
5. ทำ Canvas in-game manage/print + linked web editor + moderation แยกจากการเปิดเซิร์ฟหลัก
6. เพิ่ม variant/model และรูปประกอบหลัง UI/ownership/journal ผ่าน ไม่ทำ asset จำนวนมากก่อนพิสูจน์ pipeline

| Test | เกณฑ์ผ่าน |
|---|---|
| RTP unsafe/region/border/reset/cancel | ไม่คิดสำเร็จเมื่อถูก cancel ไม่ย้ายไปพื้นไม่ปลอดภัย ไม่ generate chunk ไม่จำกัด |
| Daily ข้ามเที่ยงคืน/restart/สอง instance | จำนวนครั้ง/วันตรง Bangkok และ delivery ครั้งเดียว |
| Claim ทุก tier/ทุกขอบ/ทุก height | ตรงตารางและ policy พร้อมชุด Protect tests ที่ลิงก์ |
| Home สิทธิ์หาย/epochเปลี่ยน/combat | ยกเลิกอย่างชัดเจน ไม่มี bypass แปลง/สนาม |
| Skin + repair/upgrade/relog/trade | metadata/stat/serial ถูกต้องและไม่ปลด entitlement ให้คนอื่น |
| Exchange ย้าย input/เต็ม/duplicate/crash | ไม่กินของผิด ไม่สร้าง output ซ้ำ มี recovery proof |
| Canvas payload ผิด/งาน reject/map copy | validate/refuse ถูก ไม่แก้ map คนอื่น ไม่คิดค่าพิมพ์ก่อนอนุมัติ |
| GUI shift/drag/hotbar/double click | ของโชว์ไม่หลุดเป็นของจริง ไม่มีผลซ้ำ |
| Native commands/namespace/admin node | ข้าม policy ไม่ได้ หรือปิด command นั้นไว้ชัดเจน |
| DB/provider unavailable | read-only/status ตาม capability ไม่ใช้ grant สำเร็จปลอม |
| Load | วัด MSPT/chunk/map updates กับ hardware จริงก่อนกำหนด capacity ผู้เล่น |

ไม่มีผลทดสอบ runtime ของตารางนี้ในรอบเพิ่มเอกสาร ต้องลงชื่อ build/config/test evidence ก่อนเปลี่ยนสถานะเป็น release-ready

อ่านร่วมกับ [เริ่มอ่านทั้งหมด](START-HERE-th.md), [ระบบเดิม](SERVER-SYSTEMS-PLAN-th.md), [Protect tiers](PROTECTIONSTONES-TIERS-th.md),
[HUD/TAB](HUD-TAB-SCOREBOARD-SPEC-th.md), [โมเดล](MODEL-AND-CONTENT-PLAN-th.md), [เว็บ/automation](WEB-AND-AUTOMATION-PLAN-th.md)
