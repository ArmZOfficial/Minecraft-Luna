# Moonfall — ดันเจี้ยนฝึกเดี่ยว (ประวัติ FantasyCore v0.7)

**รุ่นปัจจุบัน v0.8:** อ่าน [Party Dungeons](PARTY-DUNGEONS-th.md) ก่อนติดตั้ง ใช้ JAR0.8/schema7; เพิ่ม2ห้องปาร์ตี้และreconnect60s แม้solo บทนี้เก็บbehavior/schema/ขั้นติดตั้งv0.7ไว้เพื่ออ้างอิงย้อนหลัง ไม่มีการเปิดเซิร์ฟจริงแล้ว

วันที่ 5 ต.ค. 2026 · สถานะ: source/JAR สำหรับ staging, **ยังไม่เคยรัน Minecraft หรือเปิดดันให้เล่นจริง**

รุ่นนี้ต่อจาก [คอนเซปต์ซากวิหารจันทรา 4 ภาพ](../output/lobby-concept/dungeons/moonfall/README-th.md)
มีโค้ดสร้างโครงแมพ 144×144, เข้า/ออก, ต่อสู้ 3 ห้อง, BossBar, ทุบพื้นมีวงเตือน, protect และส่งรางวัลเข้า `/mail`
เปิดทีละหนึ่งผู้เล่นในโลกฝึกเฉพาะ เพื่อไม่ให้คนละรอบเกิดม็อบหรือรับสิทธิ์รางวัลร่วมกัน
นี่คือโหมดฝึกสำหรับพิสูจน์ flow; ปาร์ตี้ 2–4 คน/instance หลายชุด/บอสโมเดลอนิเมชันยังตามแผนหลัก
บล็อกที่สร้างเป็นโครงและพื้นรูน ไม่ใช่การแปลงภาพ AI เป็นแมพที่เหมือนทุกบล็อก และยังไม่มี NPC/พร็อพภายในอัตโนมัติ

## 1. เปิดทดสอบอย่างเป็นลำดับ

1. หยุดเซิร์ฟและสำรอง Core DB พร้อม `-wal/-shm` ถ้ามี, playerdata, โลก, regions และ config เป็นชุดเดียวกัน
2. Build/copy `FantasyCore-0.7.0.jar` โดยใช้ [ขั้นตอน staging](../server/README-th.md); เอา JAR Core เก่าออกจาก `plugins` ตามวิธีเดิม
3. เปิดบน staging ที่เจ้าของยอมรับ EULA แล้วเท่านั้น ตรวจ log `schema v6` และ `/fa doctor`
4. ไฟล์ใหม่ `plugins/FantasyCore/dungeons.yml` มี `enabled: false`; config เดิมอื่นไม่ถูกเขียนทับ
5. `/fa dungeon build สร้างแมพฝึกสำหรับทดสอบ` → อ่าน preview แล้ว `/fa confirm <รหัส>` (60 วินาที, ผูกผู้สั่ง, ใช้ครั้งเดียว)
6. รอข้อความสร้างเสร็จ → `/fa dungeon visit` เพื่อตรวจแมพ โดยยังไม่มีม็อบ/รางวัล ใช้ `/dungeon leave` กลับออก
7. ทำ checklist R ใน [server/README-th.md](../server/README-th.md) รวมดู client 26.2 และ 1.16.5 จริง
8. เปลี่ยน `dungeons.yml` เป็น `enabled: true` และ restart บน staging เมื่อพร้อมทดสอบ combat; ไม่ใช้ `/reload`
9. `/dungeon` เปิดเมนู หรือ `/dungeon join`; ต้อง Survival ยืนบนพื้นปลอดภัย ไม่ขี่/บิน/ต่อสู้/มีวาร์ปค้าง
10. เดินผ่านทางเข้าห้องรูน → เคลียร์ม็อบ → ประตูเปิด → ศิลาจันทร์ → บอส → รางวัลรอ `/mail` และกลับจุดก่อนเข้า

