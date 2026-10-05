# ItemsCore + FantasyCore — แผนผสมและชุดทดสอบ

ตรวจ 5 ตุลาคม 2026; ผู้ใช้เลือก **พิจารณารุ่นเต็มหลังทดสอบ**
สถานะ: Core v0.5 มี craft/repair สำหรับ Core/vanilla; มี ItemsCore trial imports 4 ไฟล์และ validator proof
**ยังไม่มี ItemsCore JAR, ไม่ได้ import ลงเกม และยังไม่มี Java provider bridge สำหรับ craft/repair/mail**

## 1. รุ่นที่ใช้ตัดสินใจ

หน้า [Spigot รุ่นฟรี 109713](https://www.spigotmc.org/resources/custom-items-plugin-itemscore-free-version-1-8-26-2.109713/)
ระบุว่าเวอร์ชันนี้เลิกอัปเดตและย้ายการทดลองไป hosted testing server แล้ว
ชื่อหน้า 1.8–26.2 ไม่ได้พิสูจน์ว่า backend 26.2 + client 1.16.5 ผ่าน ViaVersion และ resource pack ของเราจะใช้ได้พร้อมกัน

[หน้า ItemsCore ปัจจุบัน](https://www.coredevelopment.shop/plugins/itemscore) และ
[เอกสาร v5](https://www.coredevelopment.shop/docs/itemscore) มี GUI, abilities/stats, custom models, live updates และ AI workflow
ให้ทดลองบน [demo ทางการ](https://www.coredevelopment.shop/plugins/itemscore/demo) ก่อน แล้วค่อยเลือกรุ่น JAR ที่จะนำมาตรวจบน staging
ไม่ใช้ตาราง Free/Premium หรือ Java API ของ GitBook รุ่นเก่าเป็นเงื่อนไขของ v5 เพราะข้อมูลที่ดึงล่าสุดต่างจาก cache เก่า

บันทึก version จาก JAR จริง, SHA256, `/ic exportapi`, client versions และผลทดสอบใน compatibility lock
ตอนนี้ [candidate manifest](../server/manifest/compatibility-manifest.json) เพิ่ม ItemsCore แบบ optional/manual ไว้แล้ว ไม่ดาวน์โหลดหรือซื้ออัตโนมัติ

## 2. เจ้าของแต่ละระบบ

| หน้าที่ | เจ้าของที่เลือก | กติกาผสม |
|---|---|---|
| ทองพก/เงินฝาก/เงินแดง, ledger, ค่าตาย | FantasyCore | แหล่งยอดเดียว; ItemsCore ใช้ Vault สำหรับทองเฉพาะเมื่อผ่านทดสอบ hook |
| ธนาคาร, บ้าน/RTP, daily, NPC action, menu | FantasyCore | Item ability เปิดบริการด้วย command ของผู้เล่น ไม่รัน OP/console |
| Core gear + serial + craft/repair/mail ตอนนี้ | FantasyCore | รองรับ provider core; output.providerอื่นถูกปิดพร้อมแจ้ง doctor |
| Ability graph, custom stats/rarity/talismans ของไอเทมทดลอง | ItemsCore | เริ่มเฉพาะ trial permission; ไม่ลงระบบ stats อีกชุดมาคูณซ้ำ |
| ItemsCore creation/live update | ItemsCore | Bridge ต้องรักษา UUID/PDC/owner/instance vars/damage ก่อนเปิด gear ผ่าน Core |
| NPC animation skeletal | ModelEngine/MythicMobs candidate ตามแผนเดิม | ItemsCore particle animation ไม่แทน NPC bone animation; action เปิดบริการเดิม |
| Model/texture ของไอเทม | ทดลอง ItemsCore Texture Pack Manager | ทดสอบ client packs และเลือกเจ้าของ pack เดียวก่อนใช้จริง |
| Custom blocks/furniture/HUD glyph ที่เกินตัวจัดการ pack | ประเมิน Nexo/ItemsAdder ภายหลัง | รวม asset registry/pack delivery; อย่าเปิดหลายปลั๊กอินส่งคนละ pack ชนกัน |
| ร้านเว็บ/เติมเงินอัตโนมัติ | Web queue + Core bridge ในแผน | ยังไม่เปิด; ห้ามใช้ `/ic give` ซ้ำเป็นทางลัดของ fulfillment |

Core metadata ใช้ `loadbefore: [ItemsCore]` เพื่อให้ Vault economy provider ลงทะเบียนก่อน provider นี้เริ่ม
นี่เป็นลำดับโหลดสำหรับการทดลอง ไม่ใช่ Java adapter หรือหลักฐานว่า Vault hook สำเร็จ
เช็ก hook/ยอด/rounding จริงเมื่อมี JAR; ส่วน addon bridge ต้องโหลดหลังทั้ง Core และ ItemsCore

ช่วงทดลองให้ native ItemsCore recipes/loot/paid roll/reward generation ว่าง และแยกไอเทม `luma_*` ออกจาก Core template IDs
Trial imports ไม่ stamp Core serial จึงไม่เอาไปปลอมเป็น starter_runeblade ในทะเบียน
ItemAdapter เดิมปฏิเสธ metadata ที่ไม่รู้จัก ไม่ลบ stats/ability/skin ของ provider เพื่อซ่อมให้ผ่าน

## 3. ชุดทดลองที่ทำแล้ว

ดู [ไฟล์และวิธี import](../server/content/itemscore/README-th.md)
ทั้ง4ชิ้น `stackable: false` และต้องมี `luma.itemscore.trial`; ไม่มี native recipe, loot, custom events หรือ stat ที่ยังไม่กำหนด
คทาใช้ประกาย teal แบบจำกัด5ticks, เครื่องรางฮีลตนเองมี cooldown20วิ, เสื้อคลุมเป็นเกราะหนังสีประจำเซิร์ฟ
เข็มทิศเรียก `core.executeCommand(player, "menu")` จึงเข้าทาง command/permission ของ FantasyCore ตามปกติ

ไฟล์เป็น clean JSON `.import`; GUI action graph สร้างโดย ItemsCore ตอน import ไม่เขียน generated JavaScript/internal item YAML
ตรวจด้วย library ของ official Helper 1.31.0 ที่ตรวจ SHA512 แล้ว: **4 valid, 0 error/warning** บน bundled manifest **API 5.0**
นี่พิสูจน์รูปแบบและ method signatures ใน snapshot เท่านั้น ไม่พิสูจน์ runtime หรือ ABI ของ JAR รุ่นที่ยังไม่มี
ไม่ได้ตั้ง MCP ของ helper ใน Codex/Antigravity และยังไม่ได้เรียก MCP health_check

## 4. แผน Original addon: FantasyItemsBridge

สร้างเมื่อมี JAR/API export จริงและผล demo เป็นที่พอใจ ตาม [Developer API v5](https://www.coredevelopment.shop/docs/itemscore/developer)
ไม่ decompile หรือเปลี่ยนโค้ด ItemsCore; แยก addon ของเราและใช้ public API/config เท่านั้น

### 4.1 Contract ที่ต้องพิสูจน์ก่อนรับชำระเงินหรือวัตถุดิบ

1. อ่าน item โดย **internal ID** จาก API; ใช้ factory/creation hook ที่คืน ItemStack ให้ Core serialize ก่อนจอง ห้าม give ลง inventory แล้วค่อยสำรอง
2. ยืนยันสำเนาใหม่ non-stackable, amount1, ItemsCore instance ID ใหม่ และ Core serialใหม่สองค่าที่ผูกกัน; template version/fingerprintและ owner ชัดเจน
3. Freeze bytes/item/provider version/price/input snapshot ก่อนตัด แล้ว register + mail + audit atomic แบบ Core v0.5
4. จดหมาย claim ต้องรักษา instance ID เดิม ไม่เรียก factoryใหม่; duplicate claim/recoveryไม่มี UUIDใหม่หรือสำเนาเพิ่ม
5. ใช้ `onItemCreate`/`onItemUpdate` ที่เอกสารระบุเพื่อรักษา Core markers; ห้ามกรณี rebuild/live lore update ล้าง damage, skin/reforge, charges หรือ cooldown state
6. ขณะ pending mail/REVIEW ห้าม reconcileจากชื่อที่เหมือนกัน; providermissing/versionผิด/formatผิด → disabled/REVIEW ก่อนตัดเงิน
7. ซ่อมจากชิ้นจริงและเปลี่ยนเฉพาะ durability ผ่าน provider API ที่ยืนยันแล้ว ไม่ rebuildจากtemplate/ชื่อ; ตรวจ owner/registry/state+unknown components
8. ตีบวก/สกิน/รีฟอร์จเป็น workflow ที่มี auditและ previewของมันเอง; อย่าเปิด native anvil/grindstoneเส้นทางที่ล้างidentityโดยไม่ตรวจ

API `giveItem` คืนvoidและส่งของโดยตรงตามเอกสาร จึงยังไม่เพียงพอสำหรับ prepare/freeze/atomic mailbox ของ Core
หากไม่มี public factory ที่เหมาะสมให้ขอ interface จากผู้พัฒนาหรือจำกัด ItemsCore เป็น content แยกก่อน ไม่ดัก inventory เพื่อเดาผล
การเพิ่ม provider จริงต้องมี migration ต่อท้าย (เช่น provider/provider_item_id/provider_instance_id/fingerprint) พร้อมพิสูจน์ข้อมูล Coreเก่า ไม่แก้ schema v1–v5 ย้อนหลัง

### 4.2 Methods ของเราใน editor

ออกแบบ addon ให้มีหมวด `luma` ที่ใช้ง่าย: เปิด service, อ่านข้อมูลผู้เล่นที่อนุญาต, ตรวจ region/phase, และอ่านความคืบหน้า
การเปิด `bank.main`/`craft.main`/`repair.main` ต้องเรียก ActionRegistry ด้วยสิทธิ์และระยะเดิม
ไม่เผย arbitrary command, console execution, raw SQL, OP, give money หรือ absolute file access ในเมนูสำหรับ content designer
เมนู AdminPanel ในแผนให้เลือก provider/ID/เวอร์ชัน, previewผลจริงและคีย์ที่ต้องรักษา; mismatchต้องปิดปุ่มยืนยัน

### 4.3 ความเสียหายและ stat

ให้ ItemsCore เป็นเจ้าของ custom stat หนึ่งชุด; ถ้าใช้ AuraSkills/SkillsCore/MMOItems ต้องมี mappingและเลือกระบบคำนวณเดียว
กำหนดเพดาน damage/health/mana/cooldownและแหล่งอ่านค่าให้ชัด ไม่เพิ่ม damageทั้ง Core listenerและItemsCore scriptในhitเดียว
Ability ที่ทำ AoE/ขุด/วาง/ระเบิด/วาร์ปต้องเรียก region/combat/instance policyก่อนทุก target ไม่เพียงตรวจจุดที่ผู้เล่นยืน
ใช้ action suppressor/extension points ของ providerเมื่อ APIรุ่นจริงยืนยัน signatureแล้ว; Particle/FXมีbudgetและไม่ใช้วงวนตลอดเวลาในhub
Trial healerยังไม่มีการเชื่อม combat/WorldGuardของเซิร์ฟจริง จึงยังไม่เปิดให้ผู้เล่นทั่วไป

## 5. แมพ NPC และ model workflow

โรงตีเหล็กโซน07 ใช้ `craft.main` เคาน์เตอร์ซ้าย, upgradeกลางปิดไว้, `repair.main` ขวาตาม [ใบงาน Craft](CRAFT-th.md)
NPCทุกตัวหันเข้าจุดผู้เล่นและมีanchorตรงposition; UIผู้เล่นไม่ต้องรู้ชื่อprovider
แสดงตัวอย่าง item ของ providerเป็น displayที่หยิบไม่ได้ อย่าใช้สำเนา instance ID จริงหลายตัวเป็นprops

1. กำหนด content ID/internal provider ID/base material/rarity/skill/cooldown/recipeจากcatalogก่อนสร้างภาพ
2. ChatGPT imagegenทำภาพ item orthographic/front-side-back/palette และรายละเอียด pivot/scale
3. ส่งภาพและใบงานเข้า Antigravity + Blockbench MCP ทำ geometry/UV/texture/export
4. Item modelต้องเป็น Minecraft resource-pack JSON/PNG ที่ providerใช้ได้; `.bbmodel` เป็นsource ไม่ใช่ไฟล์ให้clientอ่านโดยตรง
5. NPCที่ต้องidle/greet/workใช้rig/animationexportของruntimeNPC แยกจากstatic itemJSON; pilot `npc_rune_smith` อยู่ใน [Asset Gallery](../output/lobby-concept/ASSET-GALLERY-th.md)
6. ทดลอง Texture Pack Managerด้วยassetkeyและไฟล์modelจริงก่อนassign texture; ไม่ใส่คีย์ว่างที่ยังไม่มีassetแล้วอ้างว่าrenderได้
7. แยก pack legacy1.16.5และmodernตามformat/featuresที่clientอ่านได้และรวมHUD glyph/GUI/item assetsด้วยdeliveryเดียว; ทดสอบhash/cache/decline pack/Via mapping
8. จึงค่อยผลิตจำนวนมากหลังmappingผ่าน; ไม่จำเป็นต้องลงNexo/ItemsAdderเพิ่มเพื่อไอเทมชุดแรกหากmanagerของItemsCoreผ่านเงื่อนไข

โทนประจำเซิร์ฟใช้ teal #47C8D3, gold #DDA54A และพื้นstone/ไม้เข้ม; สอดคล้องกับเว็บ/Logoเดิม
รายการขยายหลังtrial: runeblade3tier, gatheringtools3, healer/mage/stormwand3, roleplayprops4, armor2sets และcosmetics
ทั้งหมดเป็นbacklog ยังไม่มีimportที่อ้างว่าใช้งานครบ; itemการเงินจริงต้องผ่านbridgeก่อนแจก

## 6. การตัดสินใจหลังทดสอบ

ใช้ [checklist N](../server/README-th.md) แยกจาก Core checklist M และเก็บผลdemoก่อนซื้อ
บนvendor demoตรวจ GUI/edit/export/cooldown/FXได้ แต่ทดสอบCore/Vault/claim/journal/restore/Paper buildของเราไม่ได้ครบ
เมื่อมี JARแล้วทดสอบบนสำเนาstaging: Coreไม่รับผิดชอบข้อมูลproviderที่ยังอ่าน/รักษาไม่ได้ ให้doctorบอกสถานะและปิดเฉพาะความสามารถนั้น
ตัดสินใจใช้รุ่นเต็มเมื่อวิธีauthoring/รูปภาพถูกใจและintegration gateผ่าน; หากไม่ผ่านยังคงใช้Core gearชุดเดิมได้
รายละเอียดแหล่งข้อมูลล่าสุด: [หน้า plugin](https://www.coredevelopment.shop/plugins/itemscore), [Docs](https://www.coredevelopment.shop/docs/itemscore),
[Developer API](https://www.coredevelopment.shop/docs/itemscore/developer), [Helper MIT](https://github.com/Core-Pluginss/ItemsCore-Helper)
