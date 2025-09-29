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

---

### 2. Encapsulation 
- ตัวแปรภายในแต่ละคลาสถูกกำหนดเป็น `private`  
  - `id`, `name` → [บรรทัด 7–8]  
  - `total`, `available` → [บรรทัด 38–39]  
- ใช้ `getter` หรือ `method` ในการเข้าถึงและแก้ไขค่า เช่น  
  - `getId()`, `getName()` → [บรรทัด 14–15]  
  - `getTotal()`, `getAvailable()`, `getBorrowed()` → [บรรทัด 45–47]  
  - `addQuantity()` → [บรรทัด 49]  
  - `borrow()` → [บรรทัด 62]  
  - `giveBack()` → [บรรทัด 69]  

---

### 3. Inheritance 
- คลาส `Equipment` สืบทอด (`extends`) มาจาก `Item` → [บรรทัด 33]  
- ใช้ `super()` เพื่อเรียก constructor ของคลาสแม่ → [บรรทัด 36]  

---

### 4. Polymorphism 
- เมธอด `public abstract String type();` อยู่ใน `Item` → [บรรทัด 17]  
- คลาสลูก `Equipment` override เมธอดนี้ → คืนค่า `"Equipment"` → [บรรทัด 42]  
- การ override `toString()` ของแต่ละคลาสเพื่อแสดงผลในรูปแบบต่างกัน → [บรรทัด 21–23, 74–76]  

---

### 5. Abstraction 
- คลาส `Item` ถูกประกาศเป็น `abstract` → [บรรทัด 4]  
- มีเมธอด abstract `type()` ที่บังคับให้คลาสลูกต้อง implement → [บรรทัด 17]  

---
