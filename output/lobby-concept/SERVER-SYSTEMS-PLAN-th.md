# แผนระบบ MMO RPG ตามคลิป — Java-first และเล่นด้วย Java 1.16.5 ขึ้นไป

ตรวจเอกสารทางการ: 5 ตุลาคม 2026
ขอบเขตงานฉบับนี้: แผนระบบและสเปกพัฒนา ยังไม่ได้ติดตั้งปลั๊กอินหรือสร้างเซิร์ฟเวอร์ทดสอบ

## 1. ข้อสรุปที่ใช้สร้างเซิร์ฟเวอร์

ใช้ Paper หนึ่งตัวเป็น backend รุ่นเดียว ใช้ปลั๊กอินพื้นฐานที่มีผู้ดูแล และเขียนปลั๊กอินแกนชื่อชั่วคราว **FantasyCore** คุมระบบที่ต้องสอดคล้องกัน เช่น เงินสองชนิด ธนาคาร ตายเสียเงิน รางวัลบอส ซ่อมของ ออเดอร์ และการรับรางวัล

- Java เป็นเป้าหมายเปิดใช้งานหลักตามความต้องการล่าสุด
- Java 1.16.5 เป็น client ต่ำสุดที่ต้องนำมาทดสอบ ไม่ใช่เวอร์ชันที่ต้องใช้รัน backend
- ใช้ ViaVersion + ViaBackwards เชื่อม Java หลายเวอร์ชัน; ผ่านการทดสอบแล้วจึงประกาศช่วงที่รองรับ
- Bedrock เก็บเป็นส่วนขยาย ไม่เป็นเงื่อนไขที่ทำให้งาน Java ต้องลดคุณภาพ
- ไม่ต้องติดตั้ง client mod เพื่อใช้ระบบหลัก
- เมนูมาตรฐานภาษาไทยและบล็อก vanilla เป็นชุดหลัก Resource pack เป็นชุดเสริม
- แยกโลก hub, survival, resource, dungeon, boss และ pvp ภายในเซิร์ฟเวอร์เดียวก่อน
- รายการบริการเสริมมีครบให้เลือก แต่ไม่เปิดทุกระบบพร้อมกันก่อนทดสอบเศรษฐกิจ

เอกสารนี้ไม่ได้ยืนยันว่า SIXPIXEL ใช้ปลั๊กอินชื่อใด การได้พฤติกรรมเหมือนคลิปต้องปรับ config และอาจเขียน integration แม้เลือกปลั๊กอินใกล้เคียงแล้ว

## 2. หลักฐานจากคลิปกับสิ่งที่ยังไม่รู้

