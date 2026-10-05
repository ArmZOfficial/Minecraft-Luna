# FantasyCore v0.6 — Enchant จริงและแม่แบบย้อนหลัง

ระบบ enchant เริ่มใน v0.6 และยังใช้ใน v0.7; ขั้นตอน schema v5 ด้านล่างเป็นของ JAR v0.6 โดยเฉพาะ
หากใช้ JAR ปัจจุบัน v0.7 ให้ backup และ migrate schema v6 ตาม [คู่มือดันฝึก](DUNGEONS-th.md) พร้อมขั้นตอน archive แม่แบบเดิมในหน้านี้

ฉบับ 5 ตุลาคม 2026: เพิ่ม native enchant ใน factory ของ Core, แสดงค่าก่อนคราฟต์ และอ่านแม่แบบหลายเวอร์ชันเพื่อซ่อมไอเทมเดิม
**สถานะ: source/JAR build และ unit tests; ยังไม่ผ่าน Minecraft runtime QA**
ใช้ Paper/Java ตาม [manifest](../server/manifest/compatibility-manifest.json); เป้าหมาย client Java 1.16.5+ ยังต้องผ่าน ViaVersion matrix จริง

## 1. อุปกรณ์เริ่มต้นที่ทำแล้ว

| ID / ชื่อ | Material / template version | Native enchant | ค่าคราฟต์ / โควตาต่อวัน |
|---|---|---|---|
| `starter_runeblade` / ดาบรูนผู้เริ่มต้น | IRON_SWORD / v2 | Sharpness II + Sweeping Edge I + Unbreaking II | 200 Gold / 3 |
| `moonstone_pickaxe` / อีเต้อจันทร์นักสำรวจ | IRON_PICKAXE / v2 | Efficiency III + Fortune I + Unbreaking II | 300 Gold / 2 |
| `sentinel_chestplate` / เกราะผู้พิทักษ์ลูม่า | IRON_CHESTPLATE / v2 | Protection II + Thorns I + Unbreaking II | 400 Gold / 2 |

วัตถุดิบและขั้นตอนดู [CRAFT-th.md](CRAFT-th.md); ราคาเป็น Gold ในเกม ไม่ใช้การเติมเงินจริง
ค่าเป็นระดับเริ่มต้นของ [BALANCE-th.md](../server/content/library/BALANCE-th.md): ดาบมีความต่างจากเหล็กธรรมดาเล็กน้อย อีเต้อขุดสะดวกขึ้น เกราะช่วยลดความเสียหาย
ยังไม่เพิ่ม Mending, unbreakable, attack/armor attributes, custom skill, lifesteal, AoE, set bonus หรือตีบวก
คู่กับ [ระบบความลึกมอนสเตอร์](MONSTERS-th.md) เพื่อให้ของรู้สึกมีพลังและศัตรูลึกลงไปท้าทายขึ้น
ค่าของ material/durability และผล enchant ใช้ vanilla; enchant ไม่ได้ถูกจำกัดเฉพาะ PvE และอาจมีผลใน PvP หากโลกเปิด PvP
ข้อกำหนด PvE-only ของทักษะ 17 ชุดเป็นแผนอีกส่วน ยังไม่ได้เปิดใช้งาน

หน้าเลือกสูตรและ preview แสดง enchant แยกหนึ่งบรรทัดต่อชนิด พร้อมชื่อไทยและชื่อ vanilla/ระดับ เช่น `คมรูน (Sharpness II)`
`/fa item list` แสดงแม่แบบปัจจุบันและค่า enchant เช่นเดียวกัน
ชื่อไทยเป็นคำอธิบายในเมนู ไม่เปลี่ยนชื่อ vanilla enchant ทั่วเซิร์ฟ และไม่ได้เป็นหลักฐานว่าไอเทมมีพลัง
factory ใส่ enchant ลง ItemMeta จริงก่อน serialize snapshot ที่ส่งเข้า mail; ชื่อ/lore อย่างเดียวเพิ่มพลังไม่ได้

