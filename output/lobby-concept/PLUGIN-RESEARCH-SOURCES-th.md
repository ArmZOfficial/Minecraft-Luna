# ผลวิจัยปลั๊กอินสำหรับ Fantasy MMO RPG Lobby

ตรวจสอบเอกสารผู้พัฒนาและซอร์สโครงการวันที่ **5 ตุลาคม 2026** สำหรับแผน Java-first; ตั้งเป้าไคลเอนต์ Java 1.16.5 ขึ้นไปบน backend รุ่นใหม่ที่ชุดปลั๊กอินรองรับร่วมกัน ส่วน Bedrock เป็นงานเพิ่มภายหลัง

นี่คือชุดเครื่องมือที่สามารถนำมาสร้างพฤติกรรมคล้ายคลิป [SIXPIXEL อ้างอิง](https://www.youtube.com/watch?v=rKIdKLOJPSY) ไม่ใช่หลักฐานว่าเซิร์ฟเวอร์ในคลิปใช้ปลั๊กอินเหล่านี้จริง และไม่ใช่การรับรองว่าติดตั้งรายการนี้แล้วจะได้ระบบเหมือนคลิปทันที สูตรเศรษฐกิจ เนื้อหาเควส สกิล ตารางดรอป กติกา และหน้าตาเมนูต้องออกแบบ/config/เขียนเพิ่มเติม

## 1. ชุดหลักที่ควรใช้

| งาน | เครื่องมือ | ความสามารถที่ตรวจสอบแล้ว / งานที่ต้องเพิ่ม | แหล่งผู้พัฒนา |
|---|---|---|---|
| สิทธิ์ผู้เล่นและทีมงาน | LuckPerms | สร้างกลุ่มและ permissions; ใช้กำหนดสิทธิ์ NPC, วาร์ป, ร้านค้า และฉายา ไม่ใช่ระบบกิลด์ | [README / MIT](https://github.com/LuckPerms/LuckPerms) |
| เงินหลักเริ่มต้น | EssentialsX | มี economy API และคำสั่งพื้นฐาน วาร์ป บ้าน kit; เป็นทางเลือกของ **ผู้เก็บยอดเงินจริง** ถ้า custom core เป็นเจ้าของยอดเงินแล้วต้องปิดการจ่ายเงินซ้ำซ้อน | [EssentialsX](https://essentialsx.net/) |
| เชื่อมเศรษฐกิจกับปลั๊กอินอื่น | Vault / VaultAPI | เป็น bridge/API ให้ระบบเศรษฐกิจและ permissions; **Vault ไม่ใช่ธนาคารและไม่ได้สร้างยอดเงินเอง** | [Vault README / LGPL-3.0](https://github.com/MilkBowl/Vault) |
| NPC ประจำร้านและบริการ | Citizens | NPC สามารถเรียกคำสั่งเมื่อคลิก มี cooldown, permission, cost; NPC เป็นจุดเข้าบริการ ไม่ใช่ตัวตรรกะธนาคาร/เควสทั้งหมด | [Citizens Commands](https://wiki.citizensnpcs.co/Commands) |
| ป้องกันเมืองและกำหนดกติกาโซน | WorldGuard | region, ห้าม PvP/TNT/ความเสียหายบางชนิด, จำกัดการก่อสร้าง; claim ผู้เล่นต้องมีระบบอนุญาต/ขนาด/ราคาเพิ่ม | [WorldGuard](https://worldguard.enginehub.org/en/latest/) |
| เครื่องมือทีมสร้างแมพ | WorldEdit | แก้ไขพื้นที่ คัดลอก วาง schematic สร้างรูปทรงและ terrain | [WorldEdit](https://worldedit.enginehub.org/en/latest/) |
| ข้อมูลในป้ายและเมนู | PlaceholderAPI | ส่งข้อมูลจากปลั๊กอินหนึ่งไปแสดงในอีกปลั๊กอิน; custom core ต้องเขียน expansion สำหรับยอดธนาคาร ออร์เดอร์ contribution ฯลฯ | [PlaceholderAPI Wiki](https://wiki.placeholderapi.com/) |
| อาชีพหาเงิน | Jobs Reborn | ค่าตอบแทน break/place/kill/fish/craft, XP/level อาชีพ, daily quests, GUI, income limit, MythicMobs support; สูตรรายได้และการป้องกันฟาร์มต้องตั้งเอง | [Jobs Reborn ผู้พัฒนา](https://www.zrips.net/jobs/) |
| เควสและบทสนทนา | BetonQuest | เควสแบบสคริปต์และ RPG conversations กับ NPC; ให้เป็นเจ้าของ quest progress แล้วใช้ adapter เรียก core ตอนจ่ายรางวัล | [BetonQuest README / GPL-3.0](https://github.com/BetonQuest/BetonQuest) |
| มอนสเตอร์และบอส | MythicMobs | HP/damage, skills, conditions, triggers, spawners, loot และ AI; **ไม่ใช่ระบบดันเจี้ยน instance ทั้งหมด** และ reward contribution เฉพาะเซิร์ฟต้องออกแบบเพิ่ม | [MythicMobs Home](https://wiki.mythiccraft.io/mythicmobs/home) |
| อุปกรณ์ RPG | MMOItems + MythicLib | item stats, tiers, sets, gems, upgrades, custom durability, crafting stations; MythicLib เป็น dependency ของ MMO suite ต้องเลือก build ที่เข้าชุด | [MMOItems Wiki](https://git.mythiccraft.io/root/mmoitems/-/wikis/home), [MythicLib หน้าผู้พัฒนา](https://www.spigotmc.org/resources/mmolib-mythiclib.90306/) |

เลือก backend ก่อน แล้วทำรายการ version/build ที่ทดสอบร่วมกันจริง ไคลเอนต์ 1.16.5+ ไม่ได้แปลว่าต้องใช้ปลั๊กอิน server รุ่น 1.16.5; ฝั่ง protocol และ backend เป็นคนละชั้น การรับ client เก่าต้องทดสอบภาพบล็อก เมนู item model เสียงและการต่อสู้

## 2. ปลั๊กอินเสริมที่ควรประเมินเป็นรายระบบ

| ระบบ | ตัวเลือก | ใช้ได้ตรงไหน | ข้อจำกัดในการตัดสินใจ |
|---|---|---|---|
| กิลด์ | Guilds (GlareMasters) | มีบทบาทสมาชิก home, guild bank, vault, GUI, WorldGuard claim และคำสั่ง arena | [Commands](https://wiki.glaremasters.me/guilds/commands), [Roles](https://wiki.glaremasters.me/guilds/features-w.i.p/fully-customizeable-roles); wiki บางหน้ามีอายุหลายปี ต้องยืนยัน build/API/เงื่อนไขการซื้อปัจจุบัน ก่อนผูกกับ core |
| หีบรางวัล | CrazyCrates | virtual/physical keys, preview, weighted prizes, commands และแอนิเมชันหลายแบบ | [เอกสาร](https://docs.crazycrew.us/mods/crazycrates/), [commands](https://docs.crazycrew.us/mods/crazycrates/reference/commands/), [source / MIT](https://github.com/Crazy-Crew/CrazyCrates); รุ่นปัจจุบันระบุ Paper และต้องเลือก release ตาม backend |
| สัตว์เลี้ยง | MyPet v4 หรือ custom pet module | สาย MyPet มีแนวทางจับ/ฝึก companion; ถ้าต้องการโบนัสและ progression เฉพาะคลิปให้ประเมิน API v4 หรือเขียน module ที่ใช้ MythicMobs เป็นตัว spawn/skills | [MyPet official source](https://github.com/MyPetORG/MyPet) ระบุ **3.x end of life** และให้ย้าย v4; ห้ามเลือก 3.x เพียงเพราะเป็น free build เก่า ส่วนรายละเอียดและ license ของ v4 ต้องตรวจในหน้าผู้ขายอีกครั้ง |
| ดันเจี้ยน instance | MythicDungeons หรือ custom instance controller | official suite อธิบาย instanced dungeons, triggers, conditions, scripted events และ party support | [MythicCraft Wiki index](https://wiki.mythiccraft.io/); เลือกตัวเดียวให้เป็นเจ้าของ lifecycle ของ instance, reset, party และ lockout |
| อาชีพต่อสู้/classes | MMOCore หรือ MythicRPG หลังประเมิน | ใช้เมื่อ design ระบุ class/skill tree ชัดเจน; ไม่จำเป็นต้องลงเพียงเพราะมี MMOItems | [MMOItems Wiki](https://git.mythiccraft.io/root/mmoitems/-/wikis/home), [MythicRPG ใน official index](https://wiki.mythiccraft.io/); อย่าให้สองปลั๊กอินเป็นเจ้าของ combat progression พร้อมกัน |
| เฟอร์นิเจอร์และ custom items | Oraxen **หรือ** ItemsAdder **หรือ** MythicCrucible | เสริมภาพ Fantasy ผ่าน resource pack; เลือกผู้สร้าง pack หลักเพียงตัวเดียว แล้วเชื่อมระบบไอเท็ม RPG | [Oraxen Docs](https://docs.oraxen.com/), [ItemsAdder Geyser FAQ](https://wiki.itemsadder.com/faq/geyser/), [MythicCrucible official index](https://wiki.mythiccraft.io/) |
| หมวก/ปีก/emote/cosmetics | MythicCosmetics หรือ core wardrobe | official suite ระบุ hats/backwear/emotes/mounts/particles; wardrobe ของ core เก็บสิทธิ์/ไอเท็มที่ปลดล็อก | [MythicCraft official index](https://wiki.mythiccraft.io/); ตรวจ integration และรุ่น client ก่อนซื้อ assets |

ชื่อปลั๊กอินแบบจ่ายเงินเป็น **candidate** ไม่ใช่รายการต้องซื้อทุกตัว MythicMobs มีเส้นทาง free/premium; development builds ฝั่ง premium มีคำเตือนเรื่องความไม่เสถียรในเอกสาร หลีกเลี่ยงเลือก dev build เป็น production โดยไม่มีเหตุผล MMOItems, Citizens, Oraxen และส่วนเสริม commercial ต้องตรวจช่องทาง download/license/support ปัจจุบันของแต่ละผู้พัฒนา ไม่อ้างราคาเก่าและไม่แจก jar เชิงพาณิชย์ในแพ็กเซิร์ฟเวอร์

## 3. ส่วนที่ควรเขียนเป็น FantasyCore

ข้อความส่วนนี้เป็น **ข้อเสนอทางสถาปัตยกรรม** จากการเทียบหน้าที่ปลั๊กอิน ไม่ใช่ feature ที่ตรวจยืนยันจากตัวเซิร์ฟเวอร์ในคลิป

### เศรษฐกิจและธนาคาร

เลือกหนึ่งในสองแนวทาง:

1. **เริ่มง่าย:** EssentialsX เป็นผู้เก็บ wallet เงินหลัก; Vault ให้ระบบอื่นใช้งาน; core เก็บ bank และ currency รอง การย้าย wallet → bank ต้องมี journal และ compensating transaction เพราะ API เงินกับฐานข้อมูลธนาคารไม่ใช่ SQL transaction เดียวกัน
2. **เหมาะกับเซิร์ฟเวอร์ที่ต้องการระบบเนี้ยบ:** core เป็นผู้เก็บ wallet/bank/currency ทุกชนิดในฐานข้อมูลเดียว และเปิด Vault Economy provider เฉพาะเงินหลัก ปลั๊กอินภายนอกเข้าผ่าน provider นี้ ปิด/ไม่ใช้ EssentialsX economy ในฐานะผู้เก็บยอดอีกชุด

core ควรมี currency ID, จำนวนเต็มหน่วยย่อย, account wallet/bank, transfer, statement, death penalty policy, transaction ID, idempotency, audit log และ admin rollback ที่อ้าง transaction เดิม กำหนดให้ตายหักจาก wallet เท่านั้นหรือรวม bank ตามกติกาที่ตกลง ห้ามใช้งาน NPC command chain `withdraw` แล้ว `deposit` โดยไม่มีการกู้คืนเมื่อกระบวนการขัดข้อง

### ตลาดรับออร์เดอร์

แยก **ร้านขายของ, auction และ buy orders** ให้ชัด ออร์เดอร์รับซื้อจำเป็นต้องมี escrow เงินก่อนเปิดคำสั่งซื้อ, item matcher ที่ตรวจชนิด MMOItems/tier/custom data, partial fill, expiry, refund และ claim box เมื่อ inventory เต็ม การประกาศผ่าน GUI เป็นเพียงหน้าจอ ไม่ใช่ตัวระบบ escrow หากปลั๊กอินตลาดที่เลือกไม่มี API และ semantics เหล่านี้ ให้เขียน core module

### ซ่อมและตีบวก

MMOItems มี custom durability และ repair consumables อยู่แล้ว จึงไม่ควรเขียนระบบ durability ซ้ำ ดู [implementation repair ของ MMOItems](https://gitlab.com/phoenix-dvpmt/mmoitems/-/blob/499538d8a7781e27c2c1ca794e570279b95010d7/MMOItems-API/src/main/java/net/Indyuce/mmoitems/stat/RepairPower.java)

core เพิ่ม NPC service ที่คำนวณราคา ตรวจ item identity/owner/bound state/วัสดุ ขอคำยืนยัน และเรียก API ที่เหมาะสมของ build ที่ใช้ ต้องรักษา tier, rolls, gems, upgrade, skills และ soulbound; อย่าใช้การสร้าง item ใหม่จาก template เพื่อซ่อม เพราะอาจล้าง rolls ของผู้เล่น อย่าเปิด `/repair` แบบทั่วไปเป็นช่องทางข้ามค่าใช้จ่ายของ custom items

### บอส ดันเจี้ยน และความยุติธรรม

MythicMobs ดูแลมอนสเตอร์/สกิล core ดูแล combat session, contribution ที่เป็น effective damage, เจ้าของ projectile/pet, disconnect grace, minimum participation, reward tiers และการจ่ายรางวัลครั้งเดียว ตรวจ API combat ของ MythicLib/MMOItems ที่ใช้จริง เพราะค่าดาเมจจาก event Vanilla อาจไม่สะท้อนทุกสกิลในชุด RPG

instance controller หนึ่งตัวเป็นเจ้าของ party slot, teleport, reset, cooldown และ cleanup ส่วน core เป็นเจ้าของ entry fee/reward ledger ไม่เปิดบอสจริงไว้ใน lobby ที่มีธนาคารและบริการ

### รางวัลออนไลน์ AFK และฉายา

online reward เป็น progress ตาม active playtime และ claim history; daily reset ต้องใช้ timezone เดียวกัน AFK reward แยกกติกาและ currency ชัด ตรวจ activity ที่มีความหมายแทนการสั่นตำแหน่งหรือหมุนกล้องอย่างเดียว ชื่อฉายาเป็น entitlement ที่บันทึกใน core แล้วแสดงผ่าน LuckPerms/PlaceholderAPI ตาม adapter การจ่าย crate key, item และเงินต้องรองรับ retry โดยไม่แจกซ้ำ

### สัตว์เลี้ยง ตกปลา และของตกแต่ง

Jobs Reborn จ่ายค่าอาชีพตกปลาได้ แต่ collection ปลา, rarity, quest fish, tournament และร้านแลกเฉพาะต้องมี config/addon/core เพิ่ม สัตว์เลี้ยงแยกโบนัส gameplay กับรูปลักษณ์เพื่อ balance ได้ ตกแต่งบ้านแยกสิทธิ์ซื้อ furniture กับ permission วางใน plot และตรวจการคืนไอเท็มเมื่อรื้อ ป้องกันการ duplicate ผ่าน break/drop/restart

## 4. ข้อจำกัดภาพและการเล่นข้ามรุ่น

- **Java 1.16.5+ เป็น target ที่ต้องพิสูจน์:** สถิติ RPG และเศรษฐกิจทำงานบน backend ได้ แต่ custom model, item components, display entities และบล็อกใหม่อาจถูกแปลงหรือหายบน client เก่า ให้ทดสอบรุ่นต่ำสุดด้วย pack ที่จัดเตรียมจริงและใช้วัตถุ Vanilla เป็น fallback
- **Bedrock เป็น phase เพิ่ม:** ItemsAdder FAQ ปัจจุบันระบุว่า Geyser ไม่ได้รับ official support จึงไม่ควรให้ Bedrock visual parity เป็นเงื่อนไขหลักของเวอร์ชันแรก [ItemsAdder official FAQ](https://wiki.itemsadder.com/faq/geyser/)
- **ข้อมูล Oraxen เปลี่ยนแล้ว:** เอกสารปัจจุบันระบุรองรับ Bedrock/Geyser ผ่าน **BedrockGen addon** จึงไม่ควรสรุปว่า Oraxen ไม่มีเส้นทาง Bedrock แต่ต้องยืนยัน addon release, supported assets และข้อจำกัดจริง ยังไม่ใช่หลักฐานว่า pack/เฟอร์นิเจอร์/GUI ทุกชนิดจะเหมือน Java [Oraxen official docs](https://docs.oraxen.com/)
- backend เดียวกันไม่ได้รับรอง client experience เดียวกัน ทดสอบ right-click NPC, inventory GUI, drag/shift-click, custom weapon, skill, knockback, pet, reward และ item mapping บนทุกกลุ่ม client ที่ประกาศรองรับ

## 5. เกณฑ์รับงานก่อนเลือกซื้อและเปิดจริง

| ทดสอบ | ผลที่ต้องได้ |
|---|---|
| backend dependency matrix | plugin build ทุกตัวเริ่มทำงานบน backend เดียว ไม่มี required dependency หาย |
| wallet/bank/currency | หลักบัญชีรวมถูกต้อง ฝากถอนพร้อมกันไม่ติดลบ restart ไม่แจกซ้ำ |
| orders | partial fill/refund/expiry ถูกต้อง inventory เต็มยังรับของภายหลังได้ |
| RPG repair | stats, gems, rolls, upgrade และ bound state เหมือนก่อนซ่อมทุกอย่างนอกจาก durability/cost |
| boss reward | effective damage ถูกนับ ผู้เล่น/pet/projectile อ้างเจ้าของถูก retry ไม่ให้รางวัลซ้ำ |
| quests/jobs | หนึ่ง action ไม่จ่ายสองระบบโดยไม่ตั้งใจ เควสสำเร็จแล้วไม่ claim ซ้ำ |
| older Java | 1.16.5 เข้าได้และทำทุกบริการหลักได้ มี fallback เมื่อ model/block ไม่รองรับ |
| server reload/crash | transaction/instance/reward กลับมาสถานะสอดคล้อง ไม่มี orphan escrow |

ล็อก plugin versions และ API contract หลังการทดสอบ ไม่อัปเดตปลั๊กอินต่อสู้/ไอเท็ม/pack ทีละตัวบน production โดยไม่ทดสอบสำเนาเซิร์ฟเวอร์ก่อน