| คำสั่ง/Action | สิทธิ์ | ผลจริง |
|---|---|---|
| `/dungeon`, `/dun` | `fantasy.dungeon` ผ่าน `fantasy.player` | เมนูสถานะและเงื่อนไขก่อนเข้า |
| `/dungeon join` | เหมือนเมนู | จองรอบใน DB ก่อนวาร์ป ถ้าโลกว่างและแมพพร้อม |
| `/dungeon leave` | คำสั่งพื้นฐาน | ยกเลิกรอบ/กลับจุดเดิม, กดซ้ำเพื่อลองกลับเมื่อรายการค้าง |
| `dungeon.main` | `fantasy.dungeon` | เมนูเดียวกันผ่าน NPC/anchor/Citizens; ไม่เปิด teleport ลัด |
| `/fa dungeon status` | `fantasyadmin.view` | เปิด/ปิด, readiness ของแมพ, occupied |
| `/fa dungeon build <เหตุผล>` | `fantasyadmin.dungeon.manage` | preview + confirm + audit; สร้างเฉพาะโลกที่ Core เป็นเจ้าของ |
| `/fa dungeon visit` | manage + เป็น Player | ตรวจแมพเมื่อไม่มีรอบ/ผู้เล่นอยู่; ไม่ได้รางวัล |
| `/fa dungeon abort <เหตุผล>` | manage | preview + confirm + audit; หยุดม็อบ/skill และเริ่มส่งคนกลับ |

build/abort เป็นคำสั่งของผู้ดูแลในเกมที่ต้องยืนยันตามระบบเดิมของ `/fa`; ไม่ใช่การขออนุญาตเพิ่มจาก agent
สถานะ "แมพพร้อม" หมายถึงโค้ดวางบล็อกจบและบันทึกแล้ว ไม่ได้หมายความว่า checklist Minecraft ผ่านแล้ว

## 2. โลกและผังที่โค้ดสร้าง

ชื่อโลกตรึงไว้ `luma_moonfall_training` เป็น NORMAL void world โดย ChunkGenerator ของ Core
**ไม่ใส่ชื่อนี้ใน `config.yml worlds`, ไม่ให้ Multiverse/provider อื่นสร้างโลกชื่อนี้ก่อน** และไม่ใช้เป็นโลกหลัก `level-name`
`enabled: true` ไม่สร้างโลกเอง ต้องเรียก build ก่อน
โฟลเดอร์ใหม่มี marker `.fantasycore-moonfall-owner`; ถ้าพบโฟลเดอร์เดิม/โลกโหลดอยู่โดยไม่มี ownership จะหยุดโดยไม่เขียนทับ
ไม่รับ symlink ของโฟลเดอร์/marker; อย่าคัดลอก marker ไปยังโลกอื่นหรือแก้ไฟล์นี้เพื่อข้ามการตรวจ
โลกที่ build เสร็จแล้วไม่ถูก rebuild ทับเมื่อเรียกซ้ำ จึงเก็บงานตกแต่งที่ทำต่อไว้
โลกของ Core ที่สร้างค้างสามารถ build ต่อจากแผนเดิมได้เมื่อไม่มีผู้เล่น โดย readiness ยังปิดไว้จนงานจบ

วางบล็อกบน main thread ตาม Paper API: ไม่เกิน 400 placements/tick และหยุดรอบเมื่อใช้ประมาณ 3ms
นี่เป็น budget ของ loop; การสร้าง chunk และ `world.save()` ยังอาจกินเวลาเกิน ต้องวัด MSPT จริง
งานวางโลกไม่ใช้ async; งาน DB ใช้ executor เดิม ส่วนวาร์ปโหลดปลายทางด้วย `getChunkAtAsync` ก่อนตรวจพื้นอีกครั้ง
chunk transfer มี timeout 300 ticks และป้องกัน callback ช้าไม่ให้วาร์ปซ้ำ; PREPARING มี timeout 15 วินาทีจริง

| ห้อง/ทางเชื่อม | X | Z | Y เท้าบน landing | หน้าที่ |
|---|---|---|---:|---|
| ทางเข้า | 8..36 | 8..32 | 88 | จุดลง 22.5,88,18.5 yaw0 |
| บันได A | 20..24 | 33..42 | 88→78 | stair facing north (ด้านสูงหันกลับทางเข้า) |
| ห้องรูน | 10..34 | 43..69 | 78 | ต่อสู้ stage0; checkpoint22.5,78,46.5 |
| บันได B | 35..54 | 59..63 | 78→58 | facing west; gate X35 Y78..81 Z59..63 |
| ห้องทางผ่าน/โถงสะพาน | 55..82 | 49..73 | 58 | พื้นโถงเต็มในโหมดฝึก; สะพาน/หุบเหวตามภาพเป็นงานตกแต่งต่อ |
| บันได C | 83..102 | 59..63 | 58→38 | facing west |
| ห้องศิลาจันทร์ | 103..129 | 48..74 | 38 | stage1; checkpoint106.5,38,61.5 |
| บันได D | 114..118 | 75..92 | 38→20 | facing north; gate Z75 X114..118 Y38..41 |
| landing | 114..118 | 93..94 | 20 | เชื่อมประตูห้องบอส |
| ห้องบอสวงกลม | 93..139 | 95..141 | 20 | ศูนย์116.5,20,118.5; checkpoint116.5,20,100.5 |

