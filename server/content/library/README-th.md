# คลังโมเดล ไอคอน และสินค้า Luma

นำข้อมูลจากโฟลเดอร์ `All for module` มาจัดเป็นแคตตาล็อกและสำเนาตั้งค่าที่แก้ชื่อซ้ำ พร้อมชื่อไทยแล้ว
**สถานะ:** เว็บใช้ไอคอนจริงและซื้อแบบจำลองได้; การติดตั้ง provider, การวางของในโลก, enchant และทักษะประจำชุดยังต้องทำและทดสอบใน staging
FantasyCore ปัจจุบันยังเป็น **0.5.0** รองรับการออกไอเทม Core/vanilla; การรับของจาก ItemsAdder/Oraxen/ItemsCore ยังไม่ใช่ความสามารถที่เปิดใช้แล้ว

## 1. ไฟล์ที่ใช้ทำงาน

| ไฟล์ | หน้าที่ |
|---|---|
| [catalog.json](catalog.json) | 26 แพ็ก, 493 item definitions, ID เดิม, ชื่อไทย, material, model path, role, แหล่งที่มา |
| [source-inventory.json](source-inventory.json) | 32 ไฟล์ ณ รอบตรวจ: 26 content ZIP + 1 ZIP รวม build + 5 JAR พร้อม SHA256/metadata |
| [repairs.json](repairs.json) | บันทึกชื่อซ้ำที่แก้และจุดอ้างอิงโมเดลที่แก้ |
| [icons.json](icons.json) | 50 ภาพที่เลือกใช้ พร้อม source entry, hash, เครดิต |
| [balance.json](balance.json) | สูตรระดับ/enchant/ทักษะฉบับออกแบบ ปิดการทำงานไว้ |
| [BALANCE-th.md](BALANCE-th.md) | รายละเอียดพลัง ราคาในเกม เงื่อนไข และเกณฑ์ทดสอบ |
| [../../../website/lib/library-products.json](../../../website/lib/library-products.json) | SKU และราคาเสนอของร้านค้า 22 ชุด |

แคตตาล็อกสินค้าของเว็บอยู่ที่ `website/lib/library-products.json` จากรากโปรเจกต์ ส่วนคู่มือชำระเงินคือ [PromptPay DEV](../../../website/PROMPTPAY-th.md)
493 รายการรวมอาวุธ เกราะ ของตกแต่ง โมเดลสำหรับตัวผู้เล่น/ผู้ชม และวัตถุช่วยแอนิเมชัน จึงไม่ใช่สินค้า 493 ชิ้นที่ควรขายแยกทั้งหมด

ต้นฉบับ ZIP/JAR ไม่ถูกแก้ ไม่ถูกเรียกทำงาน และไม่ถูกอัปโหลด Git
ไอคอนที่คัดมาและ config ของผู้สร้างอยู่ในไฟล์ local ที่ regenerate ได้; repo เก็บเครื่องมือ แคตตาล็อก และคู่มือ
คัดลอกไฟล์สินค้าที่ใช้เฉพาะตอนจัด deploy ของเจ้าของเซิร์ฟ ไม่แจก ZIP โมเดลต้นฉบับเป็นสินค้าให้ผู้เล่นดาวน์โหลด

## 2. ชื่อชุดและการใช้ในเมือง