อ้างอิง [คลิป SIXPIXEL ของ AllenTH](https://www.youtube.com/watch?v=rKIdKLOJPSY) จากถอดเสียงอัตโนมัติที่อ่านในงานนี้ ตัวเลขต่อไปนี้เป็นสิ่งที่ผู้เล่ากล่าวถึงหรือแสดงตัวอย่าง ไม่ใช่ข้อมูลภายในเซิร์ฟเวอร์ครบทั้งหมด และไม่ใช่การยืนยันค่าปัจจุบัน

| ช่วงคลิป | สิ่งที่กล่าวถึง | จุดในแมพ | สิ่งที่ยังต้องกำหนด |
|---|---|---|---|
| 00:41–01:31 | เมนูนำทาง เชื่อม Discord ล็อกอินรายวัน เมนู Shift+F | 01/02 | ของรางวัลและสิทธิ์รับ |
| 01:34–03:35 | โลกสร้างฐาน Protect ตัวอย่าง 10 × 10 ร้านขายของ home | 06/09 | พื้นที่เคลมจริง ช่วงราคา จำนวน home |
| 02:10–02:29 | เงินทองและเงินแดงจากกิจกรรมต่างกัน | 05/06 | อัตราได้และใช้เงินทั้งระบบ |
| 03:50–04:06 | กุญแจฉายาตัวอย่าง 1,000 เงินแดง | 02 | โอกาสสุ่ม รายชื่อฉายา |
| 04:08–04:17 | จับสัตว์เลี้ยง มีค่าใช้จ่ายตัวอย่าง 20 เงินแดง | 10 | โอกาสจับ ชนิดสัตว์ ความสามารถ |
| 04:22–05:14 | ฝากเงิน เฟอร์นิเจอร์ เควสทอง/แดง | 03/05/06 | กติกาธนาคารและสูตรเควส |
| 05:35–06:26 | ดันเจี้ยนหลายระดับ ของดรอป ขายเป็นเงินแดง | 07/09 | แผนที่ ความยาก ตารางดรอป |
| 06:10–06:19 | ตายตัวอย่างเสียทอง 30% เงินแดงไม่เสีย ของไม่ดรอป | 05 | บริบทโลกและข้อยกเว้น |
| 06:29–07:58 | อุปกรณ์คราฟต์ เซต/อาวุธต่างกัน บอส 3 ตัว | 07/09 | ชื่อ สูตร สกิล สเตตัส |
| 08:01–08:35 | บอสเกิดช่วง 1–3 ชั่วโมง ดาเมจ 500 เป็นเกณฑ์ Top 10 รางวัลดีขึ้น | 09 | เวลาแต่ละบอส สูตรคิดดาเมจ โอกาสรางวัล |
| 08:44–10:21 | สนับสนุนเซิร์ฟ หัวก้อย คอสเมติก หัวตกแต่ง | 02/06 | สิทธิ์และรายการสินค้า |
| 10:24–10:53 | หีบหลายประเภทและกุญแจ | 02 | ตารางรางวัลและโอกาสสุ่ม |
| 11:11–12:05 | ความทนทาน ซ่อมที่ช่าง ตัวอย่างค่าซ่อม 500 ทอง | 07 | ราคาของแต่ละชิ้น สูตรซ่อม |
| 11:29–11:46 | แรงค์ปลดล็อกความสะดวก เช่น ฝากเงินผ่านคำสั่ง | 04/05 | ขั้นแรงค์ วิธีเลื่อนแรงค์ |
| 12:08–12:43 | อาชีพนักขุด/นักล่า และตกปลา | 08/10 | อาชีพทั้งหมด XP และสิทธิ์ |
| 12:56–13:06 | AFK 30 นาที ได้เงินแดง 1 | 11 | นับเวลาต่อเนื่องหรือสะสม ข้อจำกัดบัญชี |
| 13:33–13:40 | รูนยังเป็นระบบในอนาคต ณ เวลาถ่าย | 12 | ยังไม่เปิดเป็นระบบจากคลิป |
| 13:43–14:20 | ออเดอร์ให้คนหาของ รางวัลออนไลน์ | 02/06 | escrow อายุออเดอร์ เวลาแลกรางวัล |

ต้นคลิปกล่าวถึงกิลด์และการแข่งขันอันดับ แต่ไม่อธิบายสูตรคะแนนกิลด์ละเอียด การเคลื่อนที่ด้วยเบ็ดมีคำกล่าวถึงสั้น ๆ จึงเก็บเป็นฟีเจอร์ทดลอง ไม่ออกแบบให้เป็นวิธีเดินทางจำเป็น

## 3. ชุดปลั๊กอินที่แนะนำจริง

รายละเอียดแหล่งข้อมูลและสถานะโครงการอยู่ใน [ผลวิจัยปลั๊กอิน](PLUGIN-RESEARCH-SOURCES-th.md) เลือกชุดด้านล่างเป็นฐาน และตรวจเวอร์ชันที่เข้ากันบน staging ก่อนล็อกไฟล์ JAR

| ชั้น | เลือกใช้ | หน้าที่และขอบเขต |
|---|---|---|
| Server | Paper รุ่นที่ยังได้รับการรองรับและผ่านชุดทดสอบ | backend API และโลกเกม |
| Protocol | ViaVersion + ViaBackwards | การแปล protocol ของ Java เก่า/ใหม่ |
| สิทธิ์ | LuckPerms | สิทธิ์ผู้เล่น แรงค์ และ permission node |
| สะพานระบบเงิน | Vault | API เชื่อม ไม่ได้สร้างเงินหรือธนาคารเอง |
| เงินหลัก | FantasyCore Economy | เจ้าของยอดทอง เงินแดง เงินฝาก และ ledger เพียงรายเดียว |
| NPC | Citizens | ตัวละครจุดบริการ; เรียก action ของ Core ผ่าน adapter |
| พื้นที่ | WorldEdit + WorldGuard | งานสร้างและ region ล็อบบี้/สนาม |
| Protect | ProtectionStones หรือ Core Claim adapter | การเคลมด้วยบล็อก; ตรวจขนาดที่ต้องการจริง |
| utility | EssentialsX เฉพาะ home/spawn/ข้อความที่ต้องใช้ | ปิดเส้นทาง economy/repair/item grant ที่ข้าม Core; ไม่เป็นแหล่งยอดเงินอีกตัว |
| placeholders | PlaceholderAPI | แสดงข้อมูลที่ Core เปิดให้; ไม่เก็บยอดเงินเอง |
| อาชีพ | Jobs Reborn | งาน/XP และ payout ที่เชื่อม gold ผ่าน Vault; ตรวจ anti-farm |
| เควสเนื้อเรื่อง | BetonQuest เมื่อจำเป็น | objective/conversation; เงินและรางวัลผ่าน Core action เดียว |
| มอนสเตอร์ | MythicMobs | นิยามม็อบ บอส สกิล และจุดเกิด |
| อาวุธ RPG | MMOItems + MythicLib รุ่นที่เข้ากัน | templates/สเตตัส/สกิล; Core ใช้ adapter รักษาข้อมูลไอเทม |
| ระบบเฉพาะ | FantasyCore | bank, death, reward, boss credit, repair, orders, menu, rank progression |

สำหรับเควสเก็บของที่มีเพียงไม่กี่แบบ Core ทำเองได้โดยยังไม่เพิ่ม BetonQuest ส่วน MythicMobs/MMOItems เลือกเมื่อเวอร์ชันและ license เหมาะสม ถ้าไม่ใช้ให้เขียน item/combat module เองและประเมินงานสกิลเพิ่ม ไม่ถือว่าการเปลี่ยนเป็นปลั๊กอินฟรีจะเทียบเท่าทันที

**อย่าให้ EssentialsX Economy, ปลั๊กอินธนาคาร และ Core แต่ละตัวมียอดทองอิสระแล้วพยายาม sync กัน** ชุดแนะนำให้ Core เป็นเจ้าของเงิน และเปิด Vault provider สำหรับทองเพียงชนิดเดียว เงินแดงเรียกผ่าน Core API โดยตรง

[Vault repository](https://github.com/MilkBowl/Vault) อธิบายบทบาท API เชื่อม ส่วน [ProtectionStones](https://github.com/espidev/ProtectionStones) รองรับการเคลมผ่านบล็อกและขนาดใน config; ขนาด 10 × 10 ต้องตรวจว่าผลลัพธ์รวมขอบตรงที่ต้องการ ไม่เทียบกับค่ารัศมีโดยอัตโนมัติ

## 4. แผนรองรับ Java หลายเวอร์ชัน

เอกสาร [ViaVersion](https://github.com/ViaVersion/ViaVersion) ระบุการให้ client ใหม่เชื่อม server เก่า และ [ViaBackwards](https://github.com/ViaVersion/ViaBackwards) ให้ client เก่าเชื่อม server ใหม่โดยต้องมี ViaVersion การแปล protocol ไม่ทำให้ฟีเจอร์ใหม่มีอยู่จริงใน client เก่า

### Backend
- ทดลองด้วย Paper stable ที่ยังรองรับปัจจุบันก่อน จากนั้นตรวจ MythicMobs/MMOItems/Citizens/Jobs ทั้งชุด
- หาก plugin สำคัญยังไม่รองรับรุ่นใหม่ ให้เปรียบเทียบการเปลี่ยน plugin กับการรอรุ่นที่รองรับ ไม่เลือก backend เก่าที่เลิกดูแลเพียงเพื่อเปิดระบบเร็ว
- เอกสาร Paper ณ วันที่ตรวจระบุสาย 26.1+ ใช้ Java 25; สาย 1.20–1.21.11 ใช้ Java 21 ดู [ข้อกำหนด Paper](https://docs.papermc.io/paper/getting-started/)
- 1.21.11 เป็นตัวเลือกความเข้ากันที่ต้องตรวจสถานะการดูแลก่อนใช้จริง ไม่ได้ยืนยันเป็นรุ่น production ในเอกสารนี้
- plugin เขียนและ compile สำหรับ backend ที่เลือกเพียงรุ่น API เป้าหมาย; client 1.16.5 ไม่ได้บังคับให้ Java runtime เป็น Java 8

### ช่วง client ที่ตั้งเป้าทดสอบ
| กลุ่ม | เป้าหมาย | ข้อกำหนด |
|---|---|---|
| เวอร์ชันเดียวกับ backend | มาตรฐานภาพและระบบ | ต้องผ่านทุก acceptance test |
| Java 1.20.5–1.21.x ที่ประกาศรองรับ | กลุ่มใหม่ | ทดสอบเมนู ข้อมูล item model และ resource pack ตามรุ่น |
| Java 1.17–1.20.4 | กลุ่มกลาง | ไม่มี dependency บน native dialog ใหม่; pack ต้องแยกตาม format |
| Java 1.16.5 | เก่าสุดที่ผู้ใช้เลือก | ต้องผ่านซื้อขาย คราฟต์ ซ่อม เควส บอส กิลด์ และ home |
| Java รุ่นใหม่หลัง backend | เปิดเมื่อ ViaVersion รองรับและทดสอบผ่าน | ไม่รับประกันวันแรกที่ Mojang ออกเวอร์ชัน |
| ต่ำกว่า 1.16.5 | ไม่อยู่ในเป้าหมาย | ไม่จำเป็นต้องติดตั้ง ViaRewind |

ViaBackwards ระบุข้อจำกัด เช่น client ต่ำกว่า 1.17 มอง/โต้ตอบ Y นอก 0–255 ไม่ได้ และบางการคลิก inventory มีข้อจำกัด จึงกำหนดทุกโลก RPG ที่รองรับ client เก่าให้อยู่ในช่วง Y=16–200 และใช้ GUI คราฟต์/ซ่อมของ Core แทนการพึ่ง smithing table รุ่นใหม่ รายละเอียดใน [known issues](https://github.com/ViaVersion/ViaBackwards#known-issues)

### Visual baseline เพื่อความเนี๊ยบ
- ใช้ quartz/polished diorite แทน calcite, purple wool/terracotta แทนใบ cherry และ glass สีฟ้า/ม่วงแทนคริสตัล amethyst
- เสาไฟใช้ lantern/end rod/sea lantern ที่มีในฐาน 1.16.5
- ไม่ใช้ display entity รุ่นใหม่เป็นส่วนจำเป็นของ NPC หรือป้ายสำคัญ
- อาวุธใช้วัสดุพื้นฐานที่มีใน 1.16.5 และรหัส item ฝั่ง server สเตตัสไม่ขึ้นกับชื่อ/lore ที่ client ส่งมา
- มีเมนู inventory ภาษาไทยแบบ vanilla ใช้ได้แม้ไม่โหลด pack
- ถ้าทำโมเดลกำหนดเอง ต้องมี pack แยกตาม client group และตรวจ fallback แต่ไม่ต้องเห็นเหมือนกันทุกพิกเซล
- บล็อกใหม่ที่ protocol map ให้เก่ามองเห็นทดแทนได้เป็นเพียง fallback ไม่ใช้เป็นวัสดุหลักถ้าต้องการเมืองเหมือนกันทุก client

## 5. Bedrock เป็น Phase ภายหลัง

ถ้าจะเพิ่มภายหลัง ใช้ Geyser + Floodgate โดยตรวจรุ่นที่รับ ณ เวลาติดตั้ง [Geyser supported versions](https://geysermc.org/wiki/geyser/supported-versions/) ระบุขณะตรวจว่ารองรับ Bedrock 26.30–26.51 และจำลอง Java 26.2; Bedrock รุ่นเก่าไม่อยู่ในนโยบายรองรับแบบ Java

[Geyser custom items](https://geysermc.org/wiki/geyser/custom-items/) ต้องมี mapping และ Bedrock pack แยก Geyser ไม่แปลง Java pack ให้เองอัตโนมัติ ส่วน [Floodgate](https://geysermc.org/wiki/floodgate/) ใช้ช่วยการเข้าระบบด้วยบัญชี Bedrock

- สำรอง action ID ของทุกเมนูไว้ให้ adapter เปิด Bedrock form ภายหลัง
- ทุก action มีคำสั่ง/NPC/เข็มทิศเป็นทางเข้า ไม่บังคับ Shift+F อย่างเดียว
- เก็บผู้เล่นด้วย UUID ไม่ใช่ชื่อที่มี prefix; การ link account ต้องมี migration และป้องกันรวมรางวัลซ้ำ
- สิทธิ์และสเตตัสคิดฝั่ง server เหมือน Java แต่ต้องทดสอบ combat/control และ resource pack อีกชุด
- ถ้าเปิด network/proxy ภายหลัง ให้ตั้ง forwarding/การเข้าถึง backend ตามคู่มือของ proxy
- ไม่เปิด Bedrock ในรอบแรกหาก item, เมนู หรือการต่อสู้ยังไม่ผ่านเกณฑ์ Java

## 6. โครงสร้าง FantasyCore ที่ควรเขียน

เริ่มเป็น plugin เดียวแยก package ตามหน้าที่ ไม่แตก microservice หลายตัวตั้งแต่ต้น ชื่อเหล่านี้เป็นข้อเสนอออกแบบ ไม่ใช่ plugin ที่มีให้ดาวน์โหลดแล้ว

| โมดูล | งาน |
|---|---|
| PlayerProfile | UUID, แรงค์, title ที่เลือก, tutorial flags |
| EconomyService | gold wallet, gold bank, red wallet, ledger, Vault bridge |
| ItemAdapter | item ID/version/serial, metadata, MMOItems integration |
| CraftRepairService | สูตรคราฟต์ อัปเกรด ซ่อม ราคาและยืนยัน |
| BossRewardService | encounter, contribution, ranking, eligibility, mail reward |
| QuestRewardAdapter | แปล completion ของ quest engine เป็น reward operation |
| RewardService | starter/daily/online/Discord/AFK/virtual keys |
| MarketService | system shop, buy orders, escrow, expiry, returns |
| GuildService | สมาชิก/บทบาท/คะแนนเมื่อกำหนดสูตรแล้ว |
| MenuService | action IDs, permission, validation, feedback |
| TravelService | spawn/home/warp ตรวจปลายทางและ combat lock |
| PetService | capture/ownership/summon เมื่อเลือก plugin/framework แล้ว |
| Operations | transaction audit, compensation, config validation |

ไม่เขียน movement ด้วย packet/NMS เพื่อระบบพื้นฐาน ใช้ API ที่ backend รองรับ ค่าถาวรของ item ใช้ PDC/server-side metadata และ API ของผู้ให้ระบบ item ดู [Paper PDC](https://docs.papermc.io/paper/dev/pdc/) การระบุชนิดไอเทมด้วยชื่อหรือ lore อย่างเดียวไม่เหมาะกับของที่ผู้เล่นซื้อขายได้

### ข้อมูลที่ต้องเก็บ
- players: UUID, profile version, rank
- wallets: UUID, currency, bucket, balance
- ledger: operation ID, account, delta, reason, reference, timestamp
- pending_operations: intent, state, escrow reference, compensation
- claims: UUID, reward type, period ID; unique key ป้องกันรับซ้ำ
- item_instances: serial สำหรับอุปกรณ์สำคัญ template/version และสถานะ
- boss_encounters/contributions/reward_claims
- orders/order_fills/escrow
- mailbox/deliveries
- guilds/members/score_events
- jobs เก็บโดย Jobs Reborn ไม่คัด XP อีกชุดใน Core โดยไม่มีเหตุผล

เซิร์ฟเวอร์เดียวใช้ SQLite พร้อม transaction ได้ หากขยายหลาย backend ให้ย้ายเป็น MariaDB/PostgreSQL ตามผลทดสอบและแผน migration ไม่มี Redis ที่จำเป็นสำหรับเวอร์ชันแรก เงินเป็นจำนวนเต็มหรือ fixed-point ไม่ใช้ floating point โดยไม่กำหนดกติกาปัดเศษ

### ธุรกรรมกับของใน inventory
SQL transaction ไม่สามารถทำ atomic ร่วมกับ inventory Minecraft โดยอัตโนมัติ ต้องมี journal, escrow, ขั้นส่งของ และการกู้คืน:
1. ตรวจสิทธิ์ โลก ยอดเงิน สูตร และ item จากฝั่ง server
2. ล็อก action ต่อผู้เล่นและออก operation ID; double click ใช้ผลเดิม
3. บันทึก intent ก่อนย้ายไอเทมเข้าพื้นที่ escrow ที่ออกแบบให้กู้คืนได้
4. ทำการย้าย item บน server thread และบันทึกสถานะเพื่อ reconciliation
5. commit เงิน/ผลลัพธ์ในฐานข้อมูล แล้วส่ง item/reward ผ่าน delivery ที่มีรหัสอ้างอิง
6. inventory เต็มให้ส่ง mailbox ไม่ทำของตกพื้นโดยไม่ตั้งใจ
7. crash/reconnect ต้องมี recovery ตรวจทั้ง item serial, escrow และ pending operation; ไม่อ้างว่ามี SQL แล้วไม่มีโอกาส dupe
8. ปิด plugin หรือขาด dependency ให้หยุดบริการนั้นแบบ fail closed ไม่หักเงินต่อ

อ่านฐานข้อมูล async ได้ แต่การแก้โลก/ผู้เล่น/inventory ต้องใช้ thread ที่ API กำหนด ดู [Paper scheduler](https://docs.papermc.io/paper/dev/scheduler/) ห้ามใช้ async ครอบทุก Bukkit API เพื่อหวังแก้ lag

## 7. สเปกของระบบหลักที่ต้องทำให้ตรงกัน

### เงินและธนาคาร
- gold.wallet = ทองที่พก; gold.bank = เงินฝาก; red.wallet = เงินแดง
- รับขายของ/รางวัลทั่วไปเป็นทอง รับกิจกรรมยากตามสเปกเป็นเงินแดง
- ฝากทองเป็นการย้ายระหว่าง wallet กับ bank ใน transaction เดียว ไม่มีการสร้างเงินใหม่
- ตั้ง death loss เริ่มต้นเพื่อจำลองฉากตัวอย่างเป็น 30% ของ gold.wallet ปัดลง เงินฝากและเงินแดงไม่เสีย
- ค่านี้เป็นข้อเลือกออกแบบจากฉากตัวอย่าง ต้องมี config ต่อโลกและทำให้ป้ายในธนาคารตรงกัน
- ตายไม่ทำ item drop ในโลก RPG ที่กำหนด; ไม่เขียน handler หักเงินซ้ำเมื่อ event อื่นเรียก
- permission ฝากผ่านคำสั่งปลดล็อกเมื่อถึงแรงค์ ไม่ให้เมนูที่อื่นข้ามเงื่อนไข
- ประวัติฝากถอนมีเวลาพร้อม timezone ที่กำหนดและ reference สำหรับแอดมินค้น
- ไม่เพิ่มดอกเบี้ย/เงินกู้โดยอ้างว่ามีในคลิป

### คราฟต์และซ่อม
- recipe ID กำหนดวัสดุ เงิน และ output; ใช้ ID ไอเทมจริงจาก provider
- ซ่อมอ่าน durability/stats ผ่าน adapter ของ item framework
- ตัวอย่าง 500 ทองในคลิปเป็นราคาของชิ้นหนึ่ง ไม่บังคับทั้งเกมราคาเดียว
- การซ่อมต้องคง enchant, stats, owner/bind, gems และ serial ที่กำหนด
- อุปกรณ์เต็ม durability ไม่เสียค่าซ่อมซ้ำ
- GUI แสดงก่อน/หลัง ต้นทุน และสถานะ “ยืนยัน” แยกจาก preview
- native anvil/Essentials repair/คำสั่งแอดมินที่ผู้เล่นเข้าถึงได้ต้องไม่ข้ามกติกา
- ตั้งเป้าชุดคราฟต์ระดับปลายทางเก่งกว่าชุดเริ่มต้น/สนับสนุนตามที่ผู้เล่ากล่าว ไม่เดาค่า stat จากภาพ

### ดันเจี้ยนและบอส
- เริ่มด้วย dungeon tiers จำนวนจำกัด แต่ขยายผ่าน data config ได้
- บอสหลัก 3 encounter IDs ตารางเกิดเป็นข้อมูลรายตัว; ช่วง 1–3 ชั่วโมงจากคลิปไม่ยืนยันแต่ละตัว
- contribution นับดาเมจจริงหลัง mitigation จำกัดไม่เกิน HP ที่หักจริง และกำหนด credit ของ pet/projectile
- กันเครดิตจากม็อบอื่น NPC ซ้อม ความเสียหายที่ถูกยกเลิก และการนับ event ซ้ำ
- เกณฑ์ตัวอย่าง 500 ดาเมจ ตรวจว่า >=500 หรือ >500 เป็นข้อกำหนดก่อนเปิดจริง
- Top 10 ได้ bonus เมื่อจบ encounter เท่านั้น tie-break ระบุชัดและคงที่
- item reward เข้าตัว/mailbox ตามรหัส encounter+UUID ป้องกันรับซ้ำ
- reconnect หลังจบบอสไม่ทำเครดิตหายหรือได้ซ้ำ
- timer เก็บเวลาจริงและ state หลัง restart; tick lag ไม่ทำเวลาบอสเลื่อนโดยไม่ตั้งใจ
- การตาย/หนี/หมดเวลา/แอดมิน kill ต้องมีผลรางวัลตามกติกาที่แยกกัน

### Rewards
- starter และ Discord reward รับครั้งเดียวต่อ identity ที่กำหนด
- daily ใช้วัน Asia/Bangkok; เปลี่ยนเครื่อง/ชื่อไม่ reset
- online reward นับเวลาออนไลน์ตามเงื่อนไขจริง ไม่ใช้เวลาหลัง login อย่างเดียว
- AFK ตัวอย่าง 1 red ต่อ 1,800 วินาทีใน region; ไม่ควรใช้ตรวจไม่ขยับแล้วตัดรางวัล เพราะระบบนี้ตั้งใจให้ AFK
- ต้องเลือกนโยบายสะสม/ต่อเนื่องและหลายบัญชีเอง คลิปไม่บอกครบ
- การ reboot ไม่ให้ย้อนหลังตามเวลา offline
- keys เลือก physical หรือ virtual เป็นเจ้าของสิทธิ์แหล่งเดียว
- crates/title draw ต้องมีตารางโอกาส preview และ duplicate policy ถ้าเลือก CrazyCrates ให้ Core adapter ใช้บริการนั้น ไม่สุ่มอีกชั้นพร้อมกัน

### ตลาดและออเดอร์
- system shop แยกจาก player order; การตั้งซื้อกับประมูลเป็นคนละระบบ
- ผู้ตั้งออเดอร์ล็อกเงินจริงเข้าบัญชี escrow ตอนสร้าง
- ผู้ส่งของเห็นจำนวนที่รับได้ ราคา และ ID ไอเทมที่รับ
- partial fill ต้องตัดส่วนที่รับได้จริงใน transaction ที่มี version/lock
- หมดอายุ/ยกเลิกคืนเงินคงเหลือครั้งเดียว
- ของมี lore คล้ายกันแต่ ID ต่างกันไม่ผ่านเงื่อนไข
- auction house และ direct trade เป็นชุดเพิ่มเติม ต้องมี transaction service เช่นเดียวกัน

### Guild / jobs / land / cosmetics
- กิลด์: สมาชิก บทบาท และคะแนนเริ่มแบบง่าย สูตรคะแนนกำหนดเองและใช้ event ที่ตรวจสอบได้
- Jobs: แยก XP ของอาชีพออกจาก rank; ไม่จ่ายจากการวางแล้วขุดบล็อกเดิมซ้ำโดยไร้เงื่อนไข
- Protect: ทดสอบเพื่อนร่วมพื้นที่ การรื้อบล็อก การขยาย และ world whitelist
- คอสเมติก/title: เป็นสิทธิ์ server-side ลูกค้าปลอมชื่อ item ไม่ได้สิทธิ์
- Pet: capture cost ตัวอย่าง 20 red เป็นค่า config เมื่อเลือกสูตรแล้ว; summon จำกัดต่อคนและเครดิตบอสสัมพันธ์กับกติกา
- Furniture: ใช้ vanilla ก่อน ถ้าผู้เล่นวางในบ้านต้องบันทึก ownership และป้องกัน dupes/region bypass

## 8. บริการ Fantasy เพิ่มเติม — รายการครบสำหรับเลือกใช้

รายการนี้คือชุดบริการสำหรับเซิร์ฟแนวไทยภาษาไทย ไม่ใช่สถิติว่าทุกเซิร์ฟไทยใช้ปลั๊กอินเหล่านี้ และหลายรายการไม่มีหลักฐานในคลิป

| บริการ | โซน | วิธีเริ่ม | สถานะ |
|---|---|---|---|
| ธนาคาร/ประวัติเงิน | 05 | Core Economy | หลัก |
| คราฟต์อาวุธ/เซต | 07 | MMOItems adapter + Core | หลัก |
| ซ่อม/แจ้งเตือนของใกล้พัง | 07 | Core Repair | หลัก |
| เควสทอง/แดง | 03 | Core หรือ BetonQuest adapter | หลัก |
| อาชีพ/XP | 08 | Jobs Reborn | หลัก |
| กิลด์/อันดับ | 04 | Core simple guild หรือ candidate ที่ตรวจแล้ว | หลัก |
| โลกฟาร์ม/บ้าน/Protect | 09 | Essentials utility + ProtectionStones adapter | หลัก |
| ดันเจี้ยน/บอส/อันดับดาเมจ | 09 | MythicMobs + Core | หลัก |
| Shop และออเดอร์รับจ้าง | 06 | Core Market | หลัก |
| เฟอร์นิเจอร์/หัวตกแต่ง | 06 | vanilla stock + Core shop | หลัก |
| สัตว์เลี้ยง/ตกปลา | 10 | Core pet adapter + vanilla fishing/Jobs | หลัก |
| ล็อกอิน/ออนไลน์/Discord | 02 | Core Reward | หลัก |
| หีบ/ฉายา | 02 | Core หรือ CrazyCrates integration เลือกเจ้าของเดียว | หลัก |
| คอสเมติก/สนับสนุน | 02 | entitlement + LuckPerms | หลัก |
| แรงค์/สิทธิ์ความสะดวก | 04 | Core progression + LuckPerms | หลัก |
| AFK reward | 11 | Core region reward | หลัก |
| PvP entry | 09 | Travel + WorldGuard + กติกาสนาม | หลักเมื่อพร้อม |
| ประมูลผู้เล่น | 06 | Core Auction หรือ plugin ที่ API/escrow ผ่านทดสอบ | เสริม |
| เทรดต่อหน้า | 06 | Core trade escrow | เสริม |
| ร้านปรุงยา/อาหารบัฟ | 06 | MMOItems recipes + Core shop | เสริม |
| ห้องสมุดสูตร/บันทึกมอนสเตอร์ | 04 | Menu catalog เปิดจาก NPC | เสริม |
| ตีบวกอาวุธ | 07 | Core upgrade rules + item adapter | เสริม; clip ไม่เผยสูตร |
| รูน/อัญมณี/ถอดอัญมณี | 12 | Core socket/rune rules | อนาคต; ยังไม่มีในคลิป |
| สุ่มสเตตัส | 07/12 | Core reroll ยืนยันราคา/โอกาส | เสริม |
| หุ่นทดสอบ DPS | 08 | Dummy + bounded damage session | เสริม |
| ปาร์ตี้/หาเพื่อนลงดัน | 09 | Core party roster | เสริม |
| ร้านแผนที่/วาร์ปเร็ว | 09 | Travel permissions | เสริม |
| ศาลาฟื้นฟู/กลับเมือง | 11 | safe-region heal cooldown | เสริม; ไม่เพิ่ม resurrection fee โดยอ้างคลิป |
| Mailbox/รับของค้าง | 02 | Core delivery | จำเป็นเพื่อความทนทาน |
| กระดานเทศกาล/เควสรายสัปดาห์ | 03 | scheduled content + claim period | เสริม |
| หัวก้อยด้วยเงินในเกม | 06 | wager escrow + fairness log | ตามคลิป; เลือกเปิดและทดสอบแยก |
| เบ็ดเคลื่อนที่ | 08/09 | movement module ต้องทดสอบ anti-cheat | ทดลองเท่านั้น |
| งานตกแต่งโมเดลขั้นสูง | ทุกโซน | Oraxen/ItemsAdder อย่างใดอย่างหนึ่งเมื่อผ่าน matrix | เสริม ไม่บังคับโหลด pack |

อย่าลงทั้ง Oraxen และ ItemsAdder หรือหลาย economy/menu/crate framework โดยไม่มีเจ้าของข้อมูลชัด รายการ optional ต้องยืนยัน license, supported backend และ API ของรุ่นที่จะใช้ก่อนซื้อ

## 9. ลำดับพัฒนาที่รักษาคุณภาพ

1. ทำ PoC เวอร์ชัน: เข้า client backend และ 1.16.5, NPC เปิด GUI, item template 1 ชิ้น, WorldGuard และ home
2. ล็อก backend/Java/plugin build และเก็บ compatibility manifest
3. ทำ economy+bank+ledger+death พร้อม admin audit
4. ทำ item adapter+recipe+repair+mailbox ให้ผ่าน crash/double-click cases
5. ทำ quest gold/red และ Jobs payout โดยเจ้าของเงินรายเดียว
6. ทำ dungeon 1 tier และ boss 1 encounter ก่อนขยาย 3 บอส
7. ทำ rewards/keys/titles และ order escrow
8. เชื่อม NPC ทุกโซนพร้อมข้อความภาษาไทยสม่ำเสมอ
9. เพิ่ม guild/pets/furniture หลังระบบรายการแรกผ่าน
10. เปิด optional fantasy ทีละบริการ ตรวจเศรษฐกิจและ client matrix ใหม่เฉพาะส่วนที่เปลี่ยน
11. เพิ่ม Bedrock เมื่อ Java เปิดใช้ตามเกณฑ์แล้ว และแยก validation อีกชุด

## 10. Acceptance tests ที่ต้องผ่านก่อนเปิด

- คนใหม่รับ starter/Discord/daily ซ้ำด้วย reconnect หรือชื่อใหม่ไม่ได้
- ฝากถอน double-click แล้วไม่มีเงินเพิ่ม/หาย และ bank ไม่ถูกหักจาก death
- คราฟต์ขณะ inventory เต็มส่งของครบผ่าน mailbox
- ซ่อมคง metadata/stats/enchant/serial และไม่หักค่าของเต็ม durability
- ยิง projectile และ pet ตีบอสได้เครดิตตรงกติกา ไม่มีนับสองครั้ง
- Top 10 และผู้มีดาเมจถึงเกณฑ์รับรางวัลครั้งเดียวต่อ encounter
- crash ระหว่างซื้อ คราฟต์ ส่ง order รับ reward มีผลลัพธ์ที่ตรวจสอบและกู้คืนได้
- ออเดอร์สองคนส่งพร้อมกันไม่ทำยอดติดลบ และ refund ไม่ซ้ำ
- GUI shift-click, drag, double click, hotbar swap ไม่ย้าย item ของเมนูออกมาได้
- เงิน/รางวัลยังถูกเมื่อ DB ช้า หยุดบริการอย่างชัดเจนเมื่อ storage unavailable
- client 1.16.5 เล่นเส้นทางหลักครบ แม้ไม่ใช้ pack
- client รุ่นมาตรฐานและรุ่นกลางมีตัวเลข/สิทธิ์เท่ากัน
- คำสั่ง utility ไม่ข้ามราคา repair, death policy หรือ rank
- ปลายทาง world ไม่ส่งเข้า void และ PvP ไม่ถูกเปิดโดยเดินผ่านถนนเมือง
- ความล้มเหลวทุกแบบมีข้อความไทยที่บอกผู้เล่นว่าต้องทำอะไร ไม่แสดง stacktrace
- staging มีการทดสอบสำรองและกู้ข้อมูลก่อนเปิดจริง

## 11. สิ่งที่ยังไม่สามารถอ้างว่า “เหมือนเป๊ะ” ได้

สูตร stat อาวุธ รายชื่อชุด armor/drop, boss skill/timer รายตัว, probability ของ crates/title/pets, daily rewards, rank thresholds, guild scoring และ command/menu ทุกหน้าของต้นฉบับยังไม่มีข้อมูลครบ ต้องใส่ลง content specification เพิ่มก่อนเรียกว่าจำลองต้นฉบับทั้งหมด

สร้างระบบหลักและ flow ให้ตรงหลักฐานในคลิปได้ แต่ไม่สามารถอนุมาน plugin list หรือ config ส่วนตัวจากวิดีโอเพียงคลิปเดียว

## เอกสารประกอบ

- [ผังและภายในทุกโซน](INTERIOR-AND-MAP-PLAN-th.md)
- [ภาพแยกโซน](ZONE-ILLUSTRATIONS-th.md)
- [แหล่งทางการของปลั๊กอิน](PLUGIN-RESEARCH-SOURCES-th.md)
- [คู่มือตกแต่งโครงสร้างเดิม](LOBBY-DECORATION-COMPACT-th.md)
