# หลักฐานทางการ: Custom Items, Blockbench และ NPC ที่มี Animation

ตรวจเอกสาร: **5 ตุลาคม 2026** ตามเวลาประเทศไทย
ขอบเขต: ผลวิจัยและข้อเสนอออกแบบ ยังไม่ได้ติดตั้งปลั๊กอิน ทดสอบเซิร์ฟเวอร์ หรือยืนยันโมเดลในเกม

เอกสารนี้ต่อจาก [แผนระบบหลัก](SERVER-SYSTEMS-PLAN-th.md) โดยคง **Java-first และ Java 1.16.5 เป็น client ต่ำสุดที่ต้องทดสอบ** ผู้เล่นเวอร์ชันเก่ายังเป็นเป้าหมายเดิม การทำภาพแบบใหม่จึงต้องแยกจากการรับรองว่า client เก่าจะแสดงผลเหมือน client ใหม่

## 1. ข้อสรุปที่นำไปใช้กับแผนได้

1. ให้ ChatGPT สร้างภาพอ้างอิง แล้วสร้าง geometry, UV, rig และ keyframes ใน Blockbench ก่อนตรวจในเกม ภาพหนึ่งภาพไม่ใช่โมเดลพร้อมใช้งาน
2. ทำ **ไอเทมที่ถือ/อยู่ใน inventory** และ **NPC ที่ขยับกระดูกได้** เป็นคนละงานส่งออก
3. ไอเทมกลุ่มใหม่ใช้ `minecraft:item_model` และนิยามใน `assets/<namespace>/items/`; กลุ่มเก่าต้องมีโมเดล/pack และ mapping สำหรับวิธีเก่า
4. NPC โมเดลหลักเสนอ **MythicMobs + ModelEngine + FantasyCore adapter** ส่วน Citizens ใช้กับ NPC skin มาตรฐานหรือเป็นตัวเลือก integration ที่ผ่านการทดสอบแล้ว
5. ItemsAdder หรือ Oraxen เป็นตัวเลือกช่วยจัดการ content/pack ให้เลือกเจ้าของ pack หลักเพียงรายเดียว ไม่ติดตั้งทั้งคู่เพื่อหวังให้ทุกอย่างรวมกันอัตโนมัติ
6. ใช้ gameplay ID คงที่ เช่น `fantasy:moonsteel_blade` แยกจาก model ID, CustomModelData, material และชื่อภาษาไทย เพื่อให้เปลี่ยนภาพโดยไม่เปลี่ยนตัวตนหรือสเตตัสของของ

ข้อ 1, 4, 5 และ 6 เป็น **ข้อเสนอออกแบบของงานนี้** ข้อจำกัดของ engine ที่ใช้ประกอบการตัดสินใจมีหลักฐานในหัวข้อต่อไป

## 2. Modern item model เปลี่ยนอะไรจริง

| เรื่อง | ข้อเท็จจริงที่ตรวจได้ | ผลต่อแผน |
|---|---|---|
| นิยามภาพไอเทมใหม่ | Java 1.21.4 แยก client item info ไป `assets/<namespace>/items/<path>.json` และเลือกด้วย `minecraft:item_model` | export geometry ไฟล์เดียวไม่พอ ต้องมี item definition ที่อ้าง geometry |
| geometry ยังแยกต่างหาก | ชนิด `minecraft:model` อ้างไฟล์จากโฟลเดอร์ `models` | เก็บ geometry และ item definition เป็นคนละไฟล์ |
| การเลือกภาพตามสถานะ | มี `select`, `condition`, `range_dispatch` และชนิดอื่น; `range_dispatch` แทน overrides แบบเก่า | ทำภาพตามสถานะของไอเทมได้ แต่ไม่ใช่ระบบ bone animation ทั่วไป |
| CustomModelData แบบใหม่ | ใช้ข้อมูล floats/flags/strings/colors ตาม property ที่อ่าน | ไม่ส่ง config รูปแบบเก่าไปทุก client แล้วถือว่าใช้ได้เหมือนกัน |

