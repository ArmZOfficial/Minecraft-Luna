# FantasyCore — แกนระบบของ Luma (v0.3)

ปลั๊กอิน Paper ตาม [SERVER-SYSTEMS-PLAN §6](../output/lobby-concept/SERVER-SYSTEMS-PLAN-th.md)
- v0.1: **ลำดับพัฒนาข้อ 1–3** และ **CASUAL-SURVIVAL §12 ข้อ 2 (บ้าน + RTP)**
- v0.2: ผูก NPC ของ Citizens ตรง, **กล่องจดหมาย** (ส่วน mailbox ของข้อ 4) และ **รับของรายวัน** (CASUAL-SURVIVAL §4 / §12 ข้อ 3 ส่วน Reward)
- v0.3: **QuestExchange** (CASUAL §7) — เลือกสูตร → preview → ยืนยัน → ตัดวัตถุดิบ → รางวัลเข้า `/mail`; 3 สูตร vanilla พร้อมโควตาและ recovery
Backend: Paper 26.2 · Java 25 · `api-version: 26.2` · client 1.16.5+ ผ่าน ViaVersion/ViaBackwards

## มีอะไรในรุ่นนี้

| ระบบ | สิ่งที่ทำได้ | ตามเอกสาร |
|---|---|---|
| Economy | `gold.wallet` / `gold.bank` / `red.wallet` เป็นจำนวนเต็ม, ทุกการเปลี่ยนยอดอยู่ใน transaction เดียวพร้อม ledger + operation ID (ส่ง ID ซ้ำไม่ทำซ้ำ) | SERVER-SYSTEMS §6–7 |
| ธนาคาร | `/bank` เมนูฝาก/ถอน 100/1,000/10,000/ทั้งหมด + ประวัติ 10 รายการ (เวลาไทย); ใช้ได้ที่ NPC/anchor ธนาคาร หรือมีสิทธิ์ `fantasy.bank.remote` | §7 เงินและธนาคาร |
| ตาย | เสีย `gold.wallet` ตาม % ปัดลงต่อโลก (staging: 30% ใน luma_housing/luma_resource), เงินฝาก/เงินแดงไม่เสีย, keep-inventory ต่อโลก | §7 |
| Vault | ลงทะเบียน provider เฉพาะทองที่พก (deposit ปัดลง / withdraw ปัดขึ้น) — ห้ามมี economy plugin อื่นซ้อน | §3 |
| แอดมิน | `/fa doctor`, `/fa bank`, `/fa eco give/take … <เหตุผล>` → preview → `/fa confirm <รหัส>` (ตรวจยอดซ้ำตอน apply), `/fa audit` | ADMIN-PANEL-SPEC |
| เมนู | `/menu` 54 ช่อง 7 ปุ่มตามภาพ + ธนาคาร/จุดเกิด; ระบบที่ยังไม่มีแสดง "ยังไม่เชื่อม"; กัน shift/drag/hotbar/double-click | CASUAL §2 |
| NPC สถานี | `/fa npc spawn <action> [ชื่อ]` วาง Villager อมตะไม่มี AI ผูก action ID; หรือมองไปที่ NPC ของ Citizens แล้ว `/fa npc bind <action>` (คลิกแล้วเปิดบริการทันที ไม่ต้องตั้ง `/npc command`) | INTERIOR §16 |
| กล่องจดหมาย | `/mail` รับทีละชิ้น/ทั้งหมด; ของที่เข้ากระเป๋าไม่ได้ (รางวัล, `/fa item give`) มารอที่นี่ ไม่ตกพื้น; สถานะ PENDING→CLAIMING→CLAIMED และรายการที่ค้างตอนเซิร์ฟดับถูกย้ายไป REVIEW ให้ทีมงานตัดสิน (`/fa mail review/release/void`) | SERVER-SYSTEMS §6 ข้อ 6–7 |
| รับของรายวัน | `/rewards` ปฏิทินรอบสะสม 7 ครั้ง วันใหม่ 00:00 เวลาไทย รับวันละครั้ง (ไม่ไล่ย้อนหลัง); สิทธิ์ + เงิน + ของในกล่อง commit ใน transaction เดียว; แจ้งตอนเข้าเกมถ้ายังไม่รับ | CASUAL §4 |
| แลกของ | `/exchange`, เมนู 3 สูตร vanilla, batch 1–16, โควตาต่อวันไทยรวมทุก recipe version, ไม่ตัด custom/named/PDC; ของรอรับใน `/mail`; journal เก็บ slot ก่อน/หลัง + รางวัลที่ตรึงไว้; รายการค้าง CONSUMING → REVIEW และต้องแอดมินยืนยัน | CASUAL §7 / [คู่มือ Exchange](EXCHANGE-th.md) |
| ไอเทม | แม่แบบใน `items.yml` (ตัวอย่าง `starter_runeblade`), ตัวตนอยู่ใน PDC + serial ลงทะเบียนใน DB, ไม่ทิ้งของลงพื้น | §6 ItemAdapter |
| บ้าน | `/sethome [ชื่อ] [confirm]`, `/home [ชื่อ]`, `/delhome`, `/homes`; เฉพาะ `luma_housing` + ต้องเป็นเจ้าของ/สมาชิกแปลง PS, ตรวจซ้ำทุกครั้งก่อนวาร์ป, โควตา 1/3/5 | CASUAL §9 |
| RTP | `/rtp [housing|resource]` วงแหวนกระจายตามพื้นที่, footprint 2×2 + ช่องหัว 3, ไม่ลงพื้นอันตราย/border/region, โหลด chunk async ≤2 งาน/โลก, 24 จุด/10 วิ, cooldown จาก receipt ในฐานข้อมูล | CASUAL §3 |
| วาร์ป | อุ่นเครื่อง 3 วิ ยกเลิกเมื่อขยับ/โดนตี, ล็อกหลังต่อสู้ 15 วิ, บันทึกผลเมื่อ teleport สำเร็จจริงเท่านั้น; `/spawn` | §3, §9 |
| ที่ดิน | `/land` อ่าน region ของ WorldGuard/ProtectionStones ตรงที่ยืน (ขนาด = max − min + 1) | PROTECTIONSTONES §1 |
| โลก | สร้าง/โหลด `luma_housing` (border 5,000) และ `luma_resource` (border 3,000) | |
| Placeholder | `%fantasycore_gold%` `_bank` `_red` (+ `_raw`) `%fantasycore_home_limit%` อ่านจาก cache | HUD spec |