| แพ็ก / ID ของ Luma | ชื่อที่แสดง | รายการ | เส้นทางเล่น / โซนโชว์ |
|---|---|---:|---|
| azureset / azure | ธาราฟ้า | 27 | เควสธนาคาร, ตู้โชว์คริสตัลฟ้า |
| beatsset / beats | จังหวะดารา | 27 | นักดนตรีโรงเตี๊ยม, กิจกรรมเมือง |
| Beserkset / berserker | พยัคฆ์คลั่ง | 21 | สนามฝึก, ช่างอาวุธ |
| Demon_commander / commander | จอมทัพเงารัตติกาล | 23 | กิลด์, ภารกิจบอส |
| Demonic_Killeffect / killfx | ประกายอสูร | 2 | VFX โชว์สนามฝึก; ไม่มี damage จากสินค้าร้าน |
| draculaset / dracula | ขุนนางจันทราเลือด | 26 | กิลด์รัตติกาล, เควสสุสาน |
| fiendskullset / fiendskull | กระโหลกผนึกวิญญาณ | 29 | ร้านวัตถุโบราณ, เควสสืบสวน |
| Ifritset / ifrit | อิฟริทเพลิงอรุณ | 23 | เตารูน, เควสแร่ภูเขาไฟ |
| infernoset / inferno | อัคคีล้างเงา | 29 | ช่างอาวุธระดับสูง |
| littledragonset / little-dragon | ผู้พิทักษ์มังกรน้อย | 33 | เควสเริ่มต้นเขตสัตว์เลี้ยง |
| lunardragon_set / lunar-dragon | มังกรจันทรา | 31 | กิลด์, หอดูดาว |
| Madhatterset / madhatter | นักมายาหมวกพิศวง | 26 | ตลาดมายา, กิจกรรมตามหา NPC |
| Medieval-Kitchen / kitchen | ครัวรูนแสนอุ่น | 18 | ครัว NPC, ร้านอาหาร, ตกแต่งบ้าน |
| Medieval-Tavern / tavern | โรงเตี๊ยมแสงจันทร์ | 14 | เคาน์เตอร์ บาร์ โต๊ะ เก้าอี้ ชั้นวาง |
| MystiCrates / keys | กุญแจห้าดารา | 5 | กุญแจรางวัลเควส; ไม่เปิดขายสุ่มด้วยเงินจริง |
| Nogs Dinosaurs / dinosaurs | เพื่อนจิ๋วยุคดึกดำบรรพ์ | 12 | ร้านของเล่น, เควสสำรวจฟอสซิล |
| Nogs Superheroes / heroes | ผู้พิทักษ์ตัวจิ๋ว | 10 | ของสะสมกิจกรรม, ร้านของเล่น |
| nora-limited / nora | นภาผู้เดินทาง | 23 | สมาคมนักสำรวจ, ร้านแฟชั่น |
| OnePunchMan / onepunch | หมัดดาวตก | 21 | สนามฝึก; ไม่ทำสกิลสังหารทีเดียว |
| radiance / radiance | รุ่งอรุณศักดิ์สิทธิ์ | 21 | หอผู้รักษา, เควสปาร์ตี้ |
| sanpatric / clover | โคลเวอร์โชคสี่แฉก | 24 | สวน, เควสชุมชน |
| Skelton_overload / skeleton | ราชันกระดูกนิรันดร์ | 24 | กิลด์, ดันเจี้ยนสุสาน |
| starlight / starlight | แสงดาวผู้พิทักษ์ | 24 | หอเวท, เควสคืนดาวตก |
| Essential Icons Vol.1 | สัญลักษณ์เมืองลูม่า | glyphs | ป้ายระดับและคำใน TAB/scoreboard |
| EssentialIconsVol1 | สัญลักษณ์บริการชุดหนึ่ง | icons | เมนู/เว็บ, เหรียญ ทั่ง คทา สมุด |
| EssentialIconsVol2 | สัญลักษณ์บริการชุดสอง | icons | เมนู/เว็บ, หีบ เข็มทิศ เฟือง ใบโคลเวอร์ |

ชื่อไทยเปลี่ยนเฉพาะ `display_name` ในสำเนา ไม่เปลี่ยน namespace หรือ ID อ้างอิง
รายการที่มีสองโมเดลจะยังแยก ID; ตัวละคร/ผู้เล่นต้องเห็น serial เดิมเมื่อเปลี่ยน skin หรือซ่อม
ไม่ทำสูตรเควสด้วยการตรวจชื่อ/lore เพราะผู้เล่นสามารถตั้งชื่อเลียนแบบได้

## 3. ข้อผิดพลาดที่แก้ในสำเนา

- 20 config ของป้าย Chamby มี `noble_color_N` ซ้ำ: เปรียบเทียบเนื้อหาตรงกันก่อนยุบรายการซ้ำ
- Lunar Dragon มีชื่อ `lunardragonset_tail_cosmetics_self` ใช้ทั้งหางและหมวก: เปลี่ยนรายการหมวกเป็น `lunardragonset_helmet_cosmeticscore`
- OnePunch มี greatsword สองคำอธิบาย: แยกอีกชื่อเป็น `onepunchman_greatsword_alternate` เพื่อไม่ทับกัน
- `onepunchman:greatsword` ไม่มีโมเดลใน ZIP: แก้สำเนาให้ชี้ `onepunchman:onepunchman_greatsword` ซึ่งพบไฟล์จริง

