# FantasyCore — แกนระบบของ Luma (v0.1 PoC)

ปลั๊กอิน Paper ตาม [SERVER-SYSTEMS-PLAN §6](../output/lobby-concept/SERVER-SYSTEMS-PLAN-th.md) — รุ่นนี้ทำ **ลำดับพัฒนาข้อ 1–3** และ **CASUAL-SURVIVAL §12 ข้อ 2 (บ้าน + RTP)**
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
| NPC สถานี | `/fa npc spawn <action> [ชื่อ]` วาง Villager อมตะไม่มี AI ผูก action ID; `/fa npc anchor <action>` สำหรับ Citizens (`/npc command add -p fc action <action>`) | INTERIOR §16 |
| ไอเทม | แม่แบบใน `items.yml` (ตัวอย่าง `starter_runeblade`), ตัวตนอยู่ใน PDC + serial ลงทะเบียนใน DB, ไม่ทิ้งของลงพื้น | §6 ItemAdapter |
| บ้าน | `/sethome [ชื่อ] [confirm]`, `/home [ชื่อ]`, `/delhome`, `/homes`; เฉพาะ `luma_housing` + ต้องเป็นเจ้าของ/สมาชิกแปลง PS, ตรวจซ้ำทุกครั้งก่อนวาร์ป, โควตา 1/3/5 | CASUAL §9 |
| RTP | `/rtp [housing|resource]` วงแหวนกระจายตามพื้นที่, footprint 2×2 + ช่องหัว 3, ไม่ลงพื้นอันตราย/border/region, โหลด chunk async ≤2 งาน/โลก, 24 จุด/10 วิ, cooldown จาก receipt ในฐานข้อมูล | CASUAL §3 |
| วาร์ป | อุ่นเครื่อง 3 วิ ยกเลิกเมื่อขยับ/โดนตี, ล็อกหลังต่อสู้ 15 วิ, บันทึกผลเมื่อ teleport สำเร็จจริงเท่านั้น; `/spawn` | §3, §9 |
| ที่ดิน | `/land` อ่าน region ของ WorldGuard/ProtectionStones ตรงที่ยืน (ขนาด = max − min + 1) | PROTECTIONSTONES §1 |
| โลก | สร้าง/โหลด `luma_housing` (border 5,000) และ `luma_resource` (border 3,000) | |
| Placeholder | `%fantasycore_gold%` `_bank` `_red` (+ `_raw`) `%fantasycore_home_limit%` อ่านจาก cache | HUD spec |

**ยังไม่มี:** mailbox, daily reward, quest exchange, skins, canvas, upgrade, ซื้อ/อัปเกรดหินผ่าน Core, AdminPanel แบบ GUI, Bedrock — เป็น phase ถัดไปตามลำดับในเอกสาร

## Build

ต้องมี Java 25 (ถ้าไม่มี Gradle toolchain จะดาวน์โหลดให้)

```powershell
cd fantasycore
.\gradlew.bat build        # compile + unit test → build\libs\FantasyCore-0.1.0.jar
```

หรือใช้ `server\build-plugin.cmd` ซึ่ง build แล้วคัดลอกเข้าเซิร์ฟ staging ให้
GitHub Actions (`.github/workflows/fantasycore.yml`) build + test ทุกครั้งที่แก้โฟลเดอร์นี้ และแนบ jar เป็น artifact

## สิทธิ์

| Node | ค่าเริ่มต้น | ใช้ทำ |
|---|---|---|
| `fantasy.player` (menu, balance, bank.use, home.use, rtp.use, spawn, land.use) | ทุกคน | ใช้งานพื้นฐาน |
| `fantasy.bank.remote` | ไม่มี | ใช้ธนาคารจากทุกที่ (แรงค์) |
| `fantasy.home.limit.3` / `.5` | ไม่มี | โควตาบ้าน (เควส housing_workshop / housing_community) |
| `fantasy.rtp.bypass-cooldown` | OP | ทดสอบ |
| `fantasyadmin.view` / `.economy.adjust` / `.npc.edit` / `.content.edit` / `.audit` | OP | งานแอดมิน — แจกผ่านกลุ่ม LuckPerms ใน `server/setup/luckperms-setup.txt` |

## ข้อมูลและการกู้คืน

- ฐานข้อมูล `plugins/FantasyCore/fantasycore.db` (SQLite WAL) — ตาราง `accounts`, `operations`, `ledger`, `audit_log`, `players`, `homes`, `item_instances`, `stations`, `travel_receipts`
- สำรองไฟล์ `.db` + `-wal` + `-shm` **พร้อมกับ** โฟลเดอร์โลกและ `plugins/WorldGuard/worlds/*/regions.yml` เป็นชุดเดียว
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
menu/      Menu + MenuListener (กันย้ายของ), MainMenu, BankMenu, HomesMenu, RtpMenu
item/      ItemTemplateService, ItemInstanceStore
hook/      VaultHook/VaultEconomyProvider, PlaceholderHook (โหลดเฉพาะเมื่อมีปลั๊กอินนั้น)
command/   PlayerCommands, CoreCommand (/fc), AdminCommand (/fa), PendingConfirmations
```

## สิ่งที่ตรวจแล้วในรอบนี้ และสิ่งที่ยังไม่ได้ตรวจ

ตรวจแล้ว: compile กับ Paper API 26.2 + Java 25 ไม่มี warning, unit test 21 รายการผ่าน (รวมฝาก/ถอนพร้อมกัน 200 รายการ, op ID ซ้ำ, death loss ทุกเปอร์เซ็นต์เทียบ BigInteger, โควตาบ้าน, การกระจาย RTP, ทุก key ข้อความมีจริง), YAML/MiniMessage ทุกบรรทัด parse ได้,
คลาสหลักโหลดได้แม้ไม่มี Vault/WorldGuard/PlaceholderAPI

**ยังไม่ได้ตรวจ:** การรันบนเซิร์ฟ Paper จริงและการเล่นในเกม — ทำตาม checklist ใน [server/README-th.md](../server/README-th.md)