แหล่งหลัก: [Mojang — Java Edition 1.21.4, Item Models](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-4). เป็นหลักฐานการเปลี่ยน format ในรุ่นนี้ ไม่ใช่คำรับรองว่า schema จะไม่เปลี่ยนในรุ่นถัดไป

### ตัวอย่างโครงสร้าง modern ที่ทีมพัฒนาควรเข้าใจ

ตัวอย่างเป็น **schema อ้างอิงสำหรับทดสอบ** ยังไม่ใช่ resource pack ที่สร้างและโหลดผ่านแล้ว

```text
pack-modern/
  pack.mcmeta
  assets/fantasy/items/moonsteel_blade.json
  assets/fantasy/models/item/moonsteel_blade.json
  assets/fantasy/textures/item/moonsteel_blade.png
```

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "fantasy:item/moonsteel_blade"
  }
}
```

ไฟล์ตัวอย่างด้านบนคือ `assets/fantasy/items/moonsteel_blade.json`; geometry ที่ Blockbench ส่งออกอยู่ใน `models/item/moonsteel_blade.json` ปลั๊กอินฝั่ง server ต้องกำหนด `item_model` ที่ตรงกันผ่าน API ของ backend เป้าหมายด้วย

## 3. ItemsAdder และ Oraxen: ทางเลือกที่มีข้อจำกัดชัดเจน

### ItemsAdder

- เอกสาร modern ระบุว่า `graphics` ต้องใช้ Minecraft **1.21.4+ ทั้ง client และ server** และให้ใช้ `resource` แบบเดิมต่อเมื่อรับ client 1.21.3 หรือต่ำกว่า. [Modern Items Creation](https://wiki.itemsadder.com/adding-content/items/modern-items-creation/)
- FAQ ยืนยันว่า client เก่าจะมองไม่เห็น graphics แบบใหม่ การเข้าผ่าน protocol translator ไม่ได้เปลี่ยนข้อนี้. [graphics and item_model on 1.21.3 and lower](https://wiki.itemsadder.com/faq/graphics-and-item_model-on-1.21.3-and-lower/)
- มีวิธีนำ pack ของ ModelEngine มารวม และมีกรณีไฟล์หาไม่พบจากลำดับโหลดปลั๊กอิน จึงต้องตรวจลำดับ build/import จริง. [ModelEngine compatibility](https://wiki.itemsadder.com/compatibility-with-other-plugins/compatible/modelengine/)

**ข้อเสนอ:** ถ้าเลือก ItemsAdder เป็น pack owner ให้เริ่ม pilot ด้วย legacy `resource` สำหรับภาพพื้นฐานของทุก client ก่อน แล้วค่อยเพิ่ม modern profile เมื่อสร้างเส้นทาง pack/mapping ที่แยกตาม client ได้จริง ห้ามอ้างว่า `graphics` อย่างเดียวรองรับ 1.16.5

### Oraxen

- Oraxen ใช้ server plugin และ resource pack โดยผู้เล่นไม่จำเป็นต้องลง client mod. [Introduction](https://docs.oraxen.com/)
- เอกสาร ViaVersion ที่ตรวจระบุช่วง MultiVersionPacks **1.19–26.3** และ default mode **1.21.4–26.3**; ไม่มีคำรับรอง stock MultiVersionPacks สำหรับ 1.16.5 ในหน้านี้. [ViaVersion compatibility](https://docs.oraxen.com/compatibility/viaversion)
- มี API สำหรับ items/furniture/pack และใช้ item ID ใน PDC; การ reload ต้องรับ event และไม่เก็บผลจาก API ไว้ตลอดโดยไม่ปรับ. [Developer API](https://docs.oraxen.com/developers/api)
- การรวม ModelEngine มีข้อควรตรวจเรื่อง base item ซ้ำและ core shader ของ pack ที่นำเข้า ไม่ควรเปลี่ยนตามตัวอย่างแล้วปล่อยจริงโดยยังไม่ได้ดู animation. [ModelEngine integration](https://docs.oraxen.com/compatibility/modelengine)

**ข้อเสนอ:** Oraxen เหมาะเป็นตัวเลือก modern content layer เมื่อช่วง client อยู่ในขอบเขตที่ผู้พัฒนารับรอง ส่วน 1.16.5 ต้องทำ adapter/pack แยกหรือใช้ fallback ที่ทดสอบแล้ว จึงไม่เลือกเป็นคำตอบสำเร็จรูปสำหรับภาพเหมือนกันทุกเวอร์ชัน

ตัวเลขในหัวข้อนี้เป็นสถานะหน้าเอกสาร ณ วันที่ตรวจ ต้องตรวจใหม่ก่อนซื้อหรือ deploy ไม่ใช่การล็อก JAR รุ่น production

## 4. รูปแบบ Blockbench ที่ต้องสั่งให้ถูก

| สิ่งที่จะทำ | รูปแบบสร้างงาน | ไฟล์ที่ต้องเก็บ | ข้อจำกัดสำคัญ |
|---|---|---|---|
| ดาบ คทา เครื่องมือ ไอเทม GUI | Java Block/Item | `.bbmodel`, Java model `.json`, texture `.png`, item definition/legacy override | format นี้ไม่มี bone animation แบบ entity |
| NPC ธนาคาร ช่างตีเหล็ก ผู้ดูแลกิลด์ | Generic Model ตาม ModelEngine workflow | `.bbmodel` ที่มี geometry, textures, bones และ animations | engine ต้องนำเข้าและสร้าง resource pack ให้ |
| ม็อบ บอส สัตว์เลี้ยง | Generic Model พร้อม rig | `.bbmodel` + blueprint/runtime config | ต้องทดสอบ hitbox และสกิลแยกจากภาพ |
| เฟอร์นิเจอร์นิ่ง | Java Block/Item หรือโมเดลตาม content layer ที่เลือก | model/texture/source + placement config | ภาพกว้างไม่ทำให้ collision เปลี่ยนตามภาพเอง |
| เฟอร์นิเจอร์ที่ขยับ เช่น ตู้เซฟเปิดฝา | Generic Model พร้อม rig หรือวิธีเฉพาะของ runtime | `.bbmodel` + state/action config | ประเมินจำนวน bone/entity และจุดกด |

[Blockbench — Formats](https://blockbench.net/wiki/blockbench/formats/) แยกความสามารถ model animation ของ Generic/Bedrock ออกจาก Java Block/Item ซึ่งรองรับ texture animation แต่ไม่มี model animation ในตาราง อย่า export NPC animated เป็น Java item JSON แล้วคาดว่ากระดูกจะเล่นในเกม

ItemsAdder มีเทคนิค **Animated 3D Items** ที่ทำหลาย model frames แล้วรวมกับ texture animation; นี่เป็นทางเลือกเฉพาะและต้องตรวจ output ตามคำแนะนำ ไม่ใช่การนำ `.bbmodel` rig มาทำ skeletal animation ในมือแบบทั่วไป. [Animated 3D Items](https://wiki.itemsadder.com/adding-content/items/animated-3d-items/)

## 5. ModelEngine: หน่วย ทิศ กระดูก และพื้น

แนวทางทางการให้เริ่ม Generic Model, ใช้กลุ่มเป็น bones และบันทึก `.bbmodel`; หน้าตัวอย่างกำหนดหน้าโมเดลไป **North** และเท้าแตะ grid โดย **16 หน่วย = 1 block** ส่วน cube rotation ใน workflow นี้มีข้อจำกัดหนึ่งแกนและมุมที่ระบุ จึงควรใช้ bone เป็นจุดหมุนของแขนขา. [Creating a Model](https://wiki.mythiccraft.io/modelengine/Modeling/Creating-a-Model)

ข้อกำหนดของงานนี้สำหรับทุก NPC:

1. จัด forward direction เป็น North (`-Z`) ตั้งแต่ source model; ห้ามแก้กลับด้านด้วย offset ลับเฉพาะจุดในแมพ
2. ให้เท้าทั้งสองแตะระดับ `Y=0` ของ model เมื่อตั้ง idle; ไม่มี root translation ที่ทำให้ตัวค่อย ๆ เลื่อนออกจากเคาน์เตอร์
3. ลง pivot ที่ข้อไหล่ ข้อศอก คอและสะโพกจริง ตรวจ child hierarchy โดยหมุน parent แล้วชิ้นส่วนต้องไม่ลอยหลุด
4. head look และงานมือเป็น animation layer แยก; ไม่ให้ idle keyframe จับ head rotation แข็งจนมองผู้เล่นไม่ได้
5. ตรวจความสูงตาและ hitbox ในเกม อย่าใช้ bounding box ที่คำนวณจากภาพอ้างอิง

### Head behavior

ModelEngine มี tag `h_` ให้ bone หมุนเป็นหัว และ `hi_` ให้ child bones รับ head behavior ต่อโดยอัตโนมัติ. [Bone Behaviors — Head / Inherited Head](https://wiki.mythiccraft.io/modelengine/Modeling/Bone-Behaviors)

**ข้อเสนอ naming:** ใช้ `hi_head` กับหัว NPC พร้อมลูกเป็นหมวก/ผม/แว่น เพื่อลดข้อผิดพลาดเวลา tracking; จัด head yaw clamp และการคืนมุมเดิมใน Core adapter ให้สัมพันธ์กับเคาน์เตอร์ ไม่ให้ทั้งตัวหมุนหลังให้ผู้เล่นคนอื่น

### Animation

ModelEngine อ่าน animation จาก `.bbmodel`; รองรับ loop/once/hold และ priority override; default states มี idle/walk/spawn/death ส่วนชื่อ greet/work/success เป็น state ที่ทีมเราต้องสั่งเล่นเอง. [Animating a Model](https://wiki.mythiccraft.io/modelengine/Modeling/Animating-a-Model)

**สเปกแนะนำ:** ทุก service NPC มี `idle`, `greet`, `work`, `success`, `deny`, `reset`; ไม่ใส่ walk/death ให้ NPC ยืนประจำที่ถ้าไม่ได้ใช้จริง ระยะเวลา ขอบเขตการขยับ และ cooldown เป็นค่าดีไซน์ของงานนี้ ต้องทดสอบด้วยผู้เล่นหลายคน

## 6. Runtime ของ NPC: เลือกเจ้าของพฤติกรรมเดียว

เอกสาร ModelEngine แนะนำ MythicMobs สำหรับ modeled NPC และมี integration ของ Citizens เป็นอีกทางเลือก คำแนะนำเรื่องเสถียรภาพเป็นคำกล่าวของผู้พัฒนา ModelEngine ไม่ใช่ benchmark เปรียบเทียบที่งานนี้ได้ทดสอบเอง. [Citizens integration](https://wiki.mythiccraft.io/modelengine/Citizens)

MythicMobs มีแนวทาง NPC แบบไม่มี AI/ไม่เคลื่อนที่/ไม่ despawn แล้วผูกข้อความ คำสั่งและ skills. [Making an NPC](https://wiki.mythiccraft.io/mythicmobs/Guides/Making-an-NPC)

**ชุดหลักที่เสนอ:**

```text
FantasyCore service ID + permission + transaction
        ↓
