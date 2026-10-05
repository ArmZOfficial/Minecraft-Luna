# คำสั่งส่งต่อ Antigravity / Blockbench สำหรับ Luma

## การเชื่อมที่ทำในเครื่องนี้

MCP server ที่ตรวจตอบจริง: `http://localhost:3000/bb-mcp`
Blockbench 5.2.1 + MCP 1.10.0; Antigravity config ที่แก้: `C:/Users/Administrator/.gemini/config/mcp_config.json`
เพิ่ม server entry อย่างเดียวและสำรองไฟล์เดิมชื่อ `mcp_config.json.fantasy-backup-<timestamp>`

```json
{
  "mcpServers": {
    "blockbench": {"serverUrl": "http://localhost:3000/bb-mcp"}
  }
}
```

ตัวอย่างนี้เป็นเฉพาะ entry ของ Blockbench ห้ามใช้แทนทั้งไฟล์จนลบ servers อื่น
Refresh MCP servers ใน Antigravity หากยังไม่แสดง tools; ไม่ต้องเปิด port ออกอินเทอร์เน็ต
อ้างอิง [Blockbench MCP README / Antigravity](https://github.com/jasonjgardner/blockbench-mcp-plugin#installation)

ในรอบนี้ ChatGPT เรียก MCP ผ่าน local HTTP sessionใหม่และ export pilot แล้ว ไม่ได้ส่ง taskไป agent Antigravity อัตโนมัติ
ใช้ไฟล์นี้เป็น task ที่ผู้ใช้สั่ง Antigravity ต่อได้ หรือใช้ local MCP client `tools/blockbench_mcp.py`

## Prompt งาน polish สำหรับ pilot

> ปรับโมเดล Luma จากไฟล์ pilot และภาพ reference ใน assets/references โดยรักษา asset ID และ gameplay contract อ่าน MODEL-AND-CONTENT-PLAN-th.md กับ ZONE-BUILD-TICKETS-th.md ก่อน ใช้ Blockbench MCP get_capabilities/tools schema จริง เลือก project ที่เป็นของงานนี้หรือสร้าง revisionใหม่ อย่าแก้ projectผู้ใช้อื่น NPCใช้ Generic .bbmodel หันหน้าNorthเท้าY0ที่16units/block มี hi_head/arms/forearms/legsและpropตามticket ปรับ silhouette/UVpixeltextureให้ใกล้ภาพ reference เพิ่มรายละเอียดเท่าที่ budgetอนุญาต ทดสอบ idle/greet/workให้คืนneutralpose ห้ามถือว่าภาพAIเป็นUVหรือanimationสำเร็จ ส่งไฟล์bbmodel texture preview manifestและรายการผลตรวจกลับ ห้ามอ้างproductionจนimportModelEngineและทดสอบclientmatrixแล้ว

## Ticket ที่เริ่มได้ทันที

| Asset | Reference | Outputใหม่ | ข้อกำหนดเฉพาะ |
|---|---|---|---|
| banker ✅ | assets/references/npc-arcane-banker.png | npc_arcane_banker_v2.bbmodel | ทำแล้ว: `tools/build_arcane_banker.py` → `preview_arcane_banker.py` → `manifest_arcane_banker.py` → `verify_arcane_banker.py` |
| smith ✅ | assets/references/npc-rune-smith.png | npc_rune_smith_v2.bbmodel | ทำแล้ว: `tools/build_rune_smith.py` → `preview_rune_smith.py` → `manifest_rune_smith.py` → `verify_rune_smith.py`; ขวามือ=+X (โมเดลหันทิศเหนือ/−Z) |
| quest ✅ | assets/references/npc-quest-warden.png | npc_quest_warden_v2.bbmodel | ทำแล้ว: `tools/build_quest_warden.py` → `preview_quest_warden.py` → `manifest_quest_warden.py` → `verify_quest_warden.py` |
| mage ✅ | assets/references/npc-portal-mage.png | npc_portal_mage_v2.bbmodel | ทำแล้ว: hood ใต้hi_head, staff_socket, ทุกท่า≤2.66บล็อก; `tools/build_portal_mage.py` → `preview_portal_mage.py` → `manifest_portal_mage.py` → `verify_portal_mage.py` |
| halo ✅ | assets/references/item-aether-halo.png | item_aether_halo_v2.bbmodel/JSON | ทำแล้ว: display8context; legacy 1.16.5 ยังต้องทดสอบ; `tools/build_luma_items_v2.py halo` → `preview_luma_items_v2.py` → `verify_luma_items_v2.py --write-manifest` |
| sword ✅ | assets/references/item-runeblade.png | item_runeblade_v2.bbmodel/JSON | ทำแล้ว: ปลายขั้น/ร่องรูน/การ์ด, display8context รวมมือซ้าย(mirror); `tools/build_luma_items_v2.py runeblade` |
| guide ✅ | ไม่มี (ออกแบบจากใบงานโซน01/ธงลาน) | npc_crystal_guide.bbmodel | ทำแล้ว: 148ชิ้น/18bones/5ท่า, revision2 แก้ผมจากโมเดลที่ผู้ใช้ปรับใน Blockbench หน้าม้าต่อช่อพอดีไม่ทับระนาบ เปิดคิ้ว; ไม้เท้าขวา/แผนที่ซ้ายติดมือ, ทดสอบทะลุ27จุดต่อชิ้น,269เฟรม; `build_crystal_guide.py` → `preview_crystal_guide.py` → `manifest_crystal_guide.py` → `verify_crystal_guide.py` |
| alchemist ✅ | assets/references/npc-lyra-alchemist-turnaround.png | npc_lyra_alchemist.bbmodel | ทำแล้ว: 206ชิ้น/21bones/5ท่า, คอขวดซ้าย/ด้ามไม้ขวาติดมือ, เตา root ไม่เคลื่อน,265เฟรม; `build_alchemist.py` → `preview_alchemist.py` → `manifest_alchemist.py` → `verify_alchemist.py` |

## ข้อกำหนดไฟล์

- source file, exported runtime file, PNG texture, inventory icon, previewfront/side/back, animationframes, manifest
- manifestมี id/revision/reference/source/runtime_target/scale/forward/textures/bones/animations/legacyfallback/tests/packhash
- ไฟล์ใหม่ไม่ทับ v1; texture namesอยู่ namespace luma, ไม่มีเลขCustomModelDataชน
- อนุญาต idle loop; greet/work one-shot; ไม่มี locomotion สำหรับ stationaryserviceNPC
- การซ่อม/รับเงิน/ส่งของเรียก Core transaction ไม่ทำจาก scriptในanimation
- รุ่นละเอียดตามคำขอผู้ใช้: ตรวจภาพหน้า/ข้าง/หลังและจุดต่อผม/นิ้ว/อุปกรณ์จริง ไม่จำกัด geometry ไว้เท่า pilot; UV แยกหน้าและปิด Auto UV หลัง bake การเพิ่มจำนวนชิ้นยังต้องผ่าน runtime performance

## ไลรา — ใบงานส่งต่อ

[คู่มือร้านยาและภาพทุกมุม](../../server/content/npc-models/LYRA-ALCHEMIST-th.md) · [contract disabled](../../server/content/npc-models/lyra-model-contract.json)

> เปิดต้นฉบับไลราใน project ใหม่และทำ revision ใหม่เมื่อ polish เพิ่ม รักษา anatomicalright=+X, North/−Z, เท้าY0, 16units:block; แว่น/ผมถักตามhi_head, flaskตามleft_hand, rodตามright_hand, เตาแยกrootไม่เคลื่อน ตรวจทุกเฟรม20FPSว่าคอขวด/ด้ามไม้ติดมือและปลายไม้อยู่ในน้ำยา UVแยก1236หน้า/atlas512ต้องคมและมีgutter ไม่ปล่อยAutoUVเขียนทับ วางShopDหน้าลูกค้าSouthyaw0และcalibrateoffsetจริง ห้ามให้ animation ทำเงิน/ยา/วัตถุดิบ ทดสอบ ModelEngineformat5/packlegacy-modern/counter/region/performance ก่อนenable; ผูกบริการด้วยalchemy.mainในCorev0.9; market.mainยังplanned และanimation event bridgeยังไม่มี

## คำสั่งตรวจที่มีใน workspace

`tools/blockbench_mcp.py` ใช้ MCP HTTP initใหม่ทุก invocation; อ่าน tools/list ก่อนใช้ชื่อ/schema
`tools/build_luma_models.py` เป็นสคริปต์สร้าง pilot ที่ทำแล้ว **ไม่ต้องรันซ้ำโดยไม่ตรวจ active project**
ไฟล์ `.bbmodel` ใน assets/models มี embeddedtexture และ keyframesจริง แต่ต้อง polishและruntime QAต่อ

## Moonfall boss — ผลงานที่ทำต่อผ่าน MCP แล้ว

[ไฟล์บอสv1](assets/models/boss_moonfall_guardian_v1.bbmodel) · [reference4มุม](assets/references/boss-moonfall-guardian-turnaround.png) · [คู่มือ/contract](../../server/content/dungeons/moonfall/README-th.md)
สร้างprojectใหม่ในlocalBlockbench5.2.1/MCP1.10.0 ลงสีatlas/geometry146cubes/19bones/8animationsแล้ว ไม่แก้NPCprojectที่เปิดอยู่ก่อน
nativepreview29เฟรมและofflinegateผ่าน; ModelEngine/Paper/client/runtimeยังไม่ทดสอบ
ไม่มีการส่งข้อความหรือtaskไปagentAntigravityอัตโนมัติ ผลงานรอบนี้ทำจากChatGPTผ่านlocalMCPโดยตรง

> งานpolishต่อ: เปิดboss_moonfall_guardian_v1.bbmodelในprojectใหม่และออกrevisionv2 รักษาNorth/−Z/เท้าY0/16units:block/assetnamespace/animationcontractตามmodel-contract.json เพิ่มรอยหิน/UVgold/cyan/violetเท่าที่มองเห็นได้ อนิเมชันslamต้องimpactที่1.25sหรือ25ticks แล้วคืนneutral ห้ามscriptkeyframeทำdamage/loot ห้ามanimatehitbox ตรวจeyeheight/footprintชนิดจริงและทุกpose/มุมก่อนexport เก็บv1ไว้ ยืนยันexactModelEngineimportformat5และclient1.16.5/รุ่นกลาง/26.2บนstagingก่อนอ้างเกมพร้อม

คำสั่งoffline: `python tools/verify_moonfall_model.py`; builder `tools/build_moonfall_boss.py --dry-run` อ่านbudgetโดยไม่เชื่อมMCP
หากจะใช้builderจริงต้องสร้างemptyprojectชื่อบอสและส่งUUIDที่ตรวจจากget_project_info; builderปฏิเสธไฟล์revisionที่มีแล้ว
จึงใช้เปิดv1/polishผ่านnativeMCPและexportชื่อv2สำหรับงานรอบถัดไป ไม่รันbuilderทับv1
