# AdminPanel — FantasyCore v0.10

ฉบับ 6 ตุลาคม 2026: `/fa` เปิด inventory GUI 54 ช่องสำหรับทีมงานแล้ว
**source/JAR build ผ่าน; Core tests 141 กรณีผ่าน แต่ยังไม่มีผลทดสอบ Minecraft จริง**
Paper 26.2 build129 / Java25 / schema7 เดิม; client เป้าหมาย Java1.16.5+ ยังต้องผ่าน [checklist U](../server/README-th.md#u-adminpanel-v010--ยังรอ-minecraft-จริง)

## 1. สิ่งที่ทำได้ในรอบนี้

| หน้า | การใช้งานจริงใน source | ขอบเขต |
|---|---|---|
| ภาพรวม | จำนวน REVIEW ของ mail/exchange/craft/repair, ปุ่ม doctor/audit/สถานะ Moonfall, สถานะร้านยา | รายงานรายละเอียดเดิมเปิดเป็นข้อความ; craft รวมรายการยา |
| ผู้เล่น | รายชื่อออนไลน์เรียงชื่อ/แบ่งหน้า, ค้นหาชื่อหรือ UUID ของคนที่เคยเข้าเซิร์ฟ | คนออฟไลน์ค้นหาได้; ไม่สร้างบัญชีใหม่จากชื่อที่หาไม่เจอ |
| โปรไฟล์ | UUID, ทองพก/ทองฝาก/เงินแดง, อ่านใหม่, เปิด bank ledger/audit | อ่านยอดจาก DB async; ใช้ UUID ตรึงเป้าหมายหลังเลือก |
| ปรับเงิน | เลือก gold.wallet/gold.bank/red.wallet, เพิ่ม/ลด, จำนวน, เหตุผล, preview และปุ่มยืนยัน | เป็นงานเขียนผ่าน GUI ที่ทำครบในรอบนี้ ใช้ ledger/audit/nonce เดิม |
| ไอเทม | ID/version/material/enchant/ผล native potion, แบ่งหน้า | อ่าน current template; ไม่แจก/แก้/revoke จากไอคอน |
| NPC | action/kind/world/X/Y/Z/yaw/UUID สถานี/มีบริการหรือยัง planned | อ่านทะเบียน; ไม่มีปุ่ม spawn/delete/move/bind ใน GUI รอบนี้ |

คำสั่งเดิมสำหรับ recovery/NPC/items/dungeon ยังทำงาน; ปุ่มรายงานไม่ตัดสินรายการหรือสร้างแมพให้เอง
สเปกเต็ม [ADMIN-PANEL-SPEC](../output/lobby-concept/ADMIN-PANEL-SPEC-th.md) ยังมีหมวดขยาย: editor/claims/canvas/skins/market/web orders/pack และ external adapters
หน้าใหม่นี้ไม่เรียกคำสั่ง console อิสระ ไม่รับข้อความเป็น command และไม่ควบคุม JAR ที่ไม่มี adapter

## 2. เส้นทางใช้งานและตำแหน่งปุ่ม

```text
/fa                         เปิดภาพรวมในเกม; console แสดง help
/fa panel                   เปิดภาพรวม
/fa player                  เปิดรายชื่อออนไลน์
/fa player <ชื่อหรือUUID>    ค้นหาและเปิดโปรไฟล์
/fa help                    คำสั่งข้อความเดิม
/fa bank <ชื่อหรือUUID>      ดูยอด/ประวัติข้อความ
```

ไอคอนใช้ vanilla ที่มีใน1.16.5 โทนชื่อทอง/ฟ้า พื้นกระจกเทา ไม่ต้องใช้ font/pack ใหม่
slot นับ0–53 ตาม Bukkit:

| หน้า | Layout |
|---|---|
| ทุกหน้า | หัวข้อ4, ย้อนกลับ45, ปิด53 |
| ภาพรวม | ผู้เล่น10, เงิน12, ไอเทม14, NPC16, mail28/exchange29/craft30/repair31, dungeon32, ร้านยา34, doctor48/refresh49/audit50 |
| รายการ | content28ช่องในแถว1–4; หน้าก่อน47/เลขหน้า49/ถัดไป51; ค้นหาผู้เล่น48 |
| โปรไฟล์ | ผู้เล่น/ยอด13, ปรับเงิน28, history30, audit32, refresh48 |
| ปรับเงิน | ผู้เล่น/ยอด13; wallet20/bank21/red22; เพิ่ม–ลด24; จำนวน29/เหตุผล33; preview49 |
| ยืนยัน | รายการ22 มีชื่อ+UUID+ช่องเงิน+delta+ยอดก่อน/หลัง+เหตุผล; ยืนยัน49 หรือย้อน45 |

คลิกซ้ายหรือขวาให้ผลเดียวกัน ไม่มี right-click ลบข้อมูล
ย้อนจากโปรไฟล์กลับรายการผู้เล่น; ย้อนจากปรับเงินกลับโปรไฟล์; ย้อนจากยืนยันกลับฟอร์มและยกเลิก nonce เก่า
รายการว่างบอกว่ายังไม่มี ไม่แสดงผลสำเร็จปลอม; อ่าน DB ไม่ได้แสดง error แทน REVIEW0
เงินที่โชว์เป็น snapshot ณ ตอนโหลด กดอ่านข้อมูลใหม่ได้ และ preview ดึงยอดจริงอีกครั้ง

## 3. ปรับเงินทีละขั้น

1. `/fa` → เลือกผู้เล่นออนไลน์ หรือค้นหาชื่อ/UUID → ตรวจชื่อและ **UUID** ในโปรไฟล์
2. กดปรับเงิน เลือกช่องเงิน และโหมดเพิ่ม/ลด; แต่ละงานปรับเพียงช่องเดียว ไม่ย้ายเงินระหว่างช่อง
3. กดจำนวน → inventory ปิดและเปิดฟอร์มแชตเฉพาะทีมงานคนนั้น → พิมพ์จำนวนเต็มบวก เช่น `1,000`
4. กดเหตุผล → พิมพ์ข้อความ3–200ตัวอักษร → กลับฟอร์ม; ช่องที่กรอกผิดล้างค่านั้นเพื่อให้กรอกใหม่
5. กดดูยอดก่อน–หลัง → backend อ่านยอดและตรวจสิทธิ์/จำนวน/เพดาน/ยอดติดลบ/overflow → สร้าง nonce60วินาที
6. อ่านหน้า preview ให้ตรง UUID/ช่อง/เครื่องหมาย/ยอดและเหตุผล แล้วกดยืนยันครั้งเดียว
7. GUI ปิดหลังส่งการยืนยัน; ข้อความ “กำลังบันทึก” ยังไม่ถือว่าสำเร็จ ต้องรอผล DB พร้อม operation ID

preview ไม่เปลี่ยนยอด; งานจริงใช้ `EconomyService.adminAdjust` / `EconomyStore.adjust` เดิม
ยอดของช่องนั้นต้องตรง `expectedBefore` ใน transaction ที่ commit มิฉะนั้นปฏิเสธและให้ preview ใหม่
ledger + balance + audit commit/rollback ชุดเดียวกัน operation ID เดิมไม่ทำซ้ำ; ไม่แก้ SQL balance โดยตรงจาก GUI
การเปลี่ยนช่องเงินอื่นระหว่างรอไม่ใช่การเปลี่ยน expected balance ของช่องที่เลือก
จำนวนไม่เกิน `economy.max-transaction` ตาม config; ไม่รับ0/ลบ/ทศนิยมหรือผลที่ล้น long
การแก้ด้วยคำสั่ง `/fa eco` ใช้ validator/preview/nonce/permission gate เดียวกับ GUI แล้ว

## 4. สิทธิ์และการปิดงาน

| Node | หน้าที่ |
|---|---|
| `fantasyadmin.view` | เปิด panel/อ่านโปรไฟล์และ bank history/current templates/ทะเบียนจุดบริการ/รายงาน REVIEW |
| `fantasyadmin.economy.adjust` **พร้อม view** | ฟอร์มและยืนยันปรับเงินทั้งสามช่อง |
| `fantasyadmin.audit` **พร้อม view** | ปุ่ม/คำสั่ง audit |
| `.npc.edit/.content.edit/.exchange.resolve/.craft.resolve/.repair.resolve/.dungeon.manage` | ใช้คำสั่งงานเขียนเดิมตามแต่ละระบบ; ไม่ได้เปิด write GUI ด้วยปุ่มอ่านข้อมูล |

ตั้ง role ใน LuckPerms โดย node ที่ต้องใช้ ไม่จำเป็นต้อง OP เพื่อเปิด panel
การมี view หมายถึงได้รับอนุญาตให้อ่านยอดผู้เล่นและรายงานตามตารางนี้; อย่าให้กลุ่มผู้เล่นทั่วไป
ปุ่มที่ไม่มีสิทธิ์มีข้อความกำกับและตรวจอีกครั้งตอนคลิก ไม่อาศัยการซ่อนปุ่มเพียงอย่างเดียว
ตรวจ view/adjust ตอนเริ่ม preview หลัง async DB ตอบ ตอน nonce ถูกใช้ และก่อนส่งงานปรับเงินเข้า DB
รหัสผูก actor UUID ใช้ได้ครั้งเดียว; คนอื่นใช้/ยกเลิกรหัสของเจ้าของไม่ได้ รหัสหมดอายุหรือ permission check ปฏิเสธไม่ apply

ปิด/ย้อนหน้า confirmation, quit, death, world change หรือ disable ยกเลิก GUI nonce ที่ยังไม่ใช้
ปิดเมนูหลังยืนยันไปแล้วไม่ย้อนธุรกรรมที่ commit; ให้ตรวจ audit/ledger ก่อนทำรายการชดเชยพร้อมเหตุผลใหม่
เมนูที่ปิดไปแล้วไม่เปิดกลับจาก callbackเก่าหรือแสดงข้อมูลผิดหน้า; preview ที่ตอบหลังปิดถูกทิ้งพร้อมยกเลิก nonce
หาก DB error ปุ่ม preview กลับมาใช้ได้ ไม่ค้าง busy จนต้อง restart
Tab completion ซ่อนคำสั่ง/งานเขียนที่ไม่มี node และไม่ส่งรายชื่อแนะนำให้คนไม่มี view

## 5. ฟอร์มแชตและ client เก่า

ใช้ `AsyncChatEvent.originalMessage` รับ plain text ยกเลิก event และล้าง viewers ของฟอร์ม
callback/เช็กโลก/สถานะ/สิทธิ์/เปิด inventory กลับบน server thread ตาม [Paper chat events](https://docs.papermc.io/paper/dev/chat-events/)
API [AbstractChatEvent](https://jd.papermc.io/paper/26.2/io/papermc/paper/event/player/AbstractChatEvent.html) ให้ mutable viewers และแจ้งว่า cancelled event ยังผ่านไปถึงปลั๊กอินอื่น
จึงต้องทดสอบ chat/Discord bridge/logger ที่ติดตั้งจริงว่าเคารพ cancellation; ไม่ถือว่ารายละเอียดฟอร์มเป็นความลับจากปลั๊กอินที่อ่าน event เอง

- คำตอบหนึ่งครั้งต่อ session; แชตรัวระหว่างรอ callback ถูกกั้นจากแชตปกติด้วย
- `cancel`, `ยกเลิก`, `/cancel` ยกเลิก; คำสั่งอื่นยกเลิกฟอร์มก่อนให้ command นั้นทำงานตามสิทธิ์ของมัน
- หมดเวลา60วินาที ย้ายโลก ตาย quit เปิด inventory อื่น หรือเสียสิทธิ์ก่อนตอบทำให้ฟอร์มใช้ต่อไม่ได้
- จำกัดคำตอบ1–200ตัวอักษร ไม่รับ control/newline; เหตุผลต้อง3–200; ไม่ตีความข้อความเหตุผลเป็น MiniMessage หรือคำสั่ง
- sessionเก่าหรือ timerเก่าไม่ลบ sessionใหม่ของผู้เล่นเดียวกัน; ข้อมูลที่กรอกแล้วยังต้องผ่าน preview ก่อนเงินเปลี่ยน

## 6. อัปเกรดและ rollback

สำรอง DB/playerdata/โลก/config/JAR ชุดเดียวกัน และทดสอบสำเนาเซิร์ฟก่อน
เปลี่ยนเป็น `FantasyCore-0.10.0.jar` และ restart (ไม่ใช้ `/reload`); schema7 คงเดิม ไม่มี migration ใหม่
Core ไม่เขียนทับ config เดิม; key ข้อความที่ไม่มีใช้ defaults ใน JAR ผ่าน Messages loader
หากปรับข้อความเองให้ merge `admin.panel`, `admin.eco.limit`, `admin.reason-required`, `admin.help` จาก [defaults](src/main/resources/messages_th.yml) โดยไม่สร้าง keyซ้ำ
ฟังก์ชันร้านยา/gear/ดันเดิมคงอยู่; [Alchemy](ALCHEMY-th.md) บันทึกรุ่นที่เพิ่มบริการคือv0.9 และยังรอ QA

nonce/chat session อยู่ใน memory ไม่ใช่รายการเงินที่ commit: restart ทิ้งงานยังไม่ยืนยัน; ยอดที่ commit/ledger/audit อยู่ DB
ก่อนถอยv0.9หยุดเซิร์ฟและตรวจ ledger/operationที่เกิดระหว่างอัป; config/database schema-compatible แต่ v0.9 ไม่มี panel และไม่มี permission-at-confirm guardใหม่ของ economy
หาก restore snapshot ให้ใช้ชุดเดียวกันและกระทบยอดงานหลัง snapshot ตามนโยบายเดิม ไม่ถอย DB เพียงไฟล์เดียว

## 7. หลักฐานที่มี

Java25/Paper API26.2.build.129-stable buildผ่าน; Core tests **141 passed / 0 failed, error, skipped**
10 testsใหม่ครอบคลุม actor/replay/revocation/cancel/exact expiry/concurrentconfirm, concurrentprompt/input bounds, node/suggestion policy, offline UUID/name reuse และ recipient/reason/ยอด previewจริงที่ renderเป็น plain text
testsเดิมของ economy และ mail/craft/alchemy/ดันยังผ่านร่วมกัน
ยังไม่มี native inventory/client/chat bridge/GUI screenshot/playtest/MSPT จากเซิร์ฟจริง; รายการ U ต้องตรวจบน staging ก่อนให้ทีมงานใช้งาน live

ขั้นถัดไปของ panelคือ recovery GUI ที่เปิดหลักฐาน receiptและรับเหตุผลก่อนใช้ backend resolveเดิม จากนั้นจึงเพิ่ม write editor/NPC/claims/provider ทีละโมดูลตามสเปก
