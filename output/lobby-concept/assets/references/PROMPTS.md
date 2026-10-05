# ภาพต้นแบบสำหรับผลิตโมเดล Luma

สร้างด้วย built-in image generation; ภาพเป็น concept reference ไม่ใช่ UV หรือไฟล์ 3D
ไฟล์ภาพแต่ละใบอยู่ในโฟลเดอร์นี้ รายการ prompt ฉบับเต็มเก็บใน `prompts.json`

| Asset ID | ภาพ | งานที่ส่งต่อ |
|---|---|---|
| npc_arcane_banker | npc-arcane-banker.png | Generic .bbmodel, idle/greet/count_coins |
| npc_rune_smith | npc-rune-smith.png | Generic .bbmodel, idle/greet/hammer |
| npc_quest_warden | npc-quest-warden.png | Generic .bbmodel, idle/greet/offer_scroll |
| npc_portal_mage | npc-portal-mage.png | Generic .bbmodel, idle/greet/cast |
| item_aether_halo | item-aether-halo.png | Java Block/Item JSON, head display; halo cosmetic หรือ gameplay item คนละ ID |
| item_runeblade | item-runeblade.png | Java Block/Item JSON, hand/GUI/ground display |

ภาพ halo มี inventory icon ที่เป็นวงโค้งกว่าต้นแบบ ให้ยึด geometry วงสี่เหลี่ยมใน TOP เป็นหลัก
สัดส่วนและระยะจริงให้ยึด production ticket; รายละเอียดสี/ตัวหนังสือที่ AI วาดไม่ใช่ค่าคอนฟิกบังคับ