NPC interaction adapter (server validates click)
        ↓
MythicMobs base entity / lifecycle
        ↓
ModelEngine appearance + animations
        ↓
one version-aware resource-pack delivery owner
```

- เลือกคนคุม spawn/movement/respawn เพียงรายเดียวต่อ service ID
- NPC ที่ช่างตีเหล็กตีค้อนเป็นภาพประกอบธุรกรรม ให้ Core ตรวจรายการ/ราคาแล้วทำธุรกรรมก่อนสั่ง success visual
- การกดปุ่มซ้ำต้องไม่ให้รางวัลหรือหักเงินซ้ำ แม้ animation ยังเล่นอยู่
- Citizens fallback ใช้ base controller คนละเส้นทางตาม profile ไม่ spawn ซ้อนเป็นสองตัวสำหรับคนคนเดียว
- อย่าถือว่า ModelEngine รุ่นใหม่รันบน backend 1.16.5 ได้เพียงเพราะ client ต่ำสุดคือ 1.16.5; backend และ client เป็นคนละข้อกำหนด

### การนำเข้าและส่ง pack

ModelEngine นำ `.bbmodel` เข้า blueprints และสร้าง pack ได้ แต่เอกสารระบุว่า engine **ไม่ได้ส่ง resource pack ให้ผู้เล่นเอง** จึงต้องมี pack host/delivery อีกชั้น. [Importing a Model](https://wiki.mythiccraft.io/modelengine/Modeling/Importing-a-Model)

สำหรับงานนี้ให้ asset build ออก release directory ก่อน upload ตรวจ UUID/hash/URL และโหลดได้จริง แล้วจึงเปิด NPC ไม่เอา preview pack ที่ยังไม่ตรวจมา overwrite production

## 7. ทำไม GeckoLib ไม่ใช่ตัวเลือก default ของเซิร์ฟ Java vanilla

GeckoLib เป็น animation library สำหรับ Minecraft **mods** ตามโครงการเจ้าของ. [GeckoLib repository](https://github.com/bernie-g/geckolib)

เอกสาร GeckoLib entity workflow ต้องสร้างและลงทะเบียน renderer ของ entity. [GeckoLib Entities](https://github.com/bernie-g/geckolib/wiki/Geckolib-Entities-%28Geckolib5%29)

**ข้อสรุปเชิงสถาปัตยกรรม:** ผู้เล่น vanilla บน Paper ไม่มี custom client renderer ที่ mod นี้เพิ่มให้ จึงไม่เลือกเส้นทาง GeckoLib/Forge/Fabric เป็นข้อบังคับของงานนี้ ถ้าอนาคตเปลี่ยนเป็น modpack ต้องออกนโยบาย client และการแจก modpack ใหม่ ไม่วาง GeckoLib JAR ใน `plugins/` แล้วถือว่าเทียบกับ ModelEngine ได้

## 8. Original plugin: แยกตัวตน สเตตัส และภาพ

Paper แนะนำ PDC สำหรับเก็บ arbitrary data ด้วย NamespacedKey บนวัตถุ รวมถึง items โดยไม่พึ่ง server internals. [Persistent Data Container](https://docs.papermc.io/paper/dev/pdc/)

**ข้อเสนอข้อมูลของ FantasyCore:**

```text
item_id       = fantasy:moonsteel_blade
schema        = 1
instance_id   = server-generated UUID for unique equipment
stat_profile  = moonsteel_blade_t1
visual_key    = moonsteel_blade
```

- `item_id` ใช้ตรวจสูตรซ่อม คราฟต์ ออเดอร์และรางวัล ไม่ใช้ชื่อ/lore หรือ CustomModelData เป็น identity
- สเตตัสมีเจ้าของเพียงหนึ่งราย: ถ้าใช้ MMOItems ให้ Core อ่าน/รักษาข้อมูลผ่าน adapter; ถ้าเขียน Core combat เองต้องเป็นผู้สร้าง stats หลักแทน
- visual adapter แปลง `visual_key` เป็น item_model/legacy model data ตาม profile; เปลี่ยน appearance ไม่สร้าง item instance ใหม่
- เซิร์ฟเวอร์เป็นผู้ตรวจสิทธิ์และค่าธุรกรรม แม้ item tag มี ID ก็ไม่ได้หมายความว่าข้อมูลทั้งหมดถูกต้องโดยอัตโนมัติ

Paper DataComponent API ระบุว่าเป็น **version-specific และไม่รับประกัน backwards compatibility**. [Data components](https://docs.papermc.io/paper/dev/data-component-api/)

จึงควร compile สำหรับ backend เป้าหมายหนึ่งชุด แยกส่วนเรียก component API ออกจาก domain logic และทำ migration/test เมื่อ backend เปลี่ยน Java client 1.16.5 ไม่ได้บังคับให้ plugin ใช้ Java runtime เก่า

### AdminPanel

ใช้ GUI มาตรฐาน inventory ภาษาไทยเป็น baseline และตรวจ holder/action ID ไม่ใช้ชื่อหน้าต่างเป็นตัวระบุเมนู Paper อธิบายว่าชื่อ inventory ซ้ำกันได้ จึงไม่ใช่ตัวระบุที่เชื่อถือได้. [Custom InventoryHolders](https://docs.papermc.io/paper/dev/custom-inventory-holder/)

**ขอบเขตเสนอ:** `/admin` เปิดหมวดที่ Core และ adapters ควบคุมได้: NPC/ตำแหน่ง/ทิศ/animation, items, recipes, repair, bank, rewards, shops, zones และ release validation ปลั๊กอินภายนอกที่ไม่มี adapter แสดงสถานะพร้อมทางไปคู่มือ ห้ามบอกว่าแก้ทุก config ของปลั๊กอินใดก็ได้อัตโนมัติ

ทุกปุ่มเขียนข้อมูลควรมี preview, permission, audit และ concurrency guard; งานใหญ่มีสถานะ/ปุ่มกลับ ไม่ปล่อย GUI ค้าง การเพิ่มเฟอร์นิเจอร์หรือแก้ NPC เป็น workflow ในเกมที่ใช้ง่าย แต่การ deploy JAR หรือเปลี่ยน schema เก็บเป็นงาน release ของผู้ดูแล

## 9. แผน Java 1.16.5+: ภาพระดับใดรับรองได้

ViaBackwards ช่วย client เก่าเชื่อม server ใหม่และต้องมี ViaVersion; โครงการบันทึกข้อจำกัด client <1.17 เรื่อง Y นอก 0–255, inventory บาง action และ smithing รุ่นเก่า. [ViaBackwards README / Known issues](https://github.com/ViaVersion/ViaBackwards)

การแปล protocol จึงต้องทดสอบเป็นคนละ gate จาก item model/resource pack ช่วงเวอร์ชันที่เข้า server ได้ไม่ใช่คำรับรองภาพ custom NPC

| Client profile | เป้าหมายภาพ | สิ่งที่ต้องตรวจ | สถานะปัจจุบัน |
|---|---|---|---|
| รุ่นเดียวกับ backend | modern items + NPC animations เต็ม | pack schema, materials, animation state, interactions | ต้องทำ staging |
| 1.21.4+ ที่ชุดปลั๊กอินรับรอง | modern profile ตามรุ่น | item_model definitions, pack format, shader compatibility | ต้องทำ staging ทุกกลุ่ม |
| 1.19–1.21.3 | legacy item appearance หรือ pack ที่ provider สร้างให้ | mapping, old metadata, ModelEngine visual runtime | ยังไม่รับรองภาพ |
| 1.17–1.18.x | legacy profile ที่ทีมสร้างเอง | model overrides, hitbox, GUI, hidden display entities | ยังไม่รับรองภาพ |
| **1.16.5** | gameplay ครบ; legacy item/vanilla skin NPC fallback ตามผลทดสอบ | ไม่มี required new display entity, Y, inventory sync, pack download | เป้าหมายเดิม; ยังไม่รับรองการเล่น |
| ปฏิเสธ pack/ดาวน์โหลดล้มเหลว | GUI vanilla และ NPC fallback ที่มองเห็น/คลิกได้ | ผู้เล่นไม่ติดเพราะ service NPC หาย | ต้องสร้าง/ทดสอบ fallback |

**ข้อเสนอการเปิดจริง:** รายชื่อเวอร์ชันที่รองรับออกจาก test report หลังตรวจครบ หาก 1.16.5 มีปัญหาระบบหลักที่แก้ไม่ได้ ต้องแจ้งผลและเสนอเปลี่ยนนโยบายก่อนเปิด ไม่เลิก target เก่าโดยไม่บอกผู้ใช้ และไม่ลดความถูกต้องของธุรกรรมเพื่อให้ animation เหมือนกัน

Fallback ของ NPC แบบ per-player เป็นงาน integration ที่ต้องพิสูจน์ว่า hide model และแทน skin NPC ได้โดยไม่ซ้อน hitbox ยังไม่มีหลักฐานจากการทดสอบจริงในงานนี้

## 10. เกณฑ์ก่อนรับโมเดลและเปิดบริการ

รายการด้านล่างเป็น **acceptance plan ของงานนี้** ไม่ใช่ผลทดสอบที่ผ่านแล้ว:

- Source `.bbmodel` เปิดได้ และมี textures/animations จริง; image concept อย่างเดียวไม่ผ่าน
- Geometry ฝั่ง Java item ไม่มี forbidden cube transforms/UV missing; มุมในมือซ้าย-ขวา, GUI, ground และ head ที่ใช้ต้องตรวจ
- NPC เท้าถูกพื้น, yaw ถูกทิศ, head หันได้โดยเสื้อ/หมวกไม่แยก, ไม่มี state ที่ root เลื่อนจาก anchor
- เปลี่ยน idle→greet→work→success/deny→idle ได้ครบ; reconnect และ chunk reload ไม่ทำให้ animation หรือ NPC ค้าง
- ModelEngine import ไม่มี error/warning ที่ยังไม่อธิบาย; pack โหลดด้วย client ที่ประกาศรองรับและไม่มี missing texture
- ร้าน/ธนาคาร/ซ่อมทนต่อ double-click, latency, inventory เต็ม, disconnect และ restart ระหว่างธุรกรรม
- ไม่มี invisible hitbox ขวางถนนหรือ counter; อาวุธ/มือ/ปีกไม่ตัดผ่านกำแพงในทุก keyframe
- พื้นแมพ ถนน บันได trapdoor/slab และเฟอร์นิเจอร์ตรงตำแหน่งที่ผู้เล่นมองเห็นบน profile เก่าสุดด้วย
- Load test ตรวจ MSPT/network และจำนวน modeled NPC ที่ผู้เล่นเห็นจริง พร้อมบันทึกข้อจำกัดที่ยอมรับ
- บันทึก release manifest: backend/plugin builds, model IDs, pack hashes, test clients, known defects และ rollback path

คำว่า “ไม่มีบัคเลย” ตั้งเป็นเกณฑ์ **ไม่มี defect ที่รู้แล้วและขัด acceptance เหลือก่อนเปิด** โดยต้องมี test report ปิดรายการ ไม่ใช่คำรับประกันว่าจะไม่มีบัคในทุกเครื่องหรือทุกสถานการณ์

## 11. สถานะคลิปและ Blockbench MCP ในงานนี้

- [คลิปอ้างอิงใหม่ของผู้ใช้](https://www.youtube.com/watch?v=ZQzGY6yheZc) เป็นแหล่งเป้าหมายที่ต้องดูเพิ่ม การ fetch หน้า YouTube ระหว่างวิจัยนี้ถูก throttled จึงไม่ได้อ้างว่าถอดวิธีทั้งหมดหรือยืนยัน plugins ในคลิปแล้ว
- ผู้ทำงานหลักแจ้งว่า callable Blockbench MCP ที่พบใช้ชื่อ Arcadia และการอ่าน capabilities จบด้วย `Session terminated`; งานวิจัยนี้ไม่ได้เรียกหรือแก้ session นั้น
- ยังไม่พบ callable tool ชื่อ Antigravity ในข้อมูลที่ผู้ทำงานหลักส่งมา จึงไม่เขียนว่าทำ handoff ผ่าน Antigravity สำเร็จแล้ว
- ภาพอ้างอิง/asset job สามารถเตรียมจน review ได้ก่อน; เมื่อ MCP เชื่อมได้ จึงสร้าง model ตรวจ source ตรวจ preview แล้ว import staging ตาม gate ด้านบน

## 12. รายการแหล่งหลักสำหรับทีมพัฒนา

| เจ้าของข้อมูล | เอกสาร | ใช้ยืนยัน |
|---|---|---|
| Mojang | [Java 1.21.4](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-4) | item_model, items directory, dispatch formats |
| Blockbench | [Formats](https://blockbench.net/wiki/blockbench/formats/) | Java item กับ animated model เป็นคนละ format |
| MythicCraft | [Creating a Model](https://wiki.mythiccraft.io/modelengine/Modeling/Creating-a-Model) | forward North, scale, source/rig workflow |
| MythicCraft | [Animating a Model](https://wiki.mythiccraft.io/modelengine/Modeling/Animating-a-Model) | states, loop, override, animation import |
| MythicCraft | [Bone Behaviors](https://wiki.mythiccraft.io/modelengine/Modeling/Bone-Behaviors) | head tracking tags |
| MythicCraft | [Importing a Model](https://wiki.mythiccraft.io/modelengine/Modeling/Importing-a-Model) | blueprints/pack generation/delivery responsibility |
| MythicCraft | [Citizens](https://wiki.mythiccraft.io/modelengine/Citizens) | NPC controller integration choice |
| MythicCraft | [Making an NPC](https://wiki.mythiccraft.io/mythicmobs/Guides/Making-an-NPC) | stationary NPC without combat AI |
| ItemsAdder | [Modern Items](https://wiki.itemsadder.com/adding-content/items/modern-items-creation/) | modern graphics requirements and legacy resource option |
| ItemsAdder | [Legacy client graphics](https://wiki.itemsadder.com/faq/graphics-and-item_model-on-1.21.3-and-lower/) | old client limitations |
| ItemsAdder | [Animated 3D Items](https://wiki.itemsadder.com/adding-content/items/animated-3d-items/) | frame/texture animation technique |
| ItemsAdder | [ModelEngine integration](https://wiki.itemsadder.com/compatibility-with-other-plugins/compatible/modelengine/) | merging and load order |
| Oraxen | [ViaVersion](https://docs.oraxen.com/compatibility/viaversion) | current multi-version pack boundary |
| Oraxen | [ModelEngine](https://docs.oraxen.com/compatibility/modelengine) | pack/material/shader collision checks |
| Oraxen | [API](https://docs.oraxen.com/developers/api) | content and pack integration surface |
| PaperMC | [PDC](https://docs.papermc.io/paper/dev/pdc/) | stable item metadata identity |
| PaperMC | [Data components](https://docs.papermc.io/paper/dev/data-component-api/) | backend-specific API boundary |
| PaperMC | [InventoryHolders](https://docs.papermc.io/paper/dev/custom-inventory-holder/) | GUI identity |
| ViaVersion project | [ViaBackwards](https://github.com/ViaVersion/ViaBackwards) | protocol translation and known issues |
| GeckoLib project | [Repository](https://github.com/bernie-g/geckolib), [Entity guide](https://github.com/bernie-g/geckolib/wiki/Geckolib-Entities-%28Geckolib5%29) | mod library and client renderer requirement |