## 2. Config และการปฏิเสธค่าผิด

ไฟล์ใช้งานคือ `plugins/FantasyCore/items.yml`; ตัวอย่างใน [src/main/resources/items.yml](src/main/resources/items.yml)

```yaml
templates:
  starter_runeblade:
    version: 2
    material: IRON_SWORD
    name: '<gradient:#47C8D3:#DDA54A>ดาบรูนผู้เริ่มต้น</gradient>'
    lore:
      - '<gray>อาวุธฝึกหัดของนักผจญภัยเมืองลูม่า'
      - '<gold>✦ <#47C8D3>รูนประจำเมืองลูม่า · <gray>นักเดินทาง I'
    serialized: true
    glint: false
    enchantments:
      sharpness: 2
      sweeping_edge: 1
      unbreaking: 2
    revisions:
      '1':
        version: 1
        material: IRON_SWORD
        name: '<gradient:#47C8D3:#DDA54A>ดาบรูนผู้เริ่มต้น</gradient>'
        lore:
          - '<gray>อาวุธฝึกหัดของนักผจญภัยเมืองลูม่า'
          - '<dark_gray>ระดับ: เริ่มต้น · แม่แบบ v1'
        serialized: true
        glint: false
```

`glint: false` คงความหมายเดิม: ไม่บังคับ glint override; ไอเทมที่มี enchant จริงจะมีประกายตาม vanilla
ไม่ระบุ `enchantments` = ไม่มี enchant เริ่มต้น; หากระบุ ต้องเป็นหัวข้อ `key: integer` ทั้งชุด ห้ามใส่ข้อความ/รายการแทน
ใช้ชื่อสั้นหรือ `minecraft:<ชื่อ>`; ห้ามสอง alias ของ enchant เดียวกันในแม่แบบเดียว
ไม่มี fallback เป็นไอเทมเปล่าเมื่อ enchant ผิด: ปิดเฉพาะแม่แบบ/revision ที่ผิด และรายงานใน log + `/fa doctor`
YAML syntax เสีย/อ่านไฟล์ไม่ได้ทำให้ Core เปิดไม่ได้ จึงต้องตรวจไฟล์บน staging ก่อน deploy

เพดาน factory ที่เปิดไว้สำหรับ native enchant:

| Key | ระดับสูงสุดที่ Core ออกได้ |
|---|---:|
| sharpness / protection / power | 4 |
| unbreaking / loyalty / quick_charge / sweeping_edge / fortune | 3 |
| efficiency | 5 |
| feather_falling | 4 |
| luck_of_the_sea / lure / thorns | 2 |

ทุกค่าต้องเป็นจำนวนเต็มตั้งแต่ 1 ถึงเพดานของ Core และอยู่ในช่วง start/max level ของ registry จริง
factory ตรวจว่า enchant รองรับ material และไม่ขัดกัน; custom namespace, enchant นอก 13 ชนิดนี้และ Mending ถูกปฏิเสธ
การอนุญาต 13 key ไม่ได้แปลว่ามีสูตรทุกอาวุธหรือแจกขั้น II/III แล้ว; รุ่นนี้มีสูตรเริ่มต้นเพียง 3 ชิ้น
เพดานนี้ควบคุมการออกไอเทมจากแม่แบบ Core; ไม่ clamp vanilla loot, ไอเทมของ provider อื่น หรือ enchant ที่ปลั๊กอินภายนอกแก้ภายหลัง

