# Luma — เว็บ ข่าว แมพสด ร้านค้า และการส่งของเข้าเกมอัตโนมัติ

วันที่5ตุลาคม2026 ผู้ใช้เพิ่มเว็บและอนุญาตปรับสีเป็นสีประจำเซิร์ฟ; แบรนด์เสนอ **Luma / ลูม่า** casual fantasy roleplay
รูปลักษณ์ล่าสุดปรับเป็น **Minecraft modern community portal** ตามคำขอใหม่: ภาพเมืองเต็มสี โลโก้ voxel เขียวหยก–ทอง ธีม dark forest และแผงข้อมูลที่อ่านง่าย
หน้าแรกเปิดให้เข้าถึงแผนที่ คู่มือ กิจกรรม และร้านค้าได้ทันที พร้อมสถานะบริการที่แสดงตามข้อมูลจริง
ชื่อ/โดเมนยังไม่ได้ตรวจความเป็นเอกลักษณ์หรือจดทะเบียน ไม่ใช้IP/เว็บสมมติเป็นปลายทางรับเงินจริง

## สิ่งที่ทำแล้วกับสิ่งที่ต้องเชื่อมจริง

ต้นแบบใน `website/dist/` มี home/news/map/shop/account/wiki/events/rankings/status/privacy/terms
มี responsive, dark/light, ลดการเคลื่อนไหว, ข่าวเปิดอ่านได้, ตัวกรองข่าว/คู่มือ, ค้นหาโซน 12 แห่ง, หมุดเลือกสถานที่, pan/zoom/copy พิกัด และ product details
ค้นหาทั้งเว็บด้วยปุ่มค้นหาหรือ Ctrl+K แล้วเปิดสถานที่ คู่มือหรือสินค้าได้ ศูนย์บัญชีมีแท็บเชื่อมเกม/รายการซื้อ/กล่องจดหมาย/ตั้งค่า
อันดับและกิจกรรมแสดงสถานะรอข้อมูลและแผนงาน ไม่สร้างคะแนน ผู้เล่น วันที่ หรือจำนวนออนไลน์ปลอม
ภาพเมืองและmarkerเป็นconceptpreview **ยังไม่ใช่BlueMapสด** ราคา/ข่าวเป็นตัวอย่างที่ติดป้ายไว้
ไม่มีpayment/auth/backend/bridgeในstaticprototype ปุ่มชำระเงินปิด ไม่สร้างQRปลอมและไม่แสดงdeliveryสำเร็จโดยไม่มีเกม

Productionต้องมีserverendpoint/DB/secrets/paymentmerchantและFantasyCorebridgeตามแผนนี้ ยังไม่มีMinecraftserverruntimeในworkspaceที่ตรวจได้
ไม่บังคับให้userส่งAPIkeyในแชต ให้ตั้งserversecretหรือproviderdashboardเมื่อทำdeploymentจริง

## Pages และflow

| Page | ผู้เล่นทำอะไรได้ | Data owner |
|---|---|---|
| Home | ข่าวล่าสุด เซิร์ฟstatusจริง วิธีเข้าplayIP/versionsที่ผ่านtest | publishedposts/healthsnapshot |
| News | update/changelog/event หมวด/วันที่/สถานะเปิดบริการ | webCMSdraft→publish |
| Map | BlueMap3D เปลี่ยนworld ค้นหาservice/zone/POI ไปยังตำแหน่ง | Corezoneregistry→BlueMap/API |
| Shop | ดูสินค้า previewmodelราคา/สิทธิ์ เลือกซื้อหรือเติมcurrencyที่เปิดไว้ | servercatalogversioned |
| Checkout | ยืนยันlinkedUUIDและสินค้า ชำระproviderpage | ordersDB/provider |
| Account | linkUUID/myorders/paymentstate/deliverystate/mailbox | authenticatedsession→Corebridge |
| Wiki | ค้นหาและกรองคู่มือ เชื่อมคู่มือไปยังโซนบริการ | published handbook + command registry |
| Events | ปฏิทิน เวลาเริ่ม สิทธิ์เข้าร่วม ลงทะเบียนเมื่อเปิด | event registry / Core event state |
| Rankings | ฤดูกาล หมวด อันดับจริง และเวลา snapshot | Core sanitized seasonal aggregates |
| Status | uptime/service state ตาม health snapshot | public sanitized health service |
| Admin | newsdraft/catalog/orders/retry/POIpublish/servicehealth | RBAC+FantasyAdminAPI |