ตรวจ YAML 201 ไฟล์ด้วย SafeConstructor และไม่อนุญาต duplicate keys; ตรวจโมเดล custom 589 รายการกับ custom texture/parent path ใน ZIP
นี่เป็นการตรวจไฟล์ ไม่ยืนยันว่า client render, hitbox, animation หรือ provider startup ผ่านแล้ว
ไฟล์ auto-generated ที่ใช้ namespace `minecraft` ต้องตรวจหลัง provider สร้าง pack เพราะไม่ได้อ่าน vanilla resource ของทุกเวอร์ชันในขั้นตอนนี้

## 4. วิธี regenerate และเตรียม config

จากราก repo รัน:

```powershell
python tools/import_module_library.py
python tools/verify_module_library.py
cd website
npm run db:migrate
npm test
npm run build
npm start
```

ต้องมี Python, Java และ dependency SnakeYAML/Gson จาก Gradle cache ที่ใช้ build FantasyCore อยู่แล้ว
ถ้าไม่มี cache ให้ build FantasyCore ก่อนตาม [คู่มือ staging](../../README-th.md)
ในเครื่องนี้ใช้ bundled Python ที่ `C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe` ได้
config ที่แก้และตั้งชื่อไทยอยู่ใน `server/content/library/local/normalized/<ชื่อ ZIP>/<entry เดิม>`
รูปในเว็บอยู่ใน `website/public/assets/library/` ทั้งสองตำแหน่งเป็น local และไม่ถูก push
สคริปต์หยุดเมื่อพบ duplicate ที่ยังไม่เคยตรวจหรือ custom model reference ขาด แทนการติดตั้งต่ออัตโนมัติ

## 5. Provider และเวอร์ชัน

ใช้ provider โมเดลหลัก **ตัวเดียว** ต่อ backend เพื่อให้ namespace, recipe, custom model data และเฟอร์นิเจอร์มีเจ้าของชัดเจน
สำเนาที่เตรียมมี config ของ ItemsAdder และบางแพ็กมี Oraxen/Nexo; เลือกเฉพาะชุดเดียว ไม่วางทั้งสาม
เลือก ItemsAdder เป็น candidate รอบแรกเพราะแพ็กอาวุธส่วนใหญ่มี config นี้; ยังไม่ล็อกเป็นรุ่น production
ItemsCore เป็น candidate สำหรับ logic/effect ของไอเทมหลังทดลองตาม [แผน ItemsCore](../../../fantasycore/ITEMSCORE-INTEGRATION-th.md) ไม่ให้สองระบบออกของชิ้นเดียวกันเอง

| JAR ที่พบ | สถานะและสิ่งที่ต้องพิสูจน์ |
|---|---|
| ItemsAdder 4.0.18 | ต้องมี ProtocolLib ตาม metadata; ตรวจ startup และ pack บน Paper 26.2 ที่ล็อกไว้ก่อน |
| Oraxen 1.211.0 | ทางเลือกแทน ItemsAdder; ยังไม่ทดสอบบน backend นี้ |
| MythicMobs 5.6.1 | version metadata มีตัวแปร CI ค้างอยู่; ต้องตรวจ release/compatibility จริงก่อนใช้ |
| CommandAPI 12.1.0 Paper | ไม่ได้ทำให้ config จาก provider อื่นใช้ได้เอง; ติดตั้งเฉพาะเมื่อ dependency ต้องใช้ |
| Vulcan 2.9.7.26 | พบเพิ่มในโฟลเดอร์ระหว่างตรวจ; บันทึก metadata/hash แล้ว ยังไม่รัน ต้องทดสอบ false positive ของ dash/model/furniture ก่อน |
| ModelEngine | ไม่พบ JAR ในโฟลเดอร์นี้; skeletal animation ต้องจัด runtime ที่รองรับก่อน |

`archive.zip` ที่พบเพิ่มมี PacketEvents 2.14.1-SNAPSHOT หลาย platform และ Javadoc/source JAR; ไม่คัดทุก JAR ไปลง `plugins`
เลือก release/platform ที่ provider ต้องใช้หลังพิสูจน์ dependency และ backend compatibility; build snapshot ใน ZIP ยังไม่เป็นรุ่นที่ล็อกใน staging