**ยังไม่มี:** online/AFK reward, exchange สูตร custom/coupon, skins, canvas, upgrade, ซื้อ/อัปเกรดหินผ่าน Core, AdminPanel แบบ GUI, Bedrock — เป็น phase ถัดไปตามลำดับในเอกสาร

## Build

ตั้ง Java 25 ใน `JAVA_HOME` หรือ PATH ก่อนเรียก wrapper (Gradle ต้องมี Java เริ่มตัวเองก่อนดาวน์โหลด toolchain)
Paper API ที่ใช้ compile ถูกตรึงเป็น `26.2.build.129-stable` ให้ตรง backend build 129 ใน manifest

```powershell
cd fantasycore
.\gradlew.bat build        # compile + unit test → build\libs\FantasyCore-0.3.0.jar
```

หรือใช้ `server\build-plugin.cmd` ซึ่ง build แล้วคัดลอกเข้าเซิร์ฟ staging ให้
GitHub Actions (`.github/workflows/fantasycore.yml`) build + test ทุกครั้งที่แก้โฟลเดอร์นี้ และแนบ jar เป็น artifact

เครื่อง Windows นี้มี JDK ที่ตรวจ SHA256 แล้วใน `.tools/jdk25` (ไม่อยู่ใน Git)
หากยังไม่มี Java ใน PATH ใช้ PowerShell จาก root repo:

```powershell
$env:JAVA_HOME = (Get-ChildItem '.tools/jdk25' -Directory | Select-Object -First 1).FullName
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.cache/gradle'
# เฉพาะเครื่องที่เจอ Unable to establish loopback connection เพราะ socket temp path:
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp --enable-native-access=ALL-UNNAMED'
Set-Location fantasycore
.\gradlew.bat build --no-daemon
```

ค่า environment นี้มีผลกับ PowerShell session นี้; temp path ต้องเป็นโฟลเดอร์สั้นที่เครื่องคุณเขียนได้

## สิทธิ์

| Node | ค่าเริ่มต้น | ใช้ทำ |
|---|---|---|
| `fantasy.player` (menu, balance, bank.use, home.use, rtp.use, spawn, land.use, rewards, mail, exchange) | ทุกคน | ใช้งานพื้นฐาน |
| `fantasy.bank.remote` | ไม่มี | ใช้ธนาคารจากทุกที่ (แรงค์) |
| `fantasy.home.limit.3` / `.5` | ไม่มี | โควตาบ้าน (เควส housing_workshop / housing_community) |
| `fantasy.rtp.bypass-cooldown` | OP | ทดสอบ |
| `fantasyadmin.view` / `.economy.adjust` / `.npc.edit` / `.content.edit` / `.audit` | OP | งานแอดมิน — แจกผ่านกลุ่ม LuckPerms ใน `server/setup/luckperms-setup.txt` |
| `fantasyadmin.exchange.resolve` | OP | ตัดสินรายการแลกที่ค้าง โดย preview + เหตุผล + `/fa confirm` |

## ข้อมูลและการกู้คืน