พื้น landing อยู่ Y−1, บันไดแต่ละแถวลดหนึ่งบล็อกและมีครึ่งระดับตามรูปทรง stairs
พื้นทางเดินมีฐานหินรองรับ, corridor กว้าง5พร้อมผนัง/หลังคา; พื้นบอสวงในรัศมี20ไม่มีเสา/พร็อพขวาง
palette รุ่นฝึกใช้ stone bricks, polished andesite, prismarine light, cyan concrete, purpur และ gold เพื่อมีบล็อกเดิมที่ client1.16.5รู้จัก
ไม่ใช้ crystal entity หลายร้อยตัว, ไม่มีไฟจริง/หีบ loot บนพื้น; ยังต้องตรวจ light updates/block connection และเดินบันไดในเกม

## 3. ห้องต่อสู้และตัวเลขบาลานซ์เริ่มต้น

| Stage | ม็อบ | จุดเกิด X,Y,Z | HP/ตัว | โจมตีดิบ |
|---|---|---|---:|---:|
| 0 รูน | Zombieผู้ใหญ่3ตัว | 18.5,78,59.5 / 26.5,78,59.5 / 22.5,78,63.5 | 30 | 4 |
| 1 ศิลาจันทร์ | Huskผู้ใหญ่2ตัว | 117.5,38,56.5 / 121.5,38,66.5 | 50 | 5 |
| 2 ผู้พิทักษ์ | Huskผู้ใหญ่1ตัว (fallback ของบอส) | 116.5,20,118.5 | 180 | 6 |

Husk ยังมี native hunger effect ของมัน ต้องทดสอบอาหาร/เกราะร่วมกับ starter gear v2; ค่าเหล่านี้ยังไม่ใช่ balance ที่ยืนยันแล้ว
ไม่มี random gear ของ Zombie, มีหมวก chainmailไม่ดรอป, ปิดการเผากลางวัน/หยิบของ/พังประตู และไม่มี natural reinforcements
Mob capปกติสูงสุด3ตัวต่อคลื่น; แถวใหม่ไม่เกิดจนคลื่นเก่าตายครบและ DB ยืนยัน stage
Mobมี run marker ของ Core, spawn CUSTOM, โลกไม่อยู่ใน `monsters.yml` เริ่มต้น จึงไม่ซ้อน [depth scaling](MONSTERS-th.md)
ไม่ใช้ ItemsCore/Mythic/ModelEngine สำหรับ AI ของโหมดฝึกนี้; provider ที่ cancel/แปลง entity ต้องทดสอบก่อนเปิดร่วม

บอสเป็น Huskธรรมดาและมีสกิลทุบพื้น: ล็อกจุดตอนเตรียมท่า, วงม่วงรัศมี4, AIหยุดช่วง windup25ticks (~1.25sที่20TPS)
จบ windup ทำ raw damage8 ผ่าน damage event/เกราะถ้าผู้เล่นอยู่ในวงและ line of sight ตรง; ไม่ลบเลือดแบบข้ามเกราะ
ท่าครั้งแรกหลังเข้า8s; รอบถัดไป10s, เลือด≤50%ลดเป็น8s (เวลา tick จึงช้าลงถ้า TPSลด)
ยังไม่มีโมเดล4.5บล็อก/สามphase/add waves/cone sweep/animation hitbox; รายการเหล่านั้นอยู่ในใบงานบอส
BossBarแสดง HPรวมของม็อบที่ยังมีชีวิตในห้อง; เปลี่ยนชื่อและสีเป็นบอสในstage2 ไม่มีการปลอมตัวเลขใน hologram
ม็อบ/ผู้เล่นถูกดึงกลับ checkpoint ถ้าออกห้องระหว่างคลื่น; การย้าย game mode/บิน/ขี่/ม็อบหายก่อน death event ยกเลิกรอบ
ไม่ให้ยิงฆ่าม็อบจากห้องก่อนคลื่นเริ่ม; การตีใช้สิทธิ์ของผู้เล่นคนเดียวที่จองรอบ