อ้างอิง API ที่ตรวจตรงกับ dependency: [Paper registry access](https://docs.papermc.io/paper/dev/registries/),
[ItemMeta.addEnchant](https://jd.papermc.io/paper/26.2/org/bukkit/inventory/meta/ItemMeta.html#addEnchant(org.bukkit.enchantments.Enchantment,int,boolean)),
[Enchantment validation](https://jd.papermc.io/paper/26.2/org/bukkit/enchantments/Enchantment.html)
ใช้ registry lookup และ `addEnchant(..., false)`; ไม่ใช้ unsafe enchant หรือ custom enchant registry สำหรับ client เก่า

## 3. แม่แบบเก่าและ serial

- PDC ของไอเทมเดิมยังเก็บ ID/version/serial เดิม; ไม่เปลี่ยน metadata ใน inventory ย้อนหลัง
- `template(id)` เลือกแม่แบบปัจจุบันสำหรับ `/fa item give` และคราฟต์; archive ไม่ปรากฏเป็นรายการแจกใหม่
- `template(id, version)` ใช้โดย adapter ซ่อม เพื่อสร้าง baseline ตามรุ่นที่ออกจริง
- `revisions` ต้องเก็บ snapshot เต็มของ material/name/lore/serialized/glint/version; ไม่สืบทอดข้อมูลจากแม่แบบใหม่
- revision key เป็นเลข 1–1,000,000 ไม่มีศูนย์นำหน้า ต้องตรง version ข้างใน และเก่ากว่า version ปัจจุบัน
- ไม่มี revision ที่ตรง = ปฏิเสธซ่อมก่อนจองทอง แจ้งให้ทีมงานคืน snapshot จาก backup; ไม่เดาหรือ fallback ไป v2
- template ปัจจุบันมี enchant ผิดยังโหลด revision เก่าที่ถูกต้องได้; revision ผิดไม่ทำให้รุ่นปัจจุบันเปลี่ยนตาม
- registry ใน SQLite ตรวจ owner/version/serial/state ตามเดิม; ของ v1 ไม่ผ่านด้วย registry v2
- repair เปลี่ยน DAMAGE ของสำเนาไอเทมที่ถือเท่านั้น; ไม่เติม enchant ใหม่จาก baseline และไม่ล้าง enchant/serial ของชิ้นเดิม
- journal/mail ใช้ bytes ที่ตรึงตอนสร้างรายการเดิม; การเปลี่ยน config ไม่สร้างผลลัพธ์ใหม่แทนรายการค้าง

**อย่าแก้ snapshot หลังแจก** แม้เป็นการแก้คำสะกด lore: component ที่ไม่อนุญาตให้เปลี่ยนอาจไม่ตรง baseline และทำให้ซ่อมไม่ผ่าน
เก็บ snapshot ของเซิร์ฟคุณจริง ถ้า v1 เคยแก้ชื่อ/lore/material ให้ใช้ config v1 จาก backup ไม่ใช้ตัวอย่างใน repo แทน
config เก่าแบบไม่มี enchantments และใช้ default version/name/lore/serialized/glint ยังอ่านได้ตามพฤติกรรมเดิม
อย่าลบ current entry ทั้ง ID หากยังต้องการให้ archive ใต้ ID นั้นอ่านได้

## 4. อัปเกรดและ rollback

1. หยุดเซิร์ฟ สำรอง JAR/config/SQLite DB-WAL-SHM/playerdata/โลก/regions เป็นชุดเดียว
2. เปลี่ยน JAR เป็น `FantasyCore-0.6.0.jar`; จาก v0.5 ใช้ schema v5 เดิม ไม่เพิ่ม migration และไม่แก้ทะเบียน/เงิน/จดหมายเดิม
3. หากยังไม่เปิด enchants ใช้ items.yml/crafting.yml เดิมได้ต่อ: v1 ยังไม่มี enchant เริ่มต้น ไม่ได้เปลี่ยนเป็น v2 อัตโนมัติ
4. เมื่อต้องการเปิด v2 ของแต่ละ ID ให้คัดลอกแม่แบบ v1 **ของเซิร์ฟจริง** เข้า `revisions: '1':` ก่อน แล้วเพิ่ม current version เป็น 2, lore v2 และ enchantments ตามตาราง
5. เพิ่มสอง template ที่ยังไม่มี โดยตรวจ ID ชน; ถ้าไม่ใช้ให้ปิดสูตรนั้น ไม่คัดลอกทับ items.yml ทั้งไฟล์
6. ใน crafting.yml เพิ่ม recipe `version: 2` และ `output.version: 2` ของสูตรที่อัปเดต; `output.template` ต้องอ้าง current template แบบ serialized
7. ถ้าเปลี่ยน current template แต่ลืม output.version สูตรจะถูกปิดก่อนตัดของ; archive v1 ใช้เพื่อซ่อม ไม่ใช้คราฟต์ของใหม่
8. messages_th.yml เดิมคงอยู่; key ใหม่ `craft.menu.enchant` fallback จาก JAR ได้ แต่ให้ merge `admin.item.list-line` จากตัวอย่างเพื่อแสดง enchant ในรายการแอดมิน
9. Restart แล้วตรวจ `/fa doctor`, `/fa item list`, `/fa item inspect`, `/craft`; ห้าม `/reload` หรือแก้ config ขณะมีรายการคราฟต์/ซ่อมค้าง
10. ทำ checklist **O** ใน [server/README-th.md](../server/README-th.md) บนสำเนา staging ทั้ง v1 และ v2 ก่อนเปิดจริง

ตัวอย่างไฟล์จาก repo มี v1 ที่ตรงกับ default รุ่นก่อน; ใช้ได้เมื่อคุณไม่เคยแก้แม่แบบเดิม
รุ่น v0.1–v0.4 ต้องผ่าน migration schema v5 ตาม [คู่มือ Craft](CRAFT-th.md) พร้อม archive แม่แบบเดิมเช่นเดียวกัน

Rollback ที่พิสูจน์ได้คือกู้ backup ชุดก่อนอัปเกรดทั้งหมด
อย่าลดแค่เลข version หรือนำ JAR v0.5 ลงบนข้อมูลที่มีไอเทม v2: แม้ schema เท่ากัน v0.5 ไม่มี lookup archive และอาจปฏิเสธซ่อมรุ่นเก่า
โควตาคราฟต์ต่อวันยังรวมทุก recipe version ของ ID เดิม; อัปเดตสูตรไม่รีเซ็ตโควตาของผู้เล่น

## 5. สิ่งที่พิสูจน์และสิ่งที่ต้องเล่นจริง

Unit tests ตรวจ config default/เก่า, v1/v2 lookup, snapshot เต็ม, config เสียปิดเฉพาะรายการ, duplicate aliases, ชนิดค่าและเพดาน enchant,
การตรึงข้อมูลหลัง config เปลี่ยน, การเก็บ version/serial/owner แยกในทะเบียนและ mail และโควตารวมสอง recipe versions
ไม่เริ่ม Minecraft registry/ItemMeta factory ใน unit tests จึงไม่อ้างว่าการสร้าง enchant/ประกาย/ความแรง/การซ่อมบนเกมผ่านแล้ว

ต้องทดสอบบน Paper staging: enchant ใน item จริง, ความเสียหาย/ขุด/เกราะ, preview, serial หลัง mail/repair/restart,
native anvil/grindstone/crafting guard, Via 1.16.5 และรุ่นใหม่ รวม provider conflicts ก่อนขยายแพ็ก
ข้อจำกัดความเป็นอะตอมของ playerdata กับ SQLite และการพักรายการ REVIEW ยังเหมือน [Repair](REPAIR-th.md)/[Craft](CRAFT-th.md)
การผูกโมเดล 26 แพ็ก, ทักษะ 17 ชุด, ชุดขั้น II/III และการส่งของจากเว็บเป็นงานถัดไปหลัง runtime proof; ไม่ได้เปิดใช้จากไฟล์ balance.json
