# เขาวงกตตัวเลข (Number Maze)

Mini project วิชา Object-Oriented Programming — เกมเขาวงกตที่ต้องแก้สมการคณิตศาสตร์เพื่อเปิดประตู เขียนด้วยภาษา Java (Swing)

**ผู้จัดทำ:** 6804062611033 นายวัชรเศรษฐ์ ว่องรัตน์วานิช

![ภาพหน้าจอเกม ด่าน 3](docs/screenshot.png)

## ไฟล์ที่ส่ง

| ไฟล์ | คำอธิบาย |
|---|---|
| [NumberMaze.jar](NumberMaze.jar) | ไฟล์เกม ดับเบิลคลิกเพื่อเล่น (ต้องมี Java 17 ขึ้นไป) |
| [docs/NumberMaze-Report.pdf](docs/NumberMaze-Report.pdf) | รายงานฉบับสมบูรณ์ (PDF) |
| [docs/NumberMaze-Report.docx](docs/NumberMaze-Report.docx) | รายงานฉบับสมบูรณ์ (Word) |
| ไฟล์ `.java` ในโฟลเดอร์หลัก | Source code ทั้งหมด 20 คลาส |
| [docs/](docs) | Class Diagram, โครงสร้าง GUI และ Sequence Diagram (ไฟล์ `.png` และต้นฉบับ PlantUML `.puml`) |

## เนื้อเรื่อง

นักสำรวจติดอยู่ในเขาวงกตลึกลับ ซึ่งแต่ละเส้นทางถูกปิดกั้นด้วย "ประตูรหัสผ่าน" วิธีเดียวที่จะผ่านไปได้คือการแก้สมการให้ถูกต้อง ผู้เล่นต้องเดินสำรวจเพื่อเก็บตัวเลขและเครื่องหมายที่กระจายอยู่ในด่าน แล้วนำมาจัดเรียงเพื่อปลดล็อกเส้นทางสู่ทางออก

## วิธีเล่น

| ปุ่ม | การทำงาน |
|---|---|
| `W` `A` `S` `D` หรือ ลูกศร | เดิน |
| `R` | เริ่มด่านใหม่ |
| `Enter` | เล่นใหม่หลังจบเกม |

- เดินทับตัวเลข/เครื่องหมายเพื่อเก็บเข้ากระเป๋า
- เดินชนประตูภารกิจ แล้วกดไอเทมเรียงเป็นสมการให้ได้ผลลัพธ์ตามเป้าหมาย
- ระบบคำนวณตามลำดับความสำคัญทางคณิตศาสตร์จริง (คูณ/หาร ก่อน บวก/ลบ) เช่น `3 + 2 * 4 = 11`
- ไอเทมที่ใช้ในสมการที่ถูกต้องจะหายไป ต้องวางแผนการใช้ให้ดี
- ระวังกับดักหนามและผู้พิทักษ์ ถ้าโดนจะถูกส่งกลับจุดเริ่มต้น
- ผ่านประตูชัยของด่านสุดท้ายเพื่อรับข้อความ **YOU SURVIVED!**

### ด่าน

| ด่าน | อุปสรรค | ขนาด |
|---|---|---|
| 1: ห้องฝึกหัด | ไม่มี | 15 × 11 |
| 2: ทางหนาม | กับดักหนาม | 15 × 11 |
| 3: ผู้พิทักษ์ | หนาม + ผู้พิทักษ์ลาดตระเวน | 21 × 13 |

## วิธีรัน

ต้องมี Java 17 ขึ้นไป

**แบบที่ 1 — เล่นทันที:** ดาวน์โหลด [`NumberMaze.jar`](NumberMaze.jar) (หรือจากหน้า [Releases](../../releases)) แล้วดับเบิลคลิก หรือรันคำสั่ง

```bash
java -jar NumberMaze.jar
```

**แบบที่ 2 — รันจากโค้ด:** เปิดโฟลเดอร์นี้ใน VS Code แล้วกด Run ที่ `Main.java` หรือ

```bash
javac -encoding UTF-8 *.java
java Main
```

**สร้างไฟล์ jar เอง:** ดับเบิลคลิก `build.bat` (Windows) จะได้ไฟล์ `dist\NumberMaze.jar`

## โครงสร้างคลาส

Class Diagram แบบเต็ม: [docs/class-diagram.png](docs/class-diagram.png) (ต้นฉบับ PlantUML: [docs/class-diagram.puml](docs/class-diagram.puml))

```
GameObject (abstract)
├── Player                  มี Inventory (Composition)
├── Item (abstract)
│   ├── NumberItem
│   └── OperatorItem
├── Door (abstract) ──────── implements Interactable
│   ├── PuzzleDoor          เปิดหน้าต่าง PuzzleDialog
│   └── ExitDoor            จบด่าน
└── Obstacle (abstract)
    ├── Spike               หนามโผล่-หดเป็นจังหวะ
    └── Guard               เดินลาดตระเวนซ้าย-ขวา

GamePanel (extends JPanel)  ลูปเกม, รับปุ่ม, วาดหน้าจอ
Level / Levels              อ่านแผนที่จากข้อความ / เก็บข้อมูลทุกด่าน
PuzzleDialog (extends JDialog)
ExpressionEvaluator         คำนวณสมการตามลำดับความสำคัญ
Inventory, Sprites, Main
```

## แนวคิด OOP ที่ใช้

- **Encapsulation** — field เป็น `private`/`protected` เข้าถึงผ่าน getter เช่น `Inventory.getItems()` คืน list แบบอ่านอย่างเดียว, `Door.unlock()` เป็น `protected`
- **Inheritance** — `Player`, `Item`, `Door`, `Obstacle` สืบทอดจาก `GameObject`
- **Abstraction** — abstract class `GameObject`, `Item`, `Door`, `Obstacle` และ interface `Interactable`
- **Polymorphism** — `door.interact(...)`, `obstacle.update(...)`, `item.draw(...)` แต่ละคลาสทำงานต่างกัน
- **Composition** — `Player` มี `Inventory`, `Level` มี `Item`/`Door`/`Obstacle`, `GamePanel` มี `Level` และ `Player`

## เครื่องมือที่ใช้

Java 25 (Swing, Java2D, ImageIO) · VS Code · Git / GitHub · Python + Pillow (สร้างภาพพื้น/กำแพง/ผู้พิทักษ์)
