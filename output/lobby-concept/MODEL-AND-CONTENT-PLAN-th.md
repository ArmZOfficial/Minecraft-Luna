# Luma — modern custom items, animated NPC และงานผลิต asset

ฉบับ 5 ตุลาคม 2026 เพิ่มจากแผนเดิม ใช้ชื่อ Luma / ลูม่า เป็นชื่อเสนอสำหรับเซิร์ฟ fantasy casual roleplay
Java client เป้าหมายยังเป็น 1.16.5 ขึ้นไป ส่วน backend ต้องเลือกจาก plugin compatibility matrix จริงก่อนล็อกรุ่น

## สถานะที่ตรวจได้ในรอบนี้

- สร้างภาพอ้างอิงใหม่ 6 ใบด้วย built-in image generation อยู่ที่ `assets/references/`
- เพิ่ม `blockbench.serverUrl = http://localhost:3000/bb-mcp` ลง Antigravity config ที่พบในเครื่อง พร้อมสำรองไฟล์เดิม
- MCP ใน Blockbench ตอบ initialize สำเร็จ: Blockbench 5.2.1 / MCP plugin 1.10.0
- สร้างและ export ผ่าน MCP จริง: NPC 4 ตัวมี animation คนละ 3 ชุด และ Java item 2 ชิ้น
- ไฟล์ `assets/models/*.bbmodel` เป็น **pilot model** สำหรับพิสูจน์ pipeline มี rig/texture/keyframe จริง แต่รายละเอียด texture และ silhouette ยังไม่ใช่ final ตามภาพอ้างอิง
- ยังไม่ import เข้า ModelEngine/Paper ไม่ได้ทดสอบ client 1.16.5/รุ่นใหม่ในเกม และยังไม่ได้ compile original plugin
- Arcadia connector ที่ติดกับแชตยังใช้ session เก่าที่หมดอายุ จึงเชื่อม HTTP MCP ในเครื่องด้วย session ใหม่ ไม่ได้อ้างว่าส่งข้อความให้ agent Antigravity ทำงานแล้ว
- Antigravity อาจต้อง Refresh MCP servers จึงเห็น entry ใหม่ ไม่ปิดหรือ restart IDE ที่มีงานอยู่

หลักฐาน/ข้อจำกัดจากผู้พัฒนา: [MODERN-MODELING-SOURCES-th.md](MODERN-MODELING-SOURCES-th.md)
งานแต่ละอาคาร: [ZONE-BUILD-TICKETS-th.md](ZONE-BUILD-TICKETS-th.md)

## สิ่งที่คลิปใหม่สอน และสิ่งที่ต้องเพิ่มสำหรับ production

