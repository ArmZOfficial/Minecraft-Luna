# ภาพต้นแบบสำหรับผลิตโมเดล Luma

สร้างด้วย built-in image generation; ภาพเป็น concept reference ไม่ใช่ UV หรือไฟล์ 3D
ไฟล์ภาพแต่ละใบอยู่ในโฟลเดอร์นี้ รายการ prompt ของ pilot เก็บใน `prompts.json`; brief ของงานผลิตเพิ่มเติมอยู่ท้ายเอกสาร

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
# Lyra alchemist — 6 October 2026

`npc-lyra-alchemist-turnaround.png`: ChatGPT image generation, FRONT/SIDE/BACK plus construction details.
Brief: young adult female elven alchemist, silver side braid, violet eyes, sage teal tailored long coat,
cream shirt, violet leather apron and gold botanical embroidery. Hollow gold goggles on the forehead,
separate cuffs, tiny belt vials in red/cyan/violet, leather boots with buckles. Anatomical right holds
a brass stirring rod into a compact floor kettle; left holds an upright violet flask by its neck.
Cuboid Blockbench-feasible shapes, crisp painted Minecraft pixels, matching views on the same baseline,
off-white grid sheet. This is a modeling reference, not a ready UV atlas or evidence of animation.
Actual model/rig and native views are in `../models/npc_lyra_alchemist*`; runtime is still pending.