ไม่แสดงstatusonline/fakeplayercountจนรับsnapshotจริง; oldsnapshotขึ้น“ข้อมูลล่าสุดเมื่อ...”และ“ยังไม่เชื่อม” ไม่แสดง0เป็นข้อมูลจริง
เมนูMinecraftดูมีความเป็นเกมจากlogo/images/icons/เขียวทอง ส่วนฟอร์มราคาและorderstatusอ่านง่ายไม่ตกแต่งจนบังข้อมูล

## Skillที่ผู้ใช้ให้มา

- Ponytailมีและใช้: stdlib/nativeCSS, dependencyเท่าที่ต้องใช้, ไม่สร้างElectronappเพื่อให้เปิดเว็บได้
- Grillingมี: ใช้decisiontreeตรวจสิ่งที่ต้องตัดสินใจจริง (ชื่อ/merchant/gameidentity) และบันทึกสมมติฐาน ไม่ทำinterviewยาวขวางงานที่ผู้ใช้สั่งทำได้เลย
- Design Taste Frontendมี: editoriallayout, typehierarchy, contrast, responsive, animationที่มีหน้าที่
- Sitesมี: localpreview/source/privatepublishของต้นแบบเมื่อทำได้
- Redesign Existing Projects ใช้ในรอบ Minecraft modern: เปลี่ยนองค์ประกอบและความหนาแน่นโดยรักษา HTML/CSS/JS และเมนูเดิม
- ไม่พบSKILL.mdชื่อdaisyUI/Electron/frontend-design/UI UX Pro Max/OpenPood Releaseในskilldirectoriesที่ตรวจ ชื่อที่ไม่มีไม่ได้อ้างว่าเรียกใช้แล้ว
- daisyUIเป็นcomponentlibraryที่เพิ่มได้เมื่อเลือกTailwindจริง ส่วนElectronเป็นdesktopappframework ไม่จำเป็นต่อbrowserwebนี้
- AutoReleaseต้องรอserverrepo/build/test/stagingจริง ไม่deployJAR/liveworldจากข้อความAIโดยไม่มีgate

## แมพMinecraftผ่านเว็บ: BlueMapเป็นทางหลัก

