# FantasyAdminPanel — ใช้ง่าย ควบคุมระบบ Luma และตรวจย้อนหลังได้

สเปกเต็มของ panel: FantasyCore v0.10 มี `/fa` GUI dashboard/ผู้เล่น/ฟอร์มปรับเงิน/อ่านtemplates–NPCแล้ว ดู [implementation และขอบเขตจริง](../../fantasycore/ADMIN-PANEL-th.md)
source/JAR compileและ141testsผ่าน; ยังไม่install/playtestMinecraft ส่วน editor/recoveryGUI/externaladapterยังเป็นขั้นถัดไป เป้าหมายคือ staffใช้งานประจำได้จาก `/fa` ไม่ต้องจำคำสั่งหลายปลั๊กอิน
PanelควบคุมทุกโมดูลของFantasyCoreและexternaladapterที่ลงทะเบียนไว้ ฟีเจอร์ปลั๊กอินที่ไม่มีadapterต้องแสดงว่าไม่รองรับ ไม่อ้างว่าคุมทุกJARได้เอง

## หน้าตาและการใช้งาน

ใช้ inventory GUI54slots ที่1.16.5แสดงได้ ชื่อไทยสั้น ไอคอนคุ้นเคย พื้นneutral/ขอบjade-gold; optionalcustomfontภาพโลโก้ แต่packปฏิเสธแล้วยังอ่านรู้เรื่อง
ไม่พึ่งmoderndialogเป็นทางหลัก มีbreadcrumb, back, search, page, actionsummary และหน้าช่วยเหลือสั้น
slotนับ0–53:

| แถว | Layout |
|---|---|
| 0 (0–8) | back / breadcrumb / heading / help / close |
| 1 (9–17) | profile / economy / item / NPC / world / quest / reward / market / delivery |
| 2 (18–26) | guild / professions / pets / travel / UI / event / website / pack / adapters |
| 3 (27–35) | แสดงหมวดที่เลือก ไม่ใส่คำสั่งดิบของทุกระบบรวมกัน |
| 4 (36–44) | search / filters / selectedtarget / preview summary |
| 5 (45–53) | previous / page / audit / doctor / applyหรือปิด |

ในหน้ารายการให้ใช้ contentarea7×4=28rowslotsกับpagination ไม่ยัดiconทุกหมวดซ้ำจนเหลือitem3ช่อง
สีสถานะมีคำกำกับ “พร้อม / ปิด / ไม่ได้เชื่อม / ต้องแก้ไข” ไม่ใช้กระจกแดงอย่างเดียว
คลิกซ้ายดู/เลือก เปลี่ยนค่าผ่านformย่อย ไม่ใช้rightclickแอบลบข้อมูล
Searchใช้chatinputใน1.16.5 มีcancelและtimeout; คำตอบchatไม่หลุดpublicchat

## งานที่ทำได้

| หมวด | งานหลัก | ตรวจ/ป้องกัน |
|---|---|---|
| ผู้เล่น | หาUUID/ชื่อ, mute/banผ่านmoderationadapter, profile, warp, inventoryinspect | accountUUIDจริง; readonlyinspectปกติ; แก้inventoryต้องjournal |
| เงิน/ธนาคาร | wallet/bank/red, adjustment, fees, ledger | delta/เหตุผล/ผู้กระทำ/idempotency; ห้ามแก้balanceSQLตรง |
| Items | registry, preview, give/revokeโดยserial, balance | templateversion/soulbound/stat/gemsไม่หาย |
| NPC | เลือกstation, snaplocation, facinganchor, previewanimation, toggle, servicebinding | bodyyawกับmodeloffset, collision, persistentID, ไม่spawnซ้ำ |
| แมพ | zones/regions, POI, warp landing, lightingaudit, blockedpath | ไม่วางWorldEditบนliveโดยไม่มีstaging/snapshot |
| บ้าน/Protect | tier/ขอบเขต/member/home/unclaim preview | bounds จริง, owner, revision, reserved funds และ snapshot |
| Canvas | draft/submitted/approve/reject/print receipts | artwork owner/version/hash, moderation แยกจาก permission เงิน |
| สกิน/Exchange/RTP | unlock/recipe/profile/cooldown | typed adapter, serial/receipt, landing validator ไม่มี arbitrary command |
| เควส | objectives/rewards/cooldown, testplayerpreview | rewardtransactionไม่ซ้ำ; adaptervalidation |
| รางวัล | daily/online/AFK/boss/crate | เวลาBangkok, earnedvsclaimed, recipientcorrect |
| ตลาด | offers/orders/escrow/refunds/auctionเมื่อเปิด | refundcompensatingtransaction, partialfillcorrect |
| Guild/อาชีพ/pets | settings/roster/jobs/cosmeticownership | APIadapterและpermissionของCore |
| UI | TAB/scoreboard/MOTD/topHUD preset, glyphpreview, reducedmotion | conflictcheckrenderer, generateddefaultvalidation |
| เว็บ/ร้าน | POIpublish, draftnews, orderstatus/deliveryretry | retryorderIDเดิมไม่grantซ้ำ; เฉพาะpaidorder |
| Pack | currenthash/clientprofiles/failedload/mappingerrors | validateก่อนactivate, rollbackprevioushash |
| Ops | servicehealth, backupstatus, queue, logs, audit, emergencyfreeze | freezeเฉพาะmodule, ไม่killทั้งserverอัตโนมัติ |

“ทำทุกอย่าง” ในขอบเขตนี้หมายถึงทุกworkflowของเซิร์ฟที่สร้างไว้ ไม่เปิดshell/consolecommandอิสระจากเว็บหรือให้OPเพื่อใช้panel

## คำสั่งสั้นและชื่อที่ชัด

