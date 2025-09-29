# InventoryBorrowApp

โปรแกรม **ยืม-คืนอุปกรณ์กีฬา** แบบ Console Application เขียนด้วยภาษา **Java** โดยใช้แนวคิด **OOP (Object-Oriented Programming)** อย่างครบถ้วน

---

## 📌 สิ่งที่โปรแกรมทำได้
- เพิ่มอุปกรณ์ใหม่ พร้อมระบุจำนวน
- ลดจำนวนอุปกรณ์ (โดยไม่ให้ต่ำกว่ายอดที่ถูกยืมค้าง)
- ค้นหาอุปกรณ์ตามชื่อ
- แสดงรายการอุปกรณ์ทั้งหมด
- ยืมอุปกรณ์ โดยระบุนิสิต, รหัสอุปกรณ์ และจำนวน
- คืนอุปกรณ์ (เช็คจำนวนก่อน, ถ้าไม่มีการยืมค้างจะคืนไม่ได้)
- ดูประวัติการยืม-คืนทั้งหมด หรือค้นหาตามรหัสนิสิต

---

## 📌 Feature / Function
1. **Add item** → สร้าง object ใหม่จากคลาส `Equipment`  
2. **Reduce item** → ลดจำนวนอุปกรณ์ที่มีอยู่  
3. **Search item** → ค้นหาอุปกรณ์จากชื่อ  
4. **List all items** → แสดงรายการอุปกรณ์ทั้งหมด  
5. **Borrow** → ยืมอุปกรณ์ พร้อมบันทึกประวัติ  
6. **Return** → คืนอุปกรณ์ ถ้ามีการยืมค้างอยู่  
7. **Show Transaction** → แสดงประวัติการยืม-คืน (ทั้งหมดหรือเฉพาะรหัสนิสิต)  

---

## 📌 หลักการ OOP ที่ใช้
### 1. Class
มีคลาสทั้งหมด **7 คลาส**
- `Item` (abstract base class)  
- `Equipment` (extends Item)  
- `EquipmentRepository`  
- `Tx` (Transaction)  
- `EquipmentService`  
- `BorrowService`  
- `InventoryBorrowApp` (Main + CLI Menu)

### 2. Encapsulation (การห่อหุ้ม)
- ตัวแปรภายในแต่ละคลาสถูกกำหนดเป็น `private` เช่น `id`, `name`, `total`, `available`  
- ใช้ `getter` หรือ `method` ในการเข้าถึงและแก้ไขค่า เช่น `getAvailable()`, `addQuantity()`, `borrow()`, `giveBack()`

### 3. Inheritance (การสืบทอด)
- คลาส `Equipment` สืบทอด (`extends`) มาจาก `Item`  
- ใช้ `super()` เพื่อเรียก constructor ของคลาสแม่

### 4. Polymorphism (พหุรูป)
- เมธอด `public abstract String type();` อยู่ใน `Item`  
- คลาสลูก `Equipment` override เมธอดนี้ → คืนค่า `"Equipment"`  
- การ override `toString()` ของแต่ละคลาสเพื่อแสดงผลในรูปแบบต่างกัน

### 5. Abstraction (การทำให้เป็นนามธรรม)
- คลาส `Item` ถูกประกาศเป็น `abstract`  
- มีเมธอด abstract `type()` ที่บังคับให้คลาสลูกต้อง implement

---