เลือก [BlueMap](https://bluemap.bluecolored.de/wiki/getting-started/Installation.html) สำหรับJavaworldและ3Dexploration; Dynmapเป็นทางเลือก2Dหากต้องการลดstorage/renderbudget ไม่ติดทั้งสองเพื่อหน้าที่เดียวกันในlaunch
versionของBlueMapต้องตรงbackendPaperที่เลือก การเข้าด้วยclient1.16.5ไม่ทำให้rendererต้องรันbackend1.16.5

[BlueMap plugin config](https://bluemap.bluecolored.de/wiki/configs/Plugin.html) มีlive-player-markers, hidegamemodes/vanish settingsและrenderpauseเมื่อonlineถึงเกณฑ์
[Map config](https://bluemap.bluecolored.de/wiki/configs/Maps.html) รองรับmarker setsและdynamicผ่านAPI
นี่เป็นการrenderworldและอัปเดตmarkers ไม่ใช่videostream60FPSของเกม: blockgeometryอัปเดตตามqueue/renderเวลา ส่วนตำแหน่งผู้เล่นเป็นliveupdatesเมื่อเชื่อมได้

### การติดตั้งและเสิร์ฟ

1. BlueMapในstagingPaper ตรวจruntimeversionและสิทธิ์ดาวน์โหลดassetsตามเอกสาร
2. Renderเฉพาะlobby400×400และพื้นที่publicก่อน ตั้งrender-mask/worldvisibility ไม่เผยdungeonsecret/adminroom/claimprivateที่ไม่ต้องการ
3. เริ่มrenderต่ำ1–2threadsเป็นค่าทดลองแล้วprofile CPU/IO/TPS ไม่รันfullworldrenderช่วงpeak
4. เสิร์ฟmapdomainผ่านHTTPS reverseproxy ให้browserถึงBlueMapassetsและlive endpoints ไม่เปิดRCON/Minecraftconsole
5. เว็บembedmapที่originอนุญาตframe/CSP หรือเปิดmapviewข้างเดียวถ้าproviderheadersกันiframe
6. Renderqueue/freshnesstimestamp/errorstateในหน้าแมพ มี“เปิดแมพเต็มหน้า”และlowbandwidthmode

HostedSitesfrontendเข้าถึง`localhost`ของเครื่องMinecraftไม่ได้ Productionต้องมีHTTPSmaporiginจริง/securetunnelที่เจ้าของจัดไว้ ไม่ใส่privateLANURLบนเว็บแล้วเรียกว่าสด
เปิดเฉพาะendpointจำเป็น หน้าดูแมพไม่รับservercommandหรือtokenของadmin

### SearchและPOIใช้ทะเบียนเดียวกับNPC

CorezoneID/stationIDเป็นsourceเดียว: zoneNameTH/EN/tags/services/world/x/y/z/facing/polygon/access/openstate/thumbnail/revision
ตัวอย่างค้น“ซ่อม/ตีเหล็ก/repair”พบforge_repair; “ธนาคาร/ฝาก/bank”พบbank_teller
searchส่งresultพร้อมworldและfloor ชั้น2ไม่ใช้Yเดียวกับชั้น1; sortใกล้spawn/ใกล้playerเมื่อplayerยินยอมแชร์พิกัด
Clickresult→BlueMapcameraไปPOI→แสดงpathบนพื้น/regionoutline + hours/เงื่อนไขบริการ ถ้ายังปิดให้เห็นป้ายปิดและไม่warp
อย่าคัดพิกัดmanualจากภาพconceptลงBlueMapAPI ต้องsyncactualstationหลังworldbuildQA

customresourcepacksสำหรับblocksต้องทดสอบrenderer ตาม [BlueMap customization](https://bluemap.bluecolored.de/wiki/customization/)
ไม่รับรองModelEngineNPC/helditems/animationsจะปรากฏตรงกันอัตโนมัติ: ใช้POImarker+ภาพthumbnailสำหรับserviceNPC
[BlueMapEntities](https://github.com/BlueMap-Minecraft/BlueMapEntities) เป็นaddonrenderentityที่บันทึกอยู่ในworld ไม่ใช่liveanimationของNPC จึงไม่ใช้แทนgameModelEngine

### Privacyและperformance

- staffvanish/hiddenworld/privateclaimต้องถูกกรองฝั่งserver/APIก่อนส่ง ไม่เพียงซ่อนด้วยCSS
- playerlive opt-outได้; ถ้าpublicsurvivallocationทำให้ถูกไล่ล่า ให้แสดงเฉพาะlobbyหรือใช้delayตามpolicy
- worldใหญ่paginate/indexPOI, lazyloadtiles, cancellablefetch, cacheTTL ขึ้นstalenessเมื่อbridgeหลุด
- viewportมือถือ/lowGPUมี2D/POIlistfallback ไม่บังคับโหลด3Dหลายworldพร้อมกัน

## ระบบรับเงิน: hostedcheckoutและwebhook

แผนเริ่มด้วย [Stripe Checkout](https://docs.stripe.com/payments/checkout/quickstarts) และPromptPayที่merchantเปิดใช้ได้: [Stripe PromptPay](https://docs.stripe.com/payments/promptpay) ระบุTHB/บัญชีธุรกิจTHและflowQRผ่านbankapp
ถ้าmerchantจริงเลือกผู้ให้บริการอื่น ใช้adapterเดียวตามAPI/สัญญาของproviderนั้น ไม่ทำslipOCRเป็นหลักฐานรับเงินโดยลำพัง
ยังไม่ได้เปิดmerchant/ยืนยันผลิตภัณฑ์กับproviderหรือสร้างchargeในรอบนี้

สินค้าlaunchเสนอcosmeticsที่บอกของที่ได้แน่นอน ไม่มีrandompaidcrate; haloมีรุ่นcosmeticคนละIDกับRPGrelicที่มีstat
currencyซื้อด้วยเงินจริงเมื่อเปิดในอนาคตต้องไม่มีcashoutและกำหนดการใช้ชัด ตาม [Minecraft usage guidelines](https://www.minecraft.net/en-us/usage-guidelines) การขายสิทธิ์/เกมเพลย์ยังต้องเป็นไปตามเงื่อนไขของMojangแม้ได้รับอนุญาตให้เปิดเซิร์ฟในประเทศแล้ว
usercountryapprovalไม่ใช่ผลยืนยันmerchantหรือlicenseจากผู้ให้บริการ ไม่ตีความแทนหลักฐานจริง

### Flowออนไลน์

```text
ผู้เล่น loginเว็บ + linkเกมเป็นUUID
  → เลือกSKUจากcatalogversionที่serverเชื่อถือ
  → เว็บสร้างorderและราคาหน่วยสตางค์/THBฝั่งserver
  → providerhostedcheckout (webไม่มีsecretkey)
  → webhookที่verify signature/amount/currency/session/order
  → commit PAID + enqueuefulfillmentในtransactionเดียวกัน
  → FantasyCoreรับdeliveryjobทางHTTPS
  → Coreinboxclaim/dedupe/orderline validation
  → ลงcosmeticentitlement/creditledger หรือinventorydeliveryjournal
  → ACK receiptกลับweb
  → accountpageแสดงdelivered/queued/mailbox
```

เป้าหมายonlineอยู่ในเกม: ภายใน2–5s **หลังproviderยืนยันรับเงินแล้ว** บนระบบสุขภาพปกติ เป็นSLOที่จะทดสอบ ไม่รับรองทันทีแม้webhook/เกม/networkล่ม
ออนไลน์และinventoryพร้อม→physicalitemเข้ากระเป๋า; offline/dead/fullinventory→mailbox/entitlementเก็บถาวร พร้อมรับเมื่อjoin
cosmeticownershipเปิดใช้ได้โดยไม่แจกItemStackซ้ำทุกครั้งlogin

### รับเงินอย่างถูกต้อง

- Return/successURLไม่ใช่หลักฐานรับเงิน; frontendไม่เป็นผู้grantitem ตาม [Stripe fulfillment](https://docs.stripe.com/checkout/fulfillment.md?payment-ui=stripe-hosted)
- webhookตรวจrawbody/signatureด้วยofficialSDK/secret, amountcurrencyต้องตรงordersnapshotและmetadataorderID
- Handlecompletedเมื่อpaid, async_payment_succeeded, async_payment_failed, expiryตามflowที่เลือก ไม่grantบนunpaidcompleted
- uniqueprovider_event_IDกันeventซ้ำ; order transitionsเช็คสถานะ ไม่downgradepaidจากeventfailedเก่าที่มาทีหลัง
- Checkoutcreateใช้idempotencykeyจากorderID/attemptไม่สร้างchargeทุกrefresh; serverpricingไม่รับยอด/quantityผิดจากclient
- เก็บmoneyเป็นintegerหน่วยย่อย ไม่ใช้float; snapshotSKU/quantity/revision/finalamount/discountและผู้รับUUIDตอนเริ่ม
- webhookcommitdurableแล้วตอบ2xxเร็ว ไม่รอMinecraftตอบจนproviderretryกอง
- Reconcilejobตรวจorderpaidที่ไม่มีdelivery/outboxและproviderstatesที่มีเหตุผล ไม่blindgrantตามclientreceipt

## Exactly-onceที่ระดับธุรกิจ และข้อจำกัดinventory

Webqueueส่งซ้ำได้ Coreต้องมีinboxunique(order_id,line_id,fulfillment_version) และoperationIDเดิมสำหรับretry
Cosmeticentitlement/creditledgerบันทึกพร้อมinboxcompleteในDBtransactionเดียวที่Coreเป็นเจ้าของ จึงไม่grantซ้ำจากeventretry
MinecraftinventoryกับSQLไม่ได้atomictransactionเดียวกัน: physicaldeliveryต้องintentjournal/serials/itemescrow/slotlocking/recoveryและทดสอบcrashwindow
ไม่อ้างว่า `addItem(); markDelivered();` สองคำสั่งทำให้exactlyonceโดยอัตโนมัติ
ถ้าcrashหลังใส่itemก่อนmarkdelivered recoveryตรวจserial/receiptและquarantineรายการคลุมเครือแทนaddซ้ำทันที
Coreไม่ยอมรับUUIDเปลี่ยนหลังจ่ายเพียงเพราะusernameเปลี่ยน และมีorderlineownershipตรวจใหม่

### การคืนเงินและปัญหา

- orderมีdelivery_stateแยกpayment_state; paidแต่เกมofflineคือqueuedไม่ใช่failedpayment
- retrybuttonสร้างความพยายามใหม่ของjobเดิมไม่สร้างorderใหม่
- refundใช้providerAPIและreconcileผลสำเร็จจริง; cosmeticrevokeได้ตามpolicy มีaudit ถ้าphysicalitemใช้/เทรดแล้วให้reviewcase ไม่ลบของคนอื่นตามชื่อitem
- chargeback/refundไม่แก้walletโดยsecretSQLdelta ห้ามกลบร่องรอยledger
- dashboardบอกpending/paid/queued/delivered/partial/review/refunded พร้อมtimestamps ไม่ใช้คำว่าสำเร็จรวมทุกชั้น

## IdentityและAPI

ใช้websessionauthที่providerยืนยัน แล้วให้ผู้เล่นในเกมใช้`/link`สร้างrandomone-timecode hashed, TTL5min, rate-limitและผูกUUID
Webกรอกcodeพร้อมlogin→Coreverify→linkaccountatomic; ห้ามผูกด้วยชื่อเกมอย่างเดียวหรืออ้างอีเมลMojangที่webไม่ได้verify
online-mode/proxyforwardingต้องออกแบบจากserverจริง UUIDsourceถูกต้อง; ไม่เปิดรับuserปลอมจากclientpaymentmetadata
คำสั่ง`/unlink`ต้องverifyบัญชี+cooldown/recentauth และบอกผลorders/rights ไม่unlinkแล้วนำorderไปให้UUIDใหม่

APIproposal:

| Method/route | Scope |
|---|---|
| GET /api/public/status | sanitizedcachedserverstatus |
| GET /api/public/zones?q= | publiczone/POIonly |
| GET /api/news | publishedposts |
| GET /api/catalog | publishedSKUwithprice/currency/revision |
| POST /api/account/link | authenticatedweb + one-timecode |
| POST /api/checkout | authenticatedlinkedUUID + SKU fromservercatalog |
| GET /api/orders/:id | owneronly |
| POST /api/payments/webhook | verifiedproviderpayload |
| POST /api/bridge/jobs/claim | signedservermachineidentity; leasedjobs |
| POST /api/bridge/jobs/:id/ack | samelease + receipt + uniqueoperationID |
| POST /api/admin/orders/:id/retry | staffRBAC/reason/audit, sameorder |

CoreHTTPintegration async/networkthread DBpool แก้Bukkitinventoryบนmainthreadเท่านั้น
OutboundHTTPSpoll/longpollเหมาะกับgamebehindNAT; ไม่เปิดpublicRCONและไม่ส่งfree-formconsolecommandsจากweb
ServerrequestมีHMAC/nonce/timestamp/bodyhashและreplayprotection หรือmTLS/tokenrotationตามinfraที่เลือก; schemaระบุoperationtyped/allowlisted
PublicAPIไม่มีIPผู้เล่น/UUIDที่ไม่ต้องใช้/walletledgerหรือinternalpaths; productรูป/ราคาpublic แต่grantkeyเป็นserversecret

## Storageและoriginalpluginownership

เริ่มmonolithweb+DB+Corebridgeแทนmicroservicesหลายชุด; providerหนึ่งตัว/packownerหนึ่งตัว/ledgerownerCoreหนึ่งตัว
Webเป็นเจ้าของposts/catalog/orders/provider_events/outbox/account_links; Coreเป็นเจ้าของeconomy/items/inbox/entitlements/physicaldeliveryjournal
อย่าให้webเขียนwalletrowตรง และอย่าส่งdatafeedที่clientแก้goldจากdashboardเอง
SQLite/D1/Postgresเลือกจากhostingจริง transactionconstraintsไม่เหมือนกัน ใช้schemaที่เลือกจริงในimplementation ห้ามใช้DBtransactionsyntaxเดียวข้ามทุกตัวโดยไม่ตรวจ
StaticSitesprototypeปัจจุบันไม่มีbindingsเหล่านี้ ถ้าสร้างbackendผ่านSitesต้องใช้Workerstarter/auth/storagecapabilitiesที่รองรับและHTTPSสำหรับexternalAPI (ไม่rawTCPSQL)

## Automationที่ต้องมีในsoftware

| Trigger | Action | เมื่อผิดพลาด |
|---|---|---|
| publishnews | เวอร์ชันโพสต์ออกเว็บ/cacheinvalidate | เก็บdraft/rollbackrevision |
| Corezonepublish | updatePOIindex/BlueMapmarkers | retryrevisionเดียว, showstale |
| providerpaid | order/outboxcommit | duplicateeventdedupe |
| gamejobclaim | deliverentitlement/itemjournal | leaseexpiry/recovery |
| playerjoin | claimqueuedmailbox/refreshHUD | limitbatchไม่lagjoin |
| outboxbacklog | retrybackoff+reconcile | deadletterพร้อมstaffreview |
| providerrefund | refundstate/revokeที่policyรองรับ | no blinddelete |
| deployment | tests→staging→releaseversion→healthcheck | rollbackpack/plugin/configcompatible |

เป็นruntimeautomation/webhookjobs ไม่ใช่คำสั่งให้ChatGPTส่งmessage/ตั้งschedulerนอกเว็บในรอบนี้
Externalemail/Discordnotificationต่อเป็นoptionalหลังuser/providercredentialsพร้อม ไม่มีการส่งข้อความถึงบุคคลอื่นแล้วในงานนี้

## Gateพร้อมรับเงินจริง

1. เชื่อมgameUUIDจริงและmerchanttestmode ผ่านsignatureforged/replay/wrongamount/wrongcurrency/duplicate/out-of-orderevents
2. ปิดbrowserก่อนกลับsuccesspageแล้วยังfulfillได้; providerpaidแต่Minecraftofflineก็queuedถูก
3. 2paymentพร้อมกัน, refreshcheckout, providerretrys, bridgeacklost, restartgameก่อน/หลังinventoryapply ไม่grantซ้ำ
4. fullinventory/offline/dead/worldchange/namechange/linkedUUIDwrong, cancel/expire/refundpartialชัดเจน
5. staffRBAC/CSRF/authcookies/rate-limit/audit secretsไม่อยู่browser/sourcearchive/log
6. mapprivatezones/vanishไม่leak, APIsearchพบครบ12zoneและพิกัดตรงworldจริง
7. productioncatalogมีคำอธิบาย/ราคา/การส่งมอบ/คืนเงินตามจริง ownermerchantพร้อม secretstoredและrotationtested
8. releasegateผ่านแล้วจึงเปิดcheckoutfeatureflag ไม่เริ่มรับเงินจริงจากprototypeที่ยังไม่เชื่อมgame

## แผนส่งมอบ

- PhaseAทำแล้ว: map/UI/modelplans, ภาพconcept, pilotmodels, staticwebsiteที่ตรวจแบบได้
- PhaseB: serverrepoPaper/MythicMobs/ModelEngine/packprofile + Corebank/repair/station + adminpanelcompile/integrationtests
- PhaseC: WorldEditbuildจากorigins→walkthrough→BlueMap→publicPOIAPI
- PhaseD: webauth/orderDB/merchanttest/webhook/outbox/Corebridge end-to-endtest
- PhaseE: clientmatrix/load/recovery + approvedcatalog +เปิดจริงพร้อมmonitor

คำว่า automationทั้งหมดหมายถึงflowที่ระบุและผ่านทดสอบ ข้อมูลที่ยังไม่มีเช่นmerchant/host/เซิร์ฟที่รันจริงต้องเชื่อมก่อนถึงจะclaimว่าเงินและของทำงานแล้วได้
