# Luma — หินกันบ้านและระดับ ProtectionStones

ฉบับออกแบบ 5 ตุลาคม 2026 สำหรับโลกสร้างบ้านของ Luma และ Java client เป้าหมาย 1.16.5 ขึ้นไป
เป็นสเปกสำหรับพัฒนาและทดสอบ ยังไม่มี ProtectionStones/WorldGuard ที่ติดตั้งในโลกจริงของโปรเจกต์นี้
ค่าราคาและข้อจำกัดด้านล่างเป็นค่าเริ่มต้นของ Luma ที่เสนอให้ทดลองบน staging ไม่ใช่ค่าที่อ่านได้จากเซิร์ฟในภาพหรือคลิป

## 1. คำตอบเรื่องความกว้างและขนาดสูงสุด

กำหนด 5 ระดับ ตั้งแต่ **11 × 11 ถึง 81 × 81 บล็อกต่อแปลง**
เพดาน 81 × 81 เป็นนโยบายของ Luma; ขนาดของ ProtectionStones ปรับได้ตาม config ไม่ได้มีเพดานสากล 81

| ระดับ | ID / alias ที่เสนอ | ชื่อไทย | x_radius / z_radius | กว้าง × ยาวจริง | พื้นที่แนวนอน | เหมาะกับ |
|---|---|---|---:|---|---:|---|
| I | `luma_land_1` | ศิลาคุ้มครองบ้านเล็ก | 5 | 11 × 11 | 121 บล็อก | บ้านเริ่มต้นและสวนเล็ก |
| II | `luma_land_2` | ศิลาคุ้มครองเรือนพัก | 10 | 21 × 21 | 441 บล็อก | บ้านพร้อมสวนและคอกสัตว์ |
| III | `luma_land_3` | ศิลาคุ้มครองคฤหาสน์ | 15 | 31 × 31 | 961 บล็อก | บ้านใหญ่ ร้าน และพื้นที่เพื่อน |
| IV | `luma_land_4` | ศิลาคุ้มครองปราสาท | 25 | 51 × 51 | 2,601 บล็อก | คฤหาสน์แฟนตาซีและลานกิจกรรม |
| V | `luma_land_5` | ศิลาคุ้มครองชุมชน | 40 | 81 × 81 | 6,561 บล็อก | กลุ่มเพื่อนหรือชุมชนขนาดย่อม |

สูตรเมื่อใช้ radius แบบบล็อก: `ความกว้าง = 2 × radius + 1`
ที่ตั้งหิน X=100, Z=100 และ radius=5 จะป้องกัน X95..105, Z95..105 รวมขอบทั้งสองฝั่งได้ 11 × 11
ตรวจค่าจริงจาก region bounds: `maxX - minX + 1` และ `maxZ - minZ + 1`; lore, GUI และเว็บต้องอ่านค่าชุดเดียวกัน