```text
/fa                         เปิดหน้าแรก
/fa player <name>            เปิดผู้เล่น
/fa bank <name>              เปิดเงิน/ธนาคาร
/fa item <id>                ดูแม่แบบไอเทม
/fa npc                      จัด station และทิศหน้า
/fa zone                     จัดโซนและจุดบนเว็บแมพ
/fa shop                     ร้าน/รายการซื้อ/ส่งมอบ
/fa ui                       แก้ TAB/scoreboard/MOTD
/fa doctor                   ตรวจปัญหาและคำแนะนำ
/fa audit [target]           ประวัติ
/fa help                     วิธีใช้สั้น
```

TabcompleteเฉพาะIDที่ผู้ใช้มีสิทธิ์ ไม่แสดงUUID/ยอดเงินของผู้เล่นอื่นให้staffที่ไม่ควรดู
Playercommands `/menu /bank /repair /mail /link /hud` มีaliasชัด; registryเช็คcommandconflictและใช้namespace `fantasy:repair`เมื่อชน

## การแก้ไขเป็น preview → apply

1. ดูค่าเดิมและrevision ผู้ใช้เลือกtargetและแก้ค่าทีละงาน
2. Previewบอกผลจริง: “เพิ่ม gold100 ให้ UUID... เหตุผล...” หรือ “ย้ายbankerไปzone05 ... yaw...”
3. งานเสี่ยงต่อเงิน/ลบ/ย้ายmapแสดงconfirmของpanelพร้อมtarget ไม่ใช้คำว่าแน่ใจหรือไม่แบบไม่บอกผล
4. Serverตรวจpermission/targetrevision/ราคาปัจจุบันใหม่ก่อนcommit; ถ้าเปลี่ยนระหว่างpreview ให้previewใหม่
5. ApplyมีoperationUUID, auditbefore/after/result/actor/server/time/reason; สถานะค้างrecoverจากjournal
6. Undoใช้transactionชดเชยตามpolicyและversioncheck ไม่ย้อนทุกSQLหรือคืนitemที่ถูกใช้ไปแล้วแบบปลอม

Previewfieldไม่เป็นpermissiongateที่ต้องถามhumanทุกงานเอกสาร แต่เป็นUXของproductionadminสำหรับป้องกันคลิกผิด

## สิทธิ์

| Role | Scope |
|---|---|
| observer | health/ownpermittedaudit readonly |
| moderator | playerhelp/moderation ไม่มีปรับเงิน/ร้านจริง |
| builder | zone/NPCvisual/POI staging ไม่มีwallet |
| content_editor | draftitems/quests/UI ไม่publishpaymentproductเอง |
| economy_admin | walletadjustment/escrow/refunds พร้อมเหตุผล |
| owner | publishconfig/pack/paymentcredentials/permissions |

ใช้ LuckPerms nodes `fantasyadmin.view`, `.player`, `.economy.adjust`, `.npc.edit`, `.content.edit`, `.content.publish`, `.web.orders`, `.ui.edit`, `.audit`, `.ops.freeze` แยกกัน ไม่ใช้ `isOp()`อย่างเดียว
Webadminใช้RBAC + MFA/recentreauthงานรับเงิน ความสามารถตรงกับingamepanel แต่ไม่มีRCONpublic
ชุดระบบใหม่ใช้ `.claims.inspect/.adjust/.unclaim`, `.rewards.edit`, `.skins.grant`, `.exchange.edit`, `.canvas.moderate`, `.travel.edit`
อ่าน [flow และเกณฑ์ตรวจ](CASUAL-SURVIVAL-SYSTEMS-th.md) และ [Protect 5 ระดับ](PROTECTIONSTONES-TIERS-th.md); การมี node ในสเปกไม่ใช่ผลติดตั้งแล้ว

## Inventory implementation ที่ต้องเนี๊ยบ

- ใช้ custom InventoryHolder/menu session IDระบุเมนูจริง ไม่เทียบชื่อinventoryที่ผู้เล่นปลอมได้
- ตรวจtopinventory/rawslot/shiftclick/numberkey/doubleclick/drag/offhand/creativeactions; decorativeitemไม่ถูกลากออก
- confirmationnonceผูกactor/operation/expiry/revision ห้ามreuseหลังapply
- SQL/HTTP async; Bukkitinventory/entity APIบนserverthread; pendingbuttondisabled ไม่ล็อกmainthreadรอweb
- close/quit/death/worldchangeยกเลิกsessionที่ยังไม่commit; committedoperationมีreceiptไม่หาย
- reloadconfigvalidateลงtemporarysnapshotแล้วswapatomically; ไม่มี `/reload` กลางtransaction
- adapter capabilitiesversioncheck; ถ้าไม่ตรงปิดwriteพร้อมข้อความและคงreadได้เมื่อปลอดภัย

## Acceptance ก่อนเปิดให้ทีมงาน

งานประจำเปิดได้ภายใน2–3คลิก, breadcrumbbackไม่หลง, keyboard/tabcompletion, Thaiชื่อยาว, packaccepted/rejected
ทดสอบprivilege escalation/forgedmenus/shiftclickdupe/concurrentadjustment/retry/crashmidoperation
Auditค้นเจอทุกwrite; doctorบอกserviceที่เสียและทำอะไรได้ต่อ; ไม่มีปุ่มsuccessที่ยังไม่commitจริง
ยังไม่มีผลทดสอบในเกม; implementationบางส่วนv0.10 buildผ่านแล้ว ส่วนที่เกินขอบเขตในคู่มือปัจจุบันยังเป็นspec ต้องผ่านchecklist Uและเพิ่มทีละworkflowก่อนอ้างว่าคุมครบทุกระบบ