คลิป [How to make custom items the modern way](https://www.youtube.com/watch?v=ZQzGY6yheZc) เป็น tutorial ไอเทม halo บน API รุ่นใหม่ อ่าน transcript จากหน้า YouTube ได้ในรอบนี้

| เวลาโดยประมาณ | แนวทางในคลิป | แนวทาง Luma |
|---|---|---|
| 0:36–3:38 | resource pack namespace และโฟลเดอร์ items/models/textures | ใช้ namespace `luma`; registry จอง ID และ path ไม่ซ้ำ |
| 3:54–8:40 | Java Block/Item ใน Blockbench, texture, display transforms | แยกงาน held item ออกจาก NPC rig และตรวจ GUI/head/ground/มือทั้งสอง |
| 8:50–10:58 | item definition และ `item_model` เชื่อม resource pack | modern adapter สำหรับรุ่นรองรับ; legacy CustomModelData สำหรับ 1.16.5 |
| 11:02–12:58 | ItemStack/DataComponent ITEM_MODEL/ITEM_NAME/EQUIPPABLE | แยก business item ID ใน PDC กับ visual model key; pin API build |
| 13:05–16:20 | health/gravity/fall attributes ในช่องหัว | เป็นไอเทม gameplay คนละ ID กับ halo cosmetic; ไม่สมมติ gravity ทำงานเหมือนกันทุก client |
| 16:24–ท้ายคลิป | ให้ไอเทมตอน join สำหรับทดลอง | production ใช้ claim transaction/admin command ไม่แจกใหม่ทุก login |

ไม่คัด `pack_format` หรือเวอร์ชันจาก transcript มาเป็นค่าปัจจุบันอัตโนมัติ เลือกจาก release ที่ล็อกจริง; เสียงถอดคำอาจอ่านเลขรุ่นผิด

## สถาปัตยกรรมที่เลือก

1. **FantasyCore** เป็น original plugin หลัก: profile, wallet/bank, transaction, rewards, RPG services, item identity, NPC station registry, website bridge, PlaceholderAPI expansion
2. **FantasyAdminPanel** เป็น original companion plugin เรียก Core API และ adapter ที่ลงทะเบียน ไม่ถือเงินหรือไอเทมสำเนาอีกชุด
3. **MythicMobs + ModelEngine** เป็น runtime เสนอสำหรับ animated service NPC; Citizens ใช้กับ skin NPC หรือบริการที่เลือกใช้ Citizens จริงเท่านั้น
4. **Resource pack owner เพียงตัวเดียว**: เริ่มประเมิน ItemsAdder หรือ Oraxen อย่างใดอย่างหนึ่งกับ ModelEngine merge หรือใช้ Core pack pipeline ถ้ามีทีมพัฒนาเพียงพอ
5. **MMOItems/MythicLib adapter** หากใช้ระบบ stat เหล่านี้ Core อ่าน/แก้ item ผ่าน adapter ของปลั๊กอินนั้น ไม่ตีความ lore เป็นข้อมูลจริง

อย่าติดผู้จัดการ custom item หลายตัวให้ใช้ base material/CustomModelData/glyph เดียวกัน ไม่ให้ Citizens กับ MythicMobs สร้าง base entity สองตัวใน station เดียว

## Modern และ legacy ใช้ gameplay registry เดียวกัน

| Profile | การแสดง item | NPC | สิ่งที่พิสูจน์ก่อนเปิด |
|---|---|---|---|
| modern | `item_model` และ `assets/luma/items/*.json` บนรุ่นที่รองรับ | ModelEngine animated model | API, visual key, pack load, equip/use/drop |
| legacy 1.16.5 | base material + numeric CustomModelData และ pack ของรุ่นนั้น | ทดสอบ modeled NPC ก่อน; ถ้าไม่ผ่านใช้ skin NPC/ภาพไอคอน | gameplay เท่ากัน เมนูอ่านออก collision/การคลิกถูกต้อง |
| ไม่รับ pack | vanilla material + ชื่อไทย/คำอธิบาย | skin/base NPC fallback ถ้ารองรับ | ทุกบริการยังเปิดได้จากคำสั่ง ไม่มีปุ่ม glyph ที่อ่านไม่ออก |

[ItemsAdder modern items](https://wiki.itemsadder.com/adding-content/items/modern-items-creation/) ระบุ 1.21.4+ ทั้ง client/server; [Oraxen MultiVersionPacks](https://docs.oraxen.com/compatibility/viaversion) รับรองช่วงที่เริ่ม 1.19 จึงไม่ใช้คำว่า turnkey 1.16.5
ViaVersion/ViaBackwards แปลง protocol ไม่แปลทุก custom model/pack/attribute ให้เอง

ID ตัวอย่าง:

```yaml
# proposal schema ของ Core ยังไม่ใช่คอนฟิกของปลั๊กอินที่ติดตั้งแล้ว
id: luma:aether_halo_skin
kind: cosmetic
schema_version: 1
base_material: CARVED_PUMPKIN
modern_model: luma:aether_halo
legacy_custom_model_data: 110001
equipment_slot: HEAD
gameplay_attributes: []
pack_required_for_visual: true
fallback_name: "วงแหวนอีเธอร์"
```

base material ของ legacy hat ต้องเลือกและทดสอบ first-person overlay, equip, rendering และ ViaBackwards จริง ไม่สมมติว่าหมวกบน STICK ใหม่จะทำงานบน 1.16.5
`luma:aether_halo_relic` ที่มีโบนัส RPG ต้องเป็น item คนละรายการ แหล่งได้จากเกม และสูตร balance คนละชุด

## มาตรฐาน model และ animation

- NPC: Generic `.bbmodel` (`free`) ที่ runtime ยอมรับ, cube geometry, ตัวหัน North/-Z, เท้าแตะ Y=0, 16 editor units ต่อ 1 block ตาม [ModelEngine model guide](https://wiki.mythiccraft.io/modelengine/Modeling/Creating-a-Model)
- item: Java Block/Item `.bbmodel` และ Java JSON; ไฟล์นี้ไม่ส่ง skeleton animation แบบ NPC ให้ vanilla client
- GeckoLib เป็นทางเลือกของ modded client ไม่ใช้เป็น runtime หลักของเซิร์ฟ vanilla ที่รับหลายรุ่น
- ใช้ body, hi_head, upper/lower arms, legs และ prop socket; pivot อยู่หัวไหล่/ศอกจริง ของถือเป็นลูก forearm ไม่ลอยแยกเมื่อเล่น animation
- NPC ตู้บริการสูงเป้าหมาย 1.9–2.3 blocks; pilot ปัจจุบันยังต้องปรับ scale ให้ตรงแต่ละ reference
- งบเริ่มต้นต่อ service NPC: 40–70 cubes, 9–14 bones, texture atlas 128×128 เดียว, 3–5 animations; เป็น budget ออกแบบ ไม่ใช่ผล benchmark
- prop เล็ก 6–20 cubes, item 6–30 cubes texture 32–64 หรือ atlasตาม pack; ไม่ทำรายละเอียดเล็กที่มองไม่เห็นใน GUI
- ปิด shadow/particle ต่อเนื่องที่ไม่จำเป็น ไม่สร้าง ArmorStand เพิ่มหนึ่งตัวทุกของตกแต่ง
- Idle loop 4–6s ความเคลื่อนไหวเบา; greet 1.2s one-shot; work 1.4–2s one-shot และ return neutral pose
- Body ยืน anchor เดิม; ไม่ขยับเท้าหรือชนเคาน์เตอร์ idle; การทำงานเป็น feedback หลังคำขอรับสำเร็จ ไม่ผูกการจ่ายเงินกับ keyframe
- ลด/ปิด particle ด้วย `/visual` ได้ ไม่ลดข้อมูล gameplay

การเกลี่ย animation ใน runtime/การส่ง packet ต้องตรวจจริง ไม่สัญญา FPS หรือไม่มี lag จากจำนวน cubes อย่างเดียว

## Pilot ที่สร้างแล้วและงานต่อ

| ID | ไฟล์ต้นทาง | Animation จริง | งาน polish ที่ต้องทำ |
|---|---|---|---|
| npc_arcane_banker | assets/models/npc_arcane_banker.bbmodel | idle/greet/count_coins | v2 ทำแล้ว: npc_arcane_banker_v2.bbmodel 182cubes/17bones + inspect_ledger; เหลือruntime QA |
| npc_rune_smith | assets/models/npc_rune_smith.bbmodel | idle/greet/hammer | ค้อนตรงทั่ง เพิ่มแว่นและลายรูนตาม reference |
| npc_quest_warden | assets/models/npc_quest_warden.bbmodel | idle/greet/offer_scroll | หู/ผม เสื้อแยกชาย และ gesture ม้วนเควส |
| npc_portal_mage | assets/models/npc_portal_mage.bbmodel | idle/greet/cast | hood silhouette, staff socket, ตั้ง cast ไม่ชนเพดาน |
| item_aether_halo | assets/models/item_aether_halo.bbmodel + JSON | ไม่มี skeleton animation | modern/legacy mapping, head/GUI transforms, UVละเอียด |
| item_runeblade | assets/models/item_runeblade.bbmodel + JSON | ไม่มี skeleton animation | ปรับปลายดาบ/guard, GUI/มือซ้ายขวา, namespace texture |

JSON จาก Blockbench 5.2.1 default Java 26.3 เป็น source export เท่านั้น ก่อนใช้ pack legacy ต้อง compile profile ให้ไม่มี field ที่ client เก่าไม่รับ เปลี่ยน texture path เป็น `luma:item/<id>` และทดสอบ อย่าโยน JSON เดียวใส่ทุก pack
preview PNG เป็น render จากโมเดลจริง แยกจากภาพ reference; `manifest.json` บันทึก runtime_tested=false

## Catalog จำนวนมากโดยมีคุณภาพเป็นลำดับ

ทุก ID ใช้ lowercase snake_case ไม่เปลี่ยน ID ตามชื่อแสดงผล และมี owner module, rarity, acquisition, tradable, soulbound, max_stack, repair_policy, legacy mapping, modern mapping, fallback, icon, revision

| กลุ่ม | แม่แบบที่แตกต่าง | รุ่น/รูปแบบ | จำนวนเป้าหมาย |
|---|---|---|---:|
| weapons | sword, dagger, axe, spear, bow, staff, tome, shield | traveler/rune/warden/royal 4 tier | 32 |
| armor | helmet/chest/legs/boots | traveler/rune/warden/royal | 16 |
| professions | pickaxe/hatchet/fishing_rod/sickle/hammer/mining_lamp | apprentice/artisan/master | 18 |
| material | ore/crystal/herb/hide/wood/fish/rune_dust/cloth/ingot/gem | common/rare | 20 |
| consumable | heal/mana/antidote/food/return_scroll/repair_kit | small/large | 12 |
| cosmetic | halo/hat/circlet/goggles/lantern_pet/weapon_skin | 2 distinct variants | 12 |
| props | bank_ledger/key/coin_stack/forge_hammer/tongs/scroll/map_table/mailbox/shop_sign/fish_crate/portal_focus/rune_plinth | 1 | 12 |
| service NPC | guide/concierge/quest_warden/guild_keeper/banker/merchant/alchemist/smith/job_master/portal_mage/pet_keeper/fisher/healer/archivist | 1 | 14 |
| pets | rune_cat/owl/crystal_snail/mini_golem | 1 | 4 |
| **รวม** | | | **140 assets/variants ตามแผน ไม่ใช่สร้างเสร็จแล้ว** |

Tier ที่ geometry เหมือนกันให้ใช้ texture variant และ data template ถ้ารันไทม์รองรับ ไม่สร้าง rigซ้ำ 4 ตัวเพื่อเปลี่ยนสี
เริ่ม launch slice 6 pilot → 14 NPCหลัก/12props → 20 gameplay items → เพิ่มตาม playtest; 140คือ backlog ไม่โหลดทั้งหมดตั้งแต่ spawn
item gameplay ต้องมีแหล่งหาในเกม สูตรคราฟต์และ balance ก่อนผลิตรูป ไม่ทำ asset มากโดยไม่มีหน้าที่

## เพิ่มเติมจากภาพระบบ Survival/casual roleplay

อ่าน [ชุดระบบใหม่](CASUAL-SURVIVAL-SYSTEMS-th.md) และ [Protect tiers](PROTECTIONSTONES-TIERS-th.md)
เพิ่ม backlog artist variant, paintbrush, easel และหินกันบ้าน 5 texture variants; ยังไม่ได้ผลิตรูป/โมเดลของรายการเหล่านี้
สกินไอเทมเป้าหมาย 12 แบบเป็น catalog ย่อยที่ต้อง reconcile กับ cosmetic/weapon_skin เดิม ไม่บวกเลขรวม 140 โดยนับซ้ำ
หิน Protect ทั้ง 5 ระดับใช้ shape ร่วมเมื่อทำได้ สี/lore ต่างกัน; placement authority อยู่กับ PS/Core ไม่ใช้ decorative entity เป็น claim ที่สอง
artist idle/greet/paint ใช้ brief และ QA pipeline เดิม; ไฟล์ model/texture/animation ต้องมีจริงก่อน publish catalog

## Casual roleplay โดยไม่เพิ่มความซับซ้อน

- เควส/อาชีพ/ตลาดเป็นกิจกรรมหลักที่เข้าได้ทันที; บทสนทนา NPC มีปุ่ม “เปิดบริการ” ข้ามเรื่องเล่า
- roleplay เลือกเข้าร่วม ไม่มี voice requirement/แบบฟอร์มประวัติก่อนเข้าเมือง
- แยก RP chat กับ global/help; ผู้เล่นยินยอมก่อนฉากต่อสู้/จับกุม/บทที่กระทบทรัพย์สิน
- NPC มีบุคลิกสั้น ๆ แต่ชื่อบริการอยู่บรรทัดแรก เช่น “ธนาคาร · นายคลังโอริน”
- ไม่มีบทบาทเจ้าหน้าที่ที่ใช้สิทธิ์ admin ของเซิร์ฟจริง การลงโทษต้องมาจาก moderator ตามกติกา

## ขั้นตอน ChatGPT → Antigravity → Blockbench → staging

1. เลือก asset ticket จากโซนและ registry ดูว่าใช้ vanilla block/texture variant แทนได้หรือไม่
2. ChatGPT สร้างภาพ front/side/back และส่วนแยก prop, บันทึก prompt + revision
3. ตรวจรูป: silhouette consistent, no extra arms, scale, readable icon; reference ไม่ใช่ UVสำเร็จ
4. Antigravity อ่านภาพ+ticket แล้วเรียก MCP get_capabilities, ตรวจ project ที่เปิด และสร้าง projectใหม่เพื่อรักษางานผู้ใช้
5. สร้าง cubes/bones/UVtexture/animation ผ่าน tools ที่ค้น schema จริง; ใช้ Undo สำหรับ mutation; ถ้า evalจำเป็นให้ scoped Undo
6. Export `.bbmodel`, textures, Java JSON/pack mapping, preview, manifest; ห้าม overwrite asset revisionเดิมโดยไม่ตั้งใจ
7. ตรวจไฟล์และ preview จาก Blockbench; animation0/กลาง/ท้าย/วนครบ ไม่บิดหัวหรืออาวุธหลุด
8. นำเข้า ModelEngine/packowner ใน staging; ล็อก export/tool/runtime versions และ checksum
9. Client matrix ผ่านแล้วจึง activate mapping และ station registry ผ่าน admin preview/apply
10. asset ไม่ผ่าน runtime ให้คง fallback และปิดเฉพาะ visualfeature ไม่ปิดบริการ

ไฟล์สำหรับส่งให้ Antigravity: [ANTIGRAVITY-BLOCKBENCH-HANDOFF-th.md](ANTIGRAVITY-BLOCKBENCH-HANDOFF-th.md)

## Gate ก่อนเรียกว่า production

| Gate | ผ่านเมื่อ | หลักฐาน |
|---|---|---|
| BB source | JSON/UUID references/bones/UV/keyframesถูก, animationวน pose ไม่กระโดด | manifest + viewport frames |
| Export | runtimeimport ไม่มี invalid bone/missingtexture, collisionถูก | staging log + screenshot |
| Client legacy | 1.16.5เข้าได้และคลิกบริการ/ถือ/equip/use/itemรักษาค่าถูก | test run + packhash |
| Client modern | รุ่น launch และ latestที่ประกาศผ่าน samecases | exactversion matrix |
| Performance | targetconcurrency ที่กำหนด, spawn NPC load, menu/hud ไม่มี packet spike | spark profile, MSPT + clientFPS/network observation |
| Recovery | reload/restart/chunkunloadไม่มี duplicateNPC/reward/asset orphan | recovery test |

คำว่าไม่มีบัคใช้เป็นเป้าหมายของ gate และการแก้ข้อบกพร่องก่อนปล่อย ไม่เป็นคำรับรองที่ยังไม่มีผลทดสอบ

## งานบอสที่ผลิตแล้วและมาตรฐานรายละเอียดเพิ่ม (5 ต.ค.2026)

[ผู้พิทักษ์จันทร์แตก](../../server/content/dungeons/moonfall/README-th.md) มี146cubes/19bones/atlas128×128/8animationsจริง
AIturnaround → nativeBlockbenchgeometry/paint/rig/keys → export → viewportreview → UUID/UV/rotation/loop/hash gate ทำแล้ว
native .bbmodel5.0แยกgroups/outliner; รูปแบบsourceยังต้องทดสอบimporterModelEngineที่จะใช้งานจริง
Corev0.8ไม่ได้ใช้modelนี้ในเกม มี [renderercontractdisabled](../../server/content/dungeons/moonfall/model-contract.json) และHuskfallback

ตามผู้ใช้กำหนดให้โมเดลลงสีละเอียดและแฟนตาซีอลังการ: ทุกrevisionใหม่ต้องมีรายละเอียดที่อ่านได้จากsilhouette/วัสดุ/UV
- หินใช้โทนหลัก/เงา/ขอบสว่าง/รอยแตก; ผ้าใช้ตะเข็บ/ชายเสื้อ/ตราสถานี; โลหะใช้ขอบถลอก/หมุด/รอยประกอบ
- ไม่ใช้แถบสีpaletteเดียวป้ายทั้งหน้า; UVเสื้อ/มือ/ผม/ใบหน้า/propsแยกบริเวณ มีpadding ไม่ยืดลายจนอ่านไม่ออก
- gold/cyan/violetใช้เป็นจุดนำสายตา จำกัดรูนที่จุดทำงาน ไม่ปิดตา/มือหรือป้ายบริการด้วยeffect
- เปรียบเทียบหน้า/ข้าง/หลัง/three-quarterกับreference เห็นเท้าครบและแอนิเมชันไม่ทำpropsหลุดจากมือ
- บอสใช้รายละเอียดระดับชิ้นนี้เป็นbenchmarkภาพ; NPCบริการคงbudget40–70cubes/9–14bonesก่อนเพิ่ม เพราะต้องแสดงหลายตัวในเมือง
- ไม่เพิ่มboneเพื่อทุกหมุด/รอยแตกหรือsubdivision; รายละเอียดเล็กทำในtexture ภาพสวยไม่แทนbenchmarkMSPT/clientframepacing

ลำดับpolishNPCv2: ~~banker(แว่น/สมุด/เหรียญ/ชุดคลัง)~~ เสร็จ6ต.ค. → smith(เกราะหนัง/ค้อน/ผ้ากันเปื้อน) → warden(ผ้าคลุม/scroll/pouch) → mage(hood/crystal/staff)
itemv2ตามด้วยรูนดาบและhaloที่displayทุกcontextถูก; ไม่เปลี่ยนPDC/serial/ราคา/enchantเพื่อแค่เปลี่ยนรูป
ทั้งหมดexportrevisionใหม่พร้อมtexture/preview/manifest และตรวจruntimeก่อนเปลี่ยนรูปสินค้าหรือประกาศproduction