## 4. Protect/การกลับออก

Coreคุมเฉพาะโลกฝึกที่มี ownership: blockbreak/place/bucket/fluid/piston/fire/spread/explosion/entity changeblock/hanging
ปิด interaction กับ container/บล็อก, pickup/drop item, การขี่, portal, natural/provider creature spawnที่ไม่ได้อยู่ในrun
Mobและblockไม่ดรอป, monster XPเป็น0, PVPปิด, keep-inventory/keep-level และgold loss0ในโลกฝึกเท่านั้น
**เกราะ/อาวุธยังเสีย durability, อาหาร/ลูกธนู/ยาที่ใช้จริงยังถูกใช้** — โหมดฝึกไม่สำรองหรือคืนinventoryให้ใหม่
Ender pearl/chorus/plugin travelเข้าออกไม่ได้ผ่าน teleport event; ใช้ `/dungeon leave` ซึ่ง bypass combat lockเพื่อออกจากรอบโดยยกเลิก
Core/NPCในเมืองอยู่นอกโลกนี้ ใช้ protectเดิม; ไม่มีการเปิด staff edit bypass ระหว่างเล่น
WorldEdit/คำสั่ง console/providerที่เขียนโลกโดยไม่ผ่าน Bukkit block eventsเป็นสิทธิ์แอดมินที่ระบบนี้กันไม่ได้ ต้องหยุดรอบก่อนแก้

ก่อนเข้าบันทึก UUIDโลกและตำแหน่ง/มุมหันจริง เมื่อออกลองจุดเดิมหลังโหลดchunkและตรวจ `LandingValidator`
จุดเดิมเสีย/โลกไม่โหลด → ลองจุดเกิดhubที่ปลอดภัย ถ้าทั้งสองจุดไม่พร้อม/WorldGuard cancel จะคง `needs_return=1`
ไม่ประกาศกลับสำเร็จถ้า teleportล้มเหลว ใช้ `/dungeon leave` ซ้ำ/ทีมงานแก้safe spawnและตรวจlog
ตาย/quit/gamemode change/timeout/abortหยุดรอบและ skill/tasks/markers/chunk tickets; reconnectไม่resumeการต่อสู้
รีสตาร์ตย้ายPREPARING/ACTIVEเป็นABORTEDโดยไม่แจกจากความคืบหน้าที่ไม่ครบ; login/re-enableพาคนที่ยังอยู่โลกฝึกกลับ
โลกฝึกไม่ถูกลบ/คัดลอกทับหลังจบและไม่มีงานrecursive delete; ปุ่มเข้าให้รอจนรอบ/visitorออกเรียบร้อย

## 5. รางวัลและฐานข้อมูล

มีschema6 เพิ่ม `dungeon_runs`, `dungeon_rewards` โดยไม่แก้ตารางเงิน/บ้าน/mailเดิม
run มี UUIDเจ้าของ, PREPARING/ACTIVE/COMPLETED/ABORTED, stage0..3, จุดกลับ, needs_return, frozen rewardbytes/version
unique indexล็อก laneเมื่อPREPARING/ACTIVE และล็อกผู้เล่นที่ยังมีภาระกลับ; menu/command/NPCใช้บริการเดียวกัน
ผ่าน3 encounterที่DBยืนยัน → `complete`บันทึกCOMPLETED + daily receipt + mail + auditในtransactionเดียว
ไม่ใช้ GUI/chestprop/ชื่อม็อบเป็นreceipt และไม่ให้ clientส่งผลชนะเอง

