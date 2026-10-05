# Luma website prototype

เว็บต้นแบบแนว Minecraft modern สำหรับ Luma ใช้เขียวเข้ม–ทอง ภาพเมืองเต็มสีและโลโก้ original
มีหน้าแรก ข่าว แผนที่ คู่มือ กิจกรรม ร้านค้า ศูนย์บัญชี อันดับ สถานะ และร่างเงื่อนไข/ความเป็นส่วนตัว
ทดลองค้นหาทั้งเว็บ (`Ctrl+K`), ค้นหา 12 โซน, เลื่อน/ขยายภาพ, กรองคู่มือ/ข่าว, เปิดรายละเอียด และสลับแท็บบัญชีได้
มีธีมกลางวัน/กลางคืนและตัวเลือกลดการเคลื่อนไหว
ใช้ native HTML/CSS/JS ไม่มีขั้นตอนติดตั้ง dependency หรือ build

**สถานะจริง:** ไม่มี payment endpoint, authentication, order database หรือ Minecraft delivery bridge ในต้นแบบนี้
ปุ่มรับเงินปิดไว้ ไม่สร้าง QR รับเงินจริง ไม่อ้างว่าตรวจรหัสเกมสำเร็จ
ภาพแผนที่เป็น concept art กับหมุดตัวอย่าง ไม่ใช่ BlueMap สด

แผนเชื่อมระบบจริงอยู่ใน [WEB-AND-AUTOMATION-PLAN-th.md](../output/lobby-concept/WEB-AND-AUTOMATION-PLAN-th.md)
รันดูในเครื่องด้วย Python HTTP server โดยชี้ root ไป `dist` หรือเปิด `dist/index.html`
เส้นทางใช้ hash เพื่อรองรับ static hosting ได้โดยไม่ต้อง rewrite

ชื่อ Luma เป็นข้อเสนอสำหรับงานนี้ ยังไม่ตรวจ trademark/domain availability

ภาพเว็บแปลงเป็น WebP รวมประมาณ 7.4 MB จากต้นฉบับ PNG ประมาณ 50 MB และโหลดภาพส่วนล่างแบบ lazy
เก็บภาพ PNG ต้นฉบับใน `output/lobby-concept` ครบ
หากต้อง regenerate WebP ใช้ `tools/prepare_web_assets.mjs` กับ Node และ package `sharp`
เว็บปลายทางไม่ต้องติดตั้ง sharp หรือ Node

Source repository: https://github.com/ArmZOfficial/Minecraft-Luna
