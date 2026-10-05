# ItemsCore trial content ของลูม่า

มี 4 ไฟล์ `.import` เป็น clean item JSON ที่เปิดแก้ใน GUI ของ ItemsCore ได้
ผ่าน validator ทางการ `itemscore-helper@1.31.0`/bundled API 5.0 แล้ว: **4 valid, 0 errors, 0 warnings**
ยังไม่ได้ import หรือเล่นบน Minecraft; ไม่มี ItemsCore JAR ใน workspace และไม่ได้ซื้อรุ่นเต็ม

อ่าน [แผนผสม ItemsCore + FantasyCore](../../../fantasycore/ITEMSCORE-INTEGRATION-th.md) ก่อนทดลอง
Metadata/แหล่ง validator อยู่ใน [manifest.json](manifest.json)

| ไฟล์ | หน้าที่ทดลอง | ต้องมีอะไร |
|---|---|---|
| [luma_moon_wand.import](imports/luma_moon_wand.import) | ประกาย teal 5 ticks + เสียงเบา, คูลดาวน์8วิ | ItemsCore |
| [luma_healers_charm.import](imports/luma_healers_charm.import) | ฮีลตนเอง4 HP = 2หัวใจ, คูลดาวน์20วิ | ItemsCore |
| [luma_wayfinder.import](imports/luma_wayfinder.import) | เปิด `/menu` ด้วยสิทธิ์ผู้ถือ; ไม่รัน console | ItemsCore + FantasyCore |
| [luma_sentinel_coat.import](imports/luma_sentinel_coat.import) | เกราะหนังสี #47C8D3, type armor | ItemsCore |

ทดลองสามชิ้นแรกที่ไม่ต้องเรียก Core ได้บน [demo ทางการ](https://www.coredevelopment.shop/plugins/itemscore/demo)
เข็มทิศต้องทดสอบบน staging ของเราที่มีทั้งสองปลั๊กอิน; vendor demo อาจไม่มี FantasyCore จึงเปิด `/menu` นี้ไม่ได้
ชุดนี้ไม่มี texture/custom model/stat RPG/native recipe/loot อัตโนมัติ และยังไม่เชื่อม Core registry/repair
`luma_healers_charm` เป็น normal item ที่ถือใช้ ไม่ใช่ passive talisman แม้ชื่อไทยใช้คำว่าเครื่องราง

## ทดลอง import บน demo หรือ staging

1. สำรอง/ใช้เซิร์ฟทดลอง และเช็ก ItemsCore รุ่น/API ที่เปิดจริง; staging ของเราต้องมี JAR ที่ได้จากผู้พัฒนาและผ่านขั้นตอน EULA เดิมก่อนเริ่ม
2. คัดลอก `.import` ที่เลือกลง `plugins/ItemsCore/imports/` หรือใช้ file manager ของ demo อัปโหลด
3. ผู้เล่นทดสอบต้องได้ permission `luma.itemscore.trial`; ยังไม่แจก permission นี้ให้ผู้เล่นทั่วไป
4. ใช้ `/ic import luma_moon_wand` แล้ว `/itemeditor luma_moon_wand` ต้องเห็น visual method tiles
5. ใช้ `/ic give <ผู้เล่นทดสอบ> luma_moon_wand 1`; ทดสอบคลิกขวาในอากาศ/คูลดาวน์/เอฟเฟกต์และ non-OP ที่ไม่มีสิทธิ์
6. ทำแบบเดียวกันสำหรับ healer/coat และเข็มทิศเมื่อมี Core; อ่าน checklist N ใน [staging](../../README-th.md)
7. Export กลับด้วย `/ic export <name>` เพื่อตรวจ round trip; บันทึกรุ่น JAR/API, client, screenshot/log และ MSPT ก่อนพิจารณารุ่นเต็ม

```text
# บน staging ที่ติดตั้ง LuckPerms แล้ว ให้เฉพาะบัญชีทดสอบ
lp user <ชื่อผู้ทดสอบ> permission set luma.itemscore.trial true

# เมื่อจบทดลอง
lp user <ชื่อผู้ทดสอบ> permission unset luma.itemscore.trial
```

อย่าใช้ `/ic give` เป็น fulfillment ของร้านเว็บหรือผลคราฟต์ Core ในรุ่นนี้
คำสั่ง give ของ provider มี workflow ส่งของของตัวเองซึ่งยังไม่ได้ผูกกับ ledger/mail ของ Core

## ตรวจไฟล์ซ้ำในเครื่อง

```powershell
node tools/verify_itemscore_trials.cjs .tools/itemscore-helper-1.31.0
# เมื่อติดตั้งปลั๊กอินจริงแล้ว export API ใหม่ก่อน และใช้ไฟล์ของ server เป็น argument ที่สาม
node tools/verify_itemscore_trials.cjs .tools/itemscore-helper-1.31.0 server/runtime/plugins/ItemsCore/itemscore-api.json
```

เครื่องนี้ดาวน์โหลด helper ลง `.tools` และตรวจ published SHA512 แล้ว; ไม่ได้เชื่อม MCP server ของ helper เข้าแอป
คำสั่งนี้เรียก library validator ทางการโดยตรง ไม่โหลด Minecraft หรือ runtime plugin
ถ้า clone repo ใหม่ ให้ดาวน์โหลดแพ็กเกจ pinned จาก npm ทางการและตรวจ integrity ใน manifest ก่อนแตกลง `.tools`
ต่อ MCP ภายหลังตาม [README ทางการ](https://github.com/Core-Pluginss/ItemsCore-Helper) และตรวจ `health_check` จริงก่อนแจ้งว่าเชื่อมแล้ว