รางวัลทดลอง: **เศษจันทรา ×2** = PRISMARINE_SHARDมีชื่อ/loreและPDC `fantasycore:dungeon_reward=1`
เป็นวัตถุดิบเควสที่ยังไม่มีสูตรใช้ในรุ่นนี้ ไม่ใช่เงินขายได้/เครดิตเว็บ/serial gear; ไม่รับเป็นวัตถุดิบธรรมดาในExchange
รับรางวัลได้1ครั้งต่อUUID+dungeon+วันจบตามAsia/Bangkok (00:00วันใหม่); เล่นฟรีซ้ำได้แต่ไม่แจกเพิ่มในวันเดียวกัน
ไม่คิดค่าเข้า/ไม่ให้ทอง, เงินแดง, XP, paid key/drop chance; จุดขายเว็บและสินค้าต่อสู้ไม่ผูกกับโหมดนี้
rewardbytesถูกตรึงตอนจองก่อนวาร์ป, completeซ้ำ/สองthread/late quitไม่ส่งmailซ้ำ และรางวัลคงอยู่แม้ออฟไลน์
crashหลังส่งmailแต่ก่อนกลับไม่แจกใหม่, crashก่อนcommitไม่ถือว่าจบ; Mail CLAIMING ที่ไม่แน่ชัดยังใช้REVIEWตาม [คู่มือ](README-th.md)
room advanceกับfinal completeเป็นคนละtransaction: ถ้าดับตรงช่วงนี้ recoveryยกเลิกรอบ ไม่เดาว่าควรให้รางวัล
ไม่อ้างว่ามี atomicityระหว่างการต่อสู้/world/playerdataกับSQLite; รับประกันที่ทดสอบคือtransactionของreceipt+mailในDB

อัปจากv0.6ต้องbackupก่อน schema5→6อัตโนมัติ; การย้อนJARv0.6ไม่อ่านschema6 ต้องกู้backupชุดเดียวกัน
ปิดโหมดโดยenabledfalseและrestart; protection/return recoveryยังทำงานกับโลกowned, เงิน/Coreบริการอื่นยังใช้ได้
จะเอาJARCoreออกต้องพาผู้เล่นออกและเคลียร์รอบก่อน เพราะจะไม่มีlistenerprotect/returnของCore

## 6. NPC/ตกแต่งและงานถัดไป

วางผู้คุมประตู Moonfall ในโซน09ประตูผจญภัยเมืองตาม [ใบงานเมือง](../output/lobby-concept/ZONE-BUILD-TICKETS-th.md)
ใช้ NPC/anchorเฉพาะซุ้มดัน ไม่เปลี่ยน `portal_keeper` ที่คุมtravelรวมทั้งลาน; หัน NPCเข้าหาจุดที่ผู้เล่นยืน
สูตรyaw=`atan2(-(targetX-npcX),targetZ-npcZ)*180/pi`: South0,West90,North180,East−90,pitch0
มองจากตำแหน่งNPCไปยังหน้าเคาน์เตอร์ก่อน `/fa npc spawn dungeon.main ผู้คุมวิหารจันทรา` หรือ bindCitizensที่เลือกไว้
ตัวอย่างภายในโลกtemplateในภาพยังเป็นref; อย่าวางserviceVillagerในโลกtrainingที่ห้ามcreatureอื่นเกิด
แอนิเมชันboss: ใช้ภาพ04ทำrefturnaround → BlockbenchMCPที่เชื่อมจริง → `.bbmodel`/texture/animations/export → testhitbox/telegraphทุกclient
ยังไม่ได้สร้างหรือสั่งMCPทำบอสในรุ่นนี้ และไม่ได้แสดงโมเดลHuskเป็นผลงานcustom model

ก่อนขยายเป็นปาร์ตี้ต้องผ่านโหมดฝึกจริง แล้วทำinstanceแยก/สมาชิกตรึงตอนเริ่ม/reconnectgrace/checkpoints/lootrecipeที่ตรวจจริง
ไม่เพิ่มcapacityโดยให้สองปาร์ตี้ใช้โลกฝึกเดียวกัน; ตั้งbudgetจำนวนinstanceและใช้templateที่ตรวจhashแล้ว

## 7. หลักฐานตรวจและเอกสาร API

Unit testsใหม่13กรณี: store8 (ownership/order, concurrent run/complete, frozenmail, daily quota, rollback, restart/reopen, v5migration)
และmap/rules5 (footprint/Y, จุดเกิดและbossfloor, บันได4ชุด/headroom, gateไม่ลบfloor, fail-closed config)
ยังไม่มีBukkitserverในtests จึงต้องตรวจspawn/AI/armor/telegraph/WorldGuard/Via/cleanup/teleportและMSPTจริงในchecklistR
ใช้[Paper scheduler](https://docs.papermc.io/paper/dev/scheduler/), [ChunkGenerator26.2](https://jd.papermc.io/paper/26.2/org/bukkit/generator/ChunkGenerator.html)
และ[GameRules26.2](https://jd.papermc.io/paper/26.2/org/bukkit/GameRules.html)เป็นAPIหลัก; ไม่ใช้NMSหรือpacketlibraryเพิ่ม