- ฐานข้อมูล `plugins/FantasyCore/fantasycore.db` (SQLite WAL, schema v3) — ตาราง `accounts`, `operations`, `ledger`, `audit_log`, `players`, `homes`, `item_instances`, `stations`, `travel_receipts`, `mail`, `reward_claims`, `exchange_operations`, `exchange_outputs`
- อัปจาก v0.1/v0.2 → v0.3: สำรองก่อน ปลั๊กอิน migrate เป็น schema v3 และสร้าง `exchanges.yml` เมื่อยังไม่มี โดยไม่เขียนทับ config ที่แก้แล้ว
- SQLite นี้เป็นข้อมูล Core ฝั่งเกม ส่วน PostgreSQL เป็นข้อมูลเว็บ; ยังไม่มี bridge payment/ส่งคำสั่งจากเว็บในรุ่นนี้
- หยุดเซิร์ฟก่อนสำรอง `.db` (+ `-wal`/`-shm` ถ้ายังมี) **พร้อมกับ** playerdata/โฟลเดอร์โลกและ `plugins/WorldGuard/worlds/*/regions.yml` เป็นชุดเดียว
- เปิดฐานข้อมูลไม่ได้ → ปลั๊กอินปิดตัวเอง (fail closed) ไม่ให้บริการเงินครึ่ง ๆ กลาง ๆ
- schema มีเลข version; ปลั๊กอินรุ่นเก่าจะไม่ยอมเปิดฐานข้อมูลที่ใหม่กว่า

## โครงสร้างโค้ด

```
storage/   Database (SQLite + lock + thread DB), Migrations, PlayerStore
economy/   EconomyStore (logic เงินล้วน ทดสอบได้), EconomyService, DeathListener
audit/     AuditStore
claim/     ClaimAdapter → WorldGuardClaimAdapter | NoClaimAdapter, LandInfo
travel/    TeleportService (warmup), CombatTracker, LandingValidator, RtpService, RtpMath, SpawnTravel
home/      HomeService, HomeStore, HomeNames
station/   ActionRegistry (action ID → บริการ), StationService, StationListener
mail/      MailStore (สถานะจดหมาย), MailService (ส่งเข้า inventory หรือกล่อง)
reward/    RewardStore (สิทธิ์รายวัน + เงิน + จดหมายใน transaction เดียว), RewardService
exchange/  ExchangePlanner, ExchangeStore (journal + quota + recovery), ExchangeService (inventory + recipes)
menu/      Menu + MenuListener (กันย้ายของ), MainMenu, BankMenu, HomesMenu, RtpMenu, RewardMenu, MailMenu
item/      ItemTemplateService, ItemInstanceStore
hook/      VaultHook/VaultEconomyProvider, PlaceholderHook (โหลดเฉพาะเมื่อมีปลั๊กอินนั้น), CitizensBridge (reflection)
command/   PlayerCommands, CoreCommand (/fc), AdminCommand (/fa), PendingConfirmations
```

## สิ่งที่ตรวจแล้วในรอบนี้ และสิ่งที่ยังไม่ได้ตรวจ

รอบ v0.3: build กับ Paper API 26.2 + Java 25 ผ่าน, unit tests **42 รายการผ่าน** (27 เดิม + 15 ใหม่)
รวมตัดเฉพาะของธรรมดา, วัตถุดิบไม่ครบ, โควตาข้าม recipe version/วัน, จองพร้อมกัน 20 รายการได้ 1 รายการ,
commit ซ้ำไม่แจกซ้ำ, rollback เมื่อ mailbox เขียนไม่ได้, การกักสถานะ CONSUMING, resolve ซ้ำ, migration v2→v3 รักษาเงินและจดหมาย,
และการปิดจดหมายพร้อมส่วนเกินใน transaction เดียว

การตัด inventory กับ SQLite เป็นคนละระบบ แม้เรียก `Player.saveData()` ก่อน commit ก็ยังไม่ใช่ transaction เดียวกัน
ข้อจำกัด disk failure/การ restore backup และวิธีกู้คืนอยู่ใน [คู่มือ Exchange](EXCHANGE-th.md) — ต้องผ่าน staging ก่อนเปิดบริการจริง

**ยังไม่ได้ตรวจ:** การรันบนเซิร์ฟ Paper จริงและการเล่นในเกม — ทำตาม checklist ใน [server/README-th.md](../server/README-th.md)

## Citizens หรือ NPC ของ Core?

ใช้ร่วมกัน: **Citizens ดูแลหน้าตา/ท่าทาง** (สกินผู้เล่น ชุด หันมอง เดินตามเส้นทาง) ส่วน **FantasyCore ดูแลว่าคลิกแล้วเกิดอะไร** (สิทธิ์ ระยะ เงิน audit)
NPC ของ Core (Villager) เหมาะกับ staging, วางด่วน และเป็น fallback เมื่อ Citizens ยังไม่ผ่านการทดสอบบน 26.2
NPC อนิเมชันจาก Blockbench เป็นอีกเส้นทาง (MythicMobs + ModelEngine ตาม MODEL-AND-CONTENT-PLAN) — ห้ามให้ Citizens กับ MythicMobs สร้างตัวละครซ้อนในสถานีเดียว
