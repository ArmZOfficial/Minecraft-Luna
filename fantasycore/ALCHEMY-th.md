# ร้านยาไลรา — FantasyCore v0.9

ฉบับ 6 ตุลาคม 2026: **source/JAR build ผ่าน และ Core unit tests 131 กรณีผ่าน**
ยังไม่ได้เปิดทดสอบ Minecraft จริง; ใช้ [checklist T](../server/README-th.md#t-ร้านยาไลรา-v09--ยังรอ-minecraft-จริง) ก่อนเปิดผู้เล่น
Paper 26.2 build 129 / Java 25 / schema 7 เดิม เป้าหมาย client Java 1.16.5+ ผ่าน ViaVersion/ViaBackwards ยังต้องทดสอบ

## 1. ผู้เล่นใช้อย่างไร

ยืนใกล้สถานี `alchemy.main` → คลิกไลรา หรือ `/alchemy` (`/elixir`) หรือปุ่มร้านยาใน `/menu`
เมนู 54 ช่องแสดงขวดยาสีแดง/ฟ้า/ม่วง → เลือกสูตร → อ่านผลยา จำนวนวัตถุดิบที่มี/ต้องใช้/ขาด ราคา และโควตา → ยืนยัน
การเปิดเมนูหรือดู preview ยังไม่หักเงิน/ของ; ตัวเลขวัตถุดิบเป็นภาพ ณ ตอนวาดเมนู ต้องตรวจ inventory จริงอีกครั้งก่อนตัด

ปรุงครั้งละ **1 ขวด** จ่ายเฉพาะ `gold.wallet` และวัตถุดิบ vanilla ที่ไม่มีชื่อ/lore/PDC/meta
ไม่ตัดของจากเกราะ/มือรอง ไม่ใช้ทองฝาก/เงินแดง และไม่เอาขวด custom มาเป็นวัตถุดิบ
ยาที่สำเร็จ **รอรับใน `/mail` ทุกครั้ง** แม้กระเป๋าว่าง; มี template/version/UUID serial ใน PDC และทะเบียน Core
ไอคอนเมนูและขวดในโมเดลเป็นภาพแสดงผล ไม่เป็นเส้นทางแจกของ

| สูตร / ผลลัพธ์ v1 | เอฟเฟกต์จริง | วัตถุดิบ | ค่าทอง | ขวด/วันไทย |
|---|---|---|---:|---:|
| `alchemy_moondew` → `lyra_moondew` น้ำค้างจันทร์ | HEALING / ฟื้นพลัง I | GLASS_BOTTLE 1 + NETHER_WART 2 + GLISTERING_MELON_SLICE 1 | 40 | 12 |
| `alchemy_lifebloom` → `lyra_lifebloom` น้ำยาดอกชีพจร | REGENERATION / ฟื้นฟูต่อเนื่อง I | GLASS_BOTTLE 1 + NETHER_WART 2 + GHAST_TEAR 1 | 65 | 8 |
| `alchemy_starlight` → `lyra_starlight` แสงดาวนักสำรวจ | NIGHT_VISION / มองกลางคืน | GLASS_BOTTLE 1 + NETHER_WART 2 + GOLDEN_CARROT 1 | 60 | 6 |

โควตาต่อ UUID/สูตร ใช้วัน Asia/Bangkok เปลี่ยนเวลา 00:00; เปลี่ยน recipe version ไม่เปิดโควตาใหม่
ใช้ `PotionMeta.setBasePotionType` สร้างผลจริงจาก vanilla type พร้อมสีขวดและ glint; ความแรง/ระยะเวลามาจาก Minecraft ของ backend
อ้างอิง [PotionMeta ทางการ](https://jd.papermc.io/paper/26.1.2/org/bukkit/inventory/meta/PotionMeta.html) และ [PotionType](https://jd.papermc.io/paper/26.1.2/org/bukkit/potion/PotionType.html); source compile กับ API 26.2 ที่ล็อกในโปรเจกต์
ไม่มี custom heal stat, strength, instant heal II, long/splash/lingering หรือ enchant บนยาในรุ่นนี้

ราคานี้เป็นจุดเริ่มต้นสำหรับ playtest ยังไม่ถือว่าบาลานซ์ผ่าน: ลอง gear v1/v2 ในโลกทรัพยากรและ Moonfall เดี่ยว/ปาร์ตี้
บันทึกเวลาหาวัตถุดิบ ยาที่ใช้ต่อรอบ อัตราตายและรายได้ทอง; ปรับราคา/โควตาจากผลจริงโดยไม่เปลี่ยนผลยาของ version ที่ออกไปแล้ว
วัตถุดิบหายากและค่าทองเป็นต้นทุน ยาช่วยเตรียมตัวก่อนลงดัน แต่ไม่เพิ่ม damage ให้ข้ามวงเตือนหรือ gate
vanilla brewing ของยา vanilla ยังใช้งานตามปกติ จึงไม่ใช่สินค้าผูกขาดหรือแพ็กขายเงินจริง

## 2. วางร้านและผูก NPC

![ไลราและเตาปรุงจาก Blockbench](../output/lobby-concept/assets/models/npc_lyra_alchemist-preview.png)

รายละเอียดโมเดล/สี/rig/ทุกท่าอยู่ใน [ใบงานไลรา](../server/content/npc-models/LYRA-ALCHEMIST-th.md)
Shop D โซน 06: NPC X−69.5/Z68.5 เท้า Y0+1 บนพื้นเต็ม; ลูกค้า X−69.5/Z72.5
**body yaw 0° / pitch 0 หัน South/+Z** เข้าหาลูกค้า; authored model หัน North/−Z ต้อง calibrate offset ใน renderer จริง
เผื่อพื้นที่ anchor 3×3 และสูง 3 บล็อก เตาอยู่ด้านขวาตัวละคร ไม่วางโต๊ะบังมือ/ขวด/หม้อ
ทางเดินร้าน ≥3 บล็อก ซอยหลัก 5 บล็อก ชั้นสมุนไพรชิดผนังและตู้โชว์ปิดหยิบ
ป้ายหน้าเคาน์เตอร์: “ร้านยาไลรา · ดูสูตรก่อนปรุง · ใช้วัตถุดิบและทอง · รับใน /mail · /alchemy”

```text
# ยืนบนจุด NPC และหัน South ก่อนสร้าง Core fallback
/fa npc spawn alchemy.main ไลรา นักปรุงยา
/fa npc list
/fa doctor

# หรือใช้ Citizens ที่จัดตำแหน่ง/ทิศไว้แล้ว: มองตัวนั้นแล้ว bind
/fa npc bind alchemy.main

# หากใช้จุดบริการที่ไม่มีตัวคลิก: ยืนบนจุดแล้วตั้ง anchor
/fa npc anchor alchemy.main
```

เลือกผู้ดูแลตัวคลิกหนึ่งตัวต่อสถานี ไม่วาง Core Villager และ Citizens ซ้อนกัน
Citizens ย้ายหลัง bind ต้อง bind ใหม่และตรวจระยะ; ป้องกันพื้นที่ร้านด้วย WorldGuard/สิทธิ์ที่วางแผนไว้
ผู้เล่นต้องมี `fantasy.alchemy.use` (รวมใน `fantasy.player`, default true)
`fantasy.alchemy.remote` default false อนุญาตใช้นอกสถานี; ไม่ข้ามค่าทอง/วัตถุดิบ/โควตา
ตรวจระยะตาม `stations.remote-radius` ใน config (default 6 บล็อก) ทั้งตอนเปิดและอีกครั้งก่อนเริ่มตัดวัตถุดิบ

**ModelEngine/Citizens visual adapter และ pack ยังไม่มีผลทดสอบ**: contract `enabled:false`, pack ยังไม่ compiled
Core fallback เปิดบริการได้ใน source โดยไม่ต้องมี pack แต่ต้องผ่าน QA เช่นกัน
greet/stir/offer_potion/brew_success เป็นใบงาน animation ที่ export แล้ว ยังไม่มี event bridge เรียกจากธุรกรรม
เฟรม animation ห้ามหักเงิน/ของหรือแจกยา; brew_success เรียกได้หลัง receipt COMMITTED เท่านั้นเมื่อทำ adapter

## 3. Config และขอบเขตไอเทม

- [alchemy.yml](src/main/resources/alchemy.yml): enabled, recipe ID/version, ราคา, วัตถุดิบ, daily-limit และ Core output version
- [items.yml](src/main/resources/items.yml): `lyra_moondew`, `lyra_lifebloom`, `lyra_starlight`; material POTION, potion type, serialized true, สี/glint/lore
- [messages_th.yml](src/main/resources/messages_th.yml): `alchemy`, `menu.main.alchemy`
- [plugin.yml](src/main/resources/plugin.yml): `/alchemy`, alias `/elixir`, permission use/remote

ทุกสูตรร้านยาต้องขึ้นต้น `alchemy_` เพื่อแยกโควตาจาก gear ใน CRAFT journal เดียวกัน
ห้ามใช้ prefix นี้กับสูตรใน `crafting.yml`; `/exchange` เดิมยังอนุญาต ID custom ที่เคยใช้
Alchemy output ต้องเป็น Core template แบบ serialized ที่มี native potion และ version ตรงกัน
parser ปฏิเสธ unknown/strong/long/coerced type, potion บน material อื่น, nonserialized หรือ enchant บนยา
POTION/SPLASH/LINGERING/TIPPED_ARROW template ที่ไม่มี profile จะปิดเฉพาะแม่แบบนั้นแทนออกน้ำเปล่าที่ชื่ออ้างผลยา

ห้ามเปลี่ยน profile/material/ชื่อ/lore ของ template ที่ออกแล้วใน version เดิม
เก็บ snapshot เต็มใน `revisions.<เลขเก่า>` ก่อนเพิ่ม version ใหม่ และให้ output version ตาม current template
archive ต้องระบุ potion ของรุ่นเก่าเอง ไม่สืบทอดผลจากปัจจุบัน; อ่าน [ENCHANTS-th.md](ENCHANTS-th.md) สำหรับหลัก revision เดิม
ราคาหรือวัตถุดิบสูตรที่เปลี่ยนต้องเพิ่ม recipe version โดยคง ID เพื่อรักษาโควตาของวันนั้น

Listener ยกเลิก vanilla `BrewEvent` หากแท่นปรุงมี potion ที่ถือ Core identity fields เพื่อไม่ให้เปลี่ยนผลของยา version เดิม
ต้องตรวจบน staging ว่าตอนยกเลิก ingredient/fuel/ผลขวดอื่นเป็นไปตามที่ตั้งใจ และยา vanilla ล้วนยังปรุงได้
ยาดื่มตามกลไก vanilla; ขวดเปล่าหลังดื่มต้องไม่มี Core identity และไม่ถูกให้ยา/เงินเพิ่ม
ทะเบียน serial เป็นหลักฐานการออกและรับ mail **ยังไม่ได้บังคับล็อกเจ้าของตอนดื่ม/ซื้อขายหรือ mark serial ว่า consumed**
รุ่นนี้ไม่เพิ่ม drink cooldown/รับซื้อคืน/โอน owner/custom effect/ส่งของจากเว็บ; ไม่ใช้รายงาน registry เป็นจำนวนยาคงเหลือจริง

## 4. Journal และการกู้คืน

ใช้ `ExchangeService.Profile.ALCHEMY` และ **CRAFT store เดิม** ไม่มี table/schema ใหม่
state/snapshot/reserved gold/frozen output/serial/mail ใช้กลไก [Core Craft](CRAFT-th.md#5-journal-และการกู้คืน)
PREPARED จองทองพร้อม operation ID; ตรวจ online/alive/สิทธิ์/ระยะ/GUI/slot snapshot ซ้ำก่อน CONSUMING
ปิดเมนูหรือย้ายก่อน consume ยกเลิกและคืนค่าจอง; การหัก inventory อยู่ main thread และขอ saveData ก่อน DB commit
COMMITTED ลงทะเบียน serial และ mail ใน transaction เดียว; output bytes ตรึงไว้ ไม่สร้างผลใหม่จาก config หลัง restart
งาน gear/ยา/exchange ที่ PREPARED/CONSUMING/REVIEW ของผู้เล่นเดียวกันกั้นกันใน DB แม้กดจากคนละเมนู

ถ้า crash หลังเริ่มตัดหรือผลไม่แน่ชัดเป็น REVIEW ห้ามเดาว่าทำสำเร็จแล้วแจกหรือคืนอัตโนมัติ
staff ใช้คำสั่ง **craft** เดิมกับรายการยา เนื่องจาก journal kind เป็น CRAFT:

```text
/fa craft review
/fa craft complete <op> <เหตุผล>
/fa craft cancel <op> <เหตุผล>
/fa confirm <token>
/fa audit <ผู้เล่น>
```

ต้องมี `fantasyadmin.craft.resolve`; ดู recipe `alchemy_*` และผล `lyra_*` ใน preview ก่อน confirm
complete ส่ง payload/serial เดิมและเก็บค่าทอง; cancel คืนทอง **ไม่คืนวัตถุดิบเอง**
หากตัดของแล้วต้องตรวจ snapshot/playerdata/ledger/mail และชดเชยพร้อม audit ตาม [Craft recovery](CRAFT-th.md)
หากผลเข้า mail แล้วแต่ CLAIMING ค้างใช้ `/fa mail review/release/void`; อย่า complete craft ซ้ำ
`/fa doctor` แสดงสถานะร้านยา จำนวนสูตร ปัญหา config และเตือนว่า recovery อยู่ `/fa craft review`
อย่าลบ receipt/serial/ledger ตรง ๆ หรือข้ามรายการ REVIEW เพื่อให้ปรุงต่อ

## 5. อัปเกรดและย้อนรุ่น

1. ปิดเซิร์ฟตามปกติและสำรอง **DB + playerdata/โลก + config + JAR** ชุดเดียวกัน ทดสอบบนสำเนาก่อน
2. ใช้ `FantasyCore-0.9.0.jar` จาก build; รุ่นนี้คง schema 7 จาก v0.8 ไม่ต้อง reset DB
3. Core สร้าง `alchemy.yml` เฉพาะเมื่อไม่มีไฟล์; **ไม่เขียนทับ items.yml/messages_th.yml/config เดิม**
4. merge เฉพาะสาม `templates.lyra_*` จาก defaults และหัวข้อ `alchemy` / `menu.main.alchemy` ในข้อความ ป้องกัน YAML key ซ้ำ อย่าทับ gear/revisions ที่ปรับไว้
5. ถ้ามี template ID หรือ recipe prefix ชนกับของเดิม ให้เปลี่ยน ID ใหม่ก่อนออกยาและอัป output ให้ตรงกัน; อย่าทับ identity เดิม
6. config gear เก่าที่ใช้ `alchemy_` จะถูกปิดสูตรพร้อม doctor error ใน v0.9 ต้องจัดชื่อ/โควตาเดิมก่อนเปิด ไม่มีการเปลี่ยนชื่อ receipt ให้เอง
7. restart ตรวจ `/fa doctor`, `/fa npc list`, permission และ checklist T; config ขาดปิดสูตรที่โหลดไม่ผ่าน ห้ามถือว่าทั้งร้านพร้อมเพราะ JAR โหลดได้
8. ทดสอบอัปสำเนาจาก v0.8 ว่าเงิน/gear archive/mail/NPC/dungeon/recovery ยังครบ; ยังไม่ส่งยาเข้าระบบเว็บ

ถ้าย้อนก่อนมีรายการยา/ยาออกไปแล้ว ใช้ backup ก่อนอัปชุดเดียวกัน
**หลังออกยาแล้วห้ามถอย JAR เดี่ยว ๆ**: v0.8 ไม่เข้าใจ native potion profile และไม่ป้องกัน brewing แบบใหม่
restore backup ชุดเดียวกันทำให้สถานะทั้งหมดกลับจุดนั้น; ตรวจรายการที่เกิดหลัง snapshot และกระทบยอดก่อนเปิด ไม่ลบข้อมูลใหม่ทิ้งเงียบ ๆ
ถ้าต้องพักร้านโดยไม่ย้อนข้อมูล ใช้ `alchemy.yml enabled:false` + restart; ยาที่มีอยู่และระบบ mail ยังใช้ v0.9 อ่าน profile ต่อ
ห้ามใช้ `/reload`; renderer contract ยังปิดไว้แยกจาก service config

## 6. หลักฐานที่มีและงานตรวจจริง

- Java 25 compile กับ Paper API 26.2.build.129-stable และ Core tests **131 passed / 0 failed, error, skipped**
- 7 tests ใหม่: native profiles/invalid data/archive/namespace 5 และ shared craft-alchemy journal/frozen payload/quota 2
- tests เดิมครอบคลุม repeated/concurrent reservation, rollback, refund, serial/mail/audit conflict และ startup review; ผ่านร่วมกัน
- YAML defaults 10 ไฟล์/ข้อความ MiniMessage 401 รายการ และลิงก์ไฟล์ตรวจแยกจากธุรกรรม; Lyra contract hash อัปเดตโดยไม่แก้ geometry/texture/animation

Unit tests ไม่สร้าง PotionMeta ผ่านเซิร์ฟจริง จึงไม่แทนการตรวจ native drink effect, brewing cancellation, inventory save/crash หรือหน้าจอ client เก่า
ผล runtime/checklist T/ModelEngine pack/performance **ยังไม่มี**; แยกสถานะนี้ใน manifest และใบงาน NPC