ตัวอย่าง 10 × 10 ในคลิปและเอกสารเดิมยังเก็บเป็นหลักฐานอ้างอิง ส่วนตารางใหม่นี้เป็นขนาดจริงที่เลือกใช้กับ Luma
หากภายหลังต้องการ 10 × 10 เป๊ะ ต้องพัฒนาขอบเขตแบบไม่สมมาตรเองและทดสอบ adapter; การใส่ radius=10 จะได้ 21 × 21
คอนฟิกต้นฉบับผู้พัฒนาระบุวิธีนับ radius ไว้ชัดเจนใน [block1.toml](https://github.com/espidev/ProtectionStones/blob/master/src/main/resources/block1.toml)

## 2. ความสูง โลก และจำนวนแปลง

- ทุกระดับใช้ `y_radius = -1` เพื่อป้องกันทุกระดับความสูงของโลก ตามความหมายของคอนฟิก ProtectionStones
- adapter อ่าน `world.minHeight` ถึง `world.maxHeight - 1` จาก runtime; ห้าม hardcode 0..255 เป็นเขตป้องกันบน backend ใหม่
- อาคาร/จุดบริการหลักสำหรับ client เก่ายังคงออกแบบใน Y16..200 ตามแผน compatibility; การป้องกันเต็มความสูงไม่ทำให้ client เก่าใช้งานบล็อกนอกช่วงที่รองรับได้
- world whitelist เริ่มที่ logical world `housing` เท่านั้น ชื่อ Bukkit ที่เสนอคือ `luma_housing` และต้องผูกกับ world UUID ตอน deploy
- ห้ามเคลมใน `lobby`, `resource`, `dungeon`, `boss`, `arena`; โลกทรัพยากรที่จะ reset แยกจากโลกบ้านที่คงอยู่ถาวร
- เริ่มได้ 1 แปลงต่อ Minecraft UUID; ปลดล็อกแปลงที่ 2 จากเควส `housing_second_plot` หลังมีแปลงระดับ III อย่างน้อย 1 แปลง
- เพดานปกติ 2 แปลงต่อคน และแต่ละแปลงไม่เกินระดับ V: พื้นที่สูงสุดรวม 13,122 บล็อกแนวนอน
- เป็นเพดานต่อบัญชี ไม่ได้ป้องกันการใช้หลายบัญชีโดยตัวมันเอง; staff ตรวจการยึดที่จำนวนมากด้วยข้อมูล claim/audit ตามนโยบายของเซิร์ฟ
- ปิดการ merge/overlap ทุกแบบใน launch รวมแปลงของเจ้าของคนเดียวกัน เพื่อไม่ให้เกิดพื้นที่เกินเพดานและรักษา geometry ของการ upgrade
- ช่องว่างระหว่างขอบแปลงอย่างน้อย 5 บล็อก ตรวจขอบเขตใหม่ทั้งแปลงก่อนสร้าง/ขยาย; ห้ามทับ reserve region ถนน และ world border
- ยังไม่เปิดขายหรือเช่าแปลง โอน ownership ภาษีที่ดิน หรือปล่อยที่คืนอัตโนมัติเมื่อเจ้าของ offline
- staff cleanup ต้อง preview เป้าหมาย สำรอง region และข้อมูลบ้านก่อนดำเนินการ ไม่ลบบ้านจากตาราง inactivity โดยอัตโนมัติ

ชื่อโลกในเอกสารนี้เป็น deployment proposal ไม่ใช่โลกที่มีอยู่จริงในเครื่องแล้ว

## 3. ราคาและการปลดล็อกที่เสนอ

| ระดับ | มูลค่าหินมาตรฐาน (ทองในเกม) | ค่า upgrade จากระดับก่อนหน้า | เงื่อนไข |
|---|---:|---:|---|
| I | 1,000 | — | เควสสอนบ้าน; หินชิ้นแรกให้ฟรีครั้งเดียว |
| II | 5,000 | 4,000 | จบ `housing_garden` |
| III | 20,000 | 15,000 | จบ `housing_workshop` |
| IV | 75,000 | 55,000 | จบ `housing_estate` |
| V | 200,000 | 125,000 | จบ `housing_community` |

ใช้ gold.wallet ผ่าน FantasyCore Economy เท่านั้น ไม่ใช้เงินจริงหรือ red เพื่อซื้อพื้นที่เพิ่มใน launch
หินเริ่มต้นฟรีมี upgrade credit ตามมูลค่ามาตรฐาน 1,000 แต่ไม่ขายคืนเป็นเงิน; ต้องแสดงต้นทุนก่อนยืนยันทุกครั้ง
ราคาเป็นจุดเริ่มต้นสำหรับ balance pass ตรวจเวลาเล่นที่ต้องใช้จากรายได้ Jobs/เควสจริงก่อน release ไม่ถือว่าเศรษฐกิจสมดุลแล้ว
หินและใบอนุญาตผูกเจ้าของ ไม่ใช้การแลกหินระหว่างผู้เล่นเพื่อข้าม unlock หรือ claim count
สร้างหินใหม่ด้วย provider API ที่ถูกต้อง และเก็บ serial/owner ใน Core; แร่ที่ขุดได้หรือไอเทมเปลี่ยนชื่อใน anvil ไม่เป็นหินกันบ้าน
ใช้ `restrict_obtaining` ของ ProtectionStones ร่วมกับการตรวจ server-side ของ Core ไม่ใช้ชื่อ/lore เป็นหลักฐานสิทธิ์

## 4. สมาชิกและการป้องกัน

| บทบาท | สร้าง/ทุบ | หีบและเฟอร์นิเจอร์ | ดูข้อมูลแปลง | เพิ่ม/ลบสมาชิก | upgrade/unclaim |
|---|---|---|---|---|---|
| เจ้าของ | ได้ | ได้ | ได้ | ได้ | ได้หลัง preview |
| เพื่อนที่เชื่อถือ | ได้ | ได้ | ได้ | ไม่ได้ | ไม่ได้ |
| ผู้เยี่ยมชม | ไม่ได้ | ไม่ได้ | เฉพาะข้อมูล public | ไม่ได้ | ไม่ได้ |
| Moderator | ไม่ bypass อัตโนมัติ | ตรวจตามสิทธิ์ช่วยเหลือ | ตามงาน | ไม่ได้ | ไม่ได้ |
| Owner ของเซิร์ฟ | break-glass ตามสิทธิ์แยก | ตามสิทธิ์ | ได้ | ตามงาน | ต้องมีเหตุผลและ audit |

เพื่อนใน launch มีบทบาทเดียว: **เชื่อถือให้สร้างและเปิดหีบได้** GUI ต้องบอกผลนี้ก่อนเพิ่ม
หากต้องการเพื่อนที่สร้างได้แต่เปิดหีบไม่ได้ ให้เพิ่ม Core role adapter ใน phase ถัดไป; membership ของ WorldGuard เพียงอย่างเดียวไม่แสดงว่าบทบาทละเอียดนี้ทำงานแล้ว
resolve ชื่อเป็น UUID ที่ยืนยันกับเซิร์ฟแล้ว; ไม่สร้างบัญชีจากชื่อที่ผู้เล่นพิมพ์เฉย ๆ
เจ้าของคนเดียวต่อแปลงใน launch ไม่เปิด native add-owner ให้ผู้เล่น เพื่อป้องกันช่องทางข้ามจำนวนแปลง

นโยบายเริ่มต้น:

- ทุก claim ปิด PvP, TNT, creeper/wither/ghast damage, other explosions และไฟลามตาม capability ที่ build รองรับ
- คนอื่นเข้าเยี่ยมชมและเดินผ่านได้ แต่สร้าง ทุบ เปิด container หมุน/ทุบ item frame ทำร้ายสัตว์เลี้ยง หรือย้ายเฟอร์นิเจอร์ไม่ได้
- แปลง private ที่ปิด entry เป็นส่วนขยายภายหลัง ต้องมี visitor preview และทางออกที่ปลอดภัยก่อนเปิด
- piston/hopper/น้ำ/ลาวาข้ามขอบ claim ของคนอื่นต้องถูกป้องกันหรือกำหนดวิธีติดตั้งที่ปลอดภัย; ต้องทดสอบทุกชนิด ไม่อ้างว่า flag เดียวครอบคลุมทั้งหมด
- ฟาร์ม piston ภายในแปลงเจ้าของควรทำงานได้; ไม่ตั้ง global `pistons deny` โดยไม่ประเมินผลฟาร์ม
- หิน Protect ต้องไม่ถูกผลักโดย piston ไม่ถูกระเบิด และไม่ถูกย้ายจากการทำลายโดยสมาชิก
- native block, armor stand, item frame, pet, custom furniture และ model interaction ตรวจ ownership/region แยกผ่าน adapter ที่รับผิดชอบ
- ป้องกันทุก entry point รวมคำสั่ง namespace, GUI, wand, API และ automation; ปิดเส้นทาง PS native ที่ข้ามกติกา Core

WorldGuard แยก membership และ flag override การตั้ง `build allow` หรือ `block-break deny` บนแปลงผู้เล่นแบบเหมาอาจทำให้สิทธิ์ผิด
การตั้งค่าจริงต้องอ้างอิง [WorldGuard region flags](https://worldguard.enginehub.org/en/latest/regions/flags/)
และทดสอบ region priority กับพื้นที่เมืองและทางสาธารณะ ไม่อ้างว่าข้อกำหนดด้านบนถูก enforce แล้ว

## 5. เมนูและคำสั่ง

`/land` เปิดเมนูไทย 54 ช่อง: แปลงของฉัน, ซื้อหิน, ดูขอบเขต, สมาชิก, ตั้งจุดกลับบ้าน, อัปเกรด, ประวัติ, ถอนแปลง
ใช้ชื่อหิน สีระดับ ไอคอนบ้าน และข้อความขนาดจริง; client ไม่รับ pack ยังใช้ vanilla icon/ข้อความได้
คำสั่ง Core ที่เสนอ: `/land`, `/land view`, `/land members`, `/land upgrade`, `/land home`
คำสั่งเหล่านี้ยังไม่มี executable plugin ในโปรเจกต์นี้

เพิ่มสมาชิกต้องแสดงว่าเขาจะสร้าง/ทุบ/เปิดหีบได้ ไม่ใช้ checkbox ที่บอกเพียง “เพื่อน”
upgrade แสดงกรอบเดิม/ใหม่ ราคาส่วนต่าง สิ่งกีดขวาง และพิกัดคงเดิม ไม่เรียก animation จนกว่าการเปลี่ยนแปลงบันทึกสำเร็จ
unclaim ผ่าน Core เท่านั้น: แจ้งว่าบ้านและบล็อกยังอยู่ แต่พื้นที่จะไม่ป้องกัน; ยืนยันด้วยชื่อแปลงและคืนหินเดิมครั้งเดียว
สมาชิกและ home ถูกยกเลิกตาม policy เมื่อ unclaim; ไม่มีการคืนเงินต้นทุน upgrade และไม่ delete บล็อกบ้าน

## 6. ชุด config ที่ต้องเตรียมหลังเลือก build

คัดลอก template ที่มากับ JAR ที่เลือกเป็นหิน 5 ชนิด ใช้ material ต่างกัน 5 แบบ และ alias ตามตาราง
แนวทาง vanilla material: IRON_ORE, LAPIS_ORE, GOLD_ORE, DIAMOND_ORE, EMERALD_ORE; ตรวจ pack fallback และ recognition ของรุ่นจริง
model ใน inventory สามารถต่างกันตามระดับ แต่บล็อกที่วางกับ decorative model ต้องไม่เป็น authority สองชุด

ตัวอย่าง override สำหรับระดับ I **ไม่ใช่ไฟล์ TOML เต็มสำหรับนำไปวางทับทันที**:

```toml
alias = "luma_land_1"
restrict_obtaining = true
world_list_type = "whitelist"
worlds = ["luma_housing"]
prevent_block_place_in_restricted_world = true

[region]
x_radius = 5
y_radius = -1
z_radius = 5
chunk_radius = -1
allow_overlap_unowned_regions = false
allow_merging = false
```

ห้ามเปิด chunk mode พร้อมคาดหวังความกว้างจาก radius; ในโหมด chunk ต้องออกแบบขนาดตาม chunk อีกชุด
ระยะห่างใน native PS วัดจากหินถึงขอบอีก region ไม่ใช่ช่องว่าง edge-to-edge โดยตรง
เพื่อให้ขอบเว้น 5 บล็อกเหมือนกันทุกระดับ Core ตรวจ bounding boxes และใช้ค่าคอนฟิก native เป็นการเสริม ไม่ใส่เลข 5 แล้วอ้างว่าได้ผลเหมือนกันทุก tier
สิทธิ์จำนวนรวม จำนวนต่อ alias และ per-world ต้องตรวจ priority/LuckPerms และ native commands กับ build ที่เลือก
ปลั๊กอิน PS ไม่ได้ enforce ราคาส่วนต่าง/quest/serial/เพดานรวมที่เรากำหนดทั้งหมดโดยอัตโนมัติ ต้องเขียน `FantasyCore ClaimAdapter`

ตอนตรวจครั้งนี้ README ผู้พัฒนาระบุ dependency ของ PS 2.10.6 กับ server รุ่นใหม่กว่า client 1.16.5
เป้าหมาย 1.16.5 หมายถึง **client เข้า backend รุ่นที่เลือกผ่าน ViaBackwards** ไม่ใช่เอา JAR ใหม่ไปลง Paper 1.16.5
ยังไม่ล็อก build จนกว่า dependency และชุดปลั๊กอินอื่นจะผ่าน compatibility matrix
แหล่งข้อมูล: [ProtectionStones README](https://github.com/espidev/ProtectionStones), [native permissions](https://github.com/espidev/ProtectionStones/blob/master/src/main/resources/plugin.yml)

## 7. Upgrade และ recovery ที่ต้องเขียน

Core เป็นเจ้าของ entitlement/ราคา/operation journal ส่วน WorldGuard/PS เป็นเจ้าของ region ที่ใช้ enforce
การเปลี่ยน region และ SQL ไม่ใช่ transaction เดียว ต้องมี recovery ที่ระบุ state ชัด:

1. lock การเปลี่ยนแปลง claim ตาม worldUUID+regionID ตรวจ owner, revision, quest, funds และ target bounds
2. ตรวจ reserve/แปลงอื่น/border/gap รวมถึง home และ objects ที่สัมพันธ์กับแปลง; เงินยังไม่ถูกตัดใน preview
3. ยืนยันแล้ว reserve เงินด้วย operation UUID และ snapshot ค่าเก่า โดยไม่ให้ใช้ยอดที่ reserve ไปจ่ายงานอื่น
4. เปลี่ยน bounds/type/flags ผ่าน adapter ของ build ที่เลือก คง regionID, owners, members, home และ protected stone identity
5. persist region แล้วบันทึก committed debit + tier revision; cache, lore, POI และแอนิเมชันเปลี่ยนหลัง success
6. หาก crash ให้ reconcile operation เดิมกับ region snapshot ก่อนเปิดการแก้แปลงใหม่ ไม่หักเงินหรือแจกหินซ้ำ
7. หาก apply ไม่ได้ คืน reservation ครั้งเดียวและคงแปลงเดิม; ถ้าไม่รู้ว่า persist ถึงไหน ให้ review/freeze claim ไม่ลบ region เพื่อเริ่มใหม่

ห้ามทำ “ลบแปลงเก่า → วางหินใหม่” โดยปล่อยบ้านไม่ป้องกันระหว่างสองงาน
ห้ามสมมติว่าเปลี่ยน type ของ PS อย่างเดียวแล้ว bounds จะขยายเอง; ตรวจ API/build และเขียน integration proof
world data, region storage และ SQL ต้อง backup เป็นชุด recovery ที่สัมพันธ์กัน

## 8. Acceptance ที่ต้องผ่าน

| กรณี | ผลที่ต้องเห็น |
|---|---|
| ทุก tier วางที่ X100/Z100 | bounds และจำนวนบล็อกตรงตาราง รวมบล็อกขอบ |
| ชื่อหิน/lore/GUI/web | ขนาดเดียวกันกับ bounds จริง |
| ความสูง | ทดสอบ Y min/max ที่ build รองรับ ไม่มีช่องทางเข้าไปขุดใต้ claim |
| สลับ client 1.16.5/รุ่นใหม่/ปฏิเสธ pack | สิทธิ์และจำนวนแปลงตรงกัน |
| แร่ขุดได้/หินปลอม/serial ถูกใช้แล้ว | ไม่สร้าง claim |
| native /ps และคำสั่ง namespace | ข้าม tier, count, unlock หรือ Core journal ไม่ได้ |
| upgrade ติดบ้านเพื่อน/ถนน/border | ปฏิเสธ ไม่ตัดเงิน แปลงเดิมไม่หาย |
| upgrade พร้อมกัน/double-click | เปลี่ยนเพียง revision เดียวและคิดส่วนต่างครั้งเดียว |
| offline member เปลี่ยนชื่อ/ออกกิลด์ | ownership ไม่ย้ายตาม display name; guild role ไม่ให้สิทธิ์บ้านอัตโนมัติ |
| คนอื่น/เพื่อน trusted | สิทธิ์ block/container/pet/furniture ตรงตาราง |
| piston/hopper/fluid/explosion สองฝั่ง | ไม่ขโมยหรือทำลายข้ามขอบ; ฟาร์มเจ้าของที่อนุญาตยังทำงาน |
| unclaim/restart/inventory เต็ม | คืนหินครั้งเดียวผ่าน mailbox ถ้าจำเป็น; ไม่แจกพร้อม world drop ซ้ำ |
| restart หลัง reserve/apply/persist | reconcile ได้ ไม่มี free upgrade/เงินหาย/แปลงหาย |
| ยกเลิก claim หลังมี home | teleport เข้าแปลงที่ไม่เป็นเจ้าของโดยใช้ home เก่าไม่ได้ |
| restore backup | bounds, สมาชิก, tier, serial และเงินสัมพันธ์กับ recovery point |

การกันโกง client และ DDoS เป็นระบบแยก ไม่ได้รับจาก ProtectionStones; เอกสารนี้ไม่รับรองว่าปิดช่องโหว่ทั้งหมดแล้ว

## 9. เชื่อมกับแมพ AdminPanel และเว็บ

โซน 09 ใช้ portal keeper เดิมเปิดเมนู Travel/บ้าน/Protect; บ้านสอน 7 × 7 อยู่ในมุม 13 × 13 แสดงระดับ I ที่ป้องกัน 11 × 11 จริง
ตารางระดับใหญ่ใช้ป้าย/GUI ไม่สร้าง demo ปราสาท 81 × 81 เพิ่มใน lobby ที่ลดสเกลแล้ว
admin ใช้ permission แยก `.claims.inspect`, `.claims.adjust`, `.claims.unclaim`; ทุก write ระบุ actor/reason/revision/operationID
เว็บแสดงคู่มือและระดับ public ส่วนแปลงของฉันต้องผ่าน session+game UUID; ไม่ publish ชื่อบ้าน พิกัดและสมาชิกทั้งหมดบน BlueMap
ค่าเริ่มต้น home/claim location เป็น private มี opt-in เผยแพร่แปลงตาม policy ที่เจ้าของเลือก

อ่านร่วมกับ [ชุดระบบตามภาพ](CASUAL-SURVIVAL-SYSTEMS-th.md), [แผนระบบหลัก](SERVER-SYSTEMS-PLAN-th.md), [ใบงานสร้างแมพ](ZONE-BUILD-TICKETS-th.md)
