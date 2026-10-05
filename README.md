# Minecraft Luna — Luma Fantasy Town

พื้นที่ทำงานสำหรับเซิร์ฟ Minecraft แฟนตาซีและ casual roleplay
ชื่อแบรนด์ที่เสนอคือ **Luma / ลูม่า** ส่วน repository ใช้ชื่อ Minecraft-Luna ตามที่เจ้าของกำหนด

- [เริ่มอ่านแผนทั้งหมด](output/lobby-concept/START-HERE-th.md)
- [เว็บและวิธีเปิดดู](website/README-th.md)
- [ผังแมพและแปลนภายใน](output/lobby-concept/INTERIOR-AND-MAP-PLAN-th.md)
- [ใบงานสร้าง 12 โซน](output/lobby-concept/ZONE-BUILD-TICKETS-th.md)
- [ระบบและปลั๊กอิน](output/lobby-concept/SERVER-SYSTEMS-PLAN-th.md)
- [โมเดลและภาพอ้างอิง](output/lobby-concept/ASSET-GALLERY-th.md)

## สถานะงาน

มีเว็บต้นแบบที่ทดลองเมนู ค้นหาโซน คู่มือ ข่าว และศูนย์บัญชีได้ พร้อมภาพและเอกสารออกแบบ
มี NPC pilot 4 ตัวที่มี animation และ item pilot 2 ชิ้น export จาก Blockbench MCP
ยังไม่มีโลก Minecraft ที่ติดตั้งระบบจริง, plugin JAR, แผนที่สด, payment backend หรือการส่งของจริง

เป้าหมาย client คือ Java 1.16.5 ขึ้นไป ต้องผ่าน staging matrix ก่อนยืนยันการรองรับจริง
ไฟล์โลกและ schematic ที่ดาวน์โหลดมาเดิมไม่ได้รวมใน repository

## เปิดเว็บในเครื่อง

```powershell
python -m http.server 4178 --bind 127.0.0.1 --directory website/dist
```

เปิด `http://127.0.0.1:4178/` เว็บใช้ HTML/CSS/JavaScript โดยไม่ต้องติดตั้ง framework

## GitHub

Remote: https://github.com/ArmZOfficial/Minecraft-Luna.git
การผูก remote และ commit ในเครื่องไม่เท่ากับการ push สำเร็จ ให้ดูสถานะส่งมอบในแชตหรือประวัติ GitHub