ItemsAdder v4 ระบุยุติ native compatibility 1.15–1.20.4 เพื่อเน้น 1.20.5+ จึงยังรับประกันภาพครบสำหรับ Java 1.16.5 ไม่ได้ ([เอกสารผู้สร้าง](https://wiki.itemsadder.com/faq/itemsadder-v4/))
เป้าหมาย Java 1.16.5+ ยังอยู่: แยกทดสอบการเข้าเล่น/ระบบ Core ออกจาก pack และภาพ custom; ViaVersion/ViaBackwards ไม่ใช่หลักฐานว่าโมเดลทุกอย่างเหมือนกัน
เก่าใช้ชื่อไทย ไอคอน vanilla และ block decoration fallback; ระบบที่ต้องมี display entity/model จริงต้องแจ้งข้อจำกัดหรือปิดเฉพาะฟีเจอร์หลังทราบ client protocol
ยังไม่มี pack routing/fallback renderer ที่ implement แล้วสำหรับคลังนี้ ห้ามประกาศรองรับเต็มก่อนผ่าน matrix

## 6. ใช้ไอคอนในเว็บและเกม

เว็บใช้ pictogram ของ Crystal Creations; ป้ายข้อความของ Chamby เป็นคนละประเภท จึงเก็บชื่อ `tag-*` แยกจากภาพไอคอน
เครดิตอยู่ใน footer แล้ว รายการ hash/source ของแต่ละภาพอยู่ใน `icons.json`
ไฟล์ Terms ของ Chamby ระบุใช้/แก้บนเซิร์ฟตัวเองได้ ห้ามขายต่อไฟล์ผลิตภัณฑ์และต้องให้เครดิตเมื่อโชว์สาธารณะ; งานนี้ใส่ชื่อผู้สร้างไว้แล้ว

| ระบบ | ไอคอนเว็บ | ตำแหน่ง GUI เกมที่เสนอ | รูปแบบ fallback |
|---|---|---:|---|
| เงิน/ธนาคาร | bank / currency | เมนูหลัก 11 | GOLD_INGOT |
| ซ่อม | repair | 13 | ANVIL |
| สร้าง | craft | 28 ที่มีใน Core | CRAFTING_TABLE |
| เควส | quest | 15 | WRITABLE_BOOK |
| ร้านค้า | shop | 22 | EMERALD |
| จดหมาย | mail | 30 | CHEST |
| Enchant | enchant | 32 | ENCHANTING_TABLE |
| เฟอร์นิเจอร์ | furniture | ร้านตกแต่ง 11 | OAK_STAIRS |
| สัตว์/ตุ๊กตา | pets | ร้านของเล่น 15 | PLAYER_HEAD |
| AdminPanel | admin | เมนูแอดมิน 4 | COMPARATOR |

slot นับจาก 0; ยกเว้น craft slot 28 รายการในตารางเป็น layout เสนอ ต้องตรวจเมนูเดิมก่อนแก้
เกมต้อง resolve ไอคอนเป็น item/glyph ผ่าน provider ที่พร้อม; ถ้าไม่พร้อมใช้ fallback โดยรักษาข้อความและ action เดิม
TAB/scoreboard ใช้ font glyph ของ provider ที่ assign Unicode จริง ห้ามหยิบเลข Unicode สุ่มไปชนโลโก้/ตัวเลข HUD
วัด advance, baseline และ negative space บน client ทุกรุ่น; font glyph ไม่ใช่ PNG ที่แปะโดย TAB ได้ทันที
เลือกสีทอง–ฟ้าและใช้ป้าย rank 7 แบบที่คัดไว้เป็นจุดเริ่มต้น; รายการอื่นยังอยู่ใน ZIP เลือกเพิ่มเมื่อมีการใช้จริง

## 7. แปลนวางของและ NPC

ใช้ระบบพิกัดอาคารท้องถิ่น: มุมตะวันตกเฉียงเหนือพื้นอาคารคือ `(u=0,v=0)`; u ไปตะวันออก(+X), v ไปใต้(+Z)
แปลงเป็น world ด้วย anchor `(X0,Y0,Z0)` แล้ว `X=X0+u`, `Z=Z0+v`; Y คือระดับพื้นเดินที่สำรวจจริง
ตัวเลขนี้เป็น layout สำหรับช่างสร้าง ไม่ใช่อาคารที่วางในโลกแล้ว

### โรงเตี๊ยมและครัว 29×23

- ประตูกลางใต้กว้าง 3 บล็อก พื้นทางเดินหลัก u=12..16 จาก v=21 ไป v=5; ไม่วางเก้าอี้/กองของบนทางนี้
- เคาน์เตอร์ `workshop_six:tavern_bar_counter` กึ่งกลาง `(7.5,7.5)` หมุน 0° ให้ด้านขายหันใต้; ตรวจมุมจริงของ asset ก่อน commit world
- hitbox ที่ config ให้กว้าง 3 บล็อก: เว้นพื้นที่ u=6..8, v=7 และแถวบริการ v=8..10; ไม่ใช้ภาพโมเดลเดาขอบชน
- NPC เจ้าของโรงเตี๊ยม `(7.5,5.5)` มองจุดลูกค้า `(7.5,9.5)` → yaw 0°, pitch 0°; ต้องมีเส้นทางเดินด้านหลัง NPC 2 บล็อก
- โต๊ะยาว `(5.5,14.5)` และ `(22.5,14.5)`; ม้านั่งแยกจากโต๊ะและเว้นช่องลุกอย่างน้อย 1 บล็อก
- keg/keg_station บนผนังเหนือ `(3.5,3.5)` และ `(10.5,3.5)`; ขวดกับแก้วใช้ prop ตกแต่ง ไม่ให้ดูดของด้วย hopper
- ครัวกรอบ u=18..27, v=2..10: stove `(20.5,3.5)`, oven `(23.5,3.5)`, wash_station `(26.5,3.5)`, island `(22.5,7.5)`
- NPC เชฟ `(23.5,5.5)` มองจุดส่งเควส `(19.5,5.5)` → yaw 90°; เควสส่งวัตถุดิบอยู่ฝั่งว่างจาก island
- ตู้ kitchen_cabinet_a/b บนผนังเหนือ; pot, dishes, flour_stacks บนเคาน์เตอร์ที่มีพื้นรับจริง เว้นหัวเตาไม่ให้โมเดลซ้อน
- เควสเปิดครัว: ส่งแป้ง/ปลา vanilla ที่นับรวมได้ → รับ token สูตรอาหาร ไม่แจกอาวุธระดับสูงจากส่งอาหารหนึ่งครั้ง

### ร้านของเล่น 17×15

- ประตูกลางใต้ u=7..9, v=14; ทางเดินกว้าง 3 บล็อกจนถึงเคาน์เตอร์ v=4
- ชั้นโชว์ไดโนเสาร์ u=2 และ u=14, v=5/8/11; เริ่มครั้งละ 6 ตัว กระจายสูงไม่เกิน 2.5 บล็อก
- ใช้ `nm_plushie_triceratops`, `nm_plushie_tyrannosaurus`, `nm_plushie_pteranodon` เป็นไฮไลต์ก่อนเติมอีก 9 ตัว
- NPC `(8.5,3.5)` มอง `(8.5,6.5)` → yaw 0°; marker จุดคลิกต้องอยู่หน้าตัวและไม่ทับ armor stand ที่โชว์
- เควสฟอสซิลให้เลือกตุ๊กตาหนึ่งตัว; เควสต้องออก entitlement/serial ตาม ID ไม่ใช้การลากตุ๊กตาจากตู้โชว์
- ของบนเว็บเป็นสิทธิ์ของสะสม ไม่ใช่ bundle source file; ไม่มอบ stats ต่อสู้จากตุ๊กตา

### ร้านแฟชั่นและพิพิธภัณฑ์ชุด 25×21

- ลานกลาง u=9..15, v=5..18 ว่าง; ตู้ฝั่งซ้าย 4 ชุดและขวา 4 ชุด ห่างกันอย่างน้อย 3 บล็อก
- โชว์พร้อมกัน 8 ชุด ไม่สร้าง armor stand ของทั้ง 493 รายการใน lobby
- ล็อกตู้โชว์ ไม่มี inventory สามารถขโมยและไม่มี drop; click entity เปิด preview ที่ใช้ action จาก server registry
- NPC ช่างแฟชั่น `(12.5,3.5)` มอง `(12.5,7.5)` → yaw 0°; ใช้ idle นิ่งและ greet เมื่อมีผู้เล่นจริงในระยะ
- rotation ของไอเทมในมือ/ตู้มีค่าแยกจาก yaw NPC: ดู display transform ใน model JSON แล้วตรวจในเกมทั้ง first/third person
- เพิ่ม pedestal preview ทีละชุดตามการเลือก; reuse entity แทนสร้างใหม่ทุก tick; ปิด effect หากผู้เล่นห่าง 24 บล็อก

### ธนาคาร/โรงตีเหล็ก/กุญแจ

- ธนาคารใช้ธาราฟ้าเป็นงานโชว์ข้างเคาน์เตอร์ ไม่ปิดทางเดินหรือบดบังป้ายยอดเงิน; bank.main เดิมเป็นผู้ดำเนินรายการเงินจริงในเกม
- โรงตีเหล็กยึด [แปลน crafting และ station เดิม](../../../fantasycore/CRAFT-th.md); สกินอิฟริท/อัคคีเป็นตู้ตัวอย่าง ไม่ทำร้าน give ตรงทับ journal ของ Core
- กุญแจ common/rare/epic/legendary/divine ใช้ชื่อไทย กุญแจนักเดินทาง/กุญแจแร่จันทรา/กุญแจรูน/กุญแจผู้พิทักษ์/กุญแจดาราศักดิ์สิทธิ์
- หน้าเปิดหีบแสดงรางวัล เงื่อนไข และโอกาสก่อนใช้; คีย์ 5 ไฟล์ `.bbmodel` ต้องตรวจ rig/animations และ runtime ModelEngine ก่อนโชว์เคลื่อนไหว
- เริ่มเอฟเฟกต์หีบหนึ่งตัวต่อคน เปิดแล้วคุม state ห้ามคลิกสองครั้งสร้างรางวัลสองชุด; ผลรางวัลต้อง journal ก่อน animation

สูตรหัน NPC: `yaw = degrees(atan2(-(targetX-npcX), targetZ-npcZ))` ปรับอยู่ช่วง 0..360; คำนวณ pitch จากระดับสายตาแทนระดับพื้น
ทางเหนือ yaw=180, ใต้=0, ตะวันตก=90, ตะวันออก=270; ถ้ามี look-close ให้มีมุม idle เดิมกลับหลังคนออกไป
งานเคาน์เตอร์ใช้มุมหันคงที่หรือจำกัด look-close ไม่ให้หันย้อนผนัง; ยืนยันในเกมเพราะ model root rotation อาจมี offset

## 8. ลำดับทดสอบก่อนเปิดขาย/เปิดโลก

1. ล็อก provider/JAR พร้อม hash และพิสูจน์ startup บน backend เดิม; ยังไม่คัด JAR จากโฟลเดอร์ไปทำงานอัตโนมัติ
2. ตรวจ normalized config ด้วย loader ของ provider และ build resource pack; ทดสอบ PNG, animation frames, armor layers, pull ของธนู, shield blocking
3. ตรวจ client 1.16.5, 1.20.4, 1.20.5+, รุ่นปัจจุบัน; จดเฉพาะ feature ที่ผ่านและ fallback ของรุ่นเก่า
4. วางอาคารทีละโซน ตรวจบล็อกลอย มุมบันได แผ่น slab น้ำรั่ว ประตู ช่องเดิน และ hitbox ของเฟอร์นิเจอร์จริง
5. ทดสอบวาง/ทุบ/หมุน/ระเบิด/hopper/piston ในและนอก ProtectionStones/WorldGuard; คืน item เพียงหนึ่งชิ้นเมื่ออนุญาตทุบ
6. ตรวจ NPC facing, click action, permission, range, serial และ offline/full-inventory delivery โดยไม่ใช้ `/give` เป็นทางเลี่ยง
7. ทดสอบ Web entitlement → game mailbox ที่เชื่อม UUID แล้ว; duplicate callback/restart ไม่เพิ่ม entitlement ซ้ำ
8. ทำ benchmark ผู้เล่นทดสอบ 20 คนในโซน model หนาแน่น เป้าหมาย p95 MSPT ต่ำกว่า 40 ms และไม่มี frame drop ต่อเนื่องบนเครื่องอ้างอิง
9. ผ่านเกณฑ์ BALANCE และเก็บ world/database backup คู่กัน; เปิดขายเฉพาะ SKU ที่ delivery factory ทำงานจริงแล้ว

การไม่มี YAML error ยังไม่แปลว่าแมพไม่มีบัค; checklist ในโลกและ performance proof เป็นเงื่อนไขก่อนปล่อยให้ผู้เล่น
